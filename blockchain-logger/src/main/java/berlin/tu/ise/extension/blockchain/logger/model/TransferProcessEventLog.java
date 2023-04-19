package berlin.tu.ise.extension.blockchain.logger.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.api.model.BaseResponseDto;
import org.eclipse.edc.api.model.CriterionDto;

import java.util.List;

public class TransferProcessEventLog {
    public String assetId;
    public String consumerId;
    public String providerId;
    public String transactionId;
    public String contractRef;
    public String customerName;
    public String customerGaiaId;
    public String customerInvoiceAddress;
    public String invoiceDate;
    public String paymentTerm;
    public String currency;

    public String getCustomerName() {
        return customerName == null ? "customerName_placeholder" : customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerGaiaId() {
        return customerGaiaId == null ? "customerGaiaId_placeholder" : customerGaiaId;
    }

    public void setCustomerGaiaId(String customerGaiaId) {
        this.customerGaiaId = customerGaiaId;
    }

    public String getCustomerInvoiceAddress() {
        return customerInvoiceAddress == null ? "customerInvoiceAddress_placeholder" : customerInvoiceAddress;
    }

    public void setCustomerInvoiceAddress(String customerInvoiceAddress) {
        this.customerInvoiceAddress = customerInvoiceAddress;
    }

    public String getInvoiceDate() {
        return invoiceDate == null ? "2021-01-01" : invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getPaymentTerm() {
        return paymentTerm == null ? "paymentTerm_placeholder" : paymentTerm;
    }

    public void setPaymentTerm(String paymentTerm) {
        this.paymentTerm = paymentTerm;
    }

    public String getCurrency() {
        return currency == null ? "EUR" : currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransferProcessEventLog(String assetId, String consumerId, String providerId, String transactionId, String contractRef) {
        this.assetId = assetId;
        this.consumerId = consumerId;
        this.providerId = providerId;
        this.transactionId = transactionId;
        this.contractRef = contractRef;
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getContractRef() {
        return contractRef;
    }

    public void setContractRef(String contractRef) {
        this.contractRef = contractRef;
    }
}