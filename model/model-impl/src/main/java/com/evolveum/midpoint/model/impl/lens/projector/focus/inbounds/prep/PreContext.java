/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.NotNull;

class PreContext extends Context {

    PreContext(@NotNull MappingEvaluationEnvironment env, @NotNull OperationResult result, @NotNull ModelBeans beans) {
        super(env, result, beans);
    }

    @Override
    String getOperation() {
        return null;
    }

    @Override
    PrismObject<SystemConfigurationType> getSystemConfiguration() {
        return null; //TODO
    }

    @Override
    ConfigurableValuePolicySupplier createValuePolicySupplier() {
        return null;
    }
}
