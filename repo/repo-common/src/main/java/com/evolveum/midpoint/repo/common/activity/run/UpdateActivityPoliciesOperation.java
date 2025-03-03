/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import java.util.Collection;
import java.util.Map;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;

import org.jetbrains.annotations.NotNull;

public class UpdateActivityPoliciesOperation {

    private @NotNull Task task;

    private @NotNull ItemPath policiesItemPath;

    private @NotNull Collection<ActivityPolicyStateType> policies;

    private @NotNull CommonTaskBeans beans;

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

    public Map<String, ActivityPolicyStateType> execute(OperationResult result) {
        // todo MID-10412 implement
        return null;
    }
}
