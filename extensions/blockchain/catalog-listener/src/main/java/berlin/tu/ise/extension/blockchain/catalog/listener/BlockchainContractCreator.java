package berlin.tu.ise.extension.blockchain.catalog.listener;

import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import com.msg.plcaad.edc.ccp.contractdefinition.CcpIntegrationForContractDefinitionService;
import com.msg.plcaad.edc.ccp.exception.CcpException;
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

public class BlockchainContractCreator implements EventSubscriber {

    private final Monitor monitor;
    private final String idsWebhookAddress;

    private final ContractDefinitionService contractDefinitionService;

    private final AssetIndex assetIndex;

    private final String edcInterfaceUrl;

    private final ContractDefinitionApiController contractDefinitionApiController;

    private final String claimComplianceProviderEndpoint;

    private final JsonLd jsonLd;
    private final BlockchainSmartContractService blockchainSmartContractService;
    private final CcpIntegrationForContractDefinitionService ccpIntegrationForContractDefinitionService;

    public BlockchainContractCreator(Monitor monitor, ContractDefinitionService contractDefinitionService, String idsWebhookAddress, String edcInterfaceUrl, AssetIndex assetIndex,
                                     ContractDefinitionApiController contractDefinitionApiController, JsonLd jsonLd, BlockchainSmartContractService blockchainSmartContractService, final CcpIntegrationForContractDefinitionService ccpIntegrationForContractDefinitionService, String claimComplianceProviderEndpoint) {
        this.monitor = monitor;
        this.idsWebhookAddress = idsWebhookAddress;
        this.contractDefinitionService = contractDefinitionService;
        this.edcInterfaceUrl = edcInterfaceUrl;
        this.assetIndex = assetIndex;
        this.contractDefinitionApiController = contractDefinitionApiController;
        this.jsonLd = jsonLd;
        this.blockchainSmartContractService = blockchainSmartContractService;
        this.ccpIntegrationForContractDefinitionService = ccpIntegrationForContractDefinitionService;
        this.claimComplianceProviderEndpoint = claimComplianceProviderEndpoint;
    }



    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var payload = event.getPayload();
        if (!(payload instanceof ContractDefinitionCreated)) return;
        // the event only returns the contract id, so we need to get the contract object

        ContractDefinitionCreated contractDefinitionCreated = (ContractDefinitionCreated) payload;
        String contractId = contractDefinitionCreated.getContractDefinitionId();
        monitor.debug("ContractDefinitionCreated event triggered for contractId: " + contractId);

        ContractDefinition contractDefinition = contractDefinitionService.findById(contractId);

        String jsonRepresentationOfContractDefinition = transformToJson(contractDefinition);

        String verifiablePresentationsOfContract;
        String combinedVpAndContract;

        if (this.claimComplianceProviderEndpoint == null || this.claimComplianceProviderEndpoint.isEmpty()) {
            monitor.debug("CCP-URL is not configured. Creating new VP the legacy way.");
            verifiablePresentationsOfContract = BlockchainVerifiablePresentationCreator.createVerifiablePresentation(contractDefinition, "", idsWebhookAddress, edcInterfaceUrl, assetIndex, monitor);

            combinedVpAndContract = "{\n" +
                    "  \"edcContractdefinition\": " + jsonRepresentationOfContractDefinition + ",\n" +
                    "  \"verifiablePresentation\": " + verifiablePresentationsOfContract + "\n" +
                    "}";
        } else {
            monitor.debug("CCP-URL is configured. Get VPs from Asset(s).");
            try {
                verifiablePresentationsOfContract = ccpIntegrationForContractDefinitionService.getVerifiablePresentationsFromAssets(contractDefinition, assetIndex);
            } catch (CcpException e) {
                monitor.warning("Something went wrong during the CCP-Response extraction of the Contract with id " + contractDefinition.getId(), e);
                verifiablePresentationsOfContract = null;
            }
            if (verifiablePresentationsOfContract == null) {
                // see issue https://github.com/GAIA-X4PLC-AAD/EDC-Blockchain-Catalog/issues/7 to remove this legacy handling.
                monitor.info("No CCP-Responses found for Contract with id " + contractDefinition.getId() + ". Creating new VP the legacy way.");
                verifiablePresentationsOfContract = BlockchainVerifiablePresentationCreator.createVerifiablePresentation(contractDefinition, "", idsWebhookAddress, edcInterfaceUrl, assetIndex, monitor);
                combinedVpAndContract = "{\n" +
                        "  \"edcContractdefinition\": " + jsonRepresentationOfContractDefinition + ",\n" +
                        "  \"verifiablePresentation\": " + verifiablePresentationsOfContract + "\n" +
                        "}";
            } else {
                combinedVpAndContract = "{\n" +
                        "  \"edcContractdefinition\": " + jsonRepresentationOfContractDefinition + ",\n" +
                        "  \"claimComplianceProviderResponses\": " + verifiablePresentationsOfContract + "\n" +
                        "}";
            }
        }

        monitor.debug("Sending Combined Verifiable Presentation and Contract to Blockchain for Contract with id " + contractDefinition.getId() + " with JSON: " + verifiablePresentationsOfContract);

        ReturnObject returnObject = blockchainSmartContractService.sendToContractSmartContract(combinedVpAndContract);
        if (returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Contract Definition creation of the Contract with id " + contractDefinition.getId());
            return;
        }
        monitor.info(String.format("[%s] Created Contract %s and minted it successfully with the hash: %s\n", this.getClass().getSimpleName(), contractDefinition.getId(), returnObject.getHash()));
    }

    private String transformToJson(ContractDefinition contractDefinition) {

        monitor.info(String.format("[%s] ContractDefinition: for '%s' and '%s' targeting '%s' created in EDC, start now with Blockchain related steps ...",
                this.getClass().getSimpleName(), contractDefinition.getContractPolicyId(), contractDefinition.getAccessPolicyId(), "not implemented"));

        monitor.info(String.format("[%s] formating POJO to JSON ...", this.getClass().getSimpleName()));

        var contractDefinitionJson = contractDefinitionApiController.getContractDefinition(contractDefinition.getId());

        monitor.debug(String.format("[%s] Contract Definition: %s", this.getClass().getSimpleName(), jsonLd.compact(contractDefinitionJson).getContent().toString()));

        return jsonLd.compact(contractDefinitionJson).getContent().toString();
    }
}
