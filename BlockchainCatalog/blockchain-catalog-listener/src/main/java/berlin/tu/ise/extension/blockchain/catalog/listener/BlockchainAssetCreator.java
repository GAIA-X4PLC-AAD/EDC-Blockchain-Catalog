package berlin.tu.ise.extension.blockchain.catalog.listener;



import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.DataAddressDto;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.event.asset.AssetCreated;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BlockchainAssetCreator implements EventSubscriber {


    private final Monitor monitor;
    private int stateCounter;

    private final AssetService assetService;

    private final AssetIndex assetIndex;

    private final String edcInterfaceUrl;

    private final String providerUrl;

    public BlockchainAssetCreator(Monitor monitor, AssetService assetService, AssetIndex assetIndex, String edcInterfaceUrl, String providerUrl) {
        this.monitor = monitor;
        this.assetService = assetService;
        this.assetIndex = assetIndex;
        this.edcInterfaceUrl = edcInterfaceUrl;
        this.stateCounter = 0;
        this.providerUrl = providerUrl;
    }

    @Override
    public void on(Event event) {
        if (!(event instanceof AssetCreated)) {
            return;
        }
        // the event only returns the asset id, so we need to get the asset from the index
        AssetCreated assetCreated = (AssetCreated) event;
        String assetId = assetCreated.getPayload().getAssetId();
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

        ObjectMapper mapper = new ObjectMapper();
        // Get the dataAddress because its not stored in the Asset Object for some reasons ...
        DataAddress dataAddress = assetIndex.resolveForAsset(asset.getId());

        // Adding provider ids (own) url to the asset properties, so that the consumer nows with whom to initiate a contract negotiation
        asset.getProperties().put("asset:provider:url", providerUrl);

        // Using the already created Dto Classes from the Web API Datamangement Extension
        AssetRequestDto assetRequestDto = AssetRequestDto.Builder.newInstance().id(asset.getId()).properties(asset.getProperties()).build();
        DataAddressDto dataAddressDto = DataAddressDto.Builder.newInstance().properties(dataAddress.getProperties()).build();
        AssetEntryDto assetEntryDto = AssetEntryDto.Builder.newInstance().asset(assetRequestDto).dataAddress(dataAddressDto).build();

        String jsonString = "";
        // Format them to JSON and print them for debugging. Change later, for now the system out println looks prettier than using monitor
        try {
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(asset));
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataAddress));
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(assetEntryDto);
            System.out.println(jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return jsonString;
    }







}
