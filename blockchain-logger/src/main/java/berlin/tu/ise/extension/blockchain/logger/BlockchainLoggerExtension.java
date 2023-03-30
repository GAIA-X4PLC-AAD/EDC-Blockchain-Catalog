package berlin.tu.ise.extension.blockchain.logger;

import berlin.tu.ise.extension.blockchain.logger.listener.TransferProcessEventSubscriber;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = BlockchainLoggerExtension.NAME)
public class BlockchainLoggerExtension implements ServiceExtension {
    public static final String NAME = "Blockchain Extension: Logger";


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

    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private ContractAgreementService contractAgreementService;

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




        var edcInterfaceUrl = context.getSetting(EDC_BLOCKCHAIN_INTERFACE_URL, DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL); // getEdcBlockchainInterfaceUrl();
        monitor.info("BlockchainLoggerExtension: URL to blockchain interface (edc-interface): " + edcInterfaceUrl);

        TransferProcessEventSubscriber transferProcessEventSubscriber = new TransferProcessEventSubscriber(monitor, transferProcessStore, context.getConnectorId(), edcInterfaceUrl);
        eventRouter.register(transferProcessEventSubscriber);


    }


}
