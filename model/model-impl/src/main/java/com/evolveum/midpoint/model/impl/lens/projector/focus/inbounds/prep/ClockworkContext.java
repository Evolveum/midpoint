/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.prism.path.PathSet;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

public class ClockworkContext extends Context {

    @NotNull private final LensContext<?> lensContext;

    public ClockworkContext(
            @NotNull LensContext<?> lensContext,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result,
            @NotNull ModelBeans beans) {
        super(env, result, beans);
        this.lensContext = lensContext;
    }

    @Override
    protected PrismObject<SystemConfigurationType> getSystemConfiguration() {
        return lensContext.getSystemConfiguration();
    }

    @Override
    protected String getOperation() {
        return lensContext.getFocusContext().getOperation().getValue();
    }

    @Override
    ConfigurableValuePolicySupplier createValuePolicySupplier() {
        return new ConfigurableValuePolicySupplier() {
            private ItemDefinition<?> outputDefinition;

            @Override
            public void setOutputDefinition(ItemDefinition<?> outputDefinition) {
                this.outputDefinition = outputDefinition;
            }

            @Override
            public ValuePolicyType get(OperationResult result) {
                if (outputDefinition.getItemName().equals(PasswordType.F_VALUE)) {
                    return beans.credentialsProcessor.determinePasswordPolicy(lensContext.getFocusContext());
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public @NotNull PathSet getCorrelationItemPaths() {
        return new PathSet();
    }
}
