plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

group = "com.yourgame"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.yourgame.server.ApplicationKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(kotlin("test"))
}

tasks.shadowJar {
    archiveFileName.set("server-all.jar")
    mergeServiceFiles()
}

tasks.test {
    useJUnitPlatform()
}
