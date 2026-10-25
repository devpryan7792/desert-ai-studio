package ai.desert.studio

import ai.desert.studio.core.*
import ai.desert.studio.engine.PipelineEngine
import ai.desert.studio.engine.PipelineStepState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipelineEngineTest {

    @Test
    fun testPiiRedactionMasksAllSensitiveFields() = runBlocking {
        val adapter = RedactPiiAdapter()
        val text = "Contact John Doe at john.doe@secure.internal or call 415-555-0199 with card 4111-2222-3333-4444."
        val result = adapter.execute(ModelTask.PiiMask(text))

        assertFalse(result.sanitizedText.contains("415-555-0199"), "Phone number was not redacted")
        assertFalse(result.sanitizedText.contains("john.doe@secure.internal"), "Email was not redacted")
        assertFalse(result.sanitizedText.contains("4111-2222-3333-4444"), "Card was not redacted")
        assertFalse(result.sanitizedText.contains("John Doe"), "Name was not redacted")
        assertTrue(result.redactionCount >= 4, "Expected at least 4 redactions, got ${result.redactionCount}")
        assertTrue(result.latencyMs >= 0)
    }

    @Test
    fun testGistSummaryExtractsKeyActionItems() = runBlocking {
        val adapter = GistSummaryAdapter()
        val text = "Team sync on mobile models. We confirmed the release timeline. Please email john for the security review. Budget allocated is within limits."
        val result = adapter.execute(ModelTask.Summarize(text, maxPoints = 3))

        assertTrue(result.summaryBullets.isNotEmpty())
        assertTrue(result.actionItems.any { it.contains("email", ignoreCase = true) || it.contains("confirmed", ignoreCase = true) })
        assertTrue(result.topicTags.isNotEmpty())
    }

    @Test
    fun testClearAudioDenoiseSmoothsBuffer() = runBlocking {
        val adapter = ClearAudioAdapter()
        val noisyBuffer = ByteArray(500) { (it * 7 % 127).toByte() }
        val result = adapter.execute(ModelTask.AudioDenoise(noisyBuffer, aggressiveDenoise = true))

        assertEquals(500, result.cleanedAudioBytes.size)
        assertTrue(result.estimatedSnrImprovementDb > 10.0f)
    }

    @Test
    fun testConfidentialAudioMemoPipelineFlow() = runBlocking {
        val dummyAudio = "Meeting recording test with John Doe".toByteArray()
        val states = PipelineEngine.executeConfidentialAudioMemo(dummyAudio).toList()

        assertTrue(states.any { it is PipelineStepState.Running })
        assertTrue(states.any { it is PipelineStepState.StepCompleted })
        val finalState = states.last()
        assertTrue(finalState is PipelineStepState.Finished, "Pipeline did not finish successfully")
        assertEquals(3, finalState.finalResult.results.size)
        assertTrue(finalState.finalResult.totalLatencyMs >= 0)
    }
}
