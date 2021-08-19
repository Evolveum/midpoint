/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work.workers;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.template.StringSubstitutorUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Describes how the workers should like.
 *
 * Key elements are {@link #workers} and {@link #workersConfiguration}.
 */
class ExpectedSetup {

    @NotNull private final Activity<?, ?> activity;
    @NotNull private final WorkersManagementType workersConfigBean;
    @NotNull private final CommonTaskBeans beans;
    @NotNull private final Task coordinatorTask;
    @NotNull private final Task rootTask;

    /**
     * A collection of expected workers, characterized by group + name + scavenger flag.
     */
    @NotNull private final Set<WorkerCharacterization> workers = new HashSet<>();

    @NotNull private final Map<WorkerCharacterization, WorkerTasksPerNodeConfigurationType> workersConfiguration = new HashMap<>();

    private ExpectedSetup(
            @NotNull Activity<?, ?> activity,
            @NotNull WorkersManagementType workersConfigBean,
            @NotNull CommonTaskBeans beans,
            @NotNull Task coordinatorTask,
            @NotNull Task rootTask) {
        this.activity = activity;
        this.workersConfigBean = workersConfigBean;
        this.beans = beans;
        this.coordinatorTask = coordinatorTask;
        this.rootTask = rootTask;
    }

    static ExpectedSetup create(@NotNull Activity<?, ?> activity, @NotNull WorkersManagementType workersConfigBean,
            @NotNull CommonTaskBeans beans, @NotNull Task coordinatorTask, @NotNull Task rootTask,
            @NotNull OperationResult result) {
        ExpectedSetup setup = new ExpectedSetup(activity, workersConfigBean, beans, coordinatorTask, rootTask);
        setup.initialize(result);
        return setup;
    }

    private void initialize(OperationResult result) {
        Lazy<Collection<String>> allRunningNodesLazy = Lazy.from(() -> getAllRunningNodes(result));
        for (WorkerTasksPerNodeConfigurationType perNodeConfig : getWorkersPerNode()) {
            for (String nodeIdentifier : getNodeIdentifiers(perNodeConfig, allRunningNodesLazy)) {
                int count = defaultIfNull(perNodeConfig.getCount(), 1);
                int scavengers = defaultIfNull(perNodeConfig.getScavengers(), 1);
                for (int index = 1; index <= count; index++) {
                    WorkerCharacterization characterization = createWorkerCharacterization(nodeIdentifier, index, perNodeConfig,
                            index <= scavengers);
                    workers.add(characterization);
                    workersConfiguration.put(characterization, perNodeConfig);
                }
            }
        }
    }

    private Collection<String> getAllRunningNodes(OperationResult result) {
        try {
            return beans.taskManager.determineClusterState(result)
                    .getNodeUp();
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
    }

    private WorkerCharacterization createWorkerCharacterization(String nodeIdentifier, int index,
            WorkerTasksPerNodeConfigurationType perNodeConfig, boolean scavenger) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("node", nodeIdentifier);
        replacements.put("index", String.valueOf(index));
        replacements.put("activity", activity.isRoot() ? "root activity" : "activity '" + activity.getPath() + "'"); // TODO i18n
        replacements.put("coordinatorTaskName", coordinatorTask.getName().getOrig());
        replacements.put("coordinatorTaskOid", coordinatorTask.getOid());
        replacements.put("rootTaskName", rootTask.getName().getOrig());
        replacements.put("rootTaskOid", rootTask.getOid());

        String nameTemplate;
        if (perNodeConfig.getTaskName() != null) {
            nameTemplate = perNodeConfig.getTaskName();
        } else if (workersConfigBean.getTaskName() != null) {
            nameTemplate = workersConfigBean.getTaskName();
        } else {
            nameTemplate = "Worker {node}:{index} for {activity} in {rootTaskName}";
        }

        String name = StringSubstitutorUtil.simpleExpand(nameTemplate, replacements);

        String executionGroupTemplate = defaultIfNull(perNodeConfig.getExecutionGroup(), "{node}");
        String executionGroup = MiscUtil.nullIfEmpty(StringSubstitutorUtil.simpleExpand(executionGroupTemplate, replacements));

        return WorkerCharacterization.forParameters(executionGroup, name, scavenger);
    }

    private List<WorkerTasksPerNodeConfigurationType> getWorkersPerNode() {
        if (!workersConfigBean.getWorkersPerNode().isEmpty()) {
            return workersConfigBean.getWorkersPerNode();
        } else {
            return List.of(new WorkerTasksPerNodeConfigurationType(PrismContext.get()));
        }
    }

    private Collection<String> getNodeIdentifiers(WorkerTasksPerNodeConfigurationType perNodeConfig,
            Lazy<Collection<String>> allNodesLazy) {
        if (!perNodeConfig.getNodeIdentifier().isEmpty()) {
            return perNodeConfig.getNodeIdentifier();
        } else {
            return allNodesLazy.get();
        }
    }

    @NotNull Set<WorkerCharacterization> getWorkers() {
        return workers;
    }

    @NotNull Map<WorkerCharacterization, WorkerTasksPerNodeConfigurationType> getWorkersConfiguration() {
        return workersConfiguration;
    }
}
