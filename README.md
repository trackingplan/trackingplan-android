# Trackingplan for Android SDK

## Table of Contents

- [Trackingplan](#trackingplan)  
- [Add Trackingplan to your Android app](#add-trackingplan-to-your-android-app)
- [Advanced options](#advanced-options)
- [Disable Trackingplan](#disable-trackingplan)
- [Building from source code](#building-from-source-code)
- [Need help?](#need-help)
- [Learn more](#learn-more)

## Trackingplan

With Trackingplan for Android you can make sure that your tracking is going as you planned without changing your current analytics stack or code. 

Trackingplan will monitor traffic between your app and data destinations and automatically detect any changes in your analytics implementation and warn you about inconsistencies like hit drops, missing properties, rogue events, and more.

<img src="https://user-images.githubusercontent.com/47759/125635223-8298353f-168f-4e31-a881-bc1cb7b21b7e.png" width="400" />

Trackingplan is currently available for [Web](https://github.com/trackingplan/trackingplan.js), [iOS](https://github.com/trackingplan/trackingplan-ios) and [Android](https://github.com/trackingplan/trackingplan-android). More clients will come soon.

Please request your ```TrackingplanId``` at <a href='https://www.trackingplan.com'>trackingplan.com</a> or write us to team@trackingplan.com.

## Add Trackingplan to your Android app

The recommended way to install Trackingplan for Android is by using Android Studio. Please, make sure your project targets API level 21 (Lollipop) or later.

First, add the Trackingplan dependency using Android Studio, like so:

In Android Studio, expand the `Gradle Scripts` section

![image](https://user-images.githubusercontent.com/3706385/126515536-1d2e2775-d3ae-4d80-be15-3127328db89e.png)

Select the `project-level build.gradle` file and add `com.trackingplan.client:adapter:1.2.3` as a classpath dependency to the dependencies section:

```gradle
dependencies {   
    // ...
    classpath "com.trackingplan.client:adapter:1.2.3"
    // ...
}
```

After that, select the `module-level build.gradle` file and modify it as indicated below:

- Add `id 'com.trackingplan.client'` to the plugins section.
```gradle 
plugins {
    // ...
    id 'com.trackingplan.client'
    // ...
}
```

- Add `implementation 'com.trackingplan.client:sdk:1.2.3'` to the dependencies section.
```gradle
dependencies {
    // ...
    implementation 'com.trackingplan.client:sdk:1.2.3'
    // ...
}
```

Then in the `onCreate` method of your Application's Main Activity, set up the SDK like so:

```java
Trackingplan.init("YOUR_TP_ID").start(this)
```

And of course, import the SDK:

```java
import com.trackingplan.client.sdk.Trackingplan;
```

All set!

## Advanced options

Trackingplan for Android supports the following `advanced options` during its initialization:


| Parameter | Description | Default |
| ----------|-------------|---------------|
| `environment(value)` | Allows to isolate the data between production and testing environments. | `PRODUCTION` |
| `sourceAlias(value)` | Allows to differentiate between sources. | `android` |
| `customDomains(map)` | Allows to extend the list of monitored domains. Any request made to these domains will also be forwarded to Trackingplan. The `map argument` must be a `key-value` with the domain to be looked for and the alias you want to use for that analytics domain. | `empty map`            |
| `enableDebug()`         | Enables debug mode. Prints debug information in the console. | `disabled` | 
| `dryRun()` | Enables dry run mode. Do not send intercepted requests to Trackingplan. | `disabled` |

### Example

```java
Trackingplan.init("YOUR_TP_ID")
    .environment("development")
    .sourceAlias("my_application")
    .customDomains(new HashMap<>(){{
        put("my.domain.com", "myanalytics");
    }})
//  .enableDebug()
//  .dryRun()
    .start(this)
```

## Disable Trackingplan

To disable `Trackingplan for Android SDK` without removing it completely both the adapter plugin that works at compile time and the client that works at runtime must be disabled.

### Disable the adapter plugin
Add next line to your `gradle.properties` file:

```groovy
trackingplan.enableSdk=false
```

### Disable the client
Replace the call to `Trackingplan.init("YOUR_TP_ID").start(this)` by the next one:

```java
Trackingplan.stop(this);
```

Alternatively, since `Trackingplan for Android SDK` uses [App Startup](https://developer.android.com/topic/libraries/app-startup) to perform its runtime initialization, the client can be disabled by adding the following to the `AndroidManifest.xml` of the app:

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="com.trackingplan.client.sdk.TrackingplanInitializer"
        tools:node="remove" />
</provider>
```




## Building from source code

First of all, clone this repository to a local directory in your machine. After that, open a terminal in that directory and run:

```console
$ ./gradlew cleanBuildPublishLocal
```

In order to use this custom built, modify your `project-level build.gradle` file as indicated below:

```gradle
buildscript {
    repositories {
        // ...
        mavenLocal() // <-- Add maven local
    }

}

allprojects {
    repositories {
        // ...
        mavenLocal() // <-- Add maven local
    }
}    
```

Remember to change the version of Trackingplan in your dependencies to  `1.2.0-SNAPSHOT`.

## Need help?
Questions? Problems? Need more info? We can help! Contact us [here](mailto:team@trackingplan.com).


## Learn more
Visit www.trackingplan.com


## License
Copyright © 2021 Trackingplan Inc. All Rights Reserved.
