package ru.oshokin.pledgechecker.entities.legal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@JsonPropertyOrder({"type", "surname", "name", "patronymic", "surnameInLatinLetters",
        "nameInLatinLetters", "patronymicInLatinLetters", "birthDate",
        "identityDocument", "actualResidenceAddressInRF", "email"})
@AllArgsConstructor
public class PhysicalLegalEntity implements LegalEntity {

    @JsonProperty("type")
    @Getter
    @Setter
    private LegalEntityType legalEntityType = LegalEntityType.PHYSICAL;

    @Getter
    @Setter
    private String surname;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String patronymic;

    @Getter
    @Setter
    private String surnameInLatinLetters;

    @Getter
    @Setter
    private String nameInLatinLetters;

    @Getter
    @Setter
    private String patronymicInLatinLetters;

    @Getter
    @Setter
    private LocalDate birthDate;

    @Getter
    @Setter
    private String identityDocument;

    @Getter
    @Setter
    private String actualResidenceAddressInRF;

    @Getter
    @Setter
    private String email;

    public PhysicalLegalEntity() {
    }

}
