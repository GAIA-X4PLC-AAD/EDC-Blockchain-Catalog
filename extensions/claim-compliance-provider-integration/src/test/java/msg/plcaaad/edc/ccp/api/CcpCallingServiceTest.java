package msg.plcaaad.edc.ccp.api;

import com.msg.plcaad.edc.ccp.api.CcpCallingService;
import com.msg.plcaad.edc.ccp.api.ClaimComplianceServiceApi;
import com.msg.plcaad.edc.ccp.exception.CcpException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CcpCallingServiceTest {
    @Mock
    private ClaimComplianceServiceApi claimComplianceServiceApi;

    private CcpCallingService ccpCallingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.ccpCallingService = new CcpCallingService("https://example.com", claimComplianceServiceApi);
    }

    @Test
    @DisplayName("WHEN the api call is successful THEN return the response")
    void testExecuteClaimComplianceProviderCall_success() throws IOException, CcpException {
        // Prepare
        final String claims = "{\"claims\":[]}";
        final String participantCredentials = "{\"credentials\":[]}";
        final String responseJson = "{\"response\":\"success\"}";

        final Call<String> call = mock(Call.class);
        final Response<String> response = Response.success(responseJson);

        when(claimComplianceServiceApi.callClaimComplianceProvider(any(RequestBody.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Action
        final String result = ccpCallingService.executeClaimComplianceProviderCall(claims, participantCredentials);

        // Test
        assertEquals(responseJson, result);
    }

    @Test
    @DisplayName("WHEN the api call fails THEN throw an exception")
    void testExecuteClaimComplianceProviderCall_failure() throws IOException {
        // Prepare
        final String claims = "{\"claims\":[]}";
        final String participantCredentials = "{\"credentials\":[]}";
        final int errorCode = 500;

        final Call<String> call = mock(Call.class);
        final  Response<String> response = Response.error(errorCode, okhttp3.ResponseBody.create(MediaType.parse("application/json"), ""));

        when(claimComplianceServiceApi.callClaimComplianceProvider(any(RequestBody.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Action & Test
        final CcpException exception = assertThrows(CcpException.class, () -> {
            ccpCallingService.executeClaimComplianceProviderCall(claims, participantCredentials);
        });

        assertEquals("Unexpected response status: " + errorCode, exception.getMessage());
    }
}
