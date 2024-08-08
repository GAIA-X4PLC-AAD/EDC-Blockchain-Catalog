package berlin.tu.ise.extension.blockchain.catalog.listener;

import com.msg.plcaad.edc.ccp.asset.CcpIntegrationForAssetService;
import com.msg.plcaad.edc.ccp.contractdefinition.CcpIntegrationForContractDefinitionService;
import jakarta.json.Json;
import org.eclipse.edc.connector.api.management.asset.v3.AssetApiController;
import org.eclipse.edc.connector.api.management.asset.validation.AssetValidator;
import org.eclipse.edc.connector.api.management.configuration.transform.ManagementApiTypeTransformerRegistry;
import org.eclipse.edc.connector.api.management.contractdefinition.ContractDefinitionApiController;
import org.eclipse.edc.connector.api.management.contractdefinition.transform.JsonObjectFromContractDefinitionTransformer;
import org.eclipse.edc.connector.api.management.contractdefinition.transform.JsonObjectToContractDefinitionTransformer;
import org.eclipse.edc.connector.api.management.policy.PolicyDefinitionApiController;
import org.eclipse.edc.connector.api.management.policy.transform.JsonObjectFromPolicyDefinitionTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.JsonObjectToPolicyDefinitionTransformer;
import org.eclipse.edc.connector.api.management.policy.validation.PolicyDefinitionValidator;
import org.eclipse.edc.connector.asset.spi.event.AssetCreated;
import org.eclipse.edc.connector.contract.spi.event.contractdefinition.ContractDefinitionCreated;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.event.PolicyDefinitionCreated;
import org.eclipse.edc.connector.spi.asset.AssetService;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.core.transform.transformer.OdrlTransformersFactory;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromCatalogTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromCriterionTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.core.transform.transformer.from.JsonObjectFromQuerySpecTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToCatalogTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDataServiceTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDatasetTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDistributionTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import java.util.Map;

import static org.eclipse.edc.connector.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.edc.spi.CoreConstants.JSON_LD;


@Extension(value = BlockchainCatalogExtension.NAME)
public class BlockchainCatalogExtension implements ServiceExtension {
    public static final String NAME = "Blockchain Extension: Catalog";

    //@Inject
    //private TypeTransformerRegistry transformerRegistry;

    // Needs to be injected to get Access to AssetObservable
    @Inject
    private AssetService assetService;

    @Setting
    private static final String CCP_INTERFACE_URL = "ccp.interface.url";

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
    private static final String EDC_BLOCKCHAIN_INTERFACE_URL = "edc.blockchain.interface.url";

    private static final String DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL = "http://edc-interface:3000/";

    private String getEdcBlockchainInterfaceUrl() {
        return context.getSetting(EDC_BLOCKCHAIN_INTERFACE_URL, DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL);
    }

    private  ServiceExtensionContext context;

    @Inject
    private ManagementApiTypeTransformerRegistry transformerRegistry;


    @Inject
    private JsonObjectValidatorRegistry validator;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLd;

//    @Inject
//    private CcpIntegrationForAssetService ccpIntegrationForAssetService;
//    @Inject
//    private CcpIntegrationForContractDefinitionService ccpIntegrationForContractDefinitionService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        this.context = context;

        // TODO: remove those lines as soon as dependency injection is working
        final var ccpIntegrationForAssetService = new CcpIntegrationForAssetService(monitor);
        final var ccpIntegrationForContractDefinitionService = new CcpIntegrationForContractDefinitionService(monitor);


        registerTransformers();

        jsonLd.registerNamespace("tuberlin", "https://ise.tu.berlin/edc/v0.0.1/ns/");

        validator.register(EDC_POLICY_DEFINITION_TYPE, PolicyDefinitionValidator.instance());
        validator.register(Asset.EDC_ASSET_TYPE, AssetValidator.instance());

        BlockchainSmartContractService blockchainSmartContractService = new BlockchainSmartContractService(monitor, transformerRegistry, validator, jsonLd, getEdcBlockchainInterfaceUrl());

        AssetApiController assetApiController = new AssetApiController(assetService, transformerRegistry, monitor, validator);
        PolicyDefinitionApiController policyDefinitionApiController = new PolicyDefinitionApiController(monitor, transformerRegistry, policyDefinitionService, validator);
        ContractDefinitionApiController contractDefinitionApiController = new ContractDefinitionApiController(transformerRegistry, contractDefinitionService, monitor, validator);

        // address used after (during?) negotiation to initiate the asset transfer
        String originatorAddress = context.getSetting("edc.dsp.callback.address", "http://localhost:9194/protocol");

        var edcInterfaceUrl = context.getSetting(EDC_BLOCKCHAIN_INTERFACE_URL, DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL);
        monitor.info("BlockchainCatalogExtension: URL to blockchain interface (edc-interface): " + edcInterfaceUrl);

        final String ccpInterfaceUrl = context.getSetting(CCP_INTERFACE_URL, CCP_INTERFACE_URL);
        BlockchainAssetCreator blockchainAssetCreator = new BlockchainAssetCreator(monitor, assetIndex, assetService, edcInterfaceUrl, originatorAddress, assetApiController, jsonLd, blockchainSmartContractService, ccpIntegrationForAssetService, ccpInterfaceUrl);
        eventRouter.registerSync(AssetCreated.class, blockchainAssetCreator); // asynchronous dispatch

        eventRouter.registerSync(PolicyDefinitionCreated.class, new BlockchainPolicyCreator(monitor, policyDefinitionService, edcInterfaceUrl, policyDefinitionApiController, jsonLd, blockchainSmartContractService));

        eventRouter.registerSync(ContractDefinitionCreated.class,
                new BlockchainContractCreator(monitor, contractDefinitionService, originatorAddress, edcInterfaceUrl, assetIndex, contractDefinitionApiController, jsonLd, blockchainSmartContractService, ccpIntegrationForContractDefinitionService, ccpInterfaceUrl));

        //initWithTestDate();

        /*
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://tu.berlin")
                .path("/tmp/test.txt")
                .build();
        var asset = Asset.Builder.newInstance()
                .name("TestAsset2")
                .description("TestAssetDescription2")
                .dataAddress(dataAddress)
                .build();

        var result = assetService.create(asset);

        var assetAsJsonObject = assetApiController.getAsset(result.getContent().getId());
        //var jsonObject = transformerRegistry.transform(asset, JsonObject.class);
        monitor.debug("Test JsonObject: " + assetAsJsonObject.toString());


         */

    }


    // TODO: find out which of them are actually needed
    private void registerTransformers() {
        var mapper = typeManager.getMapper(JSON_LD);
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class);

        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());
        transformerRegistry.register(new JsonObjectToPolicyDefinitionTransformer());
        transformerRegistry.register(new JsonObjectFromPolicyDefinitionTransformer(jsonBuilderFactory));
        transformerRegistry.register(new JsonObjectToContractDefinitionTransformer());
        transformerRegistry.register(new JsonObjectFromContractDefinitionTransformer(jsonBuilderFactory));

        // EDC model to JSON-LD transformers
        transformerRegistry.register(new JsonObjectFromCatalogTransformer(jsonBuilderFactory, mapper));
        transformerRegistry.register(new JsonObjectFromDatasetTransformer(jsonBuilderFactory, mapper));
        transformerRegistry.register(new JsonObjectFromPolicyTransformer(jsonBuilderFactory));
        transformerRegistry.register(new JsonObjectFromDistributionTransformer(jsonBuilderFactory));
        transformerRegistry.register(new JsonObjectFromDataServiceTransformer(jsonBuilderFactory));
        transformerRegistry.register(new JsonObjectFromAssetTransformer(jsonBuilderFactory, mapper));
        transformerRegistry.register(new JsonObjectFromDataAddressTransformer(jsonBuilderFactory));
        transformerRegistry.register(new JsonObjectFromQuerySpecTransformer(jsonBuilderFactory));
        transformerRegistry.register(new JsonObjectFromCriterionTransformer(jsonBuilderFactory, mapper));

        // JSON-LD to EDC model transformers
        // DCAT transformers
        transformerRegistry.register(new JsonObjectToCatalogTransformer());
        transformerRegistry.register(new JsonObjectToDataServiceTransformer());
        transformerRegistry.register(new JsonObjectToDatasetTransformer());
        transformerRegistry.register(new JsonObjectToDistributionTransformer());

        // ODRL Transformers
        OdrlTransformersFactory.jsonObjectToOdrlTransformers().forEach(transformerRegistry::register);
        transformerRegistry.register(new JsonValueToGenericTypeTransformer(mapper));
        transformerRegistry.register(new JsonObjectToAssetTransformer());
        transformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        transformerRegistry.register(new JsonObjectToCriterionTransformer());
        transformerRegistry.register(new JsonObjectToDataAddressTransformer());
    }

    public void initWithTestDate() {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://tu.berlin")
                .path("/tmp/test.txt")
                .build();
        var asset = Asset.Builder.newInstance()
                .name("TestAsset")
                .description("TestAssetDescription")
                .dataAddress(dataAddress)
                .build();
        var result = assetService.create(asset);

        var policy = Policy.Builder.newInstance()
                .assignee("TestAssignee")
                .assigner("TestAssigner")
                .target("TestTarget")
                .build();

        var policyDefinition = PolicyDefinition.Builder.newInstance()
                .policy(policy)
                .build();
        var resultPolicyDefinition = policyDefinitionService.create(policyDefinition);


        var assetSelectorCriterion = Criterion.Builder.newInstance()
                .operandLeft("name")
                .operator("=")
                .operandRight("TestAsset")
                .build();

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .accessPolicyId(resultPolicyDefinition.getContent().getUid())
                .contractPolicyId(resultPolicyDefinition.getContent().getUid())
                .assetsSelectorCriterion(assetSelectorCriterion)
                .build();
        var resultContractDefinition = contractDefinitionService.create(contractDefinition);
    }


}
