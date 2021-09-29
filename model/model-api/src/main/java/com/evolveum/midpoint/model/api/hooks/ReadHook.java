/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.hooks;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Collection;

/**
 * This applies to all read operations, therefore it will add "hook" into:
 * <ul>
 * <li>{@link com.evolveum.midpoint.model.api.ModelService#getObject(Class, String, java.util.Collection, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)}</li>
 * <li>{@link com.evolveum.midpoint.model.api.ModelService#searchObjects(Class, com.evolveum.midpoint.prism.query.ObjectQuery, java.util.Collection, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)}</li>
 * <li>{@link com.evolveum.midpoint.model.api.ModelService#searchObjectsIterative(Class, com.evolveum.midpoint.prism.query.ObjectQuery, ResultHandler, java.util.Collection, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)}</li>
 * </ul>
 *
 * TODO: this is just super simple, not stable, not finished yet.
 *
 * @author lazyman
 */
public interface ReadHook {

    /**
     * todo
     *
     * @param object
     * @param options
     * @param task
     * @param parentResult
     * @param <T>
     */
    <T extends ObjectType> void invoke(PrismObject<T> object, Collection<SelectorOptions<GetOperationOptions>> options,
                                       Task task, OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException;
}
