plugins {
    id("java")
}

group = "com.msg.plcaad.edc.ccp"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.edc.control.plane.core)
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.api.core)
    implementation(libs.edc.monitor.jdk.logger)

    // Test dependencies
    testImplementation(libs.mockito.core)
    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}