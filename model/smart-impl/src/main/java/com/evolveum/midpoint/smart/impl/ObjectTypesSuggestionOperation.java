/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.smart.api.ServiceClient.Method.SUGGEST_OBJECT_TYPES;
import static com.evolveum.midpoint.util.MiscUtil.nullIfEmpty;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.HashSet;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;

/**
 * Implements "suggest object types" operation.
 */
class ObjectTypesSuggestionOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypesSuggestionOperation.class);

    private final OperationContext ctx;

    ObjectTypesSuggestionOperation(OperationContext context) {
        this.ctx = context;
    }

    /**
     * Calls the `suggestObjectTypes` method on the remote service.
     *
     * Response processing includes:
     *
     * * resolving conflicts in object type identification (multiple rules for the same kind and intent)
     * ** if the base context is the same, filters are merged by OR-ing them together
     * ** if the base context is different, conflicting intents are renamed by appending a number
     */
    ObjectTypesSuggestionType suggestObjectTypes(ShadowObjectClassStatisticsType shadowObjectClassStatistics)
            throws SchemaException {
        var siRequest = new SiSuggestObjectTypesRequestType()
                .schema(ResourceObjectClassSchemaSerializer.serialize(ctx.objectClassDefinition, ctx.resource))
                .statistics(shadowObjectClassStatistics);

        var siResponse = ctx.serviceClient.invoke(SUGGEST_OBJECT_TYPES, siRequest, SiSuggestObjectTypesResponseType.class);
        stripBlankStrings(siResponse);

        var response = new ObjectTypesSuggestionType();

        var shadowObjectDef = ctx.objectClassDefinition.getPrismObjectDefinition();

        var typeIdsSeen = new HashSet<ResourceObjectTypeIdentification>();
        for (var siObjectType : siResponse.getObjectType()) {
            var delineation =
                    new ResourceObjectTypeDelineationType()
                            .objectClass(ctx.objectClassDefinition.getTypeName());

            for (String filterString : siObjectType.getFilter()) {
                delineation.filter(parseAndSerializeFilter(filterString, shadowObjectDef));
            }
            AiUtil.markAsAiProvided(delineation, ResourceObjectTypeDelineationType.F_FILTER);

            var siBaseContextClassLocalName = siObjectType.getBaseContextObjectClassName();
            var siBaseContextFilter = siObjectType.getBaseContextFilter();
            if (siBaseContextClassLocalName != null || siBaseContextFilter != null) {
                stateCheck(siBaseContextClassLocalName != null,
                        "Base context class name must be set if base context filter is set");
                stateCheck(siBaseContextFilter != null,
                        "Based context filter must be set if base context class name is set");
                var baseContextClassQName = new QName(NS_RI, siBaseContextClassLocalName);
                var baseContextObjectDef = ctx.resourceSchema.findObjectClassDefinitionRequired(baseContextClassQName);
                delineation.baseContext(new ResourceObjectReferenceType()
                        .objectClass(baseContextClassQName)
                        .filter(parseAndSerializeFilter(siBaseContextFilter, baseContextObjectDef.getPrismObjectDefinition())));
                AiUtil.markAsAiProvided(delineation, ResourceObjectTypeDefinitionType.F_BASE_CONTEXT);
            }

            var typeId = ResourceObjectTypeIdentification.of(
                    ShadowKindType.fromValue(siObjectType.getKind()),
                    SchemaConstants.INTENT_UNKNOWN.equals(siObjectType.getIntent()) // temporary hack
                            ? SchemaConstants.INTENT_DEFAULT : siObjectType.getIntent());
            if (!typeIdsSeen.add(typeId)) {
                LOGGER.warn("Duplicate typeId {}, ignoring the suggestion:\n{}",
                        typeId, PrismContext.get().xmlSerializer().serializeRealValue(
                                siObjectType, SiSuggestObjectTypesResponseType.F_OBJECT_TYPE));
                continue;
            }
            var displayName = siObjectType.getDisplayName();
            var description = siObjectType.getDescription();
            var objectType = new ResourceObjectTypeDefinitionType()
                    .synchronization(defaultSynchronizationReactions())
                    .kind(typeId.getKind())
                    .intent(typeId.getIntent())
                    .displayName(displayName)
                    .description(description)
                    .delineation(delineation);
            AiUtil.markAsAiProvided(
                    objectType,
                    ResourceObjectTypeDefinitionType.F_KIND,
                    ResourceObjectTypeDefinitionType.F_INTENT,
                    ResourceObjectTypeDefinitionType.F_DISPLAY_NAME,
                    ResourceObjectTypeDefinitionType.F_DESCRIPTION);
            response.getObjectType().add(objectType);
        }

        LOGGER.debug("Suggested object types for {}:\n{}", ctx.objectClassDefinition, response.debugDump(1));

        return response;
    }

    /**
     * Creates a default set of synchronization reactions for a resource suggested object type.
     * <p>
     * The following simple reactions are configured:
     * <ul>
     *   <li>{@link SynchronizationSituationType#LINKED} → synchronize the object.</li>
     *   <li>{@link SynchronizationSituationType#UNLINKED} → link the object.</li>
     *   <li>{@link SynchronizationSituationType#UNMATCHED} → add a new focus object.</li>
     * </ul>
     * These defaults are meant as a starting point for basic synchronization scenarios.
     *
     * @return a {@link SynchronizationReactionsType} instance containing the default reactions
     */
    private @NotNull SynchronizationReactionsType defaultSynchronizationReactions() {
        SynchronizationReactionsType reactions = new SynchronizationReactionsType();

        reactions.beginReaction()
                .name("linked-synchronize")
                .situation(SynchronizationSituationType.LINKED)
                .beginActions()
                .beginSynchronize()
                .end();

        reactions.beginReaction()
                .name("unlinked-link")
                .situation(SynchronizationSituationType.UNLINKED)
                .beginActions()
                .beginLink()
                .end();

        reactions.beginReaction()
                .name("unmatched-add-focus")
                .situation(SynchronizationSituationType.UNMATCHED)
                .beginActions()
                .beginAddFocus()
                .end();

        return reactions;
    }

    // FIXME we should fix prism parsing in midPoint to avoid this
    private void stripBlankStrings(SiSuggestObjectTypesResponseType response) {
        for (var objectType : response.getObjectType()) {
            objectType.getFilter().removeIf(f -> StringUtils.isBlank(f));
            objectType.setBaseContextObjectClassName(nullIfEmpty(objectType.getBaseContextObjectClassName()));
            objectType.setBaseContextFilter(nullIfEmpty(objectType.getBaseContextFilter()));
        }
    }

    private static SearchFilterType parseAndSerializeFilter(
            String filterString, PrismObjectDefinition<ShadowType> shadowObjectDef)
            throws SchemaException {
        LOGGER.trace("Parsing filter: {}", filterString);
        try {
            var parsedFilter = PrismContext.get().createQueryParser().parseFilter(shadowObjectDef, filterString);
            return PrismContext.get().querySerializer().serialize(parsedFilter).toSearchFilterType();
        } catch (Exception e) {
            throw new SchemaException(
                    "Cannot process suggested filter (%s): %s".formatted(filterString, e.getMessage()),
                    e);
        }
    }
}
