package com.msg.plcaad.edc.ccp.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msg.plcaad.edc.ccp.api.CcpCallingService;
import com.msg.plcaad.edc.ccp.exception.CcpException;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.util.Base64;

import static com.msg.plcaad.edc.ccp.Constants.CLAIMS_LIST_FIELD_NAME;
import static com.msg.plcaad.edc.ccp.Constants.GX_PARTICIPANT_CREDENTIALS_FIELD_NAME;
import static com.msg.plcaad.edc.ccp.Constants.CLAIM_COMPLIANCE_PROVIDER_RESPONSE_FIELD_NAME;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * This class provides the functionality to call the Claim Compliance Provider for an asset.
 */
public class CcpIntegrationForAssetService {
    private final Monitor monitor;
    private CcpCallingService ccpCallingService;

    public CcpIntegrationForAssetService(final Monitor monitor) {
        this.monitor = monitor;
    }

    public CcpIntegrationForAssetService(final Monitor monitor, final CcpCallingService ccpCallingService) {
        this(monitor);
        this.ccpCallingService = ccpCallingService;
    }

    /**
     * Calls the Claim Compliance Provider.
     * This method expects a `EDC_NAMESPACE + claimsList` and `EDC_NAMESPACE + gxParticipantCredentials` in the asset properties as base64 encoded strings.
     * These parameters are taken and forwarded to the Claim Compliance Provider.
     * @param claimComplianceProviderEndpoint The endpoint of the Claim Compliance Provider
     * @param assetService The AssetService to update the asset with the response
     * @param asset The asset to be processed
     * @return The updated asset with an additional property `EDC_NAMESPACE + claimComplianceProviderResponse` containing the response from the Claim Compliance Provider
     * @throws CcpException If the Claim Compliance Provider endpoint is not set, the claimsList or gxParticipantCredentials are not valid JSON or the asset could not be updated
     */
    public Asset callClaimComplianceProvider(final String claimComplianceProviderEndpoint, final AssetService assetService, final Asset asset) throws CcpException {
        if (claimComplianceProviderEndpoint == null || claimComplianceProviderEndpoint.isEmpty()) {
            throw new CcpException("ClaimComplianceProvider endpoint is not set. Please set a valid value.");
        }

        monitor.debug("Check if asset should be processed with ClaimComplianceProvider ...");
        final String encodedClaimComplianceProviderResponse = (String) asset.getProperty(EDC_NAMESPACE + CLAIM_COMPLIANCE_PROVIDER_RESPONSE_FIELD_NAME);
        final String encodedClaimsList = (String) asset.getProperty(EDC_NAMESPACE + CLAIMS_LIST_FIELD_NAME);
        final String encodedGxParticipantCredentials = (String) asset.getProperty(EDC_NAMESPACE + GX_PARTICIPANT_CREDENTIALS_FIELD_NAME);

        final String decodedClaimComplianceProviderResponse = decodeBase64(encodedClaimComplianceProviderResponse);
        final String decodedClaimsList = decodeBase64(encodedClaimsList);
        final String decodedGxParticipantCredentials = decodeBase64(encodedGxParticipantCredentials);

        if (decodedClaimComplianceProviderResponse == null || decodedClaimComplianceProviderResponse.isEmpty()) {
            if (isValidJson(decodedClaimsList) && isValidJson(decodedGxParticipantCredentials)) {
                monitor.info("Calling ClaimComplianceProvider ...");

                final String claimsListJson = getRawJson(decodedClaimsList);
                final String gxParticipantCredentialsJson = getRawJson(decodedGxParticipantCredentials);
                final String response = getCcpCallingService(claimComplianceProviderEndpoint, monitor).executeClaimComplianceProviderCall(claimsListJson, gxParticipantCredentialsJson);
                monitor.info("Updating asset with successful ccp response.");
                return updateAsset(assetService, asset, response);
            } else {
                throw new CcpException("Asset cannot not be processed with ClaimComplianceProvider since claimsList or gxParticipantCredentials is not valid JSON.");
            }
        } else {
            monitor.info("Asset will not be processed with ClaimComplianceProvider since there is already a response in the asset.");
        }
        return asset;
    }

    private Asset updateAsset(final AssetService assetService, final Asset asset, final String ccpResponse) throws CcpException {
        asset.getProperties().put(EDC_NAMESPACE + CLAIM_COMPLIANCE_PROVIDER_RESPONSE_FIELD_NAME, Base64.getEncoder().encodeToString(ccpResponse.getBytes()));
        final ServiceResult<Asset> updatedAsset = assetService.update(asset);
        if (updatedAsset.succeeded()) {
            monitor.info("Updated asset with CCP response.");
            return updatedAsset.getContent();
        } else {
            throw new CcpException("Error while updating asset with CCP response: " + updatedAsset.getFailureDetail());
        }
    }

    private String decodeBase64(final String encodedString) {
        return encodedString != null ? new String(Base64.getDecoder().decode(encodedString)) : null;
    }

    private boolean isValidJson(final String jsonString) {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            final JsonNode node = objectMapper.readTree(jsonString);
            return node.isObject() || node.isArray();
        } catch (Exception e) {
            monitor.warning("Invalid JSON string detected: " + jsonString);
            return false;
        }
    }

    private String getRawJson(final String jsonString) throws CcpException{
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode node;
        try {
            node = objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new CcpException("Failed to parse JSON string", e);
        }
        return node.toString();
    }

    private CcpCallingService getCcpCallingService(final String url, final Monitor monitor) {
        if (this.ccpCallingService == null) {
            this.ccpCallingService = new CcpCallingService(url, monitor);
        }
        return this.ccpCallingService;
    }
}
