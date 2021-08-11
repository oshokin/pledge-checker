package ru.oshokin.pledgechecker.entities.legal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({"type", "fullName", "inn", "registrationNumber", "location"})
@AllArgsConstructor
public class ForeignLegalEntity implements LegalEntity {

    @JsonProperty("type")
    @Getter
    @Setter
    private LegalEntityType legalEntityType = LegalEntityType.FOREIGN;

    @Getter
    @Setter
    private String fullName;

    @Getter
    @Setter
    private String INN;

    @Getter
    @Setter
    private String registrationNumber;

    @Getter
    @Setter
    private String Location;

    public ForeignLegalEntity() {
    }

}
