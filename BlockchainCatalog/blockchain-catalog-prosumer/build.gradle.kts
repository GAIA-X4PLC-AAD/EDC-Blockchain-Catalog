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
    /*
    implementation("$groupId:control-plane-core:$edcVersion")

    implementation("$groupId:http:$edcVersion")

    implementation("$groupId:configuration-filesystem:$edcVersion")
    implementation("$groupId:iam-mock:$edcVersion")

    implementation("$groupId:auth-tokenbased:$edcVersion")
    implementation("$groupId:management-api:$edcVersion")
    */
    //implementation(project(":BlockchainCatalog:blockchain-catalog-api"))
    //implementation(project(":BlockchainCatalog:blockchain-catalog-listener"))
    //implementation(project(":blockchain-logger"))



    /*


    // data plane related dependencies to enable transfer via azurerite
    implementation("$groupId:data-plane-azure-storage:$edcVersion")
    implementation("$groupId:data-plane-util:$edcVersion")
    implementation("$groupId:data-plane-core:$edcVersion")
    implementation("$groupId:data-plane-framework:$edcVersion")
    implementation("$groupId:data-plane-api:$edcVersion")
    implementation("$groupId:control-plane-core:$edcVersion")
    implementation("$groupId:data-plane-core:$edcVersion")
    implementation("$groupId:data-plane-http:$edcVersion")
    implementation("$groupId:data-plane-azure-storage:$edcVersion")
    // implementation("$groupId:vault-azure:$edcVersion")
    implementation("$groupId:transfer-data-plane:$edcVersion")
    implementation("$groupId:transfer-core:$edcVersion")
    implementation("$groupId:data-plane-client:$edcVersion")
    implementation("$groupId:data-plane-selector-client:$edcVersion")
    implementation("$groupId:data-plane-selector-core:$edcVersion")
    implementation("$groupId:data-plane-selector-api:$edcVersion")
    implementation("$groupId:transfer-data-plane:$edcVersion")

    implementation("${groupId}:control-plane-core:${edcVersion}")
    implementation("${groupId}:configuration-filesystem:${edcVersion}")
    implementation("${groupId}:vault-filesystem:${edcVersion}")
    implementation("${groupId}:iam-mock:${edcVersion}")
    implementation("${groupId}:management-api:${edcVersion}")
    implementation("${groupId}:transfer-data-plane:${edcVersion}")

    implementation("${groupId}:data-plane-selector-api:${edcVersion}")
    implementation("${groupId}:data-plane-selector-core:${edcVersion}")
    implementation("${groupId}:data-plane-selector-client:${edcVersion}")

    implementation("${groupId}:data-plane-api:${edcVersion}")
    implementation("${groupId}:data-plane-core:${edcVersion}")
    implementation("${groupId}:data-plane-http:${edcVersion}")

    implementation("$groupId:vault-filesystem:$edcVersion")

    api("$groupId:control-plane-spi:$edcVersion")
    api("$groupId:data-plane-spi:$edcVersion")
    */

    api(libs.edc.control.plane.spi)
    implementation(libs.edc.http)
    implementation(libs.edc.dsp)
    implementation(libs.edc.management.api)
    implementation(libs.edc.data.plane.selector.core)
    implementation(libs.edc.iam.mock)

    //implementation(project(":playground:apiTest"))
    //implementation(project(":transfer:transfer-07-provider-push-http:http-push-connector"))
    //implementation(project(":transfer:transfer-07-provider-push-http:provider-push-http-backend-service"))
    implementation(project(":BlockchainCatalog:blockchain-catalog-api"))
    implementation(project(":BlockchainCatalog:blockchain-catalog-listener"))
    implementation(project(":blockchain-logger"))


//    implementation(project(":transfer:StatusChecker"))
//    implementation(project(":transfer:TransferFileLocal"))

}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}
