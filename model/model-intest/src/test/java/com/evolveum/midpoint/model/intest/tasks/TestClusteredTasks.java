/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@TestPropertySource(properties = {
        "midpoint.taskManager.clustered=true",
        "midpoint.nodeId=node1",
        "midpoint.taskManager.jdbcJobStore=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestClusteredTasks extends AbstractInitializedModelIntegrationTest {

    /** MID-11928: ordered reconciliation subtasks must not be restarted before their children are closed. */
    @Test
    public void testCompositeReconciliationsWithSubtasks() throws Exception {
        Task testTask = getTestTask();
        OperationResult result = testTask.getResult();
        TaskType task = new TaskType()
                .name("MID-11928")
                .ownerRef(SystemObjectsType.USER_ADMINISTRATOR.value(), UserType.COMPLEX_TYPE)
                .executionState(TaskExecutionStateType.RUNNABLE)
                .binding(TaskBindingType.LOOSE)
                .activity(new ActivityDefinitionType()
                        .composition(new ActivityCompositionType()
                                .activity(reconciliationActivity(1))
                                .activity(reconciliationActivity(2)))
                        .distribution(new ActivityDistributionDefinitionType()
                                .subtasks(new ActivitySubtaskDefinitionType())));

        String oid = addObjectSilently(task, testTask, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(oid, result, 30000);

        var taskTree = assertTaskTree(oid, "after").display();
        assertThat(fatalTaskErrors(taskTree.getObject().asObjectable()))
                .as("fatal task errors")
                .isEmpty();
        taskTree.assertSuccess().assertClosed();
    }

    private List<String> fatalTaskErrors(TaskType task) {
        List<String> errors = new ArrayList<>();
        collectFatalTaskErrors(task.getActivityState().getTree().getActivity(), errors);
        return errors;
    }

    private void collectFatalTaskErrors(ActivityStateOverviewType activity, List<String> errors) {
        activity.getTask().stream()
                .filter(task -> task.getResultStatus() == OperationResultStatusType.FATAL_ERROR)
                .map(ActivityTaskStateOverviewType::getMessage)
                .forEach(errors::add);
        activity.getActivity().forEach(child -> collectFatalTaskErrors(child, errors));
    }

    private ActivityDefinitionType reconciliationActivity(int order) {
        return new ActivityDefinitionType()
                .order(order)
                .work(new WorkDefinitionsType()
                        .reconciliation(new ReconciliationWorkDefinitionType()
                                .resourceObjects(new ResourceObjectSetType()
                                        .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                                        .kind(ShadowKindType.ACCOUNT))));
    }
}
