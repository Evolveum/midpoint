/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.wf.old;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.history.HistoryLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static com.evolveum.midpoint.schema.util.MiscSchemaUtil.getDefaultImportOptions;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
public class BasicTestUnused extends AbstractTestNGSpringContextTests {

    private static final String TEST_FILE_DIRECTORY = "src/test/resources/repo/";
    private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
    private static final File IMPORT_USERS_AND_ROLES_FILE = new File(TEST_FILE_DIRECTORY, "users-and-roles.xml");
    private static final String USER_JACK_OID = "00000000-d34d-b33f-f00d-111111111110";
    private static final String ROLES_OID = "00000001-d34d-b33f-f00d-00000000000";
    private static String getRoleOid(String roleNumber) { return ROLES_OID + roleNumber; }

    public static final String R1BOSS_OID = "00000000-d34d-b33f-f00d-111111111111";
    public static final String R2BOSS_OID = "00000000-d34d-b33f-f00d-111111111112";
    public static final String R3BOSS_OID = "00000000-d34d-b33f-f00d-111111111113";

    @Autowired(required = true)
    ModelService modelService;

    @Autowired(required = true)
    private com.evolveum.midpoint.repo.api.RepositoryService repositoryService;

    @Autowired(required = true)
    private TaskManager taskManager;

    private static ProcessEngine processEngine;

    @BeforeClass
    public void init() throws FileNotFoundException {
        processEngine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setHistory(HistoryLevel.FULL.getKey())
                .buildProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment().addClasspathResource("processes/AddRoles._bpmn20.xml").deploy();

        import1();
    }

    public void import1() throws FileNotFoundException {

        OperationResult result = new OperationResult("import1");
        Task task = taskManager.createTaskInstance();
        FileInputStream stream = new FileInputStream(IMPORT_USERS_AND_ROLES_FILE);

        modelService.importObjectsFromStream(stream, getDefaultImportOptions(), task, result);

        result.computeStatus();
        display("Result after good import", result);
        if (!result.isSuccess()) {
            throw new RuntimeException("Import of users and roles has failed: " + result.getMessage());
        }
    }

//    @Test
//    public void test000Integrity() throws Exception {
//        OperationResult result = createResult("000Integrity");
//        assertNotNull(modelService);
//        assertNotNull(repositoryService);
//
//        PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, result);
//        assertNotNull("Jack is not there", jack);
//
//        assertNotNull("Role1 is not there", repositoryService.getObject(RoleType.class, getRoleOid("1"), result));
//        assertNotNull("Role2 is not there", repositoryService.getObject(RoleType.class, getRoleOid("2"), result));
//        assertNotNull("Role3 is not there", repositoryService.getObject(RoleType.class, getRoleOid("3"), result));
//    }
//
//    @Test
//    public void test010StartProcessInstance() throws ObjectNotFoundException, SchemaException {
//
//        OperationResult result = createResult("010StartProcessInstance");
//        Map<String, Object> variableMap = new HashMap<String, Object>();
//
//        variableMap.put(WfConstants.VARIABLE_PROCESS_NAME, "Adding some roles to a user");
//        variableMap.put(WfConstants.VARIABLE_START_TIME, new Date());
//        variableMap.put(AddRolesWrapper.USER_NAME, "jack");
//
//        List<AssignmentToApprove> assignmentToApproveList = new ArrayList<AssignmentToApprove>();
//        assignmentToApproveList.add(createAssignmentToApprove("1", result));
//        assignmentToApproveList.add(createAssignmentToApprove("2", result));
//        assignmentToApproveList.add(createAssignmentToApprove("3", result));
//
//        LOGGER.info("AssignmentsToApprove = " + assignmentToApproveList);
//
//        variableMap.put(AddRolesWrapper.ASSIGNMENTS_TO_APPROVE, assignmentToApproveList);
//        variableMap.put(AddRolesWrapper.ASSIGNMENTS_APPROVALS, new AssignmentsApprovals());
//        variableMap.put(AddRolesWrapper.ALL_DECISIONS, new ArrayList<Decision>());
//        variableMap.put(WfConstants.VARIABLE_UTIL, new ActivitiUtil());
//
////        variableMap.put(WfConstants.VARIABLE_MIDPOINT_OBJECT_OID, objectOid);
////        variableMap.put(WfConstants.VARIABLE_MIDPOINT_OBJECT_BEFORE, fc.getObjectOld());
////        variableMap.put(WfConstants.VARIABLE_MIDPOINT_OBJECT_AFTER, fc.getObjectNew());
////        //spi.addProcessVariable(WfConstants.VARIABLE_MIDPOINT_DELTA, change);
////        variableMap.put(WfConstants.VARIABLE_MIDPOINT_REQUESTER, requester);
////        variableMap.put(WfConstants.VARIABLE_MIDPOINT_REQUESTER_OID, task.getOwner().getOid());
//        variableMap.put(WfConstants.VARIABLE_MIDPOINT_ADDITIONAL_DATA, "@role");
//
//        ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey(AddRolesWrapper.ADD_ROLE_PROCESS, variableMap);
//        assertNotNull(processInstance.getId());
//        System.out.println("test010: id " + processInstance.getId() + " " + processInstance.getProcessDefinitionId());
//
//        TaskService taskService = processEngine.getTaskService();
//        List<org.activiti.engine.task.Task> tasks1 = taskService.createTaskQuery().taskAssignee(R1BOSS_OID).list();
//        assertEquals("Number of tasks for R1 approver is not correct", 1, tasks1.size());
//        completeTask(tasks1.get(0), "true", "Role1 OK");
//
//        List<org.activiti.engine.task.Task> tasks2 = taskService.createTaskQuery().taskAssignee(R2BOSS_OID).list();
//        assertEquals("Number of tasks for R2 approver is not correct", 1, tasks2.size());
//        completeTask(tasks2.get(0), "false", "Role2 NOT OK");
//
//        List<org.activiti.engine.task.Task> tasks3 = taskService.createTaskQuery().taskAssignee(R3BOSS_OID).list();
//        assertEquals("Number of tasks for R3 approver is not correct", 1, tasks3.size());
//        completeTask(tasks3.get(0), "true", "Role3 OK");
//
//        assertEquals("Process instance is still running", 0, processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstance.getProcessInstanceId()).count());
//
//        boolean decisionListTested = false;
//        List<HistoricDetail> historicVariableUpdateList = processEngine.getHistoryService()
//                .createHistoricDetailQuery()
//                .variableUpdates()
//                .orderByTime().desc()
//                .list();
//        for (HistoricDetail historicDetail : historicVariableUpdateList) {
//            HistoricVariableUpdate historicVariableUpdate = (HistoricVariableUpdate) historicDetail;
//            if(AddRolesWrapper.ALL_DECISIONS.equals(historicVariableUpdate.getVariableName())) {
//                decisionListTested = true;
//                List<Decision> decisionList = (List<Decision>) historicVariableUpdate.getValue();
//                assertEquals("There are not 3 answers", 3, decisionList.size());
//                int yes = 0, no = 0;
//                for (Decision decision : decisionList) {
//                    if (decision.isApproved()) {
//                        yes++;
//                        assertTrue("Wrong OK comment", "Role1 OK".equals(decision.getComment()) || "Role3 OK".equals(decision.getComment()));
//                    } else {
//                        no++;
//                        assertEquals("Wrong NOT OK comment", decision.getComment(), "Role2 NOT OK");
//                    }
//                }
//                break;
//            }
//        }
//        assertTrue("allDecisions variable was not found", decisionListTested);
//    }
//
//    private AssignmentToApprove createAssignmentToApprove(String s, OperationResult result) throws ObjectNotFoundException, SchemaException {
//        RoleType role = repositoryService.getObject(RoleType.class, getRoleOid(s), result).asObjectable();
//        AssignmentType assignment = new AssignmentType();
//        assignment.setTarget(role);
//        ObjectReferenceType ort = new ObjectReferenceType();
//        ort.setOid(role.getOid());
//        ort.setType(role.asPrismObject().getName());
//        assignment.setTargetRef(ort);
//        return new AssignmentToApprove(assignment, role);
//    }
//
//    private void completeTask(org.activiti.engine.task.Task task, String decision, String comment) {
//        TaskFormData tfd = processEngine.getFormService().getTaskFormData(task.getId());
//        Map<String,String> items = new HashMap<String,String>();
//        for (FormProperty fp : tfd.getFormProperties()) {
//            items.put(fp.getId(), fp.getValue());
//            LOGGER.trace("Task id " + task.getId() + " form variable " + fp.getId() + " = " + fp.getValue());
//        }
//        assertEquals("Username is not correct in form", "jack", items.get("userName"));
//        String role = items.get("role");
//        LOGGER.trace("Role: " + role);
////        if ("Role1".equals(role)) {
////            decision = "true";
////            comment = "Role1 OK";
////        } else if ("Role2".equals(role)) {
////            decision = "false";
////            comment = "Role2 NOT OK";
////        } else if ("Role3".equals(role)) {
////            decision = "true";
////            comment = "Role3 OK";
////        } else {
////            throw new IllegalStateException("Unknown role name: " + role);
////        }
//
//        Map<String,String> outputItems = new HashMap<String,String>();
//        outputItems.put(WfConstants.FORM_FIELD_DECISION, decision);
//        outputItems.put(AddRolesWrapper.FORM_FIELD_COMMENT, comment);
//        processEngine.getFormService().submitTaskFormData(task.getId(), outputItems);
//    }
//
//    private OperationResult createResult(String test) {
//        displayTestTile(this, "test" + test);
//        return new OperationResult(BasicTestUnused.class.getName() + ".test" + test);
//    }

}
