package berlin.tu.ise.extension.blockchain.catalog.listener;

import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.api.management.contractdefinition.ContractDefinitionApiController;
import org.eclipse.edc.connector.contract.spi.event.contractdefinition.ContractDefinitionCreated;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class BlockchainContractCreator implements EventSubscriber {

    private final Monitor monitor;
    private final String idsWebhookAddress;

    private final ContractDefinitionService contractDefinitionService;

    private final AssetIndex assetIndex;

    private final String edcInterfaceUrl;

    private final ContractDefinitionApiController contractDefinitionApiController;

    private String claimComplianceProviderEndpoint;

    private final JsonLd jsonLd;
    private BlockchainSmartContractService blockchainSmartContractService;

    public BlockchainContractCreator(Monitor monitor, ContractDefinitionService contractDefinitionService, String idsWebhookAddress, String edcInterfaceUrl, AssetIndex assetIndex, ContractDefinitionApiController contractDefinitionApiController, JsonLd jsonLd, BlockchainSmartContractService blockchainSmartContractService) {
        this.monitor = monitor;
        this.idsWebhookAddress = idsWebhookAddress;
        this.contractDefinitionService = contractDefinitionService;
        this.edcInterfaceUrl = edcInterfaceUrl;
        this.assetIndex = assetIndex;
        this.contractDefinitionApiController = contractDefinitionApiController;
        this.jsonLd = jsonLd;
        this.blockchainSmartContractService = blockchainSmartContractService;
    }

    public BlockchainContractCreator(Monitor monitor, ContractDefinitionService contractDefinitionService, String idsWebhookAddress, String edcInterfaceUrl, AssetIndex assetIndex, ContractDefinitionApiController contractDefinitionApiController, JsonLd jsonLd, BlockchainSmartContractService blockchainSmartContractService, String claimComplianceProviderEndpoint) {
        this(monitor, contractDefinitionService, idsWebhookAddress, edcInterfaceUrl, assetIndex, contractDefinitionApiController, jsonLd, blockchainSmartContractService);
        this.claimComplianceProviderEndpoint = claimComplianceProviderEndpoint;
    }


    @Override
    public <E extends Event> void on(EventEnvelope<E> event){
        var payload = event.getPayload();
        if (!(payload instanceof ContractDefinitionCreated)) return;
        // the event only returns the contract id, so we need to get the contract object

        ContractDefinitionCreated contractDefinitionCreated = (ContractDefinitionCreated) payload;
        String contractId = contractDefinitionCreated.getContractDefinitionId();
        monitor.debug("ContractDefinitionCreated event triggered for contractId: " + contractId);

        ContractDefinition contractDefinition = contractDefinitionService.findById(contractId);

        String jsonRepresentationOfContractDefinition = transformToJSON(contractDefinition);

        String verifiablePresentationsOfContract;
        String combinedVPandContract;

        if (this.claimComplianceProviderEndpoint == null || this.claimComplianceProviderEndpoint.isEmpty()) {
            monitor.debug("CCP-URL is not configured. Creating new VP the legacy way.");
            verifiablePresentationsOfContract = BlockchainVerifiablePresentationCreator.createVerifiablePresentation(contractDefinition, "", idsWebhookAddress, edcInterfaceUrl, assetIndex, contractDefinitionApiController, jsonLd, monitor, blockchainSmartContractService);

            combinedVPandContract = "{\n" +
                    "  \"edcContractdefinition\": " + jsonRepresentationOfContractDefinition + ",\n" +
                    "  \"verifiablePresentation\": " + verifiablePresentationsOfContract + "\n" +
                    "}";
        } else {
            monitor.debug("CCP-URL is configured. Get VPs from Asset(s).");
            verifiablePresentationsOfContract = getVerifiablePresentationsFromAssets(contractDefinition);
            if (verifiablePresentationsOfContract == null) {
                monitor.info("No CCP-Responses found for Contract with id " + contractDefinition.getId() + ". Creating new VP the legacy way.");
                verifiablePresentationsOfContract = BlockchainVerifiablePresentationCreator.createVerifiablePresentation(contractDefinition, "", idsWebhookAddress, edcInterfaceUrl, assetIndex, contractDefinitionApiController, jsonLd, monitor, blockchainSmartContractService);
                combinedVPandContract = "{\n" +
                        "  \"edcContractdefinition\": " + jsonRepresentationOfContractDefinition + ",\n" +
                        "  \"verifiablePresentation\": " + verifiablePresentationsOfContract + "\n" +
                        "}";
            } else {
                combinedVPandContract = "{\n" +
                        "  \"edcContractdefinition\": " + jsonRepresentationOfContractDefinition + ",\n" +
                        "  \"claimComplianceProviderResponses\": " + verifiablePresentationsOfContract + "\n" +
                        "}";
            }
        }

        monitor.debug("Sending Combined Verifiable Presentation and Contract to Blockchain for Contract with id " + contractDefinition.getId() + " with JSON: " + verifiablePresentationsOfContract);

        ReturnObject returnObject = blockchainSmartContractService.sendToContractSmartContract(combinedVPandContract);
        if(returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Contract Definition creation of the Contract with id " + contractDefinition.getId());
            return;
        }

        monitor.info(String.format("[%s] Created Contract %s and minted it successfully with the hash: %s\n", this.getClass().getSimpleName(), contractDefinition.getId(), returnObject.getHash()));

    }

    private String getVerifiablePresentationsFromAssets(final ContractDefinition contractDefinition) {
        final List<String> ccpResponses = new ArrayList<>();
        contractDefinition.getAssetsSelector().forEach(criterion -> {
            final Object operandLeft = criterion.getOperandLeft();
            if (operandLeft.equals(EDC_NAMESPACE + "id")) {
                final Object operandRight = criterion.getOperandRight();
                final Asset asset = assetIndex.findById(operandRight.toString());
                if (asset != null) {
                    monitor.debug("Asset found: " + asset.getId());
                    final Object ccpResponse = asset.getProperty(EDC_NAMESPACE + "claimComplianceProviderResponse");
                    if (ccpResponse == null) {
                        monitor.warning("No CCP-Response found for Asset with id " + asset.getId());
                    } else {
                        monitor.debug("CCP-Response found for Asset with id " + asset.getId());
                        ccpResponses.add(ccpResponse.toString());
                    }

                } else {
                    monitor.warning("Asset " + operandRight+ " not found. Skipping this criterion.");
                }
            }
        });
        if (ccpResponses.isEmpty()) {
            return null;
        }
        return getJsonArray(ccpResponses);
    }

    private String getJsonArray(final List<String> ccpResponses) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(ccpResponses);
        } catch (Exception e) {
            monitor.warning("Failed to convert ccpResponses to JSON: " + e.getMessage());
            return null;
        }
    }

    private String transformToJSON(ContractDefinition contractDefinition) {

        monitor.info(String.format("[%s] ContractDefinition: for '%s' and '%s' targeting '%s' created in EDC, start now with Blockchain related steps ...", this.getClass().getSimpleName(), contractDefinition.getContractPolicyId(), contractDefinition.getAccessPolicyId(), "not implemented"));

        monitor.info(String.format("[%s] formating POJO to JSON ...", this.getClass().getSimpleName()));

        var contractDefinitionJson = contractDefinitionApiController.getContractDefinition(contractDefinition.getId());

        monitor.debug(String.format("[%s] Contract Definition: %s", this.getClass().getSimpleName(), jsonLd.compact(contractDefinitionJson).getContent().toString()));

        return jsonLd.compact(contractDefinitionJson).getContent().toString();

    }
}
