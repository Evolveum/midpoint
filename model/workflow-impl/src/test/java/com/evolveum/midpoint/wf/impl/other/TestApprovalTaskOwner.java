/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.other;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.midpoint.schema.util.ObjectQueryUtil;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestApprovalTaskOwner extends AbstractWfTestPolicy {

    private static final File TEST_RESOURCE_DIR = new File("src/test/resources/approval-task-owner");

    private static final File FILE_USER_MANAGER = new File(TEST_RESOURCE_DIR, "user-manager.xml");
    private static final String USER_MANAGER_NAME = "manager";
    private static final String USER_MANAGER_OID = "00000000-0000-0000-6d6e-000000000002";

    private static final File FILE_ROLE_META_APPROVE_MANAGER = new File(TEST_RESOURCE_DIR, "metarole-approve-manager.xml");
    private static final File FILE_ROLE_DUELLING_CLASSROOM_TEACHER = new File(TEST_RESOURCE_DIR, "role-duelling-classroom-teacher.xml");
    private static final String ROLE_DUELLING_CLASSROOM_TEACHER_OID = "00000000-726c-0000-0000-111111111777";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(FILE_USER_MANAGER);
        repoAddObjectFromFile(FILE_ROLE_META_APPROVE_MANAGER, initResult);
        repoAddObjectFromFile(FILE_ROLE_DUELLING_CLASSROOM_TEACHER, initResult);
    }

    String caseOid;

    @Test
    public void test100assignRoleJack() throws Exception {
        displayTestTitle(getTestName());

        given();
        login(USER_MANAGER_NAME);

        Task task = getTestTask();
        task.setOwner(getObject(UserType.class, USER_MANAGER_OID));

        OperationResult result = task.getResult();

        when();
        assignRole(USER_JACK.oid, ROLE_DUELLING_CLASSROOM_TEACHER_OID, task, result);

        then();
        display("RESULT: \n" + result.debugDump());
        caseOid = result.findCaseOid();
        assertThat(caseOid).isNotNull();

    }

    @Test
    public void test110approveRoleJack() throws Exception {
        displayTestTitle(getTestName());

        given();
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        approveCase(task, result);
        PrismObject<CaseType> rootCase = getObject(CaseType.class, caseOid);
        testHelper.waitForCaseClose(rootCase.asObjectable(), 60000);

        then();
        PrismObject<TaskType> executionTask = resolveExecutionTask(caseOid, task, result);

        assertTask(executionTask.asObjectable(), "execution task after")
                .display()
                .assertArchetypeRef(SystemObjectsType.ARCHETYPE_APPROVAL_TASK.value())
                .owner()
                .assertOid(USER_MANAGER_OID);

        //assert assigned role
        assertUserAfter(USER_JACK.oid).assignments().assertRole(ROLE_DUELLING_CLASSROOM_TEACHER_OID);
    }

    @Test
    public void test200unassignJackAssignment() throws Exception {
        displayTestTitle(getTestName());

        given();
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(USER_JACK.oid, ROLE_DUELLING_CLASSROOM_TEACHER_OID, task, result);

        then();
        assertUserAfter(USER_JACK.oid).assignments().assertNoRole(ROLE_DUELLING_CLASSROOM_TEACHER_OID);
    }

    @Test
    public void test300assignRoleJack() throws Exception {
        displayTestTitle(getTestName());

        given();
        login(USER_MANAGER_NAME);
        Task task = getTestTask();
        task.setOwner(getObject(UserType.class, USER_MANAGER_OID));
        OperationResult result = task.getResult();

        ObjectDelta<SystemConfigurationType> systemConfig = prismContext.deltaFor(SystemConfigurationType.class)
                .item(ItemPath.create(SystemConfigurationType.F_WORKFLOW_CONFIGURATION, WfConfigurationType.F_EXECUTION_TASKS, WfExecutionTasksConfigurationType.F_OWNER_REF))
                .add(ObjectTypeUtil.createObjectRef(USER_ADMINISTRATOR_OID, ObjectTypes.USER))
                .asObjectDelta(SystemObjectsType.SYSTEM_CONFIGURATION.value());
        executeChanges(systemConfig, null, task, result);

        when();
        assignRole(USER_JACK.oid, ROLE_DUELLING_CLASSROOM_TEACHER_OID, task, result);

        then();
        caseOid = result.findCaseOid();
        assertThat(caseOid).isNotNull();
    }

    @Test
    public void test310approveRoleAdministrator() throws Exception {
        displayTestTitle(getTestName());

        given();
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        approveCase(task, result);
        PrismObject<CaseType> rootCase = getObject(CaseType.class, caseOid);
        testHelper.waitForCaseClose(rootCase.asObjectable(), 60000);

        then();
        PrismObject<TaskType> executionTask = resolveExecutionTask(caseOid, task, result);
        assertTask(executionTask.asObjectable(), "execution task after")
                .display()
                .assertArchetypeRef(SystemObjectsType.ARCHETYPE_APPROVAL_TASK.value())
                .owner()
                .assertOid(USER_ADMINISTRATOR_OID);

        assertUserAfter(USER_JACK.oid).assignments().assertRole(ROLE_DUELLING_CLASSROOM_TEACHER_OID);
    }

    private void approveCase(Task task, OperationResult result) throws Exception {
        SearchResultList<CaseWorkItemType> caseWorkItems = modelService.searchContainers(CaseWorkItemType.class, ObjectQueryUtil.openItemsQuery(), null, task, result);
        assertThat(caseWorkItems.size()).isEqualTo(1);

        CaseWorkItemType caseWorkItemType = caseWorkItems.get(0);
        display("CASEWI: \n" + caseWorkItemType.debugDump());

        AbstractWorkItemOutputType workItemOutputType = ApprovalUtils.createApproveOutput();

        caseManager.completeWorkItem(WorkItemId.of(caseWorkItemType), workItemOutputType, null, task, result);
    }

    private PrismObject<TaskType> resolveExecutionTask(String caseOid, Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery taskQuery = queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(caseOid)
                .build();
        return MiscUtil.extractSingletonRequired(
                modelService.searchObjects(TaskType.class, taskQuery, null, task, result),
                () -> new IllegalStateException("More execution tasks for case " + caseOid),
                () -> new IllegalStateException("No execution task for case " + caseOid));
    }

}
