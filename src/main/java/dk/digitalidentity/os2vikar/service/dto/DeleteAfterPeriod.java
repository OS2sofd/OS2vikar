package dk.digitalidentity.os2vikar.service.dto;

import lombok.Getter;

@Getter
public enum DeleteAfterPeriod {
    FIVE_DAYS("5 dage"),
    THREE_MONTHS("3 måneder"),
    SIX_MONTHS("6 måneder"),
    ONE_YEAR("1 år"),
    THREE_YEARS("3 år"),
    FIVE_YEARS("5 år");

    private String displayValue;

    private DeleteAfterPeriod(String displayValue) {
        this.displayValue = displayValue;
    }
}
