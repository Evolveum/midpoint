package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.objectTypes;

public enum EmployeeType {
    REGULAR("regular"),
    SEMI_REGULAR("semi-regular"),
    MANAGER("managers"),
    SALES("sales"),
    SECURITY_OFFICER("security officers"),
    IRREGULAR("irregular"),
    CONTRACTOR("contractor");
    private final String stringValue;

    EmployeeType(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

}
