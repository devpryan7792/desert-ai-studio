# Desert AI Studio — Technical Architecture Specification

## 1. Design Invariants

Desert AI Studio enforces three strict operational constraints:
1. **Zero-Egress Isolation:** Network permissions (`android.permission.INTERNET`) are deliberately omitted from the Android manifest. The binary has zero external socket capabilities.
2. **Deterministic Memory Ceiling:** At no point during multi-stage execution does peak RAM exceed 65 MB. Models are lazily allocated and released once downstream stages consume the intermediate tensor/buffer.
3. **Non-Blocking Reactive Flows:** All model inference calls execute off the main Android Looper / Desktop Swing event queue via `Dispatchers.Default`.

---

## 2. Multi-Stage Pipeline Lifecycle

Each pipeline execution passes through a standard state machine emitted over a Kotlin `Flow<PipelineStepState>`:

```
[Trigger] 
   │
   ▼
[Stage 1 Warmup] ──► [Stage 1 Inference] ──► [Intermediate Buffer]
                                                    │
   ┌────────────────────────────────────────────────┘
   ▼
[Stage 2 Warmup] ──► [Stage 2 Inference] ──► [Stage 1 Released]
                                                    │
   ┌────────────────────────────────────────────────┘
   ▼
[Stage N Inference] ──► [PipelineExecutionResult] ──► [Local Vault Persist]
```

### Memory De-allocation Contract
When a downstream stage initializes:
* If accumulated allocated model memory exceeds 50 MB, `ModelRegistry` invokes `release()` on previous upstream adapters whose intermediate buffers are already materialized into immutable JVM/native arrays.

---

## 3. Data Contracts & Schema Boundaries

### `ModelTask` Interface
```kotlin
sealed interface ModelTask {
    data class AudioDenoise(val audioBytes: ByteArray, val sampleRate: Int, val aggressiveDenoise: Boolean) : ModelTask
    data class SpeechToText(val audioBytes: ByteArray, val language: String?, val timestampsEnabled: Boolean) : ModelTask
    data class PiiMask(val text: String, val targets: Set<PiiType>) : ModelTask
    data class Summarize(val text: String, val maxPoints: Int, val extractActionItems: Boolean) : ModelTask
    data class MediaSplit(val mediaBytes: ByteArray, val fileName: String) : ModelTask
}
```

### `TaskResult` Contract
Each task result records its own hardware inference time (`latencyMs`) to provide deterministic profiling without external APM tooling.

---

## 4. Platform Runtime Bindings

* **Android (API 24+):**
  * Uses Google's LiteRT runtime (formerly TensorFlow Lite) and Android Neural Networks API (NNAPI) delegates targeting GPU/NPU.
* **Desktop (JVM on Linux, macOS, Windows):**
  * Uses direct native bindings or SIMD-accelerated Java Vector API operations.
