import io.micronaut.fuzzing.jazzer.PrepareClusterFuzzTask

plugins {
    id("java")
    alias(libs.plugins.micronaut.jazzer.plugin)
}

group = "org.example"
version = "1.0-SNAPSHOT"

val nettyVersionFromEnv = providers.environmentVariable("OSSFUZZ_NETTY_VERSION")
val nettyTcnativeVersionFromEnv = providers.environmentVariable("OSSFUZZ_NETTY_TCNATIVE_VERSION")
val nettyIoUringVersionFromEnv = providers.environmentVariable("OSSFUZZ_NETTY_IOURING_VERSION")

repositories {
    if (nettyVersionFromEnv.isPresent) {
        mavenLocal()
    }

    mavenCentral()
}

dependencies {

    implementation(platform("io.netty:netty-bom:" + nettyVersionFromEnv.getOrElse("4.1.116.Final")))
    implementation("io.netty:netty-codec-http")
    if (nettyVersionFromEnv.isPresent) {
        implementation("io.netty:netty-transport-classes-epoll")
    } else {
        implementation("io.netty:netty-transport-native-epoll::linux-x86_64")
    }
    if (nettyTcnativeVersionFromEnv.isPresent) {
        implementation("io.netty:netty-tcnative-classes:" + nettyTcnativeVersionFromEnv.get())
    } else {
        implementation("io.netty:netty-tcnative:2.0.69.Final:linux-x86_64")
    }
    if (nettyIoUringVersionFromEnv.isPresent) {
        implementation("io.netty.incubator:netty-incubator-transport-classes-io_uring:" + nettyIoUringVersionFromEnv.get())
    } else {
        implementation("io.netty.incubator:netty-incubator-transport-native-io_uring:0.0.26.Final:linux-x86_64")
    }

    implementation(libs.micronaut.fuzzing.api)
    implementation(libs.micronaut.fuzzing.runner)

    annotationProcessor(libs.micronaut.inject.java)
    annotationProcessor(libs.micronaut.fuzzing.annotation.processor)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<PrepareClusterFuzzTask> {
    jvmArgs = listOf(
        "-XX:+ExitOnOutOfMemoryError",
        "-Djava.library.path=\$this_dir"
    )
    jni.isEnabled = true
}

tasks.test {
    useJUnitPlatform()
}