package berlin.tu.ise.extension.blockchain.catalog.listener;



import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.edc.connector.api.management.asset.model.AssetRequestDto;
import org.eclipse.edc.connector.api.management.asset.model.DataAddressDto;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.observe.asset.AssetListener;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BlockchainAssetCreator implements AssetListener {


    private final Monitor monitor;
    private int stateCounter;

    private final AssetService assetService;

    private final AssetIndex assetIndex;

    public BlockchainAssetCreator(Monitor monitor, AssetService assetService, AssetIndex assetIndex) {
        this.monitor = monitor;
        this.assetService = assetService;
        this.assetIndex = assetIndex;

        this.stateCounter = 0;
    }

    @Override
    public void created(Asset asset) {
        String jsonString = transformToJSON(asset);
        ReturnObject returnObject = BlockchainHelper.sendToAssetSmartContract(jsonString, monitor);
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
