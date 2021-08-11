package ru.oshokin.pledgechecker.entities.assets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({"type", "description"})
@AllArgsConstructor
public class Asset implements Property {

    @JsonProperty("type")
    @Getter
    @Setter
    private AssetType assetType = AssetType.OTHER;

    @Getter
    @Setter
    private String description;

    public Asset() {
    }

}
