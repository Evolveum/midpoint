/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import java.util.*;
import java.util.stream.Collectors;

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

        LOGGER.trace("Updating activity policies");

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
            if (policy.getReaction().isEmpty()) {
                // policy without reactions will not be stored
                // (e.g., triggered constraint that updated counter, for example, but did not match any reaction)
                continue;
            }

            ActivityPolicyStateType current = getCurrentPolicyState(task, policy);

            ItemDelta<?, ?> itemDelta;

            ActivityPolicyStateType updatedPolicy;
            if (current == null) {
                itemDelta = beans.prismContext.deltaFor(TaskType.class)
                        .item(policiesItemPath)
                        .add(policy.clone())
                        .asItemDelta();

                deltas.add(itemDelta);
                updatedPolicy = policy;
            } else {
                // we don't update "similar" policy (same identifier and set of reactions)
                updatedPolicy = current;
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

        for (ActivityPolicyStateType storedPolicy : policiesContainer.getRealValues()) {
            // first, we match policy by identifier
            if (!Objects.equals(storedPolicy.getIdentifier(), policy.getIdentifier())) {
                continue;
            }

            // then, we check if the set of reactions is the same
            if (computeReactionsRefs(policy).equals(computeReactionsRefs(storedPolicy))) {
                return storedPolicy;
            }
        }

        return null;
    }

    private Set<String> computeReactionsRefs(ActivityPolicyStateType policy) {
        return policy.getReaction().stream()
                .map(r -> r.getRef())
                .collect(Collectors.toSet());
    }
}
