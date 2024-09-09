package msg.plcaaad.edc.ccp.api;

import com.msg.plcaad.edc.ccp.api.CcpCallingService;
import com.msg.plcaad.edc.ccp.exception.CcpException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class CcpCallingServiceTest {

    @Mock
    private Monitor monitor;

    @Mock
    private HttpURLConnection httpUrlConnection;

    private CcpCallingService ccpCallingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.ccpCallingService = new CcpCallingService("https://example.com", monitor) {
            @Override
            protected HttpURLConnection getHttpUrlConnection(String claims, String participantCredentials) throws IOException {
                return httpUrlConnection;
            }
        };
    }

    @Test
    @DisplayName("WHEN the API call is successful THEN return the response")
    void testExecuteClaimComplianceProviderCall_success() throws IOException, CcpException {
        // Prepare
        final String claims = "{\"claims\":[]}";
        final String participantCredentials = "{\"credentials\":[]}";
        final String responseJson = "{\"response\":\"success\"}";

        when(httpUrlConnection.getResponseCode()).thenReturn(200);
        when(httpUrlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(responseJson.getBytes()));

        // Action
        final String result = ccpCallingService.executeClaimComplianceProviderCall(claims, participantCredentials);

        // Test
        assertThat(result).isEqualTo(responseJson);
    }

    @Test
    @DisplayName("WHEN the API call fails THEN throw an exception")
    void testExecuteClaimComplianceProviderCall_failure() throws IOException {
        // Prepare
        final String claims = "{\"claims\":[]}";
        final String participantCredentials = "{\"credentials\":[]}";
        final int errorCode = 500;

        when(httpUrlConnection.getResponseCode()).thenReturn(errorCode);
        when(httpUrlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));

        // Action & Test
        assertThatThrownBy(() -> ccpCallingService.executeClaimComplianceProviderCall(claims, participantCredentials))
                .isInstanceOf(CcpException.class)
                .hasMessageContaining("Unexpected response status: " + errorCode);
    }

    @Test
    @DisplayName("WHEN an IOException occurs THEN throw a CcpException")
    void testExecuteClaimComplianceProviderCall_ioException() throws IOException {
        // Prepare
        final String claims = "{\"claims\":[]}";
        final String participantCredentials = "{\"credentials\":[]}";
        final IOException ioException = new IOException("Network error");

        when(httpUrlConnection.getResponseCode()).thenThrow(ioException);
        when(httpUrlConnection.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));

        // Action & Test
        assertThatThrownBy(() -> ccpCallingService.executeClaimComplianceProviderCall(claims, participantCredentials))
                .isInstanceOf(CcpException.class)
                .hasMessageContaining("IOException while calling CCP.");
    }
}
