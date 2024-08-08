
plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val groupId: String by project
val edcVersion: String by project

dependencies {

    api(libs.edc.control.plane.spi)
    implementation(libs.edc.http)
    implementation(libs.edc.dsp)
    implementation(libs.edc.management.api)
    // using the dsp api now instead of ids api
    // dsp-api-configuration should contain the TypeTransformerRegistry which can transform objects to json ld and vise versa
    implementation(project(":extensions:claim-compliance-provider-integration"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}
