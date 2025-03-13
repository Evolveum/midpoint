package com.evolveum.midpoint.ninja.action.mining.generator.object;

public enum RbacSecurityLevel {

    TIER_1("tier-1 security level"),
    TIER_2("tier-2 security level"),
    TIER_3("tier-3 security level"),
    TIER_4("tier-4 security level"),
    TIER_5("tier-5 security level");

    private final String displayValue;


    RbacSecurityLevel(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

}
