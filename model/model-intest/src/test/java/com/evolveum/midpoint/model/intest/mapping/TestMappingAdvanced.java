/*
 * Copyright (C) 2020-21 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mapping;

import java.io.File;
import java.io.IOException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Various advanced tests related to mappings.
 *
 * NOT a subclass of AbstractMappingTest.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMappingAdvanced extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/mapping/advanced");
    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final String ATTR_ORGANIZATION = "organization";

    private static final DummyTestResource RESOURCE_DUMMY_ALPHA = new DummyTestResource(TEST_DIR, "resource-dummy-alpha.xml", "8f6b271a-c279-4b1b-bb91-e675c67be243", "alpha");

    private static final TestResource<ObjectTemplateType> USER_TEMPLATE_INCREMENTING = new TestResource<>(TEST_DIR, "user-template-incrementing.xml", "bf0cf9b7-4c38-4ff4-afc6-c9cc9bc08490");
    private static final TestResource<UserType> USER_FRANZ = new TestResource<>(TEST_DIR, "user-franz.xml", "ac42084a-d780-4d32-ac02-bcc49bdc747b");
    private static final TestResource<UserType> USER_FREDERIC = new TestResource<>(TEST_DIR, "user-frederic.xml", "849d540c-d052-43c0-937a-1ca1cda0679e");
    private static final TestResource<UserType> USER_JOHANN = new TestResource<>(TEST_DIR, "user-johann.xml", "25fa06c6-04e2-4f9e-97c4-87fa5613bb15");

    private static final File ASSIGNMENT_FREDERIC_ALPHA_FILE = new File(TEST_DIR, "assignment-frederic-alpha.xml");
    private static final File ASSIGNMENT_JOHANN_ALPHA_FILE = new File(TEST_DIR, "assignment-johann-alpha.xml");

    // ranges
    private static final DummyTestResource RESOURCE_DUMMY_RANGES_DIRECT = new DummyTestResource(TEST_DIR, "resource-dummy-ranges-direct.xml", "0164ac9c-2727-44cf-be0f-96c4f600017c", "ranges-direct",
            controller -> {
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_ORGANIZATION, String.class, false, true);
                controller.addAttrDef(controller.getDummyResource().getGroupObjectClass(),
                        DummyGroup.ATTR_MEMBERS_NAME, String.class, false, true);
            });

    private static final DummyTestResource RESOURCE_DUMMY_RANGES_ROLE = new DummyTestResource(TEST_DIR, "resource-dummy-ranges-role.xml", "96b44c65-011f-489c-bcdb-c5f9a2502942", "ranges-role",
            controller -> {
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_ORGANIZATION, String.class, false, true);
                controller.addAttrDef(controller.getDummyResource().getGroupObjectClass(),
                        DummyGroup.ATTR_MEMBERS_NAME, String.class, false, true);
            });

    private static final TestResource<UserType> USER_MAGNUS = new TestResource<>(TEST_DIR, "user-magnus.xml", "89072af5-afc6-4650-a4f8-036fd3d92b0d");
    private static final TestResource<UserType> USER_VLADIMIR = new TestResource<>(TEST_DIR, "user-vladimir.xml", "715de93d-50c5-4c29-9da1-b5906fe3a2c3");

    private static final TestResource<RoleType> ROLE_RANGES = new TestResource<>(TEST_DIR, "role-ranges.xml", "d97c804b-28ea-404d-b075-ebb2bdd76c57");

    private static final String MP_USERS = "mp_users";
    private static final String MP_TESTERS = "mp_testers";
    private static final String VALID_GROUP = "validGroup";

    private static final String MAGNUS = "magnus";
    private static final String VLADIMIR = "vladimir";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_TEMPLATE_INCREMENTING, initResult);
        repoAdd(USER_FRANZ, initResult); // intentionally not via full processing
        repoAdd(USER_FREDERIC, initResult); // intentionally not via full processing
        repoAdd(USER_JOHANN, initResult); // intentionally not via full processing

        initDummyResource(RESOURCE_DUMMY_ALPHA, initTask, initResult);

        // "Ranges" scenario

        initDummyResource(RESOURCE_DUMMY_RANGES_DIRECT, initTask, initResult);
        initDummyResource(RESOURCE_DUMMY_RANGES_ROLE, initTask, initResult);

        RESOURCE_DUMMY_RANGES_DIRECT.controller.addGroup(MP_USERS);
        RESOURCE_DUMMY_RANGES_DIRECT.controller.addGroup(MP_TESTERS);
        RESOURCE_DUMMY_RANGES_DIRECT.controller.addGroup(VALID_GROUP);

        RESOURCE_DUMMY_RANGES_ROLE.controller.addGroup(MP_USERS);
        RESOURCE_DUMMY_RANGES_ROLE.controller.addGroup(MP_TESTERS);
        RESOURCE_DUMMY_RANGES_ROLE.controller.addGroup(VALID_GROUP);

        repoAdd(USER_MAGNUS, initResult);
        repoAdd(USER_VLADIMIR, initResult);
        repoAdd(ROLE_RANGES, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100RecomputeFranz() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        recomputeUser(USER_FRANZ.oid, task, result);

        then();
        assertUserAfter(USER_FRANZ.oid)
                .assertDescription("2") // from 0 to 1 in the first projector run, from 1 to 2 in the second
                .assertOrganizations("O2"); // should be from O0 to O1, then from O1 to O2 leading to [O2]
    }

    /**
     * Simply assigning Alpha account to Frederic (with prescribed fullname), without any conflicts.
     */
    @Test
    public void test200AssignAlphaToFredericNoConflict() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(getAssignment(ASSIGNMENT_FREDERIC_ALPHA_FILE))
                .asObjectDelta(USER_FREDERIC.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(task);
        assertDummyAccountByUsername(RESOURCE_DUMMY_ALPHA.name, "frederic")
                .display()
                .assertFullName("F. Chopin");
    }

    /**
     * Assigning Alpha account to Johann (with prescribed fullname), but this time having conflict
     * that causes repeating wave 0.
     *
     * There are three values of fullname coming into play here:
     * - JOHANN SEBASTIAN BACH: existing one
     * - Johann Sebastian Bach: weakly-mapped value from user's fullName property
     * - J. S. Bach: value provided by the (normal-strength) assignment being added
     *
     * The expected output is J. S. Bach. The mapping is "plus" because the assignment is being added.
     * The complication is that wave 0 is repeated twice:
     * - first execution fails on the conflict
     * - so we restart the wave -- but this time the assignment is not considered to be "plus",
     * because the primary delta is gone, and the assignment is part of the current focus object!
     */
    @Test
    public void test210AssignAlphaToJohannWithConflict() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        RESOURCE_DUMMY_ALPHA.controller.addAccount("johann", "JOHANN SEBASTIAN BACH");

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(getAssignment(ASSIGNMENT_JOHANN_ALPHA_FILE))
                .asObjectDelta(USER_JOHANN.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        assertDummyAccountByUsername(RESOURCE_DUMMY_ALPHA.name, "johann")
                .display()
                .assertFullName("J. S. Bach");
    }

    /**
     * Assigns "ranges direct" resource to Magnus.
     * Observe if mapping ranges are correctly processed (should be trivial, as there is no existing account).
     */
    @Test
    public void test300AssignRangesDirectToMagnus() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignAccountToUser(USER_MAGNUS.oid, RESOURCE_DUMMY_RANGES_DIRECT.oid, "default", task, result);

        then();
        assertSuccess(task);

        assertDummyAccountByUsername(RESOURCE_DUMMY_RANGES_DIRECT.name, MAGNUS)
                .display()
                .assertAttribute(ATTR_ORGANIZATION, "mp_FIDE");
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_DIRECT.name, MP_USERS)
                .display()
                .assertMembers(MAGNUS);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_DIRECT.name, MP_TESTERS)
                .display()
                .assertMembers();
    }

    /**
     * Add some extra values to the account and use recompute. Ranges should throw (some of) them out.
     *
     * Extra values:
     * - attribute/organization: mp_garbage (should be thrown out)
     * - attribute/organization: valid (should be kept as it does not belong to mapping range)
     * - association/group: mp_testers (should be thrown out)
     * - association/group: validGroup (should be kept)
     */
    @Test
    public void test310GiveMagnusExtraValuesAndRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount magnus = RESOURCE_DUMMY_RANGES_DIRECT.controller.getDummyResource().getAccountByUsername(MAGNUS);
        magnus.addAttributeValue(ATTR_ORGANIZATION, "mp_garbage");
        magnus.addAttributeValue(ATTR_ORGANIZATION, "valid");

        DummyGroup testers = RESOURCE_DUMMY_RANGES_DIRECT.controller.getDummyResource().getGroupByName(MP_TESTERS);
        testers.addMember(MAGNUS);

        DummyGroup validGroup = RESOURCE_DUMMY_RANGES_DIRECT.controller.getDummyResource().getGroupByName(VALID_GROUP);
        validGroup.addMember(MAGNUS);

        when();
        recomputeUser(USER_MAGNUS.oid, task, result);

        then();
        assertSuccess(task);
        assertDummyAccountByUsername(RESOURCE_DUMMY_RANGES_DIRECT.name, MAGNUS)
                .display()
                .assertAttribute(ATTR_ORGANIZATION, "mp_FIDE", "valid");
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_DIRECT.name, MP_USERS)
                .display()
                .assertMembers(MAGNUS);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_DIRECT.name, VALID_GROUP)
                .display()
                .assertMembers(MAGNUS);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_DIRECT.name, MP_TESTERS)
                .display()
                .assertMembers();
    }

    /**
     * Add some extra values to the account and simply modify magnus. Ranges should throw (some of) them out.
     *
     * Extra values:
     * - attribute/organization: mp_garbage (should be thrown out)
     * - attribute/organization: valid (should be kept as it does not belong to mapping range)
     * - association/group: mp_testers (should be thrown out)
     * - association/group: validGroup (should be kept)
     */
    @Test
    public void test320GiveMagnusExtraValuesAndModifyUser() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount magnus = RESOURCE_DUMMY_RANGES_DIRECT.controller.getDummyResource().getAccountByUsername(MAGNUS);
        magnus.addAttributeValue(ATTR_ORGANIZATION, "mp_garbage");

        DummyGroup testers = RESOURCE_DUMMY_RANGES_DIRECT.controller.getDummyResource().getGroupByName(MP_TESTERS);
        testers.addMember(MAGNUS);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME)
                .replace(PolyString.fromOrig("Magnus Carlsen"))
                .asObjectDelta(USER_MAGNUS.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(task);
        assertDummyAccountByUsername(RESOURCE_DUMMY_RANGES_DIRECT.name, MAGNUS)
                .display()
                .assertAttribute(ATTR_ORGANIZATION, "mp_FIDE", "valid");
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_DIRECT.name, MP_USERS)
                .display()
                .assertMembers(MAGNUS);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_DIRECT.name, VALID_GROUP)
                .display()
                .assertMembers(MAGNUS);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_DIRECT.name, MP_TESTERS)
                .display()
                .assertMembers();
    }

    /**
     * Assigns "ranges role" resource to Vladimir.
     * Observe if mapping ranges are correctly processed (should be trivial, as there is no existing account).
     */
    @Test
    public void test350AssignRangesRoleToVladimir() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignRole(USER_VLADIMIR.oid, ROLE_RANGES.oid, task, result);

        then();
        assertSuccess(task);

        assertDummyAccountByUsername(RESOURCE_DUMMY_RANGES_ROLE.name, VLADIMIR)
                .display()
                .assertAttribute(ATTR_ORGANIZATION, "mp_FIDE");
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_ROLE.name, MP_USERS)
                .display()
                .assertMembers(VLADIMIR);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_ROLE.name, MP_TESTERS)
                .display()
                .assertMembers();
    }

    /**
     * Add some extra values to the account and use recompute. Ranges should throw (some of) them out.
     *
     * Extra values:
     * - attribute/organization: mp_garbage (should be thrown out)
     * - attribute/organization: valid (should be kept as it does not belong to mapping range)
     * - association/group: mp_testers (should be thrown out)
     * - association/group: validGroup (should be kept)
     */
    @Test
    public void test360GiveVladimirExtraValuesAndRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount vladimir = RESOURCE_DUMMY_RANGES_ROLE.controller.getDummyResource().getAccountByUsername(VLADIMIR);
        vladimir.addAttributeValue(ATTR_ORGANIZATION, "mp_garbage");
        vladimir.addAttributeValue(ATTR_ORGANIZATION, "valid");

        DummyGroup testers = RESOURCE_DUMMY_RANGES_ROLE.controller.getDummyResource().getGroupByName(MP_TESTERS);
        testers.addMember(VLADIMIR);

        DummyGroup validGroup = RESOURCE_DUMMY_RANGES_ROLE.controller.getDummyResource().getGroupByName(VALID_GROUP);
        validGroup.addMember(VLADIMIR);

        when();
        recomputeUser(USER_VLADIMIR.oid, task, result);

        then();
        assertSuccess(task);
        assertDummyAccountByUsername(RESOURCE_DUMMY_RANGES_ROLE.name, VLADIMIR)
                .display()
                .assertAttribute(ATTR_ORGANIZATION, "mp_FIDE", "valid");
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_ROLE.name, MP_USERS)
                .display()
                .assertMembers(VLADIMIR);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_ROLE.name, VALID_GROUP)
                .display()
                .assertMembers(VLADIMIR);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_ROLE.name, MP_TESTERS)
                .display()
                .assertMembers();
    }

    /**
     * Add some extra values to the account and simply modify magnus. Ranges should throw (some of) them out.
     *
     * - attribute/organization: mp_garbage (should be thrown out)
     * - attribute/organization: valid (should be kept as it does not belong to mapping range)
     * - association/group: mp_testers (should be thrown out)
     * - association/group: validGroup (should be kept)
     */
    @Test
    public void test370GiveVladimirExtraValuesAndModifyUser() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount vladimir = RESOURCE_DUMMY_RANGES_ROLE.controller.getDummyResource().getAccountByUsername(VLADIMIR);
        vladimir.addAttributeValue(ATTR_ORGANIZATION, "mp_garbage");

        DummyGroup testers = RESOURCE_DUMMY_RANGES_ROLE.controller.getDummyResource().getGroupByName(MP_TESTERS);
        testers.addMember(VLADIMIR);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME)
                .replace(PolyString.fromOrig("Vladimir Kramnik"))
                .asObjectDelta(USER_VLADIMIR.oid);

        when();
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(task);
        assertDummyAccountByUsername(RESOURCE_DUMMY_RANGES_ROLE.name, VLADIMIR)
                .display()
                .assertAttribute(ATTR_ORGANIZATION, "mp_FIDE", "valid");
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_ROLE.name, MP_USERS)
                .display()
                .assertMembers(VLADIMIR);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_ROLE.name, VALID_GROUP)
                .display()
                .assertMembers(VLADIMIR);
        assertDummyGroupByName(RESOURCE_DUMMY_RANGES_ROLE.name, MP_TESTERS)
                .display()
                .assertMembers();
    }

    private AssignmentType getAssignment(File file) throws IOException, SchemaException {
        return prismContext.parserFor(file).parseRealValue();
    }
}
