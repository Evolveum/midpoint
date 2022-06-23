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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.cases.impl.WorkItemManager;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineImpl;
import com.evolveum.midpoint.model.api.CaseService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.AbstractModelImplementationIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.CaseAsserter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractWfTest extends AbstractModelImplementationIntegrationTest {

    public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");

    protected static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
    protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
    protected static final String USER_JACK_USERNAME = "jack";

    protected static final File ROLE_APPROVER_FILE = new File(COMMON_DIR, "041-role-approver.xml");
    protected static final File ARCHETYPE_MANUAL_PROVISIONING_CASE_FILE = new File(COMMON_DIR, "023-archetype-manual-provisioning-case.xml");
    protected static final File ARCHETYPE_OPERATION_REQUEST_FILE = new File(COMMON_DIR, "024-archetype-operation-request.xml");
    protected static final File ARCHETYPE_APPROVAL_CASE_FILE = new File(COMMON_DIR, "025-archetype-approval-case.xml");

    protected static final String USER_ADMINISTRATOR_OID = SystemObjectsType.USER_ADMINISTRATOR.value();

    protected String userJackOid;

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
        modelService.postInit(initResult);

        PrismObject<SystemConfigurationType> sysconfig = prismContext.parseObject(getSystemConfigurationFile());
        updateSystemConfiguration(sysconfig.asObjectable());
        repoAddObject(sysconfig, initResult);

        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        login(userAdministrator);

        repoAddObjectFromFile(ROLE_APPROVER_FILE, initResult).getOid();
        repoAddObjectFromFile(ARCHETYPE_MANUAL_PROVISIONING_CASE_FILE, initResult).getOid();
        repoAddObjectFromFile(ARCHETYPE_OPERATION_REQUEST_FILE, initResult).getOid();
        repoAddObjectFromFile(ARCHETYPE_APPROVAL_CASE_FILE, initResult).getOid();

        userJackOid = repoAddObjectFromFile(USER_JACK_FILE, initResult).getOid();

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

    protected void checkWorkItemAuditRecords(Map<String, WorkflowResult> expectedResults) {
        WfTestUtil.checkWorkItemAuditRecords(expectedResults, dummyAuditService);
    }

    protected void checkWfProcessAuditRecords(Map<String, WorkflowResult> expectedResults) {
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
        SearchResultList<CaseWorkItemType> itemsAll = modelService.searchContainers(CaseWorkItemType.class, getOpenItemsQuery(), null, task, result);
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
        return modelService.searchContainers(CaseWorkItemType.class, getOpenItemsQuery(), null, task, result);
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

    protected void assertObjectInTask(Task task, String oid) {
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
//        removeOldValues(expectedDelta);
//        removeOldValues(realDelta);
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
        S_FilterExit q = QueryUtils
                .filterForAssignees(prismContext.queryFor(CaseWorkItemType.class), SecurityUtil.getPrincipal(),
                        OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, relationRegistry);
        q = q.and().item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull();
        List<CaseWorkItemType> currentWorkItems = modelService.searchContainers(CaseWorkItemType.class, q.build(), null, task, result);
        long found = currentWorkItems.stream().filter(wi -> expectedWorkItem == null || expectedWorkItem.matches(wi)).count();
        assertEquals("Wrong # of matching work items", count, found);
    }

    protected ObjectQuery getOpenItemsQuery() {
        return prismContext.queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull()
                .build();
    }

    protected void approveWorkItem(CaseWorkItemType workItem, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, PolicyViolationException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                new AbstractWorkItemOutputType(prismContext).outcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE),
                task, result);
    }

    protected void rejectWorkItem(CaseWorkItemType workItem, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, PolicyViolationException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                new AbstractWorkItemOutputType(prismContext).outcome(SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT),
                task, result);
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
        String caseOid = OperationResult.referenceToCaseOid(result.findAsynchronousOperationReference());
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
}
