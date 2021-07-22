# Trackingplan for Android SDK

## Table of Contents

- [Trackingplan](#trackingplan)  
- [Add Trackingplan to your Android app](#add-trackingplan-to-your-android-app)
- [Advanced options](#advanced-options)
- [Building from sources](#building-from-sources)
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

Select the `project-level build.gradle` file and add `com.trackingplan.client:adapter:1.0.3` as a classpath dependency to the dependencies section:

```gradle
dependencies {   
    // ...
    classpath "com.trackingplan.client:adapter:1.0.3"
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

- Add `implementation 'com.trackingplan.client:sdk:1.0.3'` to the dependencies section.
```gradle
dependencies {
    // ...
    implementation 'com.trackingplan.client:sdk:1.0.3'
    // ...
}
```

Then in the `onCreate` method of your Application's Main Activity, set up the SDK like so:

```java
Trackingplan.init("YOUR TRACKINGPLAN ID GOES HERE").start(this)
```

And of course, import the SDK:

```java
import com.trackingplan.client.sdk.Trackingplan;
```

All set!

## Advanced options

Trackingplan for Android supports the same Advanced Options as Trackingplan for Web.
For instance, to set a `source alias` and turn on `debug mode` use the following:

```java
Trackingplan.init("YOUR TRACKINGPLAN ID GOES HERE")
    .sourceAlias("Android app")
    .enableDebug()
    .start(this)
```

Check the [JS SDK's Advanced Options](https://github.com/trackingplan/trackingplan.js#advanced-options) section for more details.

Additionaly, there is a `dryRun` option available to let you test Trackingplan for Android without actually sending any data to Trackingplan.

## Building from source code

First of all, clone this repository to a local directory in your machine. After that, open a terminal in that directory and run:

```console
$ ./gradlew cleanBuildLocalPublish
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

Remember to change the version of Trackingplan in your dependencies to  `1.0-SNAPSHOT`.

## Need help?
Questions? Problems? Need more info? Contact us at team@trackingplan.com, and we can help!

## Learn more

Visit www.trackingplan.com.
