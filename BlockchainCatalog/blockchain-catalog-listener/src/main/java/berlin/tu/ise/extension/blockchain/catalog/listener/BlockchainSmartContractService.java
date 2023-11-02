package berlin.tu.ise.extension.blockchain.catalog.listener;

import berlin.tu.ise.extension.blockchain.catalog.listener.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.*;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlockchainSmartContractService {

    private final Monitor monitor;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validatorRegistry;
    private final JsonLd jsonLd;

    private final String edcInterfaceUrl;

    public BlockchainSmartContractService(Monitor monitor, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry, JsonLd jsonLd, String edcInterfaceUrl) {
        this.monitor = monitor;
        this.transformerRegistry = transformerRegistry;
        this.validatorRegistry = validatorRegistry;
        this.jsonLd = jsonLd;
        this.edcInterfaceUrl = edcInterfaceUrl;
    }

    public ReturnObject sendToAssetSmartContract(String jsonString ) {
        return sendToSmartContract(jsonString, edcInterfaceUrl + "/mint/asset");
    }

    public ReturnObject sendToPolicySmartContract(String jsonString) {
        return sendToSmartContract(jsonString, edcInterfaceUrl + "/mint/policy");
    }

    public ReturnObject sendToContractSmartContract(String jsonString) {
        return sendToSmartContract(jsonString, edcInterfaceUrl + "/mint/contract");
    }

    public ReturnObject sendToSmartContract(String jsonString, String smartContractUrl) {
        monitor.debug(String.format("[%s] Sending data to Smart Contract, this may take some time ...", BlockchainSmartContractService.class.getSimpleName()));
        String returnJson;
        ReturnObject returnObject = null;
        try{
            URL url = new URL(smartContractUrl);
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

            while ((returnJson = br.readLine()) != null) {
                System.out.println(returnJson);
                ObjectMapper mapper = new ObjectMapper();
                returnObject = mapper.readValue(returnJson, ReturnObject.class);
            }

            System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
            http.disconnect();
        } catch(Exception e) {
            monitor.severe(e.toString());
        }

        return returnObject;
    }

    public Asset getAssetWithIdFromSmartContract(String id, String edcInterfaceUrl) {
        Asset asset = null;
        ObjectMapper mapper = new ObjectMapper();

        HttpURLConnection c = null;
        try {
            URL u = new URL(edcInterfaceUrl + "/asset/"+id);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();

                    return mapper.readValue(sb.toString(), TokenziedAsset.class).getTokenDataAsAsset();
            }

        } catch (IOException ex) {
           System.out.println(ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
        return null;
    }

    public List<ContractDefinition> getAllContractDefinitionsFromSmartContract() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<TokenizedContract> tokenziedContractList;
        List<ContractDefinition> contractOfferDtoList = new ArrayList<>();

        HttpURLConnection c = null;
        try {
            URL u = new URL(edcInterfaceUrl + "/all/contract");
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();

                    System.out.println(sb);

                    tokenziedContractList = mapper.readValue(sb.toString(), new TypeReference<List<TokenizedContract>>() {});

                    for (TokenizedContract tokenizedContract: tokenziedContractList) {
                        if(tokenizedContract != null) {

                            if(tokenizedContract != null && tokenizedContract.getTokenData() != null
                                    && tokenizedContract.getTokenData().containsKey("@id")) {
                                if (!tokenizedContract.getTokenData().containsKey("@context")) {
                                    monitor.warning("TokenizedContractDefinition " + tokenizedContract.getTokenData().getString("@id") + " does not contain @context - Skipping");
                                    continue;
                                }
                                monitor.debug("Validating contract definition: " + tokenizedContract.getTokenData());
                                try {
                                    /*
                                    var preTransformedPolicyDefinition = tokenizedContract.getTokenData();

                                    JsonValue policyNode = preTransformedPolicyDefinition.get("edc:policy");
                                    if (policyNode.getValueType() == JsonValue.ValueType.OBJECT) {
                                        JsonArray policyArray = Json.createArrayBuilder().add(policyNode).build();
                                        preTransformedPolicyDefinition = Json.createObjectBuilder(preTransformedPolicyDefinition)
                                                .remove("edc:policy")
                                                .add("edc:policy", policyArray)
                                                .build();
                                    }

                                     */

                                    //validatorRegistry.validate(ContractDefinition.CONTRACT_DEFINITION_TYPE, tokenizedContract.getTokenData()).orElseThrow(ValidationFailureException::new);
                                    //monitor.debug("Faulty Policy detection: " + tokenizedContract.getTokenData().getJsonArray("edc:assetsSelector").getJsonObject(0).getJsonObject("edc:operator").toString());

                                    if (tokenizedContract.getTokenData().containsKey("edc:assetsSelector")
                                            && tokenizedContract.getTokenData().get("edc:assetsSelector").getValueType() == JsonValue.ValueType.ARRAY
                                            && tokenizedContract.getTokenData().getJsonArray("edc:assetsSelector").size() > 0
                                            && tokenizedContract.getTokenData().getJsonArray("edc:assetsSelector").getJsonObject(0).containsKey("edc:operator")
                                            && tokenizedContract.getTokenData().getJsonArray("edc:assetsSelector").getJsonObject(0).getString("edc:operator").toString().equals("in")) {
                                        monitor.debug("Contract definition contains 'in' operator instead of '='. Skipping as not supported");
                                        continue;
                                    }
                                    var jsonContract = jsonLd.expand(tokenizedContract.getTokenData()).getContent();
                                    monitor.debug("Expanded contract definition: " + jsonContract.toString());
                                    var contract = transformerRegistry.transform(jsonContract, ContractDefinition.class)
                                            .orElseThrow(InvalidRequestException::new);

                                    contractOfferDtoList.add(contract);

                                } catch (ValidationFailureException vex) {
                                    monitor.warning("Validation failed for contract definition with message: " + vex.getMessage());
                                    continue;
                                } catch (InvalidRequestException irex) {
                                    monitor.warning("Transformation failed for contract definition with message: " + irex.getMessage());
                                    continue;
                                }
                            } else {
                                monitor.warning("TokenizedContractDefinition is null or does not contain @id");
                            }


                        }

                    }
                    monitor.debug("Validation failed for " + (tokenziedContractList.size() - contractOfferDtoList.size()) + " contract definitions and succeeded for " + contractOfferDtoList.size() + " contract definitions");
                    return contractOfferDtoList;
            }


        } catch (MalformedURLException ex) {
            System.out.println(ex);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
        return null;
    }



    /**
     * @return HashMap of Source URIs and Lists of ContractDefinitionResponseDto
     */
    public HashMap<String, List<ContractOfferMessage>> getAllContractDefinitionsFromSmartContractGroupedBySource(String edcInterfaceUrl, Monitor monitor, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry) {
        HashMap<String, List<ContractOfferMessage>> returnMap = new HashMap<>();
        ContractOfferMessage contractDefinitionResponseDto = null;
        ObjectMapper mapper = new ObjectMapper();

        List<TokenizedContract> tokenziedContractList;
        List<ContractOfferMessage> contractDefinitionResponseDtoList = new ArrayList<>();

        HttpURLConnection c = null;
        try {
            URL u = new URL(edcInterfaceUrl + "/all/contract");
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();

                    tokenziedContractList = mapper.readValue(sb.toString(), new TypeReference<List<TokenizedContract>>() {
                    });

                    for (TokenizedContract tokenizedContract: tokenziedContractList) {
                        if(tokenizedContract != null) {

                            if(tokenizedContract != null && tokenizedContract.getTokenData() != null
                                    && tokenizedContract.getTokenData().containsKey("@id")) {
                                if (!tokenizedContract.getTokenData().containsKey("@context")) {
                                    monitor.warning("TokenizedContractDefinition " + tokenizedContract.getTokenData().getString("@id") + " does not contain @context - Skipping");
                                    continue;
                                }
                                try {
                                    /*
                                    var preTransformedPolicyDefinition = tokenizedContract.getTokenData();

                                    JsonValue policyNode = preTransformedPolicyDefinition.get("edc:policy");
                                    if (policyNode.getValueType() == JsonValue.ValueType.OBJECT) {
                                        JsonArray policyArray = Json.createArrayBuilder().add(policyNode).build();
                                        preTransformedPolicyDefinition = Json.createObjectBuilder(preTransformedPolicyDefinition)
                                                .remove("edc:policy")
                                                .add("edc:policy", policyArray)
                                                .build();
                                    }

                                     */

                                    validatorRegistry.validate(PolicyDefinition.EDC_POLICY_DEFINITION_TYPE, tokenizedContract.getTokenData()).orElseThrow(ValidationFailureException::new);

                                    var contract = transformerRegistry.transform(tokenizedContract.getTokenData(), ContractOfferMessage.class)
                                            .orElseThrow(InvalidRequestException::new);


                                    // add to returnMap with source as key TODO: mocked for testing
                                    if(!returnMap.containsKey(contract.getCounterPartyAddress())) {
                                        returnMap.put(contract.getCounterPartyAddress(), new ArrayList<>());
                                    }
                                    returnMap.get(contract.getCounterPartyAddress()).add(contract);

                                } catch (ValidationFailureException vex) {
                                    monitor.warning("Validation failed for policy definition with message: " + vex.getMessage());
                                    continue;
                                } catch (InvalidRequestException irex) {
                                    monitor.warning("Transformation failed for policy definition with message: " + irex.getMessage());
                                    continue;
                                }
                            } else {
                                monitor.warning("TokenizedPolicyDefinition is null or does not contain @id");
                            }


                        }

                    }

                    return returnMap;
            }


        } catch (MalformedURLException ex) {
            System.out.println(ex);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
        return null;
    }

    /*
    public static PolicyDefinition getPolicyWithIdFromSmartContract(String id, String edcInterfaceUrl) {
        PolicyDefinition policy = null;
        ObjectMapper mapper = new ObjectMapper();

        HttpURLConnection c = null;
        try {
            URL u = new URL(edcInterfaceUrl + "/policy/"+id);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();

                    return mapper.readValue(sb.toString(), TokenizedPolicyDefinition.class).getTokenData();
            }

        } catch (MalformedURLException ex) {
            System.out.println(ex);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
        return null;
    }

     */

    public List<Asset> getAllAssetsFromSmartContract() {
        ObjectMapper mapper = new ObjectMapper();

        List<TokenizedObject> tokenziedAssetList;
        List<Asset> assetResponseList = new ArrayList<>();


        HttpURLConnection c = null;
        try {
            URL u = new URL(edcInterfaceUrl + "/all/asset");
            monitor.debug("Fetching getAllAssetsFromSmartContract from edc-interface " + u);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();

                    //monitor.debug("Read from edc-interface: " + sb.toString() + " and going on to map it to a list of TokenizedObjects");
                    List<TokenziedAsset> assetList = mapper.readValue(sb.toString(), new TypeReference<List<TokenziedAsset>>() {});

                    /*
                        I first tried to use the normal transformer and validator but it didnt work and i dont know why
                        So I will just transform them manually, but its the inferior solution
                     */
                    monitor.debug("Read " + assetList.size() + " assets from edc-interface and going on to validate them");
                    int failedCounter = 0;
                    for (TokenziedAsset tokenziedAsset : assetList) {

                        if (tokenziedAsset == null || tokenziedAsset.tokenData == null) {
                            monitor.warning("TokenizedAsset is null? IPFS file to new?");
                            continue;
                        }
                        if (!tokenziedAsset.tokenData.containsKey("@id")) {
                            monitor.warning("TokenizedAsset does not contain @id");
                            continue;
                        }
                        if (!tokenziedAsset.tokenData.containsKey("@context")) {
                            monitor.warning("TokenziedAsset " + tokenziedAsset.tokenData.getString("@id") + " does not contain @context - Skipping");
                            continue;
                        }
                        /*
                        if(!tokenziedAsset.getTokenData().containsKey("@id")) {
                            monitor.warning("TokenizedAsset does not contain @id");
                            continue;
                        }
                        monitor.debug("Validating asset: " + tokenziedAsset.getTokenData());
                        try {
                            validatorRegistry.validate(EDC_ASSET_TYPE, tokenziedAsset.getTokenData()).orElseThrow(ValidationFailureException::new);
                        } catch (ValidationFailureException vex) {
                            monitor.warning("Validation failed for asset with message: " + vex.getMessage());
                            failedCounter++;
                            continue;
                        } catch (ClassCastException cce) {
                            monitor.warning("We ignore this exception as the validator seems to be buggy: " + cce.getMessage());
                        }

                         */
                        try {
                            var asset = transformerRegistry.transform(jsonLd.expand(tokenziedAsset.tokenData).getContent(), Asset.class)
                                    .orElseThrow(InvalidRequestException::new);
                            assetResponseList.add(asset);
                        } catch (InvalidRequestException irex) {
                            monitor.warning("Transformation failed for asset " + tokenziedAsset.getTokenData().getString("@id") + " with message: " + irex.getMessage());
                            failedCounter++;
                            continue;
                        }
                        /*
                        try {
                            var asset = tokenziedAsset.getTokenDataAsAsset();
                            assetResponseList.add(asset);
                        } catch (IllegalArgumentException iae) {
                            monitor.warning("Transformation failed for asset with message: " + iae.getMessage());
                            failedCounter++;
                            continue;
                        }*/
                    }

                    monitor.debug("Validation failed for " + failedCounter + " assets and succeeded for " + assetResponseList.size() + " assets");

                    return assetResponseList;
            }


        } catch (MalformedURLException ex) {
            System.out.println(ex);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
        return null;

    }


    public List<PolicyDefinition> getAllPolicyDefinitionsFromSmartContract() {
        ObjectMapper mapper = new ObjectMapper();

        List<TokenizedPolicyDefinition> tokenizedPolicyDefinitionList = new ArrayList<>();
        List<PolicyDefinition> policyDefinitionList = new ArrayList<>();

        HttpURLConnection c = null;
        try {
            URL u = new URL(edcInterfaceUrl + "/all/policy");
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line+"\n");
                    }
                    br.close();

                    tokenizedPolicyDefinitionList = mapper.readValue(sb.toString(), new TypeReference<List<TokenizedPolicyDefinition>>(){});
                    monitor.debug("Read policies from edc-interface: " + tokenizedPolicyDefinitionList.size() + " policies and going validate them");
                    //monitor.debug("Read policies from edc-interface: " + sb.toString());
                    for (TokenizedPolicyDefinition tokenizedPolicyDefinition: tokenizedPolicyDefinitionList) {
                        if (tokenizedPolicyDefinition != null) {
                            monitor.debug("Validating policy definition: " + tokenizedPolicyDefinition.getTokenData());
                        }
                        if(tokenizedPolicyDefinition != null && tokenizedPolicyDefinition.getTokenData() != null
                            && tokenizedPolicyDefinition.getTokenData().containsKey("@id")) {
                            if (!tokenizedPolicyDefinition.getTokenData().containsKey("@context")) {
                                monitor.warning("TokenizedPolicyDefinition " + tokenizedPolicyDefinition.getTokenData().getString("@id") + " does not contain @context - Skipping");
                                continue;
                            }
                            try {
                                monitor.debug("Going to validate and transform policy definition: " + tokenizedPolicyDefinition.getTokenData().getString("@id"));
                                var preTransformedPolicyDefinition = tokenizedPolicyDefinition.getTokenData();


                                JsonValue policyNode = preTransformedPolicyDefinition.get("edc:policy");
                                if (policyNode.getValueType() == JsonValue.ValueType.OBJECT) {
                                    JsonArray policyArray = Json.createArrayBuilder().add(policyNode).build();
                                    preTransformedPolicyDefinition = Json.createObjectBuilder(preTransformedPolicyDefinition)
                                            .remove("edc:policy")
                                            .add("edc:policy", policyArray)
                                            .build();
                                }

                                var expandedTokenizedPolicyDefinition = jsonLd.expand(preTransformedPolicyDefinition).getContent();
                                validatorRegistry.validate(PolicyDefinition.EDC_POLICY_DEFINITION_TYPE, expandedTokenizedPolicyDefinition).orElseThrow(ValidationFailureException::new);

                                var policyDefinition = transformerRegistry.transform(expandedTokenizedPolicyDefinition, PolicyDefinition.class)
                                        .orElseThrow(InvalidRequestException::new);
                                policyDefinitionList.add(policyDefinition);
                            } catch (ValidationFailureException vex) {
                                monitor.warning("Validation failed for policy definition with message: " + vex.getMessage());
                                continue;
                            } catch (InvalidRequestException irex) {
                                monitor.warning("Transformation failed for policy definition with message: " + irex.getMessage());
                                continue;
                            }
                        } else {
                            monitor.warning("TokenizedPolicyDefinition is null or does not contain @id");
                        }

                    }
                    monitor.debug("Read " + policyDefinitionList.size() + " policy definitions from edc-interface");
                    return policyDefinitionList;
            }

        } catch (MalformedURLException ex) {
            System.out.println(ex);
        } catch (IOException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        }
        return null;
    }
}
