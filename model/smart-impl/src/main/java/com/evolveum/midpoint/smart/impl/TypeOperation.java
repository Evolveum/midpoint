package com.evolveum.midpoint.smart.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/** An {@link Operation} executing on a specific object type. */
class TypeOperation extends Operation {

    private static final Trace LOGGER = TraceManager.getTrace(TypeOperation.class);

    private static final int ATTRIBUTE_MAPPING_EXAMPLES = 20;

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

    MappingsSuggestionType suggestMappings(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var focusTypeDefinition = getFocusTypeDefinition();
        var match = serviceAdapter.matchSchema(typeDefinition, focusTypeDefinition);
        if (match.getAttributeMatch().isEmpty()) {
            LOGGER.warn("No schema match found for {}. Returning empty suggestion.", this);
            return new MappingsSuggestionType();
        }

        var ownedShadows = fetchOwnedShadows(result);

        var suggestion = new MappingsSuggestionType();
        for (var attributeMatch : match.getAttributeMatch()) {
            var shadowAttrName = attributeMatch.getApplicationAttribute().getItemPath().asSingleNameOrFail();
            var shadowAttrPath = ShadowType.F_ATTRIBUTES.append(shadowAttrName);
            var shadowAttrDef = typeDefinition.findSimpleAttributeDefinition(shadowAttrName);
            if (shadowAttrDef == null) {
                LOGGER.warn("No shadow attribute definition found for {}. Skipping mapping suggestion.", shadowAttrName);
                continue;
            }
            var focusPropPath = attributeMatch.getMidPointAttribute().getItemPath();
            var focusPropDef = focusTypeDefinition.findPropertyDefinition(focusPropPath);
            if (focusPropDef == null) {
                LOGGER.warn("No focus property definition found for {}. Skipping mapping suggestion.", focusPropPath);
                continue;
            }
            suggestion.getAttributeMappings().add(
                    serviceAdapter.suggestMapping(
                            shadowAttrName, shadowAttrDef, focusPropPath, focusPropDef,
                            extractPairs(ownedShadows, shadowAttrPath, focusPropPath)));
        }
        return suggestion;
    }

    private Collection<ServiceAdapter.ValuesPair> extractPairs(
            Collection<OwnedShadow> ownedShadows, ItemPath shadowAttrPath, ItemPath focusPropPath) {
        return ownedShadows.stream()
                .map(ownedShadow -> new ServiceAdapter.ValuesPair(
                        getItemRealValues(ownedShadow.shadow, shadowAttrPath),
                        getItemRealValues(ownedShadow.owner, focusPropPath)))
                .toList();
    }

    private Collection<?> getItemRealValues(ObjectType objectable, ItemPath itemPath) {
        var item = objectable.asPrismObject().findItem(itemPath);
        return item != null ? item.getRealValues() : List.of();
    }

    private Collection<OwnedShadow> fetchOwnedShadows(OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        var ownedShadows = new ArrayList<OwnedShadow>(ATTRIBUTE_MAPPING_EXAMPLES);
        b.modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(typeDefinition.getTypeIdentification())
                        .build(),
                (object, lResult) -> {
                    try {
                        var owner = b.modelService.searchShadowOwner(object.getOid(), null, task, lResult);
                        if (owner != null) {
                            ownedShadows.add(new OwnedShadow(object.asObjectable(), owner.asObjectable()));
                        }
                    } catch (Exception e) {
                        LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, object);
                    }
                    return ownedShadows.size() < ATTRIBUTE_MAPPING_EXAMPLES;
                },
                null, task, result);
        return ownedShadows;
    }

    private record OwnedShadow(ShadowType shadow, FocusType owner) {
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

    @Override
    public String toString() {
        return typeDefinition.getTypeIdentification() + " on " + resource;
    }
}
