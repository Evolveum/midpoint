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

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;

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
            ResourceSchema resourceSchema,
            ResourceType resource)
            throws SchemaException {
        var siRequest = new SiSuggestObjectTypesRequestType()
                .schema(ResourceObjectClassSchemaSerializer.serialize(objectClassDef, resource))
                .statistics(shadowObjectClassStatistics);

        var siResponse = serviceClient.invoke(SUGGEST_OBJECT_TYPES, siRequest, SiSuggestObjectTypesResponseType.class);
        stripBlankStrings(siResponse);

        var response = new ObjectTypesSuggestionType();

        var shadowObjectDef = objectClassDef.getPrismObjectDefinition();

        var typeIdsSeen = new HashSet<ResourceObjectTypeIdentification>();
        for (var siObjectType : siResponse.getObjectType()) {
            var delineation =
                    new ResourceObjectTypeDelineationType()
                            .objectClass(objectClassDef.getTypeName());

            for (String filterString : siObjectType.getFilter()) {
                delineation.filter(parseAndSerializeFilter(filterString, shadowObjectDef));
            }

            var siBaseContextClassLocalName = siObjectType.getBaseContextObjectClassName();
            var siBaseContextFilter = siObjectType.getBaseContextFilter();
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

    /** Calls the `suggestFocusType` method on the remote service. */
    QName suggestFocusType(
            ResourceObjectTypeIdentification typeIdentification,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation,
            ResourceType resource)
            throws SchemaException {
        var request = new SiSuggestFocusTypeRequestType()
                .kind(typeIdentification.getKind().value())
                .intent(typeIdentification.getIntent())
                .schema(ResourceObjectClassSchemaSerializer.serialize(objectClassDef, resource));

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

    AttributeMappingsSuggestionType suggestMapping(
            ItemPath shadowAttrPath,
            ShadowSimpleAttributeDefinition<?> attrDef,
            ItemPath focusPropPath,
            PrismPropertyDefinition<?> propertyDef,
            Collection<ValuesPair> valuesPairs) throws SchemaException {

        String transformationScript;
        if (!valuesPairs.isEmpty()) {
            var applicationAttrDefBean = new SiAttributeDefinitionType()
                    .name(shadowAttrPath.toBean())
                    .type(getTypeName(attrDef))
                    .minOccurs(attrDef.getMinOccurs())
                    .maxOccurs(attrDef.getMaxOccurs());
            var midPointPropertyDefBean = new SiAttributeDefinitionType()
                    .name(focusPropPath.toBean())
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
                                    applicationAttrDefBean.getName(), midPointPropertyDefBean.getName())));
            var siResponse = serviceClient.invoke(SUGGEST_MAPPING, siRequest, SiSuggestMappingResponseType.class);
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
        return new AttributeMappingsSuggestionType()
                .definition(new ResourceAttributeDefinitionType()
                        .ref(shadowAttrPath.rest().toBean()) // FIXME! what about activation, credentials, etc?
                        .inbound(new InboundMappingType()
                                .target(new VariableBindingDefinitionType()
                                        .path(focusPropPath.toBean()))
                                .expression(expression)));
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
        } else {
            return typeName;
        }
    }

    /** Returns suggestions for correlators - in the same order as the correlators are provided. */
    List<CorrelatorSuggestion> suggestCorrelationMappings(
            ResourceObjectTypeDefinition objectTypeDef,
            PrismObjectDefinition<?> focusDef,
            List<? extends ItemPath> correlators,
            ResourceType resource)
            throws SchemaException {
        var siResponse = matchSchema(objectTypeDef, focusDef, resource);
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

    SiMatchSchemaResponseType matchSchema(
            ResourceObjectTypeDefinition objectTypeDef, PrismObjectDefinition<?> focusDef, ResourceType resource)
            throws SchemaException {
        var siRequest = new SiMatchSchemaRequestType()
                .applicationSchema(
                        ResourceObjectClassSchemaSerializer.serialize(objectTypeDef.getObjectClassDefinition(), resource))
                .midPointSchema(PrismComplexTypeDefinitionSerializer.serialize(focusDef));
        return serviceClient.invoke(MATCH_SCHEMA, siRequest, SiMatchSchemaResponseType.class);
    }

    record ValuesPair(Collection<?> shadowValues, Collection<?> focusValues) {
        private SiSuggestMappingExampleType toSiExample(
                ItemPathType applicationAttrNameBean, ItemPathType midPointPropertyNameBean) {
            return new SiSuggestMappingExampleType()
                    .application(toSiAttributeExample(applicationAttrNameBean, shadowValues))
                    .midPoint(toSiAttributeExample(midPointPropertyNameBean, focusValues));
        }

        private @NotNull SiAttributeExampleType toSiAttributeExample(ItemPathType pathBean, Collection<?> values) {
            var example = new SiAttributeExampleType().name(pathBean);
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
