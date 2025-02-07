# OkHttp Kotlin Example

A sample Android application demonstrating the integration of Trackingplan SDK with OkHttp3 in Kotlin. This example shows how to monitor and analyze HTTP traffic in your Android apps using Trackingplan.

## Example Usage

The `MainActivity` demonstrates a basic HTTP GET request using OkHttp:

- Creates an OkHttp client
- Makes a GET request to GitHub's raw content to fetch OkHttp's README
- Prints the response to the console
- The request is automatically intercepted by Trackingplan but ignored since github.com is not a supported analytics destination, as shown in the logs:
  ```
  V  Request intercepted: HttpRequest{method='GET', failed='No', responseCode='200', provider='', intercepted by='okhttp', payloadSize='0', url='https://raw.githubusercontent.com/square/okhttp/master/README.md', created_at='1738934548981', context='{}', headers='{}'}
  V  Request ignored. Doesn't belong to a supported destination
  ```

## How this project was set up?

### 1. Add Trackingplan dependencies to Gradle Version Catalog

In `gradle/libs.versions.toml`, the following dependencies were added:

```toml
[versions]
trackingplan = "1.10.0"

[libraries]
trackingplan = { module = "com.trackingplan.client:sdk", version.ref = "trackingplan"}

[plugins]
trackingplan = { id = "com.trackingplan.client", version.ref = "trackingplan" }
```

This defines both the Trackingplan library dependency and Gradle plugin that will be used.

### 2. Include the Trackingplan plugin

The root `build.gradle.kts` was modified to include the Trackingplan plugin:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.trackingplan) apply false
}
```

### 3. Apply plugin to your app modules and Trackingplan runtime dependency

The app module's `build.gradle.kts` applies the Trackingplan plugin and adds the SDK dependency:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.trackingplan)
}

dependencies {
    implementation(libs.trackingplan)
    // ... other dependencies
}
```

### 4. Initialize Trackingplan when your app stats up

The `ExampleApplication` class initializes Trackingplan when the app starts:

```kotlin
class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Trackingplan.init("YOUR_TP_ID")
            .enableDebug()
            .dryRun()
            .start(this)
    }
}
```

This sets up Trackingplan with debug mode and dry run enabled for testing.

## Additional Resources
- [Trackingplan Documentation](https://docs.trackingplan.com)
- [OkHttp Official Documentation](https://square.github.io/okhttp/)
