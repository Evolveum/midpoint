/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import jakarta.jms.JMSException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.FilterUtil;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.messaging.JsonAsyncProvisioningRequest;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.PendingOperationsAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Tests basic asynchronous provisioning functionality:
 * - addObject
 * - modifyObject
 * - deleteObject
 *
 * Subclasses uses various options like:
 * - target: mock or JMS
 * - options: confirmations (yes/no), full data (or deltas), qualified JSON (yes/no)
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class TestAsyncProvisioning extends AbstractProvisioningIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/async/provisioning/");

    private static final String ASYNC_PROVISIONING_CONNECTOR = "AsyncProvisioningConnector";

    private static final ItemName ATTR_DRINK = new ItemName(NS_RI, "drink");
    private static final ItemPath ATTR_DRINK_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_DRINK);

    private static final ItemName ATTR_SHOE_SIZE = new ItemName(NS_RI, "shoeSize");
    private static final ItemPath ATTR_SHOE_SIZE_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_SHOE_SIZE);

    protected PrismObject<ResourceType> resource;

    private String jackAccountOid;

    protected TestObject<TaskType> propagationTask;

    protected boolean isUsingConfirmations() {
        return false;
    }

    protected boolean isQualified() {
        return false;
    }

    protected boolean isFullData() {
        return false;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        InternalsConfig.setSanityChecks(true);
        TestObject<ResourceType> testObject = TestObject.file(getResourceFile());
        PrismObject<ResourceType> resource = testObject.get().clone();
        customizeResource(resource);

        this.resource = addResource(TestObject.of(resource.asObjectable()), List.of(ASYNC_PROVISIONING_CONNECTOR), false, initResult);

        propagationTask = getPropagationTask();
        if (propagationTask != null) {
            taskManager.addTask(propagationTask.get(), getTestOperationResult());

            assertNewPropagationTask();
        }
    }

    protected void customizeResource(PrismObject<ResourceType> resource) {

    }

    protected abstract File getResourceFile();

    protected TestObject<TaskType> getPropagationTask() {
        return null;
    }

    protected String getClockForwardDurationForPropagation() {
        return null;
    }

    protected boolean hasPropagationConfigured() {
        return propagationTask != null;
    }

    protected void assertNewPropagationTask() throws Exception {
        OperationResult result = createOperationResult("assertNewPropagationTask");
        PrismObject<TaskType> propTask = repositoryService.getObject(TaskType.class, propagationTask.oid, null, result);
        display("Propagation task (new)", propTask);
        SearchFilterType filterType = propTask.asObjectable()
                .getActivity()
                .getWork()
                .getMultiPropagation()
                .getResources()
                .getQuery()
                .getFilter();
        displayDumpable("Propagation task filter", filterType);
        assertFalse("Empty filter in propagation task", FilterUtil.isFilterEmpty(filterType));
    }

    protected void assertFinishedPropagationTask(Task finishedTask, OperationResultStatusType expectedStatus) {
        display("Finished propagation task", finishedTask);
        OperationResultStatusType resultStatus = finishedTask.getResultStatus();
        if (expectedStatus == null) {
            if (resultStatus != OperationResultStatusType.SUCCESS && resultStatus != OperationResultStatusType.IN_PROGRESS) {
                fail("Unexpected propagation task result " + resultStatus);
            }
        } else {
            assertEquals("Unexpected propagation task result", expectedStatus, resultStatus);
        }
    }

    protected void runPropagation() throws Exception {
        if (propagationTask == null) {
            return;
        }

        clockForward(getClockForwardDurationForPropagation());

        Task finishedTask = rerunTask(propagationTask.oid);
        assertFinishedPropagationTask(finishedTask, OperationResultStatusType.SUCCESS);
    }

    @Test
    public void test000Sanity() throws Exception {
        testResource();
        testSanityExtra();
    }

    private void testResource() throws Exception {
        given();
        Task task = getTestTask();

        when();
        OperationResult testResult = provisioningService.testResource(resource.getOid(), task, task.getResult());

        then();
        assertSuccess(testResult);
    }

    protected void testSanityExtra() throws Exception {
    }

    private void runPropagationAndAssertShadow(
            String shadowOid, Consumer<PendingOperationsAsserter<Void>> asserterConsumer) throws Exception {

        when("Propagation runs");

        runPropagation();

        then();

        PendingOperationsAsserter<Void> asserter = assertRepoShadow(shadowOid)
                .pendingOperations();

        asserterConsumer.accept(asserter);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test100AddAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> jack = createShadow(resource, "jack");
        addAttributeValue(resource, jack, ATTR_DRINK, "rum");

        when();
        jackAccountOid = provisioningService.addObject(jack, null, null, task, result);

        if (!hasPropagationConfigured()) {
            assertSuccessOrInProgress(result);
        } else {
            runPropagationAndAssertShadow(jackAccountOid, asserter -> {
                asserter.addOperation()
                        .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                        .assertResultStatus(OperationResultStatusType.SUCCESS);
            });
        }

        dumpRequests();
        String req = getRequest();
        JsonAsyncProvisioningRequest jsonRequest = JsonAsyncProvisioningRequest.from(req);
        assertThat(jsonRequest.getOperation()).isEqualTo("add");
        assertThat(jsonRequest.getObjectClass()).isEqualTo(getAccountObjectClassName());
        assertThat(jsonRequest.getAttributes()).containsOnlyKeys(icfsUid(), icfsName(), riDrink());
        assertThat((Collection<Object>) jsonRequest.getAttributes().get(icfsUid())).containsExactly("jack");
        assertThat((Collection<Object>) jsonRequest.getAttributes().get(icfsName())).containsExactly("jack");
        assertThat((Collection<Object>) jsonRequest.getAttributes().get(riDrink())).containsExactly("rum");

        assertRepoShadow(jackAccountOid);
        assertShadowFuture(jackAccountOid)
                .attributes()
                .assertValue(ICFS_NAME, "jack")
                .assertValue(ICFS_UID, "jack")
                .assertValue(ATTR_DRINK, "rum");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test110ModifyAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clearRequests();

        List<ItemDelta<?, ?>> modifications = deltaFor(ShadowType.class)
                .item(ATTR_DRINK_PATH, getAttributeDefinition(resource, ATTR_DRINK)).add("water")
                .item(ATTR_SHOE_SIZE_PATH, getAttributeDefinition(resource, ATTR_SHOE_SIZE)).add(42)
                .asItemDeltas();

        when();
        provisioningService.modifyObject(ShadowType.class, jackAccountOid, modifications, null, null, task, result);

        if (!hasPropagationConfigured()) {
            assertSuccessOrInProgress(result);
        } else {
            runPropagationAndAssertShadow(jackAccountOid, asserter -> {
                asserter.modifyOperation()
                        .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                        .assertResultStatus(OperationResultStatusType.SUCCESS);
            });
        }

        dumpRequests();
        String req = getRequest();
        JsonAsyncProvisioningRequest jsonRequest = JsonAsyncProvisioningRequest.from(req);
        assertThat(jsonRequest.getOperation()).isEqualTo("modify");
        assertThat(jsonRequest.getObjectClass()).isEqualTo(getAccountObjectClassName());
        assertThat(jsonRequest.getAttributes()).isNull();
        assertThat(jsonRequest.getPrimaryIdentifiers()).containsOnlyKeys(icfsUid());
        assertThat((Collection<Object>) jsonRequest.getPrimaryIdentifiers().get(icfsUid())).containsExactly("jack");
        assertThat(jsonRequest.getSecondaryIdentifiers()).containsOnlyKeys(icfsName());
        assertThat((Collection<Object>) jsonRequest.getSecondaryIdentifiers().get(icfsName())).containsExactly("jack");
        if (isFullData()) {
            assertThat(jsonRequest.getChanges()).containsOnlyKeys(icfsUid(), icfsName(), riShoeSize(), riDrink());
            assertThat((Collection<Object>) jsonRequest.getChanges().get(icfsUid()).getReplace()).containsExactly("jack");
            assertThat((Collection<Object>) jsonRequest.getChanges().get(icfsName()).getReplace()).containsExactly("jack");
            assertThat((Collection<Object>) jsonRequest.getChanges().get(riShoeSize()).getReplace()).containsExactly(42);
            assertThat((Collection<Object>) jsonRequest.getChanges().get(riDrink()).getReplace()).containsExactlyInAnyOrder("water", "rum");
        } else {
            assertThat(jsonRequest.getChanges()).containsOnlyKeys(riShoeSize(), riDrink());
            assertThat((Collection<Object>) jsonRequest.getChanges().get(riShoeSize()).getAdd()).containsExactly(42);
            assertThat((Collection<Object>) jsonRequest.getChanges().get(riDrink()).getAdd()).containsExactly("water");
        }

        assertRepoShadow(jackAccountOid)
                .assertHasMetadataCreateTimestamp()
                .assertHasMetadataModifyTimestamp();
        assertShadowFuture(jackAccountOid)
                .attributes()
                .assertValue(ICFS_NAME, "jack")
                .assertValue(ICFS_UID, "jack")
                .assertValue(ATTR_DRINK, "rum", "water")
                .assertValue(ATTR_SHOE_SIZE, 42);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test120ModifyAccountAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clearRequests();

        List<ItemDelta<?, ?>> modifications = deltaFor(ShadowType.class)
                .item(ATTR_DRINK_PATH, getAttributeDefinition(resource, ATTR_DRINK)).delete("water")
                .item(ATTR_SHOE_SIZE_PATH, getAttributeDefinition(resource, ATTR_SHOE_SIZE)).replace(44)
                .asItemDeltas();

        when();
        provisioningService.modifyObject(ShadowType.class, jackAccountOid, modifications, null, null, task, result);

        if (!hasPropagationConfigured()) {
            assertSuccessOrInProgress(result);
        } else {
            runPropagationAndAssertShadow(jackAccountOid, asserter -> {
                asserter.assertNoUnfinishedOperations();
            });
        }

        dumpRequests();
        String req = getRequest();
        JsonAsyncProvisioningRequest jsonRequest = JsonAsyncProvisioningRequest.from(req);
        assertThat(jsonRequest.getOperation()).isEqualTo("modify");
        assertThat(jsonRequest.getObjectClass()).isEqualTo(getAccountObjectClassName());
        assertThat(jsonRequest.getAttributes()).isNull();
        assertThat(jsonRequest.getPrimaryIdentifiers()).containsOnlyKeys(icfsUid());
        assertThat((Collection<Object>) jsonRequest.getPrimaryIdentifiers().get(icfsUid())).containsExactly("jack");
        assertThat(jsonRequest.getSecondaryIdentifiers()).containsOnlyKeys(icfsName());
        assertThat((Collection<Object>) jsonRequest.getSecondaryIdentifiers().get(icfsName())).containsExactly("jack");
        if (isFullData()) {
            assertThat(jsonRequest.getChanges()).containsOnlyKeys(icfsUid(), icfsName(), riShoeSize(), riDrink());
            assertThat((Collection<Object>) jsonRequest.getChanges().get(icfsUid()).getReplace()).containsExactly("jack");
            assertThat((Collection<Object>) jsonRequest.getChanges().get(icfsName()).getReplace()).containsExactly("jack");
            assertThat((Collection<Object>) jsonRequest.getChanges().get(riShoeSize()).getReplace()).containsExactly(44);
            assertThat((Collection<Object>) jsonRequest.getChanges().get(riDrink()).getReplace()).containsExactlyInAnyOrder("rum");
        } else {
            assertThat(jsonRequest.getChanges()).containsOnlyKeys(riShoeSize(), riDrink());
            assertThat((Collection<Object>) jsonRequest.getChanges().get(riShoeSize()).getReplace()).containsExactly(44);
            assertThat((Collection<Object>) jsonRequest.getChanges().get(riDrink()).getDelete()).containsExactly("water");
        }

        assertRepoShadow(jackAccountOid);
        assertShadowFuture(jackAccountOid)
                .attributes()
                .assertValue(ICFS_NAME, "jack")
                .assertValue(ICFS_UID, "jack")
                .assertValue(ATTR_DRINK, "rum")
                .assertValue(ATTR_SHOE_SIZE, 44);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test130DeleteAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clearRequests();

        when();
        provisioningService.deleteObject(ShadowType.class, jackAccountOid, null, null, task, result);

        if (!hasPropagationConfigured()) {
            assertSuccessOrInProgress(result);
        } else {
            runPropagationAndAssertShadow(jackAccountOid, asserter -> {
                asserter.deleteOperation()
                        .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                        .assertResultStatus(OperationResultStatusType.SUCCESS);
            });
        }

        dumpRequests();
        String req = getRequest();
        JsonAsyncProvisioningRequest jsonRequest = JsonAsyncProvisioningRequest.from(req);
        assertThat(jsonRequest.getOperation()).isEqualTo("delete");
        assertThat(jsonRequest.getObjectClass()).isEqualTo(getAccountObjectClassName());
        assertThat(jsonRequest.getAttributes()).isNull();
        assertThat(jsonRequest.getPrimaryIdentifiers()).containsOnlyKeys(icfsUid());
        assertThat((Collection<Object>) jsonRequest.getPrimaryIdentifiers().get(icfsUid())).containsExactly("jack");
        assertThat(jsonRequest.getSecondaryIdentifiers()).containsOnlyKeys(icfsName());
        assertThat((Collection<Object>) jsonRequest.getSecondaryIdentifiers().get(icfsName())).containsExactly("jack");

        if (isUsingConfirmations()) {
            assertRepoShadow(jackAccountOid);
            assertShadowFuture(jackAccountOid);
        } else {
            if (hasPropagationConfigured()) {
                assertRepoShadow(jackAccountOid)
                        .assertIsDead(true);
            } else {
                assertNoRepoShadow(jackAccountOid);
            }
        }
    }

    protected void assertSuccessOrInProgress(OperationResult result) {
        if (isUsingConfirmations() || hasPropagationConfigured()) {
            assertInProgress(result);
        } else {
            assertSuccess(result);
        }
    }

    protected String getAccountObjectClassName() {
        return qNameAsString(RI_ACCOUNT_OBJECT_CLASS);
    }

    private String riDrink() {
        return qNameAsString(ATTR_DRINK);
    }

    private String riShoeSize() {
        return qNameAsString(ATTR_SHOE_SIZE);
    }

    protected String icfsName() {
        return qNameAsString(ICFS_NAME);
    }

    protected String icfsUid() {
        return qNameAsString(ICFS_UID);
    }

    private String qNameAsString(QName qName) {
        if (isQualified()) {
            return qName.getNamespaceURI() + "#" + qName.getLocalPart();
        } else {
            return qName.getLocalPart();
        }
    }

    protected abstract String getRequest() throws JMSException, IOException;

    protected abstract void dumpRequests() throws JMSException;

    protected abstract void clearRequests() throws JMSException;
}
