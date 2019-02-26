/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.wf.impl.general;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfTestHelper;
import com.evolveum.midpoint.wf.impl.legacy.AbstractWfTestLegacy;
import com.evolveum.midpoint.wf.impl._temp.TemporaryHelper;
import com.evolveum.midpoint.wf.impl.processors.MiscHelper;
import com.evolveum.midpoint.wf.impl.processors.general.GeneralChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
//@DependsOn("workflowServiceImpl")
public class TestGeneralChangeProcessor extends AbstractInternalModelIntegrationTest {

    protected static final Trace LOGGER = TraceManager.getTrace(TestGeneralChangeProcessor.class);

    private static final String TEST_RESOURCE_DIR_NAME = "src/test/resources";
    private static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1 = new File(TEST_RESOURCE_DIR_NAME,
            "legacy/user-jack-modify-add-assignment-role1.xml");
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE2_CHANGE_GN = TEST_RESOURCE_DIR_NAME + "/legacy/user-jack-modify-add-assignment-role2-change-gn.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE3_CHANGE_GN2 = TEST_RESOURCE_DIR_NAME + "/legacy/user-jack-modify-add-assignment-role3-change-gn2.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4 = TEST_RESOURCE_DIR_NAME + "/legacy/user-jack-modify-add-assignment-roles2-3-4.xml";
    private static final String REQ_USER_JACK_MODIFY_ACTIVATION_DISABLE = TEST_RESOURCE_DIR_NAME + "/legacy/user-jack-modify-activation-disable.xml";
    private static final String REQ_USER_JACK_MODIFY_ACTIVATION_ENABLE = TEST_RESOURCE_DIR_NAME + "/legacy/user-jack-modify-activation-enable.xml";
    private static final String REQ_USER_JACK_MODIFY_CHANGE_PASSWORD = TEST_RESOURCE_DIR_NAME + "/legacy/user-jack-modify-change-password.xml";
    private static final String REQ_USER_JACK_MODIFY_CHANGE_PASSWORD_2 = TEST_RESOURCE_DIR_NAME + "/legacy/user-jack-modify-change-password-2.xml";

    private static final String DONT_CHECK = "dont-check";

    @Autowired private Clockwork clockwork;
    @Autowired private TaskManager taskManager;
    @Autowired private TemporaryHelper temporaryHelper;
    @Autowired private MiscDataUtil miscDataUtil;
    @Autowired private MiscHelper miscHelper;
    @Autowired private PrimaryChangeProcessor primaryChangeProcessor;
    @Autowired private GeneralChangeProcessor generalChangeProcessor;
    @Autowired private PrismContext prismContext;
    @Autowired private WfTestHelper testHelper;

    public TestGeneralChangeProcessor() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
        importObjectFromFile(AbstractWfTestLegacy.USERS_AND_ROLES_FILE, initResult);
        modifyObjectReplaceProperty(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_WORKFLOW_CONFIGURATION,
                        WfConfigurationType.F_PRIMARY_CHANGE_PROCESSOR,
                        PrimaryChangeProcessorConfigurationType.F_ENABLED),
                initTask, initResult,
                false);
        modifyObjectReplaceProperty(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_WORKFLOW_CONFIGURATION,
                        WfConfigurationType.F_GENERAL_CHANGE_PROCESSOR,
                        GeneralChangeProcessorConfigurationType.F_ENABLED),
                initTask, initResult,
                true);
	}

	@Test
    public void test010AddRole1() throws Exception {
        TestUtil.displayTestTitle(this, "test010UserModifyAddRole");
        executeTest("test010UserModifyAddRole", USER_JACK_OID, 1, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
                return context;
            }

            @Override
            void assertsAfterClockworkRun(ModelContext context, Task task, OperationResult result) throws Exception {
                assertEquals("Unexpected state of the context", ModelState.PRIMARY, context.getState());
            }

            @Override
            void completeWorkItem(CaseWorkItemType workItem, String taskId, OperationResult result) throws Exception {
//                WorkItemContents contents = (WorkItemContents) workItem.getContents();
//                PrismObject<? extends QuestionFormType> qFormObject = contents.getQuestionForm().asPrismObject();
//                LOGGER.trace("workItemContents = " + qFormObject.debugDump());
//
//                // change role1 -> role2
//                final int N = 6;
//                StringBuilder ctx = new StringBuilder();
//
//                for (int ctxIndex = 0; ctxIndex < N; ctxIndex++) {
//                    QName contextQName = new QName(SchemaConstants.NS_WFCF, "modelContextToBeEdited" + ctxIndex);
//                    PrismProperty<String> contextProperty = qFormObject.findProperty(contextQName);
//                    assertNotNull(contextQName + " not found among workItem specific properties", contextProperty);
//                    ctx.append(contextProperty.getRealValue());
//                }
//
//                String newCtx = ctx.toString().replaceAll("00000001-d34d-b33f-f00d-000000000001", "00000001-d34d-b33f-f00d-000000000002");
//                for (int ctxIndex = 0; ctxIndex < N; ctxIndex++) {
//                    QName contextQName = new QName(SchemaConstants.NS_WFCF, "modelContextToBeEdited" + ctxIndex);
//                    PrismProperty<String> contextProperty = qFormObject.findProperty(contextQName);
//                    contextProperty.replace(new PrismPropertyValue<>(JaxbValueContainer.getChunk(newCtx, ctxIndex)));
//                }
//
//                login(getUser(USER_ADMINISTRATOR_OID));
////                workflowServiceImpl.completeWorkItem(taskId, qFormObject, "approve", result);
            }

            @Override
            void assertsRootTaskFinishes(CaseType aCase, Task task, OperationResult result) throws Exception {
                assertAssignedRole(USER_JACK_OID, AbstractWfTestLegacy.ROLE_R2_OID, task, result);
                checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(AbstractWfTestLegacy.ROLE_R1_OID, WorkflowResult.APPROVED));
                //checkUserApprovers(USER_JACK_OID, Arrays.asList(AbstractWfTestLegacy.R1BOSS_OID), result);
            }
        });
	}

    @Test
    public void test020AddAccountRejected() throws Exception {
        TestUtil.displayTestTitle(this, "test020AddAccountRejected");

        enableDisableScenarios(false, true);

        executeTest("test020AddAccountRejected", USER_JACK_OID, 1, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContextAddAccountFromFile(context, ACCOUNT_SHADOW_JACK_DUMMY_FILE);
                return context;
            }

            @Override
            void assertsAfterClockworkRun(ModelContext context, Task task, OperationResult result) throws Exception {
                assertEquals("Unexpected state of the context", ModelState.SECONDARY, context.getState());
            }

            @Override
            void completeWorkItem(CaseWorkItemType workItem, String taskId, OperationResult result) throws Exception {
//
//                PrismObject<? extends WorkItemContents> workItemContents = workItem.getContents().asPrismObject();
//                display("workItemContents", workItemContents);
//
//                PrismObject<? extends QuestionFormType> questionFormPrism = workItemContents.asObjectable().getQuestionForm().asPrismObject();
//
//                WfProcessInstanceType instance = null; //workflowServiceImpl.getProcessInstanceById(workItem.getProcessInstanceId(), false, true, result);
//                PrismProperty<ObjectDeltaType> dummyResourceDelta = null; // TODO ((ProcessInstanceState) instance.getState()).getProcessSpecificState().asPrismContainerValue().findProperty(ApprovingDummyResourceChangesScenarioBean.DUMMY_RESOURCE_DELTA_QNAME);
//                ObjectDeltaType deltaType = dummyResourceDelta.getRealValue();
//                display("dummyResourceDelta", DeltaConvertor.createObjectDelta(deltaType, prismContext));
//
//                PrismPropertyDefinition ppd = new PrismPropertyDefinitionImpl(new QName(SchemaConstants.NS_WFCF, "[Button]rejectAll"),
//                        DOMUtil.XSD_BOOLEAN, prismContext);
//                PrismProperty<Boolean> rejectAll = ppd.instantiate();
//                rejectAll.setRealValue(Boolean.TRUE);
//                questionFormPrism.addReplaceExisting(rejectAll);
//
//                login(getUser(USER_ADMINISTRATOR_OID));
////                workflowServiceImpl.completeWorkItem(taskId, questionFormPrism, "rejectAll", result);
            }

            @Override
            void assertsRootTaskFinishes(CaseType aCase, Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUser(USER_JACK_OID);
//                assertAssignedRole(USER_JACK_OID, AbstractWfTestLegacy.ROLE_R2_OID, task, result);
                assertNoLinkedAccount(jack);
                //checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(AbstractWfTestLegacy.ROLE_R1_OID, WorkflowResult.APPROVED));
                //checkUserApprovers(USER_JACK_OID, Arrays.asList(AbstractWfTestLegacy.R1BOSS_OID), result);
            }


        });
    }

    protected void enableDisableScenarios(boolean... values) throws ObjectNotFoundException, SchemaException, com.evolveum.midpoint.util.exception.ExpressionEvaluationException, com.evolveum.midpoint.util.exception.CommunicationException, com.evolveum.midpoint.util.exception.ConfigurationException, com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException, com.evolveum.midpoint.util.exception.PolicyViolationException, com.evolveum.midpoint.util.exception.SecurityViolationException {
        OperationResult result = new OperationResult("execution");
        Task task = taskManager.createTaskInstance("execution");
        GeneralChangeProcessorConfigurationType gcpConfig = getSystemConfiguration().getWorkflowConfiguration().getGeneralChangeProcessor();
        for (int i = 0; i < values.length; i++) {
            gcpConfig.getScenario().get(i).setEnabled(values[i]);
        }
        modifyObjectReplaceProperty(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_WORKFLOW_CONFIGURATION,
                        WfConfigurationType.F_GENERAL_CHANGE_PROCESSOR,
                        GeneralChangeProcessorConfigurationType.F_ENABLED),
                task, result,
                gcpConfig);
    }

//    @Test(enabled = false)
//    public void test028CurrentRepo() throws Exception {
//        TestUtil.displayTestTile(this, "test029NewRepo");
//
//        //old repo
//        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
//        //"extension" value
//        String xml = IOUtils.toString(new FileInputStream("./src/test/resources/model-context.xml"), "utf-8");
//        Element root = DOMUtil.parseDocument(xml).getDocumentElement();
//
//        QName name = new QName("http://midpoint.evolveum.com/xml/ns/public/model/model-context-3", "modelContext");
//
//        PrismObjectDefinition oDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
//        PrismContainerDefinition def = oDef.findContainerDefinition(prismContext.path(ObjectType.F_EXTENSION, name));
//        Item parsedItem = domProcessor.parseItem(DOMUtil.listChildElements(root), name, def);
//        LOGGER.debug("Parser:\n{}", parsedItem.debugDump());
//    }

//    @Test(enabled = false)
//    public void test029NewRepo() throws Exception {
//        TestUtil.displayTestTile(this, "test029NewRepo");
//
//        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
//        String xml = IOUtils.toString(new FileInputStream("./src/test/resources/task.xml"), "utf-8");
//        PrismObject o = domProcessor.parseObject(xml);
//        LOGGER.info("Parsed:\n{}", o.debugDump());
//    }

    @Test
    public void test030AddAccountApproved() throws Exception {
        TestUtil.displayTestTitle(this, "test030AddAccountApproved");

        enableDisableScenarios(false, true);

        executeTest("test030AddAccountApproved", USER_JACK_OID, 1, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContextAddAccountFromFile(context, ACCOUNT_SHADOW_JACK_DUMMY_FILE);
                return context;
            }

            @Override
            void assertsAfterClockworkRun(ModelContext context, Task task, OperationResult result) throws Exception {
                assertEquals("Unexpected state of the context", ModelState.SECONDARY, context.getState());
            }

            @Override
            void completeWorkItem(CaseWorkItemType workItem, String taskId, OperationResult result) throws Exception {
//
//                PrismObject<? extends WorkItemContents> workItemContents = workItem.getContents().asPrismObject();
//                display("workItemContents", workItemContents);
//
//                PrismObject<? extends QuestionFormType> questionFormPrism = workItemContents.asObjectable().getQuestionForm().asPrismObject();
//
//                WfProcessInstanceType instance = null; //workflowServiceImpl.getProcessInstanceById(workItem.getProcessInstanceId(), false, true, result);
//                PrismProperty<ObjectDeltaType> dummyResourceDelta = null; // TODO ((ProcessInstanceState) instance.getState()).getProcessSpecificState().asPrismContainerValue().findProperty(ApprovingDummyResourceChangesScenarioBean.DUMMY_RESOURCE_DELTA_QNAME);
//                ObjectDeltaType deltaType = dummyResourceDelta.getRealValue();
//                display("dummyResourceDelta", DeltaConvertor.createObjectDelta(deltaType, prismContext));
//
//                PrismPropertyDefinition ppd = new PrismPropertyDefinitionImpl(new QName(SchemaConstants.NS_WFCF, "[Button]approve"),
//                        DOMUtil.XSD_BOOLEAN, prismContext);
//                PrismProperty<Boolean> approve = ppd.instantiate();
//                approve.setRealValue(Boolean.TRUE);
//                questionFormPrism.addReplaceExisting(approve);
//
//                login(getUser(USER_ADMINISTRATOR_OID));
////                workflowServiceImpl.completeWorkItem(taskId, questionFormPrism, "approve", result);
            }

            @Override
            void assertsRootTaskFinishes(CaseType aCase, Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUser(USER_JACK_OID);
//                assertAssignedRole(USER_JACK_OID, AbstractWfTestLegacy.ROLE_R2_OID, task, result);
                assertAccount(jack, RESOURCE_DUMMY_OID);
                //checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(AbstractWfTestLegacy.ROLE_R1_OID, WorkflowResult.APPROVED));
                //checkUserApprovers(USER_JACK_OID, Arrays.asList(AbstractWfTestLegacy.R1BOSS_OID), result);
            }


        });
    }


    @Test
    public void test000LoadContext() throws Exception {
    	final String TEST_NAME = "test000LoadContext";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        LensContextType lensContextType = prismContext.parserFor(new File("src/test/resources/model-contexts/context-dummy-resource.xml")).xml().parseRealValue(LensContextType.class);
        display("LensContextType", lensContextType);
        LensContext<?> lensContext = LensContext.fromLensContextType(lensContextType, prismContext, provisioningService, task, result);
        display("LensContext", lensContext);
    }


    private abstract class ContextCreator {
        LensContext createModelContext(OperationResult result) throws Exception { return null; }
        void assertsAfterClockworkRun(ModelContext context, Task task, OperationResult result) throws Exception { }
        void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception { }
        void assertsRootTaskFinishes(CaseType aCase, Task task, OperationResult result) throws Exception { }
        String getObjectOid(Task task, OperationResult result) throws SchemaException { return null; };

        abstract void completeWorkItem(CaseWorkItemType workItem, String taskId, OperationResult result) throws Exception;
    }

	private void executeTest(String testName, String oid, int subcasesCount, boolean immediate, boolean checkObjectOnSubtasks, ContextCreator contextCreator) throws Exception {

        int workflowSubtaskCount = immediate ? subcasesCount-1 : subcasesCount;

		// GIVEN
        prepareNotifications();
        dummyAuditService.clear();

        OperationResult result = new OperationResult("execution");

        Task task = taskManager.createTaskInstance(TestGeneralChangeProcessor.class.getName() + "."+testName);
        display("Model task after creation", task);

        LensContext<UserType> context = (LensContext<UserType>) contextCreator.createModelContext(result);
        task.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

        display("Input context", context);
        assertFocusModificationSanity(context);

        // WHEN

       	HookOperationMode mode = clockwork.run(context, task, result);

        // THEN

        contextCreator.assertsAfterClockworkRun(context, task, result);
        assertEquals("Wrong mode after clockwork.run in " + context.getState(), HookOperationMode.BACKGROUND, mode);
        task.refresh(result);
        display("Model task after clockwork runs", task);

        CaseType rootCase = testHelper.getRootCase(result);
        display("Workflow root case created by clockwork run", rootCase);

        ModelContext taskModelContext = immediate ? null : temporaryHelper.getModelContext(rootCase, task, result);
        assertNotNull("Model context is not present in root task", taskModelContext);

        List<CaseType> subcases = miscHelper.getSubcases(rootCase, result);
        assertEquals("Incorrect number of subcases", subcasesCount, subcases.size());

        for (int subcaseIndex = 0; subcaseIndex < subcases.size(); subcaseIndex++) {
            CaseType subcase = subcases.get(subcaseIndex);

            // now check the workflow state

/*
            WfProcessInstanceType processInstance = null; //workflowServiceImpl.getProcessInstanceById(pid, false, true, result);
            assertNotNull("Process instance information cannot be retrieved", processInstance);
            assertEquals("Incorrect number of work items", 1, processInstance.getWorkItems().size());

            String taskId = processInstance.getWorkItems().get(0).getWorkItemId();
            //WorkItemNewType workItem = workflowServiceImpl.getWorkItemDetailsById(taskId, result);
            WorkItemNewType workItem = null;       // TODO

            org.activiti.engine.task.Task t = activitiEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
            assertNotNull("activiti task not found", t);

            String executionId = t.getExecutionId();
            LOGGER.trace("Task id = " + taskId + ", execution id = " + executionId);

            contextCreator.completeWorkItem(workItem, taskId, result);
*/
        }

        testHelper.waitForCaseClose(rootCase, 60000);
        contextCreator.assertsRootTaskFinishes(rootCase, task, result);
    }
}
