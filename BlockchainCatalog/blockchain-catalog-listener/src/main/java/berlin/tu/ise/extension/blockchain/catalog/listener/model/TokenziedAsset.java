package berlin.tu.ise.extension.blockchain.catalog.listener.model;


import org.eclipse.edc.connector.api.management.asset.model.AssetEntryDto;

public class TokenziedAsset {
    String token_id;
    String name;
    String decimals;
    AssetEntryDto tokenData;

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

    public AssetEntryDto getTokenData() {
        return tokenData;
    }

    public void setTokenData(AssetEntryDto tokenData) {
        this.tokenData = tokenData;
    }
}
