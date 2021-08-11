package ru.oshokin.pledgechecker.entities.legal;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum LegalEntityType {

    @JsonProperty("physical")
    PHYSICAL,
    @JsonProperty("russian")
    RUSSIAN,
    @JsonProperty("foreign")
    FOREIGN

}
