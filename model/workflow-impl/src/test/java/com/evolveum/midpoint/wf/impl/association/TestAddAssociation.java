/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.association;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType.F_DELTAS_TO_APPROVE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.F_APPROVAL_CONTEXT;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.wf.impl.AbstractWfTest;
import com.evolveum.midpoint.wf.impl.WfTestHelper;
import com.evolveum.midpoint.wf.impl.WorkflowResult;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineImpl;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpGeneralHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Testing approval of association "add" operation.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAddAssociation extends AbstractWfTest {

    private static final File TEST_RESOURCE_DIR = new File("src/test/resources/association");

    private static final String DONT_CHECK = "dont-check";

    @Autowired protected Clockwork clockwork;
    @Autowired protected TaskManager taskManager;
    @Autowired protected CaseManager caseManager;
    @Autowired protected CaseEngineImpl caseEngine;
    @Autowired protected MiscHelper miscHelper;
    @Autowired protected PrimaryChangeProcessor primaryChangeProcessor;
    @Autowired protected WfTestHelper testHelper;
    @Autowired protected PcpGeneralHelper pcpGeneralHelper;

    public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_RESOURCE_DIR, "system-configuration.xml");

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_RESOURCE_DIR, "resource-dummy.xml");
    public static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";

    private static final File ACCOUNT_SHADOW_JACK_DUMMY_FILE = new File(TEST_RESOURCE_DIR, "shadow-jack-dummy.xml");

    private static final File USER_DUMMY_BOSS_FILE = new File(TEST_RESOURCE_DIR, "user-dummy-boss.xml");

    private static final String SHADOW_TESTERS_OID = "20000000-0000-0000-3333-000000000002";
    private static final File SHADOW_TESTERS_FILE = new File(TEST_RESOURCE_DIR, "shadow-testers-dummy.xml");
    private static final String TESTERS_NAME = "testers";

    private static final String SHADOW_GUESTS_OID = "20000000-0000-0000-3333-000000000072";
    private static final File SHADOW_GUESTS_FILE = new File(TEST_RESOURCE_DIR, "shadow-guests-dummy.xml");
    private static final String GUESTS_NAME = "guests";

    private static final File USER_ELISABETH_FILE = new File(TEST_RESOURCE_DIR, "user-elisabeth.xml");
    private static final String USER_ELISABETH_OID = "c0c010c0-d34d-b33f-f00d-111111112222";

    private static final File SHADOW_ELISABETH_DUMMY_FILE = new File(TEST_RESOURCE_DIR, "shadow-elisabeth-dummy.xml");

    private static final File REQ_ADD_ENTITLEMENT_TESTERS = new File(TEST_RESOURCE_DIR, "req-add-entitlement-testers.xml");
    private static final File REQ_ADD_ENTITLEMENT_GUESTS = new File(TEST_RESOURCE_DIR, "req-add-entitlement-guests.xml");

    private String jackAccountShadowOid;
    private String elisabethAccountShadowOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        super.initSystem(initTask, initResult);
        importObjectFromFile(USER_DUMMY_BOSS_FILE, initResult);

        initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);

        importObjectFromFile(SHADOW_TESTERS_FILE, initResult);
        importObjectFromFile(SHADOW_GUESTS_FILE, initResult);

        getDummyResourceController().addGroup(TESTERS_NAME);
        getDummyResourceController().addGroup(GUESTS_NAME);

        systemObjectCache.invalidateCaches();

        modifyUserAddAccount(USER_JACK.oid, ACCOUNT_SHADOW_JACK_DUMMY_FILE, initTask, initResult);

        importObjectFromFile(USER_ELISABETH_FILE, initResult);
        modifyUserAddAccount(USER_ELISABETH_OID, SHADOW_ELISABETH_DUMMY_FILE, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * Add entitlement to user jack
     */
    @Test
    public void test010AddJackToTesters() throws Exception {
        executeTest("test010AddJackToTesters", USER_JACK.oid, new TestDetails() {
            @Override
            int subcasesCount() {
                return 1;
            }

            @Override
            boolean immediate() {
                return false;
            }

            @Override
            boolean checkObjectOnSubtasks() {
                return true;
            }

            @Override
            boolean removeAssignmentsBeforeTest() {
                return false;
            }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK.oid, result);

                UserType jack = context.getFocusContext().getObjectCurrent().asObjectable();
                AssertJUnit.assertEquals("Jack has wrong number of accounts", 1, jack.getLinkRef().size());
                jackAccountShadowOid = jack.getLinkRef().get(0).getOid();

                LensProjectionContext accountContext = fillContextWithAccount(context, jackAccountShadowOid, task, result);

                ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(REQ_ADD_ENTITLEMENT_TESTERS, ObjectModificationType.COMPLEX_TYPE);
                ObjectDelta shadowDelta = DeltaConvertor.createObjectDelta(modElement, ShadowType.class, prismContext);
                shadowDelta.setOid(jackAccountShadowOid);
                //noinspection unchecked
                accountContext.setPrimaryDelta(shadowDelta);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                ModelContext taskModelContext = miscHelper.getModelContext(rootCase, opTask, result);
                displayDumpable("model context from the root task", taskModelContext);
                //                assertEquals("Wrong # of projection contexts in root task", 1, taskModelContext.getProjectionContexts().size());
//                assertTrue("There are modifications in primary focus delta", ObjectDelta.isEmpty(taskModelContext.getFocusContext().getPrimaryDelta()));
//                assertTrue("There are modifications left in primary projection delta",
//                        ObjectDelta.isEmpty(
//                                ((LensProjectionContext) (taskModelContext.getProjectionContexts().iterator().next()))
//                                        .getPrimaryDelta()));
                ShadowType account = getObject(ShadowType.class, jackAccountShadowOid).asObjectable();
                IntegrationTestTools.display("jack dummy account after first clockwork run", account);
                assertEquals("Unexpected associations present", 0, account.getAssociation().size());
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                ShadowType account = getObject(ShadowType.class, jackAccountShadowOid).asObjectable();
                IntegrationTestTools.display("jack dummy account", account);
                assertHasAssociation(account, new QName("group"), SHADOW_TESTERS_OID);

                checkAuditRecords(createResultMap(SHADOW_TESTERS_OID, WorkflowResult.APPROVED));
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    ApprovalContextType wfContext) {
                return true;
            }
        });
    }

    /**
     * Add entitlement to user elisabeth (rejected)
     */
    @Test
    public void test020AddElisabethToTestersRejected() throws Exception {
        executeTest("test020AddElisabethToTestersRejected", USER_ELISABETH_OID, new TestDetails() {
            @Override
            int subcasesCount() {
                return 1;
            }

            @Override
            boolean immediate() {
                return false;
            }

            @Override
            boolean checkObjectOnSubtasks() {
                return true;
            }

            @Override
            boolean removeAssignmentsBeforeTest() {
                return false;
            }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_ELISABETH_OID, result);

                UserType elisabeth = context.getFocusContext().getObjectCurrent().asObjectable();
                AssertJUnit.assertEquals("Elisabeth has wrong number of accounts", 1, elisabeth.getLinkRef().size());
                elisabethAccountShadowOid = elisabeth.getLinkRef().get(0).getOid();

                LensProjectionContext accountContext = fillContextWithAccount(context, elisabethAccountShadowOid, task, result);

                ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(REQ_ADD_ENTITLEMENT_TESTERS, ObjectModificationType.COMPLEX_TYPE);
                ObjectDelta shadowDelta = DeltaConvertor.createObjectDelta(modElement, ShadowType.class, prismContext);
                shadowDelta.setOid(elisabethAccountShadowOid);
                //noinspection unchecked
                accountContext.setPrimaryDelta(shadowDelta);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                ModelContext taskModelContext = miscHelper.getModelContext(rootCase, opTask, result);
                displayDumpable("model context from the root task", taskModelContext);
                //                assertEquals("Wrong # of projection contexts in root task", 1, taskModelContext.getProjectionContexts().size());
//                assertTrue("There are modifications in primary focus delta", ObjectDelta.isEmpty(taskModelContext.getFocusContext().getPrimaryDelta()));
//                assertTrue("There are modifications left in primary projection delta",
//                        ObjectDelta.isEmpty(
//                                ((LensProjectionContext) (taskModelContext.getProjectionContexts().iterator().next()))
//                                        .getPrimaryDelta()));
                ShadowType account = getObject(ShadowType.class, elisabethAccountShadowOid).asObjectable();
                IntegrationTestTools.display("elisabeth dummy account after first clockwork run", account);
                assertEquals("Unexpected associations present", 0, account.getAssociation().size());
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                ShadowType account = getObject(ShadowType.class, elisabethAccountShadowOid).asObjectable();
                IntegrationTestTools.display("elisabeth dummy account", account);
                assertEquals("Unexpected associations present", 0, account.getAssociation().size());

                checkAuditRecords(createResultMap(SHADOW_TESTERS_OID, WorkflowResult.REJECTED));
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    ApprovalContextType wfContext) {
                return false;
            }
        });
    }

    /**
     * Add entitlement for 'guests' to user jack - should be created without starting wf process
     */
    @Test
    public void test100AddJackToGuests() throws Exception {
        Task modelTask = getTestTask();
        OperationResult result = createOperationResult();
        modelTask.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK.oid, result);

        UserType jack = context.getFocusContext().getObjectCurrent().asObjectable();
        AssertJUnit.assertEquals("Jack has wrong number of accounts", 1, jack.getLinkRef().size());
        jackAccountShadowOid = jack.getLinkRef().get(0).getOid();

        ShadowType accountBefore = getObject(ShadowType.class, jackAccountShadowOid).asObjectable();
        assertEquals("Wrong # of jack's account associations", 1, accountBefore.getAssociation().size());
        assertHasAssociation(accountBefore, new QName("group"), SHADOW_TESTERS_OID);

        LensProjectionContext accountContext = fillContextWithAccount(context, jackAccountShadowOid, modelTask, result);

        ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(REQ_ADD_ENTITLEMENT_GUESTS, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta shadowDelta = DeltaConvertor.createObjectDelta(modElement, ShadowType.class, prismContext);
        shadowDelta.setOid(jackAccountShadowOid);
        //noinspection unchecked
        accountContext.setPrimaryDelta(shadowDelta);

        HookOperationMode mode = clockwork.run(context, modelTask, result);

        assertEquals("Unexpected state of the context - workflow was started even if it should not", ModelState.FINAL, context.getState());
        assertEquals("Wrong mode after clockwork.run in " + context.getState(), HookOperationMode.FOREGROUND, mode);

        ShadowType accountAfter = getObject(ShadowType.class, jackAccountShadowOid).asObjectable();
        assertEquals("Wrong # of jack's account associations", 2, accountAfter.getAssociation().size());
        assertHasAssociation(accountAfter, new QName("group"), SHADOW_TESTERS_OID);
        assertHasAssociation(accountAfter, new QName("group"), SHADOW_GUESTS_OID);
    }

    public void assertHasAssociation(ShadowType shadow, QName associationName, String entitlementOid) {
        for (ShadowAssociationType association : shadow.getAssociation()) {
            if (QNameUtil.match(association.getName(), associationName) &&
                    entitlementOid.equals(association.getShadowRef().getOid())) {
                return;
            }
        }
        AssertJUnit.fail("No association of type " + associationName + " of " + entitlementOid + " in " + ObjectTypeUtil.toShortString(shadow));
    }

    abstract static class TestDetails {
        abstract int subcasesCount();
        abstract boolean immediate();
        abstract boolean checkObjectOnSubtasks();

        boolean approvedAutomatically() {
            return false;
        }

        LensContext createModelContext(Task task, OperationResult result) throws Exception {
            return null;
        }

        void assertsAfterClockworkRun(CaseType rootCase, CaseType case0,
                List<CaseType> subcases, Task opTask, OperationResult result) throws Exception {
        }

        void assertsAfterImmediateExecutionFinished(CaseType task, OperationResult result) {
        }

        void assertsRootCaseFinishes(
                CaseType aCase, List<CaseType> subcases, Task opTask, OperationResult result)
                throws Exception {
        }

        boolean decideOnApproval(CaseType subcase, ApprovalContextType wfContext) {
            return true;
        }

        String getObjectOid(CaseType task, OperationResult result) {
            return null;
        }

        boolean removeAssignmentsBeforeTest() {
            return true;
        }
    }

    private void executeTest(String testName, String focusOid, TestDetails testDetails) throws Exception {

        // GIVEN
        prepareNotifications();
        dummyAuditService.clear();
        OperationResult result = new OperationResult("execution");

        Task task = taskManager.createTaskInstance(TestAddAssociation.class.getName() + "." + testName);
        task.setOwner(userAdministrator);

        if (focusOid != null && testDetails.removeAssignmentsBeforeTest()) {
            removeAllAssignments(focusOid, result);
        }

        //noinspection unchecked
        LensContext<UserType> context = (LensContext<UserType>) testDetails.createModelContext(task, result);
        displayDumpable("Input context", context);
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
                logger.trace("wfContext = {}", wfContext);

                boolean approve = testDetails.decideOnApproval(subcase, wfContext);
                caseManager.completeWorkItem(WorkItemId.of(workItem),
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
        displayDumpable("Audit", dummyAuditService);

        displayDumpable("Output context", context);
    }

    private void assertObjectInTaskTree(CaseType rootCase, String oid, boolean checkObjectOnSubtasks, OperationResult result) throws SchemaException {
        assertObjectInTask(rootCase, oid);
        if (checkObjectOnSubtasks) {
            for (CaseType subcase : miscHelper.getSubcases(rootCase, result)) {
                assertObjectInTask(subcase, oid);
            }
        }
    }

    private void assertObjectInTask(CaseType aCase, String oid) {
        assertEquals("Missing or wrong object OID in case " + aCase, oid, aCase.getObjectRef() != null ? aCase.getObjectRef().getOid() : null);
    }
}
