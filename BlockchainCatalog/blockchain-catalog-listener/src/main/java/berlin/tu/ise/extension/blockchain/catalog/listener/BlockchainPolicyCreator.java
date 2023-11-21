package berlin.tu.ise.extension.blockchain.catalog.listener;

import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import org.eclipse.edc.connector.api.management.policy.PolicyDefinitionApiController;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.event.PolicyDefinitionCreated;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

public class BlockchainPolicyCreator implements EventSubscriber {

    private final Monitor monitor;
    private final PolicyDefinitionService policyDefinitionService;

    private final String edcInterfaceUrl;

    private final PolicyDefinitionApiController policyDefinitionApiController;
    private final JsonLd jsonLd;

    private BlockchainSmartContractService blockchainSmartContractService;
    public BlockchainPolicyCreator(Monitor monitor, PolicyDefinitionService policyDefinitionService, String edcInterfaceUrl, PolicyDefinitionApiController policyDefinitionApiController, JsonLd jsonLd, BlockchainSmartContractService blockchainSmartContractService) {
        this.monitor = monitor;
        this.policyDefinitionService = policyDefinitionService;
        this.edcInterfaceUrl = edcInterfaceUrl;
        this.policyDefinitionApiController = policyDefinitionApiController;
        this.jsonLd = jsonLd;
        this.blockchainSmartContractService = blockchainSmartContractService;
    }


    public <E extends Event> void on(EventEnvelope<E> event){
        var payload = event.getPayload();
        if (!(payload instanceof PolicyDefinitionCreated)) return;

        // the event only returns the policy id, so we need to get the policy object
        PolicyDefinitionCreated policyDefinitionCreated = (PolicyDefinitionCreated) payload;
        String policyId = policyDefinitionCreated.getPolicyDefinitionId();
        PolicyDefinition policyDefinition = policyDefinitionService.findById(policyId);

        String jsonString = transformToJSON(policyDefinition);
        System.out.println(jsonString);
        ReturnObject returnObject = blockchainSmartContractService.sendToPolicySmartContract(jsonString);
        if(returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Policy creation of the Policy with id " + policyDefinition.getId());
        } else {
            System.out.printf("[%s] Created Policy %s and minted it successfully with the hash: %s\n", this.getClass().getSimpleName(), policyDefinition.getId(), returnObject.getHash());
        }
    }



    private String transformToJSON(PolicyDefinition policyDefinition) {
        monitor.info(String.format("[%s] Policy: '%s' created in EDC, start now with Blockchain related steps ...\n", this.getClass().getSimpleName(), policyDefinition.getUid()));

        monitor.info(String.format("[%s] formating POJO to JSON ...\n", this.getClass().getSimpleName()));

        var policyJson = policyDefinitionApiController.getPolicyDefinition(policyDefinition.getUid());
        monitor.info(String.format("[%s] Policy JSON: %s\n", this.getClass().getSimpleName(), jsonLd.compact(policyJson).getContent().toString()));

        return jsonLd.compact(policyJson).getContent().toString();
    }

}
