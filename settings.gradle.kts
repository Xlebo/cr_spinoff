pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "clash-royale-mvp"

include(":shared")
include(":server")
include(":game:core")
// Skip Android module when local.properties is absent (Railway CI has no Android SDK).
// AGP fails during the Gradle configuration phase without an SDK, blocking all tasks.
val hasAndroidSdk = file("local.properties").let { f ->
    f.exists() && f.readLines().any { it.startsWith("sdk.dir") }
}
if (hasAndroidSdk) include(":game:android")
include(":game:desktop")
