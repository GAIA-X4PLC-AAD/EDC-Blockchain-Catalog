package berlin.tu.ise.extension.blockchain.catalog.listener;

import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClaimComplianceProviderService {

    protected static String callClaimComplianceProvider(final String claimComplianceServiceEndpoint, final String claims,
                                                        final String participantCredentials, final Monitor monitor) throws CcpRequestException {
        try {
            final HttpURLConnection conn = getHttpURLConnection(claimComplianceServiceEndpoint, claims, participantCredentials);
            final StringBuilder response = readResponse(conn);
            final int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                throw new CcpRequestException("Unexpected response status: " + statusCode);
            }
            return response.toString();
        } catch (final IOException ioException) {
            throw new CcpRequestException("IOException while calling CCP.", ioException);
        }
    }

    private static @NotNull StringBuilder readResponse(final HttpURLConnection conn) throws IOException {
        final StringBuilder response = new StringBuilder();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response;
    }

    private static @NotNull HttpURLConnection getHttpURLConnection(final String claimComplianceServiceEndpoint, final String claims, final String participantCredentials) throws IOException {
        final URL url = new URL(claimComplianceServiceEndpoint);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        final String jsonInputString = String.format("{\"claims\": %s, \"verifiableCredentials\": %s}", claims, participantCredentials);

        try(final OutputStream os = conn.getOutputStream()) {
            final byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return conn;
    }

    private ClaimComplianceProviderService() {
    }
}
