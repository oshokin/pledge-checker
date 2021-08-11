package ru.oshokin.pledgechecker.entities.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@JsonPropertyOrder({"type", "dateTime", "number"})
@AllArgsConstructor
public class PledgeRemoval implements PledgeEvent {

    @JsonProperty("type")
    @Getter
    @Setter
    private PledgeEventType notificationType = PledgeEventType.REMOVAL;

    @Getter
    @Setter
    private LocalDateTime dateTime;

    @Getter
    @Setter
    private String number;

    public PledgeRemoval() {
    }

}
