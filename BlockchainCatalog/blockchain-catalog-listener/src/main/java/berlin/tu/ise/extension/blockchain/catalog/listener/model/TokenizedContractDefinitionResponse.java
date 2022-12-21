package berlin.tu.ise.extension.blockchain.catalog.listener.model;

import org.eclipse.edc.connector.api.datamanagement.contractdefinition.model.ContractDefinitionResponseDto;

public class TokenizedContractDefinitionResponse {
    ContractDefinitionResponseDto tokenData;
    String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ContractDefinitionResponseDto getTokenData() {
        return tokenData;
    }

    public void setTokenData(ContractDefinitionResponseDto tokenData) {
        this.tokenData = tokenData;
    }

}
