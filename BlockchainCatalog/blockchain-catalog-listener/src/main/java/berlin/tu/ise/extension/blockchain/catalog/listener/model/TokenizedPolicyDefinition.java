package berlin.tu.ise.extension.blockchain.catalog.listener.model;


import org.eclipse.edc.connector.policy.spi.PolicyDefinition;

public class TokenizedPolicyDefinition {
    String token_id;
    String name;
    String decimals;
    PolicyDefinition tokenData;

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

    public PolicyDefinition getTokenData() {
        return tokenData;
    }

    public void setTokenData(PolicyDefinition tokenData) {
        this.tokenData = tokenData;
    }
}
