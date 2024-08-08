package msg.plcaaad.edc.ccp.contractdefinition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msg.plcaad.edc.ccp.contractdefinition.CcpIntegrationForContractDefinitionService;
import com.msg.plcaad.edc.ccp.exception.CcpException;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.msg.plcaad.edc.ccp.Constants.CLAIM_COMPLIANCE_PROVIDER_RESPONSE_FIELD_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.when;

class CcpIntegrationForContractDefinitionServiceTest {
    @Mock
    private Monitor monitor;

    @Mock
    private ContractDefinition contractDefinition;

    @Mock
    private AssetIndex assetIndex;

    private CcpIntegrationForContractDefinitionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CcpIntegrationForContractDefinitionService(this.monitor);
    }

    @Test
    @DisplayName("WHEN assets with CCP-Responses are passed THEN extract the responses")
    void testGetVerifiablePresentationsFromAssets_success() throws CcpException, JsonProcessingException {
        // Prepare
        final String responseString = "response1";
        List<String> ccpResponses = new ArrayList<>();
        ccpResponses.add(responseString);
        ccpResponses.add(responseString);

        final Map<String, Object> properties = new HashMap<>();
        properties.put(EDC_NAMESPACE + CLAIM_COMPLIANCE_PROVIDER_RESPONSE_FIELD_NAME, responseString);

        final Asset asset1 = Asset.Builder.newInstance()
                .properties(properties)
                .build();

        final Asset asset2 = Asset.Builder.newInstance()
                .properties(properties)
                .build();

        when(contractDefinition.getAssetsSelector()).thenReturn(List.of(
                new Criterion(EDC_NAMESPACE + "id", "=", "asset1"),
                new Criterion(EDC_NAMESPACE + "id", "=", "asset2")
        ));
        when(assetIndex.findById("asset1")).thenReturn(asset1);
        when(assetIndex.findById("asset2")).thenReturn(asset2);

        // Action
        String result = service.getVerifiablePresentationsFromAssets(contractDefinition, assetIndex);

        // Test
        ObjectMapper objectMapper = new ObjectMapper();
        String expectedJson = objectMapper.writeValueAsString(ccpResponses);
        assertThat(result).isEqualTo(expectedJson);
    }

    @Test
    void testGetVerifiablePresentationsFromAssets_noResponses() throws CcpException {
        // Prepare
        when(contractDefinition.getAssetsSelector()).thenReturn(List.of(
                new Criterion(EDC_NAMESPACE + "id", "=", "asset1")
        ));
        when(assetIndex.findById("asset1")).thenReturn(null);

        // Action
        String result = service.getVerifiablePresentationsFromAssets(contractDefinition, assetIndex);

        // Test
        assertThat(result).isNull();
    }
}
