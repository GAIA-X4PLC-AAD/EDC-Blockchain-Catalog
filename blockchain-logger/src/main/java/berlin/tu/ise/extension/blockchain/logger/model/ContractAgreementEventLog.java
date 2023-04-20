package berlin.tu.ise.extension.blockchain.logger.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ContractAgreementEventLog {

    public String customerId;
    public String providerId;
    public String agreementId;
    public String contractRef;
    public String customerName;
    public String customerGaiaId;
    public String customerInvoiceAddress;
    public String invoiceDate;
    public String paymentTerm;
    public String currency;

    public ContractAgreementEventLog(String customerId, String providerId, String agreementId, String contractRef) {
        this.customerId = customerId;
        this.providerId = providerId;
        this.agreementId = agreementId;
        this.contractRef = contractRef;
    }

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

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }
}
