package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.objectTypes;

public enum BirthRoleType {

    CONTRACTOR("contractor"),
    EMPLOYEE("employee");

    private final String stringValue;

    BirthRoleType(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

}
