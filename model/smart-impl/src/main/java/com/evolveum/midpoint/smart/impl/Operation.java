package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;

import javax.xml.namespace.QName;

/** A smart integration operation, knowing a resource and an object class on it. */
class Operation {

    final ResourceType resource;
    final ResourceSchema resourceSchema;
    final ResourceObjectClassDefinition objectClassDefinition;
    final ServiceAdapter serviceAdapter;
    final Task task;
    final SmartIntegrationBeans b = SmartIntegrationBeans.get();

    public Operation(
            ResourceType resource,
            ResourceSchema resourceSchema,
            ResourceObjectClassDefinition objectClassDefinition,
            ServiceAdapter serviceAdapter,
            Task task) {
        this.resource = resource;
        this.resourceSchema = resourceSchema;
        this.objectClassDefinition = objectClassDefinition;
        this.serviceAdapter = serviceAdapter;
        this.task = task;
    }

    static Operation init(
            ServiceClient serviceClient, String resourceOid, QName objectClassName, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var serviceAdapter = ServiceAdapter.create(serviceClient);
        var resource = b().modelService
                .getObject(ResourceType.class, resourceOid, null, task, result)
                .asObjectable();
        var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
        var objectClassDefinition = resourceSchema.findObjectClassDefinitionRequired(objectClassName);
        return new Operation(resource, resourceSchema, objectClassDefinition, serviceAdapter, task);
    }

    static SmartIntegrationBeans b() {
        return SmartIntegrationBeans.get();
    }

    ObjectTypesSuggestionType suggestObjectTypes(ShadowObjectClassStatisticsType statistics) throws SchemaException {
        return serviceAdapter.suggestObjectTypes(objectClassDefinition, statistics, resourceSchema);
    }
}
