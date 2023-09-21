package berlin.tu.ise.blockchain.catalog.api;

import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.configuration.transform.ManagementApiTypeTransformerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
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

    @Inject
    private ManagementApiTypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonObjectValidatorRegistry validator;

    private ServiceExtensionContext context;

    @Setting
    private final static String EDC_BLOCKCHAIN_INTERFACE_URL = "edc.blockchain.interface.url";

    private final static String DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL = "http://edc-interface:3000/";

    private String getEdcBlockchainInterfaceUrl() {
        return context.getSetting(EDC_BLOCKCHAIN_INTERFACE_URL, DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL);
    }



    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;
        monitor.info("Initializing Blockchain Catalog API Extension");
        var catalogController = new BlockchainCatalogApiController(monitor, getEdcBlockchainInterfaceUrl(), transformerRegistry, validator);
        webService.registerResource("default", catalogController);

    }


}
