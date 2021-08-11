package ru.oshokin.pledgechecker.entities.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.IntStream;

@AllArgsConstructor
public class PledgeSearchServiceRequest {

    @JsonProperty("data")
    @Getter
    @Setter
    private List<PledgeSearchRequest> requests;

    public PledgeSearchServiceRequest() {
    }

    public void enumerateRequests() {
        IntStream.range(0, requests.size()).forEach(i -> requests.get(i).setId(i));
    }

}