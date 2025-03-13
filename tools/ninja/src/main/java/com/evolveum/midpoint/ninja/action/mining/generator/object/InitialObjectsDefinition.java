/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.object;

import java.util.*;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.github.javafaker.Faker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacUserType.generateUniqueRoleName;

/**
 * The InitialObjectsDefinition class facilitates the generation of initial objects, including roles, organizations,
 * and archetypes, which are essential for data generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public class InitialObjectsDefinition {

    protected static final String BUSINESS_ROLE_ARCHETYPE_OID = "00000000-0000-0000-0000-000000000321";
    static GeneratorOptions options;
    private static final Set<String> USED_ROLE_NAMES_SET = new HashSet<>();

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

    public SpecialLowMembershipRole getSpecialLowMembershipRolesObjects() {
        return SpecialLowMembershipRole.ROLE;
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
        addOrgObjects(ProfessionOrg.values(), orgObjects);
        addOrgObjects(LocationOrg.values(), orgObjects);
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
                List.of(BasicAbstractRole.PS_ASSISTANT, BasicAbstractRole.DE_SPECIALIST, BasicAbstractRole.TE_COORDINATOR)),
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

        @Override
        public RbacSecurityLevel getSecurityLevel() {
            return RbacSecurityLevel.TIER_3;
        }
    }

    public enum NoiseApplicationBusinessAbstractRole implements InitialAbstractRole {

        NOISE_MEDIA_MANAGER("c368b9a1-3c58-4d6f-9f86-a23ccf8a4f06", "Media Manager"),
        NOISE_MEDIA_ANALYST("6e42c7ab-4c75-4c17-bf69-63049315680c", "Media Analyst"),
        NOISE_MEDIA_WRITER("f659fe15-9e98-4468-9e7d-80eabe6253c9", "Media Writer"),
        NOISE_MEDIA_READER("f3e4d45c-d311-4f8b-99da-a96313ec7eb0", "Media Reader"),
        NOISE_MEDIA_AUDITOR("dd36aaa5-d671-4a5d-b2c0-3af937f5db0c", "Media Auditor"),
        NOISE_MEDIA_ADMIN("62231b07-af48-4dfb-8250-a40f13994d0c", "Media Admin");

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

        @Override
        public RbacSecurityLevel getSecurityLevel() {
            return RbacSecurityLevel.TIER_4;
        }
    }

    public enum SpecialLowMembershipRole implements InitialAbstractRole {

        ROLE(Archetypes.SPECIAL_ROLE.getOidValue());

        final String archetypeOid;
        final Faker faker = new Faker();

        SpecialLowMembershipRole(String archetypeOid) {
            this.archetypeOid = archetypeOid;
        }

        @Override
        public String getOidValue() {
            return UUID.randomUUID().toString();
        }

        @Override
        public String getName() {
            return generateUniqueRoleName(faker, USED_ROLE_NAMES_SET);
        }

        @Override
        public @Nullable List<String> getAssociations() {
            return null;
        }

        @Override
        public String getArchetypeOid() {
            return archetypeOid;
        }

        @Override
        public int getAssociationsMultiplier() {
            return 0;
        }

        @Override
        public boolean isArchetypeRoleEnable() {
            return options.isArchetypeRoleEnable();
        }

        @Override
        public RbacSecurityLevel getSecurityLevel() {
            return RbacSecurityLevel.TIER_5;
        }
    }

    public enum BasicAbstractRole implements InitialAbstractRole {

        AD_GROUP_EMPLOYEES("c112783f-5e01-4b1b-bd5b-f5fe1a091413", "AD Group Employees",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        EXCHANGE_EMAIL("bec83f75-8878-4ba2-80b3-870900a3de66", "Exchange Email",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        EXCHANGE_CALENDAR("570168f9-7e79-4658-970d-4219c7ad1401", "Exchange Calendar",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        AD_GROUP_CONTRACTORS("d030a3f4-a1c7-477e-a11e-c8f0cf5e5309", "AD Group Contractors",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        EXIT_PROCEDURES("d1ab80d7-cb44-4814-a5d4-9555a89c4011", "Exit Procedures",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        IT_PRIVILEGES("a28b8172-300d-4832-85c3-cef10de1dc49", "IT Privileges",
                Archetypes.BIRTHRIGHT_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        LONDON("0a9fca7f-aaaf-4f23-af45-8438f47172ac", "London",
                Archetypes.AUX_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        PARIS("2e7fda60-23da-45a3-afed-3f5af5145111", "Paris",
                Archetypes.AUX_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        BERLIN("35759c46-6ee9-401c-a6b0-ffce82691ada", "Berlin",
                Archetypes.AUX_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        NEW_YORK("714cf136-8ec9-4712-8946-fb1abdf98dec", "New York",
                Archetypes.AUX_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        TOKYO("d3db4ba0-5066-4d5c-b73c-a9691e056f23", "Tokyo",
                Archetypes.AUX_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        VPN_ACCESS("51b685c9-a158-4313-8b7c-2a462e9f2f7b", "VPN Access",
                Archetypes.AUX_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        VPN_REMOTE("3ec1f90f-b0bd-4924-bbe3-99f9de3c5763", "VPN Remote",
                Archetypes.AUX_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        VPN_EXTERNAL("8dbabe0c-8421-40d6-991e-adfab8765500", "VPN External",
                Archetypes.AUX_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        CALENDAR_POWER_USER("03e34489-89ad-4247-ab91-613a184b21f2", "Calendar Power User",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        SUPERVISOR("f06c9555-2806-4aec-b730-43e34d932152", "Supervisor",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        EXTERNAL("d12fea2a-cec8-47c4-9a08-25615be2bf6a", "External",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        SAP_BASIC("711c6ee7-8fba-47a6-a6ee-0c2c105d9659", "SAP Basic",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        HR_APPROVER("5c6d2eb4-23c9-432b-a419-ce4041a92dd7", "HR Approver",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),

        TE_COORDINATOR("c1bbe3a7-b724-4e65-9acb-5150df6321c6", "TE Coordinator",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        DE_SPECIALIST("15ee62ca-bc2d-40cc-aee7-5a28c1f4e80d", "DE Specialist",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        PS_ASSISTANT("95100f69-db68-4195-957f-bb267f38e878", "Project Support Assistant",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        HQ_APPROVER("5f6f9c36-d2c5-40c5-99e2-97722bcd12de", "HQ Approver",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        SAP_HQ("814a0cf7-50c4-4d6f-b2a7-6724774694a3", "SAP HQ",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        SAP_HR("a6fdae13-5544-42d7-ba0a-b010ace20446", "SAP HR",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        AD_AUDITOR("b877b970-db3a-419a-ba03-63ae721e6da5", "AD Auditor",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        FIREWALL_ADMIN("8644d950-c8d6-4890-83b5-5d93f9ec99f8", "Firewall Admin",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        NIPPON_DB("f48cd38e-f9ff-44e7-952b-d70590eac319", "NipponDB",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        CRM_READER("ff88930c-7fd6-438d-a337-5c3f0ce63db6", "CRM Reader",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        CRM_WRITER("4610530a-9d11-4ec3-a295-6d3f54fc20fa", "CRM Writer",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        WEB_ANALYTICS("b95bf2f6-d68c-44d4-9c7f-1bdb34a21ca7", "Web Analytics",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        AD_MANAGER_GROUP("b55c1427-6491-47ea-a3df-da40889f4447", "AD Manager Group",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        AD_MANAGER_USER("11f366aa-56d0-4688-8e49-d33c7dfa4468", "AD Manager User",
                Archetypes.JOB_ROLE.getOidValue(), RbacSecurityLevel.TIER_1),
        LONDON_AD("02bce534-b8de-43a8-abb0-674f15c25bc9", "London AD",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        LONDON_BUILDING_ACCESS("76bb271c-482d-4334-a623-7e70bc91bc6c", "London Building Access",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        EUROPE_AD("0d4a04c1-4be2-431e-a8bd-18db652b23a7", "Europe AD",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        PARIS_BUILDING_ACCESS("11621bed-8aff-4a94-afb1-e95f39ef4781", "Paris Building Access",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        BERLIN_BUILDING_ACCESS("b1950c55-9869-439b-9637-37cf656fc64e", "Berlin Building Access",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        EAST_COAST_AD("97b3798f-553e-4cdb-8f7d-d36a5235de25", "East Coast AD",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        NEW_YORK_MANHATTAN_BUILDING_ACCESS("4acf28c8-8a5f-4656-8d4f-3d64a893f0f9", "New York Manhattan Building Access",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        NEW_YORK_BROOKLYN_BUILDING_ACCESS("432c6304-f3d5-4be1-b266-ba83d05a146b", "New York Brooklyn Building Access",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        TOKIO_AD("ddba9992-9179-4c1f-bc0c-a87490b40ea7", "Tokio AD",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        TOKIO_BUILDING_ACCESS("8bc3a098-6731-495c-a88a-61c2e5b4d8f2", "Tokio Building Access",
                Archetypes.LOCATION_ROLE.getOidValue(), RbacSecurityLevel.TIER_2),
        AD_AUDIT_GROUP("c21b829d-fa76-4050-bb45-f436d752e673", "AD audit group",
                Archetypes.TECHNICAL_ROLE.getOidValue(), RbacSecurityLevel.TIER_1);

        private final String oid;
        private final String name;
        private final int associationsMultiplier;
        final String archetypeOid;
        private final List<String> associations;
        private RbacSecurityLevel rbacSecurityLevel;

        BasicAbstractRole(String oid, String name, String archetypeOid, RbacSecurityLevel rbacSecurityLevel) {
            this.oid = oid;
            this.name = name;
            this.archetypeOid = archetypeOid;
            this.rbacSecurityLevel = rbacSecurityLevel;

            if (options.getRoleMultiplier() > 1 && options.getMaxRandomMultiplier() > options.getMinRandomMultiplier()) {
                if (!options.isRandomRoleMultiplier()) {
                    this.associationsMultiplier = options.getRoleMultiplier();
                } else {
                    this.associationsMultiplier = new Random().nextInt(
                            options.getMaxRandomMultiplier() - options.getMinRandomMultiplier())
                            + options.getMinRandomMultiplier();
                }
            } else if (options.isRandomRoleMultiplier()) {
                //TODO random role multiplier range should be initialized in the options setter (tmp <0;5>)
                this.associationsMultiplier = new Random().nextInt(6);
            } else {
                this.associationsMultiplier = 0;
            }

            if (this.associationsMultiplier > 0) {
                this.associations = new ArrayList<>();
                for (int i = 1; i <= this.associationsMultiplier; i++) {
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

        @Override
        public RbacSecurityLevel getSecurityLevel() {
            return rbacSecurityLevel;
        }
    }

    public enum ProfessionOrg implements InitialOrg {

        PROFESSION("758bb200-a922-4faa-9b3f-921abdf64e9d", "Profession", null),
        REGULAR("2f59f927-d1c5-4a61-93c6-6662041257af", "Regular", PROFESSION.oid),
        SEMI_REGULAR("9685e38d-df23-4d8b-9c2c-24d634c45adb", "Semi-regular", PROFESSION.oid),
        IRREGULAR("ab51f75b-8afd-4599-ba8f-b246b477d03d", "Irregular", PROFESSION.oid),
        MANAGERS("95feba24-bd40-45d4-b9ea-9b074b454c1b", "Managers", PROFESSION.oid),
        SALES("7ad7d97e-0326-401f-8cfd-36e6c8dddd58", "Sales", PROFESSION.oid),
        SECURITY_OFFICERS("020e866e-a5ac-40bb-8644-24525203bb03", "Security officers", PROFESSION.oid),
        CONTRACTORS("2e734ccb-c924-4388-b0a8-b02b461389fd", "Contractors", PROFESSION.oid);

        private final String oid;
        private final String name;
        private final String parentOid;

        ProfessionOrg(String oid, String name, String parentOid) {
            this.oid = oid;
            this.name = name;
            this.parentOid = parentOid;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String parentOid() {
            return parentOid;
        }

        @Override
        public String getName() {
            return name;
        }

    }

    public enum LocationOrg implements InitialOrg {

        LOCATION_ROOT("1f048ec2-9dcb-487d-b760-fe9d0a416a1e", "Location", null, null),
        LONDON("9919048c-5d1e-4039-901c-9edfd03ddaf0", "London group", LOCATION_ROOT.oid, LocationInitialBusinessRole.LOCATION_LONDON),
        PARIS("3cf173a9-9f9e-48f5-a385-365500f2c288", "Paris group", LOCATION_ROOT.oid, LocationInitialBusinessRole.LOCATION_PARIS),
        BERLIN("d676a9f5-6384-4719-83e1-b507f3771ef9", "Berlin group", LOCATION_ROOT.oid, LocationInitialBusinessRole.LOCATION_BERLIN),
        NEW_YORK("04772af9-4e0c-4ba1-8362-4b41ecfdb071", "New York group", LOCATION_ROOT.oid, LocationInitialBusinessRole.LOCATION_NEW_YORK),
        TOKYO("e71b843b-06e6-49c6-9d72-e0d75ae53496", "Tokyo group", LOCATION_ROOT.oid, LocationInitialBusinessRole.LOCATION_TOKYO);
        // NOT_DEFINED("13c0b991-37bc-404b-9011-c00caea79825", "Not defined", LOCATION.parentOid()); //do we want to have this?

        private final String oid;
        private final String name;
        private final String parentOid;
        private final LocationInitialBusinessRole associatedWith;

        LocationOrg(String oid, String name, String parentOid, LocationInitialBusinessRole associatedWith) {
            this.oid = oid;
            this.name = name;
            this.parentOid = parentOid;
            this.associatedWith = associatedWith;
        }

        @Override
        public String getOidValue() {
            return oid;
        }

        @Override
        public String parentOid() {
            return parentOid;
        }

        @Override
        public String getName() {
            return name;
        }

        public LocationInitialBusinessRole getAssociatedLocationRole() {
            return associatedWith;
        }
    }

    public enum Archetypes implements InitialArchetype {

        PLANCTON_ROLE("b9bcc8ff-7b43-41a2-8844-4834252e21a3", "Plancton Role Archetype",
                "Roles available on demand, not subject to security requirements, "
                        + "and do not require approval for access.",
                "red", "fe fe-role", RoleType.COMPLEX_TYPE),
        TECHNICAL_ROLE("9a3c9cf0-63d3-4165-ab9d-e49865b1da1a", "Technical Role Archetype",
                "Simulates a role hierarchy for auditing purposes. Not directly assigned to "
                        + "any user but represents access"
                        + " to various Active Directory regions and audit groups.",
                "brown", "fe fe-role", RoleType.COMPLEX_TYPE),
        LOCATION_ROLE("826f255d-d2c3-4359-9bc2-51047cceeebf", "Location Role Archetype",
                "Represent location-specific access for various regions, providing users with the"
                        + " necessary permissions for buildings, Active Directory access, and region-specific systems.",
                "pink", "fe fe-role", RoleType.COMPLEX_TYPE),
        JOB_ROLE("2fd84b31-d2f2-407a-b970-907dfd1cb239", "Job Role Archetype",
                "Represent common job functions within the organization, with some roles having "
                        + "specialized permissions tied to specific systems or responsibilities. These roles are "
                        + "typically connected to specific user attributes.",
                "yellow", "fe fe-role", RoleType.COMPLEX_TYPE),
        AUX_ROLE("40549d50-4abd-4b89-a46e-2678c957afb3",
                "Aux Role Archetype",
                "Designed for users who require specialized or supplementary access based on specific needs",
                "purple", "fe fe-role", RoleType.COMPLEX_TYPE),
        BIRTHRIGHT_ROLE("d212dcd9-b062-49fd-adbd-7815868f132c", "Birthright Role Archetype",
                "Foundational access granted based on user status, such as employee or contractor.",
                "orange", "fe fe-role", RoleType.COMPLEX_TYPE),
        NOISE_ROLE("5b8a247c-443f-4a9a-a125-963b36383061", "Noise Role Archetype",
                "Represent outdated or legacy roles that are no longer needed but remain in the system.",
                "blue", "fe fe-role", RoleType.COMPLEX_TYPE),
        REGULAR_USER("86638d1c-66b6-40a9-817e-cf88ca7aaced", "Regular User Archetype",
                "Represents standard users within the system who have typical access to basic resources and "
                        + "applications required for daily operations.",
                "blue", "fa fa-user", UserType.COMPLEX_TYPE),
        SEMI_REGULAR_USER("e3b84663-1f37-46fa-ab06-70cbac038885", "Semi-regular User Archetype",
                "Represents users with access to additional resources or permissions beyond standard access, "
                        + "typically for specialized tasks or temporary roles.",
                "brown", "fa fa-user", UserType.COMPLEX_TYPE),
        IRREGULAR_USER("03a452a2-79cf-4fb5-aefd-14d23a05473f", "Irregular User Archetype",
                "Represents users with non-standard access, typically granted for specific, occasional, or "
                        + "exceptional tasks outside regular permissions.",
                "purple", "fa fa-user", UserType.COMPLEX_TYPE),
        MANAGERS_USER("fef8090c-06e3-4ca3-b706-1401f2f679e9", "Managers User Archetype",
                "Represents users with managerial access, combining standard permissions with additional "
                        + "responsibilities.",
                "green", "fa fa-user", UserType.COMPLEX_TYPE),
        SALES_USER("2068df70-7f03-41c8-91bd-8e88de626d05", "Sales User Archetype",
                "Represents users with access to sales-related resources and tools.",
                "yellow", "fa fa-user", UserType.COMPLEX_TYPE),
        SECURITY_OFFICERS_USER("cffd833f-78a5-4cc0-92dc-06a9e2787e64", "Security officers User Archetype",
                "Represents users with access to security-related systems and tools.",
                "orange", "fa fa-user", UserType.COMPLEX_TYPE),
        CONTRACTORS_USER("324f5625-de3a-4f7f-8e3b-44c8d5ce051d", "Contractors User Archetype",
                "Represents users with limited, temporary access to systems.",
                "red", "fa fa-user", UserType.COMPLEX_TYPE),
        SPECIAL_ROLE("31585cd1-b012-483b-95fd-7ffe082eda32", "Special Role Archetype",
                "Represents a special role archetype that is not tied to any specific category.",
                "black", "fe fe-role", RoleType.COMPLEX_TYPE);

        private final String oid;
        private final String name;
        private final String description;
        private final String color;
        private final String iconCss;
        private final QName holderType;

        Archetypes(String oid, String name, String description, String color, String iconCss, QName holderType) {
            this.oid = oid;
            this.name = name;
            this.description = description;
            this.color = color;
            this.iconCss = iconCss;
            this.holderType = holderType;
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

        @Override
        public QName getHolderType() {
            return holderType;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

}
