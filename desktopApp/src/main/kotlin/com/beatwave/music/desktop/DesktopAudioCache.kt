package com.beatwave.music.desktop

import com.beatwave.music.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.streams.toList

/**
 * High-performance disk cache for audio streams.
 * Enables instant 0-latency playback and eliminates rebuffering.
 */
internal object DesktopAudioCache {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val inFlight = ConcurrentHashMap<String, Boolean>()

    val directory: Path by lazy {
        val base = System.getenv("LOCALAPPDATA")?.takeIf(String::isNotBlank)
            ?: System.getenv("XDG_CACHE_HOME")?.takeIf(String::isNotBlank)
            ?: "${System.getProperty("user.home")}/.cache"
        Path.of(base, "beatwave", "audio_cache").also {
            runCatching { Files.createDirectories(it) }
        }
    }

    /**
     * Returns the cached audio file for [videoId] if it exists and is valid.
     */
    fun cachedFile(videoId: String): Path? {
        val dir = directory
        if (!Files.isDirectory(dir)) return null
        val candidates = listOf("m4a", "opus", "mp3", "flac", "webm", "audio")
        for (ext in candidates) {
            val candidate = dir.resolve("$videoId.$ext")
            if (Files.isRegularFile(candidate)) {
                val size = runCatching { Files.size(candidate) }.getOrDefault(0L)
                if (size > 100_000L) { // At least 100KB for valid audio
                    return candidate
                }
            }
        }
        return null
    }

    fun isCached(videoId: String): Boolean = cachedFile(videoId) != null

    /**
     * Automatically caches the given [song] in the background if not already cached.
     */
    fun prefetch(song: Song) {
        if (isCached(song.videoId) || DesktopDownloadManager.isSaved(song)) return
        if (inFlight.putIfAbsent(song.videoId, true) != null) return

        scope.launch {
            try {
                cacheSongInternal(song)
            } catch (t: Throwable) {
                DesktopTrackLog.log("audio cache: failed to prefetch '${song.title}': ${t.message}")
            } finally {
                inFlight.remove(song.videoId)
            }
        }
    }

    fun prefetch(songs: List<Song>, maxTracks: Int = 3) {
        songs.take(maxTracks).forEach { prefetch(it) }
    }

    private suspend fun cacheSongInternal(song: Song) {
        val resolved = DesktopMusicSources.resolve(song, forDownload = false).getOrNull() ?: return
        val url = resolved.url
        if (url.startsWith("file:") || !url.startsWith("http")) return

        val ext = when {
            url.contains(".m4a") -> "m4a"
            url.contains(".opus") -> "opus"
            url.contains(".mp3") -> "mp3"
            url.contains(".flac") -> "flac"
            else -> "m4a"
        }

        val target = directory.resolve("${song.videoId}.$ext")
        val temp = directory.resolve(".${song.videoId}.$ext.part")

        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        resolved.headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000

        try {
            connection.inputStream.use { input ->
                Files.newOutputStream(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { output ->
                    input.copyTo(output)
                }
            }
            if (Files.size(temp) > 100_000L) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                DesktopTrackLog.log("audio cache: successfully cached '${song.title}' (${Files.size(target) / 1024} KB)")
                pruneLruBudget()
            } else {
                Files.deleteIfExists(temp)
            }
        } catch (_: Exception) {
            Files.deleteIfExists(temp)
        } finally {
            connection.disconnect()
        }
    }

    /** Prunes cache if size exceeds 2 GB. */
    private fun pruneLruBudget(maxBytes: Long = 2L * 1024L * 1024L * 1024L) {
        runCatching {
            val files = Files.list(directory)
                .filter { Files.isRegularFile(it) && !it.fileName.toString().startsWith(".") }
                .toList()
            val totalSize = files.sumOf { Files.size(it) }
            if (totalSize > maxBytes) {
                val sorted = files.sortedBy { Files.getLastModifiedTime(it).toMillis() }
                var currentSize = totalSize
                for (file in sorted) {
                    if (currentSize <= maxBytes * 0.8) break
                    val sz = Files.size(file)
                    Files.deleteIfExists(file)
                    currentSize -= sz
                }
            }
        }
    }
}
