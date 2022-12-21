package berlin.tu.ise.extension.blockchain.catalog.listener;

import org.eclipse.edc.connector.contract.spi.definition.observe.ContractDefinitionObservable;
import org.eclipse.edc.connector.policy.spi.observe.PolicyDefinitionObservable;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.observe.asset.AssetObservable;
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


    @Override
    public void initialize(ServiceExtensionContext context) {


        //var assetObservable = ((AssetServiceImpl) assetService).observable;


        //var assetObservable = context.getService(AssetObservable.class);


        //var policyObservable = context.getService(PolicyDefinitionObservable.class);

        //var contractObservable = context.getService(ContractDefinitionObservable.class);

        var monitor = context.getMonitor();



        transferProcessObservable.registerListener(new MarkerFileCreator(monitor));
        //assetObservable.registerListener(new BlockchainAssetCreator(monitor, assetService, assetIndex));

        //policyObservable.registerListener(new BlockchainPolicyCreator(monitor));

        String idsWebhookAddress = context.getSetting("ids.webhook.address", "http://localhost:8282");
        // append /api/v1/ids/data to the webhook address to get the IDS data endpoint
        idsWebhookAddress = idsWebhookAddress + "/api/v1/ids/data";
        //contractObservable.registerListener(new BlockchainContractCreator(monitor, idsWebhookAddress));

    }


}
