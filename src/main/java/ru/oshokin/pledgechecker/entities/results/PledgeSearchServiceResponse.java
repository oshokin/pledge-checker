package ru.oshokin.pledgechecker.entities.results;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class PledgeSearchServiceResponse {

    @JsonProperty("data")
    @Getter
    @Setter
    private List<PledgeNotification> pledges;

    @JsonProperty("errors")
    @Getter
    @Setter
    private List<PledgeError> pledgeErrors;

    public PledgeSearchServiceResponse() {
    }

    public void addSearchResults(List<PledgeSearchResult> list) {
        for (PledgeSearchResult e : list) {
            if (e instanceof PledgeNotification) {
                if (pledges == null) pledges = new ArrayList<>();
                pledges.add((PledgeNotification) e);
            } else if (e instanceof PledgeError) {
                if (pledgeErrors == null) pledgeErrors = new ArrayList<>();
                pledgeErrors.add((PledgeError) e);
            }
        }
    }

}