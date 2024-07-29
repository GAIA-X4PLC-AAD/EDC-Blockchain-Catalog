package berlin.tu.ise.extension.blockchain.logger.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class TransferProcessEventLog {
    public String assetId;
    public String consumerId;
    public String providerId;
    public String agreementId;



    public TransferProcessEventLog(String assetId, String consumerId, String providerId, String agreementId) {
        this.assetId = assetId;
        this.consumerId = consumerId;
        this.providerId = providerId;
        this.agreementId = agreementId;
    }

    public TransferProcessEventLog() {
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(String agreementId) {
        this.agreementId = agreementId;
    }

}