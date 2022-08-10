/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.PreInboundsContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

/**
 * A {@link Context} for inbound mappings evaluation that is used in "pre-inbounds" evaluation (i.e., before clockwork is run).
 */
public class PreContext extends Context {

    @NotNull final PreInboundsContext<?> ctx;

    public PreContext(
            @NotNull PreInboundsContext<?> ctx,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result,
            @NotNull ModelBeans beans) {
        super(env, result, beans);
        this.ctx = ctx;
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
}
