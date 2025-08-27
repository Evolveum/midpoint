package com.evolveum.midpoint.smart.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;

import com.evolveum.midpoint.schema.util.AiUtil;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
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

    private static final String ID_SCHEMA_MATCHING = "schemaMatching";
    private static final String ID_SHADOWS_COLLECTION = "shadowsCollection";
    private static final String ID_MAPPINGS_SUGGESTION = "mappingsSuggestion";

    private final ResourceObjectTypeDefinition typeDefinition;

    private TypeOperation(
            ResourceType resource,
            ResourceSchema resourceSchema,
            ResourceObjectTypeDefinition typeDefinition,
            ServiceAdapter serviceAdapter,
            @Nullable CurrentActivityState<?> activityState,
            Task task) {
        super(resource, resourceSchema, typeDefinition.getObjectClassDefinition(), serviceAdapter, activityState, task);
        this.typeDefinition = typeDefinition;
    }

    static TypeOperation init(
            ServiceClient serviceClient,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            @Nullable CurrentActivityState<?> activityState,
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
        return new TypeOperation(resource, resourceSchema, typeDefinition, serviceAdapter, activityState, task);
    }

    FocusTypeSuggestionType suggestFocusType() throws SchemaException {
        return serviceAdapter.suggestFocusType(
                typeDefinition.getTypeIdentification(),
                typeDefinition.getObjectClassDefinition(),
                typeDefinition.getDelineation(),
                resource);
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
     */
    CorrelationSuggestionType suggestCorrelation() throws SchemaException {
        var correlators = KnownCorrelator.getAllFor(getFocusTypeDefinition().getCompileTimeClass());
        var attributeDefinitionsForCorrelators =
                serviceAdapter.suggestCorrelationMappings(typeDefinition, getFocusTypeDefinition(), correlators, resource);
        var suggestion = new CorrelationSuggestionType();
        if (!attributeDefinitionsForCorrelators.isEmpty()) {
            var first = attributeDefinitionsForCorrelators.get(0); // already marked as AI-provided
            suggestion.getAttributes().add(first.attributeDefinitionBean());
            var correlationDefinition = new CorrelationDefinitionType()
                    .correlators(new CompositeCorrelatorType()
                            .items(new ItemsSubCorrelatorType()
                                    .name("This is dummy name") //TODO replace with real implementation on service side
                                    .displayName("Dummy display") //TODO replace with real implementation on service side
                                    .description("This is dummy description (replace it with real implementation on service side)") //TODO replace with real implementation on service side
                                    .enabled(false) // TODO replace with real implementation on service side
                                    .item(new CorrelationItemType()
                                            .ref(first.focusItemPath().toBean()))));
            AiUtil.markAsAiProvided(correlationDefinition);
            suggestion.setCorrelation(correlationDefinition);
        }
        return suggestion;
    }

    MappingsSuggestionType suggestMappings(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException, ActivityInterruptedException {
        var focusTypeDefinition = getFocusTypeDefinition();

        var schemaMatchingState = stateHolderFactory.create(ID_SCHEMA_MATCHING, result);
        schemaMatchingState.flush(result);
        SiMatchSchemaResponseType match;
        try {
            match = serviceAdapter.matchSchema(typeDefinition, focusTypeDefinition, resource);
        } catch (Throwable t) {
            schemaMatchingState.recordException(t);
            throw t;
        } finally {
            schemaMatchingState.close(result);
        }

        checkIfCanRun();

        var attributeMatchToMapCollection = new ArrayList<AttributeMatchToMap>(match.getAttributeMatch().size());
        for (var attributeMatch : match.getAttributeMatch()) {
            var shadowAttrPath = attributeMatch.getApplicationAttribute().getItemPath();
            if (shadowAttrPath.size() != 2 || !shadowAttrPath.startsWith(ShadowType.F_ATTRIBUTES)) {
                LOGGER.warn("Ignoring attribute {}. It is not a traditional attribute.", shadowAttrPath);
                continue; // TODO implement support for activation etc
            }
            var shadowAttrName = shadowAttrPath.rest().asSingleNameOrFail();
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
            attributeMatchToMapCollection.add(
                    new AttributeMatchToMap(shadowAttrPath, shadowAttrDef, focusPropPath, focusPropDef));
        }

        if (attributeMatchToMapCollection.isEmpty()) {
            LOGGER.warn("No schema match found for {}. Returning empty suggestion.", this);
            return new MappingsSuggestionType();
        }

        var shadowsCollectionState = stateHolderFactory.create(ID_SHADOWS_COLLECTION, result);
        shadowsCollectionState.setExpectedProgress(ATTRIBUTE_MAPPING_EXAMPLES);
        shadowsCollectionState.flush(result); // because finding an owned shadow can take a while
        Collection<OwnedShadow> ownedShadows;
        try {
            ownedShadows = fetchOwnedShadows(shadowsCollectionState, result);
        } catch (Throwable t) {
            shadowsCollectionState.recordException(t);
            throw t;
        } finally {
            shadowsCollectionState.close(result);
        }

        checkIfCanRun();

        var mappingsSuggestionState = stateHolderFactory.create(ID_MAPPINGS_SUGGESTION, result);
        mappingsSuggestionState.setExpectedProgress(attributeMatchToMapCollection.size());
        try {
            var suggestion = new MappingsSuggestionType();
            for (AttributeMatchToMap m : attributeMatchToMapCollection) {
                var op = mappingsSuggestionState.recordProcessingStart(m.shadowAttrPath.toString());
                mappingsSuggestionState.flush(result);
                suggestion.getAttributeMappings().add(
                        serviceAdapter.suggestMapping(
                                m.shadowAttrPath, m.shadowAttrDef, m.focusPropPath, m.focusPropDef,
                                extractPairs(ownedShadows, m.shadowAttrPath, m.focusPropPath)));
                mappingsSuggestionState.recordProcessingEnd(op);
                checkIfCanRun();
            }
            return suggestion;
        } catch (Throwable t) {
            mappingsSuggestionState.recordException(t);
            throw t;
        } finally {
            mappingsSuggestionState.close(result);
        }
    }

    private record AttributeMatchToMap(
            ItemPath shadowAttrPath,
            ShadowSimpleAttributeDefinition<?> shadowAttrDef,
            ItemPath focusPropPath,
            PrismPropertyDefinition<?> focusPropDef) {
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

    private Collection<OwnedShadow> fetchOwnedShadows(StateHolder state, OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        // Maybe we should search the repository instead. The argument for going to the resource is to get some data even
        // if they are not in the repository yet. But this is not a good argument, because if we get an account from the resource,
        // it won't have the owner anyway.
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
                            state.incrementProgress(result);
                        }
                    } catch (Exception e) {
                        LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, object);
                    }
                    return canRun() && ownedShadows.size() < ATTRIBUTE_MAPPING_EXAMPLES;
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
