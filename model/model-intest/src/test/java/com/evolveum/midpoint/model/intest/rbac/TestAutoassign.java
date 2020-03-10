/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.rbac;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleManagementConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAutoassign extends AbstractRbacTest {

    protected static final File AUTOASSIGN_DIR = new File(TEST_DIR, "autoassign");

    protected static final File ROLE_UNIT_WORKER_FILE = new File(AUTOASSIGN_DIR, "role-unit-worker.xml");
    protected static final String ROLE_UNIT_WORKER_OID = "e83969fa-bfda-11e7-8b5b-ab60e0279f06";
    protected static final String ROLE_UNIT_WORKER_TITLE = "Worker";

    protected static final File ROLE_UNIT_SLEEPER_FILE = new File(AUTOASSIGN_DIR, "role-unit-sleeper.xml");
    protected static final String ROLE_UNIT_SLEEPER_OID = "660f0fb8-bfec-11e7-bb16-07154f1e53a6";
    protected static final String ROLE_UNIT_SLEEPER_TITLE = "Sleeper";

    protected static final File ROLE_UNIT_WALKER_FILE = new File(AUTOASSIGN_DIR, "role-unit-walker.xml");
    protected static final String ROLE_UNIT_WALKER_OID = "a2bc45fc-bfec-11e7-bdfd-af4b3e689502";
    protected static final String ROLE_UNIT_WALKER_TITLE = "Walker";

    protected static final String UNIT_WORKER = "worker";
    protected static final String UNIT_SLEEPER = "sleeper";
    protected static final String UNIT_WALKER = "walker";

    private static final XMLGregorianCalendar ROLE_SLEEPER_AUTOASSIGN_VALID_TO =
            XmlTypeConverter.createXMLGregorianCalendar(2222, 1, 2, 3, 4, 5);

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_UNIT_WORKER_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_UNIT_SLEEPER_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_UNIT_WALKER_FILE, RoleType.class, initResult);

        // Temporarily using repository service because of MID-5497
        repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                prismContext.deltaFor(SystemConfigurationType.class)
                    .item(SystemConfigurationType.F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_AUTOASSIGN_ENABLED)
                    .replace(true)
                    .asItemDeltas(), initResult);
//        modifyObjectReplaceProperty(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
//                ItemPath.create(SystemConfigurationType.F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_AUTOASSIGN_ENABLED),
//                initTask, initResult, Boolean.TRUE);
    }

    /**
     * MID-2840
     */
    @Test
    public void test100ModifyUnitWorker() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                createPolyString(UNIT_WORKER));

        // THEN
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assertOrganizationalUnit(UNIT_WORKER)
            .assignments()
                .single()
                    .assertTargetOid(ROLE_UNIT_WORKER_OID)
                    .assertTargetType(RoleType.COMPLEX_TYPE)
                    .end()
                .end()
            .links()
                .single();

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_UNIT_WORKER_TITLE);
    }

    /**
     * MID-2840
     */
    @Test
    public void test109ModifyUniNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result /* no value */);

        // THEN
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assertNoOrganizationalUnit()
            .assignments()
                .assertNone()
                .end()
            .links()
                .assertNone();

        assertNoDummyAccount(null, USER_JACK_USERNAME);
    }

    /**
     * MID-2840
     */
    @Test
    public void test110ModifyUnitSleepwalker() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                createPolyString(UNIT_SLEEPER), createPolyString(UNIT_WALKER));

        // THEN
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assertOrganizationalUnits(UNIT_SLEEPER, UNIT_WALKER)
            .assignments()
                .assertAssignments(2)
                .by()
                    .targetOid(ROLE_UNIT_SLEEPER_OID)
                .find()
                    .assertTargetType(RoleType.COMPLEX_TYPE)
                    .activation()
                        .assertValidTo(ROLE_SLEEPER_AUTOASSIGN_VALID_TO)
                        .end()
                    .end()
                .assertRole(ROLE_UNIT_WALKER_OID)
                .end()
            .links()
                .single();

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                    ROLE_UNIT_SLEEPER_TITLE, ROLE_UNIT_WALKER_TITLE);
    }

    /**
     * MID-2840
     */
    @Test
    public void test112ModifyUnitSleeperToWorker() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = prismContext.deltaFactory().object().createModificationAddProperty(UserType.class,
                USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, createPolyString(UNIT_WORKER));
        objectDelta.addModificationDeleteProperty(UserType.F_ORGANIZATIONAL_UNIT, createPolyString(UNIT_SLEEPER));

        // WHEN
        executeChanges(objectDelta, null, task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assertOrganizationalUnits(UNIT_WORKER, UNIT_WALKER)
            .assignments()
                .assertAssignments(2)
                .assertRole(ROLE_UNIT_WORKER_OID)
                .assertRole(ROLE_UNIT_WALKER_OID)
                .end()
            .links()
                .single();

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                    ROLE_UNIT_WORKER_TITLE, ROLE_UNIT_WALKER_TITLE);
    }

    /**
     * MID-2840
     */
    @Test
    public void test114ModifyUnitAddSleeper() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                createPolyString(UNIT_SLEEPER));

        // THEN
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assertOrganizationalUnits(UNIT_WORKER, UNIT_WALKER, UNIT_SLEEPER)
            .assignments()
                .assertAssignments(3)
                .assertRole(ROLE_UNIT_WORKER_OID)
                .assertRole(ROLE_UNIT_WALKER_OID)
                .assertRole(ROLE_UNIT_SLEEPER_OID)
                .end()
            .links()
                .single();

        assertDummyAccountByUsername(null, USER_JACK_USERNAME)
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                    ROLE_UNIT_WORKER_TITLE, ROLE_UNIT_WALKER_TITLE, ROLE_UNIT_SLEEPER_TITLE);
    }

    // TODO: org and relation

    // TODO: combine autoassign with object template role assign
}
