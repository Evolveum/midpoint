/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.cases.impl.WorkItemManager;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineImpl;
import com.evolveum.midpoint.model.api.CaseService;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.AbstractModelImplementationIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.*;
import com.evolveum.midpoint.test.asserter.CaseAsserter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractWfTest extends AbstractModelImplementationIntegrationTest {

    private static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");

    protected static final TestObject<UserType> USER_JACK =
            TestObject.file(COMMON_DIR, "user-jack.xml", "c0c010c0-d34d-b33f-f00d-111111111111");

    private static final File ROLE_APPROVER_FILE = new File(COMMON_DIR, "041-role-approver.xml");
    private static final File ARCHETYPE_MANUAL_PROVISIONING_CASE_FILE = new File(COMMON_DIR, "023-archetype-manual-provisioning-case.xml");
    private static final File ARCHETYPE_OPERATION_REQUEST_FILE = new File(COMMON_DIR, "024-archetype-operation-request.xml");
    private static final File ARCHETYPE_APPROVAL_CASE_FILE = new File(COMMON_DIR, "025-archetype-approval-case.xml");

    protected static final String USER_ADMINISTRATOR_OID = SystemObjectsType.USER_ADMINISTRATOR.value();

    protected static final String DUMMY_SIMPLE_WORKFLOW_NOTIFIER_PROCESSES = "dummy:simpleWorkflowNotifier-Processes";
    protected static final String DUMMY_SIMPLE_WORKFLOW_NOTIFIER_WORK_ITEMS = "dummy:simpleWorkflowNotifier-WorkItems";

    @Autowired protected Clockwork clockwork;
    @Autowired protected TaskManager taskManager;
    @Autowired protected CaseManager caseManager;
    @Autowired protected CaseEngineImpl caseEngine;
    @Autowired protected WorkItemManager workItemManager;
    @Autowired protected CaseService caseService;
    @Autowired protected PrimaryChangeProcessor primaryChangeProcessor;
    @Autowired protected SystemObjectCache systemObjectCache;
    @Autowired protected RelationRegistry relationRegistry;
    @Autowired protected WfTestHelper testHelper;
    @Autowired protected MiscHelper miscHelper;

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        PrismObject<SystemConfigurationType> sysconfig = prismContext.parseObject(getSystemConfigurationFile());
        updateSystemConfiguration(sysconfig.asObjectable());
        repoAddObject(sysconfig, initResult);
        modelService.postInit(initResult);

        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        login(userAdministrator);

        repoAddObjectFromFile(ROLE_APPROVER_FILE, initResult).getOid();
        repoAddObjectFromFile(ARCHETYPE_MANUAL_PROVISIONING_CASE_FILE, initResult).getOid();
        repoAddObjectFromFile(ARCHETYPE_OPERATION_REQUEST_FILE, initResult).getOid();
        repoAddObjectFromFile(ARCHETYPE_APPROVAL_CASE_FILE, initResult).getOid();

        USER_JACK.init(this, initTask, initResult);

        //setGlobalTracingOverride(createModelAndWorkflowLoggingTracingProfile());
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    protected void updateSystemConfiguration(SystemConfigurationType systemConfiguration) throws SchemaException, IOException {
        // nothing to do by default
    }

    protected abstract File getSystemConfigurationFile();

    protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult result) {
        Map<String, WorkflowResult> retval = new HashMap<>();
        retval.put(oid, result);
        return retval;
    }

    protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2,
            WorkflowResult approved2) {
        Map<String, WorkflowResult> retval = new HashMap<>();
        retval.put(oid, approved);
        retval.put(oid2, approved2);
        return retval;
    }

    protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2,
            WorkflowResult approved2, String oid3, WorkflowResult approved3) {
        Map<String, WorkflowResult> retval = new HashMap<>();
        retval.put(oid, approved);
        retval.put(oid2, approved2);
        retval.put(oid3, approved3);
        return retval;
    }

    protected void checkAuditRecords(Map<String, WorkflowResult> expectedResults) {
        checkWorkItemAuditRecords(expectedResults);
        checkWfProcessAuditRecords(expectedResults);
    }

    private void checkWorkItemAuditRecords(Map<String, WorkflowResult> expectedResults) {
        WfTestUtil.checkWorkItemAuditRecords(expectedResults, dummyAuditService);
    }

    private void checkWfProcessAuditRecords(Map<String, WorkflowResult> expectedResults) {
        WfTestUtil.checkWfProcessAuditRecords(expectedResults, dummyAuditService);
    }

    protected void removeAllAssignments(String oid, OperationResult result) throws Exception {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
        for (AssignmentType at : user.asObjectable().getAssignment()) {
            ObjectDelta<?> delta = prismContext.deltaFactory().object()
                    .createModificationDeleteContainer(UserType.class, oid, UserType.F_ASSIGNMENT,
                            at.asPrismContainerValue().clone());
            repositoryService.modifyObject(UserType.class, oid, delta.getModifications(), result);
            display("Removed assignment " + at + " from " + user);
        }
    }

    protected CaseWorkItemType getWorkItem(Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        SearchResultList<CaseWorkItemType> itemsAll = modelService.searchContainers(CaseWorkItemType.class, ObjectQueryUtil.openItemsQuery(), null, task, result);
        if (itemsAll.size() != 1) {
            System.out.println("Unexpected # of work items: " + itemsAll.size());
            for (CaseWorkItemType workItem : itemsAll) {
                System.out.println(PrismUtil.serializeQuietly(prismContext, workItem));
            }
        }
        assertEquals("Wrong # of total work items", 1, itemsAll.size());
        return itemsAll.get(0);
    }

    protected SearchResultList<CaseWorkItemType> getWorkItems(Task task, OperationResult result) throws Exception {
        return modelService.searchContainers(CaseWorkItemType.class, ObjectQueryUtil.openItemsQuery(), null, task, result);
    }

    protected void displayWorkItems(String title, List<CaseWorkItemType> workItems) {
        workItems.forEach(wi -> display(title, wi));
    }

    protected ObjectReferenceType ort(String oid) {
        return ObjectTypeUtil.createObjectRef(oid, ObjectTypes.USER);
    }

    protected PrismReferenceValue prv(String oid) {
        return ObjectTypeUtil.createObjectRef(oid, ObjectTypes.USER).asReferenceValue();
    }

    protected PrismReference ref(List<ObjectReferenceType> orts) {
        PrismReference rv = prismContext.itemFactory().createReference(new QName("dummy"));
        orts.forEach(ort -> {
            try {
                rv.add(ort.asReferenceValue().clone());
            } catch (SchemaException e) {
                throw new IllegalStateException(e);
            }
        });
        return rv;
    }

    protected PrismReference ref(ObjectReferenceType ort) {
        return ref(Collections.singletonList(ort));
    }

    protected void assertObjectInTaskTree(Task rootTask, String oid, boolean checkObjectOnSubtasks, OperationResult result)
            throws SchemaException {
        assertObjectInTask(rootTask, oid);
        if (checkObjectOnSubtasks) {
            for (Task task : rootTask.listSubtasks(result)) {
                assertObjectInTask(task, oid);
            }
        }
    }

    private void assertObjectInTask(Task task, String oid) {
        assertEquals("Missing or wrong object OID in task " + task, oid, task.getObjectOid());
    }

    protected void waitForTaskClose(final Task task, final int timeout) throws Exception {
        final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class + ".waitForTaskClose");
        Checker checker = new Checker() {
            @Override
            public boolean check() throws CommonException {
                task.refresh(waitResult);
                OperationResult result = task.getResult();
                if (verbose) {display("Check result", result);}
                return task.isClosed();
            }

            @Override
            public void timeout() {
                try {
                    task.refresh(waitResult);
                } catch (Throwable e) {
                    displayException("Exception during task refresh", e);
                }
                OperationResult result = task.getResult();
                display("Result of timed-out task", result);
                assert false : "Timeout (" + timeout + ") while waiting for " + task + " to finish. Last result " + result;
            }
        };
        IntegrationTestTools.waitFor("Waiting for " + task + " finish", checker, timeout, 1000);
    }

    protected String getTargetOid(CaseWorkItemType caseWorkItem) {
        ObjectReferenceType targetRef = CaseTypeUtil.getCaseRequired(caseWorkItem).getTargetRef();
        assertNotNull("targetRef not found", targetRef);
        String roleOid = targetRef.getOid();
        assertNotNull("requested role OID not found", roleOid);
        return roleOid;
    }

    protected void checkTargetOid(CaseWorkItemType caseWorkItem, String expectedOid) {
        String realOid = getTargetOid(caseWorkItem);
        assertEquals("Unexpected target OID", expectedOid, realOid);
    }

    protected void assertDeltasEqual(String message, ObjectDelta expectedDelta, ObjectDelta realDelta) {
        if (!expectedDelta.equivalent(realDelta)) {
            fail(message + "\nExpected:\n" + expectedDelta.debugDump() + "\nReal:\n" + realDelta.debugDump());
        }
    }

    protected void assertNoObject(ObjectType object)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertNull("Object was created but it shouldn't be",
                searchObjectByName(object.getClass(), object.getName().getOrig()));
    }

    protected void assertNoObject(PrismObject<? extends ObjectType> object)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertNoObject(object.asObjectable());
    }

    protected <T extends ObjectType> void assertObject(T object)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<T> objectFromRepo = searchObjectByName((Class<T>) object.getClass(), object.getName().getOrig());
        assertNotNull("Object " + object + " was not created", objectFromRepo);
        objectFromRepo.removeItem(ObjectType.F_METADATA, Item.class);
        objectFromRepo.removeItem(ObjectType.F_OPERATION_EXECUTION, Item.class);
        if (!object.equals(objectFromRepo.asObjectable())) {
            System.out.println("Expected:\n" + prismContext.xmlSerializer().serialize(object.asPrismObject()));
            System.out.println("Actual:\n" + prismContext.xmlSerializer().serialize(objectFromRepo));
        }
        assertEquals("Object is different from the one that was expected", object, objectFromRepo.asObjectable());
    }

    protected void checkVisibleWorkItem(ExpectedWorkItem expectedWorkItem, int count, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, CommunicationException {
        checkVisibleWorkItemInternal(true, expectedWorkItem, count, task, result);
        checkVisibleWorkItemInternal(false, expectedWorkItem, count, task, result);
    }

    private void checkVisibleWorkItemInternal(
            boolean explicitQuery, ExpectedWorkItem expectedWorkItem, int count, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, CommunicationException {
        S_FilterEntry builder;
        if (explicitQuery) {
            // This is the standard filter used e.g. by GUI
            builder = QueryUtils.filterForCaseAssignees(
                            prismContext.queryFor(CaseWorkItemType.class),
                            SecurityUtil.getPrincipal())
                    .and();
        } else {
            // This is "all" filter. Here we rely on authorizations to provide only relevant work items.
            builder = prismContext.queryFor(CaseWorkItemType.class);
        }
        ObjectQuery query = builder.item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull().build();
        List<CaseWorkItemType> currentWorkItems = modelService.searchContainers(CaseWorkItemType.class, query, null, task, result);
        long found = currentWorkItems.stream().filter(wi -> expectedWorkItem == null || expectedWorkItem.matches(wi)).count();
        assertEquals("Wrong # of matching work items", count, found);
    }

    protected void approveOrRejectWorkItem(CaseWorkItemType workItem, boolean decision, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        if (decision) {
            approveWorkItem(workItem, task, result);
        } else {
            rejectWorkItem(workItem, task, result);
        }
    }

    protected void approveWorkItem(CaseWorkItemType workItem, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, PolicyViolationException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                new AbstractWorkItemOutputType().outcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE),
                task, result);
    }

    /** Assumes single open work item. */
    protected void approveCase(CaseType aCase, Task task, OperationResult result) throws CommonException {
        approveWorkItem(
                getOpenWorkItemRequired(aCase),
                task, result);
    }

    protected void rejectWorkItem(CaseWorkItemType workItem, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, PolicyViolationException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                new AbstractWorkItemOutputType().outcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT),
                task, result);
    }

    public DummyAuditService getDummyAuditService() {
        return dummyAuditService;
    }

    public class RelatedCases {
        private CaseType approvalCase;
        private CaseType requestCase;

        public CaseType getApprovalCase() {
            return approvalCase;
        }

        public CaseType getRequestCase() {
            return requestCase;
        }

        public RelatedCases find(Task task, OperationResult result)
                throws SchemaException, SecurityViolationException, ConfigurationException,
                ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
            CaseWorkItemType workItem = getWorkItem(task, result);
            display("Work item", workItem);
            approvalCase = getCase(CaseTypeUtil.getCaseRequired(workItem).getOid());
            display("Approval case", approvalCase);
            assertHasArchetype(approvalCase.asPrismObject(), SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value());
            ObjectReferenceType parentRef = approvalCase.getParentRef();
            assertNotNull(parentRef);
            requestCase = modelObjectResolver.resolve(parentRef, CaseType.class, null, null, task, result);
            display("Request case", requestCase);
            assertNotNull(requestCase);
            assertHasArchetype(requestCase.asPrismObject(), SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value());
            return this;
        }
    }

    /**
     * Takes case OID from the operation result (via asynchronous identifier).
     */
    protected CaseAsserter<Void> assertCase(OperationResult result, String message)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String caseOid = result.findCaseOid();
        assertThat(caseOid).as("No background case OID").isNotNull();
        return assertCase(caseOid, message);
    }

    /**
     * Takes case from the work item (via parent reference).
     */
    protected CaseAsserter<Void> assertCase(CaseWorkItemType workItem, String message)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismContainerable<?> parent = workItem.asPrismContainerValue().getParent();
        assertThat(parent).isNotNull();
        //noinspection unchecked
        PrismContainerValue<?> grandParent = ((PrismContainer<CaseWorkItemType>) parent).getParent();
        assertThat(grandParent).isNotNull();
        String approvalCaseOid = ((PrismObjectValue<?>) grandParent).getOid();
        assertThat(approvalCaseOid).as("No parent case OID").isNotNull();
        return assertCase(approvalCaseOid, message);
    }

    protected ModelExecuteOptions getOptions(boolean immediate) {
        if (immediate) {
            return executeOptions().executeImmediatelyAfterApproval();
        } else {
            return null;
        }
    }
}
