/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Task partitions definition based on statically configured values. More restricted but easier to use.
 * However, it is extensible by subclassing - in that way, some values can be provided statically while others on-demand.
 *
 * @author mederly
 */
public class StaticTaskPartitionsDefinition implements TaskPartitionsDefinition {

    @NotNull private final TaskPartitionsDefinitionType data;
    @NotNull private final List<TaskPartitionDefinition> partitions;
    @NotNull private final PrismObjectDefinition<TaskType> taskDefinition;

    public StaticTaskPartitionsDefinition(@NotNull TaskPartitionsDefinitionType data,
            @Nullable List<TaskPartitionDefinition> partitionsOverride, @NotNull PrismObjectDefinition<TaskType> taskDefinition) {
        this.data = data;
        this.partitions = partitionsOverride != null ? partitionsOverride : createPartitionDefinitions(data);
        this.taskDefinition = taskDefinition;
    }

    public StaticTaskPartitionsDefinition(@NotNull TaskPartitionsDefinitionType definition,
            @NotNull PrismObjectDefinition<TaskType> taskDefinition) {
        this(definition, null, taskDefinition);
    }

    private List<TaskPartitionDefinition> createPartitionDefinitions(TaskPartitionsDefinitionType data) {
        int count;
        if (data.getCount() != null) {
            count = data.getCount();
            if (count < data.getPartition().size()) {
                throw new SystemException("There are more partitions defined (" + data.getPartition() + ") than declared"
                        + " by partition count item (" + count + ")");
            }
        } else {
            count = data.getPartition().size();
        }
        List<TaskPartitionDefinition> rv = new ArrayList<>(Collections.nCopies(count, null));
        boolean hasNumbered = false;
        int lastUnnumbered = 0;
        for (TaskPartitionDefinitionType definition : data.getPartition()) {
            Integer index = definition.getIndex();
            if (index != null) {
                hasNumbered = true;
                if (rv.get(index-1) != null) {
                    throw new SystemException("Multiple partitions definitions with index=" + index);
                }
                rv.set(index-1, new StaticTaskPartition(definition));
            } else {
                rv.set(lastUnnumbered++, new StaticTaskPartition(definition));
            }
            if (hasNumbered && lastUnnumbered > 0) {
                throw new SystemException("Both numbered and unnumbered partition definitions found");
            }
        }
        for (int i = 0; i < rv.size(); i++) {
            if (rv.get(i) == null) {
                rv.set(i, new StaticTaskPartition(new TaskPartitionDefinitionType()));
            }
        }
        return rv;
    }

    @Override
    public int getCount(Task masterTask) {
        if (data.getCount() != null) {
            return data.getCount();
        } else {
            return partitions.size();
        }
    }

    @Override
    public boolean isSequentialExecution(Task masterTask) {
        return data.isSequentialExecution() != null ? data.isSequentialExecution() : true;
    }

    @Override
    public boolean isDurablePartitions(Task masterTask) {
        return Boolean.TRUE.equals(data.isDurablePartitions());
    }

    @Override
    public String getName(Task masterTask) {
        return data.getTaskName();
    }

    @Override
    public String getHandlerUri(Task masterTask) {
        return data.getHandlerUri();
    }

    @Override
    public TaskWorkManagementType getWorkManagement(Task masterTask) {
        return data.getWorkManagement();
    }

    @Override
    public TaskExecutionEnvironmentType getExecutionEnvironment(Task masterTask) {
        return data.getExecutionEnvironment();
    }

    @Override
    public Boolean isCopyMasterExtension(Task masterTask) {
        return data.isCopyMasterExtension();
    }

    @Override
    @NotNull
    public Collection<ItemDelta<?, ?>> getOtherDeltas(Task masterTask) {
        return parseDeltas(data.getOtherDeltas());
    }

    private Collection<ItemDelta<?, ?>> parseDeltas(List<ItemDeltaType> deltas) {
        try {
            //noinspection unchecked
            return (Collection<ItemDelta<?, ?>>) DeltaConvertor.toModifications(deltas, taskDefinition);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't parse task item deltas: " + e.getMessage(), e);
        }
    }

    public class StaticTaskPartition implements TaskPartitionDefinition {

        @NotNull private final TaskPartitionDefinitionType data;

        public StaticTaskPartition(@NotNull TaskPartitionDefinitionType data) {
            this.data = data;
        }

        @Override
        public String getName(Task masterTask) {
            return data.getTaskName();
        }

        @Override
        public String getHandlerUri(Task masterTask) {
            return data.getHandlerUri();
        }

        @Override
        public TaskWorkManagementType getWorkManagement(Task masterTask) {
            return data.getWorkManagement();
        }

        @Override
        public TaskExecutionEnvironmentType getExecutionEnvironment(Task masterTask) {
            return data.getExecutionEnvironment();
        }

        @Override
        public ExtensionType getExtension(Task masterTask) {
            return data.getExtension();
        }

        @Override
        public Boolean isCopyMasterExtension(Task masterTask) {
            return data.isCopyMasterExtension();
        }

        @Override
        @NotNull
        public Collection<ItemDelta<?, ?>> getOtherDeltas(Task masterTask) {
            return parseDeltas(data.getOtherDeltas());
        }

        @Override
        @NotNull
        public Collection<Integer> getDependents() {
            return data.getDependents();
        }
    }

    @NotNull
    @Override
    public TaskPartitionDefinition getPartition(Task masterTask, int index) {
        return partitions.get(index - 1);
    }
}
