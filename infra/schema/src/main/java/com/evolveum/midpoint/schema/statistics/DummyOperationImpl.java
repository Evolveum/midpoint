/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

import org.jetbrains.annotations.NotNull;

/**
 * A no-op implementation of {@link Operation} used when there's nowhere to record the execution to.
 */
public class DummyOperationImpl implements Operation {

    @NotNull private final IterativeOperationStartInfo info;

    public DummyOperationImpl(@NotNull IterativeOperationStartInfo info) {
        this.info = info;
    }

    @Override
    public void done(QualifiedItemProcessingOutcomeType outcome, Throwable exception) {
    }

    @Override
    public double getDurationRounded() {
        return 0;
    }

    @Override
    public long getEndTimeMillis() {
        return 0;
    }

    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        return info.getItem();
    }

    @Override
    public @NotNull IterativeOperationStartInfo getStartInfo() {
        return info;
    }
}
