package com.evolveum.midpoint.smart.impl;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/** An {@link Operation} executing on a specific object type. */
class TypeOperation extends Operation {

    private final ResourceObjectTypeDefinition typeDefinition;

    private TypeOperation(
            ResourceType resource,
            ResourceSchema resourceSchema,
            ResourceObjectTypeDefinition typeDefinition,
            ServiceAdapter serviceAdapter,
            Task task) {
        super(resource, resourceSchema, typeDefinition.getObjectClassDefinition(), serviceAdapter, task);
        this.typeDefinition = typeDefinition;
    }

    static TypeOperation init(
            ServiceClient serviceClient,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var serviceAdapter = ServiceAdapter.create(serviceClient);
        var resource = b().modelService
                .getObject(ResourceType.class, resourceOid, null, task, result)
                .asObjectable();
        var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
        var typeDefinition = resourceSchema.getObjectTypeDefinitionRequired(typeIdentification);
        return new TypeOperation(resource, resourceSchema, typeDefinition, serviceAdapter, task);
    }

    QName suggestFocusType() throws SchemaException {
        return serviceAdapter.suggestFocusType(
                typeDefinition.getTypeIdentification(),
                typeDefinition.getObjectClassDefinition(),
                typeDefinition.getDelineation());
    }

    /**
     * Initial implementation of "suggest correlation" method:
     *
     * . identify correlation-capable properties (like name, personalNumber, emailAddress, ...)
     * . ask for schema matchings for these properties
     * . if there are any, suggest the correlation - for the first one, if there are multiple
     *
     * Future improvements:
     *
     * . when suggesting mappings to correlation-capable properties, LLM should take into account the information about
     * whether source attribute is unique or not
     *
     */
    CorrelationSuggestionType suggestCorrelation() throws SchemaException {
        var correlators = KnownCorrelator.getAllFor(getFocusTypeDefinition().getCompileTimeClass());
        var attributeDefinitionsForCorrelators =
                serviceAdapter.suggestCorrelationMappings(typeDefinition, getFocusTypeDefinition(), correlators);
        var suggestion = new CorrelationSuggestionType();
        if (!attributeDefinitionsForCorrelators.isEmpty()) {
            var first = attributeDefinitionsForCorrelators.get(0);
            suggestion.getAttributes().add(first.attributeDefinitionBean());
            suggestion.setCorrelation(
                    new CorrelationDefinitionType()
                            .correlators(new CompositeCorrelatorType()
                                    .items(new ItemsSubCorrelatorType()
                                            .item(new CorrelationItemType()
                                                    .ref(first.focusItemPath().toBean())))));
        }
        return suggestion;
    }

    MappingsSuggestionType suggestMappings() throws SchemaException {
        // TODO implement real suggestion of mappings, not only "as-is" ones
        return serviceAdapter.suggestMappings(typeDefinition, getFocusTypeDefinition());
    }

    private PrismObjectDefinition<?> getFocusTypeDefinition() {
        var focusTypeName = getFocusTypeName();
        return MiscUtil.argNonNull(
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(focusTypeName),
                "Focus type definition not found for %s", focusTypeName);
    }

    private QName getFocusTypeName() {
        return MiscUtil.argNonNull(
                typeDefinition.getFocusTypeName(),
                "Focus type not defined for %s", typeDefinition.getTypeIdentification());
    }
}
