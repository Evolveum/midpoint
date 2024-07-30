/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.prism.path.PathSet;

import com.evolveum.midpoint.schema.TaskExecutionMode;

import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CachedShadowsUseType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

/**
 * Overall context of the inbounds processing: system configuration, environment (time, task), operation result, and the like.
 */
public abstract class InboundsContext implements ExecutionModeProvider {

    /** The environment: context description, now (the clock), task. */
    @NotNull protected final MappingEvaluationEnvironment env;

    /** Useful Spring beans. */
    @NotNull final ModelBeans beans = ModelBeans.get();

    InboundsContext(@NotNull MappingEvaluationEnvironment env) {
        this.env = env;
    }

    abstract String getOperation();

    abstract PrismObject<SystemConfigurationType> getSystemConfiguration();

    SystemConfigurationType getSystemConfigurationBean() {
        return asObjectable(getSystemConfiguration());
    }

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

    @Override
    public @NotNull TaskExecutionMode getExecutionMode() {
        return env.task.getExecutionMode();
    }

    public @NotNull MappingEvaluationEnvironment getEnv() {
        return env;
    }
}
