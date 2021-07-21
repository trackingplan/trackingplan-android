# Trackingplan for Android

## Get started

### Requirements
- Android Studio
- Android SDK
- Android emulator

### Build Trackingplan Adapter (gradle plugin)

```console
$ cd _experimental/d3ce1t/android-client/adapter-plugin
$ ./gradlew clean
$ ./gradlew build
$ ./gradlew publishToMavenLocal
```

### Test

- Run Android Studio
- Open existing project located at `_experimental/d3ce1t/android-client/url-connection-app-example`
- Run application in emulator
- Observe logs in Run tab

### Adding Trackingplan Adapter to another project

- Modify build.gradle at project level (see comments)

    ```gradle

    buildscript {
        repositories {
            google()
            mavenCentral()
            mavenLocal() // <-- Add maven local
        }
        dependencies {
            classpath "com.android.tools.build:gradle:4.2.1"
            classpath "com.trackingplan.adapter:plugin:0.1" // <-- Add adapter plugin
        }
    }

    allprojects {
        repositories {
            google()
            mavenCentral()
            mavenLocal() // <-- Add maven local
        }
    }

    task clean(type: Delete) {
        delete rootProject.buildDir
    }
    ```

- Modify build.gradle at module level (see comments)

    ```gradle
    plugins {
        id 'com.android.application'
        id 'com.trackingplan.adapter' // <-- Apply Trackingplan Adapter plugin
    }

    android {
        compileSdkVersion 30
        buildToolsVersion "30.0.3"

        defaultConfig {
            applicationId "com.example.urlconnectionappexample"
            minSdkVersion 16
            targetSdkVersion 30
            versionCode 1
            versionName "1.0"

            testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes {
            release {
                minifyEnabled false
                proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            }
        }
        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
    }

    dependencies {
        implementation 'androidx.appcompat:appcompat:1.3.0'
        implementation 'com.google.android.material:material:1.3.0'
        implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
        implementation 'com.trackingplan.adapter:sdk:0.1' // <-- Add SDK dependency
        implementation 'commons-io:commons-io:2.9.0'
        testImplementation 'junit:junit:4.+'
        androidTestImplementation 'androidx.test.ext:junit:1.1.2'
        androidTestImplementation 'androidx.test.espresso:espresso-core:3.3.0'
    }
    ```

- Modify your MainActivity.java

    ```java
    // Import TrackingplanSdk in the top

    import com.trackingplan.adapter.sdk.TrackingplanSdk;

    // Initialize it in the onCreate method

    try {
        TrackingplanSdk.init("TPID").withDebugEnabled();
    } catch (TrackingplanAlreadyInitializedException e) {
        Log.e("MY_APP", e.getMessage());
    }
        
    ```
