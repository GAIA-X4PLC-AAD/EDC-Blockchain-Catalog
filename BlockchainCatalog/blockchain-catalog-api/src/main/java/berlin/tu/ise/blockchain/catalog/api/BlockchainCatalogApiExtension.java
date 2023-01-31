package berlin.tu.ise.blockchain.catalog.api;

import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.web.spi.WebService;

@Extension(value = BlockchainCatalogApiExtension.NAME)
public class BlockchainCatalogApiExtension implements ServiceExtension {
    public static final String NAME = "Blockchain Catalog API Extension";

    @Inject
    private WebService webService;

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    private ManagementApiConfiguration config;

    @Inject
    private Monitor monitor;



    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor.info("Initializing Blockchain Catalog API Extension");
        var catalogController = new BlockchainCatalogApiController(monitor);
        webService.registerResource("default", catalogController);

    }


}
