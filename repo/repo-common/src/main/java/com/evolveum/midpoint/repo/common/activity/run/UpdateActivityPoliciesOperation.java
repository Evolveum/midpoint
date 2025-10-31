/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class UpdateActivityPoliciesOperation {

    private static final Trace LOGGER = TraceManager.getTrace(UpdateActivityPoliciesOperation.class);

    private final @NotNull Task task;

    private final @NotNull ItemPath policiesItemPath;

    private final @NotNull Collection<ActivityPolicyStateType> policies;

    private final @NotNull CommonTaskBeans beans;

    private final @NotNull Map<String, ActivityPolicyStateType> updatedPolicies = new HashMap<>();

    public UpdateActivityPoliciesOperation(
            @NotNull Task task,
            @NotNull ItemPath policiesItemPath,
            @NotNull Collection<ActivityPolicyStateType> policies,
            @NotNull CommonTaskBeans beans) {
        this.task = task;
        this.policiesItemPath = policiesItemPath;
        this.policies = policies;
        this.beans = beans;
    }

    public Map<String, ActivityPolicyStateType> execute(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        LOGGER.trace("Updating information about triggered policy rules in the activity state");

        updatePolicyStates(result);

        return updatedPolicies;
    }

    private void updatePolicyStates(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        beans.plainRepositoryService.modifyObjectDynamically(
                TaskType.class, task.getOid(), null, this::prepareModifications, null, result);
    }

    private @NotNull Collection<? extends ItemDelta<?, ?>> prepareModifications(TaskType task) throws SchemaException {
        updatedPolicies.clear();

        List<ItemDelta<?, ?>> deltas = new ArrayList<>();
        for (ActivityPolicyStateType policy : policies) {
            ActivityPolicyStateType existing = getCurrentPolicyState(task, policy);

            ItemDelta<?, ?> itemDelta;

            ActivityPolicyStateType updatedPolicy;
            if (existing == null) {
                itemDelta = beans.prismContext.deltaFor(TaskType.class)
                        .item(policiesItemPath)
                        .add(policy.clone())
                        .asItemDelta();

                deltas.add(itemDelta);
                updatedPolicy = policy;
            } else {
                // we don't update "similar" policy (same identifier and set of reactions)
                updatedPolicy = existing;
            }

            updatedPolicy.freeze();
            updatedPolicies.put(policy.getIdentifier(), updatedPolicy);
        }

        return deltas;
    }

    private ActivityPolicyStateType getCurrentPolicyState(TaskType task, ActivityPolicyStateType policy) {
        //noinspection unchecked
        PrismContainer<ActivityPolicyStateType> policiesContainer =
                (PrismContainer<ActivityPolicyStateType>) task.asPrismContainerValue().findItem(policiesItemPath);

        if (policiesContainer == null) {
            return null;
        }

        return policiesContainer.getRealValues().stream()
                .filter(p -> Objects.equals(p.getIdentifier(), policy.getIdentifier()))
                .findFirst()
                .orElse(null);
    }
}
