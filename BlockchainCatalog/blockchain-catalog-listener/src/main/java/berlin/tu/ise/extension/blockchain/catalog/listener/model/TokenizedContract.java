package berlin.tu.ise.extension.blockchain.catalog.listener.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;

public class TokenizedContract {
    String token_id;
    String name;
    String decimals;
    @JsonDeserialize(using = JsonObjectDeserializer.class)
    JsonObject tokenData;


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

    public JsonObject getTokenData() {
        return tokenData;
    }

    public void setTokenData(JsonObject tokenData) {
        this.tokenData = tokenData;
    }
}
