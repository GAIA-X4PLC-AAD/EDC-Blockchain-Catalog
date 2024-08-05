package berlin.tu.ise.extension.helper;


import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value =  "TU Berlin Helper Extensions")
public class HelperExtension implements ServiceExtension {
    //@Inject
    //private FsVaultExtension fsVaultExtension;


    private  ServiceExtensionContext context;


    @Override
    public void initialize(ServiceExtensionContext context) {

        this.context = context;

        var monitor = context.getMonitor();

        monitor.info("HelperExtensions initialized");


    }


}
