package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles;

// TODO - this class is used for generating rm dataset. Do not forgot remove it.

public enum AuxBusinessRole {
    NOMAD("677fee5b-2f74-4b53-a54b-92b9800468ce"),
    VPN_REMOTE_ACCESS("c6f5d390-56a4-451f-bd81-6114e48548cf");

    private final String stringValue;

    AuxBusinessRole(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }
}
