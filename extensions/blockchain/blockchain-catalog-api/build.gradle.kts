
plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

val groupId: String by project
val edcVersion: String by project

dependencies {
    implementation(libs.edc.control.plane.core)
    implementation(libs.edc.http)

    implementation(libs.edc.configuration.filesystem)
    implementation(libs.edc.iam.mock)
    implementation(libs.edc.auth.tokenbased)
    implementation(libs.edc.management.api)


    implementation(libs.edc.spi.fcc)
    implementation(libs.edc.spi.web)

    api(libs.edc.control.plane.spi)

    implementation(libs.edc.http)


    implementation(project(":extensions:blockchain:catalog-listener"))

    implementation(libs.edc.dsp)
}


application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}