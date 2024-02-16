package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles;

import com.evolveum.midpoint.prism.polystring.PolyString;

import java.util.Random;

public enum LocationBusinessRole {
    LONDON("f087236c-1fad-4113-b3d5-f4c9226c06e6", "London"),
    PARIS("dd9222c5-b0e8-4cee-988e-108a0f62570a", "Paris"),
    BERLIN("50958537-073f-49ea-b560-ab5a48f4ed8d", "Berlin"),
    NEW_YORK("068463a6-bbb4-4fd7-a491-4d082f56d505", "New York"),
    TOKYO("bdc41dd1-42f3-4d6b-b986-fcf535e8dbd4", "Tokyo");

    private final String stringValue;
    private final String stringValue1;

    LocationBusinessRole(String stringValue, String stringValue1) {
        this.stringValue = stringValue;
        this.stringValue1 = stringValue1;
    }

    public String getOid() {
        return stringValue;
    }

    public PolyString getLocale() {
        return PolyString.fromOrig(stringValue1);
    }

    public static LocationBusinessRole getRandomLocationBusinessRole() {
        Random random = new Random();
        return values()[random.nextInt(values().length)];
    }
}
