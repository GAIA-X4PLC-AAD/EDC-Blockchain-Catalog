package berlin.tu.ise.extension.blockchain.catalog.listener;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.concurrent.CompletableFuture;

public class BlockchainCatalogServiceImpl implements CatalogService {

    @Override
    public CompletableFuture<Catalog> getByProviderUrl(String providerUrl, QuerySpec spec) {
        return null;
    }
}
