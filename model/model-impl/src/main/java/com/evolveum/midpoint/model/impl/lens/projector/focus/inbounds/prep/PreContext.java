/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.NotNull;

public class PreContext extends Context {

    @NotNull final SynchronizationContext<?> syncCtx;

    public PreContext(
            @NotNull SynchronizationContext<?> syncCtx,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result,
            @NotNull ModelBeans beans) {
        super(env, result, beans);
        this.syncCtx = syncCtx;
    }

    @Override
    String getOperation() {
        return null; // Some day we may set this...
    }

    @Override
    PrismObject<SystemConfigurationType> getSystemConfiguration() {
        return syncCtx.getSystemConfiguration();
    }

    @Override
    ConfigurableValuePolicySupplier createValuePolicySupplier() {
        return null; // Not available if there's no focus.
    }
}
