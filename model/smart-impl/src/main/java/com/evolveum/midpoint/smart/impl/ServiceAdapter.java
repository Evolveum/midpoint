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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Calls the `suggestObjectTypes` method on the remote service.
     *
     * Response processing includes:
     *
     * * resolving conflicts in object type identification (multiple rules for the same kind and intent)
     * ** if the base context is the same, filters are merged by OR-ing them together
     * ** if the base context is different, conflicting intents are renamed by appending a number
     *
     */
    ObjectTypesSuggestionType suggestObjectTypes(
            ResourceObjectClassDefinition objectClassDef,
            ShadowObjectClassStatisticsType shadowObjectClassStatistics,
            ResourceSchema resourceSchema)
            throws SchemaException {
        var siRequest = new SiSuggestObjectTypesRequestType()
                .schema(ResourceObjectClassSchemaSerializer.serialize(objectClassDef))
                .statistics(shadowObjectClassStatistics);

        var siResponse = serviceClient.invoke(SUGGEST_OBJECT_TYPES, siRequest, SiSuggestObjectTypesResponseType.class);
        stripBlankStrings(siResponse);

        var response = new ObjectTypesSuggestionType();

        var shadowObjectDef = objectClassDef.getPrismObjectDefinition();

        // Processing suggestions in compatible groups (see method javadoc above)
        var groupedObjectTypes = groupObjectTypes(siResponse.getObjectType());
        var uniqueTypeIdGenerator = new UniqueTypeIdGenerator();
        for (var objectTypeKey : groupedObjectTypes.keySet()) {
            var beans = groupedObjectTypes.get(objectTypeKey);
            LOGGER.trace("Processing {} object type(s) for key: {}", beans.size(), objectTypeKey);

            var delineation =
                    new ResourceObjectTypeDelineationType()
                            .objectClass(objectClassDef.getTypeName());

            if (beans.size() > 1) {
                var filterString = mergeFilters(beans);
                if (filterString != null) {
                    delineation.filter(parseAndSerializeFilter(filterString, shadowObjectDef));
                }
            } else {
                assert beans.size() == 1;
                var bean = beans.iterator().next();
                for (String filterString : bean.getFilter()) {
                    delineation.filter(parseAndSerializeFilter(filterString, shadowObjectDef));
                }
            }

            var siBaseContextClassLocalName = objectTypeKey.baseContextObjectClassName;
            var siBaseContextFilter = objectTypeKey.baseContextFilter;
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

            // There may be multiple incompatible definitions with the same id, e.g. account/default
            // We must ensure uniqueness of the types suggested.
            var typeId = uniqueTypeIdGenerator.generate(objectTypeKey);
            var objectType = new ObjectTypeSuggestionType()
                    .identification(new ResourceObjectTypeIdentificationType()
                            .kind(typeId.getKind())
                            .intent(typeId.getIntent()))
                    .delineation(delineation);
            response.getObjectType().add(objectType);
        }

        LOGGER.debug("Suggested object types for {}:\n{}", objectClassDef, response.debugDump(1));

        return response;
    }

    private static class UniqueTypeIdGenerator {
        private final HashSet<ResourceObjectTypeIdentification> objectTypesGenerated = new HashSet<>();

        ResourceObjectTypeIdentification generate(ObjectTypeKey objectTypeKey) {
            var typeId = ResourceObjectTypeIdentification.of(objectTypeKey.kind, objectTypeKey.intent);
            int suffix = 2;
            while (objectTypesGenerated.contains(typeId)) {
                // We have a conflict, so we need to rename the intent.
                typeId = ResourceObjectTypeIdentification.of(
                        objectTypeKey.kind, objectTypeKey.intent + "_" + (suffix++));
            }
            objectTypesGenerated.add(typeId);
            if (suffix > 2) {
                LOGGER.warn("Conflicting object type identification for kind={}, intent={}. Renaming to {}",
                        objectTypeKey.kind, objectTypeKey.intent, typeId);
            }
            return typeId;
        }
    }

    private String mergeFilters(Collection<SiSuggestedObjectTypeType> beans) {
        assert beans.size() > 1;
        var disjuncts = new ArrayList<String>();
        for (SiSuggestedObjectTypeType bean : beans) {
            var filters = bean.getFilter();
            if (filters.isEmpty()) {
                return null; // matches everything -> we return null as "match all"
            }
            // adding parentheses to avoid precedence issues
            disjuncts.add(
                    filters.stream()
                            .map(f -> "( " + f + " )")
                            .collect(Collectors.joining(" and ", "( ", " )")));
        }
        return String.join(" or ", disjuncts);
    }

    // FIXME we should fix prism parsing in midPoint to avoid this
    private void stripBlankStrings(SiSuggestObjectTypesResponseType response) {
        for (var objectType : response.getObjectType()) {
            objectType.getFilter().removeIf(f -> StringUtils.isBlank(f));
            objectType.setBaseContextObjectClassName(nullIfEmpty(objectType.getBaseContextObjectClassName()));
            objectType.setBaseContextFilter(nullIfEmpty(objectType.getBaseContextFilter()));
        }
    }

    private record ObjectTypeKey(
            ShadowKindType kind,
            String intent,
            @Nullable String baseContextObjectClassName,
            @Nullable String baseContextFilter) {
        static ObjectTypeKey of(SiSuggestedObjectTypeType bean) {
            return new ObjectTypeKey(
                    ShadowKindType.fromValue(bean.getKind()),
                    bean.getIntent(),
                    nullIfEmpty(bean.getBaseContextObjectClassName()),
                    nullIfEmpty(bean.getBaseContextFilter()));
        }
    }

    private Multimap<ObjectTypeKey, SiSuggestedObjectTypeType> groupObjectTypes(List<SiSuggestedObjectTypeType> beans) {
        // We keep the order deterministic mainly because of the tests.
        var map = LinkedHashMultimap.<ObjectTypeKey, SiSuggestedObjectTypeType>create();
        for (SiSuggestedObjectTypeType bean : beans) {
            map.put(ObjectTypeKey.of(bean), bean);
        }
        return map;
    }

    private static SearchFilterType parseAndSerializeFilter(
            String filterString, PrismObjectDefinition<ShadowType> shadowObjectDef)
            throws SchemaException {
        LOGGER.trace("Parsing filter: {}", filterString);
        var hackedFilterString = hackFilter(filterString);
        LOGGER.trace("Hacked filter: {}", hackedFilterString);
        try {
            var parsedFilter = PrismContext.get().createQueryParser().parseFilter(shadowObjectDef, hackedFilterString);
            return PrismContext.get().querySerializer().serialize(parsedFilter).toSearchFilterType();
        } catch (Exception e) {
            throw new SchemaException(
                    "Cannot process suggested filter (%s): %s".formatted(hackedFilterString, e.getMessage()),
                    e);
        }
    }

    // TEMPORARY - remove when service is fixed
    private static String hackFilter(String filterString) {
        if (filterString.contains("attributes/ri:") || filterString.contains("attributes/icfs:")) {
            return filterString; // probably correct, no need to hack
        } else {
            // hack to make it compatible with our filter parser
            return filterString
                    .replaceAll("ri:", "attributes/ri:")
                    .replaceAll("icfs:", "attributes/icfs:");
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
