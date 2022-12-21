package berlin.tu.ise.extension.blockchain.catalog.listener;

import berlin.tu.ise.extension.blockchain.catalog.listener.model.ReturnObject;
import berlin.tu.ise.extension.blockchain.catalog.listener.model.TokenizedContractDefinitionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.connector.api.datamanagement.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.edc.connector.contract.spi.definition.observe.ContractDefinitionListener;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;

import java.util.LinkedList;
import java.util.List;

public class BlockchainContractCreator implements ContractDefinitionListener {

    private final Monitor monitor;
    private final String idsWebhookAddress;
    public BlockchainContractCreator(Monitor monitor, String idsWebhookAddress) {
        this.monitor = monitor;
        this.idsWebhookAddress = idsWebhookAddress;
    }

    @Override
    public void created(ContractDefinition contractDefinition) {
        String jsonString = transformToJSON(contractDefinition);
        ReturnObject returnObject = BlockchainHelper.sendToContractSmartContract(jsonString, monitor);
        if(returnObject == null) {
            monitor.warning("Something went wrong during the Blockchain Contract Definition creation of the Contract with id " + contractDefinition.getId());
        } else {
            System.out.printf("[%s] Created Contract %s and minted it successfully with the hash: %s\n", this.getClass().getSimpleName(), contractDefinition.getId(), returnObject.getHash());
        }

    }

    private String transformToJSON(ContractDefinition contractDefinition) {
        monitor.info(String.format("[%s] ContractDefinition: for '%s' and '%s' targeting '%s' created in EDC, start now with Blockchain related steps ...", this.getClass().getSimpleName(), contractDefinition.getContractPolicyId(), contractDefinition.getAccessPolicyId(), contractDefinition.getSelectorExpression().getCriteria().get(0).getOperandRight()));

        monitor.info(String.format("[%s] formating POJO to JSON ...", this.getClass().getSimpleName()));

        ObjectMapper mapper = new ObjectMapper();

        // Cast List of Criterion to List of CriterionDto
        List<CriterionDto> criterionDtoList = new LinkedList<CriterionDto>();
        for (Criterion criterion : contractDefinition.getSelectorExpression().getCriteria()) {
            CriterionDto criterionDto = CriterionDto.Builder.newInstance()
                    .operandLeft(criterion.getOperandLeft())
                    .operandRight(criterion.getOperandRight())
                    .operator(criterion.getOperator())
                    .build();
            criterionDtoList.add(criterionDto);
        }
        // Create Conctract Definition Dto
        ContractDefinitionResponseDto contractDefinitionResponseDto = ContractDefinitionResponseDto.Builder.newInstance()
                .contractPolicyId(contractDefinition.getContractPolicyId())
                .accessPolicyId(contractDefinition.getAccessPolicyId())
                .createdAt(contractDefinition.getCreatedAt())
                .id(contractDefinition.getId())
                .criteria(criterionDtoList).build();

        TokenizedContractDefinitionResponse tokenizedContractDefinitionResponse = new TokenizedContractDefinitionResponse();
        tokenizedContractDefinitionResponse.setTokenData(contractDefinitionResponseDto);
        tokenizedContractDefinitionResponse.setSource(idsWebhookAddress);

        String jsonString = "";
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(contractDefinitionResponseDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return jsonString;
    }

}
