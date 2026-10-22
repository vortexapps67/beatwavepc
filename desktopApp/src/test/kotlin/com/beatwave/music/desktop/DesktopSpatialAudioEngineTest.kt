package com.beatwave.music.desktop

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSpatialAudioEngineTest {

    @Test
    fun `mono audio declines spatialisation and remains bit-exact`() {
        val rate = 48000
        val engine = DesktopSpatialAudioEngine(channels = 1, sampleRate = rate)
        assertFalse(engine.applies, "Engine should not apply to mono")

        val monoSamples = FloatArray(1000) { sin(it * 0.05).toFloat() }
        val copy = monoSamples.copyOf()
        engine.process(monoSamples, monoSamples.size)

        assertTrue(monoSamples.contentEquals(copy), "Mono samples should be unchanged")
    }

    @Test
    fun `OFF mode produces bit-exact passthrough on stereo audio`() {
        val rate = 48000
        val engine = DesktopSpatialAudioEngine(channels = 2, sampleRate = rate)
        engine.mode = SpatialAudioMode.OFF

        val samples = FloatArray(2000) { sin(it * 0.02).toFloat() * 0.7f }
        val copy = samples.copyOf()
        engine.process(samples, samples.size)

        assertTrue(samples.contentEquals(copy), "Stereo samples in OFF mode must be bit-exact passthrough")
    }

    @Test
    fun `8D Spatial Audio engine rotates azimuth and modulates binaural cues`() {
        val rate = 48000
        val engine = DesktopSpatialAudioEngine(channels = 2, sampleRate = rate)
        engine.mode = SpatialAudioMode.EIGHT_D
        engine.eightD.rotationHz = 0.25f // 4.0s per revolution
        engine.eightD.depth = 0.90f

        val frameCount = 48000 // 1 full second
        val samples = FloatArray(frameCount * 2) { index ->
            val frame = index / 2
            (sin(2.0 * PI * 440.0 * frame / rate) * 0.6).toFloat()
        }

        engine.process(samples, samples.size)

        // Read telemetry
        val telemetry = engine.readTelemetry()
        assertEquals(SpatialAudioMode.EIGHT_D, telemetry.mode)
        assertTrue(telemetry.azimuthDegrees in 0f..360f, "Azimuth degree must be within [0, 360]")
        assertTrue(telemetry.pan in -1f..1f, "Pan must be within [-1, 1]")

        // Verify output has no NaNs or Infinities
        for (s in samples) {
            assertFalse(s.isNaN(), "Sample must not be NaN")
            assertFalse(s.isInfinite(), "Sample must not be Infinite")
            assertTrue(abs(s) < 2.0f, "Sample must not explode: $s")
        }
    }

    @Test
    fun `7_1 Virtual Surround produces valid 8-speaker telemetry and immersive downmix`() {
        val rate = 48000
        val engine = DesktopSpatialAudioEngine(channels = 2, sampleRate = rate)
        engine.mode = SpatialAudioMode.SURROUND_7_1
        engine.surround71.roomPreset = SurroundRoomPreset.CINEMA
        engine.surround71.bassBoostEnabled = true

        val frameCount = 24000
        val samples = FloatArray(frameCount * 2) { index ->
            val frame = index / 2
            val isLeft = index % 2 == 0
            val freq = if (isLeft) 250.0 else 1000.0
            (sin(2.0 * PI * freq * frame / rate) * 0.5).toFloat()
        }

        engine.process(samples, samples.size)

        val telemetry = engine.readTelemetry()
        assertEquals(SpatialAudioMode.SURROUND_7_1, telemetry.mode)
        assertEquals(8, telemetry.speakerEnergies.size, "Should have 8 speaker energy levels")

        for (i in 0 until 8) {
            val energy = telemetry.speakerEnergies[i]
            assertFalse(energy.isNaN(), "Speaker $i energy must not be NaN")
            assertTrue(energy in 0f..1.5f, "Speaker $i energy must be non-negative: $energy")
        }

        // Verify downmix signal integrity
        for (s in samples) {
            assertFalse(s.isNaN())
            assertFalse(s.isInfinite())
            assertTrue(abs(s) < 2.0f)
        }
    }

    @Test
    fun `biquad filter frequency responses are stable and valid`() {
        val rate = 48000
        val lpf = BiquadFilter().apply { configureLowPass(rate, 120f) }
        val bpf = BiquadFilter().apply { configureBandPass(rate, 1000f) }
        val hsf = BiquadFilter().apply { configureHighShelf(rate, 4000f, -3f) }
        val lsf = BiquadFilter().apply { configureLowShelf(rate, 80f, 4.5f) }

        for (i in 0 until 1000) {
            val input = (sin(i * 0.1) * 0.8).toFloat()
            val yLpf = lpf.process(input)
            val yBpf = bpf.process(input)
            val yHsf = hsf.process(input)
            val yLsf = lsf.process(input)

            assertFalse(yLpf.isNaN())
            assertFalse(yBpf.isNaN())
            assertFalse(yHsf.isNaN())
            assertFalse(yLsf.isNaN())
        }
    }
}
