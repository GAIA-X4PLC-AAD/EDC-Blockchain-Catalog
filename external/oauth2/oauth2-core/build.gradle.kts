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
 *
 */

plugins {
    `java-library`
}

dependencies {
    /*
    api(project(":spi:common:http-spi"))
    api(project(":spi:common:oauth2-spi"))


    implementation(project(":core:common:jwt-core"))


    */
    api(libs.edc.spi.http)
    api(libs.edc.spi.oauth2)
    implementation(project(":external:oauth2:oauth2-client"))
    implementation(libs.edc.jwt.core)
    implementation(libs.nimbus.jwt)
}


