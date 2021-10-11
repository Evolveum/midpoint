/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.trigger;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

import java.util.Collection;

/**
 * This handler can take a collection of triggers that have the same timestamp.
 */
public interface MultipleTriggersHandler extends TriggerHandler {

    /**
     * @pre triggers have the same timestamp
     * @return triggers that were successfully processed (and should be therefore removed from the object)
     */
    <O extends ObjectType> Collection<TriggerType> handle(PrismObject<O> object, Collection<TriggerType> triggers, RunningTask task, OperationResult result);
}
