package ru.oshokin.pledgechecker.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@AllArgsConstructor
public class PledgeParserBatch {

    @Getter
    @Setter
    private long requestIndex;

    @Getter
    @Setter
    private String number;

    @Getter
    @Setter
    private Path filePath;

    public PledgeParserBatch() {
    }

}
