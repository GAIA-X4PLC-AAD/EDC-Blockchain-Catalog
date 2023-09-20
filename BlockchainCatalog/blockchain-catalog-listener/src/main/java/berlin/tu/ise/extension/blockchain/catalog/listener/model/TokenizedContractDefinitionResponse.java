package berlin.tu.ise.extension.blockchain.catalog.listener.model;


import jakarta.json.JsonObject;

public class TokenizedContractDefinitionResponse {
    JsonObject tokenData;
    String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public JsonObject getTokenData() {
        return tokenData;
    }

    public void setTokenData(JsonObject tokenData) {
        this.tokenData = tokenData;
    }

}
