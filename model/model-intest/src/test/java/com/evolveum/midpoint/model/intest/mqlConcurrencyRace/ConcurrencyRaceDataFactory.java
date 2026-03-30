/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.mqlConcurrencyRace;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static com.evolveum.midpoint.model.intest.mqlConcurrencyRace.ConcurrencyRaceSelection.activeCohorts;

abstract class ConcurrencyRaceDataFactory extends ConcurrencyRaceNaming {

    protected int determineUsersToSeed() {
        int shortUsers = readInt(PROP_SHORT_USERS, DEFAULT_SHORT_USERS);
        if (Boolean.getBoolean(PROP_MANUAL_ENABLED)) {
            return Math.max(shortUsers, readInt(PROP_MANUAL_USERS, DEFAULT_MANUAL_USERS));
        }
        return shortUsers;
    }

    protected List<ObjectType> createTargets(int usersToSeed) {
        List<ObjectType> objects = new ArrayList<>();
        for (CohortKey cohortKey : activeCohorts()) {
            for (int i = 1; i <= usersToSeed; i++) {
                addTargetsForCohort(objects, cohortKey, i);
            }
        }
        for (int i = 1; i <= DEFAULT_NOISE_OBJECTS; i++) {
            objects.add(createNoiseService(i));
            objects.add(createNoiseOrg(i));
            objects.add(createNoiseRole(i));
        }
        return objects;
    }

    protected List<ObjectType> createUsers(int usersToSeed) {
        List<ObjectType> objects = new ArrayList<>(usersToSeed * activeCohorts().size());
        for (CohortKey cohortKey : activeCohorts()) {
            List<WorkItem> cohortUsers = new ArrayList<>(usersToSeed);
            for (int i = 1; i <= usersToSeed; i++) {
                WorkItem workItem = createUser(i, cohortKey);
                cohortUsers.add(workItem);
                objects.add(workItem.user());
            }
            usersByCohort.put(cohortKey, cohortUsers);
        }
        return objects;
    }

    protected WorkItem createUser(int index, CohortKey cohortKey) {
        String userPrefix = userPrefix(cohortKey);
        String userOid = oid(userPrefix + "-user-" + index);
        UserType user = new UserType()
                .oid(userOid)
                .name(userPrefix + "-USER-" + format(index))
                .subtype(subtypeByCohort.get(cohortKey))
                .personalNumber(personalNumber(index, cohortKey));
        setExtensionProperty(user, RACE_WORKPLACE_ID, workplaceId(index, cohortKey));
        setExtensionProperty(user, RACE_SAP_CODE_1, sapCode1(index, cohortKey));
        return new WorkItem(index, cohortKey, userOid, user);
    }

    protected void addTargetsForCohort(List<ObjectType> objects, CohortKey cohortKey, int index) {
        switch (cohortKey.scenarioSet()) {
            case ALL -> {
                objects.add(createService(index, cohortKey));
                objects.add(createOrgByWorkplace(index, cohortKey));
                objects.add(createOrgByManager(index, cohortKey));
                objects.add(createExpectedRole(index, cohortKey));
                objects.add(createRoleDecoyWrongSapCode(index, cohortKey));
                objects.add(createRoleDecoyWrongVariant(index, cohortKey));
            }
            case SERVICE_ONLY -> objects.add(createService(index, cohortKey));
            case ORG_BY_WORKPLACE_ONLY -> objects.add(createOrgByWorkplace(index, cohortKey));
            case ORG_BY_MANAGER_ONLY -> objects.add(createOrgByManager(index, cohortKey));
            case ROLE_ONLY -> {
                objects.add(createExpectedRole(index, cohortKey));
                objects.add(createRoleDecoyWrongSapCode(index, cohortKey));
                objects.add(createRoleDecoyWrongVariant(index, cohortKey));
            }
        }
    }

    protected ServiceType createService(int index, CohortKey cohortKey) {
        String prefix = targetSeedPrefix(cohortKey);
        ServiceType service = new ServiceType()
                .oid(oid(prefix + "-service-" + index))
                .name(prefix + "-SERVICE-" + format(index));
        setExtensionProperty(service, RACE_INITIAL_OWNER_NAME, userPrefix(cohortKey) + "-USER-" + format(index));
        return service;
    }

    protected OrgType createOrgByWorkplace(int index, CohortKey cohortKey) {
        String prefix = targetSeedPrefix(cohortKey);
        return new OrgType()
                .oid(oid(prefix + "-org-workplace-" + index))
                .name(prefix + "-ORG-PM-" + format(index))
                .identifier(workplaceId(index, cohortKey));
    }

    protected OrgType createOrgByManager(int index, CohortKey cohortKey) {
        String prefix = targetSeedPrefix(cohortKey);
        OrgType org = new OrgType()
                .oid(oid(prefix + "-org-manager-" + index))
                .name(prefix + "-ORG-PN-" + format(index));
        setExtensionProperty(org, RACE_MANAGER_PERSONAL_NUMBER, personalNumber(index, cohortKey));
        return org;
    }

    protected RoleType createExpectedRole(int index, CohortKey cohortKey) {
        String prefix = targetSeedPrefix(cohortKey);
        RoleType role = new RoleType()
                .oid(oid(prefix + "-role-expected-" + index))
                .name(prefix + "-ROLE-PRIMARY-" + format(index));
        setExtensionProperty(role, RACE_EX_ROLE_SAP_CODE_1, sapCode1(index, cohortKey));
        setExtensionProperty(role, RACE_EX_ROLE_SAP_CODE, "yyy");
        setExtensionProperty(role, RACE_EX_ROLE_APP_INSTANCE, "FSD");
        setExtensionProperty(role, RACE_EX_ROLE_VARIANT, "PRIMARY");
        return role;
    }

    protected RoleType createRoleDecoyWrongSapCode(int index, CohortKey cohortKey) {
        String prefix = targetSeedPrefix(cohortKey);
        RoleType role = new RoleType()
                .oid(oid(prefix + "-role-decoy-sap-" + index))
                .name(prefix + "-ROLE-DECOY-SAP-" + format(index));
        setExtensionProperty(role, RACE_EX_ROLE_SAP_CODE_1, sapCode1(index, cohortKey));
        setExtensionProperty(role, RACE_EX_ROLE_SAP_CODE, "zzz");
        setExtensionProperty(role, RACE_EX_ROLE_APP_INSTANCE, "FSD");
        setExtensionProperty(role, RACE_EX_ROLE_VARIANT, "PRIMARY");
        return role;
    }

    protected RoleType createRoleDecoyWrongVariant(int index, CohortKey cohortKey) {
        String prefix = targetSeedPrefix(cohortKey);
        RoleType role = new RoleType()
                .oid(oid(prefix + "-role-decoy-variant-" + index))
                .name(prefix + "-ROLE-DECOY-VARIANT-" + format(index));
        setExtensionProperty(role, RACE_EX_ROLE_SAP_CODE_1, sapCode1(index, cohortKey));
        setExtensionProperty(role, RACE_EX_ROLE_SAP_CODE, "yyy");
        setExtensionProperty(role, RACE_EX_ROLE_APP_INSTANCE, "FSD");
        setExtensionProperty(role, RACE_EX_ROLE_VARIANT, "SECONDARY");
        return role;
    }

    protected ServiceType createNoiseService(int index) {
        ServiceType service = new ServiceType()
                .oid(oid("noise-service-" + index))
                .name("NOISE-SERVICE-" + format(index));
        setExtensionProperty(service, RACE_INITIAL_OWNER_NAME, "NOISE-" + format(index));
        return service;
    }

    protected OrgType createNoiseOrg(int index) {
        OrgType org = new OrgType()
                .oid(oid("noise-org-" + index))
                .name("NOISE-ORG-" + format(index))
                .identifier("ZZ-" + format(index));
        setExtensionProperty(org, RACE_MANAGER_PERSONAL_NUMBER, "NZ-" + format(index));
        return org;
    }

    protected RoleType createNoiseRole(int index) {
        RoleType role = new RoleType()
                .oid(oid("noise-role-" + index))
                .name("NOISE-ROLE-" + format(index));
        setExtensionProperty(role, RACE_EX_ROLE_SAP_CODE_1, "NOISE-" + format(index));
        setExtensionProperty(role, RACE_EX_ROLE_SAP_CODE, "noise");
        setExtensionProperty(role, RACE_EX_ROLE_APP_INSTANCE, "NOISE");
        setExtensionProperty(role, RACE_EX_ROLE_VARIANT, "NOISE");
        return role;
    }

    protected static <O extends ObjectType> void setExtensionProperty(O object, QName itemName, Object value) {
        try {
            object.asPrismObject().getOrCreateExtension().setPropertyRealValue(itemName, value);
        } catch (SchemaException e) {
            throw new IllegalStateException(
                    "Failed to initialize extension property " + itemName + " on " + object.getClass().getSimpleName(), e);
        }
    }
}
