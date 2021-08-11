package ru.oshokin.pledgechecker.entities.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.oshokin.pledgechecker.entities.assets.Property;
import ru.oshokin.pledgechecker.entities.data.PledgeContract;
import ru.oshokin.pledgechecker.entities.legal.LegalEntity;

import java.time.LocalDateTime;
import java.util.List;

@JsonPropertyOrder({"type", "dateTime", "number", "assets", "pledgors", "pledgees", "contract"})
@AllArgsConstructor
public class PledgeRegistration implements PledgeEvent {

    @JsonProperty("type")
    @Getter
    @Setter
    private PledgeEventType notificationType = PledgeEventType.REGISTRATION;

    @Getter
    @Setter
    private LocalDateTime dateTime;

    @Getter
    @Setter
    private String number;

    @Getter
    @Setter
    private List<Property> assets;

    @Getter
    @Setter
    private List<LegalEntity> pledgors;

    @Getter
    @Setter
    private List<LegalEntity> pledgees;

    @Getter
    @Setter
    private PledgeContract contract;

    public PledgeRegistration() {
    }

}
