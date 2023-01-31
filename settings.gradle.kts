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

rootProject.name = "samples"

// this is needed to have access to snapshot builds of plugins
pluginManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:0.0.1-SNAPSHOT")
            // this is not part of the published EDC Version Catalog, so we'll just "amend" it
            library("dnsOverHttps", "com.squareup.okhttp3", "okhttp-dnsoverhttps").versionRef("okhttp")
        }
    }
}



// modules for code samples ------------------------------------------------------------------------
include(":other:custom-runtime")
include("BlockchainCatalog:blockchain-catalog-api")
include("BlockchainCatalog:blockchain-catalog-listener")
include("BlockchainCatalog:blockchain-catalog-prosumer")
findProject(":BlockchainCatalog:blockchain-catalog-prosumer")?.name = "blockchain-catalog-prosumer"
