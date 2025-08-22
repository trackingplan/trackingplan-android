# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build entire project
./gradlew build

# Build and publish to local Maven for testing
./gradlew cleanBuildPublishLocal

# Build individual modules
./gradlew :sdk:build
./gradlew :adapter:build
./gradlew :junit-tools:build

# Publish to Sonatype staging
./gradlew cleanBuildPublishSonatype
```

## Testing

```bash
# Run all unit tests
./gradlew test

# Run SDK unit tests specifically
./gradlew :sdk:testDebugUnitTest

# Run instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single test class
./gradlew :sdk:testDebugUnitTest --tests "com.trackingplan.client.sdk.YourTestClass"

# Run a single test method
./gradlew :sdk:testDebugUnitTest --tests "com.trackingplan.client.sdk.YourTestClass.yourTestMethod"
```

## Lint

```bash
# Run lint checks
./gradlew lint

# Run lint on SDK module
./gradlew :sdk:lintDebug
```

## Architecture

### Module Structure
- **sdk/**: Core Android SDK library with interception and delivery logic
- **adapter/**: Gradle plugin for compile-time bytecode transformation using ASM
- **junit-tools/**: Testing utilities for instrumented regression tests
- **examples/**: Integration examples for various analytics platforms

### SDK Initialization

The SDK requires two setup steps:

1. **Build Configuration** - Apply the Gradle plugin for bytecode transformation:
   ```gradle
   // Project-level build.gradle
   classpath "com.trackingplan.client:adapter:1.10.0"

   // Module-level build.gradle
   plugins {
       id 'com.trackingplan.client'
   }
   dependencies {
       implementation 'com.trackingplan.client:sdk:1.10.0'
   }
   ```

2. **Runtime Initialization** - Initialize in Application.onCreate():
   ```java
   Trackingplan.init("YOUR_TP_ID")
       .environment("development")  // Optional
       .sourceAlias("my_app")      // Optional
       .enableDebug()               // Optional
       .dryRun()                    // Optional
       .tags(tags)                  // Optional
       .customDomains(domains)      // Optional
       .start(this)
   ```

### Core Architecture

The SDK uses bytecode transformation for all interceptions:

1. **Gradle Plugin (adapter/)**:
   - Performs compile-time bytecode transformation using ASM
   - Transforms all HTTP clients (OkHttp3, HttpURLConnection)
   - Directly targets Firebase Analytics and Google Tag Manager DataLayer public API methods
   - Can be disabled via `trackingplan.enableSdk=false` in gradle.properties

2. **Runtime SDK (sdk/)**:
   - `Trackingplan`: Main entry point using Builder pattern
   - `HttpInstrumentRequestBuilder`: Base class for handling intercepted HTTP requests
   - `TrackingplanSession`: Session lifecycle and sampling management
   - `BatchSender`: Batch delivery via WorkManager or TaskRunner

3. **Auto-initialization**:
   - Uses AndroidX Startup library via `TrackingplanInitializer`
   - Can be disabled in AndroidManifest.xml by removing the provider

### Testing Integration

For instrumented tests using junit-tools:
```java
@Rule
public TrackingplanRule trackingplanRule =
    TrackingplanJUnit.init("YOUR_TP_ID", "YOUR_ENVIRONMENT")
        .tags(tags)
        .newRule();
```

Use `TrackingplanJUnitRunner` as test runner or call `Trackingplan.enableInstrumentedTestMode()` in custom runners.

## Development Notes

- Minimum SDK: 24 (Android 7.0)
- Target SDK: 33 (Android 13)
- Java version: 11
- Uses AndroidX libraries
- Publishing group: `com.trackingplan.client`
