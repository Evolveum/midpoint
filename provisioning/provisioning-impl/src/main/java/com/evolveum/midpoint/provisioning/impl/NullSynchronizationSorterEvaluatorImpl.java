/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.provisioning.api.SynchronizationSorterEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class NullSynchronizationSorterEvaluatorImpl implements SynchronizationSorterEvaluator {

    public static final SynchronizationSorterEvaluator INSTANCE = new NullSynchronizationSorterEvaluatorImpl();

    @Override
    public @Nullable ObjectSynchronizationDiscriminatorType evaluate(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @NotNull Task task,
            @NotNull OperationResult result) {
        if (ResourceTypeUtil.getSynchronizationSorterExpression(resource) == null) {
            return null;
        } else {
            throw new UnsupportedOperationException("Synchronization sorter expression evaluation was requested, but"
                    + " no evaluator is available. In " + resource);
        }
    }
}
