package berlin.tu.ise.extension.blockchain.catalog.listener;



import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.edc.connector.api.management.asset.v3.AssetApiController;
import org.eclipse.edc.connector.asset.spi.event.AssetCreated;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BlockchainAssetCreator implements EventSubscriber {


    private final Monitor monitor;
    private int stateCounter;

    private final AssetService assetService;

    private final AssetIndex assetIndex;

    private final String edcInterfaceUrl;

    private final String providerUrl;

    private final AssetApiController assetApiController;

    public BlockchainAssetCreator(Monitor monitor, AssetService assetService, AssetIndex assetIndex, String edcInterfaceUrl, String providerUrl, AssetApiController assetApiController) {
        this.monitor = monitor;
        this.assetService = assetService;
        this.assetIndex = assetIndex;
        this.edcInterfaceUrl = edcInterfaceUrl;
        this.stateCounter = 0;
        this.providerUrl = providerUrl;
        this.assetApiController = assetApiController;
    }


    @Override
    public <E extends Event> void on(EventEnvelope<E> event){
        var payload = event.getPayload();
        if (!(payload instanceof AssetCreated)) return;

        // the event only returns the asset id, so we need to get the asset from the index
        AssetCreated assetCreated = (AssetCreated) payload;
        String assetId = assetCreated.getAssetId();
        monitor.debug("AssetCreated event triggered for assetId: " + assetId);
        Asset asset = assetIndex.findById(assetId);

        String jsonString = transformToJSON(asset);
        ReturnObject returnObject = BlockchainHelper.sendToAssetSmartContract(jsonString, monitor, edcInterfaceUrl);
        if(returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Asset creation of the Asset with id " + asset.getId());
        } else {
            System.out.printf("[%s] Created Asset %s and minted it successfully with the hash: %s", this.getClass().getSimpleName(), asset.getId(), returnObject.getHash());
        }
    }


    private String transformToJSON(Asset asset) {


        monitor.info(String.format("[%s] Asset: '%s' created in EDC, start now with Blockchain related steps ...", this.getClass().getSimpleName(), asset.getName()));

        monitor.info(String.format("[%s] formating POJO to JSON ...", this.getClass().getSimpleName()));

        /*
        ObjectMapper mapper = new ObjectMapper();
        // Get the dataAddress because its not stored in the Asset Object for some reasons ...
        DataAddress dataAddress = assetIndex.resolveForAsset(asset.getId());
         */
        // Adding provider (own) url as the asset propertie originator, so that the consumer knows with whom to initiate a contract negotiation
        // we just make sure it always exist because the datadashboard is currenlty not able to reflect this via the integrated asset creation tool
        if (!asset.getProperties().containsKey(EDC_NAMESPACE + "originator"))
            asset.getProperties().put(EDC_NAMESPACE + "originator", providerUrl);



        var jsonAsset = String.valueOf(assetApiController.getAsset(asset.getId()));
        monitor.info(String.format("[%s] formatted POJO to JSON: %s", this.getClass().getSimpleName(), jsonAsset));

        return jsonAsset;


    }




}
