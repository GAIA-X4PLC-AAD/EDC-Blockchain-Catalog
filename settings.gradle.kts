/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

rootProject.name = "Samples-Blockchain"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

// Include the external projects
includeBuild("Connector") {
    dependencySubstitution {
        substitute(module("org.example:edc-control-plane-spi")).using(project(":spi:control-plane:control-plane-spi"))
        substitute(module("org.example:edc-http")).using(project(":extensions:common:http"))
        substitute(module("org.example:edc-dsp")).using(project(":data-protocols:dsp:dsp-spi"))
        substitute(module("org.example:edc-management-api")).using(project(":extensions:control-plane:api:management-api"))
        substitute(module("org.example:edc-data-plane-selector-core")).using(project(":core:data-plane-selector:data-plane-selector-core"))
        substitute(module("org.example:edc-iam-mock")).using(project(":extensions:common:iam:iam-mock"))
        substitute(module("org.example:edc-configuration-filesystem")).using(project(":extensions:common:configuration:configuration-filesystem"))
        substitute(module("org.example:edc-vault-filesystem")).using(project(":extensions:common:vault:vault-filesystem"))
        substitute(module("org.example:edc-transfer-data-plane")).using(project(":extensions:control-plane:transfer:transfer-data-plane"))
        substitute(module("org.example:edc-data-plane-selector-api")).using(project(":extensions:data-plane-selector:data-plane-selector-api"))
        substitute(module("org.example:edc-data-plane-selector-client")).using(project(":extensions:data-plane-selector:data-plane-selector-client"))
        substitute(module("org.example:edc-data-plane-core")).using(project(":core:data-plane:data-plane-core"))
        substitute(module("org.example:edc-data-plane-http")).using(project(":extensions:data-plane:data-plane-http"))
        substitute(module("org.example:edc-controle-plane-core")).using(project(":core:control-plane:control-plane-core"))
        substitute(module("org.example:dsp-spi")).using(project(":data-protocols:dsp:dsp-spi"))


        // Add other substitutions as needed
        project(":extensions:common:http")
        project(":data-protocols:dsp:dsp-spi")
        project(":extensions:control-plane:api:management-api")
        project(":core:data-plane-selector:data-plane-selector-core")
        project(":extensions:common:iam:iam-mock")

        // provider push http example
        project(":core:control-plane:control-plane-core")
        project(":data-protocols:dsp:dsp-spi")
        project(":extensions:common:configuration:configuration-filesystem")
        project(":extensions:common:vault:vault-filesystem")
        project(":extensions:common:iam:iam-mock")
        project(":extensions:control-plane:api:management-api")
        project(":extensions:control-plane:transfer:transfer-data-plane")

        project(":extensions:data-plane-selector:data-plane-selector-api")
        project(":core:data-plane-selector:data-plane-selector-core")
        project(":extensions:data-plane-selector:data-plane-selector-client")

        project(":extensions:data-plane:data-plane-api")
        project(":core:data-plane:data-plane-core")
        project(":extensions:data-plane:data-plane-http")
    }
}



// modules for code samples ------------------------------------------------------------------------
include(":other:custom-runtime")
include("BlockchainCatalog:blockchain-catalog-api")
include("BlockchainCatalog:blockchain-catalog-listener")
include("BlockchainCatalog:blockchain-catalog-prosumer")
include("playground:apiTest")
findProject(":BlockchainCatalog:blockchain-catalog-prosumer")?.name = "blockchain-catalog-prosumer"

include("blockchain-logger")

// include("transfer:TransferFileLocal")
// include("transfer:StatusChecker")
//include("transfer:transfer-07-provider-push-http:provider-push-http-backend-service")
include("transfer:transfer-07-provider-push-http:http-push-connector")
include("transfer:transfer-07-provider-push-http:http-push-consumer")
include("transfer:transfer-07-provider-push-http:http-push-provider")

include("newtransfer:transfer-07-provider-push-http:provider-push-http-backend-service")

// modules for code samples ------------------------------------------------------------------------
include(":other:custom-runtime")

include(":system-tests")
include("playground")
