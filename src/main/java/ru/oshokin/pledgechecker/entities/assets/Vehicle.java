package ru.oshokin.pledgechecker.entities.assets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({"type", "vin", "pin", "description", "chassisNumber", "bodyNumber"})
@AllArgsConstructor
public class Vehicle implements Property {

    @JsonProperty("type")
    @Getter
    @Setter
    private AssetType assetType = AssetType.VEHICLE;

    @JsonProperty("vin")
    @Getter
    @Setter
    private String VIN;

    @JsonProperty("pin")
    @Getter
    @Setter
    private String PIN;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private String chassisNumber;

    @Getter
    @Setter
    private String bodyNumber;

    public Vehicle() {
    }

}
