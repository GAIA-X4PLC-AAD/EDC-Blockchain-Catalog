package berlin.tu.ise.extension.blockchain.catalog.api;

import berlin.tu.ise.extension.blockchain.catalog.listener.BlockchainSmartContractService;
import jakarta.json.Json;
import org.eclipse.edc.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.catalog.spi.DatasetResolver;
import org.eclipse.edc.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.configuration.transform.ManagementApiTypeTransformerRegistry;
import org.eclipse.edc.connector.api.management.contractdefinition.transform.JsonObjectFromContractDefinitionTransformer;
import org.eclipse.edc.connector.api.management.contractdefinition.transform.JsonObjectToContractDefinitionTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.JsonObjectFromPolicyDefinitionTransformer;
import org.eclipse.edc.connector.api.management.policy.transform.JsonObjectToPolicyDefinitionTransformer;
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
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;

import java.util.Map;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

/**
 * This class is the entry point for the Blockchain Catalog API extension.
 */
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

    @Inject
    private DatasetResolver datasetResolver;

    @Inject
    DistributionResolver distributionResolver;
    @Inject
    TypeManager typeManager;

    @Inject
    DataServiceRegistry dataServiceRegistry;

    @Inject
    JsonLd jsonLd;

    private ServiceExtensionContext context;

    @Setting
    private static final String EDC_BLOCKCHAIN_INTERFACE_URL = "edc.blockchain.interface.url";

    private static final String DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL = "http://edc-interface:3000/";

    private String getEdcBlockchainInterfaceUrl() {
        return context.getSetting(EDC_BLOCKCHAIN_INTERFACE_URL, DEFAULT_EDC_BLOCKCHAIN_INTERFACE_URL);
    }



    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        this.context = context;
        monitor = context.getMonitor();
        monitor.info("Initializing Blockchain Catalog API Extension");
        registerTransformers();
        monitor.info("EDC Blockchain Interface URL: " + getEdcBlockchainInterfaceUrl());
        BlockchainSmartContractService blockchainSmartContractService = new BlockchainSmartContractService(monitor, transformerRegistry, validator, jsonLd, getEdcBlockchainInterfaceUrl());
        var catalogController = new BlockchainCatalogApiController(monitor, transformerRegistry, distributionResolver, dataServiceRegistry, jsonLd, blockchainSmartContractService);
        webService.registerResource("default", catalogController);

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


}
