package berlin.tu.ise.extension.blockchain.catalog.listener.model;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.types.domain.asset.Asset;

public class TokenizedObject {

    String token_id;
    String name;
    String decimals;
    public JsonObject tokenData;
}
