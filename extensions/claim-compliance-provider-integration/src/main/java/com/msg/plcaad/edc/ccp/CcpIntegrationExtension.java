package com.msg.plcaad.edc.ccp;

import com.msg.plcaad.edc.ccp.asset.CcpIntegrationForAssetService;
import com.msg.plcaad.edc.ccp.contractdefinition.CcpIntegrationForContractDefinitionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Provides({ CcpIntegrationForAssetService.class })
@Extension(value = CcpIntegrationExtension.NAME)
public class CcpIntegrationExtension implements ServiceExtension {
    protected static final String NAME = "Claim Compliance Provider Integration";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        final Monitor monitor = context.getMonitor();
        context.registerService(CcpIntegrationForAssetService.class, new CcpIntegrationForAssetService(monitor));
        context.registerService(CcpIntegrationForContractDefinitionService.class, new CcpIntegrationForContractDefinitionService(monitor));
    }
}
