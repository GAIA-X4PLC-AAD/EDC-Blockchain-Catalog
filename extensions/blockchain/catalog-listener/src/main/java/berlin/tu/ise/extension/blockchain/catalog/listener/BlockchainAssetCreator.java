package berlin.tu.ise.extension.blockchain.catalog.listener;


import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.api.management.asset.v3.AssetApiController;
import org.eclipse.edc.connector.asset.spi.event.AssetCreated;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.util.Base64;

import static berlin.tu.ise.extension.blockchain.catalog.listener.Constants.*;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/** This class listens for AssetCreated events and sends the asset to the blockchain smart contract service. */
public class BlockchainAssetCreator implements EventSubscriber {

    private static final String TU_BERLIN_NS = "https://ise.tu.berlin/edc/v0.0.1/ns/";

    private final Monitor monitor;

    private final AssetIndex assetIndex;

    private final AssetService assetService;

    private final String edcInterfaceUrl;

    private String claimComplianceProviderEndpoint;

    private final String providerUrl;

    private final AssetApiController assetApiController;
    private final JsonLd jsonLd;

    private final BlockchainSmartContractService blockchainSmartContractService;

    public BlockchainAssetCreator(Monitor monitor, AssetIndex assetIndex, AssetService assetService, String edcInterfaceUrl, String providerUrl, AssetApiController assetApiController, JsonLd jsonLd, BlockchainSmartContractService blockchainSmartContractService) {
        this.monitor = monitor;
        this.assetIndex = assetIndex;
        this.assetService = assetService;
        this.edcInterfaceUrl = edcInterfaceUrl;
        this.providerUrl = providerUrl;
        this.assetApiController = assetApiController;
        this.jsonLd = jsonLd;
        this.blockchainSmartContractService = blockchainSmartContractService;
    }

    public BlockchainAssetCreator(Monitor monitor, AssetIndex assetIndex, AssetService assetService, String edcInterfaceUrl, String providerUrl, AssetApiController assetApiController, JsonLd jsonLd, BlockchainSmartContractService blockchainSmartContractService, String claimComplianceProviderEndpoint) {
        this(monitor, assetIndex, assetService, edcInterfaceUrl, providerUrl, assetApiController, jsonLd, blockchainSmartContractService);
        this.claimComplianceProviderEndpoint = claimComplianceProviderEndpoint;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var payload = event.getPayload();
        if (!(payload instanceof AssetCreated)) return;

        // the event only returns the asset id, so we need to get the asset from the index
        AssetCreated assetCreated = (AssetCreated) payload;
        String assetId = assetCreated.getAssetId();
        monitor.debug("AssetCreated event triggered for assetId: " + assetId);
        Asset asset = assetIndex.findById(assetId);
        String jsonString = transformToJson(asset);
        ReturnObject returnObject = blockchainSmartContractService.sendToAssetSmartContract(jsonString);
        if (returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Asset creation of the Asset with id " + asset.getId());
            throw new EdcException("Something went wrong during the Blockchain Asset creation of the Asset with id " + asset.getId());
        } else {
            monitor.debug(String.format("[%s] Created Asset %s and minted it successfully with the hash: %s", this.getClass().getSimpleName(), asset.getId(), returnObject.getHash()));
            var properties = asset.getProperties();
            properties.put(TU_BERLIN_NS + "blockchainhashvalue", returnObject.getHash());
            asset.toBuilder().properties(properties).build();
            var result = assetIndex.updateAsset(asset);
            monitor.debug("Updated asset with blockchain hash: " + result);
        }
    }

    private Asset processAssetWithClaimComplianceProvider(final Asset asset) {
        try {
            if (this.claimComplianceProviderEndpoint == null || this.claimComplianceProviderEndpoint.isEmpty()) {
                monitor.info("ClaimComplianceProvider endpoint is not set. Skipping processing asset with ClaimComplianceProvider.");
                return asset;
            }

            monitor.info("Check if asset should be processed with ClaimComplianceProvider ...");
            final String encodedClaimComplianceProviderResponse = (String) asset.getProperty(EDC_NAMESPACE + CLAIM_COMPLIANCE_PROVIDER_RESPONSE_FIELD_NAME);
            final String encodedClaimsList = (String) asset.getProperty(EDC_NAMESPACE + CLAIMS_LIST_FIELD_NAME);
            final String encodedGxParticipantCredentials = (String) asset.getProperty(EDC_NAMESPACE + GX_PARTICIPANT_CREDENTIALS_FIELD_NAME);

            final String decodedClaimComplianceProviderResponse = decodeBase64(encodedClaimComplianceProviderResponse);
            final String decodedClaimsList = decodeBase64(encodedClaimsList);
            final String decodedGxParticipantCredentials = decodeBase64(encodedGxParticipantCredentials);

            if ((decodedClaimComplianceProviderResponse == null || decodedClaimComplianceProviderResponse.isEmpty()) &&
                    isValidJson(decodedClaimsList) && isValidJson(decodedGxParticipantCredentials)) {
                monitor.info("Calling ClaimComplianceProvider ...");

                final String claimsListJson = getRawJson(decodedClaimsList);
                final String gxParticipantCredentialsJson = getRawJson(decodedGxParticipantCredentials);
                final String response = callComplianceProvider(claimsListJson, gxParticipantCredentialsJson);
                monitor.info("Updating asset with successful ccp response.");
                return updateAsset(asset, response);
            } else {
                monitor.info("Asset will not / cannot be processed with ClaimComplianceProvider.");
            }
        } catch (Exception e) {
            this.monitor.severe("Error while processing CCP part", e);
            return asset;
        }
        return asset;
    }

    private Asset updateAsset(final Asset asset, final String ccpResponse) {
        asset.getProperties().put(EDC_NAMESPACE + CLAIM_COMPLIANCE_PROVIDER_RESPONSE_FIELD_NAME, Base64.getEncoder().encodeToString(ccpResponse.getBytes()));
        final ServiceResult<Asset> updatedAsset = assetService.update(asset);
        if (updatedAsset.succeeded()) {
            monitor.info("Updated asset with CCP response.");
            return updatedAsset.getContent();
        } else {
            monitor.severe("Error while updating asset with CCP response: " + updatedAsset.getFailureDetail());
        }

        return updatedAsset.getContent();
    }

    private String callComplianceProvider(final String decodedClaimsList, final String decodedGxParticipantCredentials) throws CcpRequestException{
        return ClaimComplianceProviderService.callClaimComplianceProvider(this.claimComplianceProviderEndpoint,
                decodedClaimsList, decodedGxParticipantCredentials, this.monitor);
    }

    private String decodeBase64(String encodedString) {
        return encodedString != null ? new String(Base64.getDecoder().decode(encodedString)) : null;
    }

    private boolean isValidJson(String jsonString) {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode node = objectMapper.readTree(jsonString);
            return node.isObject() || node.isArray();
        } catch (Exception e) {
            monitor.warning("Invalid JSON string detected: " + jsonString);
            return false;
        }
    }

    private String getRawJson(String jsonString) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(jsonString);
        return node.toString();
    }

    private String transformToJson(Asset asset) {
        monitor.info(String.format("[%s] Asset: '%s' created in EDC, start now with Blockchain related steps ...", this.getClass().getSimpleName(), asset.getName()));

        monitor.info(String.format("[%s] formating POJO to JSON ...", this.getClass().getSimpleName()));

        // Adding provider (own) url as the asset propertie originator, so that the consumer knows with whom to initiate a contract negotiation
        // we just make sure it always exist because the datadashboard is currenlty not able to reflect this via the integrated asset creation tool
        if (!asset.getProperties().containsKey(EDC_NAMESPACE + "originator")) {
            asset.getProperties().put(EDC_NAMESPACE + "originator", providerUrl);
        }
        var jsonAsset = jsonLd.compact(assetApiController.getAsset(asset.getId())).getContent().toString();
        monitor.debug(String.format("[%s] formatted POJO to JSON: %s", this.getClass().getSimpleName(), jsonAsset));

        return jsonAsset;
    }
}
