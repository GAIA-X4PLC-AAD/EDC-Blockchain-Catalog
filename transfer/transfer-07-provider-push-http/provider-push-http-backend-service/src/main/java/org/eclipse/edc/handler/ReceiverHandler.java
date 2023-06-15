/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ReceiverHandler implements HttpHandler {

    private final Path savePath;

    public ReceiverHandler(Path savePath) {
        this.savePath = savePath;
    }

    /**
     * This method just prints the request body to the console and returns a 200 OK response.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        System.out.println("Received request: " + exchange.getRequestURI());
        System.out.println("Content-Type: " + exchange.getRequestHeaders().getFirst("Content-Type"));
        System.out.println("Content-Length: " + exchange.getRequestHeaders().getFirst("Content-Length"));

        byte[] buffer = exchange.getRequestBody().readAllBytes();

        // Get extension based on content type
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String extension = "";
        if ("image/jpeg".equals(contentType)) {
            extension = ".jpg";
        } else if ("image/png".equals(contentType)) {
            extension = ".png";
        } else if ("text/plain".equals(contentType)) {
            extension = ".txt";
        }

        // Save file
        Path filePath = savePath.resolve(UUID.randomUUID().toString() + extension);
        Files.write(filePath, buffer);

        exchange.sendResponseHeaders(200, 0);
    }
}