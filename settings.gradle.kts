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
include("launchers:azure")

// modules for code samples ------------------------------------------------------------------------
//include("extensions:blockchain:catalog-api")
// extensions:blockchain:blockchain-catalog-api is actually in extensions:blockchain:catalog-api
include("extensions:blockchain:blockchain-catalog-api")
include("extensions:blockchain:catalog-listener")
include("extensions:blockchain:logger")

include("extensions:transfer:http-push:provider-push-http-backend-service")

include("extensions:claim-compliance-provider-integration")

