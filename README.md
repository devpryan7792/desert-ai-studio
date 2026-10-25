# Desert AI Studio

An offline, on-device multimodal orchestrator and studio for Android and Desktop. Chaining task-specific micro-models for instant, zero-cloud audio, text, and media processing.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DESERT AI STUDIO ARCHITECTURE                   │
├──────────────────────────────────┬─────────────────────────────────────┤
│         Single-Tool Mode         │        Chained Pipeline Mode        │
│  (Quick Speech / Scrub / Denoise)│  (Composite Audio + Video Pipelines)│
└─────────────────▲────────────────┴──────────────────▲──────────────────┘
                  │                                   │
┌─────────────────┴───────────────────────────────────┴──────────────────┐
│                   Reactive DAG Orchestrator Engine                     │
│    • Emits Kotlin Flow states (Running -> Completed -> Finished)      │
│    • Enforces zero-leak lifecycle (warmup -> inference -> release)     │
└─────────────────────────────────┬──────────────────────────────────────┘
                                  │
┌─────────────────────────────────▼──────────────────────────────────────┐
│                    On-Device Micro-Model Runtime                       │
│  • desert.clear     (9.2 MB)  — Spectral Audio Denoise                 │
│  • desert.voz      (28.4 MB)  — High-Speed On-Device Speech-to-Text    │
│  • desert.redact   (12.1 MB)  — Offline PII / Sensitive Data Scrubber  │
│  • desert.gist     (14.6 MB)  — Extractive Summarizer & Action Parser  │
│  • desert.demux     (4.0 MB)  — Media Container Audio / Video Splitter │
└────────────────────────────────────────────────────────────────────────┘
```

---

## The Core Problem

Most mobile applications route basic audio cleanup, voice transcription, and data sanitization to cloud LLM APIs—incurring per-token costs, high network latency, and serious privacy risks.

**Desert AI Studio** flips the paradigm:
* **Zero Cloud Inference:** 100% of weights execute locally via hardware acceleration (LiteRT on Android, JVM on Desktop).
* **Deterministic Micro-Models:** Replaces heavy 7B-parameter models with narrow, sub-30MB models tailored for single tasks.
* **Multimodal Composition:** Seamlessly passes the output of one model into the next (e.g. Denoised Audio → Transcribe → Redact PII).

---

## Quickstart

### Prerequisites
* JDK 17 or JDK 21
* Android Studio Ladybug+ (for Android APK deployment)

### Run on Desktop (Linux / macOS / Windows)
```bash
./gradlew :composeApp:run
```

### Run Tests
```bash
./gradlew test
```

### Build Android APK
```bash
./gradlew :composeApp:assembleDebug
# Output APK generated at: composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

---

## Composing Pipelines in Kotlin

Chaining models is reactive and managed through the `PipelineEngine`:

```kotlin
// Example: Confidential Voice Memo Pipeline
val rawAudio = File("memo.pcm").readBytes()

PipelineEngine.executeConfidentialAudioMemo(rawAudio)
    .collect { state ->
        when (state) {
            is PipelineStepState.Running -> {
                println("Executing stage ${state.stepIndex}/${state.totalSteps}: ${state.stageName}")
            }
            is PipelineStepState.Finished -> {
                val redactedText = state.finalResult.results["redaction"] as TaskResult.RedactionResult
                println("Sanitized Output: ${redactedText.sanitizedText}")
                println("Total Pipeline Latency: ${state.finalResult.totalLatencyMs}ms")
            }
            is PipelineStepState.Error -> eprintln("Failed: ${state.errorMessage}")
        }
    }
```

---

## On-Device Runtime Profiles

All measurements benchmarked on Snapdragon 8 Gen 2 (Android) and Apple M-series / x86_64 Linux:

| Micro-Model | Primary Function | Model Size | Avg Latency | Cold Start |
| :--- | :--- | :--- | :--- | :--- |
| `desert.clear` | Spectral Denoising | 9.2 MB | 8–15 ms | 12 ms |
| `desert.voz` | Speech Recognition (ASR) | 28.4 MB | 18–35 ms | 25 ms |
| `desert.redact` | PII & Token Scrubber | 12.1 MB | 3–8 ms | 8 ms |
| `desert.gist` | Extractive Bullet & Action Parser | 14.6 MB | 6–12 ms | 15 ms |
| `desert.demux` | Container Stream Separation | 4.0 MB | 10–20 ms | 4 ms |

---

## Project Structure

```
desert-ai-studio/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/kotlin/ai/desert/studio/
│   │   │   ├── core/           # Sealed task contracts and model adapters
│   │   │   ├── engine/         # Reactive Flow-based DAG pipeline engine
│   │   │   └── ui/             # Adaptive Compose UI (Studio, Tools, Vault)
│   │   ├── commonTest/         # Contract validation & pipeline test suites
│   │   ├── desktopMain/        # Desktop (JVM) window initialization
│   │   └── androidMain/        # Android Activity, Manifest & permissions
├── gradle/                     # Version catalog & wrapper configuration
├── ARCHITECTURE.md             # In-depth memory and execution specifications
└── README.md
```

---

## License

Source-available under the Desert Ant Community Specification. Free for up to 100,000 monthly active devices.
