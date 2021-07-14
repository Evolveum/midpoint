/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work.workers;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.template.StringSubstitutorUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

class ExpectedWorkersSetup {

    @NotNull private final Activity<?, ?> activity;
    @NotNull private final WorkersManagementType workersConfigBean;
    @NotNull private final CommonTaskBeans beans;
    @NotNull private final Task coordinatorTask;
    @NotNull private final Task rootTask;

    @NotNull private final Map<WorkerSpec, WorkerTasksPerNodeConfigurationType> perNodeConfigurationMap = new HashMap<>();

    @NotNull private final MultiValuedMap<String, WorkerSpec> workersMap = new ArrayListValuedHashMap<>();

    private ExpectedWorkersSetup(
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

    static ExpectedWorkersSetup create(@NotNull Activity<?, ?> activity, @NotNull WorkersManagementType workersConfigBean,
            @NotNull CommonTaskBeans beans, @NotNull Task coordinatorTask, @NotNull Task rootTask,
            @NotNull OperationResult result) throws SchemaException {
        ExpectedWorkersSetup setup = new ExpectedWorkersSetup(activity, workersConfigBean, beans, coordinatorTask, rootTask);
        setup.initialize(result);
        return setup;
    }

    private void initialize(OperationResult result)
            throws SchemaException {
        for (WorkerTasksPerNodeConfigurationType perNodeConfig : getWorkersPerNode()) {
            for (String nodeIdentifier : getRunningNodesIdentifiers(perNodeConfig, result)) {
                int count = defaultIfNull(perNodeConfig.getCount(), 1);
                int scavengers = defaultIfNull(perNodeConfig.getScavengers(), 1);
                for (int index = 1; index <= count; index++) {
                    WorkerSpec key = createWorkerKey(nodeIdentifier, index, perNodeConfig, index <= scavengers);
                    workersMap.put(key.group, key);
                    perNodeConfigurationMap.put(key, perNodeConfig);
                }
            }
        }
    }

    private WorkerSpec createWorkerKey(String nodeIdentifier, int index, WorkerTasksPerNodeConfigurationType perNodeConfig,
            boolean scavenger) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("node", nodeIdentifier);
        replacements.put("index", String.valueOf(index));
        replacements.put("activity", activity.isRoot() ? "root activity" : "activity '" + activity.getPath() + "'");
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

        return new WorkerSpec(executionGroup, name, scavenger);
    }

    private List<WorkerTasksPerNodeConfigurationType> getWorkersPerNode() {
        if (!workersConfigBean.getWorkersPerNode().isEmpty()) {
            return workersConfigBean.getWorkersPerNode();
        } else {
            return List.of(new WorkerTasksPerNodeConfigurationType(PrismContext.get()));
        }
    }

    private Collection<String> getRunningNodesIdentifiers(WorkerTasksPerNodeConfigurationType perNodeConfig,
            OperationResult result)
            throws SchemaException {
        if (!perNodeConfig.getNodeIdentifier().isEmpty()) {
            return perNodeConfig.getNodeIdentifier();
        } else {
            SearchResultList<PrismObject<NodeType>> nodes =
                    beans.taskManager.searchObjects(NodeType.class, null, null, result);
            return nodes.stream()
                    .filter(n -> n.asObjectable().getExecutionState() == NodeExecutionStateType.RUNNING)
                    .map(n -> n.asObjectable().getNodeIdentifier())
                    .collect(Collectors.toSet());
        }
    }

    @NotNull MultiValuedMap<String, WorkerSpec> getWorkersMap() {
        return workersMap;
    }

    @NotNull Map<WorkerSpec, WorkerTasksPerNodeConfigurationType> getPerNodeConfigurationMap() {
        return perNodeConfigurationMap;
    }
}
