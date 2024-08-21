/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessing;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessingContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

/**
 * A {@link InboundsContext} for inbound mappings evaluation that is used in {@link SingleShadowInboundsProcessing}.
 */
public class LimitedInboundsContext extends InboundsContext {

    @NotNull private final SingleShadowInboundsProcessingContext<?> ctx;

    @NotNull private final PathSet correlationItemPaths;

    public LimitedInboundsContext(
            @NotNull SingleShadowInboundsProcessingContext<?> ctx,
            @NotNull PathSet correlationItemPaths,
            @NotNull MappingEvaluationEnvironment env) {
        super(env);
        this.ctx = ctx;
        this.correlationItemPaths = correlationItemPaths;
    }

    @Override
    String getOperation() {
        return null; // Some day we may set this...
    }

    @Override
    PrismObject<SystemConfigurationType> getSystemConfiguration() {
        return asPrismObject(ctx.getSystemConfiguration());
    }

    @Override
    ConfigurableValuePolicySupplier createValuePolicySupplier() {
        return null; // Not available if there's no focus.
    }

    @Override
    public @NotNull PathSet getCorrelationItemPaths() {
        return correlationItemPaths;
    }
}
