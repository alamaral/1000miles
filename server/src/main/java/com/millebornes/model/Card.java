package com.millebornes.model;

public enum Card {
    // Distance cards
    MILES_25("25 Miles", CardType.DISTANCE, 25, null),
    MILES_50("50 Miles", CardType.DISTANCE, 50, null),
    MILES_75("75 Miles", CardType.DISTANCE, 75, null),
    MILES_100("100 Miles", CardType.DISTANCE, 100, null),
    MILES_200("200 Miles", CardType.DISTANCE, 200, null),

    // Hazard cards
    ACCIDENT("Accident", CardType.HAZARD, null, HazardType.ACCIDENT),
    OUT_OF_GAS("Out of Gas", CardType.HAZARD, null, HazardType.OUT_OF_GAS),
    FLAT_TIRE("Flat Tire", CardType.HAZARD, null, HazardType.FLAT_TIRE),
    STOP("Stop", CardType.HAZARD, null, HazardType.STOP),
    SPEED_LIMIT("Speed Limit", CardType.HAZARD, null, HazardType.SPEED_LIMIT),

    // Remedy cards
    REPAIRS("Repairs", CardType.REMEDY, null, HazardType.ACCIDENT),
    GASOLINE("Gasoline", CardType.REMEDY, null, HazardType.OUT_OF_GAS),
    SPARE_TIRE("Spare Tire", CardType.REMEDY, null, HazardType.FLAT_TIRE),
    ROLL("Roll", CardType.REMEDY, null, HazardType.STOP),
    END_OF_LIMIT("End of Limit", CardType.REMEDY, null, HazardType.SPEED_LIMIT),

    // Safety cards
    DRIVING_ACE("Driving Ace", CardType.SAFETY, null, HazardType.ACCIDENT),
    EXTRA_TANK("Extra Tank", CardType.SAFETY, null, HazardType.OUT_OF_GAS),
    PUNCTURE_PROOF("Puncture Proof", CardType.SAFETY, null, HazardType.FLAT_TIRE),
    RIGHT_OF_WAY("Right of Way", CardType.SAFETY, null, HazardType.STOP);

    private final String displayName;
    private final CardType cardType;
    private final Integer value;
    private final HazardType hazardType;

    Card(String displayName, CardType cardType, Integer value, HazardType hazardType) {
        this.displayName = displayName;
        this.cardType = cardType;
        this.value = value;
        this.hazardType = hazardType;
    }

    public String getDisplayName() { return displayName; }
    public CardType getCardType() { return cardType; }
    public Integer getValue() { return value; }
    public HazardType getHazardType() { return hazardType; }
}
