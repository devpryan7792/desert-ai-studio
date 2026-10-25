package ai.desert.studio.ui

import ai.desert.studio.core.*
import ai.desert.studio.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun QuickToolsView(
    onSaveToVault: (String, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTool by remember { mutableStateOf("redact") }
    var inputText by remember {
        mutableStateOf("Call John Doe at 415-555-0199 or email john.doe@company.com with card 4111-2222-3333-4444.")
    }
    var executionResult by remember { mutableStateOf<String?>(null) }
    var executionLatencyMs by remember { mutableStateOf<Long?>(null) }
    var isExecuting by remember { mutableStateOf(false) }

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
                text = "Single-Task Micro-Models",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Invoke individual local models offline. Zero cloud calls, sub-25ms latency.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Tool Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "redact" to "Redact (PII)",
                "voz" to "Voz (Speech)",
                "clear" to "Clear (Audio)",
                "gist" to "Gist (Summary)"
            ).forEach { (id, label) ->
                val selected = selectedTool == id
                Button(
                    onClick = {
                        selectedTool = id
                        executionResult = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) AccentPrimary else Color.Transparent,
                        contentColor = if (selected) Color.Black else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Input Card
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
                        text = when (selectedTool) {
                            "redact" -> "Input Text to Sanitize"
                            "gist" -> "Input Text to Summarize"
                            "voz" -> "Simulated Voice Note Audio"
                            else -> "Audio Stream Sample"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when (selectedTool) {
                            "redact" -> "12 MB • Regex + Token Mask"
                            "gist" -> "14.6 MB • Extractive"
                            "voz" -> "28.4 MB • Offline ASR"
                            else -> "9.2 MB • Spectral Denoise"
                        },
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                if (selectedTool == "redact" || selectedTool == "gist") {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        placeholder = { Text("Paste text or notes...", color = TextMuted) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Synthetic 16kHz PCM Buffer Loaded (2.4 sec)",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Ready for local inference",
                                color = AccentPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        isExecuting = true
                        coroutineScope.launch {
                            when (selectedTool) {
                                "redact" -> {
                                    val res = ModelRegistry.redactAdapter.execute(ModelTask.PiiMask(inputText))
                                    executionResult = "Sanitized Text:\n${res.sanitizedText}\n\nMasked Items (${res.redactionCount}):\n${res.redactedCategories.joinToString("\n")}"
                                    executionLatencyMs = res.latencyMs
                                    onSaveToVault("Redacted Note", res.sanitizedText)
                                }
                                "gist" -> {
                                    val res = ModelRegistry.gistAdapter.execute(ModelTask.Summarize(inputText))
                                    executionResult = "Key Points:\n${res.summaryBullets.joinToString("\n")}\n\nAction Items:\n${res.actionItems.joinToString("\n")}\n\nTags: ${res.topicTags.joinToString(", ")}"
                                    executionLatencyMs = res.latencyMs
                                    onSaveToVault("Gist Summary", executionResult!!)
                                }
                                "voz" -> {
                                    val dummyAudio = "Sample voice memo containing discussion on on-device models.".toByteArray()
                                    val res = ModelRegistry.vozAdapter.execute(ModelTask.SpeechToText(dummyAudio))
                                    executionResult = "Transcript (${res.detectedLanguage}, ${(res.confidenceScore * 100).toInt()}% conf):\n${res.text}"
                                    executionLatencyMs = res.latencyMs
                                    onSaveToVault("Audio Transcript", res.text)
                                }
                                "clear" -> {
                                    val dummyAudio = ByteArray(16000 * 2) { (it % 128).toByte() }
                                    val res = ModelRegistry.clearAdapter.execute(ModelTask.AudioDenoise(dummyAudio))
                                    executionResult = "Cleaned ${res.cleanedAudioBytes.size} bytes.\nEstimated SNR Improvement: +${res.estimatedSnrImprovementDb} dB\nSample Rate: ${res.sampleRate} Hz"
                                    executionLatencyMs = res.latencyMs
                                }
                            }
                            isExecuting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isExecuting
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Execute Local Model", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Result Card
        if (executionResult != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model Output",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        executionLatencyMs?.let {
                            Text(
                                text = "Latency: ${it}ms • Offline",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = executionResult!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
