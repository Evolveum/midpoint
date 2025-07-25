/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.smart.api.ServiceClient.Method.*;
import static com.evolveum.midpoint.util.MiscUtil.nullIfEmpty;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Converts requests and responses between the Smart Integration Service and the microservice.
 */
class ServiceAdapter {

    private static final Trace LOGGER = TraceManager.getTrace(ServiceAdapter.class);

    private final ServiceClient serviceClient;

    private ServiceAdapter(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    static ServiceAdapter create(ServiceClient serviceClient) {
        return new ServiceAdapter(serviceClient);
    }

    /** Calls the `suggestObjectTypes` method on the remote service. */
    ObjectTypesSuggestionType suggestObjectTypes(
            ResourceObjectClassDefinition objectClassDef,
            ShadowObjectClassStatisticsType shadowObjectClassStatistics,
            ResourceSchema resourceSchema)
            throws SchemaException {
        var siRequest = new SiSuggestObjectTypesRequestType()
                .schema(ResourceObjectClassSchemaSerializer.serialize(objectClassDef))
                .statistics(shadowObjectClassStatistics);

        var siResponse = serviceClient.invoke(SUGGEST_OBJECT_TYPES, siRequest, SiSuggestObjectTypesResponseType.class);
        var response = new ObjectTypesSuggestionType();

        var shadowObjectDef = objectClassDef.getPrismObjectDefinition();

        for (SiSuggestedObjectTypeType siObjectType : siResponse.getObjectType()) {
            var kind = ShadowKindType.fromValue(siObjectType.getKind());
            var intent = siObjectType.getIntent();
            LOGGER.trace("Processing suggested object type: kind={}, intent={} for {}", kind, intent, objectClassDef);

            var delineation =
                    new ResourceObjectTypeDelineationType()
                            .objectClass(objectClassDef.getTypeName());
            for (String filterString : siObjectType.getFilter()) {
                if (StringUtils.isNotBlank(filterString)) { // TODO service should not return empty strings!
                    delineation.filter(parseAndSerializeFilter(filterString, shadowObjectDef));
                }
            }

            // TODO the service should not return empty strings!
            var siBaseContextClassLocalName = nullIfEmpty(siObjectType.getBaseContextObjectClassName());
            var siBaseContextFilter = nullIfEmpty(siObjectType.getBaseContextFilter());
            if (siBaseContextClassLocalName != null || siBaseContextFilter != null) {
                stateCheck(siBaseContextClassLocalName != null,
                        "Base context class name must be set if base context filter is set");
                stateCheck(siBaseContextFilter != null,
                        "Based context filter must be set if base context class name is set");
                var baseContextClassQName = new QName(NS_RI, siBaseContextClassLocalName);
                var baseContextObjectDef = resourceSchema.findObjectClassDefinitionRequired(baseContextClassQName);
                delineation.baseContext(new ResourceObjectReferenceType()
                        .objectClass(baseContextClassQName)
                        .filter(parseAndSerializeFilter(siBaseContextFilter, baseContextObjectDef.getPrismObjectDefinition())));
            }

            var objectType = new ObjectTypeSuggestionType()
                    .identification(new ResourceObjectTypeIdentificationType()
                            .kind(kind)
                            .intent(intent))
                    .delineation(delineation);
            response.getObjectType().add(objectType);
        }

        LOGGER.debug("Suggested object types for {}:\n{}", objectClassDef, response.debugDump(1));

        return response;
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

    /** Calls the `suggestFocusType` method on the remote service. */
    QName suggestFocusType(
            ResourceObjectTypeIdentification typeIdentification,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation)
            throws SchemaException {
        var request = new SiSuggestFocusTypeRequestType()
                .kind(typeIdentification.getKind().value())
                .intent(typeIdentification.getIntent())
                .schema(ResourceObjectClassSchemaSerializer.serialize(objectClassDef));

        setBaseContextFilter(request, objectClassDef, delineation);

        return serviceClient
                .invoke(SUGGEST_FOCUS_TYPE, request, SiSuggestFocusTypeResponseType.class)
                .getFocusTypeName();
    }

    private static void setBaseContextFilter(
            SiSuggestFocusTypeRequestType request,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation) throws SchemaException {
        var baseContext = delineation.getBaseContext();
        var baseContextFilter = baseContext != null ? baseContext.getFilter() : null;
        if (baseContextFilter != null) {
            // We hope that object class definition is sufficient to parse the filter.
            // It should be, because all the attributes are there.
            var filter = ShadowQueryConversionUtil.parseFilter(baseContextFilter, objectClassDef);
            try {
                request.setBaseContextFilter(
                        PrismContext.get().querySerializer().serialize(filter).filterText());
            } catch (PrismQuerySerialization.NotSupportedException e) {
                throw SystemException.unexpected(e, "Cannot serialize base context filter");
            }
        }
    }

    MappingsSuggestionType suggestMappings(
            ResourceObjectTypeDefinition objectTypeDef, PrismObjectDefinition<?> focusDef) throws SchemaException {
        var siResponse = matchSchema(objectTypeDef, focusDef);
        var response = new MappingsSuggestionType();
        for (var siAttributeMatch : siResponse.getAttributeMatch()) {
            var resourceAttr = siAttributeMatch.getApplicationAttribute();
            var focusItem = siAttributeMatch.getMidPointAttribute();
            response.getAttributeMappings().add(
                    new AttributeMappingsSuggestionType()
                            .definition(new ResourceAttributeDefinitionType()
                                    .ref(resourceAttr)
                                    .outbound(new MappingType()
                                            .source(new VariableBindingDefinitionType()
                                                    .path(focusItem)))
                                    .inbound(new InboundMappingType()
                                            .target(new VariableBindingDefinitionType()
                                                    .path(focusItem)))));
        }
        return response;
    }

    /** Returns suggestions for correlators - in the same order as the correlators are provided. */
    List<CorrelatorSuggestion> suggestCorrelationMappings(
            ResourceObjectTypeDefinition objectTypeDef, PrismObjectDefinition<?> focusDef, List<? extends ItemPath> correlators)
            throws SchemaException {
        var siResponse = matchSchema(objectTypeDef, focusDef);
        var response = new ArrayList<CorrelatorSuggestion>();
        for (ItemPath correlator : correlators) {
            for (var siAttributeMatch : siResponse.getAttributeMatch()) {
                var focusItemPathBean = siAttributeMatch.getMidPointAttribute();
                var focusItemPath = focusItemPathBean.getItemPath();
                if (correlator.equivalent(focusItemPath)) {
                    var resourceAttrPathBean = siAttributeMatch.getApplicationAttribute();
                    response.add(
                            new CorrelatorSuggestion(
                                    focusItemPath,
                                    new ResourceAttributeDefinitionType()
                                            .ref(resourceAttrPathBean)
                                            .inbound(new InboundMappingType()
                                                    .target(new VariableBindingDefinitionType()
                                                            .path(focusItemPathBean))
                                                    .use(InboundMappingUseType.CORRELATION))));
                }
            }
        }
        return response;
    }

    record CorrelatorSuggestion(
            ItemPath focusItemPath,
            ResourceAttributeDefinitionType attributeDefinitionBean) {
    }

    private SiMatchSchemaResponseType matchSchema(ResourceObjectTypeDefinition objectTypeDef, PrismObjectDefinition<?> focusDef)
            throws SchemaException {
        var siRequest = new SiMatchSchemaRequestType()
                .applicationSchema(ResourceObjectClassSchemaSerializer.serialize(objectTypeDef.getObjectClassDefinition()))
                .midPointSchema(PrismComplexTypeDefinitionSerializer.serialize(focusDef));
        return serviceClient.invoke(MATCH_SCHEMA, siRequest, SiMatchSchemaResponseType.class);
    }
}
