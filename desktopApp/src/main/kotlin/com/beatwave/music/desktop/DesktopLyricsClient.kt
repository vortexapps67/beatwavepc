package com.beatwave.music.desktop

import com.beatwave.music.data.lyrics.LrcLib
import com.beatwave.music.data.lyrics.LyricLine
import com.beatwave.music.data.lyrics.LyricSyllable
import com.beatwave.music.data.lyrics.LyricWord
import com.beatwave.music.data.lyrics.LyricsRepository
import com.beatwave.music.data.lyrics.LyricsSource
import com.beatwave.music.data.lyrics.withBackgroundVocals
import com.beatwave.music.data.model.Song
import com.beatwave.music.data.model.durationMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap

@Serializable
internal data class DiskLyricSyllable(
    val startMs: Long,
    val endMs: Long,
    val startChar: Int,
    val endChar: Int,
)

@Serializable
internal data class DiskLyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val syllables: List<DiskLyricSyllable> = emptyList(),
)

@Serializable
internal data class DiskLyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<DiskLyricWord> = emptyList(),
    val sungUntilMs: Long? = null,
)

@Serializable
internal data class DiskLyrics(
    val source: String,
    val lines: List<DiskLyricLine>,
)

/** A track's lyrics and which provider they came from, by its label. */
data class DesktopLyrics(val source: String, val lines: List<LyricLine>) {
    val plainText: String get() = lines.joinToString("\n") { it.text }
    val wordSynced: Boolean get() = lines.any(LyricLine::isWordSynced)
}

/**
 * The desktop's lyrics lookup with persistent disk cache for instant loading.
 */
object DesktopLyricsClient {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    internal val directory: Path by lazy {
        val base = System.getenv("LOCALAPPDATA")?.takeIf(String::isNotBlank)
            ?: System.getenv("XDG_CACHE_HOME")?.takeIf(String::isNotBlank)
            ?: "${System.getProperty("user.home")}/.cache"
        Path.of(base, "beatwave", "lyrics_cache").also {
            runCatching { Files.createDirectories(it) }
        }
    }

    /** Keys the settings screen writes and this reads. */
    internal const val KEY_SYNCED_LYRICS = "synced_lyrics"
    internal const val KEY_PRIORITIZE_SYLLABLES = "prioritize_syllable_lyrics"
    internal const val KEY_LYRICS_BLUR = "lyrics_blur"
    internal const val KEY_LYRICS_SOURCES = "lyrics_sources"

    /** Which sources have ever been put in front of the listener. */
    internal const val KEY_LYRICS_SOURCES_SEEN = "lyrics_sources_seen"
    internal const val KEY_LYRICS_ORDER = "lyrics_source_order"

    /** One lyric database, as the settings screen needs to describe it. */
    internal data class Source(val name: String, val detail: String, val wordSynced: Boolean)

    /** Every provider the phone has, in the order it asks them out of the box. */
    internal val sources: List<Source> = LyricsSource.offered.map { Source(it.label, it.detail, it.wordSynced) }

    internal fun enabledSources(order: List<String>, enabled: Set<String>): List<String> =
        order.filter { it in enabled }

    private class Cached(val createdAt: Long, val result: Result<DesktopLyrics>)

    private val cache = ConcurrentHashMap<String, Cached>()

    /**
     * Synchronously returns cached lyrics from memory or disk for instant 0ms display.
     */
    fun cached(song: Song, only: String? = null): DesktopLyrics? {
        val memory = cache["${song.videoId}|${only.orEmpty()}"]?.result?.getOrNull()
        if (memory != null) return memory

        val diskFile = directory.resolve("${song.videoId}.json")
        if (Files.isRegularFile(diskFile)) {
            runCatching {
                val text = Files.readString(diskFile)
                val disk = json.decodeFromString(DiskLyrics.serializer(), text)
                val lines = disk.lines.map { l ->
                    LyricLine(
                        timeMs = l.timeMs,
                        text = l.text,
                        sungUntilMs = l.sungUntilMs,
                        words = l.words.map { w ->
                            LyricWord(
                                startMs = w.startMs,
                                endMs = w.endMs,
                                text = w.text,
                                syllables = w.syllables.map { s ->
                                    LyricSyllable(s.startMs, s.endMs, s.startChar, s.endChar)
                                }
                            )
                        }
                    )
                }
                val lyrics = DesktopLyrics(disk.source, lines)
                cache["${song.videoId}|${only.orEmpty()}"] = Cached(System.currentTimeMillis(), Result.success(lyrics))
                return lyrics
            }
        }
        return null
    }

    /**
     * Prefetches lyrics for upcoming songs in background so they are ready on disk.
     */
    fun prefetch(songs: List<Song>, maxTracks: Int = 5) {
        scope.launch {
            for (song in songs.take(maxTracks)) {
                if (cached(song) != null) continue
                runCatching { lookup(song) }
            }
        }
    }

    /**
     * Looks up lyrics with instant disk cache hit or network fallback.
     */
    suspend fun lookup(
        song: Song,
        durationMs: Long = song.durationMillis(),
        only: String? = null,
    ): Result<DesktopLyrics> {
        val persistence = DesktopPersistence()
        if (!persistence.boolean(KEY_SYNCED_LYRICS, true)) {
            return Result.failure(IllegalStateException("Synced lyrics are switched off"))
        }

        // 1. Instant cache check (memory or disk)
        cached(song, only)?.let { return Result.success(it) }

        val configured = enabledSources(persistence.lyricsSourceOrder(), persistence.lyricsEnabledSources())
            .mapNotNull(::lyricsSourceNamed)
        val asked = only?.let(::lyricsSourceNamed)?.let(::listOf) ?: configured
        if (asked.isEmpty()) return Result.failure(IllegalStateException("No lyrics sources are enabled"))

        // 2. Check embedded file words
        val embedded = if (only == null) embedded(song) else null
        val result = if (embedded != null) {
            Result.success(DesktopLyrics("Downloaded", embedded))
        } else {
            val found = LyricsRepository.lyrics(
                videoId = song.videoId,
                title = song.title,
                artist = song.artist,
                durationMs = durationMs,
                album = song.albumName,
                sources = asked.toSet(),
                order = asked,
                prioritizeSyllableSync = persistence.boolean(KEY_PRIORITIZE_SYLLABLES, false),
            )
            if (found == null) {
                Result.failure(IllegalStateException("Lyrics unavailable"))
            } else {
                Result.success(DesktopLyrics(found.source.label, found.lines))
            }
        }

        // Cache in memory
        cache["${song.videoId}|${only.orEmpty()}"] = Cached(System.currentTimeMillis(), result)

        // Save to disk cache if successful
        result.getOrNull()?.let { lyrics ->
            scope.launch {
                saveToDisk(song.videoId, lyrics)
            }
        }

        return result
    }

    private fun saveToDisk(videoId: String, lyrics: DesktopLyrics) {
        runCatching {
            val diskLines = lyrics.lines.map { l ->
                DiskLyricLine(
                    timeMs = l.timeMs,
                    text = l.text,
                    sungUntilMs = l.sungUntilMs,
                    words = l.words.map { w ->
                        DiskLyricWord(
                            startMs = w.startMs,
                            endMs = w.endMs,
                            text = w.text,
                            syllables = w.syllables.map { s ->
                                DiskLyricSyllable(s.startMs, s.endMs, s.startChar, s.endChar)
                            }
                        )
                    }
                )
            }
            val disk = DiskLyrics(lyrics.source, diskLines)
            val jsonString = json.encodeToString(DiskLyrics.serializer(), disk)
            val target = directory.resolve("$videoId.json")
            val temp = directory.resolve(".$videoId.json.part")
            Files.writeString(temp, jsonString, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    private fun embedded(song: Song): List<LyricLine>? {
        val path = song.localPath?.let { runCatching { java.nio.file.Paths.get(it) }.getOrNull() }
            ?: song.localUri?.let { runCatching { java.nio.file.Paths.get(java.net.URI(it)) }.getOrNull() }
            ?: return null
        val raw = DesktopEmbeddedLyrics.read(path) ?: return null
        return LrcLib.parseLrc(raw).takeIf { lines -> lines.any { it.text.isNotBlank() } }?.withBackgroundVocals()
    }
}
