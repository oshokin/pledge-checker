package ru.oshokin.pledgechecker.entities.legal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({"type", "fullName", "inn", "ogrn", "location"})
@AllArgsConstructor
public class RussianLegalEntity implements LegalEntity {

    @JsonProperty("type")
    @Getter
    @Setter
    private LegalEntityType legalEntityType = LegalEntityType.RUSSIAN;

    @Getter
    @Setter
    private String fullName;

    @Getter
    @Setter
    private String INN;

    @Getter
    @Setter
    private String OGRN;

    @Getter
    @Setter
    private String Location;

    public RussianLegalEntity() {
    }

}
