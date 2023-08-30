package berlin.tu.ise.extension.blockchain.catalog.listener.model;

import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;

public class TokenizedContract {
    String token_id;
    String name;
    String decimals;
    ContractOfferMessage tokenData;


    public String getSource() {
        return tokenData.getCounterPartyAddress();
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

    public ContractOfferMessage getTokenData() {
        return tokenData;
    }

    public void setTokenData(ContractOfferMessage tokenData) {
        this.tokenData = tokenData;
    }
}
