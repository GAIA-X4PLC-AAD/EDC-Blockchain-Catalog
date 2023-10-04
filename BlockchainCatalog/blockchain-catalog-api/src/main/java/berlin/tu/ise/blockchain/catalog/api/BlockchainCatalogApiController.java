package berlin.tu.ise.blockchain.catalog.api;


import berlin.tu.ise.extension.blockchain.catalog.listener.BlockchainHelper;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.catalog.spi.*;
import org.eclipse.edc.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.edc.connector.api.management.asset.v3.AssetApiController;
import org.eclipse.edc.connector.api.management.contractdefinition.ContractDefinitionApiController;
import org.eclipse.edc.connector.api.management.policy.PolicyDefinitionApiController;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/{a:federatedcatalog|blockchaincatalog}")
public class BlockchainCatalogApiController implements BlockchainCatalogApi {

    private Monitor monitor;

    private String edcBlockchainInterfaceUrl;

    private TypeTransformerRegistry transformerRegistry;
    private JsonObjectValidatorRegistry validatorRegistry;
    private DatasetResolver datasetResolver;

    private DistributionResolver distributionResolver;

    public BlockchainCatalogApiController(Monitor monitor, String edcBlockchainInterfaceUrl, TypeTransformerRegistry transformerRegistry, JsonObjectValidatorRegistry validatorRegistry, DatasetResolver datasetResolver, DistributionResolver distributionResolver) {
        this.monitor = monitor;
        this.edcBlockchainInterfaceUrl = edcBlockchainInterfaceUrl;
        this.transformerRegistry = transformerRegistry;
        this.validatorRegistry = validatorRegistry;
        this.datasetResolver = datasetResolver;
        this.distributionResolver = distributionResolver;
    }



    @Override
    @POST
    public String getCatalog(FederatedCatalogCacheQuery federatedCatalogCacheQuery) {

        /*
        // test expected format

        String json = getTestString();
        return json;

        */



        monitor.info("BlockchainCatalogApiController.getCatalog() called");
        // TODO: send direct request to blockchain?
        List<ContractOffer> offers = new ArrayList<>();
        Criterion criterion = new Criterion("asset:prop:id", "=", "test-document");
        Criterion criterion1 = new Criterion("asset:prop:id", "=", "test-document-64");
        ArrayList<Criterion> queryList = new ArrayList<>();
        queryList.add(criterion);
        queryList.add(criterion1);

        //List<ContractOffer> contractOfferListFromStorage = (List<ContractOffer>) BlockchainHelper.getAllContractDefinitionsFromSmartContract();
        //monitor.info(format("Fetched %d enties from local cataloge storage", contractOfferListFromStorage.size()));

        List<Asset> assetList = BlockchainHelper.getAllAssetsFromSmartContract(edcBlockchainInterfaceUrl, this.monitor, this.transformerRegistry, this.validatorRegistry);
        if (assetList == null) {
            monitor.info(String.format("[%s] No assets found in blockchain. Is something wrong with the edc-interface or is the contract empty?", this.getClass().getSimpleName()));
        } else {
            monitor.info(String.format("[%s] fetched %d Assets from Smart Contract", this.getClass().getSimpleName(), assetList.size()));
        }

        List<PolicyDefinition> policyDefinitionList = BlockchainHelper.getAllPolicyDefinitionsFromSmartContract(edcBlockchainInterfaceUrl, this.monitor, this.transformerRegistry, this.validatorRegistry);
        if (policyDefinitionList == null) {
            monitor.info(String.format("[%s] No policies found in blockchain. Is something wrong with the edc-interface or is the contract empty?", this.getClass().getSimpleName()));
        } else {
            monitor.info(String.format("[%s] fetched %d Policies from Smart Contract", this.getClass().getSimpleName(), policyDefinitionList.size()));
        }

        List<ContractDefinition> contractDefinitionList = BlockchainHelper.getAllContractDefinitionsFromSmartContract(edcBlockchainInterfaceUrl, this.monitor, this.transformerRegistry, this.validatorRegistry);
        if (contractDefinitionList == null) {
            monitor.info(String.format("[%s] No contracts found in blockchain. Is something wrong with the edc-interface or is the contract empty?", this.getClass().getSimpleName()));
        } else {
            monitor.info(String.format("[%s] fetched %d Contracts from Smart Contract", this.getClass().getSimpleName(), contractDefinitionList.size()));
        }

        // HashMap<String, List<ContractOfferDto>> contractDefinitionResponseDtoGroupedBySource = BlockchainHelper.getAllContractDefinitionsFromSmartContractGroupedBySource();

        List<ContractOffer> contractOfferList = new ArrayList<>();

        // iterate over all sources of contractDefinitionResponseDtoGroupedBySource and fetch all contracts for each source



        monitor.debug("-------------------------------------------------");
        for (Asset asset : assetList) {
            monitor.debug("Asset: " + asset.getId());
            monitor.debug("AssetName: " + asset.getName());
            monitor.debug("AssetProperties: " + asset.getProperties().toString());
            monitor.debug("AssetDataAddress: " + asset.getDataAddress().toString());
        }
        for (PolicyDefinition policyDefinition : policyDefinitionList) {
            monitor.debug("Policy: " + policyDefinition.getId());
            monitor.debug("PolicyTarget: " + policyDefinition.getPolicy().getTarget());
        }
        for (ContractDefinition contractDefinition : contractDefinitionList) {
            monitor.debug("Contract: " + contractDefinition.getId());
            monitor.debug("ContractPolicyId: " + contractDefinition.getContractPolicyId());
            monitor.debug("ContractCriteria: " + contractDefinition.getAccessPolicyId());
        }


        // iterate over all contracts for a source
        assert contractDefinitionList != null;
        for (ContractDefinition contract : contractDefinitionList) {

            monitor.info(format("[%s] fetching contract %s", this.getClass().getSimpleName(), contract.getId()));


            // TODO: Refactor - connect everything together
            ContractOffer contractOffer = getContractOfferFromContractDefinitionDto(contract, assetList, policyDefinitionList);
            if (contractOffer != null) {
                contractOfferList.add(contractOffer);
            }

        }

        monitor.info(format("Fetched %d offers from blockchain", contractOfferList.size()));

        monitor.info(format("Going to create DCAT Datasets from Offers"));

        var catalogBuilder = Catalog.Builder.newInstance()
                .id("blockchain-catalog")
                .contractOffers(contractOfferList);

        List<Dataset> datasetList = new ArrayList<>();
        for (ContractOffer contractOffer : contractOfferList) {
            var asset = getAssetById(contractOffer.getAssetId(), assetList);
            var distributions = distributionResolver.getDistributions(asset, asset.getDataAddress());
            Dataset dataset = Dataset.Builder.newInstance()
                    .id(asset.getId())
                    .offer(contractOffer.getId(), contractOffer.getPolicy())
                    .distributions(distributions)
                    .properties(asset.getProperties())
                    .build();
            datasetList.add(dataset);
        }
        catalogBuilder.datasets(datasetList);

        DataService dataService = DataService.Builder.newInstance()
                .id("blockchain-data-service")
                .terms("blockchain-data-service-terms")
                .endpointUrl(edcBlockchainInterfaceUrl)
                .build();
        catalogBuilder.dataService(dataService);
        Catalog catalog = catalogBuilder.build();


        var catalogJsonObject = transformerRegistry.transform(catalog, JsonObject.class)
                .orElseThrow(InvalidRequestException::new);
        monitor.debug("Catalog as JSON: " + catalogJsonObject.toString());

        return catalogJsonObject.toString();
    }

    /**
     * Connecting the actual Asset Objects with Policy Objects to the ContractOffer Object
     * @param contract the contract to be created
     * @param assetList the list of all existing assets in the blockchain
     * @param policyDefinitionList the list of all existing policies in the blockchain
     * @return ContractOffer created from combining the Asset and Policy Objects identified by the ids in the ContractDefinitionResponseDto, null if not found
     */

    private ContractOffer getContractOfferFromContractDefinitionDto(ContractDefinition contract, List<Asset> assetList, List<PolicyDefinition> policyDefinitionList) {


        String assetId = getAssetIdFromCriterions(contract, assetList, "name");


        /* Fix for EDC Dashboard?
        if (contract.getCriteria().get(0).getOperandRight() instanceof ArrayList) {
            assetId = String.valueOf(((ArrayList<?>) contract.getCriteria().get(0).getOperandRight()).get(0));
        }
         */

        String policyId = contract.getContractPolicyId();
        Asset contractAsset = getAssetByName(assetId, assetList);

        PolicyDefinition policyDefinition = getPolicyById(policyId, policyDefinitionList);


        if (contractAsset == null || policyDefinition == null) {
            monitor.severe(String.format("[%s] Not able to find the Asset with id %s or policy with id %s for the contract %s", this.getClass().getSimpleName(), assetId, policyId, contract.getId()));
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeYearsFronNow = LocalDateTime.now().plusYears(3);
        // convert LocalDateTime to ZonedDateTime, with default system zone id
        ZonedDateTime nowZonedDateTime = now.atZone(ZoneId.systemDefault());
        ZonedDateTime threeYearsFronNowZonedDateTime = threeYearsFronNow.atZone(ZoneId.systemDefault());


        //Asset asset = Asset.Builder.newInstance().id(contractAsset.getId()).properties(contractAsset.getProperties()).build();
        Policy policy = policyDefinition.getPolicy();

        String providerUrl = "http://unavailable:8080";
        if (contractAsset.getProperties().get("asset:prop:originator") != null) {
            providerUrl = contractAsset.getProperties().get("asset:prop:originator").toString();
        }
        return  ContractOffer.Builder.newInstance()
                .assetId(contractAsset.getId())
                .policy(policy)
                .id(contract.getId())
                //.providerId(URI.create(providerUrl).toString())
                .build();
    }

    // check the contract criteria for the asset id to find the asset this contract is about
    // better to check all available criterias and left and right operands if the content is "name" and then we know that the other side is the asset id
    private String getAssetIdFromCriterions(ContractDefinition contract, List<Asset> assetList, String assetIdPropertyName) {

        for (Criterion criterion: contract.getAssetsSelector()) {
            if (criterion.getOperandLeft().equals(assetIdPropertyName)) {
                return String.valueOf(criterion.getOperandRight());
            } else if (criterion.getOperandRight().equals(assetIdPropertyName)) {
                return String.valueOf(criterion.getOperandLeft());
            }
        }
        return null;
    }

    private PolicyDefinition getPolicyById(String policyId, List<PolicyDefinition> policyDefinitionResponseDtoList) {
        for (PolicyDefinition p: policyDefinitionResponseDtoList) {
            if(p.getId().equals(policyId)) {
                return p;
            }
        }
        return null;
    }

    private Asset getAssetById(String assetId, List<Asset> assetEntryDtoList) {
        for (Asset a: assetEntryDtoList) {
            if(a.getId().equals(assetId)) {
                return a;
            }
        }
        return null;
    }

    private Asset getAssetByName(String assetId, List<Asset> assetEntryDtoList) {
        for (Asset a: assetEntryDtoList) {
            if(a.getName().equals(assetId)) {
                return a;
            }
        }
        return null;
    }

    private static String getTestString() {
        return "{\n" +
                "  \"contractOffers\": [\n" +
                "    {\n" +
                "      \"assetId\": \"<string>\",\n" +
                "      \"id\": \"<string>\",\n" +
                "      \"policy\": {\n" +
                "        \"@type\": \"CONTRACT\",\n" +
                "        \"assignee\": \"<string>\",\n" +
                "        \"assigner\": \"<string>\",\n" +
                "        \"extensibleProperties\": {\n" +
                "          \"utd\": {}\n" +
                "        },\n" +
                "        \"inheritsFrom\": \"<string>\",\n" +
                "        \"obligations\": [\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"consequence\": {\n" +
                "              \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "            },\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"parentPermission\": {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            \"target\": \"<string>\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"consequence\": {\n" +
                "              \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "            },\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"parentPermission\": {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            \"target\": \"<string>\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"permissions\": [\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"duties\": [\n" +
                "              {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"target\": \"<string>\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"duties\": [\n" +
                "              {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"target\": \"<string>\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"prohibitions\": [\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"target\": \"<string>\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"target\": \"<string>\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"target\": \"<string>\"\n" +
                "      },\n" +
                "      \"providerId\": \"<string>\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"assetId\": \"<string>\",\n" +
                "      \"id\": \"<string>\",\n" +
                "      \"policy\": {\n" +
                "        \"@type\": \"SET\",\n" +
                "        \"assignee\": \"<string>\",\n" +
                "        \"assigner\": \"<string>\",\n" +
                "        \"extensibleProperties\": {\n" +
                "          \"exercitation_476\": {},\n" +
                "          \"in4\": {}\n" +
                "        },\n" +
                "        \"inheritsFrom\": \"<string>\",\n" +
                "        \"obligations\": [\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"consequence\": {\n" +
                "              \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "            },\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"parentPermission\": {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            \"target\": \"<string>\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"consequence\": {\n" +
                "              \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "            },\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"parentPermission\": {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            \"target\": \"<string>\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"permissions\": [\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"duties\": [\n" +
                "              {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"target\": \"<string>\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"duties\": [\n" +
                "              {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"target\": \"<string>\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"prohibitions\": [\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"target\": \"<string>\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"action\": {\n" +
                "              \"constraint\": {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              \"includedIn\": \"<string>\",\n" +
                "              \"type\": \"<string>\"\n" +
                "            },\n" +
                "            \"assignee\": \"<string>\",\n" +
                "            \"assigner\": \"<string>\",\n" +
                "            \"constraints\": [\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"edctype\": \"<string>\"\n" +
                "              }\n" +
                "            ],\n" +
                "            \"target\": \"<string>\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"target\": \"<string>\"\n" +
                "      },\n" +
                "      \"providerId\": \"<string>\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"dataServices\": [\n" +
                "    {\n" +
                "      \"endpointUrl\": \"<string>\",\n" +
                "      \"id\": \"<string>\",\n" +
                "      \"terms\": \"<string>\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"endpointUrl\": \"<string>\",\n" +
                "      \"id\": \"<string>\",\n" +
                "      \"terms\": \"<string>\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"datasets\": [\n" +
                "    {\n" +
                "      \"distributions\": [\n" +
                "        {\n" +
                "          \"dataService\": {\n" +
                "            \"endpointUrl\": \"<string>\",\n" +
                "            \"id\": \"<string>\",\n" +
                "            \"terms\": \"<string>\"\n" +
                "          },\n" +
                "          \"format\": \"<string>\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"dataService\": {\n" +
                "            \"endpointUrl\": \"<string>\",\n" +
                "            \"id\": \"<string>\",\n" +
                "            \"terms\": \"<string>\"\n" +
                "          },\n" +
                "          \"format\": \"<string>\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"<string>\",\n" +
                "      \"offers\": {\n" +
                "        \"commodo_7b9\": {\n" +
                "          \"@type\": \"SET\",\n" +
                "          \"assignee\": \"<string>\",\n" +
                "          \"assigner\": \"<string>\",\n" +
                "          \"extensibleProperties\": {\n" +
                "            \"elit_053\": {},\n" +
                "            \"ad_af6\": {}\n" +
                "          },\n" +
                "          \"inheritsFrom\": \"<string>\",\n" +
                "          \"obligations\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"consequence\": {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"parentPermission\": {\n" +
                "                \"action\": {\n" +
                "                  \"constraint\": {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  \"includedIn\": \"<string>\",\n" +
                "                  \"type\": \"<string>\"\n" +
                "                },\n" +
                "                \"assignee\": \"<string>\",\n" +
                "                \"assigner\": \"<string>\",\n" +
                "                \"constraints\": [\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"duties\": [\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"target\": \"<string>\"\n" +
                "              },\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"consequence\": {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"parentPermission\": {\n" +
                "                \"action\": {\n" +
                "                  \"constraint\": {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  \"includedIn\": \"<string>\",\n" +
                "                  \"type\": \"<string>\"\n" +
                "                },\n" +
                "                \"assignee\": \"<string>\",\n" +
                "                \"assigner\": \"<string>\",\n" +
                "                \"constraints\": [\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"duties\": [\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"target\": \"<string>\"\n" +
                "              },\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"permissions\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"prohibitions\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"target\": \"<string>\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"properties\": {\n" +
                "        \"magna_7\": {},\n" +
                "        \"irureb\": {}\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"distributions\": [\n" +
                "        {\n" +
                "          \"dataService\": {\n" +
                "            \"endpointUrl\": \"<string>\",\n" +
                "            \"id\": \"<string>\",\n" +
                "            \"terms\": \"<string>\"\n" +
                "          },\n" +
                "          \"format\": \"<string>\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"dataService\": {\n" +
                "            \"endpointUrl\": \"<string>\",\n" +
                "            \"id\": \"<string>\",\n" +
                "            \"terms\": \"<string>\"\n" +
                "          },\n" +
                "          \"format\": \"<string>\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\": \"<string>\",\n" +
                "      \"offers\": {\n" +
                "        \"culpa34\": {\n" +
                "          \"@type\": \"SET\",\n" +
                "          \"assignee\": \"<string>\",\n" +
                "          \"assigner\": \"<string>\",\n" +
                "          \"extensibleProperties\": {\n" +
                "            \"nulla5\": {},\n" +
                "            \"laborec13\": {},\n" +
                "            \"ex_a\": {}\n" +
                "          },\n" +
                "          \"inheritsFrom\": \"<string>\",\n" +
                "          \"obligations\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"consequence\": {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"parentPermission\": {\n" +
                "                \"action\": {\n" +
                "                  \"constraint\": {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  \"includedIn\": \"<string>\",\n" +
                "                  \"type\": \"<string>\"\n" +
                "                },\n" +
                "                \"assignee\": \"<string>\",\n" +
                "                \"assigner\": \"<string>\",\n" +
                "                \"constraints\": [\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"duties\": [\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"target\": \"<string>\"\n" +
                "              },\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"consequence\": {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"parentPermission\": {\n" +
                "                \"action\": {\n" +
                "                  \"constraint\": {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  \"includedIn\": \"<string>\",\n" +
                "                  \"type\": \"<string>\"\n" +
                "                },\n" +
                "                \"assignee\": \"<string>\",\n" +
                "                \"assigner\": \"<string>\",\n" +
                "                \"constraints\": [\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"duties\": [\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"target\": \"<string>\"\n" +
                "              },\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"permissions\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"prohibitions\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"target\": \"<string>\"\n" +
                "        },\n" +
                "        \"occaecat_5f1\": {\n" +
                "          \"@type\": \"SET\",\n" +
                "          \"assignee\": \"<string>\",\n" +
                "          \"assigner\": \"<string>\",\n" +
                "          \"extensibleProperties\": {\n" +
                "            \"labore6\": {}\n" +
                "          },\n" +
                "          \"inheritsFrom\": \"<string>\",\n" +
                "          \"obligations\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"consequence\": {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"parentPermission\": {\n" +
                "                \"action\": {\n" +
                "                  \"constraint\": {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  \"includedIn\": \"<string>\",\n" +
                "                  \"type\": \"<string>\"\n" +
                "                },\n" +
                "                \"assignee\": \"<string>\",\n" +
                "                \"assigner\": \"<string>\",\n" +
                "                \"constraints\": [\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"duties\": [\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"target\": \"<string>\"\n" +
                "              },\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"consequence\": {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"parentPermission\": {\n" +
                "                \"action\": {\n" +
                "                  \"constraint\": {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  \"includedIn\": \"<string>\",\n" +
                "                  \"type\": \"<string>\"\n" +
                "                },\n" +
                "                \"assignee\": \"<string>\",\n" +
                "                \"assigner\": \"<string>\",\n" +
                "                \"constraints\": [\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"duties\": [\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"target\": \"<string>\"\n" +
                "              },\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"permissions\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"prohibitions\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"target\": \"<string>\"\n" +
                "        },\n" +
                "        \"anim6_a\": {\n" +
                "          \"@type\": \"SET\",\n" +
                "          \"assignee\": \"<string>\",\n" +
                "          \"assigner\": \"<string>\",\n" +
                "          \"extensibleProperties\": {\n" +
                "            \"exercitation_e\": {},\n" +
                "            \"id__e_\": {}\n" +
                "          },\n" +
                "          \"inheritsFrom\": \"<string>\",\n" +
                "          \"obligations\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"consequence\": {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"parentPermission\": {\n" +
                "                \"action\": {\n" +
                "                  \"constraint\": {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  \"includedIn\": \"<string>\",\n" +
                "                  \"type\": \"<string>\"\n" +
                "                },\n" +
                "                \"assignee\": \"<string>\",\n" +
                "                \"assigner\": \"<string>\",\n" +
                "                \"constraints\": [\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"duties\": [\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"target\": \"<string>\"\n" +
                "              },\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"consequence\": {\n" +
                "                \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "              },\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"parentPermission\": {\n" +
                "                \"action\": {\n" +
                "                  \"constraint\": {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  \"includedIn\": \"<string>\",\n" +
                "                  \"type\": \"<string>\"\n" +
                "                },\n" +
                "                \"assignee\": \"<string>\",\n" +
                "                \"assigner\": \"<string>\",\n" +
                "                \"constraints\": [\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"edctype\": \"<string>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"duties\": [\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                  }\n" +
                "                ],\n" +
                "                \"target\": \"<string>\"\n" +
                "              },\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"permissions\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"duties\": [\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"value\": \"<Circular reference to #/components/schemas/Duty detected>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"prohibitions\": [\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"action\": {\n" +
                "                \"constraint\": {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                \"includedIn\": \"<string>\",\n" +
                "                \"type\": \"<string>\"\n" +
                "              },\n" +
                "              \"assignee\": \"<string>\",\n" +
                "              \"assigner\": \"<string>\",\n" +
                "              \"constraints\": [\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                },\n" +
                "                {\n" +
                "                  \"edctype\": \"<string>\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"target\": \"<string>\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"target\": \"<string>\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"properties\": {\n" +
                "        \"in_625\": {}\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"id\": \"<string>\",\n" +
                "  \"properties\": {\n" +
                "    \"velit_b32\": {},\n" +
                "    \"consectetur_a2\": {}\n" +
                "  }\n" +
                "}";
    }
}
