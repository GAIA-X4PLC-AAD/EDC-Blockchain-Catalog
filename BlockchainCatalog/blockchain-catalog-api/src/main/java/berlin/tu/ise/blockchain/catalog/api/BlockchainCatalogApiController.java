package berlin.tu.ise.blockchain.catalog.api;

import berlin.tu.ise.extension.blockchain.catalog.listener.BlockchainHelper;
import berlin.tu.ise.extension.blockchain.catalog.listener.model.ContractOfferDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.QueryResponse;
import org.eclipse.edc.catalog.spi.model.FederatedCatalogCacheQuery;
import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.lang.String.format;

@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/{a:federatedcatalog|blockchaincatalog}")
public class BlockchainCatalogApiController implements BlockchainCatalogApi {

    private Monitor monitor;

    private String edcBlockchainInterfaceUrl;

    public BlockchainCatalogApiController(Monitor monitor, String edcBlockchainInterfaceUrl) {
        this.monitor = monitor;
        this.edcBlockchainInterfaceUrl = edcBlockchainInterfaceUrl;
    }



    @Override
    @POST
    public Collection<ContractOffer> getCatalog(FederatedCatalogCacheQuery federatedCatalogCacheQuery) {

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

        List<AssetEntryDto> assetEntryDtoList = BlockchainHelper.getAllAssetsFromSmartContract(edcBlockchainInterfaceUrl, this.monitor);
        if (assetEntryDtoList == null) {
            monitor.info(String.format("[%s] No assets found in blockchain. Is something wrong with the edc-interface or is the contract empty?", this.getClass().getSimpleName()));
        } else {
            monitor.info(String.format("[%s] fetched %d Assets from Smart Contract", this.getClass().getSimpleName(), assetEntryDtoList.size()));
        }

        List<PolicyDefinitionResponseDto> policyDefinitionResponseDtoList = BlockchainHelper.getAllPolicyDefinitionsFromSmartContract(edcBlockchainInterfaceUrl);
        if (policyDefinitionResponseDtoList == null) {
            monitor.info(String.format("[%s] No policies found in blockchain. Is something wrong with the edc-interface or is the contract empty?", this.getClass().getSimpleName()));
        } else {
            monitor.info(String.format("[%s] fetched %d Policies from Smart Contract", this.getClass().getSimpleName(), policyDefinitionResponseDtoList.size()));
        }

        List<ContractOfferMessage> contractOfferDtoList = BlockchainHelper.getAllContractDefinitionsFromSmartContract(edcBlockchainInterfaceUrl);
        if (contractOfferDtoList == null) {
            monitor.info(String.format("[%s] No contracts found in blockchain. Is something wrong with the edc-interface or is the contract empty?", this.getClass().getSimpleName()));
        } else {
            monitor.info(String.format("[%s] fetched %d Contracts from Smart Contract", this.getClass().getSimpleName(), contractOfferDtoList.size()));
        }

        // HashMap<String, List<ContractOfferDto>> contractDefinitionResponseDtoGroupedBySource = BlockchainHelper.getAllContractDefinitionsFromSmartContractGroupedBySource();

        List<ContractOffer> contractOfferList = new ArrayList<>();

        // iterate over all sources of contractDefinitionResponseDtoGroupedBySource and fetch all contracts for each source

        // iterate over all contracts for a source
        for (ContractOfferDto contract : contractOfferDtoList) {

            monitor.info(format("[%s] fetching contract %s", this.getClass().getSimpleName(), contract.getId()));

            ContractOffer contractOffer = getContractOfferFromContractDefinitionDto(contract, assetEntryDtoList, policyDefinitionResponseDtoList);
            if (contractOffer != null) {
                contractOfferList.add(contractOffer);
            }

        }



        monitor.info(format("Fetched %d offers from blockchain", contractOfferList.size()));

        return contractOfferList;
    }

    /**
     * Connecting the actual Asset Objects with Policy Objects to the ContractOffer Object
     * @param contract the contract to be created
     * @param assetEntryDtoList the list of all existing assets in the blockchain
     * @param policyDefinitionResponseDtoList the list of all existing policies in the blockchain
     * @return ContractOffer created from combining the Asset and Policy Objects identified by the ids in the ContractDefinitionResponseDto, null if not found
     */
    private ContractOffer getContractOfferFromContractDefinitionDto(ContractOfferDto contract, List<AssetEntryDto> assetEntryDtoList, List<PolicyDefinitionResponseDto> policyDefinitionResponseDtoList) {
        String assetId = String.valueOf(contract.getCriteria().get(0).getOperandRight());
        if (contract.getCriteria().get(0).getOperandRight() instanceof ArrayList) {
            assetId = String.valueOf(((ArrayList<?>) contract.getCriteria().get(0).getOperandRight()).get(0)); // WHAT THE FUCK WARUM MUSS ICH DAS HIER TUN WENN DIE ÜBER DAS EDC DASHBOARD ERSTELLT WERDEN?!?=?!
        }
        String policyId = contract.getContractPolicyId();

        AssetEntryDto assetEntryDto = null;
        PolicyDefinitionResponseDto policyDefinitionResponseDto = null;

        assetEntryDto = getAssetById(assetId, assetEntryDtoList);

        policyDefinitionResponseDto = getPolicyById(policyId, policyDefinitionResponseDtoList);


        if (assetEntryDto == null || policyDefinitionResponseDto == null) {
            monitor.severe(String.format("[%s] Not able to find the Asset with id %s or policy with id %s for the contract %s", this.getClass().getSimpleName(), assetId, policyId, contract.getId()));
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeYearsFronNow = LocalDateTime.now().plusYears(3);
        // convert LocalDateTime to ZonedDateTime, with default system zone id
        ZonedDateTime nowZonedDateTime = now.atZone(ZoneId.systemDefault());
        ZonedDateTime threeYearsFronNowZonedDateTime = threeYearsFronNow.atZone(ZoneId.systemDefault());


        Asset asset = Asset.Builder.newInstance().id(assetEntryDto.getAsset().getId()).properties(assetEntryDto.getAsset().getProperties()).build();
        Policy policy = Policy.Builder.newInstance()
                .target(policyDefinitionResponseDto.getPolicy().getTarget())
                .assignee(policyDefinitionResponseDto.getPolicy().getAssignee())
                .assigner(policyDefinitionResponseDto.getPolicy().getAssigner())
                .prohibitions(policyDefinitionResponseDto.getPolicy().getProhibitions())
                .permissions(policyDefinitionResponseDto.getPolicy().getPermissions())
                .extensibleProperties(policyDefinitionResponseDto.getPolicy().getExtensibleProperties())
                .duties(policyDefinitionResponseDto.getPolicy().getObligations()).build();

        String providerUrl = "http://unavailable:8080";
        if (asset.getProperties().get("asset:prop:originator") != null) {
            providerUrl = asset.getProperties().get("asset:prop:originator").toString();
        }
        return  ContractOffer.Builder.newInstance().assetId(assetEntryDto.getAsset().getId()).policy(policy).id(contract.getId()).providerId(URI.create(providerUrl).toString()).build();
    }

    private PolicyDefinitionResponseDto getPolicyById(String policyId, List<PolicyDefinitionResponseDto> policyDefinitionResponseDtoList) {
        for (PolicyDefinitionResponseDto p: policyDefinitionResponseDtoList) {
            if(p.getId().equals(policyId)) {
                return p;
            }
        }
        return null;
    }

    private AssetEntryDto getAssetById(String assetId, List<AssetEntryDto> assetEntryDtoList) {
        for (AssetEntryDto a: assetEntryDtoList) {
            if(a.getAsset().getId().equals(assetId)) {
                return a;
            }
        }
        return null;
    }
}
