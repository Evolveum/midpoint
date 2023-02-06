/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.prism.path.PathSet;

import com.evolveum.midpoint.schema.TaskExecutionMode;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Overall context of the mappings preparation: environment, result, beans.
 */
abstract class Context {

    /** The environment: context description, now (the clock), task. */
    @NotNull protected final MappingEvaluationEnvironment env;

    /** The operation result. (Beware: Do *not* create subresults from it here unless certainly knowing what you're doing!) */
    @NotNull protected final OperationResult result;

    /** Useful Spring beans. */
    @NotNull final ModelBeans beans;

    Context(@NotNull MappingEvaluationEnvironment env, @NotNull OperationResult result, @NotNull ModelBeans beans) {
        this.env = env;
        this.result = result;
        this.beans = beans;
    }

    abstract String getOperation();

    abstract PrismObject<SystemConfigurationType> getSystemConfiguration();

    /**
     * Provides value policy when needed (e.g. in `generate` expression evaluator).
     * Can be reasonable provided only in the case of clockwork processing, as the focus should be known.
     */
    abstract ConfigurableValuePolicySupplier createValuePolicySupplier();

    /**
     * Returns paths of focus items mentioned in the "items" correlators.
     * They should have their inbound mappings evaluated in beforeCorrelation state (by default).
     *
     * Should return empty set during clockwork-time processing.
     */
    public abstract @NotNull PathSet getCorrelationItemPaths();

    @NotNull TaskExecutionMode getTaskExecutionMode() {
        return env.task.getExecutionMode();
    }
}
