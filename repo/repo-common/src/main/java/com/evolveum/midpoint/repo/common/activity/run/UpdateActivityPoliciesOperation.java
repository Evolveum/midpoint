/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import java.util.*;

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

import org.jetbrains.annotations.NotNull;

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
            ActivityPolicyStateType current = getCurrentPolicyState(task, policy.getIdentifier());
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
                // MID-10412 todo if policy state exists we probably don't want to update it??? (it should already contain triggers)
                //  this looks shady
                updatedPolicy = current;
            }

            updatedPolicy.freeze();
            updatedPolicies.put(policy.getIdentifier(), updatedPolicy);
        }

        return deltas;
    }

    private ActivityPolicyStateType getCurrentPolicyState(TaskType task, String identifier) {
        //noinspection unchecked
        PrismContainer<ActivityPolicyStateType> policiesContainer =
                (PrismContainer<ActivityPolicyStateType>) task.asPrismContainerValue().findItem(policiesItemPath);

        if (policiesContainer == null) {
            return null;
        }

        for (ActivityPolicyStateType policy : policiesContainer.getRealValues()) {
            if (Objects.equals(identifier, policy.getIdentifier())) {
                return policy;
            }
        }

        return null;
    }
}
