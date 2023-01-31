package berlin.tu.ise.extension.blockchain.catalog.listener;

import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.observe.PolicyDefinitionListener;
import org.eclipse.edc.spi.monitor.Monitor;

public class BlockchainPolicyCreator implements PolicyDefinitionListener {

    private final Monitor monitor;
    public BlockchainPolicyCreator(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void created(PolicyDefinition policyDefinition) {
        String jsonString = transformToJSON(policyDefinition);
        System.out.println(jsonString);
        ReturnObject returnObject = BlockchainHelper.sendToPolicySmartContract(jsonString, monitor);
        if(returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Policy creation of the Policy with id " + policyDefinition.getId());
        } else {
            System.out.printf("[%s] Created Policy %s and minted it successfully with the hash: %s\n", this.getClass().getSimpleName(), policyDefinition.getId(), returnObject.getHash());
        }
    }

    private String transformToJSON(PolicyDefinition policyDefinition) {
        monitor.info(String.format("[%s] Policy: '%s' created in EDC, start now with Blockchain related steps ...\n", this.getClass().getSimpleName(), policyDefinition.getUid()));

        monitor.info(String.format("[%s] formating POJO to JSON ...\n", this.getClass().getSimpleName()));

        ObjectMapper mapper = new ObjectMapper();

        PolicyDefinitionResponseDto policyDefinitionResponseDto = PolicyDefinitionResponseDto.Builder.newInstance()
                .policy(policyDefinition.getPolicy())
                .id(policyDefinition.getUid())
                .createdAt(policyDefinition.getCreatedAt())
                .build();
        // Format them to JSON and print them for debugging. Change later, for now the system out println looks prettier than using monitor
        String jsonString = "";
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyDefinitionResponseDto);
            //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyDefinitionResponseDto));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return jsonString;
    }

}
