package berlin.tu.ise;

import org.eclipse.edc.spi.monitor.Monitor;

public class NewService {
    private Monitor monitor = null;

    public NewService(Monitor monitor) {
        this.monitor = monitor;
    }

    public void newMethod() {
        monitor.info("New method called");
    }
}
