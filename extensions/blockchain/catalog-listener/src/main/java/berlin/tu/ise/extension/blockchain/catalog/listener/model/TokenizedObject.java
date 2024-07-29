package berlin.tu.ise.extension.blockchain.catalog.listener.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.json.JsonObject;

public class TokenizedObject {

    @SuppressWarnings("CheckStyle")
    String token_id;
    String name;
    String decimals;
    @JsonDeserialize(using = JsonObjectDeserializer.class)
    JsonObject tokenData;

    public String getToken_id() {
        return token_id;
    }

    public void setToken_id(String tokenId) {
        this.token_id = tokenId;
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
