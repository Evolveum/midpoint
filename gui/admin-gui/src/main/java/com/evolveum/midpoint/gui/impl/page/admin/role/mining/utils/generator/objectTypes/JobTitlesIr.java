package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.objectTypes;

import java.util.Random;

public enum JobTitlesIr {

    CLERK("clerk"),
    ENGINEER("engineer"),
    ANALYST("analyst"),
    OPERATOR("operator"),
    AGENT("agent"),
    ASSISTANT("assistant"),
    NONE("");

    private final String stringValue;

    JobTitlesIr(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public static JobTitlesIr getRandomlyJobTitlesWithNone() {
        Random random = new Random();
        return values()[random.nextInt(values().length)];
    }
    public static JobTitlesIr getRandomlyJobTitles() {
        Random random = new Random();
        int index;
        do {
            index = random.nextInt(values().length);
        } while (values()[index] == NONE);
        return values()[index];
    }
}
