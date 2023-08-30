package berlin.tu.ise.extension.blockchain.catalog.listener;

import org.eclipse.edc.api.validation.DataAddressValidator;
import org.eclipse.edc.connector.api.management.asset.validation.AssetEntryDtoValidator;
import org.eclipse.edc.connector.api.management.asset.validation.AssetValidator;
import org.eclipse.edc.connector.asset.spi.event.AssetCreated;
import org.eclipse.edc.connector.contract.spi.event.contractdefinition.ContractDefinitionCreated;
import org.eclipse.edc.connector.policy.spi.event.PolicyDefinitionCreated;
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
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Extension(value = BlockchainCatalogExtension.NAME)
public class BlockchainCatalogExtension implements ServiceExtension {
    public static final String NAME = "Blockchain Extension: Catalog";


    @Inject
    private TransferProcessObservable transferProcessObservable;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

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

    @Inject
    private JsonObjectValidatorRegistry validator;


    @Override
    public void initialize(ServiceExtensionContext context) {

        this.context = context;

        var monitor = context.getMonitor();

        transformerRegistry.register(new AssetRequestDtoToAssetTransformer());
        transformerRegistry.register(new AssetUpdateRequestWrapperDtoToAssetTransformer());
        transformerRegistry.register(new AssetToAssetResponseDtoTransformer());
        transformerRegistry.register(new JsonObjectToAssetEntryNewDtoTransformer());

        validator.register(EDC_ASSET_ENTRY_DTO_TYPE, AssetValidator.instance());
        validator.register(EDC_ASSET_TYPE, AssetValidator.instance());
        validator.register(EDC_DATA_ADDRESS_TYPE, DataAddressValidator.instance());

        String idsWebhookAddress = context.getSetting("ids.webhook.address", "http://localhost:8282");
        String idsWebhookPath = context.getSetting("web.http.ids.path", "/api/v1/ids");
        idsWebhookAddress = idsWebhookAddress + idsWebhookPath;

        var edcInterfaceUrl = context.getSetting(EDC_BLOCKCHAIN_INTERFACE_URL, DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL); // getEdcBlockchainInterfaceUrl();
        monitor.info("BlockchainCatalogExtension: URL to blockchain interface (edc-interface): " + edcInterfaceUrl);

        BlockchainAssetCreator blockchainAssetCreator = new BlockchainAssetCreator(monitor, assetService, assetIndex, edcInterfaceUrl, idsWebhookAddress);
        eventRouter.registerSync(AssetCreated.class, blockchainAssetCreator); // asynchronous dispatch

        eventRouter.registerSync(PolicyDefinitionCreated.class, new BlockchainPolicyCreator(monitor, policyDefinitionService, edcInterfaceUrl));



        eventRouter.registerSync(ContractDefinitionCreated.class, new BlockchainContractCreator(monitor, contractDefinitionService, idsWebhookAddress, edcInterfaceUrl, assetIndex));

    }


}
