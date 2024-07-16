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

rootProject.name = "TU-Berlin-EDC-Blockchain-Extensions"

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



// launchers ---------------------------------------------------------------------------------------
include("launchers:edc-tu-berlin")
include("launchers:push-http-backend")

// modules for code samples ------------------------------------------------------------------------
include(":other:custom-runtime")
include("BlockchainCatalog:blockchain-catalog-api")
include("BlockchainCatalog:blockchain-catalog-listener")
//include("BlockchainCatalog:blockchain-catalog-prosumer")
include("playground:apiTest")
findProject(":BlockchainCatalog:blockchain-catalog-prosumer")?.name = "blockchain-catalog-prosumer"

include("blockchain-logger")

include("extensions:transfer:http-push:provider-push-http-backend-service")

// modules for code samples ------------------------------------------------------------------------
include(":other:custom-runtime")

include(":system-tests")
include("playground")
