package com.msg.plcaad.edc.ccp.contractdefinition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msg.plcaad.edc.ccp.exception.CcpException;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.util.ArrayList;
import java.util.List;

import static com.msg.plcaad.edc.ccp.Constants.CLAIM_COMPLIANCE_PROVIDER_RESPONSE_FIELD_NAME;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * This class provides the functionality to extract data from the Claim Compliance Provider from an contrace definition.
 */
public class CcpIntegrationForContractDefinitionService {

    private final Monitor monitor;

    public CcpIntegrationForContractDefinitionService(final Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Extracts the CCP-Response from the assets of the contract definition.
     * @param contractDefinition The contract definition to extract the CCP-Response from
     * @param assetIndex The asset index to get the assets from
     * @return The CCP-Responses of all assets as a JSON array (string) or null if no CCP-Response was found
     * @throws CcpException If the CCP-Response could not be extracted
     */
    public String getVerifiablePresentationsFromAssets(final ContractDefinition contractDefinition, final AssetIndex assetIndex) throws CcpException {
        final List<String> ccpResponses = new ArrayList<>();
        contractDefinition.getAssetsSelector().forEach(criterion -> {
            final Object operandLeft = criterion.getOperandLeft();
            if (operandLeft.equals(EDC_NAMESPACE + "id")) {
                final Object operandRight = criterion.getOperandRight();
                final Asset asset = assetIndex.findById(operandRight.toString());
                if (asset != null) {
                    this.monitor.debug("Asset found: " + asset.getId());
                    final Object ccpResponse = asset.getProperty(EDC_NAMESPACE + CLAIM_COMPLIANCE_PROVIDER_RESPONSE_FIELD_NAME);
                    if (ccpResponse == null) {
                        this.monitor.warning("No CCP-Response found for Asset with id " + asset.getId());
                    } else {
                        this.monitor.debug("CCP-Response found for Asset with id " + asset.getId());
                        ccpResponses.add(ccpResponse.toString());
                    }

                } else {
                    this.monitor.warning("Asset " + operandRight + " not found. Skipping this criterion.");
                }
            }
        });
        if (ccpResponses.isEmpty()) {
            return null;
        }
        this.monitor.info("Found " + ccpResponses.size() + " CCP-Responses");
        return getJsonArray(ccpResponses);
    }

    private String getJsonArray(final List<String> ccpResponses) throws CcpException{
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(ccpResponses);
        } catch (Exception e) {
            throw new CcpException("Failed to convert ccpResponses to JSON: " + e.getMessage(), e);
        }
    }
}
