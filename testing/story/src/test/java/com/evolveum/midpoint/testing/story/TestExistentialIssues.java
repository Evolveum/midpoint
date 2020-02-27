/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests for weird activation existence mappings, delayed deletes and similar existential issues.
 *
 * MID-4564
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestExistentialIssues extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "existential");

    protected static final File RESOURCE_DUMMY_LAZY_FILE = new File(TEST_DIR, "resource-dummy-lazy.xml");
    protected static final String RESOURCE_DUMMY_LAZY_OID = "306555be-3e38-11e8-bd96-7b572e4e7d89";
    protected static final String RESOURCE_DUMMY_LAZY_NAME = "lazy";

    protected static final File USER_DESCARTES_FILE = new File(TEST_DIR, "user-descartes.xml");
    protected static final String USER_DESCARTES_OID = "d642c1bc-3e36-11e8-8346-bf95880c9be7";
    protected static final String USER_DESCARTES_USERNAME = "descartes";
    protected static final String USER_DESCARTES_FULL_NAME = "René Descartes";

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_LAZY_NAME,
                RESOURCE_DUMMY_LAZY_FILE, RESOURCE_DUMMY_LAZY_OID, initTask, initResult);

        repoAddObjectFromFile(USER_DESCARTES_FILE, true, initResult);
    }

    /**
     * Mostly just sanity check and preparation.
     */
    @Test
    public void test100DisableUserDescartes() throws Exception {
        final String TEST_NAME = "test100DisableUserDescartes";
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        modifyUserReplace(USER_DESCARTES_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUserFromRepo(USER_DESCARTES_OID);
        display("User after (repo)", userAfter);
        assertAdministrativeStatusDisabled(userAfter);
        assertEffectiveStatus(userAfter, ActivationStatusType.DISABLED);
        assertAssignments(userAfter, 0);
        assertLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_LAZY_NAME, USER_DESCARTES_USERNAME);
    }

    /**
     * MID-4564
     */
    @Test
    public void test110DescartesAssignLazyAccount() throws Exception {
        final String TEST_NAME = "test110DescartesAssignLazyAccount";
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignAccountToUser(USER_DESCARTES_OID, RESOURCE_DUMMY_LAZY_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUserFromRepo(USER_DESCARTES_OID);
        display("User after (repo)", userAfter);
        assertAdministrativeStatusDisabled(userAfter);
        assertEffectiveStatus(userAfter, ActivationStatusType.DISABLED);
        assertAssignments(userAfter, 1);
        assertLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_LAZY_NAME, USER_DESCARTES_USERNAME);
    }

    /**
     * MID-4564
     */
    @Test
    public void test112EnableDescartes() throws Exception {
        final String TEST_NAME = "test112EnableDescartes";
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        modifyUserReplace(USER_DESCARTES_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUserFromRepo(USER_DESCARTES_OID);
        display("User after (repo)", userAfter);
        assertAdministrativeStatusEnabled(userAfter);
        assertEffectiveStatus(userAfter, ActivationStatusType.ENABLED);
        assertAssignments(userAfter, 1);
        assertLinks(userAfter, 1);

        assertDummyAccount(RESOURCE_DUMMY_LAZY_NAME, USER_DESCARTES_USERNAME, USER_DESCARTES_FULL_NAME, true);
    }

    /**
     * MID-4564
     */
    @Test
    public void test114DisableDescartes() throws Exception {
        final String TEST_NAME = "test112EnableDescartes";
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        modifyUserReplace(USER_DESCARTES_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUserFromRepo(USER_DESCARTES_OID);
        display("User after (repo)", userAfter);
        assertAdministrativeStatusDisabled(userAfter);
        assertEffectiveStatus(userAfter, ActivationStatusType.DISABLED);
        assertAssignments(userAfter, 1);
        assertLinks(userAfter, 1);

        assertDummyAccount(RESOURCE_DUMMY_LAZY_NAME, USER_DESCARTES_USERNAME, USER_DESCARTES_FULL_NAME, false);
    }

    /**
     * MID-4564
     */
    @Test
    public void test116ReenableDescartes() throws Exception {
        final String TEST_NAME = "test116ReenableDescartes";
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        modifyUserReplace(USER_DESCARTES_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUserFromRepo(USER_DESCARTES_OID);
        display("User after (repo)", userAfter);
        assertAdministrativeStatusEnabled(userAfter);
        assertEffectiveStatus(userAfter, ActivationStatusType.ENABLED);
        assertAssignments(userAfter, 1);
        assertLinks(userAfter, 1);

        assertDummyAccount(RESOURCE_DUMMY_LAZY_NAME, USER_DESCARTES_USERNAME, USER_DESCARTES_FULL_NAME, true);
    }

    /**
     * MID-4564
     */
    @Test
    public void test120DescartesUnassignLazyAccount() throws Exception {
        final String TEST_NAME = "test120DescartesUnassignLazyAccount";
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignAccountFromUser(USER_DESCARTES_OID, RESOURCE_DUMMY_LAZY_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUserFromRepo(USER_DESCARTES_OID);
        display("User after (repo)", userAfter);
        assertAdministrativeStatusEnabled(userAfter);
        assertEffectiveStatus(userAfter, ActivationStatusType.ENABLED);
        assertAssignments(userAfter, 0);
        assertLinks(userAfter, 1);

        assertDummyAccount(RESOURCE_DUMMY_LAZY_NAME, USER_DESCARTES_USERNAME, USER_DESCARTES_FULL_NAME, false);
    }

    /**
     * MID-4564
     */
    @Test
    public void test129DeleteDescartes() throws Exception {
        final String TEST_NAME = "test129DeleteDescartes";
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        deleteObject(UserType.class, USER_DESCARTES_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertNoObject(UserType.class, USER_DESCARTES_OID);

        assertNoDummyAccount(RESOURCE_DUMMY_LAZY_NAME, USER_DESCARTES_USERNAME);
    }

}
