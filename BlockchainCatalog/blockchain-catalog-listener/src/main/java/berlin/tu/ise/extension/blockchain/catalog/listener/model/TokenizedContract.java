package berlin.tu.ise.extension.blockchain.catalog.listener.model;

import org.eclipse.edc.connector.api.datamanagement.contractdefinition.model.ContractDefinitionResponseDto;

public class TokenizedContract {
    String token_id;
    String name;
    String decimals;
    ContractOfferDto tokenData;


    public String getSource() {
        return tokenData.getDataUrl();
    }

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

    public ContractOfferDto getTokenData() {
        return tokenData;
    }

    public void setTokenData(ContractOfferDto tokenData) {
        this.tokenData = tokenData;
    }
}
