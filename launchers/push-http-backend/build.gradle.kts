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
    id("java")
    id("application")
}

dependencies {
    implementation(project(":extensions:transfer:http-push:provider-push-http-backend-service"))

    //implementation(libs.opentelemetry.exporter.jaeger)
    //implementation(libs.edc.api.observability)
    //runtimeOnly(libs.edc.monitor.jdk.logger)
}

