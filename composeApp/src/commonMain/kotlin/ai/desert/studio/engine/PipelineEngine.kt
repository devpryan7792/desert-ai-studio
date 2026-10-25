package ai.desert.studio.engine

import ai.desert.studio.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface PipelineStepState {
    val stepIndex: Int
    val totalSteps: Int
    val stageName: String

    data class Running(
        override val stepIndex: Int,
        override val totalSteps: Int,
        override val stageName: String,
        val details: String
    ) : PipelineStepState

    data class StepCompleted(
        override val stepIndex: Int,
        override val totalSteps: Int,
        override val stageName: String,
        val result: TaskResult
    ) : PipelineStepState

    data class Finished(
        override val totalSteps: Int,
        val finalResult: PipelineExecutionResult
    ) : PipelineStepState {
        override val stepIndex: Int = totalSteps
        override val stageName: String = "Complete"
    }

    data class Error(
        override val stepIndex: Int,
        override val totalSteps: Int,
        override val stageName: String,
        val errorMessage: String
    ) : PipelineStepState
}

data class PipelineExecutionResult(
    val pipelineId: String,
    val pipelineName: String,
    val totalLatencyMs: Long,
    val peakMemoryBytes: Long,
    val results: Map<String, TaskResult>
)

object PipelineEngine {

    fun executeConfidentialAudioMemo(
        rawAudioBytes: ByteArray,
        aggressiveDenoise: Boolean = true
    ): Flow<PipelineStepState> = flow {
        val totalSteps = 3
        val results = mutableMapOf<String, TaskResult>()
        val startTotal = System.currentTimeMillis()

        try {
            // Stage 1: Denoise
            emit(PipelineStepState.Running(1, totalSteps, "Clear (Denoise)", "Cleaning background acoustic noise..."))
            val clearResult = ModelRegistry.clearAdapter.execute(
                ModelTask.AudioDenoise(rawAudioBytes, aggressiveDenoise = aggressiveDenoise)
            )
            results["denoise"] = clearResult
            emit(PipelineStepState.StepCompleted(1, totalSteps, "Clear (Denoise)", clearResult))

            // Stage 2: Transcribe
            emit(PipelineStepState.Running(2, totalSteps, "Voz (Speech-to-Text)", "Decoding audio phonemes to text..."))
            val vozResult = ModelRegistry.vozAdapter.execute(
                ModelTask.SpeechToText(clearResult.cleanedAudioBytes)
            )
            results["transcription"] = vozResult
            emit(PipelineStepState.StepCompleted(2, totalSteps, "Voz (Speech-to-Text)", vozResult))

            // Stage 3: Redact PII
            emit(PipelineStepState.Running(3, totalSteps, "Redact (PII Masker)", "Scanning for names, emails, phones, and cards..."))
            val redactResult = ModelRegistry.redactAdapter.execute(
                ModelTask.PiiMask(vozResult.text)
            )
            results["redaction"] = redactResult
            emit(PipelineStepState.StepCompleted(3, totalSteps, "Redact (PII Masker)", redactResult))

            val totalLatency = System.currentTimeMillis() - startTotal
            val peakMemory = ModelRegistry.getTotalAllocatedMemoryBytes()

            emit(PipelineStepState.Finished(
                totalSteps = totalSteps,
                finalResult = PipelineExecutionResult(
                    pipelineId = "confidential_memo",
                    pipelineName = "Confidential Audio Memo",
                    totalLatencyMs = totalLatency,
                    peakMemoryBytes = peakMemory,
                    results = results
                )
            ))
        } catch (e: Exception) {
            emit(PipelineStepState.Error(
                stepIndex = results.size + 1,
                totalSteps = totalSteps,
                stageName = "Pipeline Failure",
                errorMessage = e.message ?: "Unknown runtime error occurred"
            ))
        }
    }

    fun executeVideoBriefing(
        mediaBytes: ByteArray,
        fileName: String
    ): Flow<PipelineStepState> = flow {
        val totalSteps = 4
        val results = mutableMapOf<String, TaskResult>()
        val startTotal = System.currentTimeMillis()

        try {
            // Stage 1: Media Demux
            emit(PipelineStepState.Running(1, totalSteps, "Demux Media", "Separating audio stream and indexing frames..."))
            val demuxResult = ModelRegistry.mediaSplitter.execute(
                ModelTask.MediaSplit(mediaBytes, fileName)
            )
            results["demux"] = demuxResult
            emit(PipelineStepState.StepCompleted(1, totalSteps, "Demux Media", demuxResult))

            // Stage 2: Audio Denoise
            emit(PipelineStepState.Running(2, totalSteps, "Clear Audio", "Stripping background noise from video audio..."))
            val clearResult = ModelRegistry.clearAdapter.execute(
                ModelTask.AudioDenoise(demuxResult.extractedAudioBytes)
            )
            results["clear"] = clearResult
            emit(PipelineStepState.StepCompleted(2, totalSteps, "Clear Audio", clearResult))

            // Stage 3: ASR Transcription
            emit(PipelineStepState.Running(3, totalSteps, "Voz Transcribe", "Generating accurate subtitle transcript..."))
            val vozResult = ModelRegistry.vozAdapter.execute(
                ModelTask.SpeechToText(clearResult.cleanedAudioBytes)
            )
            results["transcription"] = vozResult
            emit(PipelineStepState.StepCompleted(3, totalSteps, "Voz Transcribe", vozResult))

            // Stage 4: Extractive Gist & Action items
            emit(PipelineStepState.Running(4, totalSteps, "Gist Summary", "Extracting action items, tags, and summary..."))
            val gistResult = ModelRegistry.gistAdapter.execute(
                ModelTask.Summarize(vozResult.text)
            )
            results["summary"] = gistResult
            emit(PipelineStepState.StepCompleted(4, totalSteps, "Gist Summary", gistResult))

            val totalLatency = System.currentTimeMillis() - startTotal
            emit(PipelineStepState.Finished(
                totalSteps = totalSteps,
                finalResult = PipelineExecutionResult(
                    pipelineId = "video_briefing",
                    pipelineName = "Video Note & Briefing",
                    totalLatencyMs = totalLatency,
                    peakMemoryBytes = ModelRegistry.getTotalAllocatedMemoryBytes(),
                    results = results
                )
            ))
        } catch (e: Exception) {
            emit(PipelineStepState.Error(
                stepIndex = results.size + 1,
                totalSteps = totalSteps,
                stageName = "Video Pipeline Failure",
                errorMessage = e.message ?: "Failed processing video pipeline"
            ))
        }
    }
}
