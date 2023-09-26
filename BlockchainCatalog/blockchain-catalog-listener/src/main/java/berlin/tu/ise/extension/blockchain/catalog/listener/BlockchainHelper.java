package berlin.tu.ise.extension.blockchain.catalog.listener;

import berlin.tu.ise.extension.blockchain.catalog.listener.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.Policy;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.eclipse.edc.connector.api.management.asset.v3.AssetApiController;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
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

import static org.eclipse.edc.spi.types.domain.asset.Asset.EDC_ASSET_TYPE;

public class BlockchainHelper {

    public static ReturnObject sendToAssetSmartContract(String jsonString, Monitor monitor, String edcInterfaceUrl) {
        return sendToSmartContract(jsonString, monitor, edcInterfaceUrl + "/mint/asset");
    }

    public static ReturnObject sendToPolicySmartContract(String jsonString, Monitor monitor, String edcInterfaceUrl) {
        return sendToSmartContract(jsonString, monitor, edcInterfaceUrl + "/mint/policy");
    }

    public static ReturnObject sendToContractSmartContract(String jsonString, Monitor monitor, String edcInterfaceUrl) {
        return sendToSmartContract(jsonString, monitor, edcInterfaceUrl + "/mint/contract");
    }

    public static ReturnObject sendToSmartContract(String jsonString, Monitor monitor, String smartContractUrl) {
        monitor.debug(String.format("[%s] Sending data to Smart Contract, this may take some time ...", BlockchainHelper.class.getSimpleName()));
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

    public static Asset getAssetWithIdFromSmartContract(String id, String edcInterfaceUrl) {
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

                    return mapper.readValue(sb.toString(), TokenziedAsset.class).getTokenData();
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

    public static List<ContractDefinition> getAllContractDefinitionsFromSmartContract(String edcInterfaceUrl, Monitor monitor, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<TokenizedObject> tokenziedContractList;
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

                    tokenziedContractList = mapper.readValue(sb.toString(), new TypeReference<>() {
                    });

                    /*
                    for (TokenizedObject tokenizedContract: tokenziedContractList) {
                        if(tokenizedContract != null) {
                            validatorRegistry.validate(ContractDefinition.CONTRACT_DEFINITION_TYPE, tokenizedContract.tokenData).orElseThrow(ValidationFailureException::new);

                            var contract = transformerRegistry.transform(tokenizedContract.tokenData, ContractDefinition.class)
                                    .orElseThrow(InvalidRequestException::new);
                            contractOfferDtoList.add(contract);
                        }

                    }
                    */

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
    public static HashMap<String, List<ContractOfferMessage>> getAllContractDefinitionsFromSmartContractGroupedBySource(String edcInterfaceUrl) {
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

                    tokenziedContractList = mapper.readValue(sb.toString(), new TypeReference<>() {
                    });

                    for (TokenizedContract tokenizedContract: tokenziedContractList) {
                        if(tokenizedContract != null) {
                            // add to returnMap with source as key
                            if(!returnMap.containsKey(tokenizedContract.getSource())) {
                                returnMap.put(tokenizedContract.getSource(), new ArrayList<>());
                            }
                            returnMap.get(tokenizedContract.getSource()).add(tokenizedContract.getTokenData());
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

    public static List<Asset> getAllAssetsFromSmartContract(String edcInterfaceUrl, Monitor monitor, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry) {
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

                    monitor.debug("Read from edc-interface: " + sb.toString() + " and going on to map it to a list of TokenizedObjects");
                    tokenziedAssetList = mapper.readValue(sb.toString(), new TypeReference<List<TokenizedObject>>(){});
                    monitor.debug("Read " + tokenziedAssetList.size() + " assets from edc-interface and going on to validate them");
                    int failedCounter = 0;
                    for (TokenizedObject tokenizedAsset: tokenziedAssetList) {
                        Asset asset = castJsonObjectToAsset(validatorRegistry, transformerRegistry, tokenizedAsset, monitor);
                        if(asset != null) {
                            assetResponseList.add(asset);
                        } else {
                            failedCounter++;
                        }
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

    private static Asset castJsonObjectToAsset( JsonObjectValidatorRegistry validatorRegistry, TypeTransformerRegistry transformerRegistry, TokenizedObject tokenizedAsset, Monitor monitor) {

        if(tokenizedAsset == null) {
            return null;
        }
        // monitor.debug("Validating asset " + tokenizedAsset.getTokenData() + " with " + EDC_ASSET_TYPE);

        JsonObject jsonObject = tokenizedAsset.getTokenData(); // Directly get the JsonObject
        monitor.debug("Using JsonObject: " + jsonObject.toString());
        ValidationResult result = validatorRegistry.validate(EDC_ASSET_TYPE, jsonObject);
        monitor.debug(result.toString());
    /*
    try {
            ;
        } catch (ValidationFailureException ex) {
            monitor.warning("Validation failed for asset " + ex.getMessage());
            //ex.printStackTrace();
            return null;
        } catch (Exception ex) {
            monitor.warning("Validation failed for asset with unexpected error " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
        */

        monitor.debug("Validation succeeded for asset " + jsonObject.toString() + " and going on to transform it");

        var asset = transformerRegistry.transform(jsonObject, Asset.class)
                .orElseThrow(InvalidRequestException::new);

        monitor.debug("Transformation succeeded for asset " + asset.getId() + " and going on to add it to the list of assets");
        return asset;
    }

    public static List<PolicyDefinition> getAllPolicyDefinitionsFromSmartContract(String edcInterfaceUrl, Monitor monitor, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry) {
        ObjectMapper mapper = new ObjectMapper();

        List<TokenizedObject> tokenizedPolicyDefinitionList = new ArrayList<>();
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

                    tokenizedPolicyDefinitionList = mapper.readValue(sb.toString(), new TypeReference<List<TokenizedObject>>(){});

                    /*
                    for (TokenizedObject tokenizedPolicyDefinition: tokenizedPolicyDefinitionList) {
                        if(tokenizedPolicyDefinition != null) {
                            validatorRegistry.validate(PolicyDefinition.EDC_POLICY_DEFINITION_TYPE, tokenizedPolicyDefinition.tokenData).orElseThrow(ValidationFailureException::new);

                            var policyDefinition = transformerRegistry.transform(tokenizedPolicyDefinition.tokenData, PolicyDefinition.class)
                                    .orElseThrow(InvalidRequestException::new);
                            policyDefinitionList.add(policyDefinition);
                        }
                    }

                     */

                    return policyDefinitionList;
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
}
