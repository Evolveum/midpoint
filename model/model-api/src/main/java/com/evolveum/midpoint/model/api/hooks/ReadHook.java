/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.api.hooks;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
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
 * <li>{@link com.evolveum.midpoint.model.api.ModelService#searchObjectsIterative(Class, com.evolveum.midpoint.prism.query.ObjectQuery, com.evolveum.midpoint.schema.ResultHandler, java.util.Collection, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)}</li>
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
