/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.object;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * The InitialObjectsDefinition class facilitates the generation of initial objects, including roles, organizations,
 * and archetypes, which are essential for data generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public class InitialObjectsDefinition {

    protected static final String BUSINESS_ROLE_ARCHETYPE_OID = "00000000-0000-0000-0000-000000000321";
    static GeneratorOptions options;

    public InitialObjectsDefinition(@NotNull GeneratorOptions options) {
        InitialObjectsDefinition.options = options;
    }

    public List<RoleType> getBusinessRolesObjects() {
        List<RoleType> roleObjects = new ArrayList<>();
        addBusinessRoleObjects(BirthrightBusinessRole.values(), roleObjects);
        if (options.isAuxInclude()) {
            addBusinessRoleObjects(AuxInitialBusinessRole.values(), roleObjects);
        }
        addBusinessRoleObjects(JobInitialBusinessRole.values(), roleObjects);
        addBusinessRoleObjects(LocationInitialBusinessRole.values(), roleObjects);
        addBusinessRoleObjects(TechnicalBusinessRole.values(), roleObjects);
        return roleObjects;
    }

    public BasicAbstractRole[] getBasicRolesObjects() {
        return BasicAbstractRole.values();
    }

    public List<RoleType> getPlanktonRolesObjects() {
        List<RoleType> roleObjects = new ArrayList<>();
        addRoleObjects(PlanktonApplicationBusinessAbstractRole.values(), roleObjects);
        return roleObjects;
    }

    public static List<RoleType> getNoiseRolesObjects() {
        List<RoleType> roleObjects = new ArrayList<>();
        addRoleObjects(NoiseApplicationBusinessAbstractRole.values(), roleObjects);
        return roleObjects;
    }

    public List<OrgType> getOrgObjects() {
        List<OrgType> orgObjects = new ArrayList<>();
        addOrgObjects(Organization.values(), orgObjects);
        return orgObjects;
    }

    public List<ArchetypeType> getArchetypeObjects() {
        List<ArchetypeType> archetypeObjects = new ArrayList<>();
        addArchetypeObjects(Archetypes.values(), archetypeObjects);
        return archetypeObjects;
    }

    private static void addBusinessRoleObjects(
            InitialBusinessRole @NotNull [] roles,
            @NotNull List<RoleType> roleObjects) {
        for (InitialBusinessRole role : roles) {
            RoleType roleType = role.generateRoleObject();
            roleObjects.add(roleType);
        }
    }

    private static void addRoleObjects(
            InitialAbstractRole @NotNull [] roles,
            @NotNull List<RoleType> roleObjects) {
        for (InitialAbstractRole role : roles) {
            List<RoleType> roleType = role.generateRoleObject();
            roleObjects.addAll(roleType);
        }
    }

    private static void addOrgObjects(
            InitialOrg @NotNull [] orgs,
            @NotNull List<OrgType> orgObjects) {
        for (InitialOrg org : orgs) {
            OrgType orgType = org.generateOrgObject();
            orgObjects.add(orgType);
        }
    }

    private static void addArchetypeObjects(
            InitialArchetype @NotNull [] archetypes,
            @NotNull List<ArchetypeType> archetypeObjects) {
        for (InitialArchetype archetype : archetypes) {
            ArchetypeType archetypeType = archetype.generateArchetype();
            archetypeObjects.add(archetypeType);
        }
    }

    public enum BirthrightBusinessRole implements InitialBusinessRole {
        EMPLOYEE("4ecd723a-1b80-442d-8335-8512fb141788", "Employee",
                List.of(BasicAbstractRole.AD_GROUP_EMPLOYEES,
                        BasicAbstractRole.EXCHANGE_EMAIL,
                        BasicAbstractRole.EXCHANGE_CALENDAR)),
        CONTRACTOR("0f3e3736-89b7-4c51-9424-28f193447229", "Contractor",
                List.of(BasicAbstractRole.AD_GROUP_CONTRACTORS,
                        BasicAbstractRole.EXIT_PROCEDURES,
                        BasicAbstractRole.IT_PRIVILEGES));

        private final String oid;
        private final String name;
        private final List<BasicAbstractRole> associations;

        BirthrightBusinessRole(String oid, String name, List<BasicAbstractRole> associations) {
            this.oid = oid;
            this.name = name;
            this.associations = associations;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public @Nullable List<BasicAbstractRole> getAssociations() {
            return associations;
        }

        @Override
        public String getArchetypeOid() {
            return BUSINESS_ROLE_ARCHETYPE_OID;
        }

    }

    public enum AuxInitialBusinessRole implements InitialBusinessRole {
        NOMAD("677fee5b-2f74-4b53-a54b-92b9800468ce", "Nomad",
                List.of(BasicAbstractRole.LONDON,
                        BasicAbstractRole.PARIS,
                        BasicAbstractRole.BERLIN,
                        BasicAbstractRole.NEW_YORK,
                        BasicAbstractRole.TOKYO)),
        VPN_REMOTE_ACCESS("c6f5d390-56a4-451f-bd81-6114e48548cf", "VPN Remote Access",
                List.of(BasicAbstractRole.VPN_ACCESS,
                        BasicAbstractRole.VPN_REMOTE,
                        BasicAbstractRole.VPN_EXTERNAL));
        private final String oid;
        private final String name;
        private final List<BasicAbstractRole> associations;

        AuxInitialBusinessRole(String oid, String name, List<BasicAbstractRole> associations) {
            this.oid = oid;
            this.name = name;
            this.associations = associations;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public @Nullable List<BasicAbstractRole> getAssociations() {
            return associations;
        }

        @Override
        public String getArchetypeOid() {
            return BUSINESS_ROLE_ARCHETYPE_OID;
        }

    }

    public enum JobInitialBusinessRole implements InitialBusinessRole {
        ASSISTANT("47a627c4-fd0f-42e3-a2d0-3ef0cba09ac1", "Assistant",
                List.of(BasicAbstractRole.CALENDAR_POWER_USER, BasicAbstractRole.AD_GROUP_EMPLOYEES)),
        SUPERVISOR("ccc98c8d-0313-4961-9957-929aa0336620", "Supervisor",
                List.of(BasicAbstractRole.SUPERVISOR, BasicAbstractRole.EXTERNAL)),
        HR_CLERK("ea84c7ca-74d9-43e8-ad2f-535e5f6022ef", "HR Clerk",
                List.of(BasicAbstractRole.HR_APPROVER, BasicAbstractRole.SAP_BASIC, BasicAbstractRole.SAP_HR)),
        HQ_CLERK("f1b1b3b4-1b3b-4b3b-8b3b-1b3b3b3b3b3b", "HQ Clerk",
                List.of(BasicAbstractRole.HQ_APPROVER, BasicAbstractRole.SAP_HQ)),
        IRREGULAR("f1b1b3b4-1b3b-4b3b-8b3b-1b3b3b3b3b3b", "Irregular",
                List.of(BasicAbstractRole.PS_ASSISTANT, BasicAbstractRole.DE_Specialist, BasicAbstractRole.TE_COORDINATOR)),
        SECURITY_OFFICER("084b93f5-2f23-4087-83ec-94f4f8f5183a", "Security Officer",
                List.of(BasicAbstractRole.AD_AUDITOR, BasicAbstractRole.FIREWALL_ADMIN, BasicAbstractRole.NIPPON_DB)),
        SALES("f188128d-5e50-4ea0-9d14-79c10e030cf9", "Sales",
                List.of(BasicAbstractRole.CRM_READER, BasicAbstractRole.CRM_WRITER, BasicAbstractRole.WEB_ANALYTICS)),
        MANAGER("abf214b8-2d0e-4ec8-8000-bc0f4dfa5466", "Manager",
                List.of(BasicAbstractRole.AD_MANAGER_GROUP, BasicAbstractRole.AD_MANAGER_USER));

        private final String oid;
        private final String name;
        private final List<BasicAbstractRole> associations;

        JobInitialBusinessRole(String oid, String name, List<BasicAbstractRole> associations) {
            this.oid = oid;
            this.name = name;
            this.associations = associations;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public @Nullable List<BasicAbstractRole> getAssociations() {
            return associations;
        }

        @Override
        public String getArchetypeOid() {
            return BUSINESS_ROLE_ARCHETYPE_OID;
        }

    }

    public enum LocationInitialBusinessRole implements InitialBusinessRole {
        LOCATION_LONDON("f087236c-1fad-4113-b3d5-f4c9226c06e6", "Location London", "London",
                List.of(BasicAbstractRole.LONDON_AD, BasicAbstractRole.LONDON_BUILDING_ACCESS)),
        LOCATION_PARIS("dd9222c5-b0e8-4cee-988e-108a0f62570a", "Location Paris", "Paris",
                List.of(BasicAbstractRole.EUROPE_AD, BasicAbstractRole.PARIS_BUILDING_ACCESS)),
        LOCATION_BERLIN("50958537-073f-49ea-b560-ab5a48f4ed8d", "Location Berlin", "Berlin",
                List.of(BasicAbstractRole.EUROPE_AD, BasicAbstractRole.BERLIN_BUILDING_ACCESS)),
        LOCATION_NEW_YORK("068463a6-bbb4-4fd7-a491-4d082f56d505", "Location New York", "New York",
                List.of(BasicAbstractRole.EAST_COAST_AD,
                        BasicAbstractRole.NEW_YORK_MANHATTAN_BUILDING_ACCESS,
                        BasicAbstractRole.NEW_YORK_BROOKLYN_BUILDING_ACCESS)),
        LOCATION_TOKYO("bdc41dd1-42f3-4d6b-b986-fcf535e8dbd4", "Location Tokyo", "Tokyo",
                List.of(BasicAbstractRole.TOKIO_AD, BasicAbstractRole.TOKIO_BUILDING_ACCESS));

        private final String oid;
        private final String name;
        private final String locale;
        private final List<BasicAbstractRole> associations;

        LocationInitialBusinessRole(String oid, String name, String locale, List<BasicAbstractRole> associations) {
            this.oid = oid;
            this.name = name;
            this.locale = locale;
            this.associations = associations;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public @Nullable List<BasicAbstractRole> getAssociations() {
            return associations;
        }

        @Override
        public String getArchetypeOid() {
            return BUSINESS_ROLE_ARCHETYPE_OID;
        }

        public String getLocale() {
            return locale;
        }
    }

    public enum TechnicalBusinessRole implements InitialBusinessRole {

        TECHNICAL_AD_AUDITOR("3e63b80a-23c8-4fec-97a3-22923190887b", "Technical AD Auditor",
                List.of(BasicAbstractRole.LONDON_AD,
                        BasicAbstractRole.EUROPE_AD,
                        BasicAbstractRole.EAST_COAST_AD,
                        BasicAbstractRole.TOKIO_AD,
                        BasicAbstractRole.AD_AUDIT_GROUP));

        private final String oid;
        private final String name;
        private final List<BasicAbstractRole> associations;

        TechnicalBusinessRole(String oid, String name, List<BasicAbstractRole> associations) {
            this.oid = oid;
            this.name = name;
            this.associations = associations;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public @Nullable List<BasicAbstractRole> getAssociations() {
            return associations;
        }

        @Override
        public String getArchetypeOid() {
            return BUSINESS_ROLE_ARCHETYPE_OID;
        }

    }

    public enum PlanktonApplicationBusinessAbstractRole implements InitialAbstractRole {

        WEB_EDITOR("45566a80-c2a8-402f-835e-741ed38fb121", "Web Editor"),
        WEB_ANALYTICS("a80ac1d7-2d9a-49e1-8932-9a7b735baa04", "Web Analytics"),
        AD_POWER_USER("19c06851-f176-4359-a5d2-d3b4d33d2ddb", "AD Power User"),
        SAP_BASIC("119d0d21-02da-4e7f-91c1-e9ed45a376d5", "SAP Basic"),
        DATA_WAREHOUSE_READER("b4c860a6-b4a4-4ba6-b977-96a390c074f2", "Data Warehouse Reader"),
        DB_SQL_ACCESS("b9fd1467-d6c0-43ec-a438-41f9eaa40b74", "DB SQL Access"),
        CRM_READER("f8696319-2805-4053-904f-22f3e3f19026", "CRM Reader"),
        CRM_WRITER("62861882-eeb3-4f32-a050-a62f8a3bf0b8", "CRM Writer"),
        CRM_MANAGER("f09d6584-ce9b-48dc-8b10-657de5fcc3eb", "CRM Manager"),
        ERP_REQUESTOR("a0cae92c-a2e5-49e5-b9d7-114bbc45bb97", "ERP Requestor"),
        ERP_APPROVER("c0b727b8-0184-4ab3-bcee-f0483399a3ae", "ERP Approver"),
        ERP_READER("377031b5-3738-4954-a405-2b6801302e53", "ERP Reader"),
        ERP_AUDITOR("c97511da-d6f1-4a8a-9f94-4b0f97a0431a", "ERP Auditor"),
        CRM_ADMIN("e811f194-4810-4eb8-a1bf-9c65182433c9", "CRM Admin");

        private final String oid;
        private final String name;

        PlanktonApplicationBusinessAbstractRole(String oid, String name) {
            this.oid = oid;
            this.name = name;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public @Nullable List<String> getAssociations() {
            return null;
        }

        @Override
        public String getArchetypeOid() {
            return Archetypes.PLANCTON_ROLE.getOidValue();
        }

        @Override
        public int getAssociationsMultiplier() {
            return 0;
        }

        @Override
        public boolean isArchetypeRoleEnable() {
            return true;
        }
    }

    public enum NoiseApplicationBusinessAbstractRole implements InitialAbstractRole {

        NOISE_MEDIA_MANAGER("c368b9a1-3c58-4d6f-9f86-a23ccf8a4f06", "Noise Media Manager"),
        NOISE_MEDIA_ANALYST("6e42c7ab-4c75-4c17-bf69-63049315680c", "Noise Media Analyst"),
        NOISE_MEDIA_WRITER("f659fe15-9e98-4468-9e7d-80eabe6253c9", "Noise Media Writer"),
        NOISE_MEDIA_READER("f3e4d45c-d311-4f8b-99da-a96313ec7eb0", "Noise Media Reader"),
        NOISE_MEDIA_AUDITOR("dd36aaa5-d671-4a5d-b2c0-3af937f5db0c", "Noise Media Auditor"),
        NOISE_MEDIA_ADMIN("62231b07-af48-4dfb-8250-a40f13994d0c", "Noise Media Admin");

        private final String oid;
        private final String name;

        NoiseApplicationBusinessAbstractRole(String oid, String name) {
            this.oid = oid;
            this.name = name;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public @Nullable List<String> getAssociations() {
            return null;
        }

        @Override
        public String getArchetypeOid() {
            return Archetypes.NOISE_ROLE.getOidValue();
        }

        @Override
        public int getAssociationsMultiplier() {
            return 0;
        }

        @Override
        public boolean isArchetypeRoleEnable() {
            return true;
        }
    }

    public enum BasicAbstractRole implements InitialAbstractRole {

        AD_GROUP_EMPLOYEES("c112783f-5e01-4b1b-bd5b-f5fe1a091413", "AD Group Employees",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue()),
        EXCHANGE_EMAIL("bec83f75-8878-4ba2-80b3-870900a3de66", "Exchange Email",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue()),
        EXCHANGE_CALENDAR("570168f9-7e79-4658-970d-4219c7ad1401", "Exchange Calendar",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue()),
        AD_GROUP_CONTRACTORS("d030a3f4-a1c7-477e-a11e-c8f0cf5e5309", "AD Group Contractors",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue()),
        EXIT_PROCEDURES("d1ab80d7-cb44-4814-a5d4-9555a89c4011", "Exit Procedures",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue()),
        IT_PRIVILEGES("a28b8172-300d-4832-85c3-cef10de1dc49", "IT Privileges",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue()),
        LONDON("0a9fca7f-aaaf-4f23-af45-8438f47172ac", "London",
                Archetypes.AUX_ROLE.getOidValue()),
        PARIS("2e7fda60-23da-45a3-afed-3f5af5145111", "Paris",
                Archetypes.AUX_ROLE.getOidValue()),
        BERLIN("35759c46-6ee9-401c-a6b0-ffce82691ada", "Berlin",
                Archetypes.AUX_ROLE.getOidValue()),
        NEW_YORK("714cf136-8ec9-4712-8946-fb1abdf98dec", "New York",
                Archetypes.AUX_ROLE.getOidValue()),
        TOKYO("d3db4ba0-5066-4d5c-b73c-a9691e056f23", "Tokyo",
                Archetypes.AUX_ROLE.getOidValue()),
        VPN_ACCESS("51b685c9-a158-4313-8b7c-2a462e9f2f7b", "VPN Access",
                Archetypes.AUX_ROLE.getOidValue()),
        VPN_REMOTE("3ec1f90f-b0bd-4924-bbe3-99f9de3c5763", "VPN Remote",
                Archetypes.AUX_ROLE.getOidValue()),
        VPN_EXTERNAL("8dbabe0c-8421-40d6-991e-adfab8765500", "VPN External",
                Archetypes.AUX_ROLE.getOidValue()),
        CALENDAR_POWER_USER("03e34489-89ad-4247-ab91-613a184b21f2", "Calendar Power User",
                Archetypes.JOB_ROLE.getOidValue()),
        SUPERVISOR("f06c9555-2806-4aec-b730-43e34d932152", "Supervisor",
                Archetypes.JOB_ROLE.getOidValue()),
        EXTERNAL("d12fea2a-cec8-47c4-9a08-25615be2bf6a", "External",
                Archetypes.JOB_ROLE.getOidValue()),
        SAP_BASIC("711c6ee7-8fba-47a6-a6ee-0c2c105d9659", "SAP Basic",
                Archetypes.JOB_ROLE.getOidValue()),
        HR_APPROVER("5c6d2eb4-23c9-432b-a419-ce4041a92dd7", "HR Approver",
                Archetypes.JOB_ROLE.getOidValue()),

        TE_COORDINATOR("c1bbe3a7-b724-4e65-9acb-5150df6321c6", "TE Coordinator",
                Archetypes.JOB_ROLE.getOidValue()),
        DE_Specialist("15ee62ca-bc2d-40cc-aee7-5a28c1f4e80d", "DE Specialist",
                Archetypes.JOB_ROLE.getOidValue()),
        PS_ASSISTANT("95100f69-db68-4195-957f-bb267f38e878", "Project Support Assistant",
                Archetypes.JOB_ROLE.getOidValue()),
        HQ_APPROVER("5f6f9c36-d2c5-40c5-99e2-97722bcd12de", "HQ Approver",
                Archetypes.JOB_ROLE.getOidValue()),
        SAP_HQ("814a0cf7-50c4-4d6f-b2a7-6724774694a3", "SAP HQ",
                Archetypes.JOB_ROLE.getOidValue()),
        SAP_HR("a6fdae13-5544-42d7-ba0a-b010ace20446", "SAP HR",
                Archetypes.JOB_ROLE.getOidValue()),
        AD_AUDITOR("b877b970-db3a-419a-ba03-63ae721e6da5", "AD Auditor",
                Archetypes.JOB_ROLE.getOidValue()),
        FIREWALL_ADMIN("8644d950-c8d6-4890-83b5-5d93f9ec99f8", "Firewall Admin",
                Archetypes.JOB_ROLE.getOidValue()),
        NIPPON_DB("f48cd38e-f9ff-44e7-952b-d70590eac319", "NipponDB",
                Archetypes.JOB_ROLE.getOidValue()),
        CRM_READER("ff88930c-7fd6-438d-a337-5c3f0ce63db6", "CRM Reader",
                Archetypes.JOB_ROLE.getOidValue()),
        CRM_WRITER("4610530a-9d11-4ec3-a295-6d3f54fc20fa", "CRM Writer",
                Archetypes.JOB_ROLE.getOidValue()),
        WEB_ANALYTICS("b95bf2f6-d68c-44d4-9c7f-1bdb34a21ca7", "Web Analytics",
                Archetypes.JOB_ROLE.getOidValue()),
        AD_MANAGER_GROUP("b55c1427-6491-47ea-a3df-da40889f4447", "AD Manager Group",
                Archetypes.JOB_ROLE.getOidValue()),
        AD_MANAGER_USER("11f366aa-56d0-4688-8e49-d33c7dfa4468", "AD Manager User",
                Archetypes.JOB_ROLE.getOidValue()),
        LONDON_AD("02bce534-b8de-43a8-abb0-674f15c25bc9", "London AD",
                Archetypes.LOCATION_ROLE.getOidValue()),
        LONDON_BUILDING_ACCESS("76bb271c-482d-4334-a623-7e70bc91bc6c", "London Building Access",
                Archetypes.LOCATION_ROLE.getOidValue()),
        EUROPE_AD("0d4a04c1-4be2-431e-a8bd-18db652b23a7", "Europe AD",
                Archetypes.LOCATION_ROLE.getOidValue()),
        PARIS_BUILDING_ACCESS("11621bed-8aff-4a94-afb1-e95f39ef4781", "Paris Building Access",
                Archetypes.LOCATION_ROLE.getOidValue()),
        BERLIN_BUILDING_ACCESS("b1950c55-9869-439b-9637-37cf656fc64e", "Berlin Building Access",
                Archetypes.LOCATION_ROLE.getOidValue()),
        EAST_COAST_AD("97b3798f-553e-4cdb-8f7d-d36a5235de25", "East Coast AD",
                Archetypes.LOCATION_ROLE.getOidValue()),
        NEW_YORK_MANHATTAN_BUILDING_ACCESS("4acf28c8-8a5f-4656-8d4f-3d64a893f0f9", "New York Manhattan Building Access",
                Archetypes.LOCATION_ROLE.getOidValue()),
        NEW_YORK_BROOKLYN_BUILDING_ACCESS("432c6304-f3d5-4be1-b266-ba83d05a146b", "New York Brooklyn Building Access",
                Archetypes.LOCATION_ROLE.getOidValue()),
        TOKIO_AD("ddba9992-9179-4c1f-bc0c-a87490b40ea7", "Tokio AD",
                Archetypes.LOCATION_ROLE.getOidValue()),
        TOKIO_BUILDING_ACCESS("8bc3a098-6731-495c-a88a-61c2e5b4d8f2", "Tokio Building Access",
                Archetypes.LOCATION_ROLE.getOidValue()),
        AD_AUDIT_GROUP("c21b829d-fa76-4050-bb45-f436d752e673", "AD audit group",
                Archetypes.TECHNICAL_ROLE.getOidValue());

        private final String oid;
        private final String name;
        private final int associationsMultiplier;
        final String archetypeOid;
        private final List<String> associations;

        BasicAbstractRole(String oid, String name, String archetypeOid) {
            this.oid = oid;
            this.name = name;
            this.archetypeOid = archetypeOid;

            if (options.getRoleMultiplier() > 1) {
                this.associationsMultiplier = options.getRoleMultiplier();
            } else {
                this.associationsMultiplier = 0;
            }
            int associationsMultiplier = this.associationsMultiplier;

            if (associationsMultiplier > 0) {
                this.associations = new ArrayList<>();
                for (int i = 1; i <= associationsMultiplier; i++) {
                    this.associations.add(UUID.randomUUID().toString());
                }
            } else {
                this.associations = null;
            }
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public @Nullable List<String> getAssociations() {
            return associations;
        }

        @Override
        public String getArchetypeOid() {
            return archetypeOid;
        }

        @Override
        public int getAssociationsMultiplier() {
            return associationsMultiplier;
        }

        @Override
        public boolean isArchetypeRoleEnable() {
            return options.isArchetypeRoleEnable();
        }
    }

    public enum Organization implements InitialOrg {

        REGULAR("2f59f927-d1c5-4a61-93c6-6662041257af", "Regular"),
        SEMI_REGULAR("9685e38d-df23-4d8b-9c2c-24d634c45adb", "Semi-regular"),
        IRREGULAR("ab51f75b-8afd-4599-ba8f-b246b477d03d", "Irregular"),
        MANAGERS("95feba24-bd40-45d4-b9ea-9b074b454c1b", "Managers"),
        SALES("7ad7d97e-0326-401f-8cfd-36e6c8dddd58", "Sales"),
        SECURITY_OFFICERS("020e866e-a5ac-40bb-8644-24525203bb03", "Security officers"),
        CONTRACTORS("2e734ccb-c924-4388-b0a8-b02b461389fd", "Contractors");
        private final String oid;
        private final String name;

        Organization(String oid, String name) {
            this.oid = oid;
            this.name = name;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getName() {
            return name;
        }

    }

    public enum Archetypes implements InitialArchetype {

        PLANCTON_ROLE("b9bcc8ff-7b43-41a2-8844-4834252e21a3", "Plancton Role Archetype",
                "red", "fe fe-role"),
        TECHNICAL_ROLE("9a3c9cf0-63d3-4165-ab9d-e49865b1da1a", "Technical Role Archetype",
                "brown", "fe fe-role"),
        LOCATION_ROLE("826f255d-d2c3-4359-9bc2-51047cceeebf", "Location Role Archetype",
                "pink", "fe fe-role"),
        JOB_ROLE("2fd84b31-d2f2-407a-b970-907dfd1cb239", "Job Role Archetype",
                "yellow", "fe fe-role"),
        AUX_ROLE("40549d50-4abd-4b89-a46e-2678c957afb3", "Aux Role Archetype",
                "purple", "fe fe-role"),
        BIRTHRIGHT_ROLE("d212dcd9-b062-49fd-adbd-7815868f132c", "Birthright Role Archetype",
                "orange", "fe fe-role"),
        NOISE_ROLE("5b8a247c-443f-4a9a-a125-963b36383061", "Noise Role Archetype",
                "blue", "fe fe-role"),
        REGULAR_USER("86638d1c-66b6-40a9-817e-cf88ca7aaced", "Regular User Archetype",
                "blue", "fa fa-user"),
        SEMI_REGULAR_USER("e3b84663-1f37-46fa-ab06-70cbac038885", "Semi-regular User Archetype",
                "brown", "fa fa-user"),
        IRREGULAR_USER("03a452a2-79cf-4fb5-aefd-14d23a05473f", "Irregular User Archetype",
                "purple", "fa fa-user"),
        MANAGERS_USER("fef8090c-06e3-4ca3-b706-1401f2f679e9", "Managers User Archetype",
                "green", "fa fa-user"),
        SALES_USER("2068df70-7f03-41c8-91bd-8e88de626d05", "Sales User Archetype",
                "yellow", "fa fa-user"),
        SECURITY_OFFICERS_USER("cffd833f-78a5-4cc0-92dc-06a9e2787e64", "Security officers User Archetype",
                "orange", "fa fa-user"),
        CONTRACTORS_USER("324f5625-de3a-4f7f-8e3b-44c8d5ce051d", "Contractors User Archetype",
                "red", "fa fa-user");

        private final String oid;
        private final String name;
        private final String color;
        private final String iconCss;

        Archetypes(String oid, String name, String color, String iconCss) {
            this.oid = oid;
            this.name = name;
            this.color = color;
            this.iconCss = iconCss;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String getColor() {
            return color;
        }

        @Override
        public String getIconCss() {
            return iconCss;
        }

        @Override
        public String getName() {
            return name;
        }

    }

}
