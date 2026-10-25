package ai.desert.studio.ui

import ai.desert.studio.core.TaskResult
import ai.desert.studio.engine.PipelineEngine
import ai.desert.studio.engine.PipelineExecutionResult
import ai.desert.studio.engine.PipelineStepState
import ai.desert.studio.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun PipelineStudioView(
    onSaveToVault: (String, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedPipelineId by remember { mutableStateOf("confidential_memo") }
    var currentStepState by remember { mutableStateOf<PipelineStepState?>(null) }
    var completedResult by remember { mutableStateOf<PipelineExecutionResult?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Multimodal Pipeline Studio",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Chain multiple task-specific micro-models together. Run composite audio + video + text tasks locally.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Pipeline Preset Selector Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PipelinePresetCard(
                title = "Confidential Audio Memo",
                description = "Raw Audio → Clear (Denoise) → Voz (ASR) → Redact (PII Masker)",
                badge = "3 Models • Audio + Text",
                isSelected = selectedPipelineId == "confidential_memo",
                onClick = {
                    selectedPipelineId = "confidential_memo"
                    completedResult = null
                    currentStepState = null
                }
            )

            PipelinePresetCard(
                title = "Video Note & Briefing",
                description = "Input Video → Demux → Clear Audio → Voz Subtitles → Gist Action Points",
                badge = "4 Models • Video + Audio + Text",
                isSelected = selectedPipelineId == "video_briefing",
                onClick = {
                    selectedPipelineId = "video_briefing"
                    completedResult = null
                    currentStepState = null
                }
            )
        }

        // Action Trigger Button
        Button(
            onClick = {
                isRunning = true
                completedResult = null
                coroutineScope.launch {
                    val flow = if (selectedPipelineId == "confidential_memo") {
                        val sampleAudio = "Meeting recording: John Doe discussed project budget of $50,000. Contact john@corp.internal or 555-0144.".toByteArray()
                        PipelineEngine.executeConfidentialAudioMemo(sampleAudio)
                    } else {
                        val sampleMedia = ByteArray(16000 * 5) { (it % 120).toByte() }
                        PipelineEngine.executeVideoBriefing(sampleMedia, "product_demo_recording.mp4")
                    }

                    flow.collect { state ->
                        currentStepState = state
                        if (state is PipelineStepState.Finished) {
                            completedResult = state.finalResult
                            isRunning = false

                            val exportText = buildString {
                                appendLine("# ${state.finalResult.pipelineName}")
                                appendLine("Total Latency: ${state.finalResult.totalLatencyMs}ms | Memory Peak: ${state.finalResult.peakMemoryBytes / 1024 / 1024}MB\n")
                                state.finalResult.results.forEach { (stage, res) ->
                                    appendLine("## Stage: $stage")
                                    when (res) {
                                        is TaskResult.RedactionResult -> appendLine(res.sanitizedText)
                                        is TaskResult.SummaryResult -> appendLine(res.summaryBullets.joinToString("\n"))
                                        is TaskResult.TranscriptionResult -> appendLine(res.text)
                                        else -> appendLine("Stage completed in ${res.latencyMs}ms")
                                    }
                                    appendLine()
                                }
                            }
                            onSaveToVault(state.finalResult.pipelineName, exportText)
                        } else if (state is PipelineStepState.Error) {
                            isRunning = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            enabled = !isRunning
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Executing Pipeline...", fontWeight = FontWeight.Bold)
            } else {
                Text("Run Multimodal Pipeline", fontWeight = FontWeight.Bold)
            }
        }

        // Live Execution Stepper / Progress
        currentStepState?.let { state ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pipeline Orchestration Flow",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step ${state.stepIndex} of ${state.totalSteps}",
                            fontSize = 12.sp,
                            color = AccentSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { state.stepIndex.toFloat() / state.totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentPrimary,
                        trackColor = SurfaceElevated,
                    )

                    when (state) {
                        is PipelineStepState.Running -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentSecondary)
                                )
                                Text(
                                    text = "${state.stageName}: ${state.details}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        is PipelineStepState.StepCompleted -> {
                            Text(
                                text = "✓ Finished ${state.stageName} in ${state.result.latencyMs}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is PipelineStepState.Finished -> {
                            Text(
                                text = "✓ All stages completed successfully in ${state.finalResult.totalLatencyMs}ms (Peak RAM: ${state.finalResult.peakMemoryBytes / 1024 / 1024} MB)",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        is PipelineStepState.Error -> {
                            Text(
                                text = "✗ Error: ${state.errorMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningRedaction
                            )
                        }
                    }
                }
            }
        }

        // Final Composite Output Display
        completedResult?.let { result ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Composite Output Artifact",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceDark
                        ) {
                            Text(
                                text = "${result.totalLatencyMs}ms • Auto-saved to Vault",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    result.results.forEach { (stageKey, taskResult) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stageKey.uppercase(),
                                fontSize = 11.sp,
                                color = AccentSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            when (taskResult) {
                                is TaskResult.RedactionResult -> {
                                    Text(
                                        text = taskResult.sanitizedText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Redactions applied: ${taskResult.redactionCount} items masked",
                                        fontSize = 11.sp,
                                        color = WarningRedaction
                                    )
                                }
                                is TaskResult.TranscriptionResult -> {
                                    Text(
                                        text = taskResult.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                                is TaskResult.SummaryResult -> {
                                    taskResult.summaryBullets.forEach { bullet ->
                                        Text(text = bullet, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    }
                                    if (taskResult.actionItems.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        taskResult.actionItems.forEach { act ->
                                            Text(text = act, fontSize = 12.sp, color = AccentPrimary)
                                        }
                                    }
                                }
                                is TaskResult.DenoisedAudioResult -> {
                                    Text(
                                        text = "Audio stream denoised (+${taskResult.estimatedSnrImprovementDb} dB SNR)",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                is TaskResult.MediaSplitResult -> {
                                    Text(
                                        text = "Demuxed ${taskResult.extractedFrameCount} keyframes and audio track (${taskResult.mediaDurationSec}s)",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PipelinePresetCard(
    title: String,
    description: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceElevated else SurfaceDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isSelected) AccentPrimary else BorderDark,
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) AccentPrimary else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) AccentPrimary.copy(alpha = 0.15f) else SurfaceElevated
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = if (isSelected) AccentPrimary else TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
