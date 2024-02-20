package berlin.tu.ise.extension.blockchain.catalog.listener;

import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import org.eclipse.edc.connector.api.management.contractdefinition.ContractDefinitionApiController;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BlockchainVerifiablePresentationCreator {



    public static void createVerifiablePresentation(ContractDefinition contractDefinition, String hash, String idsWebhookAddress, String edcInterfaceUrl, AssetIndex assetIndex, ContractDefinitionApiController contractDefinitionApiController, JsonLd jsonLd, Monitor monitor, BlockchainSmartContractService blockchainSmartContractService) {

        String jsonString = transformToJSON(contractDefinition, idsWebhookAddress, edcInterfaceUrl, assetIndex, hash);
        monitor.debug("Going to create Verifiable Presentation with following JSON: " + jsonString);
        String returnString = getVerifiablePresentationAsJson(jsonString, edcInterfaceUrl, monitor);

        if(returnString == null) {
            monitor.warning("Something went wrong during the Verifiable Presentation creation for the Contract with id " + contractDefinition.getId());
            return;
        }

        monitor.info("Verifiable Presentation for Contract with id " + contractDefinition.getId() + " created successfully: " + returnString);

        monitor.debug("Sending Verifiable Presentation to Blockchain for Contract with id " + contractDefinition.getId() + " with JSON: " + returnString);

        ReturnObject returnObject = blockchainSmartContractService.sendToVerifiableSmartContract(returnString);

        if (returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Verifiable Presentation creation for the Contract with id " + contractDefinition.getId());
            return;
        }

        monitor.info("Verifiable Presentation for Contract with id " + contractDefinition.getId() + " created successfully: " + returnObject.getStatus() + " " + returnObject.getHash());

    }

    private static String transformToJSON(ContractDefinition contractDefinition, String idsWebhookAddress, String edcInterfaceUrl, AssetIndex assetIndex, String hash) {
        contractDefinition.getId();
        String json = "{\n" +
                "  \"assetId\": \"" + contractDefinition.getId() + "\",\n" +
                "  \"assetTitle\": \"" + hash + "\",\n" +
                "  \"assetDescription\": \"Example Contract\",\n" +
                "  \"assetUrl\": \"" + idsWebhookAddress + "\"\n" +
                "}";
        return json;
    }

    public static String getVerifiablePresentationAsJson(String jsonString, String smartContractUrl, Monitor monitor) {
        monitor.debug(String.format("[%s] Sending data to Smart Contract, this may take some time ...", BlockchainVerifiablePresentationCreator.class.getSimpleName()));
        String returnJson = null;
        try{
            URL url = new URL(smartContractUrl+"/verifiablepresentation");
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setRequestProperty("Content-Type", "application/json");

            byte[] out = jsonString.getBytes(StandardCharsets.UTF_8);

            OutputStream stream = http.getOutputStream();
            stream.write(out);

            BufferedReader br;
            if (100 <= http.getResponseCode() && http.getResponseCode() <= 399) {
                br = new BufferedReader(new InputStreamReader(http.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(http.getErrorStream()));
            }

            returnJson = br.readLine();



            System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
            http.disconnect();
        } catch(Exception e) {
            monitor.severe(e.toString());
        }

        return returnJson;
    }
}
