/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import com.evolveum.midpoint.prism.query.FilterUtil;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * MID-4347
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualGroupingProposed extends TestSemiManualGrouping {

    private static final String USER_BIGMOUTH_NAME = "BIGMOUTH";
    private static final String USER_BIGMOUTH_FULLNAME = "Shouty Bigmouth";

    private String accountBigmouthOid;

    @Override
    protected File getResourceFile() {
        return RESOURCE_SEMI_MANUAL_GROUPING_PROPOSED_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // More resources. Not really used. They are here just to confuse propagation task.
        initDummyResourcePirate(RESOURCE_DUMMY_RED_NAME,
                RESOURCE_DUMMY_RED_FILE, RESOURCE_DUMMY_RED_OID, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_BLUE_NAME,
                RESOURCE_DUMMY_BLUE_FILE, RESOURCE_DUMMY_BLUE_OID, initTask, initResult);
    }

    @Test
    public void test020ResourcesSanity() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        SearchResultList<PrismObject<ResourceType>> resources = repositoryService.searchObjects(ResourceType.class, null, null, result);
        displayValue("Resources", resources.size() + ": " + resources);
        assertEquals("Unexpected number of resources", 3, resources.size());

        ObjectQuery query = prismContext.queryFor(ResourceType.class)
            .item("extension","provisioning").eq("propagated")
            .build();
        SearchResultList<PrismObject<ResourceType>> propagatedResources =
                repositoryService.searchObjects(ResourceType.class, query, null, result);
        displayValue("Propagated resources", propagatedResources.size() + ": " + propagatedResources);
        assertEquals("Unexpected number of propagated resources", 1, propagatedResources.size());
    }

    @Override
    protected void assertNewPropagationTask() throws Exception {
        OperationResult result = createOperationResult("assertNewPropagationTask");
        PrismObject<TaskType> propTask = repositoryService.getObject(TaskType.class, getPropagationTaskOid(), null, result);
        display("Propagation task (new)", propTask);
        SearchFilterType filterType = propTask.asObjectable().getObjectRef().getFilter();
        displayDumpable("Propagation task filter", filterType);
        assertFalse("Empty filter in propagation task",  FilterUtil.isFilterEmpty(filterType));
    }

    @Override
    protected void assertFinishedPropagationTask(Task finishedTask, OperationResultStatusType expectedStatus) {
        super.assertFinishedPropagationTask(finishedTask, expectedStatus);
        SearchFilterType filterType = finishedTask.getObjectRefOrClone().getFilter();
        displayDumpable("Propagation task filter", filterType);

        assertEquals("Unexpected propagation task progress", 1, finishedTask.getProgress());
    }

    /**
     * The resource has caseIgnore matching rule for username. Make sure everything
     * works well wih uppercase username.
     * MID-4393
     */
    @Test
    public void test500AssignBigmouthRoleOne() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_BIGMOUTH_NAME, USER_BIGMOUTH_FULLNAME, true);
        String userBigmouthOid = addObject(userBefore);
        display("User before", userBefore);

        // WHEN
        when();
        assignRole(userBigmouthOid, getRoleOneOid(), task, result);

        // THEN
        then();
        display("result", result);
        assertInProgress(result);

        PrismObject<UserType> userAfter = getUser(userBigmouthOid);
        display("User after", userAfter);
        accountBigmouthOid = getSingleLinkOid(userAfter);

        PendingOperationExecutionStatusType executionStage = PendingOperationExecutionStatusType.EXECUTION_PENDING;

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountBigmouthOid, null, result);
        display("Repo shadow", shadowRepo);
        PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, null, null, executionStage);
        assertNotNull("No ID in pending operation", pendingOperation.getId());
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
                RawType.fromPropertyRealValue(USER_BIGMOUTH_NAME.toLowerCase(), ATTR_USERNAME_QNAME, prismContext));
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
                RawType.fromPropertyRealValue(USER_BIGMOUTH_FULLNAME, ATTR_FULLNAME_QNAME, prismContext));
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertShadowExists(shadowRepo, false);
        assertNoShadowPassword(shadowRepo);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountBigmouthOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_BIGMOUTH_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_BIGMOUTH_NAME.toLowerCase());
        assertAttributeFromCache(shadowModel, ATTR_FULLNAME_QNAME, USER_BIGMOUTH_FULLNAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowModel, ActivationStatusType.ENABLED);
        assertShadowExists(shadowModel, false);
        assertNoShadowPassword(shadowModel);
        assertSinglePendingOperation(shadowModel, null, null, executionStage);
    }

    /**
     * MID-4393
     */
    @Test
    public void test502RunPropagation() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT20M");

        // WHEN
        when();
        runPropagation();

        // THEN
        then();
        assertSuccess(result);

        PendingOperationExecutionStatusType executionStage = PendingOperationExecutionStatusType.EXECUTING;

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountBigmouthOid, null, result);
        display("Repo shadow", shadowRepo);
        PendingOperationType pendingOperation = assertSinglePendingOperation(shadowRepo, null, null, executionStage);
        assertNotNull("No ID in pending operation", pendingOperation.getId());
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME,
                RawType.fromPropertyRealValue(USER_BIGMOUTH_NAME.toLowerCase(), ATTR_USERNAME_QNAME, prismContext));
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME,
                RawType.fromPropertyRealValue(USER_BIGMOUTH_FULLNAME, ATTR_FULLNAME_QNAME, prismContext));
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertShadowExists(shadowRepo, false);
        assertNoShadowPassword(shadowRepo);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountBigmouthOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_BIGMOUTH_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_BIGMOUTH_NAME.toLowerCase());
        assertAttributeFromCache(shadowModel, ATTR_FULLNAME_QNAME, USER_BIGMOUTH_FULLNAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowModel, ActivationStatusType.ENABLED);
        assertShadowExists(shadowModel, false);
        assertNoShadowPassword(shadowModel);
        PendingOperationType pendingOperationType = assertSinglePendingOperation(shadowModel, null, null, executionStage);

        String pendingOperationRef = pendingOperationType.getAsynchronousOperationReference();
        assertNotNull("No async reference in pending operation", pendingOperationRef);
        assertCaseState(pendingOperationRef, SchemaConstants.CASE_STATE_OPEN);
    }

    // Note: we have left bigmouth here half-created with open case. If should not do any harm.

}
