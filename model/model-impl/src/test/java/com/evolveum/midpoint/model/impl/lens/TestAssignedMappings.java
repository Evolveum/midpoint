/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.io.File;
import jakarta.xml.bind.JAXBElement;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class TestAssignedMappings extends AbstractLensTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "lens/focusMappings");

    private static final TestResource<RoleType> ROLE_SIMPLE = new TestResource<>(TEST_DIR, "role-simple.xml", "de0c9c11-eb2e-4b6a-9200-a4306a5c8d6c");
    private static final TestResource<UserType> USER_JIM = new TestResource<>(TEST_DIR, "user-jim.xml", "6ce717d6-414f-4d91-948a-923f02191399");

    private static final TestResource<RoleType> METAMETAROLE_MMR111 = new TestResource<>(TEST_DIR, "metametarole-mmr1.1.1.xml", "b3d1cb95-526b-4162-8d65-acd8f2994644");
    private static final TestResource<RoleType> METAMETAROLE_MMR112 = new TestResource<>(TEST_DIR, "metametarole-mmr1.1.2.xml", "49cf021c-4127-41e1-9187-abcbeb8c0903");
    private static final TestResource<RoleType> METAMETAROLE_MMR113 = new TestResource<>(TEST_DIR, "metametarole-mmr1.1.3.xml", "99ed5447-4633-40ca-a81d-faa6f364ee69");
    private static final TestResource<RoleType> METAMETAROLE_MMR121 = new TestResource<>(TEST_DIR, "metametarole-mmr1.2.1.xml", "786f30f5-083f-4e45-a06d-2213a8cbd789");
    private static final TestResource<RoleType> METAMETAROLE_MMR122 = new TestResource<>(TEST_DIR, "metametarole-mmr1.2.2.xml", "9fcb61b5-d970-4637-8cbe-6ffbaf075ab4");
    private static final TestResource<RoleType> METAMETAROLE_MMR123 = new TestResource<>(TEST_DIR, "metametarole-mmr1.2.3.xml", "717c386e-4fd0-48c7-982b-ccdbb8d4415e");
    private static final TestResource<RoleType> METAMETAROLE_MMR131 = new TestResource<>(TEST_DIR, "metametarole-mmr1.3.1.xml", "7f9239aa-e2ea-4532-a597-6cb572dadd6b");
    private static final TestResource<RoleType> METAMETAROLE_MMR132 = new TestResource<>(TEST_DIR, "metametarole-mmr1.3.2.xml", "c5f74a67-da40-4498-a2f9-1d41ff4163b6");
    private static final TestResource<RoleType> METAMETAROLE_MMR211 = new TestResource<>(TEST_DIR, "metametarole-mmr2.1.1.xml", "01f0ae41-6348-4707-ab17-a0638302d759");
    private static final TestResource<RoleType> METAMETAROLE_MMR221 = new TestResource<>(TEST_DIR, "metametarole-mmr2.2.1.xml", "0f4ff4db-53d9-4028-9995-6fa6f1d2fbfe");
    private static final TestResource<RoleType> METAMETAROLE_MMR231 = new TestResource<>(TEST_DIR, "metametarole-mmr2.3.1.xml", "eda583e0-7f6f-4770-ba59-a7a9fbbb2fa6");
    private static final TestResource<RoleType> METAMETAROLE_MMR241 = new TestResource<>(TEST_DIR, "metametarole-mmr2.4.1.xml", "06ce4987-6e3a-4e61-9ec3-f436b15ef7b1");
    private static final TestResource<RoleType> METAMETAROLE_MMR311 = new TestResource<>(TEST_DIR, "metametarole-mmr3.1.1.xml", "46fe813b-2658-4f49-b67e-1516b7154851");

    private static final TestResource<RoleType> METAROLE_MR11 = new TestResource<>(TEST_DIR, "metarole-mr1.1.xml", "2f4136cd-663f-4d52-bc29-77dd6cc64bda");
    private static final TestResource<RoleType> METAROLE_MR12 = new TestResource<>(TEST_DIR, "metarole-mr1.2.xml", "4106cf9e-443e-4218-9aef-88d7d9aac3e4");
    private static final TestResource<RoleType> METAROLE_MR13 = new TestResource<>(TEST_DIR, "metarole-mr1.3.xml", "a92858ac-52cc-4ea7-aa42-0fb3033ac9f2");
    private static final TestResource<RoleType> METAROLE_MR21 = new TestResource<>(TEST_DIR, "metarole-mr2.1.xml", "e0cf0c0e-0db4-4d41-bba4-5e6ee4928837");
    private static final TestResource<RoleType> METAROLE_MR22 = new TestResource<>(TEST_DIR, "metarole-mr2.2.xml", "cfda5d1a-c940-4648-a362-d49195ee2d17");
    private static final TestResource<RoleType> METAROLE_MR23 = new TestResource<>(TEST_DIR, "metarole-mr2.3.xml", "3108290d-08c5-4025-a574-32998da7e28c");
    private static final TestResource<RoleType> METAROLE_MR24 = new TestResource<>(TEST_DIR, "metarole-mr2.4.xml", "559efdde-90e0-465b-8989-a45472f14c9f");
    private static final TestResource<RoleType> METAROLE_MR31 = new TestResource<>(TEST_DIR, "metarole-mr3.1.xml", "1b732c09-ecf0-41d6-828a-1cbeba9d4758");
    private static final TestResource<RoleType> METAROLE_MR32 = new TestResource<>(TEST_DIR, "metarole-mr3.2.xml", "59ae7541-cdb0-4d71-99ad-2df4130d5833");
    private static final TestResource<RoleType> METAROLE_MR33 = new TestResource<>(TEST_DIR, "metarole-mr3.3.xml", "9af8fcad-05e2-46c0-9e0c-31c037931a32");
    private static final TestResource<RoleType> METAROLE_MR41 = new TestResource<>(TEST_DIR, "metarole-mr4.1.xml", "b2876ccd-ed84-40f4-9841-f694a0722196");
    private static final TestResource<RoleType> METAROLE_MR51 = new TestResource<>(TEST_DIR, "metarole-mr5.1.xml", "7323da3b-4657-42f7-8e92-0dd27c34f4fd");
    private static final TestResource<RoleType> METAROLE_MR61 = new TestResource<>(TEST_DIR, "metarole-mr6.1.xml", "2bb624c8-1d55-4fd4-9c1b-a3e51c6a572a");

    private static final TestResource<RoleType> ROLE_R1 = new TestResource<>(TEST_DIR, "role-r1.xml", "b6897584-6b3e-421c-b4f3-b57123eac50c");
    private static final TestResource<RoleType> ROLE_R2 = new TestResource<>(TEST_DIR, "role-r2.xml", "e502a2b9-6961-42f6-91dd-f45edc6e2b02");
    private static final TestResource<RoleType> ROLE_R3 = new TestResource<>(TEST_DIR, "role-r3.xml", "7fc1925f-6e54-47a8-aa4b-65b1903d65eb");
    private static final TestResource<RoleType> ROLE_R4 = new TestResource<>(TEST_DIR, "role-r4.xml", "958d0b7b-146f-4c25-aee1-ae27d26e34ed");
    private static final TestResource<RoleType> ROLE_R5 = new TestResource<>(TEST_DIR, "role-r5.xml", "beb37147-f75a-4c44-a9ec-bc482c1e2a85");
    private static final TestResource<RoleType> ROLE_R6 = new TestResource<>(TEST_DIR, "role-r6.xml", "5c58ec3c-bb67-423c-ac4b-bb276c2e8c92");

    private static final TestResource<UserType> USER_ADAM = new TestResource<>(TEST_DIR, "user-adam.xml", "cf10f112-a731-45cd-8dfb-1b3fe9375c14");
    private static final TestResource<UserType> USER_BENJAMIN = new TestResource<>(TEST_DIR, "user-benjamin.xml", "2e1a427c-d6a7-4783-90ec-9dc0ebc98630");

    private static final TestResource<UserType> USER_FRODO = new TestResource<>(TEST_DIR, "user-frodo.xml", "786919b7-23c9-4a38-90e7-5a1efd0ab853");
    private static final TestResource<RoleType> ROLE_BEARABLE = new TestResource<>(TEST_DIR, "metarole-bearable.xml", "2421b2c5-8563-4ba7-9a87-f9ef4b169620");
    private static final TestResource<ServiceType> SERVICE_RING = new TestResource<>(TEST_DIR, "service-ring.xml", "7540cf28-a143-4eea-9379-75cae5d212cb");
    private static final TestResource<ServiceType> SERVICE_STING = new TestResource<>(TEST_DIR, "service-sting.xml", "f8b109bf-d393-47b9-8eec-41d74e39a992");
    private static final TestResource<RoleType> ROLE_PROPAGATOR = new TestResource<>(TEST_DIR, "role-propagator.xml", "8e56a98a-bcb9-4178-94f2-5488da473132");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_JIM, initResult);
        repoAdd(ROLE_SIMPLE, initResult);

        repoAdd(METAMETAROLE_MMR111, initResult);
        repoAdd(METAMETAROLE_MMR112, initResult);
        repoAdd(METAMETAROLE_MMR113, initResult);
        repoAdd(METAMETAROLE_MMR121, initResult);
        repoAdd(METAMETAROLE_MMR122, initResult);
        repoAdd(METAMETAROLE_MMR123, initResult);
        repoAdd(METAMETAROLE_MMR131, initResult);
        repoAdd(METAMETAROLE_MMR132, initResult);
        repoAdd(METAMETAROLE_MMR211, initResult);
        repoAdd(METAMETAROLE_MMR221, initResult);
        repoAdd(METAMETAROLE_MMR231, initResult);
        repoAdd(METAMETAROLE_MMR241, initResult);
        repoAdd(METAMETAROLE_MMR311, initResult);
        repoAdd(METAROLE_MR11, initResult);
        repoAdd(METAROLE_MR12, initResult);
        repoAdd(METAROLE_MR13, initResult);
        repoAdd(METAROLE_MR21, initResult);
        repoAdd(METAROLE_MR22, initResult);
        repoAdd(METAROLE_MR23, initResult);
        repoAdd(METAROLE_MR24, initResult);
        repoAdd(METAROLE_MR31, initResult);
        repoAdd(METAROLE_MR32, initResult);
        repoAdd(METAROLE_MR33, initResult);
        repoAdd(METAROLE_MR41, initResult);
        repoAdd(METAROLE_MR51, initResult);
        repoAdd(METAROLE_MR61, initResult);
        repoAdd(ROLE_R1, initResult);
        repoAdd(ROLE_R2, initResult);
        repoAdd(ROLE_R3, initResult);
        repoAdd(ROLE_R4, initResult);
        repoAdd(ROLE_R5, initResult);
        repoAdd(ROLE_R6, initResult);
        addObject(USER_ADAM, initTask, initResult);
        addObject(USER_BENJAMIN, initTask, initResult);

        repoAdd(ROLE_BEARABLE, initResult);
        repoAdd(SERVICE_RING, initResult);
        repoAdd(SERVICE_STING, initResult);
        repoAdd(ROLE_PROPAGATOR, initResult);
        repoAdd(USER_FRODO, initResult);
    }

    /**
     * Assign "simple" role to jim.
     * Focus mappings should be applied in correct order: name -> fullName -> description -> title -> honorificPrefix.
     *
     * See MID-5753.
     */
    @Test
    public void test100AssignSimpleToJim() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JIM.oid, ROLE_SIMPLE.oid, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> jimAfter = getUserFromRepo(USER_JIM.oid);
        UserType jimAfterBean = jimAfter.asObjectable();
        System.out.println("name = " + jimAfterBean.getName());
        System.out.println("fullName = " + jimAfterBean.getFullName());
        System.out.println("description = " + jimAfterBean.getDescription());
        System.out.println("title = " + jimAfterBean.getTitle());
        System.out.println("honorificPrefix = " + jimAfterBean.getHonorificPrefix());
        new UserAsserter<>(jimAfter)
                .display()
                .assertName("jim")
                .assertFullName("jim")
                .assertDescription("jim")
                .assertTitle("jim")
                .assertPolyStringProperty(UserType.F_HONORIFIC_PREFIX, "jim");
    }

    /**
     * Continuing with chaining tests. Now let's chain mappings coming from various sources.
     *
     * First, let's assign "ring" service to frodo.
     *
     * Assignments are now like this:
     *  - frodo ---> ring ---> bearable  M: sets organizationalUnit to "ring-bearer"
     *      |
     *      +------> propagator          M: sets fullName -> description
     *                                           name, honorificPrefix -> fullName
     *                                           title -> honorificPrefix
     *                                           organization -> title
     *                                           organizationalUnit -> organization
     *
     * Focus mappings should be applied in correct order:
     *    () -> organizationalUnit -> organization -> title -> honorificPrefix -> fullName -> description.
     */
    @Test
    public void test110AssignRingToFrodo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignService(USER_FRODO.oid, SERVICE_RING.oid, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> frodoAfter = getUserFromRepo(USER_FRODO.oid);
        UserType frodoAfterBean = frodoAfter.asObjectable();
        System.out.println("name = " + frodoAfterBean.getName());
        System.out.println("organizationalUnit = " + frodoAfterBean.getOrganizationalUnit());
        System.out.println("organization = " + frodoAfterBean.getOrganization());
        System.out.println("title = " + frodoAfterBean.getTitle());
        System.out.println("honorificPrefix = " + frodoAfterBean.getHonorificPrefix());
        System.out.println("fullName = " + frodoAfterBean.getFullName());
        System.out.println("description = " + frodoAfterBean.getDescription());
        new UserAsserter<>(frodoAfter)
                .display()
                .assertName("frodo")
                .assertOrganizationalUnits("ring-bearer")
                .assertOrganizations("ring-bearer")
                .assertTitle("ring-bearer")
                .assertPolyStringProperty(UserType.F_HONORIFIC_PREFIX, "ring-bearer")
                .assertFullName("frodo, the ring-bearer")
                .assertDescription("frodo, the ring-bearer");
    }

    /**
     * Variation of the above. Let's add sting to frodo.
     *
     * Assignments are now like this:
     *  - frodo -+---> ring -----> bearable  M: sets organizationalUnit to "ring-bearer"
     *      |    +---> sting ----> bearable  M: sets organizationalUnit to "sting-bearer"
     *      |
     *      +------> propagator          M: sets fullName -> description
     *                                           name, honorificPrefix -> fullName
     *                                           title -> honorificPrefix
     *                                           organization -> title
     *                                           organizationalUnit -> organization
     *
     * Focus mappings should be applied in correct order:
     *    () -> organizationalUnit -> organization -> title -> honorificPrefix -> fullName -> description.
     */
    @Test
    public void test120AssignStingToFrodo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignService(USER_FRODO.oid, SERVICE_STING.oid, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> frodoAfter = getUserFromRepo(USER_FRODO.oid);
        UserType frodoAfterBean = frodoAfter.asObjectable();
        System.out.println("name = " + frodoAfterBean.getName());
        System.out.println("organizationalUnit = " + frodoAfterBean.getOrganizationalUnit());
        System.out.println("organization = " + frodoAfterBean.getOrganization());
        System.out.println("title = " + frodoAfterBean.getTitle());
        System.out.println("honorificPrefix = " + frodoAfterBean.getHonorificPrefix());
        System.out.println("fullName = " + frodoAfterBean.getFullName());
        System.out.println("description = " + frodoAfterBean.getDescription());
        new UserAsserter<>(frodoAfter)
                .display()
                .assertName("frodo")
                .assertOrganizationalUnits("ring-bearer", "sting-bearer")
                .assertOrganizations("ring-bearer", "sting-bearer")
                .assertTitle("ring-bearer, sting-bearer")
                .assertPolyStringProperty(UserType.F_HONORIFIC_PREFIX, "ring-bearer, sting-bearer")
                .assertFullName("frodo, the ring-bearer, sting-bearer")
                .assertDescription("frodo, the ring-bearer, sting-bearer");
    }

    /**
     * Let's remove ring from frodo.
     *
     * Assignments are now like this:
     *  - frodo -----> sting ----> bearable  M: sets organizationalUnit to "sting-bearer"
     *      |
     *      +------> propagator          M: sets fullName -> description
     *                                           name, honorificPrefix -> fullName
     *                                           title -> honorificPrefix
     *                                           organization -> title
     *                                           organizationalUnit -> organization
     *
     * Focus mappings should be applied in correct order:
     *    () -> organizationalUnit -> organization -> title -> honorificPrefix -> fullName -> description.
     */
    @Test
    public void test130UnassignRingFromFrodo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        unassignService(USER_FRODO.oid, SERVICE_RING.oid, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> frodoAfter = getUserFromRepo(USER_FRODO.oid);
        UserType frodoAfterBean = frodoAfter.asObjectable();
        System.out.println("name = " + frodoAfterBean.getName());
        System.out.println("organizationalUnit = " + frodoAfterBean.getOrganizationalUnit());
        System.out.println("organization = " + frodoAfterBean.getOrganization());
        System.out.println("title = " + frodoAfterBean.getTitle());
        System.out.println("honorificPrefix = " + frodoAfterBean.getHonorificPrefix());
        System.out.println("fullName = " + frodoAfterBean.getFullName());
        System.out.println("description = " + frodoAfterBean.getDescription());
        new UserAsserter<>(frodoAfter)
                .display()
                .assertName("frodo")
                .assertOrganizationalUnits("sting-bearer")
                .assertOrganizations("sting-bearer")
                .assertTitle("sting-bearer")
                .assertPolyStringProperty(UserType.F_HONORIFIC_PREFIX, "sting-bearer")
                .assertFullName("frodo, the sting-bearer")
                .assertDescription("frodo, the sting-bearer");
    }

    /**
     * Add roles with validity and conditions set by various ways.
     *
     * mmr111 - everything is enabled
     * mmr112 - condition on individual mapping is 'false'
     * mmr113 - inducement condition is 'false' (on last inducement (with mappings))
     * mmr121 - inducement activation is 'disabled'
     * mmr122 - metarole mmr122 condition is 'false'
     * mmr123 - metarole mmr123 activation is 'disabled'
     * mmr131 - assignment mr13->mmr131 has condition of 'false'
     * mmr132 - assignment mr13->mmr132 activation is 'disabled'
     * mr21 - metarole mr21 condition is 'false'
     * mr22 - metarole mr22 activation is 'disabled'
     * mr23 - assignment r2->mr23 condition is 'false'
     * mr24 - assignment r2->mr24 activation is 'disabled'
     * r3 - role activation is 'disabled'                       (MID-4449)
     * r4 - role condition is false
     * r5 - assignment adam->r5 condition is 'false'
     * r6 - assignment adam->r6 activation is 'disabled'        (MID-4430)
     *
     * Conditions are bound to "title" property: if it's 'enabled' then they are set to true.
     */
    @Test
    public void test200AssignRolesToAdam() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(
                                createRoleAssignment(ROLE_R1),
                                createRoleAssignment(ROLE_R2),
                                createRoleAssignment(ROLE_R3),
                                createRoleAssignment(ROLE_R4),
                                createRoleAssignmentWithCondition(ROLE_R5),
                                createRoleAssignmentDisabled(ROLE_R6))
                        .asObjectDelta(USER_ADAM.oid),
                null, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> adamAfter = getUserFromRepo(USER_ADAM.oid);
        new UserAsserter<>(adamAfter)
                .display()
                .assertExtensionValue("p111a", "null:mmr1.1.1")
                .assertExtensionValue("p111b", "null:mmr1.1.1")
                .assertExtensionValue("p111c", "null:mmr1.1.1")
                .assertExtensionValue("p112a", "null:mmr1.1.2")         // further processing is disabled, so no b,c here
                .assertExtensionItems(4)
                .assertExtensionValues(4);
    }

    /**
     * Add roles with validity and conditions set by various ways.
     *
     * mmr111 - everything is enabled
     * mmr112 - condition on individual mapping is 'true'
     * mmr113 - inducement condition is 'true' (on last inducement (with mappings))
     * mmr121 - inducement activation is 'disabled'
     * mmr122 - metarole mmr122 condition is 'true'
     * mmr123 - metarole mmr123 activation is 'disabled'
     * mmr131 - assignment mr13->mmr131 has condition of 'true'
     * mmr132 - assignment mr13->mmr132 activation is 'disabled'
     * mr21 - metarole mr21 condition is 'true'
     * mr22 - metarole mr22 activation is 'disabled'
     * mr23 - assignment r2->mr23 condition is 'true'
     * mr24 - assignment r2->mr24 activation is 'disabled'
     * r3 - role activation is 'disabled'                       (MID-4449)
     * r4 - role condition is true
     * r5 - assignment adam->r5 condition is 'true'
     * r6 - assignment adam->r6 activation is 'disabled'        (MID-4430)
     *
     * Conditions are bound to "title" property: here it's 'enabled' so they are set to true.
     */
    @Test
    public void test210AssignRolesToBenjamin() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(
                                createRoleAssignment(ROLE_R1),
                                createRoleAssignment(ROLE_R2),
                                createRoleAssignment(ROLE_R3),
                                createRoleAssignment(ROLE_R4),
                                createRoleAssignmentWithCondition(ROLE_R5),
                                createRoleAssignmentDisabled(ROLE_R6))
                        .asObjectDelta(USER_BENJAMIN.oid),
                null, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> benjaminAfter = getUserFromRepo(USER_BENJAMIN.oid);
        new UserAsserter<>(benjaminAfter)
                .display()
                .assertExtensionValue("p111a", "null:mmr1.1.1")
                .assertExtensionValue("p111b", "null:mmr1.1.1")
                .assertExtensionValue("p111c", "null:mmr1.1.1")
                .assertExtensionValue("p112a", "null:mmr1.1.2")
                .assertExtensionValue("p112b", "null:mmr1.1.2")
                .assertExtensionValue("p112c", "null:mmr1.1.2")
                .assertExtensionValue("p113a", "null:mmr1.1.3")
                .assertExtensionValue("p113b", "null:mmr1.1.3")
                .assertExtensionValue("p113c", "null:mmr1.1.3")
                // no p121 (activation disabled)
                .assertExtensionValue("p122a", "null:mmr1.2.2")
                .assertExtensionValue("p122b", "null:mmr1.2.2")
                .assertExtensionValue("p122c", "null:mmr1.2.2")
                // no p123 (activation disabled)
                .assertExtensionValue("p131a", "null:mmr1.3.1")
                .assertExtensionValue("p131b", "null:mmr1.3.1")
                .assertExtensionValue("p131c", "null:mmr1.3.1")
                // no p132 (activation disabled)
                .assertExtensionValue("p211a", "null:mmr2.1.1")
                .assertExtensionValue("p211b", "null:mmr2.1.1")
                .assertExtensionValue("p211c", "null:mmr2.1.1")
                // no p221 (activation disabled)
                .assertExtensionValue("p231a", "null:mmr2.3.1")
                .assertExtensionValue("p231b", "null:mmr2.3.1")
                .assertExtensionValue("p231c", "null:mmr2.3.1")
                // no p241 (activation disabled)
                // no p311 (activation disabled)
                .assertExtensionValue("p41", "null:mr4.1")
                .assertExtensionValue("p51", "null:mr5.1")
                // no p61 (activation disabled)
                .assertExtensionItems(23)
                .assertExtensionValues(23);
    }

    /**
     * Switch Adam condition from false to true.
     *
     * mmr111 - everything is enabled
     * mmr112 - condition on individual mapping is 'false' -> 'true'
     * mmr113 - inducement condition is 'false' -> 'true' (on last inducement (with mappings))  (MID-5783)
     * mmr121 - inducement activation is 'disabled'
     * mmr122 - metarole mmr122 condition is 'false' -> 'true'                                  (MID-5783)
     * mmr123 - metarole mmr123 activation is 'disabled'
     * mmr131 - assignment mr13->mmr131 has condition of 'false' -> 'true'                      (MID-5783)
     * mmr132 - assignment mr13->mmr132 activation is 'disabled'
     * mr21 - metarole mr21 condition is 'false' -> 'true'                                      (MID-5783)
     * mr22 - metarole mr22 activation is 'disabled'
     * mr23 - assignment r2->mr23 condition is 'false' -> 'true'                                (MID-5783)
     * mr24 - assignment r2->mr24 activation is 'disabled'
     * r3 - role activation is 'disabled'                                                       (MID-4449)
     * r4 - role condition is 'false' -> 'true'                                                 (MID-5783)
     * r5 - assignment adam->r5 condition is 'false' -> 'true'                                  (MID-5783)
     * r6 - assignment adam->r6 activation is 'disabled'                                        (MID-4430)
     *
     * Conditions are bound to "title" property: if it's 'enabled' then they are set to true.
     */
    @Test
    public void test220AdamConditionFalseToTrue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_TITLE)
                        .replace(PolyString.fromOrig("enabled"))
                        .asObjectDelta(USER_ADAM.oid),
                null, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> adamAfter = getUserFromRepo(USER_ADAM.oid);
        new UserAsserter<>(adamAfter)
                .display()
                .assertExtensionValue("p111a", "null:mmr1.1.1")
                .assertExtensionValue("p111b", "null:mmr1.1.1")
                .assertExtensionValue("p111c", "null:mmr1.1.1")
                .assertExtensionValue("p112a", "null:mmr1.1.2")
                .assertExtensionValue("p112b", "null:mmr1.1.2")
                .assertExtensionValue("p112c", "null:mmr1.1.2")
                .assertExtensionValue("p113a", "null:mmr1.1.3")
                .assertExtensionValue("p113b", "null:mmr1.1.3")
                .assertExtensionValue("p113c", "null:mmr1.1.3")
                // no p121 (activation disabled)
                .assertExtensionValue("p122a", "null:mmr1.2.2")
                .assertExtensionValue("p122b", "null:mmr1.2.2")
                .assertExtensionValue("p122c", "null:mmr1.2.2")
                // no p123 (activation disabled)
                .assertExtensionValue("p131a", "null:mmr1.3.1")
                .assertExtensionValue("p131b", "null:mmr1.3.1")
                .assertExtensionValue("p131c", "null:mmr1.3.1")
                // no p132 (activation disabled)
                .assertExtensionValue("p211a", "null:mmr2.1.1")
                .assertExtensionValue("p211b", "null:mmr2.1.1")
                .assertExtensionValue("p211c", "null:mmr2.1.1")
                // no p221 (activation disabled)
                .assertExtensionValue("p231a", "null:mmr2.3.1")
                .assertExtensionValue("p231b", "null:mmr2.3.1")
                .assertExtensionValue("p231c", "null:mmr2.3.1")
                // no p241 (activation disabled)
                // no p311 (activation disabled)
                .assertExtensionValue("p41", "null:mr4.1")
                .assertExtensionValue("p51", "null:mr5.1")
                // no p61 (activation disabled)
                .assertExtensionItems(23)
                .assertExtensionValues(23);
    }

    /**
     * Switch Benjamin condition from true to false.
     *
     * mmr111 - everything is enabled
     * mmr112 - condition on individual mapping is 'true' -> 'false'
     * mmr113 - inducement condition is 'true' -> 'false' (on last inducement (with mappings))      (MID-5783)
     * mmr121 - inducement activation is 'disabled'
     * mmr122 - metarole mmr122 condition is 'true' -> 'false'                                      (MID-5783)
     * mmr123 - metarole mmr123 activation is 'disabled'
     * mmr131 - assignment mr13->mmr131 has condition of 'true' -> 'false'                          (MID-5783)
     * mmr132 - assignment mr13->mmr132 activation is 'disabled'
     * mr21 - metarole mr21 condition is 'true' -> 'false'                                          (MID-5783)
     * mr22 - metarole mr22 activation is 'disabled'
     * mr23 - assignment r2->mr23 condition is 'true' -> 'false'                                    (MID-5783)
     * mr24 - assignment r2->mr24 activation is 'disabled'
     * r3 - role activation is 'disabled'                                                           (MID-4449)
     * r4 - role condition is 'true' -> 'false'                                                     (MID-5783)
     * r5 - assignment adam->r5 condition is 'true' -> 'false'                                      (MID-5783)
     * r6 - assignment adam->r6 activation is 'disabled'                                            (MID-4430)
     *
     * Conditions are bound to "title" property: if it's 'enabled' then they are set to true.
     */
    @Test
    public void test230BenjaminConditionTrueToFalse() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_TITLE)
                        .replace()
                        .asObjectDelta(USER_BENJAMIN.oid),
                null, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> benjaminAfter = getUserFromRepo(USER_BENJAMIN.oid);
        new UserAsserter<>(benjaminAfter)
                .display()
                .assertExtensionValue("p111a", "null:mmr1.1.1")
                .assertExtensionValue("p111b", "null:mmr1.1.1")
                .assertExtensionValue("p111c", "null:mmr1.1.1")
                .assertExtensionValue("p112a", "null:mmr1.1.2") // further processing is disabled, so no b,c here
                .assertExtensionItems(4)
                .assertExtensionValues(4);
    }

    /**
     * Unassign the roles. Extension values should disappear.
     */
    @Test
    public void test280UnassignRolesFromAdam() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .replace()
                        .asObjectDelta(USER_ADAM.oid),
                null, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> adamAfter = getUserFromRepo(USER_ADAM.oid);
        new UserAsserter<>(adamAfter)
                .display()
                .assertExtensionItems(0)
                .assertExtensionValues(0);
    }

    /**
     * Unassign the roles. Extension values should disappear.
     */
    @Test
    public void test290UnassignRolesFromBenjamin() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .replace()
                        .asObjectDelta(USER_BENJAMIN.oid),
                null, task, result);

        // THEN
        assertSuccess(result);
        PrismObject<UserType> benjaminAfter = getUserFromRepo(USER_BENJAMIN.oid);
        new UserAsserter<>(benjaminAfter)
                .display()
                .assertExtensionItems(0)
                .assertExtensionValues(0);
    }

    private AssignmentType createRoleAssignment(TestResource<?> role) {
        return new AssignmentType()
                .targetRef(role.oid, RoleType.COMPLEX_TYPE);
    }

    @SuppressWarnings("SameParameterValue")
    private AssignmentType createRoleAssignmentWithCondition(TestResource<?> role) {
        ScriptExpressionEvaluatorType scriptExpressionEvaluator = new ScriptExpressionEvaluatorType();
        scriptExpressionEvaluator.setCode("basic.stringify(title) == 'enabled'");
        ExpressionType expression = new ExpressionType();
        expression.getExpressionEvaluator().add(new JAXBElement<>(SchemaConstantsGenerated.C_SCRIPT,
                ScriptExpressionEvaluatorType.class, scriptExpressionEvaluator));
        MappingType condition = new MappingType()
                .source(new VariableBindingDefinitionType().path(new ItemPathType(UserType.F_TITLE)))
                .expression(expression);

        return new AssignmentType()
                .targetRef(role.oid, RoleType.COMPLEX_TYPE)
                .condition(condition);
    }

    @SuppressWarnings("SameParameterValue")
    private AssignmentType createRoleAssignmentDisabled(TestResource<?> role) {
        return new AssignmentType()
                .targetRef(role.oid, RoleType.COMPLEX_TYPE)
                .beginActivation()
                    .administrativeStatus(ActivationStatusType.DISABLED)
                .end();
    }
}
