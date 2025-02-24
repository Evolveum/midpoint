/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.hooks;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public interface HookRegistry {

    void registerChangeHook(String url, ChangeHook changeHook);

    List<ChangeHook> getAllChangeHooks();

    void registerReadHook(String url, ReadHook searchHook);

    Collection<ReadHook> getAllReadHooks();

    /** Unused for now, as we have no read hooks today. */
    default void invokeReadHooks(
            PrismObject<? extends ObjectType> object,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Task task,
            OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException {
        for (ReadHook hook : getAllReadHooks()) {
            hook.invoke(object, options, task, result);
        }
    }
}
