package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.objectTypes;

import java.util.Random;

public enum JobTitles {

    ENGINEER("engineer"),
    ANALYST("analyst"),
    REVIEWER("reviewer"),
    EDITOR("editor"),
    NONE("");

    private final String stringValue;

    JobTitles(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public static JobTitles getRandomlyJobTitlesWithNone() {
        Random random = new Random();
        return values()[random.nextInt(values().length)];
    }
    public static JobTitles getRandomlyJobTitles() {
        Random random = new Random();
        int index;
        do {
            index = random.nextInt(values().length);
        } while (values()[index] == NONE);
        return values()[index];
    }
}
