plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

group = "com.yourgame"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.yourgame.desktop.DesktopLauncherKt")
}

val libgdxVersion: String = libs.versions.libgdx.get()

dependencies {
    implementation(project(":game:core"))
    implementation(libs.libgdx.backend.desktop)
    implementation("com.badlogicgames.gdx:gdx-platform:$libgdxVersion:natives-desktop")
}
