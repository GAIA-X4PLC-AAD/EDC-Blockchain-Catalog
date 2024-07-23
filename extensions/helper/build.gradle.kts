
plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.swagger.core.v3.swagger-gradle-plugin")
}

val groupId: String by project
val edcVersion: String by project

dependencies {
    implementation(libs.edc.vault.filesystem)
}


application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}