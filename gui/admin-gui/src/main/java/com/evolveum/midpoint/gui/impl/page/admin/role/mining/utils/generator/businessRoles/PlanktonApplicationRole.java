package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.generator.businessRoles;

import java.util.*;

public enum PlanktonApplicationRole {
    WEB_EDITOR("45566a80-c2a8-402f-835e-741ed38fb121"),
    WEB_ANALYTICS("a80ac1d7-2d9a-49e1-8932-9a7b735baa04"),
    AD_POWER_USER("19c06851-f176-4359-a5d2-d3b4d33d2ddb"),
    SAP_BASIC("119d0d21-02da-4e7f-91c1-e9ed45a376d5"),
    DATA_WAREHOUSE_READER("b4c860a6-b4a4-4ba6-b977-96a390c074f2"),
    DB_SQL_ACCESS("b9fd1467-d6c0-43ec-a438-41f9eaa40b74"),
    CRM_READER("f8696319-2805-4053-904f-22f3e3f19026"),
    CRM_WRITER("62861882-eeb3-4f32-a050-a62f8a3bf0b8"),
    CRM_MANAGER("f09d6584-ce9b-48dc-8b10-657de5fcc3eb"),
    ERP_REQUESTOR("a0cae92c-a2e5-49e5-b9d7-114bbc45bb97"),
    ERP_APPROVER("c0b727b8-0184-4ab3-bcee-f0483399a3ae"),
    ERP_READER("377031b5-3738-4954-a405-2b6801302e53"),
    ERP_AUDITOR("c97511da-d6f1-4a8a-9f94-4b0f97a0431a"),
    CRM_ADMIN("e811f194-4810-4eb8-a1bf-9c65182433c9");

    private final String stringValue;

    PlanktonApplicationRole(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public static List<PlanktonApplicationRole> getRandomPlanktonRoles() {
        Random random = new Random();
        int numRoles = random.nextInt(PlanktonApplicationRole.values().length);
        Set<PlanktonApplicationRole> selectedRoles = new HashSet<>();

        while (selectedRoles.size() < numRoles) {
            PlanktonApplicationRole randomRole = getRandomPlanktonRole();
            selectedRoles.add(randomRole);
        }

        return new ArrayList<>(selectedRoles);
    }

    public static List<PlanktonApplicationRole> getLargeRandomPlanktonRoles() {
        int minRoles = 7;
        int maxRoles = PlanktonApplicationRole.values().length;

        Random random = new Random();
        int numRoles = minRoles + random.nextInt(maxRoles - minRoles + 1);

        Set<PlanktonApplicationRole> selectedRoles = EnumSet.noneOf(PlanktonApplicationRole.class);

        while (selectedRoles.size() < numRoles) {
            PlanktonApplicationRole randomRole = getRandomPlanktonRole();
            selectedRoles.add(randomRole);
        }

        return new ArrayList<>(selectedRoles);
    }


    public static PlanktonApplicationRole getRandomPlanktonRole() {
        Random random = new Random();
        return values()[random.nextInt(values().length)];
    }
}
