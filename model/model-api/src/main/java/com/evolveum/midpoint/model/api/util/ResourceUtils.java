/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

import static java.util.Collections.singleton;

/**
 * TODO find appropriate place for this class
 */
public class ResourceUtils {

    public static void deleteSchema(PrismObject<? extends ResourceType> resource, ModelService modelService, Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException, SecurityViolationException {
        deleteSchema(resource.getOid(), modelService, task, parentResult);
    }
        public static void deleteSchema(String resource, ModelService modelService, Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<ResourceType> delta = PrismContext.get().deltaFor(ResourceType.class)
                .item(ItemPath.create(ResourceType.F_SCHEMA, XmlSchemaType.F_DEFINITION)).replace()
                .item(ItemPath.create(ResourceType.F_SCHEMA, XmlSchemaType.F_CACHING_METADATA)).replace()
                .asObjectDelta(resource);
        modelService.executeChanges(singleton(delta), ModelExecuteOptions.create().raw(), task, parentResult);
    }
}
