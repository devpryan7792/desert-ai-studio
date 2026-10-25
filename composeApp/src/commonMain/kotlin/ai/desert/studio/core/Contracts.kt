package ai.desert.studio.core

import kotlinx.serialization.Serializable

enum class PiiType {
    NAMES,
    PHONE_NUMBERS,
    EMAILS,
    CREDIT_CARDS,
    LOCATIONS,
    ALL
}

sealed interface ModelTask {
    data class AudioDenoise(
        val audioBytes: ByteArray,
        val sampleRate: Int = 16000,
        val aggressiveDenoise: Boolean = true
    ) : ModelTask {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AudioDenoise) return false
            return audioBytes.contentEquals(other.audioBytes) && sampleRate == other.sampleRate
        }
        override fun hashCode(): Int = audioBytes.contentHashCode() * 31 + sampleRate
    }

    data class SpeechToText(
        val audioBytes: ByteArray,
        val language: String? = null,
        val timestampsEnabled: Boolean = false
    ) : ModelTask {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SpeechToText) return false
            return audioBytes.contentEquals(other.audioBytes) && language == other.language
        }
        override fun hashCode(): Int = audioBytes.contentHashCode() * 31 + (language?.hashCode() ?: 0)
    }

    data class PiiMask(
        val text: String,
        val targets: Set<PiiType> = setOf(PiiType.ALL)
    ) : ModelTask

    data class Summarize(
        val text: String,
        val maxPoints: Int = 4,
        val extractActionItems: Boolean = true
    ) : ModelTask

    data class MediaSplit(
        val mediaBytes: ByteArray,
        val fileName: String
    ) : ModelTask {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MediaSplit) return false
            return mediaBytes.contentEquals(other.mediaBytes) && fileName == other.fileName
        }
        override fun hashCode(): Int = mediaBytes.contentHashCode() * 31 + fileName.hashCode()
    }
}

sealed interface TaskResult {
    val latencyMs: Long

    data class DenoisedAudioResult(
        val cleanedAudioBytes: ByteArray,
        val sampleRate: Int,
        val estimatedSnrImprovementDb: Float,
        override val latencyMs: Long
    ) : TaskResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DenoisedAudioResult) return false
            return cleanedAudioBytes.contentEquals(other.cleanedAudioBytes) && latencyMs == other.latencyMs
        }
        override fun hashCode(): Int = cleanedAudioBytes.contentHashCode() * 31 + latencyMs.hashCode()
    }

    data class TranscriptionResult(
        val text: String,
        val confidenceScore: Float,
        val detectedLanguage: String,
        override val latencyMs: Long
    ) : TaskResult

    data class RedactionResult(
        val originalText: String,
        val sanitizedText: String,
        val redactionCount: Int,
        val redactedCategories: List<String>,
        override val latencyMs: Long
    ) : TaskResult

    data class SummaryResult(
        val summaryBullets: List<String>,
        val actionItems: List<String>,
        val topicTags: List<String>,
        override val latencyMs: Long
    ) : TaskResult

    data class MediaSplitResult(
        val extractedAudioBytes: ByteArray,
        val extractedFrameCount: Int,
        val mediaDurationSec: Float,
        override val latencyMs: Long
    ) : TaskResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MediaSplitResult) return false
            return extractedAudioBytes.contentEquals(other.extractedAudioBytes) && latencyMs == other.latencyMs
        }
        override fun hashCode(): Int = extractedAudioBytes.contentHashCode() * 31 + latencyMs.hashCode()
    }
}

interface LocalModelAdapter<T : ModelTask, R : TaskResult> {
    val modelId: String
    val displayName: String
    val memoryFootprintBytes: Long
    val isLoaded: Boolean

    suspend fun warmup()
    suspend fun execute(task: T): R
    fun release()
}
