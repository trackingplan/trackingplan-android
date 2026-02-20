@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.android.lint")
    id("maven-publish")
    id("signing")
    kotlin("plugin.serialization") version "2.0.21"
}

group = rootProject.extra["groupId"] as String
version = rootProject.extra["TrackingplanVersion"] as String

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.trackingplan.shared"
        compileSdk = 36
        minSdk = 24

        // Host tests disabled - org.json unavailable in JVM, use device tests instead
        // withHostTestBuilder {
        // }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    // For iOS targets, this is also where you should
    // configure native binary output. For more information, see:
    // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

    // A step-by-step guide on how to include this library in an XCode
    // project can be found here:
    // https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "TrackingplanShared"
    val xcf = XCFramework(xcfName)
    val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    iosTargets.forEach {
        it.binaries.framework {
            baseName = xcfName
            xcf.add(this)
            freeCompilerArgs += listOf(
                "-Xoverride-konan-properties=minVersion.ios=14.0"
            )
        }
        it.compilations.getByName("main") {
            cinterops {
                create("oslog")
            }
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                // Add KMP dependencies here
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation("androidx.test:runner:1.7.0")
                implementation("androidx.test:core:1.7.0")
                implementation("androidx.test.ext:junit:1.3.0")
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP's default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }

    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

}

// Custom task to deploy XCFramework to iOS project
tasks.register("deployXCFrameworkToiOS") {
    group = "ios deployment"
    description = "Copies the TrackingplanShared XCFramework to the iOS SDK project"

    dependsOn("assembleTrackingplanSharedReleaseXCFramework")

    doLast {
        val xcframeworkDir = file("build/XCFrameworks/release/TrackingplanShared.xcframework")
        val iosProjectDir = file("../../ios/SDK/Frameworks")
        val targetXcframeworkDir = file("${iosProjectDir}/TrackingplanShared.xcframework")

        // Create Frameworks directory in iOS project if it doesn't exist
        iosProjectDir.mkdirs()

        // Copy unzipped XCFramework to iOS project
        copy {
            from(xcframeworkDir)
            into(targetXcframeworkDir)
        }
        println("✅ TrackingplanShared XCFramework copied to: ${targetXcframeworkDir.absolutePath}")
    }
}

publishing {
    publications {
        // Common POM configuration for all publications
        withType<MavenPublication> {
            pom {
                url = "https://www.trackingplan.com"
                licenses {
                    license {
                        name = "Copyright Trackingplan 2025 - All rights reserved"
                    }
                }
                developers {
                    developer {
                        organization = "Trackingplan"
                        organizationUrl = "https://www.trackingplan.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/trackingplan/trackingplan-android.git"
                    developerConnection = "scm:git:ssh://github.com/trackingplan/trackingplan-android.git"
                    url = "https://github.com/trackingplan/trackingplan-android/tree/main"
                }
            }
        }

        named<MavenPublication>("kotlinMultiplatform") {
            artifactId = "shared"
            pom {
                name = "Trackingplan Shared"
                description = "Shared library for Trackingplan SDK with metadata for all platforms"
            }
        }

        named<MavenPublication>("android") {
            artifactId = "shared-android"
            pom {
                name = "Trackingplan Shared - Android"
                description = "Shared Android library for Trackingplan SDK"
                packaging = "aar"
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"

            val releasesRepoUrl = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://ossrh-staging-api.central.sonatype.com/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = rootProject.findProperty("ossrhUsername") as String?
                password = rootProject.findProperty("ossrhPassword") as String?
            }
        }
    }
}

signing {
    isRequired = gradle.taskGraph.hasTask("publishAllPublicationsToSonatypeRepository")
    sign(publishing.publications)
}
