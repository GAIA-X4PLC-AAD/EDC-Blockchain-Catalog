plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "org.eclipse.edc"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    api(libs.edc.control.plane.spi)
    implementation(libs.edc.http)
    implementation(libs.edc.dsp)
    implementation(libs.edc.management.api)
    implementation(libs.edc.control.plane.core)
    implementation(libs.edc.data.plane.selector.core)


    implementation(libs.edc.configuration.filesystem)
    implementation(libs.edc.iam.mock)

    implementation(libs.edc.auth.tokenbased)
    implementation(libs.edc.management.api)

}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}

tasks.test {
    useJUnitPlatform()
}