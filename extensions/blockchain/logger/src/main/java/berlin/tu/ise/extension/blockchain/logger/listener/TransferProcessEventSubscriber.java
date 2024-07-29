package berlin.tu.ise.extension.blockchain.logger.listener;

import berlin.tu.ise.extension.blockchain.logger.model.ReturnOperationObject;
import berlin.tu.ise.extension.blockchain.logger.model.TransferProcessEventLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
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

public class TransferProcessEventSubscriber implements EventSubscriber {

    private Monitor monitor;

    private TransferProcessStore transferProcessStore;

    private ContractDefinitionStore contractDefinitionStore;

    private ContractNegotiationStore contractNegotiationStore;

    private String edcInterfaceUrl;

    private String ownConnectorId;

    public TransferProcessEventSubscriber(Monitor monitor, TransferProcessStore transferProcessStore, ContractDefinitionStore contractDefinitionStore, ContractNegotiationStore contractNegotiationStore, String ownConnectorId, String edcInterfaceUrl) {
        this.monitor = monitor;
        this.transferProcessStore = transferProcessStore;
        this.contractDefinitionStore = contractDefinitionStore;
        this.contractNegotiationStore = contractNegotiationStore;
        this.ownConnectorId = ownConnectorId;
        this.edcInterfaceUrl = edcInterfaceUrl;
    }

    /*
        TransferProcessCancelled
        TransferProcessCompleted
        TransferProcessDeprovisioned
        TransferProcessDeprovisioningRequested
        TransferProcessEnded
        TransferProcessEvent
        TransferProcessFailed
        TransferProcessInitiated
        TransferProcessProvisioned
        TransferProcessProvisioningRequested
        TransferProcessRequested
        */

    @Override
    public <E extends Event> void on(EventEnvelope<E> eventEnvelope) {
        var event = eventEnvelope.getPayload();
        if (!(event instanceof TransferProcessInitiated process)) {
            return;
        }

        TransferProcess transferProcess = transferProcessStore.findById(process.getTransferProcessId());

        if (transferProcess == null) {
            monitor.warning(String.format("Transfer Process with id %s not found in local Transfer Process Store.", process.getTransferProcessId()));
            return;
        }

        transferProcessInitiated(transferProcess);
    }

    public void transferProcessInitiated(TransferProcess process) {

        monitor.debug("Transfer Process Event: " + process.getDataRequest().getProcessId());

        // monitor.debug("Transfer Process Completed");
        DataRequest dataRequest = process.getDataRequest();
        String assetId = dataRequest.getAssetId();
        String contractId = dataRequest.getContractId(); // better to get aggrement id?

        String connectorId = dataRequest.getConnectorId();

        String agreementId = contractNegotiationStore.findContractAgreement(contractId).getId();

        // TODO: How to get agreement Id from transfer
        TransferProcessEventLog transferProcessEventLog = new TransferProcessEventLog(assetId, ownConnectorId, connectorId, agreementId);
        String jsonString;

        try {
            jsonString = transferProcessEventLog.toJson();
            monitor.debug(jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ReturnOperationObject returnObject = sendToSmartContract(jsonString, monitor, edcInterfaceUrl);
        if (returnObject != null && Objects.equals(returnObject.getStatus(), "ok")) {
            monitor.debug("[TransferProcessEventSubscriber] Data sent to Smart Contract");
        } else {
            monitor.debug("[TransferProcessEventSubscriber] Data could not be sent to Smart Contract");
        }
    }

    public static ReturnOperationObject sendToSmartContract(String jsonString, Monitor monitor, String smartContractUrl) {
        monitor.debug("[TransferProcessEventSubscriber] Sending data to Smart Contract, this may take some time ...");
        String returnJson;
        ReturnOperationObject returnObject = null;
        try {
            URL url = new URL(smartContractUrl + "/transfer/add");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
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
        } catch (Exception e) {
            monitor.severe(e.toString());
        }

        return returnObject;
    }
}