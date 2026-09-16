/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.common.AbstractRepoCommonTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests activities with the task manager running in clustered mode (i.e. with the JDBC Quartz job store).
 *
 * The clustered mode brings specific timing conditions: Quartz triggers live in the database and can fire
 * concurrently, so races that are invisible with the in-memory job store can appear here. See issue #11928.
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@TestPropertySource(properties = {
        "midpoint.taskManager.clustered=true",
        "midpoint.nodeId=node1",
        "midpoint.taskManager.jdbcJobStore=true"
})
@DirtiesContext
public class TestActivitiesClustered extends AbstractRepoCommonTest {

    /**
     * Issue #11928: ordered composition of activities delegated to subtasks must complete even in clustered mode.
     *
     * Each composition child is delegated to its own subtask, so the root task goes through several
     * wait-unpause cycles. Each of them is an opportunity for a scheduling race: the duplicate fire-now
     * trigger created when unpausing the root led to two concurrent runs of the root task, which then
     * collided in {@link com.evolveum.midpoint.repo.common.activity.run.DelegatingActivityRun}
     * ("Child ... is not closed") or left the tree frozen.
     */
    @Test
    public void test100CompositionWithSubtasks() throws Exception {
        given();
        Task testTask = getTestTask();
        OperationResult result = testTask.getResult();

        ActivityCompositionType composition = new ActivityCompositionType();
        for (int i = 1; i <= 4; i++) {
            composition.activity(noOpActivity(i));
        }
        TaskType task = new TaskType()
                .name("composition-with-subtasks")
                .ownerRef(SystemObjectsType.USER_ADMINISTRATOR.value(), UserType.COMPLEX_TYPE)
                .executionState(TaskExecutionStateType.RUNNABLE)
                .activity(new ActivityDefinitionType()
                        .composition(composition)
                        .distribution(new ActivityDistributionDefinitionType()
                                .subtasks(new ActivitySubtaskDefinitionType())));

        when();
        String oid = taskManager.addTask(task.asPrismObject(), result);
        waitForTaskTreeCloseCheckingSuspensionWithError(oid, result, 30000);

        then();
        var taskTree = assertTaskTree(oid, "after").display();
        assertThat(fatalTaskErrors(taskTree.getObject().asObjectable()))
                .as("fatal task errors")
                .isEmpty();
        taskTree.assertSuccess().assertClosed();
    }

    private ActivityDefinitionType noOpActivity(int order) {
        return new ActivityDefinitionType()
                .order(order)
                .work(new WorkDefinitionsType()
                        .noOp(new NoOpWorkDefinitionType()
                                .steps(1)
                                .delay(0)));
    }

    private List<String> fatalTaskErrors(TaskType task) {
        List<String> errors = new ArrayList<>();
        collectFatalTaskErrors(task.getActivityState().getTree().getActivity(), new ArrayList<>());

        return errors;
    }

    private void collectFatalTaskErrors(ActivityStateOverviewType activity, List<String> errors) {
        activity.getTask().stream()
                .filter(t -> t.getResultStatus() == OperationResultStatusType.FATAL_ERROR)
                .map(ActivityTaskStateOverviewType::getMessage)
                .forEach(errors::add);
        activity.getActivity().forEach(child -> collectFatalTaskErrors(child, errors));
    }
}
