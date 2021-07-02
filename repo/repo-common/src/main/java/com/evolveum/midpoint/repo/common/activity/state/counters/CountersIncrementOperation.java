/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.state.counters;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.*;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCounterType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCounterGroupType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.Countable;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Represents/carries out an execution of a set of updates of a counter group in an activity.
 */
public class CountersIncrementOperation {

    private static final Trace LOGGER = TraceManager.getTrace(CountersIncrementOperation.class);

    /** Task that hosts the counter group. */
    @NotNull private final Task task;

    /** Points directly to multi-valued "counter" sub-container in the group. */
    @NotNull private final ItemPath countersItemPath;

    /** In-memory representation of the counters. */
    @NotNull private final List<? extends Countable> countables;

    /** Useful beans */
    @NotNull private final CommonTaskBeans beans;

    /** Real values that got into the repository. These are needed to update the in-memory counters. */
    private final Map<String, Integer> updatedValues = new HashMap<>();

    public CountersIncrementOperation(@NotNull Task task, @NotNull ItemPath counterGroupItemPath,
            @NotNull List<? extends Countable> countables,
            @NotNull CommonTaskBeans beans) {
        this.task = task;
        this.countersItemPath = counterGroupItemPath.append(ActivityCounterGroupType.F_COUNTER);
        this.countables = countables;
        this.beans = beans;
    }

    public void execute(OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        incrementCountersInRepository(result);
        updateCountables();
    }

    private void incrementCountersInRepository(OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        beans.plainRepositoryService.modifyObjectDynamically(TaskType.class, task.getOid(), null, this::prepareModifications,
                null, result);
    }

    private @NotNull Collection<? extends ItemDelta<?, ?>> prepareModifications(TaskType task) throws SchemaException {
        updatedValues.clear();
        List<ItemDelta<?, ?>> deltas = new ArrayList<>();
        for (Countable countable : countables) {
            String identifier = countable.getIdentifier();
            ActivityCounterType currentCounter = getCurrentCounter(task, identifier);
            ItemDelta<?, ?> itemDelta;
            int newValue;
            if (currentCounter != null) {
                newValue = or0(currentCounter.getValue()) + 1;
                itemDelta = beans.prismContext.deltaFor(TaskType.class)
                        .item(countersItemPath.append(currentCounter.getId(), ActivityCounterType.F_VALUE))
                        .replace(newValue)
                        .asItemDelta();
            } else {
                newValue = 1;
                itemDelta = beans.prismContext.deltaFor(TaskType.class)
                        .item(countersItemPath)
                        .add(new ActivityCounterType(beans.prismContext)
                                .identifier(identifier)
                                .value(newValue))
                        .asItemDelta();
            }
            deltas.add(itemDelta);
            System.out.println("New value for " + identifier + " is " + newValue); // TODO remove
            updatedValues.put(identifier, newValue);
        }
        LOGGER.info("Counter deltas:\n{}", DebugUtil.debugDumpLazily(deltas, 1)); // TODO trace
        return deltas;
    }

    private ActivityCounterType getCurrentCounter(TaskType task, String identifier) {
        //noinspection unchecked
        PrismContainer<ActivityCounterType> counterContainer =
                (PrismContainer<ActivityCounterType>) task.asPrismContainerValue().findItem(countersItemPath);
        if (counterContainer == null) {
            return null;
        }
        for (ActivityCounterType counter : counterContainer.getRealValues()) {
            if (Objects.equals(identifier, counter.getIdentifier())) {
                return counter;
            }
        }
        return null;
    }

    private void updateCountables() {
        for (Countable countable : countables) {
            int updatedValue = requireNonNull(
                    updatedValues.get(countable.getIdentifier()),
                    "Missing updated value for " + countable.getIdentifier());
            countable.setCount(updatedValue);
        }
    }
}
