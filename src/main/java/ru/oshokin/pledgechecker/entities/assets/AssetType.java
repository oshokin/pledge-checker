package ru.oshokin.pledgechecker.entities.assets;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AssetType {

    @JsonProperty("vehicle")
    VEHICLE,
    @JsonProperty("other")
    OTHER

}
