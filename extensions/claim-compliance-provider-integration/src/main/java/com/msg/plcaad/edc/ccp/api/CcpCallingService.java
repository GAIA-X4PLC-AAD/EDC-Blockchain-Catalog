package com.msg.plcaad.edc.ccp.api;

import com.msg.plcaad.edc.ccp.exception.CcpException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CcpCallingService {

    private final String claimComplianceServiceEndpoint;
    private final Monitor monitor;

    public CcpCallingService(String url, final Monitor monitor) {
        this.claimComplianceServiceEndpoint = url;
        this.monitor = monitor;
    }

    public String executeClaimComplianceProviderCall(final String claims, final String participantCredentials) throws CcpException {
        try {
            final HttpURLConnection conn = getHttpUrlConnection(claims, participantCredentials);
            final StringBuilder response = readResponse(conn);
            final int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                this.monitor.severe("Unexpected response status: " + statusCode);
                this.monitor.severe("Response: " + response);
                throw new CcpException("Unexpected response status: " + statusCode);
            }
            return response.toString();
        } catch (final IOException ioException) {
            this.monitor.severe("IOException while calling CCP: " + ioException.getMessage(), ioException);
            throw new CcpException("IOException while calling CCP.", ioException);
        }
    }

    private @NotNull StringBuilder readResponse(final HttpURLConnection conn) throws IOException {
        final StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response;
    }

    protected @NotNull HttpURLConnection getHttpUrlConnection(final String claims, final String participantCredentials) throws IOException {
        final URL url = new URL(this.claimComplianceServiceEndpoint);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        final String jsonInputString = String.format("{\"claims\": %s, \"verifiableCredentials\": %s}", claims, participantCredentials);

        try (OutputStream os = conn.getOutputStream()) {
            final byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return conn;
    }
}
