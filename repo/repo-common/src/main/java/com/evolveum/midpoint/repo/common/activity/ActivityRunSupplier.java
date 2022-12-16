/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

/**
 * Creates (typed) {@link AbstractActivityRun} objects for given activity.
 *
 * - For standalone activities it is usually the {@link ActivityHandler} itself.
 * - For embedded activities it is usually a custom piece of code used when defining a child activity.
 */
@FunctionalInterface
public interface ActivityRunSupplier<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> {

    AbstractActivityRun<WD, AH, ?> createActivityRun(@NotNull ActivityRunInstantiationContext<WD, AH> context,
            @NotNull OperationResult result);
}
