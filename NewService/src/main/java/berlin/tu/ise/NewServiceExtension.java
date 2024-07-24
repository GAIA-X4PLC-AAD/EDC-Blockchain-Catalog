package berlin.tu.ise;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Provides({ NewService.class })
@Extension(value = NewServiceExtension.NAME)
public class NewServiceExtension implements ServiceExtension {
    public static final String NAME = "Blockchain Extension: Catalog";


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        context.registerService(NewService.class, new NewService(monitor));
    }

}
