package berlin.tu.ise.extension.blockchain.catalog.listener;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;

public class BlockchainCatalogServiceImpl implements CatalogService {
    @Override
    public CompletableFuture<StatusResult<byte[]>> request(String providerUrl, String protocol, QuerySpec querySpec) {
        return null;
    }
}
