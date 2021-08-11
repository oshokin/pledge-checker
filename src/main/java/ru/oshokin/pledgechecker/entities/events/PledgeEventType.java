package ru.oshokin.pledgechecker.entities.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PledgeEventType {

    @JsonProperty("registration")
    REGISTRATION,
    @JsonProperty("modification")
    MODIFICATION,
    @JsonProperty("removal")
    REMOVAL

}