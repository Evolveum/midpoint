package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles;

// TODO - this class is used for generating rm dataset. Do not forgot remove it.

public enum BirthrightRole {
    EMPLOYEE("4ecd723a-1b80-442d-8335-8512fb141788"),
    CONTRACTOR("0f3e3736-89b7-4c51-9424-28f193447229");

    private final String stringValue;

    BirthrightRole(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }
}
