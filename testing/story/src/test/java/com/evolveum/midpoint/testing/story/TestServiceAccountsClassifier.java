/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;

@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestServiceAccountsClassifier extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "service-accounts-classifier");

    protected static final File RESOURCE_DUMMY_CLASSIFIER_FILE = new File(TEST_DIR, "resource-dummy-classifier.xml");
    protected static final String RESOURCE_DUMMY_CLASSIFIER_OID = "1169ac14-8377-11e8-b404-5b5a1a8af0db";

    private static final File ROLE_EMPLOYEE_FILE = new File(TEST_DIR, "role-employee.xml");
    private static final String ROLE_EMPLOYEE_OID = "23d90f70-1924-419e-9beb-78a8bde6d261";

    private static final File SERVICE_JIRA_FILE = new File(TEST_DIR, "service-jira.xml");
    private static final String SERVICE_JIRA_OID = "c0c010c0-d34d-b33f-f00d-111111122222";

    private static final File SERVICE_GITHUB_FILE = new File(TEST_DIR, "service-github.xml");
    private static final String SERVICE_GITHUB_OID = "c0c010c0-d34d-b33f-f00d-111111133333";

    private static final File SHADOW_GITHUB_FILE = new File(TEST_DIR, "shadow-github.xml");
    private static final String SHADOW_GITHUB_OID = "c0c010c0-d34d-b33f-f00d-111111144444";

    private static final String ACCOUNT_DUMMY_JIRA_USERNAME = "jira";
    private static final String ACCOUNT_DUMMY_WIKI_USERNAME = "wiki";
    private static final String ACCOUNT_DUMMY_GITHUB_USERNAME = "github";

    private static final String ACCOUNT_DUMMY_WIKI_TEST_USERNAME = "test_wiki";

    private static final File TASK_RECONCILE_DUMMY_CLASSIFIER_FILE = new File(TEST_DIR, "task-dummy-classifier-reconcile.xml");
    private static final String TASK_RECONCILE_DUMMY_CLASSIFIER_OID = "10335c7c-838f-11e8-93a6-4b1dd0ab58e4";

    private static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/story/serviceAccountsClassifier/ext";
    private static final QName F_ACCOUNT_NAME = new QName(NS_EXT, "accountName");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        assertServices(0);

        importObjectFromFile(ROLE_EMPLOYEE_FILE, initResult);

        initDummyResourcePirate(null, RESOURCE_DUMMY_CLASSIFIER_FILE, RESOURCE_DUMMY_CLASSIFIER_OID, initTask, initResult);
        getDummyResource().setSyncStyle(DummySyncStyle.SMART);
    }

    @Test
    public void test001assignJackEmployeeRole() throws Exception {
        //WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_EMPLOYEE_OID);

        //THEN
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertAccount(userAfter, RESOURCE_DUMMY_CLASSIFIER_OID);

        //TODO: assert attributes?
    }

    @Test
    public void test100createServiceJira() throws Exception {
        //WHEN
        addObject(SERVICE_JIRA_FILE);

        //THEN
        then();
        PrismObject<ServiceType> service = getObject(ServiceType.class, SERVICE_JIRA_OID);
        display("Service magazine after", service);
        assertNotNull("No magazine service", service);

        assertNoLinkedAccount(service);
    }

    @Test
    public void test101jiraAssignResourceNoneEnforcement() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        //GIVEN

        //WHEN
        when();
        assignAccount(ServiceType.class, SERVICE_JIRA_OID, RESOURCE_DUMMY_CLASSIFIER_OID, "service");

        //THEN
        then();
        PrismObject<ServiceType> service = getObject(ServiceType.class, SERVICE_JIRA_OID);
        display("Service magazine after", service);
        assertNotNull("No magazine service", service);
        assertAssignedResource(ServiceType.class, SERVICE_JIRA_OID, RESOURCE_DUMMY_CLASSIFIER_OID, result);

        assertNoLinkedAccount(service);
    }

    @Test
    public void test150StartReconTask() throws Exception {
        assertUsers(getNumberOfUsers());
        assertServices(1);

        // WHEN
        when();

        importObjectFromFile(TASK_RECONCILE_DUMMY_CLASSIFIER_FILE);

        // THEN
        then();

        waitForTaskStart(TASK_RECONCILE_DUMMY_CLASSIFIER_OID);

        assertServices(1);
        assertUsers(getNumberOfUsers());
    }

    @Test
    public void test151LinkServiceAccountRecon() throws Exception {
        // Preconditions
        assertServices(1);

        DummyAccount account = new DummyAccount(ACCOUNT_DUMMY_JIRA_USERNAME);
        account.setEnabled(true);

        // WHEN
        when();

        getDummyResource().addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_RECONCILE_DUMMY_CLASSIFIER_OID);

        // THEN
        then();

        assertServices(1);
        PrismObject<ServiceType> serviceJiraAfter = getObject(ServiceType.class, SERVICE_JIRA_OID);
        display("Service magazine after", serviceJiraAfter);
        assertNotNull("No magazine service", serviceJiraAfter);
        PrismAsserts.assertPropertyValue(serviceJiraAfter,
                ItemPath.create(ServiceType.F_EXTENSION, F_ACCOUNT_NAME), ACCOUNT_DUMMY_JIRA_USERNAME);
        assertLiveLinks(serviceJiraAfter, 1);

    }

    @Test
    public void test152InactivateUnmatchedAccountRecon() throws Exception {
        // Preconditions
        assertServices(1);

        DummyAccount account = new DummyAccount(ACCOUNT_DUMMY_WIKI_USERNAME);
        account.setEnabled(true);

        // WHEN
        when();

        getDummyResource().addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_RECONCILE_DUMMY_CLASSIFIER_OID);

        // THEN
        then();

        assertServices(1);

        DummyAccount dummyAccount = getDummyAccount(getDummyResource().getInstanceName(), ACCOUNT_DUMMY_WIKI_USERNAME);
        assertFalse(dummyAccount.isEnabled(), "Dummy account should be disabled");
    }

    @Test
    public void test160AccountWikiTestDisputed() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        // Preconditions
        assertServices(1);

        DummyAccount account = new DummyAccount(ACCOUNT_DUMMY_WIKI_TEST_USERNAME);

        // WHEN
        when();

        getDummyResource().addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_RECONCILE_DUMMY_CLASSIFIER_OID);

        // THEN
        then();

        assertServices(1);

        PrismObject<ShadowType> shadow = findShadowByName(
                ShadowKindType.ACCOUNT, "default", ACCOUNT_DUMMY_WIKI_TEST_USERNAME, getDummyResourceObject(), result);
        assertShadow(shadow, "after ").assertSynchronizationSituation(SynchronizationSituationType.DISPUTED);
    }

    @Test
    public void test200createServiceGithub() throws Exception {
        //WHEN
        addObject(SERVICE_GITHUB_FILE);

        //THEN
        then();
        PrismObject<ServiceType> service = getObject(ServiceType.class, SERVICE_GITHUB_OID);
        display("Service github after", service);
        assertNotNull("No github service", service);

        assertNoLinkedAccount(service);
    }

    @Test
    public void test210createAccountGithub() throws Exception {
        //WHEN
        when();
        addObject(SHADOW_GITHUB_FILE);

        // THEN
        then();
        PrismObject<ShadowType> shadowGithub = getShadowModel(SHADOW_GITHUB_OID);

        display("Shadow github after", shadowGithub);
        assertNotNull("No magazine service", shadowGithub);
        ShadowAsserter.forShadow(shadowGithub).assertIntent("default");

        assertDummyAccount(getDummyResource().getInstanceName(), "github");
    }

    @Test
    public void test215githubAssignResourceNoneEnforcement() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        //GIVEN

        //WHEN
        when();
        assignAccount(ServiceType.class, SERVICE_GITHUB_OID, RESOURCE_DUMMY_CLASSIFIER_OID, "service");

        //THEN
        then();
        PrismObject<ServiceType> service = getObject(ServiceType.class, SERVICE_GITHUB_OID);
        display("Service github after", service);
        assertNotNull("No github service", service);
        assertAssignedResource(ServiceType.class, SERVICE_GITHUB_OID, RESOURCE_DUMMY_CLASSIFIER_OID, result);

        assertNoLinkedAccount(service);
    }

    @Test
    public void test220linkAccountGithubAndFixIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN
        PrismObject<ServiceType> serviceGithubBefore = getObject(ServiceType.class, SERVICE_GITHUB_OID);
        display("Service github before", serviceGithubBefore);
        assertNotNull("No github service", serviceGithubBefore);
        assertNoLinkedAccount(serviceGithubBefore);

        // WHEN
        when();
        waitForTaskNextRunAssertSuccess(TASK_RECONCILE_DUMMY_CLASSIFIER_OID);

        // THEN
        then();

        assertServices(2);
        PrismObject<ServiceType> serviceGithubAfter = getObject(ServiceType.class, SERVICE_GITHUB_OID);
        display("Service github after", serviceGithubAfter);
        assertNotNull("No github service", serviceGithubAfter);
        assertAssignedResource(ServiceType.class, SERVICE_GITHUB_OID, RESOURCE_DUMMY_CLASSIFIER_OID, result);
        PrismAsserts.assertPropertyValue(serviceGithubAfter, ItemPath.create(ServiceType.F_EXTENSION, F_ACCOUNT_NAME), ACCOUNT_DUMMY_GITHUB_USERNAME);
        assertLiveLinks(serviceGithubAfter, 1);

        //check if the intent was changed
        PrismObject<ShadowType> shadowGithub = getShadowModel(SHADOW_GITHUB_OID);
        display("Shadow github after", shadowGithub);
        assertNotNull("No magazine service", shadowGithub);
        ShadowAsserter.forShadow(shadowGithub).assertIntent("service");
    }

}
