import io.micronaut.fuzzing.jazzer.JazzerTask
import io.micronaut.fuzzing.jazzer.PrepareClusterFuzzTask

plugins {
    id("java")
    alias(libs.plugins.micronaut.jazzer.plugin)
}

group = "org.example"
version = "1.0-SNAPSHOT"

val nettyVersionFromEnv = providers.environmentVariable("OSSFUZZ_NETTY_VERSION")

repositories {
    if (nettyVersionFromEnv.isPresent) {
        mavenLocal()
    }

    mavenCentral()
}

dependencies {

    implementation(platform("io.netty:netty-bom:" + nettyVersionFromEnv.orElse("4.1.116.Final").get()))
    implementation("io.netty:netty-codec-http")

    implementation(libs.micronaut.fuzzing.api)
    implementation(libs.micronaut.fuzzing.runner)

    annotationProcessor(libs.micronaut.inject.java)
    annotationProcessor(libs.micronaut.fuzzing.annotation.processor)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<PrepareClusterFuzzTask> {
    jvmArgs = listOf(
        "-XX:+ExitOnOutOfMemoryError"
    )
}

tasks.named<JazzerTask>("jazzer") {
    // todo: fetch on-demand from gh releases
    jazzerBinary = File("/home/yawkat/bin/jazzer/0.22.1/jazzer")
    targets = listOf("io.netty.handler.ssl.SslHandlerFuzzer")
}

tasks.test {
    useJUnitPlatform()
}