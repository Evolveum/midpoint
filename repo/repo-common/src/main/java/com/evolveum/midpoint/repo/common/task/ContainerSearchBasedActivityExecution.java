/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;


public abstract class ContainerSearchBasedActivityExecution<
        C extends Containerable,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends SearchBasedActivityExecution<C, PrismContainer<C>, ObjectResultHandler, WD, AH, WS> {

    public ContainerSearchBasedActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
            @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    SearchActivityExecutionSupport getSearchSupport(){
        if (isObjectType()) {
            return new ObjectSearchActivityExecutionSupport();
        } else if (isAuditType()) {
            return new AuditSearchActivityExecutionSupport();
        }
        throw new IllegalArgumentException("Unsupported container type " + getContainerType());
    }

    private boolean isObjectType() {
        return MiscSchemaUtil.isObjectType(getContainerType());
    }

    private boolean isAuditType() {
        return MiscSchemaUtil.isAuditType(getContainerType());
    }
}
