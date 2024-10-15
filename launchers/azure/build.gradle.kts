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

    implementation(project(":extensions:blockchain:catalog-listener"))
    implementation(project(":extensions:blockchain:logger"))
    implementation(project(":extensions:blockchain:blockchain-catalog-api"))

    api(libs.edc.control.plane.spi)
    implementation(libs.edc.http)
    implementation(libs.edc.dsp)
    implementation(libs.edc.management.api)
    implementation(libs.edc.data.plane.selector.core)
    implementation(libs.edc.iam.mock)

    // provider push http example
    implementation(libs.edc.control.plane.core)
    implementation(libs.edc.control.plane.api.client)
    implementation(libs.edc.control.plane.api)
    implementation(libs.edc.dsp)
    implementation(libs.edc.configuration.filesystem)
    implementation(libs.edc.vault.filesystem)
    implementation(libs.edc.iam.mock)
    implementation(libs.edc.management.api)
    implementation(libs.edc.transfer.data.plane)
    implementation(libs.edc.data.plane.azure.storage)

    implementation(libs.edc.data.plane.selector.api)
    implementation(libs.edc.data.plane.selector.core)
    implementation(libs.edc.data.plane.selector.client)

    implementation(libs.edc.data.plane.api)
    implementation(libs.edc.data.plane.core)
    implementation(libs.edc.data.plane.http)

    implementation(libs.opentelemetry.exporter.jaeger)
    implementation(libs.edc.api.observability)

    implementation(libs.edc.monitor.jdk.logger)

    implementation(libs.edc.iam.mock)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("edc-tu-berlin.jar")
    dependsOn(distTar, distZip)
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

