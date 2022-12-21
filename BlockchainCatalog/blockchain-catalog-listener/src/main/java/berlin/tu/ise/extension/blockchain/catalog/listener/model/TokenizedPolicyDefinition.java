package berlin.tu.ise.extension.blockchain.catalog.listener.model;


import org.eclipse.edc.connector.api.datamanagement.policy.model.PolicyDefinitionResponseDto;

public class TokenizedPolicyDefinition {
    String token_id;
    String name;
    String decimals;
    PolicyDefinitionResponseDto tokenData;

    public String getToken_id() {
        return token_id;
    }

    public void setToken_id(String token_id) {
        this.token_id = token_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDecimals() {
        return decimals;
    }

    public void setDecimals(String decimals) {
        this.decimals = decimals;
    }

    public PolicyDefinitionResponseDto getTokenData() {
        return tokenData;
    }

    public void setTokenData(PolicyDefinitionResponseDto tokenData) {
        this.tokenData = tokenData;
    }
}
