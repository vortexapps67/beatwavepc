package com.beatwave.music.desktop

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Operating mode for the spatial acoustics processor.
 */
enum class SpatialAudioMode(val label: String) {
    OFF("Standard Stereo"),
    EIGHT_D("8D Binaural Orbit"),
    SURROUND_7_1("7.1 Virtual Surround");

    companion object {
        fun from(name: String?): SpatialAudioMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OFF
    }
}

/**
 * Acoustic room ambience presets for the 7.1 Virtual Surround engine.
 */
enum class SurroundRoomPreset(
    val label: String,
    val earlyDelayMs: Float,
    val feedback: Float,
    val wetMix: Float,
) {
    STUDIO("Studio (Tight)", 9f, 0.20f, 0.16f),
    CINEMA("Cinema (Spacious)", 34f, 0.45f, 0.32f),
    CONCERT_HALL("Concert Hall (Atmospheric)", 68f, 0.68f, 0.48f);

    companion object {
        fun from(name: String?): SurroundRoomPreset =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CINEMA
    }
}

/**
 * High-speed telemetry snapshot emitted to UI components at 60fps for the 3D Radar Visualizer.
 */
data class SpatialAudioTelemetry(
    val mode: SpatialAudioMode = SpatialAudioMode.OFF,
    val azimuthDegrees: Float = 0f,
    val orbitRadius: Float = 0.85f,
    val pan: Float = 0f,
    val isBehind: Boolean = false,
    /**
     * Normalized instantaneous energy [0.0f .. 1.0f] of the 8 virtual speakers in 7.1 mode:
     * Index 0: FL (Front Left)
     * Index 1: FC (Center)
     * Index 2: FR (Front Right)
     * Index 3: SL (Side Left)
     * Index 4: SR (Side Right)
     * Index 5: RL (Rear Left)
     * Index 6: RR (Rear Right)
     * Index 7: SUB (LFE / Subwoofer)
     */
    val speakerEnergies: FloatArray = FloatArray(8),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialAudioTelemetry) return false
        return mode == other.mode &&
            azimuthDegrees == other.azimuthDegrees &&
            orbitRadius == other.orbitRadius &&
            pan == other.pan &&
            isBehind == other.isBehind &&
            speakerEnergies.contentEquals(other.speakerEnergies)
    }

    override fun hashCode(): Int {
        var result = mode.hashCode()
        result = 31 * result + azimuthDegrees.hashCode()
        result = 31 * result + orbitRadius.hashCode()
        result = 31 * result + pan.hashCode()
        result = 31 * result + isBehind.hashCode()
        result = 31 * result + speakerEnergies.contentHashCode()
        return result
    }
}

/**
 * Direct Form II Transposed Biquad Filter.
 * Numerically stable, zero-allocation per sample.
 */
internal class BiquadFilter {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    private var z1 = 0f
    private var z2 = 0f

    fun reset() {
        z1 = 0f
        z2 = 0f
    }

    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return if (y.isNaN() || y.isInfinite()) 0f else y
    }

    fun configureLowPass(sampleRate: Int, cutoffHz: Float, q: Float = 0.7071f) {
        val omega = (2.0 * PI * cutoffHz / sampleRate).coerceIn(0.001, PI - 0.001)
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * q)

        val a0 = 1.0 + alpha
        b0 = (((1.0 - cs) * 0.5) / a0).toFloat()
        b1 = ((1.0 - cs) / a0).toFloat()
        b2 = (((1.0 - cs) * 0.5) / a0).toFloat()
        a1 = ((-2.0 * cs) / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
    }

    fun configureHighPass(sampleRate: Int, cutoffHz: Float, q: Float = 0.7071f) {
        val omega = (2.0 * PI * cutoffHz / sampleRate).coerceIn(0.001, PI - 0.001)
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * q)

        val a0 = 1.0 + alpha
        b0 = (((1.0 + cs) * 0.5) / a0).toFloat()
        b1 = ((-(1.0 + cs)) / a0).toFloat()
        b2 = (((1.0 + cs) * 0.5) / a0).toFloat()
        a1 = ((-2.0 * cs) / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
    }

    fun configureBandPass(sampleRate: Int, centerHz: Float, q: Float = 0.7071f) {
        val omega = (2.0 * PI * centerHz / sampleRate).coerceIn(0.001, PI - 0.001)
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * q)

        val a0 = 1.0 + alpha
        b0 = (alpha / a0).toFloat()
        b1 = 0f
        b2 = (-alpha / a0).toFloat()
        a1 = ((-2.0 * cs) / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
    }

    fun configureHighShelf(sampleRate: Int, cutoffHz: Float, gainDb: Float, q: Float = 0.7071f) {
        val a = Math.pow(10.0, (gainDb / 40.0))
        val omega = (2.0 * PI * cutoffHz / sampleRate).coerceIn(0.001, PI - 0.001)
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * q)
        val beta = 2.0 * sqrt(a) * alpha

        val a0 = (a + 1.0) - (a - 1.0) * cs + beta
        b0 = (a * ((a + 1.0) + (a - 1.0) * cs + beta) / a0).toFloat()
        b1 = (-2.0 * a * ((a - 1.0) + (a + 1.0) * cs) / a0).toFloat()
        b2 = (a * ((a + 1.0) + (a - 1.0) * cs - beta) / a0).toFloat()
        a1 = (2.0 * ((a - 1.0) - (a + 1.0) * cs) / a0).toFloat()
        a2 = (((a + 1.0) - (a - 1.0) * cs - beta) / a0).toFloat()
    }

    fun configureLowShelf(sampleRate: Int, cutoffHz: Float, gainDb: Float, q: Float = 0.7071f) {
        val a = Math.pow(10.0, (gainDb / 40.0))
        val omega = (2.0 * PI * cutoffHz / sampleRate).coerceIn(0.001, PI - 0.001)
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2.0 * q)
        val beta = 2.0 * sqrt(a) * alpha

        val a0 = (a + 1.0) + (a - 1.0) * cs + beta
        b0 = (a * ((a + 1.0) - (a - 1.0) * cs + beta) / a0).toFloat()
        b1 = (2.0 * a * ((a - 1.0) - (a + 1.0) * cs) / a0).toFloat()
        b2 = (a * ((a + 1.0) - (a - 1.0) * cs - beta) / a0).toFloat()
        a1 = (-2.0 * ((a - 1.0) + (a + 1.0) * cs) / a0).toFloat()
        a2 = (((a + 1.0) + (a - 1.0) * cs - beta) / a0).toFloat()
    }
}

/**
 * Circular power-of-two delay ring buffer for low-latency fractional/integer delay lookup.
 */
internal class FastRingBuffer(capacityPow2: Int = 1024) {
    private val mask = capacityPow2 - 1
    private val buffer = FloatArray(capacityPow2)
    private var writePos = 0

    fun write(sample: Float) {
        buffer[writePos] = sample
        writePos = (writePos + 1) and mask
    }

    fun read(delaySamples: Int): Float {
        val readPos = (writePos - 1 - delaySamples) and mask
        return buffer[readPos]
    }

    fun reset() {
        buffer.fill(0f)
        writePos = 0
    }
}

/**
 * PART 1: 8D SPATIAL AUDIO PROCESSOR (DYNAMIC 360° BINAURAL ORBIT)
 *
 * Implements:
 * 1. Equal-power sinusoidal panning (Interaural Level Difference, flat energy).
 * 2. Interaural Time Difference (ITD ring buffer up to 660 microseconds).
 * 3. Continuous LFO rotation around listener's head.
 * 4. High-frequency pinna / posterior shadow filtering when sound source is behind head.
 * 5. Smooth 100ms parameter ramping to eliminate audio pops.
 */
internal class EightDAudioProcessor(val sampleRate: Int) {
    @Volatile var rotationHz: Float = 0.125f // 8.0s per revolution
    @Volatile var depth: Float = 0.85f

    private var currentDepth = depth
    private var phase = 0.0

    // ITD buffer: 660 microseconds = max delay across average human head width (~22 cm)
    private val maxItdSamples = ((sampleRate * 0.000660).roundToInt()).coerceAtLeast(1)
    private val leftDelay = FastRingBuffer(512)
    private val rightDelay = FastRingBuffer(512)

    // Pinna / head shadow single-pole low-pass filters
    private var pinnaL = 0f
    private var pinnaR = 0f

    // Smoothing coefficient for parameter ramping (approx 100ms time constant)
    private val smoothCoeff = (1.0f - exp(-1.0f / (sampleRate * 0.100f))).coerceIn(0.0001f, 0.05f)

    // Telemetry cached for UI
    @Volatile var telemetryAzimuth: Float = 0f
        private set
    @Volatile var telemetryPan: Float = 0f
        private set
    @Volatile var telemetryBehind: Boolean = false
        private set

    fun reset() {
        phase = 0.0
        leftDelay.reset()
        rightDelay.reset()
        pinnaL = 0f
        pinnaR = 0f
    }

    fun process(samples: FloatArray, count: Int) {
        val step = 2.0 * PI * rotationHz.coerceIn(0.04f, 0.45f) / sampleRate.coerceAtLeast(1)
        val targetDepth = depth.coerceIn(0f, 1f)

        var i = 0
        while (i + 1 < count) {
            val inL = samples[i]
            val inR = samples[i + 1]

            // Write to ITD ring buffers
            leftDelay.write(inL)
            rightDelay.write(inR)

            // Smooth depth transitions
            currentDepth += (targetDepth - currentDepth) * smoothCoeff

            // Advance orbit phase
            phase += step
            if (phase >= 2.0 * PI) phase -= 2.0 * PI

            val currentPhase = phase
            val pan = (sin(currentPhase) * currentDepth).toFloat()
            val elevation = cos(currentPhase).toFloat() // +1.0 = Front, -1.0 = Behind

            // 1. Equal-Power Panning (ILD): Azimuth angle mapping
            val azimuthAngle = ((pan + 1.0f) * (PI / 4.0)).toFloat()
            val gainLeft = cos(azimuthAngle)
            val gainRight = sin(azimuthAngle)

            // 2. Interaural Time Difference (ITD):
            val delaySamples = (abs(pan) * maxItdSamples).roundToInt()
            val readL: Float
            val readR: Float

            if (pan > 0f) {
                // Sound on Right -> Left ear delayed
                readL = leftDelay.read(delaySamples)
                readR = inR
            } else if (pan < 0f) {
                // Sound on Left -> Right ear delayed
                readL = inL
                readR = rightDelay.read(delaySamples)
            } else {
                readL = inL
                readR = inR
            }

            // 3. Pinna / Posterior Shadow Filter:
            // When elevation < 0, source is behind the listener's head.
            val rearFactor = (-elevation).coerceIn(0f, 1f) * currentDepth
            val alpha = 1.0f - (0.50f * rearFactor)

            pinnaL += alpha * (readL - pinnaL)
            pinnaR += alpha * (readR - pinnaR)

            val shadowedL = readL * (1f - rearFactor) + pinnaL * rearFactor
            val shadowedR = readR * (1f - rearFactor) + pinnaR * rearFactor

            // Apply ILD gains and slight makeup headroom
            samples[i] = shadowedL * gainLeft
            samples[i + 1] = shadowedR * gainRight

            i += 2
        }

        // Update telemetry at end of block
        val degrees = Math.toDegrees(phase).toFloat()
        telemetryAzimuth = (degrees % 360f + 360f) % 360f
        telemetryPan = (sin(phase) * targetDepth).toFloat()
        telemetryBehind = cos(phase) < 0.0
    }
}

/**
 * PART 2: 7.1 VIRTUAL SURROUND SOUND PROCESSOR
 *
 * Matrix specification:
 * 1. FL (-30°, 1.5m)
 * 2. FC (0°, 1.4m) - Vocal clarity anchor
 * 3. FR (+30°, 1.5m)
 * 4. SL (-90°, 1.2m)
 * 5. SR (+90°, 1.2m)
 * 6. RL (-135°, 1.8m)
 * 7. RR (+135°, 1.8m)
 * 8. LFE / SUB (< 120Hz 2nd-order Butterworth, +3dB boost, saturation)
 */
internal class VirtualSurround71Processor(val sampleRate: Int) {
    @Volatile var roomPreset: SurroundRoomPreset = SurroundRoomPreset.CINEMA
    @Volatile var bassBoostEnabled: Boolean = true

    // Center channel bandpass filter (300Hz - 4000Hz vocal anchor)
    private val centerHighPass = BiquadFilter().apply { configureHighPass(sampleRate, 300f, 0.7071f) }
    private val centerLowPass = BiquadFilter().apply { configureLowPass(sampleRate, 3800f, 0.7071f) }

    // Subwoofer LFE 2nd order Butterworth filter at 120Hz
    private val subLowPass = BiquadFilter().apply { configureLowPass(sampleRate, 120f, 0.7071f) }

    // Rear speakers high-shelf filter (-3dB at 4kHz) to simulate pinna head-shadow
    private val rearShelfL = BiquadFilter().apply { configureHighShelf(sampleRate, 4000f, -3.2f, 0.7071f) }
    private val rearShelfR = BiquadFilter().apply { configureHighShelf(sampleRate, 4000f, -3.2f, 0.7071f) }

    // Headphone Bass Boost (Low-shelf at 80Hz, +4.5dB)
    private val bassBoostL = BiquadFilter().apply { configureLowShelf(sampleRate, 85f, 4.5f, 0.7071f) }
    private val bassBoostR = BiquadFilter().apply { configureLowShelf(sampleRate, 85f, 4.5f, 0.7071f) }

    // Rear speaker Haas delay buffer (18ms)
    private val haasDelaySamples = ((sampleRate * 0.018).roundToInt()).coerceAtLeast(1)
    private val rearDelayL = FastRingBuffer(1024)
    private val rearDelayR = FastRingBuffer(1024)

    // ITD delays for virtual speaker positioning
    private val maxItdSamples = ((sampleRate * 0.000660).roundToInt()).coerceAtLeast(1)
    private val speakerItdL = FastRingBuffer(512)
    private val speakerItdR = FastRingBuffer(512)

    // Reverb / early reflection comb buffers
    private val earlyReflSamples = ((sampleRate * 0.035).roundToInt()).coerceAtLeast(1)
    private val reverbCombL = FastRingBuffer(4096)
    private val reverbCombR = FastRingBuffer(4096)

    // Telemetry energy array for the 8 virtual speakers (smoothed RMS)
    private val speakerEnergiesRaw = FloatArray(8)
    private val speakerEnergiesSmooth = FloatArray(8)
    @Volatile var telemetryEnergies = FloatArray(8)
        private set

    fun reset() {
        centerHighPass.reset()
        centerLowPass.reset()
        subLowPass.reset()
        rearShelfL.reset()
        rearShelfR.reset()
        bassBoostL.reset()
        bassBoostR.reset()
        rearDelayL.reset()
        rearDelayR.reset()
        speakerItdL.reset()
        speakerItdR.reset()
        reverbCombL.reset()
        reverbCombR.reset()
        speakerEnergiesRaw.fill(0f)
        speakerEnergiesSmooth.fill(0f)
    }

    fun process(samples: FloatArray, count: Int) {
        val preset = roomPreset
        val bassBoost = bassBoostEnabled
        val roomFeedback = preset.feedback
        val roomWet = preset.wetMix
        val roomDelay = ((sampleRate * (preset.earlyDelayMs / 1000f)).roundToInt()).coerceIn(64, 4000)

        // Precalculated geometric ITD sample delays
        val itdFront = (sin(Math.toRadians(30.0)) * maxItdSamples).roundToInt() // ~15 samples
        val itdSide = (sin(Math.toRadians(90.0)) * maxItdSamples).roundToInt()  // ~32 samples
        val itdRear = (sin(Math.toRadians(45.0)) * maxItdSamples).roundToInt()  // ~22 samples

        // Speaker distances & normalized attenuations (ref = 1.4m)
        // FL, FR: 1.5m -> 1.4 / 1.5 = 0.93f
        // FC: 1.4m -> 1.0f
        // SL, SR: 1.2m -> 1.4 / 1.2 = 1.16f
        // RL, RR: 1.8m -> 1.4 / 1.8 = 0.77f
        val attFront = 0.93f
        val attCenter = 1.00f
        val attSide = 1.08f
        val attRear = 0.80f

        var blockEnergyFL = 0f
        var blockEnergyFC = 0f
        var blockEnergyFR = 0f
        var blockEnergySL = 0f
        var blockEnergySR = 0f
        var blockEnergyRL = 0f
        var blockEnergyRR = 0f
        var blockEnergySub = 0f

        var i = 0
        while (i + 1 < count) {
            val inL = samples[i]
            val inR = samples[i + 1]

            // 1. Mid / Side Decomposition
            val mid = (inL + inR) * 0.5f
            val side = (inL - inR) * 0.5f

            // 2. Center Channel Extraction (FC)
            val vocal = centerLowPass.process(centerHighPass.process(mid))
            val chFC = vocal * 1.15f * attCenter

            // 3. Subwoofer (LFE) Extraction
            val lfeRaw = subLowPass.process(mid)
            // Soft saturation warmth + +3dB boost (1.414x)
            val chSUB = tanh(lfeRaw * 1.25f) * 1.414f

            // 4. Front Left & Front Right (subtract centered vocal presence to widen front image)
            val chFL = (inL - (vocal * 0.42f)) * attFront
            val chFR = (inR - (vocal * 0.42f)) * attFront

            // 5. Side Left & Side Right
            val chSL = (side * 1.10f) * attSide
            val chSR = (-side * 1.10f) * attSide

            // 6. Rear Left & Rear Right (Haas delay 18ms + -3dB at 4kHz head shadow shelf)
            rearDelayL.write(chSL)
            rearDelayR.write(chSR)
            val haasL = rearDelayL.read(haasDelaySamples)
            val haasR = rearDelayR.read(haasDelaySamples)
            val chRL = rearShelfL.process(haasL) * attRear
            val chRR = rearShelfR.process(haasR) * attRear

            // Accumulate energy metrics for visualizer
            blockEnergyFL += chFL * chFL
            blockEnergyFC += chFC * chFC
            blockEnergyFR += chFR * chFR
            blockEnergySL += chSL * chSL
            blockEnergySR += chSR * chSR
            blockEnergyRL += chRL * chRL
            blockEnergyRR += chRR * chRR
            blockEnergySub += chSUB * chSUB

            // 7. Binaural Virtual Speaker Positioning:
            speakerItdL.write(chFL + chSL + chRL)
            speakerItdR.write(chFR + chSR + chRR)

            // Front speakers binaural sum
            // FL (-30°): Left direct, Right ear delayed by itdFront
            val binauralFL_L = chFL * 0.88f
            val binauralFL_R = speakerItdL.read(itdFront) * 0.45f

            // FR (+30°): Right direct, Left ear delayed by itdFront
            val binauralFR_R = chFR * 0.88f
            val binauralFR_L = speakerItdR.read(itdFront) * 0.45f

            // Center: perfectly centered
            val binauralFC_L = chFC * 0.707f
            val binauralFC_R = chFC * 0.707f

            // Side speakers:
            val binauralSL_L = chSL * 0.95f
            val binauralSL_R = speakerItdL.read(itdSide) * 0.25f

            val binauralSR_R = chSR * 0.95f
            val binauralSR_L = speakerItdR.read(itdSide) * 0.25f

            // Rear speakers:
            val binauralRL_L = chRL * 0.75f
            val binauralRL_R = speakerItdL.read(itdRear) * 0.32f

            val binauralRR_R = chRR * 0.75f
            val binauralRR_L = speakerItdR.read(itdRear) * 0.32f

            // Subwoofer: omnidirectional (LFE)
            val binauralSUB_L = chSUB * 0.707f
            val binauralSUB_R = chSUB * 0.707f

            // Sum all 8 speakers into binaural bus
            var outL = binauralFL_L + binauralFR_L + binauralFC_L +
                       binauralSL_L + binauralSR_L + binauralRL_L + binauralRR_L + binauralSUB_L

            var outR = binauralFL_R + binauralFR_R + binauralFC_R +
                       binauralSL_R + binauralSR_R + binauralRL_R + binauralRR_R + binauralSUB_R

            // 8. Room Ambience Diffusion Network
            val revPrevL = reverbCombL.read(roomDelay)
            val revPrevR = reverbCombR.read(roomDelay)
            reverbCombL.write(outL + revPrevR * roomFeedback)
            reverbCombR.write(outR + revPrevL * roomFeedback)

            outL += revPrevL * roomWet
            outR += revPrevR * roomWet

            // 9. Headphone Bass Boost
            if (bassBoost) {
                outL = bassBoostL.process(outL)
                outR = bassBoostR.process(outR)
            }

            // Headroom makeup attenuation (0.76f) to prevent clipping
            samples[i] = outL * 0.76f
            samples[i + 1] = outR * 0.76f

            i += 2
        }

        // Update speaker energy telemetry
        val frames = (count / 2).coerceAtLeast(1)
        val decay = 0.82f
        speakerEnergiesRaw[0] = sqrt(blockEnergyFL / frames).coerceIn(0f, 1f)
        speakerEnergiesRaw[1] = sqrt(blockEnergyFC / frames).coerceIn(0f, 1f)
        speakerEnergiesRaw[2] = sqrt(blockEnergyFR / frames).coerceIn(0f, 1f)
        speakerEnergiesRaw[3] = sqrt(blockEnergySL / frames).coerceIn(0f, 1f)
        speakerEnergiesRaw[4] = sqrt(blockEnergySR / frames).coerceIn(0f, 1f)
        speakerEnergiesRaw[5] = sqrt(blockEnergyRL / frames).coerceIn(0f, 1f)
        speakerEnergiesRaw[6] = sqrt(blockEnergyRR / frames).coerceIn(0f, 1f)
        speakerEnergiesRaw[7] = sqrt(blockEnergySub / frames).coerceIn(0f, 1f)

        for (k in 0..7) {
            speakerEnergiesSmooth[k] = (speakerEnergiesSmooth[k] * decay) + (speakerEnergiesRaw[k] * (1f - decay))
        }
        telemetryEnergies = speakerEnergiesSmooth.clone()
    }
}

/**
 * Master Spatial Audio Engine orchestrating 8D Binaural Orbit, 7.1 Virtual Surround,
 * and high-fidelity bit-exact passthrough.
 */
internal class DesktopSpatialAudioEngine(private val channels: Int, val sampleRate: Int) {
    @Volatile var mode: SpatialAudioMode = SpatialAudioMode.OFF

    val applies: Boolean = channels == 2

    val eightD = EightDAudioProcessor(sampleRate)
    val surround71 = VirtualSurround71Processor(sampleRate)

    // Preallocated crossfade buffer for glitchless mode switching
    private var crossfadeBuffer = FloatArray(0)
    private var previousMode: SpatialAudioMode = SpatialAudioMode.OFF
    private var modeCrossfadeFrames = 0
    private val totalCrossfadeFrames = ((sampleRate * 0.080).roundToInt()).coerceAtLeast(1)

    fun reset() {
        eightD.reset()
        surround71.reset()
        previousMode = mode
        modeCrossfadeFrames = 0
    }

    fun process(samples: FloatArray, count: Int) {
        if (!applies || count <= 0) return

        val currentMode = mode
        if (currentMode == SpatialAudioMode.OFF && previousMode == SpatialAudioMode.OFF) {
            // Bit-exact zero-overhead passthrough
            return
        }

        // Handle smooth crossfade when switching modes
        if (currentMode != previousMode && modeCrossfadeFrames == 0) {
            modeCrossfadeFrames = totalCrossfadeFrames
        }

        when (currentMode) {
            SpatialAudioMode.OFF -> {
                // Fading back to pure stereo
                if (modeCrossfadeFrames > 0) {
                    if (crossfadeBuffer.size < count) crossfadeBuffer = FloatArray(count)
                    System.arraycopy(samples, 0, crossfadeBuffer, 0, count)
                    if (previousMode == SpatialAudioMode.EIGHT_D) {
                        eightD.process(crossfadeBuffer, count)
                    } else if (previousMode == SpatialAudioMode.SURROUND_7_1) {
                        surround71.process(crossfadeBuffer, count)
                    }
                    var idx = 0
                    while (idx < count) {
                        val progress = 1.0f - (modeCrossfadeFrames.toFloat() / totalCrossfadeFrames)
                        samples[idx] = samples[idx] * progress + crossfadeBuffer[idx] * (1f - progress)
                        idx++
                        if (idx % 2 == 0 && modeCrossfadeFrames > 0) modeCrossfadeFrames--
                    }
                    if (modeCrossfadeFrames <= 0) previousMode = SpatialAudioMode.OFF
                }
            }
            SpatialAudioMode.EIGHT_D -> {
                eightD.process(samples, count)
                previousMode = SpatialAudioMode.EIGHT_D
                modeCrossfadeFrames = 0
            }
            SpatialAudioMode.SURROUND_7_1 -> {
                surround71.process(samples, count)
                previousMode = SpatialAudioMode.SURROUND_7_1
                modeCrossfadeFrames = 0
            }
        }
    }

    /**
     * Reads the instantaneous telemetry for the 3D Radar UI.
     */
    fun readTelemetry(): SpatialAudioTelemetry {
        val currentMode = mode
        return when (currentMode) {
            SpatialAudioMode.EIGHT_D -> SpatialAudioTelemetry(
                mode = currentMode,
                azimuthDegrees = eightD.telemetryAzimuth,
                orbitRadius = eightD.depth,
                pan = eightD.telemetryPan,
                isBehind = eightD.telemetryBehind,
            )
            SpatialAudioMode.SURROUND_7_1 -> SpatialAudioTelemetry(
                mode = currentMode,
                speakerEnergies = surround71.telemetryEnergies,
            )
            SpatialAudioMode.OFF -> SpatialAudioTelemetry(mode = SpatialAudioMode.OFF)
        }
    }
}
