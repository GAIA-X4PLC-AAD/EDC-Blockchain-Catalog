package berlin.tu.ise.extension.blockchain.catalog.listener.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.api.model.BaseResponseDto;
import org.eclipse.edc.api.model.CriterionDto;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(builder = ContractOfferDto.Builder.class)
public class ContractOfferDto extends BaseResponseDto {
    private String id;
    private String accessPolicyId;
    private String contractPolicyId;

    private String dataUrl;
    private List<CriterionDto> criteria = new ArrayList<>();

    private ContractOfferDto() {
    }

    public String getAccessPolicyId() {
        return accessPolicyId;
    }

    public String getContractPolicyId() {
        return contractPolicyId;
    }

    public List<CriterionDto> getCriteria() {
        return criteria;
    }

    public String getId() {
        return id;
    }

    public String getDataUrl() { return dataUrl; }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends BaseResponseDto.Builder<ContractOfferDto, ContractOfferDto.Builder> {
        private Builder() {
            super(new ContractOfferDto());
        }

        @JsonCreator
        public static ContractOfferDto.Builder newInstance() {
            return new ContractOfferDto.Builder();
        }

        public ContractOfferDto.Builder accessPolicyId(String accessPolicyId) {
            dto.accessPolicyId = accessPolicyId;
            return this;
        }

        public ContractOfferDto.Builder contractPolicyId(String contractPolicyId) {
            dto.contractPolicyId = contractPolicyId;
            return this;
        }

        public ContractOfferDto.Builder criteria(List<CriterionDto> criteria) {
            dto.criteria = criteria;
            return this;
        }

        public ContractOfferDto.Builder dataUrl(String dataUrl) {
            dto.dataUrl = dataUrl;
            return this;
        }

        @Override
        public ContractOfferDto.Builder self() {
            return this;
        }

        public ContractOfferDto.Builder id(String id) {
            dto.id = id;
            return this;
        }
    }
}

