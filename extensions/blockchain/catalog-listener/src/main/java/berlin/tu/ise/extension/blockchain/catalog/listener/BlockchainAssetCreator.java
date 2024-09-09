package berlin.tu.ise.extension.blockchain.catalog.listener;


import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import com.msg.plcaad.edc.ccp.asset.CcpIntegrationForAssetService;
import com.msg.plcaad.edc.ccp.exception.CcpException;
import org.eclipse.edc.connector.api.management.asset.v3.AssetApiController;
import org.eclipse.edc.connector.asset.spi.event.AssetCreated;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;

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
    private final CcpIntegrationForAssetService ccpIntegrationForAssetService;

    public BlockchainAssetCreator(Monitor monitor, AssetIndex assetIndex, AssetService assetService, String edcInterfaceUrl,
                                  String providerUrl, AssetApiController assetApiController, JsonLd jsonLd, BlockchainSmartContractService blockchainSmartContractService, final CcpIntegrationForAssetService ccpIntegrationForAssetService, final String ccpInterfaceUrl) {
        this.monitor = monitor;
        this.assetIndex = assetIndex;
        this.assetService = assetService;
        this.edcInterfaceUrl = edcInterfaceUrl;
        this.providerUrl = providerUrl;
        this.assetApiController = assetApiController;
        this.jsonLd = jsonLd;
        this.blockchainSmartContractService = blockchainSmartContractService;
        this.ccpIntegrationForAssetService = ccpIntegrationForAssetService;
        this.claimComplianceProviderEndpoint = ccpInterfaceUrl;
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
        asset = callClaimComplianceProvider(asset);
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

    private Asset callClaimComplianceProvider(final Asset asset) {
        try {
            return ccpIntegrationForAssetService.callClaimComplianceProvider(claimComplianceProviderEndpoint, assetService, asset);
        } catch (CcpException e) {
            // see issue  https://github.com/GAIA-X4PLC-AAD/EDC-Blockchain-Catalog/issues/13 for proper exception handling.
            this.monitor.severe("Error while processing CCP part: " + e.getMessage(), e);
            return asset;
        }
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
