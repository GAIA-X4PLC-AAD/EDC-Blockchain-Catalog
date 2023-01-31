package berlin.tu.ise.extension.blockchain.catalog.listener;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.event.asset.AssetCreated;
import org.eclipse.edc.spi.monitor.Monitor;

public class BlockchainAssetCreationSubscriber implements EventSubscriber {

    private final Monitor monitor;

    BlockchainAssetCreator blockchainAssetCreator;

    public BlockchainAssetCreationSubscriber(BlockchainAssetCreator blockchainAssetCreator, Monitor monitor) {
        this.blockchainAssetCreator = blockchainAssetCreator;
        this.monitor = monitor;
    }

    @Override
    public void on(Event event) {
        monitor.debug("BlockchainAssetCreationSubscriber Event triggered");
        if (event instanceof AssetCreated) {
            // react only to AssetCreated events
            monitor.debug("AssetCreated event received of " + event);
            monitor.debug("AssetCreated event received with payload " + event.getPayload());

        }

        /*
        if (event.getPayload() instanceof TransferProcessEventPayload) {
            // react on Events related to TransferProcessEvents
        }
        */
    }
}
