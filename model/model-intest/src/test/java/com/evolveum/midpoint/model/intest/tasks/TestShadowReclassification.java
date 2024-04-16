/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.util.SynchronizationRequest;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.*;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;


@SuppressWarnings("SpellCheckingInspection")
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestShadowReclassification extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/sync");

    private static final String ACCOUNT_STAN_NAME = "stan";
    private static final String ACCOUNT_RUM_NAME = "rum";
    private static final String ACCOUNT_MURRAY_NAME = "murray";

    private static final String RESOURCE_DUMMY_RECLASSIFICATION_FILE = "resource-dummy-reclassification.xml";
    private static final String RESOURCE_DUMMY_RECLASSIFICATION_OID = "10000000-0000-0000-0000-00a00000a204";
    private static final String RESOURCE_DUMMY_RECLASSIFICATION_NAME = "reclassification";


    private DummyTestResource resourceReclassification;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        resourceReclassification = new DummyTestResource(
                TEST_DIR,
                RESOURCE_DUMMY_RECLASSIFICATION_FILE,
                RESOURCE_DUMMY_RECLASSIFICATION_OID,
                RESOURCE_DUMMY_RECLASSIFICATION_NAME,
                controller -> {
                    controller.addAttrDef(
                            controller.getAccountObjectClass(),
                            DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                            String.class,
                            true,
                            false);
                });
        resourceReclassification.initAndTest(this, initTask, initResult);

        resourceReclassification.addAccount(ACCOUNT_STAN_NAME, "Stan Fai");
        resourceReclassification.addAccount(ACCOUNT_RUM_NAME, "Rum Rogers");
        resourceReclassification.addAccount(ACCOUNT_MURRAY_NAME, "Murray");
    }

    @Test
    public void test100ImportAccountsFromResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Preconditions
        assertUsers(getNumberOfUsers());
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        loginAdministrator();

        // WHEN
        when();
        modelService.importFromResource(
                resourceReclassification.oid,
                resourceReclassification.controller.getAccountObjectClassQName(),
                task,
                result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        loginAdministrator();

        waitForTaskFinish(task, 40000);

        TestUtil.assertSuccess(task.getResult());

        display(result);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertUsers(getNumberOfUsers() + 3);
    }

    @Test
    public void test110Reclassification() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        and("shadow '" + ACCOUNT_STAN_NAME + "', '" + ACCOUNT_RUM_NAME + "' and '" + ACCOUNT_MURRAY_NAME + "' is manually misclassified");
        changeIntentToFake(ACCOUNT_STAN_NAME);
        changeIntentToFake(ACCOUNT_RUM_NAME);
        changeIntentToFake(ACCOUNT_MURRAY_NAME);

        assertFaketShadowKindIntent(ACCOUNT_STAN_NAME);
        assertFaketShadowKindIntent(ACCOUNT_RUM_NAME);
        assertFaketShadowKindIntent(ACCOUNT_MURRAY_NAME);

        when("running the shadow reclassification (production)");
        var taskOid = shadowReclassificationDummyResourceRequest()
                .withTaskExecutionMode(TaskExecutionMode.PRODUCTION)
                .execute(result);
        waitForRootActivityCompletion(taskOid, DEFAULT_SHORT_TASK_WAIT_TIMEOUT);

        then("the task is OK");
        assertTask(taskOid, "shadow reclassification")
                .display();

        and("there is a classification change");
        assertDefaultShadowKindIntent(ACCOUNT_STAN_NAME);
        assertDefaultShadowKindIntent(ACCOUNT_RUM_NAME);
        assertDefaultShadowKindIntent(ACCOUNT_MURRAY_NAME);
    }

    @Test
    public void test120ReclassificationWrongTaskMode() throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        and("shadow '" + ACCOUNT_STAN_NAME + "' is manually misclassified");
        changeIntentToFake(ACCOUNT_STAN_NAME);

        when("running the shadow reclassification (production)");
        var taskOid = shadowReclassificationDummyResourceRequest()
                .withTaskExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION)
                .withNotAssertingSuccess()
                .execute(result);
        waitForRootActivityCompletion(taskOid, DEFAULT_SHORT_TASK_WAIT_TIMEOUT);

        then("the task is SUSPENDED and result contains FATAL ERROR");
        assertTaskExecutionState(taskOid, TaskExecutionStateType.SUSPENDED);
        assertTask(taskOid, "shadow reclassification")
                .display()
                .assertSuspended()
                .assertFatalError()
                .assertResultMessageContains("Execution mode PREVIEW is unsupported, "
                        + "please use FULL for full processing or SHADOW_MANAGEMENT_PREVIEW for simulation.");
    }

    private void assertFaketShadowKindIntent(String name) throws SchemaException, ObjectNotFoundException {
        assertShadowKindIntent(findShadow(name).getOid(), ShadowKindType.ACCOUNT, "fake");
    }

    private void assertDefaultShadowKindIntent(String name) throws SchemaException, ObjectNotFoundException {
        assertShadowKindIntent(findShadow(name).getOid(), ShadowKindType.ACCOUNT, "default");
    }

    private SynchronizationRequest.SynchronizationRequestBuilder shadowReclassificationDummyResourceRequest() {
        return shadowReclassificationRequest()
                .withResourceOid(resourceReclassification.oid)
                .withNamingAttribute(OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER)
                .withProcessingAllAccounts();
    }

    private void changeIntentToFake(String name) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        repositoryService.modifyObject(
                ShadowType.class,
                findShadow(name).getOid(),
                prismContext.deltaFor(ShadowType.class)
                        .item(ShadowType.F_INTENT).replace("fake")
                        .asItemDeltas(),
                getTestOperationResult());
    }

    private ShadowType findShadow(String name) throws SchemaException {
        return asObjectable(findShadowByPrismName(name, resourceReclassification.get(), getTestOperationResult()));
    }
}
