/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import static java.util.Collections.singleton;

/**
 * TODO find appropriate place for this class
 *
 * @author mederly
 */
public class ResourceUtils {

    private static final ItemPath SCHEMA_PATH = ItemPath.create(ResourceType.F_SCHEMA, XmlSchemaType.F_DEFINITION);

    public static void deleteSchema(PrismObject<ResourceType> resource, ModelService modelService, PrismContext prismContext, Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException, SecurityViolationException {
        PrismProperty<SchemaDefinitionType> definition = resource.findProperty(SCHEMA_PATH);
        if (definition != null && !definition.isEmpty()) {
            ObjectDelta<ResourceType> delta = prismContext.deltaFor(ResourceType.class)
                    .item(SCHEMA_PATH).replace()
                    .asObjectDelta(resource.getOid());
            modelService.executeChanges(singleton(delta), null, task, parentResult);
        }
    }
}
