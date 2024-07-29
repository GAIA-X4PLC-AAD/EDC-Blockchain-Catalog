package berlin.tu.ise.extension.blockchain.logger.listener;

import berlin.tu.ise.extension.blockchain.logger.model.ContractAgreementEventLog;
import berlin.tu.ise.extension.blockchain.logger.model.ReturnOperationObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.asset.spi.event.AssetCreated;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationAccepted;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ContractAgreementEventSubscriber implements EventSubscriber {

        private Monitor monitor;

        private ContractNegotiationStore contractNegotiationStore;


        private String edcInterfaceUrl;

        private String ownConnectorId;


        public ContractAgreementEventSubscriber(Monitor monitor, ContractNegotiationStore contractNegotiationStore, String ownConnectorId, String edcInterfaceUrl) {
                this.monitor = monitor;
                this.contractNegotiationStore = contractNegotiationStore;
                this.ownConnectorId = ownConnectorId;
                this.edcInterfaceUrl = edcInterfaceUrl;
        }


        public void accepted(ContractNegotiationAgreed negotiationAgreed) {
                ContractNegotiation negotiation = contractNegotiationStore.findById(negotiationAgreed.getContractNegotiationId());
                String negotiationId = negotiation.getLastContractOffer().getId();


                monitor.debug("ContractNegotiation accepted: " + negotiationId);
                // wait until contract agreement is created for x seconds then fail
                for (int i = 0; i < 10; i++) {
                        if (negotiation.getContractAgreement() != null) {
                                break;
                        }
                        try {
                                Thread.sleep(1000);
                                monitor.debug("Waiting for ContractAgreement to be created for ContractNegotiation: " + negotiationId);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                        if (i == 9) {
                                monitor.debug("ContractAgreement could not be created for ContractNegotiation: " + negotiationId);
                                return;
                        }
                }

                ContractAgreement contractAgreement = negotiation.getContractAgreement();

                monitor.debug("ContractAgreement: " + contractAgreement.getId());

                String contractOfferId = negotiation.getLastContractOffer().getId();


                ContractAgreementEventLog contractAgreementEventLog = new ContractAgreementEventLog(ownConnectorId, negotiation.getCounterPartyId(), contractAgreement.getId(), contractOfferId);
                String jsonString;

                try {
                        jsonString = contractAgreementEventLog.toJson();
                        monitor.debug(jsonString);
                } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                }

                ReturnOperationObject returnObject = sendToSmartContract(jsonString, monitor, edcInterfaceUrl);
                if (returnObject != null && Objects.equals(returnObject.getStatus(), "ok")) {
                        monitor.debug("[ContractAgreementEventSubscriber] Data sent to Smart Contract");
                } else {
                        monitor.debug("[ContractAgreementEventSubscriber] Data could not be sent to Smart Contract");
                }

        }

        public static ReturnOperationObject sendToSmartContract(String jsonString, Monitor monitor, String smartContractUrl) {
                monitor.debug("[ContractAgreementEventSubscriber] Sending data to Smart Contract, this may take some time ...");
                String returnJson;
                ReturnOperationObject returnObject = null;
                try{
                        URL url = new URL(smartContractUrl + "/agreement/add");
                        HttpURLConnection http = (HttpURLConnection)url.openConnection();
                        http.setRequestMethod("POST");
                        http.setDoOutput(true);
                        http.setRequestProperty("Content-Type", "application/json");

                        byte[] out = jsonString.getBytes(StandardCharsets.UTF_8);

                        OutputStream stream = http.getOutputStream();
                        stream.write(out);

                        BufferedReader br;
                        if (100 <= http.getResponseCode() && http.getResponseCode() <= 399) {
                                br = new BufferedReader(new InputStreamReader(http.getInputStream()));
                        } else {
                                br = new BufferedReader(new InputStreamReader(http.getErrorStream()));
                        }

                        while ((returnJson = br.readLine()) != null) {
                                System.out.println(returnJson);
                                ObjectMapper mapper = new ObjectMapper();
                                returnObject = mapper.readValue(returnJson, ReturnOperationObject.class);
                        }

                        System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
                        http.disconnect();
                } catch(Exception e) {
                        monitor.severe(e.toString());
                }

                return returnObject;
        }

        @Override
        public <E extends Event> void on(EventEnvelope<E> event) {
                var payload = event.getPayload();
                if (!(payload instanceof ContractNegotiationAgreed)) return;
                accepted(((ContractNegotiationAgreed) payload));
        }
}
