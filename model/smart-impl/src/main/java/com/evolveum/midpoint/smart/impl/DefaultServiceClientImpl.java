/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.nullIfEmpty;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Arrays;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.common.rest.MidpointXmlProvider;
import com.evolveum.midpoint.common.rest.MidpointYamlProvider;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A client for the remote Smart integration service.
 *
 * TODO implement
 */
class DefaultServiceClientImpl implements ServiceClient {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultServiceClientImpl.class);

    private static final String URL_PREFIX = "/api/v1/";
    private static final String METHOD_SUGGEST_OBJECT_TYPES = "objectType/suggestObjectType"; // This should be plural!
    private static final String METHOD_SUGGEST_FOCUS_TYPE = "focusType/suggestFocusType";

    /** TODO decide if we use this client or not. */
    private final WebClient webClient;

    // TODO decide if we use these providers or not.
    @Autowired private MidpointXmlProvider<?> xmlProvider;
    @Autowired private MidpointJsonProvider<?> jsonProvider;
    @Autowired private MidpointYamlProvider<?> yamlProvider;

    DefaultServiceClientImpl(@Nullable SmartIntegrationConfigurationType configurationBean) throws ConfigurationException {
        webClient = WebClient.create(
                getServiceUrl(configurationBean),
                Arrays.asList(xmlProvider, jsonProvider, yamlProvider),
                true);
    }

    private static String getServiceUrl(@Nullable SmartIntegrationConfigurationType configurationBean)
            throws ConfigurationException {
        var urlOverride = System.getProperty(MidpointConfiguration.SMART_INTEGRATION_SERVICE_URL_OVERRIDE);
        if (urlOverride != null) {
            return urlOverride;
        }
        return MiscUtil.configNonNull(
                configurationBean != null ? configurationBean.getServiceUrl() : null,
                "Smart integration service URL is not configured. "
                        + "Please set it in the configuration or via system property "
                        + MidpointConfiguration.SMART_INTEGRATION_SERVICE_URL_OVERRIDE);
    }

    @Override
    public ObjectTypesSuggestionType suggestObjectTypes(
            ResourceObjectClassDefinition objectClassDef,
            ShadowObjectClassStatisticsType shadowObjectClassStatistics,
            ResourceSchema resourceSchema,
            Task task,
            OperationResult result) throws SchemaException {

        var siRequest = new SiSuggestObjectTypesRequestType()
                .schema(serializeSchema(objectClassDef))
                .statistics(shadowObjectClassStatistics);

        var siResponse = callService(METHOD_SUGGEST_OBJECT_TYPES, siRequest, SiSuggestObjectTypesResponseType.class);
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
                delineation.filter(parseAndSerializeFilter(filterString, shadowObjectDef));
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
        var parsedFilter = PrismContext.get().createQueryParser().parseFilter(shadowObjectDef, filterString);
        try {
            return PrismContext.get().querySerializer().serialize(parsedFilter).toSearchFilterType();
        } catch (PrismQuerySerialization.NotSupportedException e) {
            throw SystemException.unexpected(
                    e,
                    "Cannot serialize filter: %s for shadow object definition: %s".formatted(filterString, shadowObjectDef));
        }
    }

    /** Calls the `suggestFocusType` method on the remote service. */
    @Override
    public QName suggestFocusType(
            ResourceObjectTypeIdentification typeIdentification,
            ResourceObjectClassDefinition objectClassDef,
            ResourceObjectTypeDelineation delineation,
            Task task,
            OperationResult result) throws SchemaException {

        var request = new SiSuggestFocusTypeRequestType()
                .kind(typeIdentification.getKind().value())
                .intent(typeIdentification.getIntent())
                .schema(serializeSchema(objectClassDef));

        setBaseContextFilter(request, objectClassDef, delineation);

        var response = callService(METHOD_SUGGEST_FOCUS_TYPE, request, SiSuggestFocusTypeResponseType.class);
        var typeName = response.getFocusTypeName();
        LOGGER.trace("Type suggested by the service: {}", typeName);
        return new QName(
                SchemaConstants.NS_C,
                MiscUtil.requireNonNull(typeName, "No returned type name from the service"));
    }

    private SiObjectClassSchemaType serializeSchema(ResourceObjectClassDefinition objectClassDef) {
        var schema = new SiObjectClassSchemaType()
                .name(objectClassDef.getObjectClassLocalName())
                .description(objectClassDef.getDescription()); // TODO change to native description
        for (ShadowAttributeDefinition<?, ?, ?, ?> attributeDefinition : objectClassDef.getAttributeDefinitions()) {
            schema.getAttribute().add(
                    new SiAttributeDefinitionType()
                            .name(attributeDefinition.getItemName().getLocalPart())
                            .type(attributeDefinition.getTypeName())
                            .description(attributeDefinition.getDescription())); // TODO change to native description
        }
        return schema;
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

    /** A generic method that calls a remote service. Treats serialization/parsing of the exchanged data. */
    private <REQ, RESP> RESP callService(String method, REQ request, Class<RESP> responseClass)
            throws SchemaException {
        // FIXME this is a temporary hack to work around limitations of our JSON serializer/deserializer.
        //  So we serialize/deserialize the data ourselves.
        var requestText = PrismContext.get().jsonSerializer().serializeRealValueContent(request);
        LOGGER.trace("Calling {} with request (class: {}):\n{}", method, request.getClass().getName(), requestText);
        webClient.type(MediaType.APPLICATION_JSON);
        webClient.accept(MediaType.APPLICATION_JSON);
        webClient.path(URL_PREFIX + method);
        try (var response = webClient.post(requestText)) {
            var statusType = response.getStatusInfo();
            var responseText = response.readEntity(String.class);
            LOGGER.trace("Response (status: {}, expected class: {}):\n{}",
                    statusType.getStatusCode(), responseClass, responseText);
            if (statusType.getFamily() == Response.Status.Family.SUCCESSFUL) {
                // Another hack: we don't have "parseRealValueContent" method that would parse the response.
                // So we wrap it in a JSON object that will look like a regularly serialized Item.
                var wrappedResponseText = "{ \"wrapper\": " + responseText + " }";
                return PrismContext.get().parserFor(wrappedResponseText).parseRealValue(responseClass);
            } else {
                throw new SystemException("Service call (%s) failed with status: %d %s".formatted(
                        method, statusType.getStatusCode(), statusType.getReasonPhrase()));
            }
        }
    }

    @Override
    public void close() {
        webClient.close();
    }
}
