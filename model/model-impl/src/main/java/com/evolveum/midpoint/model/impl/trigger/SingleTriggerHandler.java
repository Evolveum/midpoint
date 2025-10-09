/*
 * Copyright (c) 2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.trigger;

import com.evolveum.midpoint.model.api.trigger.TriggerHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

import org.jetbrains.annotations.NotNull;

/**
 * @author Radovan Semancik
 *
 */
public interface SingleTriggerHandler extends TriggerHandler {

    <O extends ObjectType> void handle(@NotNull PrismObject<O> object, @NotNull TriggerType trigger,
            @NotNull RunningTask task, @NotNull OperationResult result);

}
