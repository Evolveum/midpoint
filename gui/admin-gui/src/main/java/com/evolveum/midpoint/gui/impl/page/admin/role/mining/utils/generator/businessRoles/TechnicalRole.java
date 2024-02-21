package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles;

// TODO - this class is used for generating rm dataset. Do not forgot remove it.

public enum TechnicalRole {
    AD_AUDITOR("3e63b80a-23c8-4fec-97a3-22923190887b");

    private final String stringValue;

    TechnicalRole(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }
}
