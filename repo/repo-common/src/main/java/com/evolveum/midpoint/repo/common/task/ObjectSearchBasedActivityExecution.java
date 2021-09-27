/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public abstract class ObjectSearchBasedActivityExecution<
        O extends ObjectType,
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        WS extends AbstractActivityWorkStateType>
        extends SearchBasedActivityExecution<O, PrismObject<O>, ObjectResultHandler, WD, AH, WS>{

    private final ObjectSearchActivityExecutionSupport searchSupport = new ObjectSearchActivityExecutionSupport();

    public ObjectSearchBasedActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context,
                                              @NotNull String shortNameCapitalized) {

        super(context, shortNameCapitalized);
    }

    @Override
    SearchActivityExecutionSupport getSearchSupport(){
        return searchSupport;
    }
}
