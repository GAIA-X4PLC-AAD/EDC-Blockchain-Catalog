package msg.plcaaad.edc.ccp.asset;

import com.msg.plcaad.edc.ccp.api.CcpCallingService;
import com.msg.plcaad.edc.ccp.asset.CcpIntegrationForAssetService;
import com.msg.plcaad.edc.ccp.exception.CcpException;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CcpIntegrationForAssetServiceTest {

    @Mock
    private Monitor monitor;
    @Mock
    private AssetService assetService;
    @Mock
    private CcpCallingService ccpCallingService;
    @InjectMocks
    private CcpIntegrationForAssetService ccpIntegrationForAssetService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ccpIntegrationForAssetService = new CcpIntegrationForAssetService(this.monitor, this.ccpCallingService);
    }

    @Test
    @DisplayName("WHEN claims and participant credentials are passed THEN call CCP and update asset with response")
    void testCallClaimComplianceProvider_successWithClaims() throws Exception {
        // Prepare
        final String endpoint = "https://example.com";
        final String claimsListJson = "{\"claims\":[]}";
        final String gxParticipantCredentialsJson = "{\"credentials\":[]}";
        final String responseJson = "{\"response\":\"success\"}";
        final Map<String, Object> properties = new HashMap<>();
        properties.put(EDC_NAMESPACE + "claimsList", Base64.getEncoder().encodeToString(claimsListJson.getBytes()));
        properties.put(EDC_NAMESPACE + "gxParticipantCredentials", Base64.getEncoder().encodeToString(gxParticipantCredentialsJson.getBytes()));

        final Asset asset = Asset.Builder.newInstance()
                .properties(properties)
                .build();

        when(ccpCallingService.executeClaimComplianceProviderCall(anyString(), anyString()))
                .thenReturn(responseJson);
        when(assetService.update(any(Asset.class))).thenReturn(ServiceResult.success(asset));

        // Action
        Asset result = ccpIntegrationForAssetService.callClaimComplianceProvider(endpoint, assetService, asset);

        // Test
        assertThat(result).isNotNull();
        assertThat(result.getProperty(EDC_NAMESPACE + "claimComplianceProviderResponse")).isEqualTo(Base64.getEncoder().encodeToString(responseJson.getBytes()));
    }

    @Test
    @DisplayName("WHEN claimComplianceProviderResponse is also passed THEN do not call CCP")
    void testCallClaimComplianceProvider_successWithExistingResponse() throws Exception {
        // Prepare
        final String endpoint = "https://example.com";
        final String claimsListJson = "{\"claims\":[]}";
        final String gxParticipantCredentialsJson = "{\"credentials\":[]}";
        final String claimComplianceProviderResponseJson = "{\"response\":\"already there\"}";
        final Map<String, Object> properties = new HashMap<>();
        properties.put(EDC_NAMESPACE + "claimComplianceProviderResponse", Base64.getEncoder().encodeToString(claimComplianceProviderResponseJson.getBytes()));
        properties.put(EDC_NAMESPACE + "claimsList", Base64.getEncoder().encodeToString(claimsListJson.getBytes()));
        properties.put(EDC_NAMESPACE + "gxParticipantCredentials", Base64.getEncoder().encodeToString(gxParticipantCredentialsJson.getBytes()));

        final Asset asset = Asset.Builder.newInstance()
                .properties(properties)
                .build();

        // Action
        final Asset result = ccpIntegrationForAssetService.callClaimComplianceProvider(endpoint, assetService, asset);

        // Test
        verify(monitor).info("Asset will not be processed with ClaimComplianceProvider since there is already a response in the asset.");
        verify(ccpCallingService, never()).executeClaimComplianceProviderCall(anyString(), anyString());
        verify(assetService, never()).update(any());
        assertThat(result.getProperty(EDC_NAMESPACE + "claimComplianceProviderResponse")).isEqualTo(Base64.getEncoder().encodeToString(claimComplianceProviderResponseJson.getBytes()));

    }

    @Test
    @DisplayName("WHEN invalid json is passed THEN CcpException is thrown.")
    void testCallClaimComplianceProvider_invalidJson() {
        // Prepare
        String endpoint = "https://example.com";
        String invalidJson = "invalid json";
        final Map<String, Object> properties = new HashMap<>();
        properties.put(EDC_NAMESPACE + "claimsList", Base64.getEncoder().encodeToString(invalidJson.getBytes()));
        properties.put(EDC_NAMESPACE + "gxParticipantCredentials", Base64.getEncoder().encodeToString(invalidJson.getBytes()));

        final Asset asset = Asset.Builder.newInstance()
                .properties(properties)
                .build();

        // Action & Test
        assertThatThrownBy(() -> ccpIntegrationForAssetService.callClaimComplianceProvider(endpoint, assetService, asset))
                .isInstanceOf(CcpException.class)
                .hasMessageContaining("Asset cannot not be processed with ClaimComplianceProvider since claimsList or gxParticipantCredentials is not valid JSON.");
    }
}