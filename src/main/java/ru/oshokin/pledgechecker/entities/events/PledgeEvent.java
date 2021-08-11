package ru.oshokin.pledgechecker.entities.events;
import java.time.LocalDateTime;

public interface PledgeEvent {
    PledgeEventType getNotificationType();
    LocalDateTime getDateTime();
    void setDateTime(LocalDateTime dateTime);
    String getNumber();
    void setNumber(String number);
}