package berlin.tu.ise.extension.blockchain.logger.listener;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationConfirmed;
import org.eclipse.edc.spi.event.transferprocess.TransferProcessEvent;
import org.eclipse.edc.spi.monitor.Monitor;

public class ContractAgreementEventSubscriber implements EventSubscriber {

        private Monitor monitor;

        private ContractNegotiationStore contractNegotiationStore;

        public ContractAgreementEventSubscriber(Monitor monitor, ContractNegotiationStore contractNegotiationStore) {
                this.monitor = monitor;
                this.contractNegotiationStore = contractNegotiationStore;
        }

        @Override
        public void on(Event event) {
                if (event instanceof ContractNegotiationConfirmed) {
                        ContractNegotiationConfirmed contractNegotiationConfirmed = (ContractNegotiationConfirmed) event;
                        String negotiationId = contractNegotiationConfirmed.getPayload().getContractNegotiationId();
                        ContractNegotiation contractNegotiation = contractNegotiationStore.find(negotiationId);

                        monitor.debug("ContractNegotiationConfirmed: " + negotiationId);
                        while (contractNegotiation.getContractAgreement() == null) {
                                try {
                                        Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                        e.printStackTrace();
                                }
                        }
                        ContractAgreement contractAgreement = contractNegotiation.getContractAgreement();
                        monitor.debug("ContractAgreement: " + contractAgreement.getId());
                }
        }
}
