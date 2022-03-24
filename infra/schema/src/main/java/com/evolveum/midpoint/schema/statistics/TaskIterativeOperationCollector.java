/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * Support for recording iterative operations directly on tasks - where activities are not available.
 */
public interface TaskIterativeOperationCollector {

    /**
     * Records the start of iterative operation.
     * The operation end is recorded by calling appropriate method on the returned object.
     */
    @NotNull
    default Operation recordIterativeOperationStart(PrismObject<? extends ObjectType> object) {
        return recordIterativeOperationStart(new IterationItemInformation(object));
    }

    /**
     * Records the start of iterative operation.
     * The operation end is recorded by calling appropriate method on the returned object.
     */
    @NotNull default Operation recordIterativeOperationStart(IterationItemInformation info) {
        IterativeOperationStartInfo startInfo = new IterativeOperationStartInfo(info);
        startInfo.setSimpleCaller(true);
        return recordIterativeOperationStart(startInfo);
    }

    /**
     * Records the start of iterative operation.
     * The operation end is recorded by calling appropriate method on the returned object.
     */
    @NotNull Operation recordIterativeOperationStart(@NotNull IterativeOperationStartInfo info);
}
