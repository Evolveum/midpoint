/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import com.evolveum.midpoint.repo.api.RepoAddOptions;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.intest.util.StaticHookRecorder;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author Radovan Semancik
 */
@SuppressWarnings({ "FieldCanBeLocal", "unused" })
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestScriptHooks extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/scripthooks");

    private static final File RESOURCE_DUMMY_HOOK_FILE = new File(TEST_DIR, "resource-dummy-hook.xml");
    private static final String RESOURCE_DUMMY_HOOK_OID = "10000000-0000-0000-0000-000004444001";
    private static final String RESOURCE_DUMMY_HOOK_NAME = "hook";

    private static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");

    private static final File GENERIC_BLACK_PEARL_FILE = new File(TEST_DIR, "generic-blackpearl.xml");

    private static final File SYSTEM_CONFIGURATION_HOOKS_FILE = new File(TEST_DIR, "system-configuration-hooks.xml");
    private static final File SYSTEM_CONFIGURATION_PRIMARY_DELTA_HOOK_FILE = new File(TEST_DIR, "system-configuration-primary-delta-hook.xml");

    private DummyResource dummyResourceHook;
    private DummyResourceContoller dummyResourceCtlHook;
    private ResourceType resourceDummyHookType;
    private PrismObject<ResourceType> resourceDummyHook;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceCtlHook = DummyResourceContoller.create(
                RESOURCE_DUMMY_HOOK_NAME, resourceDummyHook);
        dummyResourceCtlHook.extendSchemaPirate();
        dummyResourceHook = dummyResourceCtlHook.getDummyResource();
        resourceDummyHook = importAndGetObjectFromFile(ResourceType.class,
                RESOURCE_DUMMY_HOOK_FILE, RESOURCE_DUMMY_HOOK_OID, initTask, initResult);
        resourceDummyHookType = resourceDummyHook.asObjectable();
        dummyResourceCtlHook.setResource(resourceDummyHook);

        importObjectFromFile(GENERIC_BLACK_PEARL_FILE);
        importObjectFromFile(ORG_TOP_FILE);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_HOOKS_FILE;
    }

    @Test
    public void test100JackAssignHookAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        dummyAuditService.clear();
        StaticHookRecorder.reset();

        // WHEN
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_HOOK_OID, null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Shadow (repo)", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow (model)", accountModel);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_HOOK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        displayValue("StaticHookRecorder", StaticHookRecorder.dump());
        StaticHookRecorder.assertInvocationCount("org", 1);
        StaticHookRecorder.assertInvocationCount("foo", 5);
        StaticHookRecorder.assertInvocationCount("bar", 5);
        StaticHookRecorder.assertInvocationCount("bar-user", 1);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test110JackAddOrganization() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        dummyAuditService.clear();
        StaticHookRecorder.reset();

        // WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATION, task, result, createPolyString("Pirate Brethren"));

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Shadow (repo)", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow (model)", accountModel);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_HOOK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        PrismObject<OrgType> brethrenOrg = searchObjectByName(OrgType.class, "Pirate Brethren", task, result);
        assertNotNull("Brethren org was not created", brethrenOrg);
        display("Brethren org", brethrenOrg);

        assertAssignedOrg(userJack, brethrenOrg);

        displayValue("StaticHookRecorder", StaticHookRecorder.dump());
        StaticHookRecorder.assertInvocationCount("org", 1);
        StaticHookRecorder.assertInvocationCount("foo", 10);
        StaticHookRecorder.assertInvocationCount("bar", 10);
        StaticHookRecorder.assertInvocationCount("bar-user", 1);
    }

    /**
     * MID-6122
     */
    @Test
    public void test200DeleteAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        XMLGregorianCalendar dayBeforeYesterday = XmlTypeConverter.fromNow("-P2D");
        XMLGregorianCalendar yesterday = XmlTypeConverter.fromNow("-P1D");

        PrismObject<ObjectType> newConfiguration = prismContext.parseObject(SYSTEM_CONFIGURATION_PRIMARY_DELTA_HOOK_FILE);
        repositoryService.addObject(newConfiguration, RepoAddOptions.createOverwrite(), result);

        UserType user = new UserType(prismContext)
                .name("test200")
                .beginAssignment()
                    .targetRef(ROLE_SUPERUSER_OID, RoleType.COMPLEX_TYPE)
                    .subtype("fragile")
                    .beginActivation()
                        .effectiveStatus(ActivationStatusType.ENABLED)
                        .validFrom(dayBeforeYesterday)
                        .validTo(yesterday)
                    .<AssignmentType>end()
                .end();
        repoAddObject(user.asPrismObject(), result);

        when();
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME).replace(PolyString.fromOrig("new-name"))
                .asObjectDelta(user.getOid());
        executeChanges(delta, null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);
        assertUser(user.getOid(), "after")
                .display()
                .assertAssignments(0)
                .assertFullName("new-name");
    }
}
