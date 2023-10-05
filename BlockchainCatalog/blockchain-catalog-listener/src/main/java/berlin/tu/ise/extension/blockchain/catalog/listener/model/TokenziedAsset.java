package berlin.tu.ise.extension.blockchain.catalog.listener.model;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;

import javax.xml.crypto.Data;

public class TokenziedAsset  {
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

    /*
       Strangely there is some mismatch where the transformer is creating JSON representations of Assets
       where dataAddress and properties are objects containing objects but when trying to deserialize them back
         into an Asset the dataAddress and properties are exprected to be arrays
     */
    public JsonObject getTokenData() {
        JsonObject returnTokenData = tokenData;
        /*
        JsonValue dataAddressNode = returnTokenData.get("https://w3id.org/edc/v0.0.1/ns/dataAddress");
        if (dataAddressNode != null && dataAddressNode.getValueType() == JsonValue.ValueType.OBJECT) {
            JsonArray dataAddressArray = Json.createArrayBuilder().add(dataAddressNode).build();
            returnTokenData = Json.createObjectBuilder(returnTokenData)
                    .remove("https://w3id.org/edc/v0.0.1/ns/dataAddress")
                    .add("https://w3id.org/edc/v0.0.1/ns/dataAddress", dataAddressArray)
                    .build();
        }
        */
        JsonValue dataAddressTypeNode = returnTokenData.getJsonObject("https://w3id.org/edc/v0.0.1/ns/dataAddress").get("@type");
        if (dataAddressTypeNode != null && dataAddressTypeNode.getValueType() == JsonValue.ValueType.STRING) {
            JsonArray dataAddressTypeArray = Json.createArrayBuilder().add(dataAddressTypeNode).build();
            var changes = Json.createObjectBuilder(returnTokenData.getJsonObject("https://w3id.org/edc/v0.0.1/ns/dataAddress"))
                       .remove("@type")
                        .add("@type", dataAddressTypeArray)
                        .build();
            returnTokenData = Json.createObjectBuilder(returnTokenData)
                    .remove("https://w3id.org/edc/v0.0.1/ns/dataAddress")
                    .add("https://w3id.org/edc/v0.0.1/ns/dataAddress", changes)
                    .build();
        }



        JsonValue propertiesNode = returnTokenData.get("https://w3id.org/edc/v0.0.1/ns/properties");
        if (propertiesNode != null && propertiesNode.getValueType() == JsonValue.ValueType.OBJECT) {
            JsonArray propertiesArray = Json.createArrayBuilder().add(propertiesNode).build();
            returnTokenData = Json.createObjectBuilder(returnTokenData)
                    .remove("https://w3id.org/edc/v0.0.1/ns/properties")
                    .add("https://w3id.org/edc/v0.0.1/ns/properties", propertiesArray)
                    .build();
        }
        return returnTokenData;
    }

    public Asset getTokenDataAsAsset() throws IllegalArgumentException {
        var jsonDataAddressTypeObject = tokenData.getJsonObject("https://w3id.org/edc/v0.0.1/ns/dataAddress"); // typo DataAddress vs dataAdress - DataAddress.EDC_DATA_ADDRESS_TYPE);
        if (jsonDataAddressTypeObject == null) {
            throw new IllegalArgumentException("The token data does not contain a data address type");
        }
        if (!jsonDataAddressTypeObject.containsKey(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY)) {
            throw new IllegalArgumentException("The token data does not contain a data address type property");
        }
        DataAddress transformedDataAddress;
        if (jsonDataAddressTypeObject.containsKey("baseUrl")) {
            HttpDataAddress.Builder builder = HttpDataAddress.Builder.newInstance()
                    .baseUrl(jsonDataAddressTypeObject.getString("baseUrl"));
            if (jsonDataAddressTypeObject.containsKey("name")) {
                builder.name(jsonDataAddressTypeObject.getString("name"));
            } else {
                builder.name("default");
            }

            transformedDataAddress = builder
                    .type(jsonDataAddressTypeObject.getString(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY))
                    .build();
        } else {
            transformedDataAddress = DataAddress.Builder.newInstance()
                    .type(jsonDataAddressTypeObject.getString(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY))
                    .build();
        }
        /*
        if (!jsonDataAddressTypeObject.containsKey("path")) {
            throw new IllegalArgumentException("The token data does not contain a data address type path");
        }*/

        /*
        var transformedDataAddress = HttpDataAddress.Builder.newInstance()
                //.type(tokenData.getJsonObject(DataAddress.EDC_DATA_ADDRESS_TYPE).getString("@type"))
                .baseUrl(jsonDataAddressTypeObject.getString("baseUrl"))
                .path(jsonDataAddressTypeObject.getString("path"))
                .type(jsonDataAddressTypeObject.getString(DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY))
                .build();
        */
        var assetBuilder = Asset.Builder.newInstance()
                .id(tokenData.getString("@id"))
                .contentType(tokenData.getString("@type"))
                .dataAddress(transformedDataAddress);

        var jsonAssetPropertiesObject = tokenData.getJsonObject(Asset.EDC_ASSET_PROPERTIES);
        if (jsonAssetPropertiesObject == null) {
            throw new IllegalArgumentException("The token data does not contain an asset properties object");
        }
        if (!jsonAssetPropertiesObject.containsKey(Asset.PROPERTY_ID)) {
            throw new IllegalArgumentException("The token data does not contain an asset properties id");
        }
        if (!jsonAssetPropertiesObject.containsKey(Asset.PROPERTY_NAME)) {
            throw new IllegalArgumentException("The token data does not contain an asset properties name");
        }
        if (jsonAssetPropertiesObject.containsKey(Asset.PROPERTY_DESCRIPTION)) {
            assetBuilder.property(Asset.PROPERTY_DESCRIPTION, jsonAssetPropertiesObject.getString(Asset.PROPERTY_DESCRIPTION));
        }

        var transformedAsset = assetBuilder
                .property(Asset.PROPERTY_ID, jsonAssetPropertiesObject.getString(Asset.PROPERTY_ID))
                .property(Asset.PROPERTY_NAME, jsonAssetPropertiesObject.getString(Asset.PROPERTY_NAME))
                .build();
        return transformedAsset;
    }
}
