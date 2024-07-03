/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *       ZF Friedrichshafen AG - add dependency
 *
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val groupId: String by project
val edcVersion: String by project

dependencies {

    api(libs.edc.control.plane.spi)
    implementation("org.example:edc-http")
    implementation("org.example:dsp-spi")
    implementation("org.example:edc-management-api")
    implementation("org.example:edc-data-plane-selector-core")
    implementation("org.example:edc-iam-mock")

    implementation(project(":BlockchainCatalog:blockchain-catalog-api"))
    implementation(project(":BlockchainCatalog:blockchain-catalog-listener"))
    implementation(project(":blockchain-logger"))

    // provider push http example
    implementation("org.example:edc-controle-plane-core")
    implementation("org.example:edc-configuration-filesystem")
    implementation("org.example:edc-vault-filesystem")
    implementation("org.example:edc-transfer-data-plane")
    implementation("org.example:edc-data-plane-selector-client")
    implementation("org.example:edc-data-plane-selector-api")
    implementation("org.example:edc-data-plane-core")
    implementation("org.example:edc-data-plane-http")
    
    implementation(libs.opentelemetry.exporter.jaeger)
    implementation(libs.edc.api.observability)

    runtimeOnly(libs.edc.monitor.jdk.logger)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}

val openTelemetry by configurations.creating

dependencies {
    openTelemetry(libs.opentelemetry)
    openTelemetry(libs.opentelemetry.exporter.jaeger)
}

tasks.register("copyOpenTelemetryJar", Copy::class) {
    from(openTelemetry)
    into("build/libs")
}

tasks.build {
    finalizedBy("copyOpenTelemetryJar")
}
