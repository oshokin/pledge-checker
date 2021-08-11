package ru.oshokin.pledgechecker.entities.results;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.oshokin.pledgechecker.entities.assets.Property;
import ru.oshokin.pledgechecker.entities.data.PledgeContract;
import ru.oshokin.pledgechecker.entities.events.PledgeEvent;
import ru.oshokin.pledgechecker.entities.legal.LegalEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({"requestIndex", "number", "dateTime", "active", "assets", "pledgors", "pledgees", "contract", "history"})
@AllArgsConstructor
public class PledgeNotification implements PledgeSearchResult {

    @Getter
    @Setter
    private long requestIndex;

    @Getter
    @Setter
    private String number;

    @Getter
    @Setter
    private LocalDateTime dateTime;

    @Getter
    @Setter
    private boolean isActive;

    @Getter
    @Setter
    private List<Property> assets;

    //Залогодатели
    @Getter
    @Setter
    private List<LegalEntity> pledgors;

    //Залогодержатели
    @Getter
    @Setter
    private List<LegalEntity> pledgees;

    @Getter
    @Setter
    private PledgeContract contract;

    @Getter
    @Setter
    private List<PledgeEvent> history;

    public PledgeNotification() {
    }

    public void addEventToHistory(PledgeEvent pe) {
        if (history == null) history = new ArrayList<>();
        history.add(pe);
    }

}