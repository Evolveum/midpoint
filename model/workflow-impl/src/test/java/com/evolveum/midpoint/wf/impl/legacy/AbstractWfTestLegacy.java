/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.legacy;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.WfTestHelper;
import com.evolveum.midpoint.wf.impl.WfTestUtil;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.wf.impl.WorkflowResult;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.wf.impl.processors.general.GeneralChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpGeneralHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.F_APPROVAL_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType.F_DELTAS_TO_APPROVE;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractWfTestLegacy extends AbstractInternalModelIntegrationTest {

    static final File TEST_RESOURCE_DIR = new File("src/test/resources/legacy");
    static final String DONT_CHECK = "dont-check";

    @Autowired protected Clockwork clockwork;
	@Autowired protected TaskManager taskManager;
    @Autowired protected WorkflowManager workflowManager;
    @Autowired protected WorkflowEngine workflowEngine;
    @Autowired protected MiscHelper miscHelper;
    @Autowired protected PrimaryChangeProcessor primaryChangeProcessor;
    @Autowired protected GeneralChangeProcessor generalChangeProcessor;
    @Autowired protected WfTestHelper testHelper;
    @Autowired protected PcpGeneralHelper pcpGeneralHelper;

    public static final File USERS_AND_ROLES_FILE = new File(TEST_RESOURCE_DIR, "users-and-roles.xml");

    public static final String ROLE_R1_OID = "00000001-d34d-b33f-f00d-000000000001";
    public static final String ROLE_R2_OID = "00000001-d34d-b33f-f00d-000000000002";
    public static final String ROLE_R3_OID = "00000001-d34d-b33f-f00d-000000000003";
    public static final String ROLE_R4_OID = "00000001-d34d-b33f-f00d-000000000004";
    public static final File USER_BILL_FILE = new File(TEST_RESOURCE_DIR, "user-bill.xml");
    public static final String USER_BILL_OID = "c0c010c0-d34d-b33f-f00d-11111111111a";
    public static final String ROLE_R10_OID = "00000001-d34d-b33f-f00d-000000000010";
    public static final String ROLE_R10_SKIP_OID = "00000001-d34d-b33f-f00d-000000000S10";

    public static final File ROLE_R11_FILE = new File(TEST_RESOURCE_DIR, "role11.xml");
    public static final String ROLE_R11_OID = "00000001-d34d-b33f-f00d-000000000011";
    public static final File ROLE_R12_FILE = new File(TEST_RESOURCE_DIR, "role12.xml");
    public static final String ROLE_R12_OID = "00000001-d34d-b33f-f00d-000000000012";
    public static final File ROLE_R13_FILE = new File(TEST_RESOURCE_DIR, "role13.xml");
    public static final String ROLE_R13_OID = "00000001-d34d-b33f-f00d-000000000013";

    public static final String R1BOSS_OID = "00000000-d34d-b33f-f00d-111111111111";
    public static final String R2BOSS_OID = "00000000-d34d-b33f-f00d-111111111112";
    public static final String R3BOSS_OID = "00000000-d34d-b33f-f00d-111111111113";
    public static final String DUMMYBOSS_OID = "00000000-d34d-b33f-f00d-111111111333";

    public static final String GROUP_TESTERS_OID = "20000000-0000-0000-3333-000000000002";
    public static final File GROUP_TESTERS_FILE = new File(TEST_RESOURCE_DIR, "group-testers-dummy.xml");
    public static final String GROUP_TESTERS_NAME = "testers";

    public static final String GROUP_GUESTS_OID = "20000000-0000-0000-3333-000000000072";
    public static final File GROUP_GUESTS_FILE = new File(TEST_RESOURCE_DIR, "/group-guests-dummy.xml");
    public static final String GROUP_GUESTS_NAME = "guests";

    public static final File USER_ELISABETH_FILE = new File(TEST_RESOURCE_DIR, "user-elisabeth.xml");
    public static final String USER_ELISABETH_OID = "c0c010c0-d34d-b33f-f00d-111111112222";

    public static final File ACCOUNT_SHADOW_ELISABETH_DUMMY_FILE = new File(TEST_RESOURCE_DIR, "account-shadow-elisabeth-dummy.xml");

    public AbstractWfTestLegacy() {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

		super.initSystem(initTask, initResult);
        importObjectFromFile(USERS_AND_ROLES_FILE, initResult);
        importObjectFromFile(ROLE_R11_FILE, initResult);
        importObjectFromFile(ROLE_R12_FILE, initResult);
        importObjectFromFile(ROLE_R13_FILE, initResult);

        // add dummyboss as approver for Dummy Resource
        ResourceBusinessConfigurationType businessConfigurationType = new ResourceBusinessConfigurationType(prismContext);
        ObjectReferenceType dummyApproverRef = new ObjectReferenceType();
        dummyApproverRef.setType(UserType.COMPLEX_TYPE);
        dummyApproverRef.setOid(DUMMYBOSS_OID);
        businessConfigurationType.getApproverRef().add(dummyApproverRef);
        ObjectDelta objectDelta = prismContext.deltaFactory().object()
		        .createModificationAddContainer(ResourceType.class, RESOURCE_DUMMY_OID, ResourceType.F_BUSINESS,
                        businessConfigurationType);
        repositoryService.modifyObject(ResourceType.class, RESOURCE_DUMMY_OID, objectDelta.getModifications(), initResult);

        // check Role2 approver OID (it is filled-in using search filter)
        List<PrismObject<RoleType>> roles = findRoleInRepoUnchecked("Role2", initResult);
        assertEquals("Wrong number of Role2 objects found in repo", 1, roles.size());
        RoleType role2 = roles.get(0).asObjectable();

//        could be done also like this
//        RoleType role2 = repositoryService.getObject(RoleType.class, ROLE_R2_OID, null, initResult).asObjectable();

        ObjectReferenceType approver = role2.getApprovalSchema().getStage().get(0).getApproverRef().get(0);
        assertEquals("Wrong OID of Role2's approver", R2BOSS_OID, approver.getOid());

        importObjectFromFile(GROUP_TESTERS_FILE, initResult);
        importObjectFromFile(GROUP_GUESTS_FILE, initResult);

        getDummyResourceController().addGroup(GROUP_TESTERS_NAME);
        getDummyResourceController().addGroup(GROUP_GUESTS_NAME);

        systemObjectCache.invalidateCaches();
    }

    protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult result) {
        Map<String,WorkflowResult> retval = new HashMap<>();
        retval.put(oid, result);
        return retval;
    }

    protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2, WorkflowResult approved2) {
        Map<String,WorkflowResult> retval = new HashMap<>();
        retval.put(oid, approved);
        retval.put(oid2, approved2);
        return retval;
    }

    protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2, WorkflowResult approved2, String oid3, WorkflowResult approved3) {
        Map<String,WorkflowResult> retval = new HashMap<>();
        retval.put(oid, approved);
        retval.put(oid2, approved2);
        retval.put(oid3, approved3);
        return retval;
    }

    protected void checkAuditRecords(Map<String, WorkflowResult> expectedResults) {
        checkWorkItemAuditRecords(expectedResults);
        checkWfProcessAuditRecords(expectedResults);
    }

    void checkWorkItemAuditRecords(Map<String, WorkflowResult> expectedResults) {
        WfTestUtil.checkWorkItemAuditRecords(expectedResults, dummyAuditService);
    }

    protected void checkWfProcessAuditRecords(Map<String, WorkflowResult> expectedResults) {
	    WfTestUtil.checkWfProcessAuditRecords(expectedResults, dummyAuditService);
    }

    protected void removeAllAssignments(String oid, OperationResult result) throws Exception {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
        for (AssignmentType at : user.asObjectable().getAssignment()) {
            ObjectDelta delta = prismContext.deltaFactory().object()
		            .createModificationDeleteContainer(UserType.class, oid, UserType.F_ASSIGNMENT,
                            at.asPrismContainerValue().clone());
            repositoryService.modifyObject(UserType.class, oid, delta.getModifications(), result);
            LOGGER.info("Removed assignment " + at + " from " + user);
        }
    }

    protected abstract class TestDetails {
        abstract int subcasesCount();
        abstract boolean immediate();
        abstract boolean checkObjectOnSubtasks();
        boolean approvedAutomatically() { return false; }
        LensContext createModelContext(Task task, OperationResult result) throws Exception { return null; }
        void assertsAfterClockworkRun(CaseType rootCase, CaseType case0,
                List<CaseType> subcases, Task opTask, OperationResult result) throws Exception { }
        void assertsAfterImmediateExecutionFinished(CaseType task, OperationResult result) throws Exception { }
        void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                OperationResult result) throws Exception { }
        boolean decideOnApproval(CaseType subcase, ApprovalContextType wfContext) throws Exception { return true; }
        String getObjectOid(CaseType task, OperationResult result) throws SchemaException { return null; };
        boolean removeAssignmentsBeforeTest() { return true; }
    }

    protected boolean decideOnRoleApproval(CaseType subcase,
            ApprovalContextType wfContext) throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        ObjectReferenceType targetRef = subcase.getTargetRef();
        assertNotNull("targetRef not found", targetRef);
        String roleOid = targetRef.getOid();
        assertNotNull("requested role OID not found", roleOid);

        if (ROLE_R1_OID.equals(roleOid)) {
            login(getUser(R1BOSS_OID));
            return true;
        } else if (ROLE_R2_OID.equals(roleOid)) {
            login(getUser(R2BOSS_OID));
            return false;
        } else if (ROLE_R3_OID.equals(roleOid)) {
            login(getUser(R3BOSS_OID));
            return true;
        } else {
            throw new AssertionError("Unknown role OID in assignment to be approved: " + roleOid);
        }
    }

    void executeTest(String testName, String focusOid, TestDetails testDetails) throws Exception {

		// GIVEN
        prepareNotifications();
        dummyAuditService.clear();
        OperationResult result = new OperationResult("execution");

        Task task = taskManager.createTaskInstance(AbstractWfTestLegacy.class.getName() + "."+testName);
        task.setOwner(userAdministrator);

        if (focusOid != null && testDetails.removeAssignmentsBeforeTest()) {
            removeAllAssignments(focusOid, result);
        }

        LensContext<UserType> context = (LensContext<UserType>) testDetails.createModelContext(task, result);
        display("Input context", context);
        assertFocusModificationSanity(context);

        // WHEN

        HookOperationMode mode = clockwork.run(context, task, result);

        // THEN

        assertEquals("Unexpected state of the context", ModelState.PRIMARY, context.getState());
        assertEquals("Wrong mode after clockwork.run in " + context.getState(), HookOperationMode.BACKGROUND, mode);
        CaseType rootCase = testHelper.getRootCase(result);

        if (!testDetails.approvedAutomatically()) {
            List<CaseType> subcases = miscHelper.getSubcases(rootCase, result);
            assertEquals("Incorrect number of subcases", testDetails.subcasesCount(), subcases.size());

            CaseType case0 = WfTestHelper.findAndRemoveCase0(subcases);
            testDetails.assertsAfterClockworkRun(rootCase, case0, subcases, task, result);
            if (testDetails.immediate()) {
                CaseType rootCaseAfter = testHelper.waitForCaseClose(case0, 20000);
                testDetails.assertsAfterImmediateExecutionFinished(rootCaseAfter, result);
            }

            for (int i = 0; i < subcases.size(); i++) {
                CaseType subcase = subcases.get(i);
                //noinspection unchecked
                PrismProperty<ObjectTreeDeltasType> deltas = subcase.asPrismContainerValue().findProperty(
                        ItemPath.create(F_APPROVAL_CONTEXT, F_DELTAS_TO_APPROVE));
                assertNotNull("There are no modifications in subcase #" + i + ": " + subcase, deltas);
                assertEquals("Incorrect number of modifications in subcase #" + i + ": " + subcase, 1, deltas.getRealValues().size());
                // todo check correctness of the modification?

                // now check the workflow state

//                WfProcessInstanceType processInstance = workflowServiceImpl.getProcessInstanceById(pid, false, true, result);
//                assertNotNull("Process instance information cannot be retrieved", processInstance);
//                assertEquals("Incorrect number of work items", 1, processInstance.getWorkItems().size());

                //String taskId = processInstance.getWorkItems().get(0).getWorkItemId();
                //WorkItemDetailed workItemDetailed = wfDataAccessor.getWorkItemDetailsById(taskId, result);

                List<CaseWorkItemType> workItems = getWorkItemsForCase(subcase.getOid(), null, result);
                CaseWorkItemType workItem = MiscUtil.extractSingleton(workItems);
                assertNotNull("work item not found", workItem);

                ApprovalContextType wfContext = subcase.getApprovalContext();
                LOGGER.trace("wfContext = {}", wfContext);

                boolean approve = testDetails.decideOnApproval(subcase, wfContext);
                workflowManager.completeWorkItem(WorkItemId.of(workItem),
                        new AbstractWorkItemOutputType(prismContext)
                                .outcome(ApprovalUtils.toUri(approve)),
                        null, task, result);
                login(userAdministrator);
            }
        }

        CaseType rootCaseAfter = testHelper.waitForCaseClose(rootCase, 60000);

        List<CaseType> subcases = miscHelper.getSubcases(rootCaseAfter, result);
        WfTestHelper.findAndRemoveCase0(subcases);
        //TestUtil.assertSuccess(rootCase.getResult());
        testDetails.assertsRootCaseFinishes(rootCaseAfter, subcases, task, result);

        if (focusOid == null) {
            focusOid = testDetails.getObjectOid(rootCaseAfter, result);
        }
        assertNotNull("object oid is null after operation", focusOid);
        if (!focusOid.equals(DONT_CHECK)) {
            assertObjectInTaskTree(rootCaseAfter, focusOid, testDetails.checkObjectOnSubtasks(), result);
        }

        if (!testDetails.approvedAutomatically()) {
            // ZZZ temporarily disabled
//            checkDummyTransportMessages("simpleWorkflowNotifier-Processes", workflowSubtaskCount * 2);
//            checkDummyTransportMessages("simpleWorkflowNotifier-WorkItems", workflowSubtaskCount * 2);
        }
        notificationManager.setDisabled(true);

        // Check audit
        display("Audit", dummyAuditService);

        display("Output context", context);
	}

    protected void assertObjectInTaskTree(CaseType rootCase, String oid, boolean checkObjectOnSubtasks, OperationResult result) throws SchemaException {
        assertObjectInTask(rootCase, oid);
        if (checkObjectOnSubtasks) {
            for (CaseType subcase: miscHelper.getSubcases(rootCase, result)) {
                assertObjectInTask(subcase, oid);
            }
        }
    }

    protected void assertObjectInTask(CaseType aCase, String oid) {
        assertEquals("Missing or wrong object OID in case " + aCase, oid, aCase.getObjectRef() != null ? aCase.getObjectRef().getOid() : null);
    }

    protected void deleteUserFromModel(String name) throws SchemaException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        OperationResult result = new OperationResult("dummy");
        Task t = taskManager.createTaskInstance();
        t.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

        if (!findUserInRepoUnchecked(name, result).isEmpty()) {

            PrismObject<UserType> user = findUserInRepo(name, result);

            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
            deltas.add(prismContext.deltaFactory().object().createDeleteDelta(UserType.class, user.getOid()));
            modelService.executeChanges(deltas, new ModelExecuteOptions(), t, result);

            LOGGER.info("User " + name + " was deleted");
        } else {
            LOGGER.info("User {} was not found", name);
        }
    }

    <O extends ObjectType> ObjectDelta<O> encryptAndAddFocusModificationToContext(
            LensContext<O> context, File file)
            throws JAXBException, SchemaException, IOException {
        ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(
                file, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<O> focusDelta = DeltaConvertor.createObjectDelta(
                modElement, context.getFocusClass(), prismContext);
        ModelImplUtils.encrypt((Collection) Arrays.asList(focusDelta), protector, null, new OperationResult("dummy"));
        return addFocusDeltaToContext(context, focusDelta);
    }

    protected ObjectQuery getOpenItemsQuery() {
	    return prismContext.queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull()
                .build();
    }
}
