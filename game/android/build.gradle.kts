plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val libgdxVersion: String = libs.versions.libgdx.get()

android {
    namespace = "com.yourgame"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.yourgame.clashroyalemvp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Override serverUrl in gradle.properties for Wi-Fi or Railway testing.
        val serverUrl = project.findProperty("serverUrl")?.toString() ?: "ws://10.0.2.2:8080/ws"
        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }
}

// Configuration for LibGDX native .so files
val natives: Configuration by configurations.creating

dependencies {
    implementation(project(":game:core"))
    implementation(libs.libgdx.backend.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.client.okhttp)
    natives("com.badlogicgames.gdx:gdx-platform:$libgdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$libgdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$libgdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$libgdxVersion:natives-x86_64")
}

// Unpack native .so files into src/main/libs/<abi>/
tasks.register("copyAndroidNatives") {
    doFirst {
        natives.copy().files.forEach { jar ->
            val outputDir = file("libs/${jar.nameWithoutExtension.substringAfterLast("natives-")}")
            outputDir.mkdirs()
            copy {
                from(zipTree(jar))
                into(outputDir)
                include("*.so")
            }
        }
    }
}

tasks.whenTaskAdded {
    if (name.contains("package", ignoreCase = true)) {
        dependsOn("copyAndroidNatives")
    }
}
