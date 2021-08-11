package ru.oshokin.pledgechecker.entities.data;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@JsonPropertyOrder({"name", "date", "number", "term"})
@AllArgsConstructor
public class PledgeContract {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private LocalDate date;

    @Getter
    @Setter
    private String number;

    @Getter
    @Setter
    private String term;

    public PledgeContract() {
    }

}
