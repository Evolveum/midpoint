/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.smart.api.ServiceClient.Method.SUGGEST_MAPPING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;

import com.evolveum.midpoint.util.MiscUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Implements "suggest mappings" operation.
 */
class MappingsSuggestionOperation {

    private static final Trace LOGGER = TraceManager.getTrace(MappingsSuggestionOperation.class);

    private static final int ATTRIBUTE_MAPPING_EXAMPLES = 20;

    private static final String ID_SCHEMA_MATCHING = "schemaMatching";
    private static final String ID_SHADOWS_COLLECTION = "shadowsCollection";
    private static final String ID_MAPPINGS_SUGGESTION = "mappingsSuggestion";
    private final TypeOperationContext ctx;

    private MappingsSuggestionOperation(TypeOperationContext ctx) {
        this.ctx = ctx;
    }

    static MappingsSuggestionOperation init(
            ServiceClient serviceClient,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            @Nullable CurrentActivityState<?> activityState,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return new MappingsSuggestionOperation(
                TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, activityState, task, result));
    }

    MappingsSuggestionType suggestMappings(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException, ActivityInterruptedException {
        var focusTypeDefinition = ctx.getFocusTypeDefinition();
        var shadowDefinition = ctx.getShadowDefinition();

        var matchingOp = new SchemaMatchingOperation(ctx);

        var schemaMatchingState = ctx.stateHolderFactory.create(ID_SCHEMA_MATCHING, result);
        schemaMatchingState.flush(result);
        SiMatchSchemaResponseType match;
        try {
            match = matchingOp.matchSchema(ctx.typeDefinition, focusTypeDefinition, ctx.resource);
        } catch (Throwable t) {
            schemaMatchingState.recordException(t);
            throw t;
        } finally {
            schemaMatchingState.close(result);
        }

        ctx.checkIfCanRun();

        var attributeMatchToMapCollection = new ArrayList<AttributeMatchToMap>(match.getAttributeMatch().size());
        for (var attributeMatch : match.getAttributeMatch()) {
            var shadowAttrPath = matchingOp.getApplicationItemPath(attributeMatch.getApplicationAttribute());
            if (shadowAttrPath.size() != 2 || !shadowAttrPath.startsWith(ShadowType.F_ATTRIBUTES)) {
                LOGGER.warn("Ignoring attribute {}. It is not a traditional attribute.", shadowAttrPath);
                continue; // TODO implement support for activation etc
            }
            var shadowAttrName = shadowAttrPath.rest().asSingleNameOrFail();
            var shadowAttrDef = ctx.typeDefinition.findSimpleAttributeDefinition(shadowAttrName);
            if (shadowAttrDef == null) {
                LOGGER.warn("No shadow attribute definition found for {}. Skipping mapping suggestion.", shadowAttrName);
                continue;
            }
            var focusPropPath = matchingOp.getFocusItemPath(attributeMatch.getMidPointAttribute());
            var focusPropDef = focusTypeDefinition.findPropertyDefinition(focusPropPath);
            if (focusPropDef == null) {
                LOGGER.warn("No focus property definition found for {}. Skipping mapping suggestion.", focusPropPath);
                continue;
            }
            attributeMatchToMapCollection.add(
                    new AttributeMatchToMap(
                            DescriptiveItemPath.of(shadowAttrPath, shadowDefinition),
                            shadowAttrDef,
                            DescriptiveItemPath.of(focusPropPath, focusTypeDefinition),
                            focusPropDef));
        }

        if (attributeMatchToMapCollection.isEmpty()) {
            LOGGER.warn("No schema match found for {}. Returning empty suggestion.", this);
            return new MappingsSuggestionType();
        }

        var shadowsCollectionState = ctx.stateHolderFactory.create(ID_SHADOWS_COLLECTION, result);
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

        ctx.checkIfCanRun();

        var mappingsSuggestionState = ctx.stateHolderFactory.create(ID_MAPPINGS_SUGGESTION, result);
        mappingsSuggestionState.setExpectedProgress(attributeMatchToMapCollection.size());
        try {
            var suggestion = new MappingsSuggestionType();
            for (AttributeMatchToMap m : attributeMatchToMapCollection) {
                var op = mappingsSuggestionState.recordProcessingStart(m.shadowAttrDescPath.asString());
                mappingsSuggestionState.flush(result);
                var pairs = getValuesPairs(m, ownedShadows);
                suggestion.getAttributeMappings().add(
                        suggestMapping(
                                m.shadowAttrDescPath,
                                m.shadowAttrDef,
                                m.focusPropDescPath,
                                m.focusPropDef,
                                pairs));
                mappingsSuggestionState.recordProcessingEnd(op);
                ctx.checkIfCanRun();
            }
            return suggestion;
        } catch (Throwable t) {
            mappingsSuggestionState.recordException(t);
            throw t;
        } finally {
            mappingsSuggestionState.close(result);
        }
    }

    private Collection<ValuesPair> getValuesPairs(AttributeMatchToMap m, Collection<OwnedShadow> ownedShadows) {
        return extractPairs(
                ownedShadows, m.shadowAttrDescPath.getItemPath(), m.focusPropDescPath.getItemPath());
    }

    private record AttributeMatchToMap(
            DescriptiveItemPath shadowAttrDescPath,
            ShadowSimpleAttributeDefinition<?> shadowAttrDef,
            DescriptiveItemPath focusPropDescPath,
            PrismPropertyDefinition<?> focusPropDef) {
    }

    private Collection<ValuesPair> extractPairs(
            Collection<OwnedShadow> ownedShadows, ItemPath shadowAttrPath, ItemPath focusPropPath) {
        return ownedShadows.stream()
                .map(ownedShadow -> new ValuesPair(
                        getItemRealValues(ownedShadow.shadow, shadowAttrPath),
                        getItemRealValues(ownedShadow.owner, focusPropPath)))
                .toList();
    }

    private Collection<?> getItemRealValues(ObjectType objectable, ItemPath itemPath) {
        var item = objectable.asPrismObject().findItem(itemPath);
        return item != null ? item.getRealValues() : List.of();
    }

    private Collection<OwnedShadow> fetchOwnedShadows(OperationContext.StateHolder state, OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        // Maybe we should search the repository instead. The argument for going to the resource is to get some data even
        // if they are not in the repository yet. But this is not a good argument, because if we get an account from the resource,
        // it won't have the owner anyway.
        var ownedShadows = new ArrayList<OwnedShadow>(ATTRIBUTE_MAPPING_EXAMPLES);
        ctx.b.modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(ctx.resource)
                        .queryFor(ctx.typeDefinition.getTypeIdentification())
                        .build(),
                (object, lResult) -> {
                    try {
                        var owner = ctx.b.modelService.searchShadowOwner(object.getOid(), null, ctx.task, lResult);
                        if (owner != null) {
                            ownedShadows.add(new OwnedShadow(object.asObjectable(), owner.asObjectable()));
                            state.incrementProgress(result);
                        }
                    } catch (Exception e) {
                        LoggingUtils.logException(LOGGER, "Couldn't fetch owner for {}", e, object);
                    }
                    return ctx.canRun() && ownedShadows.size() < ATTRIBUTE_MAPPING_EXAMPLES;
                },
                null, ctx.task, result);
        return ownedShadows;
    }

    private record OwnedShadow(ShadowType shadow, FocusType owner) {
    }

    private AttributeMappingsSuggestionType suggestMapping(
            DescriptiveItemPath shadowAttrPath,
            ShadowSimpleAttributeDefinition<?> attrDef,
            DescriptiveItemPath focusPropPath,
            PrismPropertyDefinition<?> propertyDef,
            Collection<ValuesPair> valuesPairs) throws SchemaException {

        String transformationScript;
        if (!valuesPairs.isEmpty() && isTransformationNeeded(valuesPairs, propertyDef)) {
            var applicationAttrDefBean = new SiAttributeDefinitionType()
                    .name(shadowAttrPath.asString())
                    .type(getTypeName(attrDef))
                    .minOccurs(attrDef.getMinOccurs())
                    .maxOccurs(attrDef.getMaxOccurs());
            var midPointPropertyDefBean = new SiAttributeDefinitionType()
                    .name(focusPropPath.asString())
                    .type(getTypeName(propertyDef))
                    .minOccurs(propertyDef.getMinOccurs())
                    .maxOccurs(propertyDef.getMaxOccurs());
            var siRequest = new SiSuggestMappingRequestType()
                    .applicationAttribute(applicationAttrDefBean)
                    .midPointAttribute(midPointPropertyDefBean)
                    .inbound(true);
            valuesPairs.forEach(pair ->
                    siRequest.getExample().add(
                            pair.toSiExample(
                                    shadowAttrPath, focusPropPath)));
            var siResponse = ctx.serviceClient.invoke(SUGGEST_MAPPING, siRequest, SiSuggestMappingResponseType.class);
            transformationScript = siResponse.getTransformationScript();
        } else {
            transformationScript = null; // no pairs, no transformation script
        }
        ExpressionType expression = StringUtils.isNotBlank(transformationScript) && !transformationScript.equals("input") ?
                new ExpressionType()
                        .expressionEvaluator(
                                new ObjectFactory().createScript(
                                        new ScriptExpressionEvaluatorType().code(transformationScript))) :
                null;
        var suggestion = new AttributeMappingsSuggestionType()
                .definition(new ResourceAttributeDefinitionType()
                        .ref(shadowAttrPath.getItemPath().rest().toBean()) // FIXME! what about activation, credentials, etc?
                        .inbound(new InboundMappingType()
                                .name(shadowAttrPath.getItemPath().lastName().getLocalPart()
                                        + "-to-" + focusPropPath.getItemPath().toString()) //TODO TBD
                                .target(new VariableBindingDefinitionType()
                                        .path(focusPropPath.getItemPath().toBean()))
                                .expression(expression)));
        AiUtil.markAsAiProvided(suggestion); // everything is AI-provided now
        return suggestion;
    }

    /**
     * Returns {@code true} if a transformation script is needed to convert the values,
     * i.e. the default "asIs" mapping is not enough.
     */
    private boolean isTransformationNeeded(Collection<ValuesPair> valuesPairs, PrismPropertyDefinition<?> propertyDef) {
        for (var valuesPair : valuesPairs) {
            var shadowValues = valuesPair.shadowValues();
            var focusValues = valuesPair.focusValues();
            if (shadowValues.size() != focusValues.size()) {
                return true;
            }
            var expectedFocusValues = new ArrayList<>(focusValues.size());
            for (Object shadowValue : shadowValues) {
                Object converted;
                try {
                    converted = ExpressionUtil.convertValue(
                            propertyDef.getTypeClass(), null, shadowValue, ctx.b.protector);
                } catch (Exception e) {
                    // If the conversion is not possible e.g. because of different types, an exception is thrown
                    // We are OK with that (from performance point of view), because this is just a sample of values.
                    LOGGER.trace("Value conversion failed, assuming transformation is needed: {} (value: {})",
                            e.getMessage(), shadowValue); // no need to provide full stack trace here
                    return true;
                }
                if (converted != null) {
                    expectedFocusValues.add(converted);
                }
            }
            if (!MiscUtil.unorderedCollectionEquals(focusValues, expectedFocusValues)) {
                return true;
            }
        }
        return false;
    }

    private QName getTypeName(@NotNull PrismPropertyDefinition<?> propertyDefinition) {
        if (propertyDefinition.isEnum()) {
            // We don't want to bother Python microservice with enums; maybe later.
            // It should work with the values as with simple strings.
            return DOMUtil.XSD_STRING;
        }
        var typeName = propertyDefinition.getTypeName();
        if (QNameUtil.match(PolyStringType.COMPLEX_TYPE, typeName)) {
            return DOMUtil.XSD_STRING; // We don't want to bother Python microservice with polystrings.
        } else if (QNameUtil.match(ProtectedStringType.COMPLEX_TYPE, typeName)) {
            return DOMUtil.XSD_STRING; // the same
        } else {
            return typeName;
        }
    }

    record ValuesPair(Collection<?> shadowValues, Collection<?> focusValues) {
        private SiSuggestMappingExampleType toSiExample(
                DescriptiveItemPath applicationAttrNameBean, DescriptiveItemPath midPointPropertyNameBean) {
            return new SiSuggestMappingExampleType()
                    .application(toSiAttributeExample(applicationAttrNameBean, shadowValues))
                    .midPoint(toSiAttributeExample(midPointPropertyNameBean, focusValues));
        }

        private @NotNull SiAttributeExampleType toSiAttributeExample(DescriptiveItemPath path, Collection<?> values) {
            var example = new SiAttributeExampleType().name(path.asString());
            example.getValue().addAll(stringify(values));
            return example;
        }

        private Collection<String> stringify(Collection<?> values) {
            return values.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
        }
    }
}
