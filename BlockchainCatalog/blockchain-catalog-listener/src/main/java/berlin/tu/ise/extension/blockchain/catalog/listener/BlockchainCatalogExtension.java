package berlin.tu.ise.extension.blockchain.catalog.listener;

import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = BlockchainCatalogExtension.NAME)
public class BlockchainCatalogExtension implements ServiceExtension {
    public static final String NAME = "Blockchain Extension: Catalog";


    @Inject
    private TransferProcessObservable transferProcessObservable;

    // Needs to be injected to get Access to AssetObservable
    @Inject
    private AssetService assetService;


    // Needs to be injected to get Access to PolicyDefinitionObservable
    @Inject
    private PolicyDefinitionService policyDefinitionService;

    @Inject
    private ContractDefinitionService contractDefinitionService;

    @Inject
    private AssetIndex assetIndex;

    @Inject
    private EventRouter eventRouter;

    @Setting
    private final static String EDC_BLOCKCHAIN_INTERFACE_URL = "edc.blockchain.interface.url";

    private final static String DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL = "http://edc-interface:3000/";

    private String getEdcBlockchainInterfaceUrl() {
        return context.getSetting(EDC_BLOCKCHAIN_INTERFACE_URL, DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL);
    }

    private  ServiceExtensionContext context;


    @Override
    public void initialize(ServiceExtensionContext context) {

        this.context = context;

        var monitor = context.getMonitor();

        String idsWebhookAddress = context.getSetting("ids.webhook.address", "http://localhost:8282");
        String idsWebhookPath = context.getSetting("web.http.ids.path", "/api/v1/ids");
        idsWebhookAddress = idsWebhookAddress + idsWebhookPath;

        var edcInterfaceUrl = getEdcBlockchainInterfaceUrl();
        monitor.info("BlockchainCatalogExtension: URL to blockchain interface (edc-interface): " + edcInterfaceUrl);

        BlockchainAssetCreator blockchainAssetCreator = new BlockchainAssetCreator(monitor, assetService, assetIndex, edcInterfaceUrl, idsWebhookAddress);
        eventRouter.register(blockchainAssetCreator); // asynchronous dispatch

        eventRouter.register(new BlockchainPolicyCreator(monitor, policyDefinitionService, edcInterfaceUrl));



        eventRouter.register(new BlockchainContractCreator(monitor, contractDefinitionService, idsWebhookAddress, edcInterfaceUrl, assetIndex));

    }


}
