package berlin.tu.ise.extension.blockchain.catalog.api;


import berlin.tu.ise.extension.blockchain.catalog.listener.BlockchainSmartContractService;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.DistributionResolver;
import org.eclipse.edc.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * This class implements the API for the blockchain catalog.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/{a:federatedcatalog|blockchaincatalog}")
public class BlockchainCatalogApiController implements BlockchainCatalogApi {

    private final Monitor monitor;
    private final TypeTransformerRegistry transformerRegistry;

    private final DistributionResolver distributionResolver;

    private final DataServiceRegistry dataServiceRegistry;

    private final JsonLd jsonLd;

    private final BlockchainSmartContractService blockchainSmartContractService;

    /** Constructor
     *
     * @param monitor the monitor used for logging
     * @param transformerRegistry holds the transformers for the different types
     * @param distributionResolver is needed to find the actual data location
     * @param dataServiceRegistry is needed to retrieve the data assets
     * @param jsonLd is needed to convert the catalog to JSON-LD
     * @param blockchainSmartContractService is needed to fetch the contract offers from the blockchain
     */
    public BlockchainCatalogApiController(Monitor monitor,  TypeTransformerRegistry transformerRegistry, DistributionResolver distributionResolver,
                                          DataServiceRegistry dataServiceRegistry, JsonLd jsonLd, BlockchainSmartContractService blockchainSmartContractService) {
        this.monitor = monitor;
        this.transformerRegistry = transformerRegistry;
        this.distributionResolver = distributionResolver;
        this.dataServiceRegistry = dataServiceRegistry;
        this.jsonLd = jsonLd;
        this.blockchainSmartContractService = blockchainSmartContractService;
    }


    /**
     * This method is called to get the catalog from the blockchain.
     * It fetches all contract offers from the blockchain and creates a catalog from them.
     *
     * @param federatedCatalogCacheQuery the query to be executed. Currently ignored in this implementation
     * @return the catalog as JSON
     */
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

        List<Asset> assetList = blockchainSmartContractService.getAllAssetsFromSmartContract();
        if (assetList == null) {
            monitor.info(String.format("[%s] No assets found in blockchain. Is something wrong with the edc-interface or is the contract empty?", this.getClass().getSimpleName()));
        } else {
            monitor.info(String.format("[%s] fetched %d Assets from Smart Contract", this.getClass().getSimpleName(), assetList.size()));
        }

        List<PolicyDefinition> policyDefinitionList = blockchainSmartContractService.getAllPolicyDefinitionsFromSmartContract();
        if (policyDefinitionList == null) {
            monitor.info(String.format("[%s] No policies found in blockchain. Is something wrong with the edc-interface or is the contract empty?", this.getClass().getSimpleName()));
        } else {
            monitor.info(String.format("[%s] fetched %d Policies from Smart Contract", this.getClass().getSimpleName(), policyDefinitionList.size()));
        }

        List<ContractDefinition> contractDefinitionList = blockchainSmartContractService.getAllContractDefinitionsFromSmartContract();
        if (contractDefinitionList == null) {
            monitor.info(String.format("[%s] No contracts found in blockchain. Is something wrong with the edc-interface or is the contract empty?", this.getClass().getSimpleName()));
        } else {
            monitor.info(String.format("[%s] fetched %d Contracts from Smart Contract", this.getClass().getSimpleName(), contractDefinitionList.size()));
        }

        // HashMap<String, List<ContractOfferDto>> contractDefinitionResponseDtoGroupedBySource = BlockchainHelper.getAllContractDefinitionsFromSmartContractGroupedBySource();

        List<ContractOffer> contractOfferList = new ArrayList<>();

        // iterate over all sources of contractDefinitionResponseDtoGroupedBySource and fetch all contracts for each source

        var extendedDebugging = false;

        if (extendedDebugging) monitor.debug("-------------------------------------------------");
        for (Asset asset : assetList) {
            if (extendedDebugging) monitor.debug("Asset: " + asset.getId());
            if (extendedDebugging) monitor.debug("AssetName: " + asset.getName());
            if (extendedDebugging) monitor.debug("AssetProperties: " + asset.getProperties().toString());
            if (extendedDebugging) monitor.debug("AssetDataAddress: " + asset.getDataAddress().toString());
        }
        for (PolicyDefinition policyDefinition : policyDefinitionList) {
            if (extendedDebugging) monitor.debug("Policy: " + policyDefinition.getId());
            if (extendedDebugging) monitor.debug("PolicyTarget: " + policyDefinition.getPolicy().getTarget());
        }
        for (ContractDefinition contractDefinition : contractDefinitionList) {
            if (extendedDebugging) monitor.debug("Contract: " + contractDefinition.getId());
            if (extendedDebugging) monitor.debug("ContractPolicyId: " + contractDefinition.getContractPolicyId());
            if (extendedDebugging) monitor.debug("ContractCriteria: " + contractDefinition.getAccessPolicyId());
        }


        // iterate over all contracts for a source
        assert contractDefinitionList != null;
        for (ContractDefinition contract : contractDefinitionList) {

            if (extendedDebugging) monitor.debug(format("[%s] fetching contract %s", this.getClass().getSimpleName(), contract.getId()));


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


        /*
        DataService dataService = DataService.Builder.newInstance()
                .id("blockchain-data-service")
                .terms("blockchain-data-service-terms")
                .endpointUrl(edcBlockchainInterfaceUrl)
                .build();


        catalogBuilder.dataService(dataService);

         */
        catalogBuilder.dataServices(dataServiceRegistry.getDataServices());

        Catalog catalog = catalogBuilder.build();


        var catalogJsonObject = transformerRegistry.transform(catalog, JsonObject.class)
                .orElseThrow(InvalidRequestException::new);
        //monitor.debug("Catalog as JSON: " + catalogJsonObject.toString());

        return jsonLd.compact(catalogJsonObject).getContent().toString();
        //return catalogJsonObject.toString();
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

        // try other valid format
        if (assetId == null) {
            assetId = getAssetIdFromCriterions(contract, assetList, "https://w3id.org/edc/v0.0.1/ns/id");
        }


        /* Fix for EDC Dashboard?
        if (contract.getCriteria().get(0).getOperandRight() instanceof ArrayList) {
            assetId = String.valueOf(((ArrayList<?>) contract.getCriteria().get(0).getOperandRight()).get(0));
        }
         */

        String policyId = contract.getContractPolicyId();
        Asset contractAsset = getAssetByName(assetId, assetList);

        PolicyDefinition policyDefinition = getPolicyById(policyId, policyDefinitionList);


        if (contractAsset == null || policyDefinition == null) {
            monitor.debug(String.format("[%s] Not able to find the Asset with id %s or policy with id %s for the contract %s", this.getClass().getSimpleName(), assetId, policyId, contract.getId()));
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

    /** Get the asset id from the criterions parameter of the contract
     * check the contract criteria for the asset id to find the asset this contract is about
     * better to check all available criterias and left and right operands if the content is "name" and then we know that the other side is the asset id
     *
     * @param contract the contract to be checked
     * @param assetList the list of all assets
     * @param assetIdPropertyName the name of the asset id property
     * @return the asset id, null if not found
     */
    private String getAssetIdFromCriterions(ContractDefinition contract, List<Asset> assetList, String assetIdPropertyName) {

        for (Criterion criterion : contract.getAssetsSelector()) {
            if (criterion.getOperandLeft().equals(assetIdPropertyName)) {
                return String.valueOf(criterion.getOperandRight());
            } else if (criterion.getOperandRight().equals(assetIdPropertyName)) {
                return String.valueOf(criterion.getOperandLeft());
            }
        }
        return null;
    }

    /** Get the policy by id
     *
     * @param policyId the id of the policy
     * @param policyDefinitionResponseDtoList the list of all policies
     * @return the policy, null if not found
     */
    private PolicyDefinition getPolicyById(String policyId, List<PolicyDefinition> policyDefinitionResponseDtoList) {
        for (PolicyDefinition p : policyDefinitionResponseDtoList) {
            if (p.getId().equals(policyId)) {
                return p;
            }
        }
        return null;
    }

    /** Get the asset by id
     *
     * @param assetId the id of the asset
     * @param assetEntryDtoList the list of all assets
     * @return the asset, null if not found
     */
    private Asset getAssetById(String assetId, List<Asset> assetEntryDtoList) {
        for (Asset a : assetEntryDtoList) {
            if (a.getId().equals(assetId)) {
                return a;
            }
        }
        return null;
    }

    /** Get the asset by name
     *
     * @param assetId the id of the asset
     * @param assetEntryDtoList the list of all assets
     * @return the asset, null if not found
     */
    private Asset getAssetByName(String assetId, List<Asset> assetEntryDtoList) {
        for (Asset a : assetEntryDtoList) {
            if (a.getId().equals(assetId)) {
                return a;
            }
        }
        return null;
    }
}
