package berlin.tu.ise.extension.blockchain.catalog.listener;

import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import berlin.tu.ise.extension.blockchain.catalog.listener.model.TokenizedContract;
import berlin.tu.ise.extension.blockchain.catalog.listener.model.TokenizedObject;
import berlin.tu.ise.extension.blockchain.catalog.listener.model.TokenizedPolicyDefinition;
import berlin.tu.ise.extension.blockchain.catalog.listener.model.TokenziedAsset;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

    public ReturnObject sendToAssetSmartContract(String jsonString) {
        return sendToSmartContract(jsonString, edcInterfaceUrl + "/mint/asset");
    }

    public ReturnObject sendToPolicySmartContract(String jsonString) {
        return sendToSmartContract(jsonString, edcInterfaceUrl + "/mint/policy");
    }

    public ReturnObject sendToContractSmartContract(String jsonString) {
        return sendToSmartContract(jsonString, edcInterfaceUrl + "/mint/contract");
    }

    public ReturnObject sendToVerifiableSmartContract(String jsonString) {
        return sendToSmartContract(jsonString, edcInterfaceUrl + "/mint/verifiable_credentials");
    }

    /** Send a JSON string to a smart contract.
     *
     * @param jsonString The JSON string to send
     * @param smartContractUrl The URL of the smart contract
     * @return The return object
     */
    public ReturnObject sendToSmartContract(String jsonString, String smartContractUrl) {
        monitor.debug(String.format("[%s] Sending data to Smart Contract, this may take some time ...", BlockchainSmartContractService.class.getSimpleName()));
        String returnJson;
        ReturnObject returnObject = null;
        try {
            URL url = new URL(smartContractUrl);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
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
        } catch (Exception e) {
            monitor.severe(e.toString());
        }

        return returnObject;
    }

    /** Get all contract definitions from the smart contract.
     *
     * @return List of ContractDefinitionResponseDto
     */
    public List<ContractDefinition> getAllContractDefinitionsFromSmartContract() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<TokenizedContract> tokenziedContractList;
        List<ContractDefinition> contractOfferDtoList = new ArrayList<>();

        HttpURLConnection c = null;
        try {
            URL u = new URL(edcInterfaceUrl + "/all/edccontract");
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.connect();
            int status = c.getResponseCode();

            if (status != 200 && status != 201) {
                monitor.warning("Failed to fetch contracts from edc-interface with status code: " + status);
                return null;
            }


            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            tokenziedContractList = mapper.readValue(sb.toString(), new TypeReference<List<TokenizedContract>>() {});

            for (TokenizedContract tokenizedContract : tokenziedContractList) {
                if (tokenizedContract == null) {
                    monitor.warning("TokenizedContractDefinition is null");
                    continue;
                }

                JsonObject contractAsExpandedJson = jsonLd.expand(tokenizedContract.getTokenData()).getContent();

                if (contractAsExpandedJson == null) {
                    monitor.warning("TokenizedContractDefinition does not contain tokenData");
                    continue;
                }

                if (contractAsExpandedJson.getString("@id") == null) {
                    monitor.warning("TokenizedContractDefinition does not contain @id");
                    continue;
                }

                monitor.debug("Validating contract definition: " + contractAsExpandedJson.getString("@id"));
                try {


                    ValidationResult result = validatorRegistry.validate(ContractDefinition.CONTRACT_DEFINITION_TYPE, contractAsExpandedJson);

                    if (result.failed()) {
                        monitor.warning("Validation failed for contract definition with message: " + result.getFailureDetail());
                        continue;
                    }


                    var contract = transformerRegistry.transform(contractAsExpandedJson, ContractDefinition.class)
                            .orElseThrow(InvalidRequestException::new);

                    contractOfferDtoList.add(contract);

                } catch (ValidationFailureException vex) {
                    monitor.warning("Validation failed for contract definition with message: " + vex.getMessage());
                    continue;
                } catch (InvalidRequestException irex) {
                    monitor.warning("Transformation failed for contract definition with message: " + irex.getMessage());
                    continue;
                }

            }
            monitor.info("Validation failed for " + (tokenziedContractList.size() - contractOfferDtoList.size()) + " contract definitions and succeeded for " + contractOfferDtoList.size() + " contract definitions");


        } catch (IOException ex) {
            monitor.warning(ex.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    monitor.warning(ex.getMessage());
                }
            }
        }
        return contractOfferDtoList;
    }



    /** Get all contract definitions from the smart contract and group them by source.
     *
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
            URL u = new URL(edcInterfaceUrl + "/all/edccontract");
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

                    for (TokenizedContract tokenizedContract : tokenziedContractList) {
                        if (tokenizedContract != null) {

                            if (tokenizedContract != null && tokenizedContract.getTokenData() != null &&
                                    tokenizedContract.getTokenData().containsKey("@id")) {
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
                                    if (!returnMap.containsKey(contract.getCounterPartyAddress())) {
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
                default:
                    return null;
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

    private ValidationResult isJsonObjectAnValidAsset(JsonObject assetAsJson) {
        if (!assetAsJson.containsKey("@id")) {
            monitor.warning("TokenizedAsset does not contain @id");
            return ValidationResult.failure(new Violation("TokenizedAsset does not contain @id", "@id", assetAsJson));
        }
        monitor.debug("Validating asset: " + assetAsJson.getString("@id"));
        // It is important to expand the json before validation as otherwise the exact paths in the json does not match the schema
        return validatorRegistry.validate(Asset.EDC_ASSET_TYPE, assetAsJson);

    }

    /** Get all policy definitions from the smart contract and group them by source.
     *
     * @return HashMap of Source URIs and Lists of PolicyDefinitionResponseDto
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

            if (status != 200 && status != 201) {
                monitor.warning("Failed to fetch assets from edc-interface with status code: " + status);
                return null;
            }


            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            List<TokenziedAsset> assetList = mapper.readValue(sb.toString(), new TypeReference<>() {
            });

            monitor.debug("Read " + assetList.size() + " assets from edc-interface and going on to validate them");
            int failedCounter = 0;
            for (TokenziedAsset tokenziedAsset : assetList) {

                if (tokenziedAsset == null || tokenziedAsset.getTokenData(jsonLd) == null) {
                    monitor.warning("TokenizedAsset is null? IPFS file to new?");
                    failedCounter++;
                    continue;
                }

                var assetAsExpandedJson = tokenziedAsset.getTokenData(jsonLd);

                ValidationResult result = isJsonObjectAnValidAsset(assetAsExpandedJson);

                if (result.failed()) {
                    monitor.warning("Validation failed for asset with message in result: " + result.getFailureDetail());
                    failedCounter++;
                    continue;
                }

                try {
                    var asset = transformerRegistry.transform(jsonLd.expand(tokenziedAsset.getTokenData(jsonLd)).getContent(), Asset.class)
                            .orElseThrow(InvalidRequestException::new);
                    assetResponseList.add(asset);
                } catch (InvalidRequestException irex) {
                    monitor.warning("Transformation failed for asset " + tokenziedAsset.getTokenData(jsonLd).getString("@id") + " with message: " + irex.getMessage());
                    failedCounter++;
                    continue;
                }

            }

            monitor.info("Validation failed for " + failedCounter + " assets and succeeded for " + assetResponseList.size() + " assets");

        } catch (IOException ex) {
            monitor.warning(ex.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    monitor.warning(ex.getMessage());
                }
            }
        }
        return assetResponseList;

    }

    /** Get all policy definitions from the smart contract
     *
     * @return List of PolicyDefinitionResponseDto
     */
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

            if (status != 200 && status != 201) {
                monitor.warning("Failed to fetch policies from edc-interface with status code: " + status);
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            tokenizedPolicyDefinitionList = mapper.readValue(sb.toString(), new TypeReference<List<TokenizedPolicyDefinition>>(){});
            monitor.debug("Read policies from edc-interface: " + tokenizedPolicyDefinitionList.size() + " policies and going validate them");
            //monitor.debug("Read policies from edc-interface: " + sb.toString());
            for (TokenizedPolicyDefinition tokenizedPolicyDefinition : tokenizedPolicyDefinitionList) {
                if (tokenizedPolicyDefinition == null) {
                    monitor.warning("TokenizedPolicyDefinition is null");
                    continue;
                }
                JsonObject policyAsExpandedJson = tokenizedPolicyDefinition.getTokenData(jsonLd);

                if (policyAsExpandedJson == null) {
                    monitor.warning("TokenizedPolicyDefinition does not contain tokenData");
                    continue;
                }

                if (policyAsExpandedJson.getString("@id") == null) {
                    monitor.warning("TokenizedPolicyDefinition does not contain @id");
                    continue;
                }
                monitor.debug("Validating policy definition: " + policyAsExpandedJson.getString("@id"));

                ValidationResult result = validatorRegistry.validate(PolicyDefinition.EDC_POLICY_DEFINITION_TYPE, policyAsExpandedJson);

                if (result.failed()) {
                    monitor.warning("Validation failed for policy definition with message: " + result.getFailureDetail());
                    continue;
                }

                try {
                    monitor.debug("Transforming policy definition: " + policyAsExpandedJson.getString("@id"));

                    var policyDefinition = transformerRegistry.transform(policyAsExpandedJson, PolicyDefinition.class)
                            .orElseThrow(InvalidRequestException::new);
                    policyDefinitionList.add(policyDefinition);
                } catch (ValidationFailureException vex) {
                    monitor.warning("Validation failed for policy definition with message: " + vex.getMessage());
                    continue;
                } catch (InvalidRequestException irex) {
                    monitor.warning("Transformation failed for policy definition with message: " + irex.getMessage());
                    continue;
                }
            }
            monitor.info("Read " + policyDefinitionList.size() + " policy definitions from edc-interface");

        } catch (IOException ex) {
            monitor.warning(ex.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    monitor.warning(ex.getMessage());
                }
            }
        }
        return policyDefinitionList;
    }
}
