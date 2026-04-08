/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.toMillis;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowObjectTypeUtil;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@Service
public class SchemaMatchService {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaMatchService.class);

    private static final String OP_GET_LATEST_OBJECT_TYPE_SCHEMA_MATCH = "getLatestObjectTypeSchemaMatch";
    private static final String OP_SAVE_SCHEMA_MATCH = "saveSchemaMatch";

    /** Default time-to-live for schema match objects if not configured. */
    private static final Duration DEFAULT_SCHEMA_MATCH_TTL = XmlTypeConverter.createDuration("P1D");

    private final RepositoryService repositoryService;
    private final ServiceClientFactory clientFactory;
    private final WellKnownSchemaService wellKnownSchemaService;
    private final SystemObjectCache systemObjectCache;

    public SchemaMatchService(
            @Qualifier("cacheRepositoryService") RepositoryService repositoryService,
            ServiceClientFactory clientFactory,
            WellKnownSchemaService wellKnownSchemaService,
            SystemObjectCache systemObjectCache) {
        this.repositoryService = repositoryService;
        this.clientFactory = clientFactory;
        this.wellKnownSchemaService = wellKnownSchemaService;
        this.systemObjectCache = systemObjectCache;
    }

    public SchemaMatchResultType loadSchemaMatch(ObjectReferenceType schemaMatchRef, OperationResult result) {
        try {
            if (schemaMatchRef == null) {
                return null;
            }
            var schemaMatchOid = Referencable.getOid(schemaMatchRef);
            if (schemaMatchOid == null) {
                return null;
            }
            var schemaMatchObject = repositoryService
                    .getObject(GenericObjectType.class, schemaMatchOid, null, result)
                    .asObjectable();
            return ShadowObjectTypeUtil.getObjectTypeSchemaMatchRequired(schemaMatchObject);
        } catch (Exception e) {
            LOGGER.warn("Failed to load schema match, proceeding without it: {}", e.getMessage());
            return null;
        }
    }

    public SchemaMatchResultType computeSchemaMatch(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            boolean useAiService,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var result = parentResult.subresult("computeSchemaMatch")
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeIdentification", typeIdentification)
                .build();
        try (var serviceClient = this.clientFactory.getServiceClient(result)) {
            var options = GetOperationOptionsBuilder.create()
                    .item(ResourceType.F_CONNECTOR_REF).resolve()
                    .build();
            var ctx = TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, options, null, task, result);
            var focusTypeDefinition = ctx.getFocusTypeDefinition();
            var matchingOp = new SchemaMatchingOperation(serviceClient, wellKnownSchemaService, useAiService);
            var match = matchingOp.matchSchema(ctx.typeDefinition, focusTypeDefinition, ctx.resource);

            SchemaMatchResultType schemaMatchResult = new SchemaMatchResultType()
                    .timestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));

            var detectedSchemaType = matchingOp.getDetectedSchemaType();
            if (detectedSchemaType != null) {
                schemaMatchResult.setWellKnownSchemaType(detectedSchemaType.name());
                LOGGER.debug("Stored known schema type: {} for resource {}", detectedSchemaType, resourceOid);
            }

            for (var attributeMatch : match.getAttributeMatch()) {
                processAttributeMatch(attributeMatch, matchingOp, ctx, focusTypeDefinition)
                        .ifPresent(schemaMatchResult.getSchemaMatchResult()::add);
            }
            return schemaMatchResult;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private Optional<SchemaMatchOneResultType> processAttributeMatch(
            SiAttributeMatchSuggestionType attributeMatch,
            SchemaMatchingOperation matchingOp,
            TypeOperationContext ctx,
            PrismObjectDefinition<?> focusTypeDefinition) {
        var shadowAttrPath = matchingOp.getApplicationItemPath(attributeMatch.getApplicationAttribute());
        if (shadowAttrPath.size() != 2 || !shadowAttrPath.startsWith(ShadowType.F_ATTRIBUTES)) {
            LOGGER.warn("Ignoring attribute {}. It is not a traditional attribute.", shadowAttrPath);
            return Optional.empty();
        }

        var shadowAttrName = shadowAttrPath.rest().asSingleNameOrFail();
        var shadowAttrDef = ctx.typeDefinition.findSimpleAttributeDefinition(shadowAttrName);
        if (shadowAttrDef == null) {
            LOGGER.warn("No shadow attribute definition found for {}. Skipping schema match record.", shadowAttrName);
            return Optional.empty();
        }

        var focusPropPath = matchingOp.getFocusItemPath(attributeMatch.getMidPointAttribute());
        var focusPropDef = focusTypeDefinition.findPropertyDefinition(focusPropPath);
        if (focusPropDef == null) {
            LOGGER.warn("No focus property definition found for {}. Skipping schema match record.", focusPropPath);
            return Optional.empty();
        }

        //TODO this is a nasty hack for MCM demo, remove later.
        var applicationAttrDefBean = createAttributeDefinition(
                shadowAttrPath, shadowAttrDef, ctx.getShadowDefinition());
        if (isLdapResource(ctx.resource)) {
            applicationAttrDefBean.setMaxOccurs(1);
        }
        var midPointPropertyDefBean = createAttributeDefinition(
                focusPropPath, focusPropDef, focusTypeDefinition);

        SchemaMatchOneResultType result = new SchemaMatchOneResultType()
                .shadowAttributePath(shadowAttrPath.toStringStandalone())
                .shadowAttribute(applicationAttrDefBean)
                .focusPropertyPath(focusPropPath.toStringStandalone())
                .focusProperty(midPointPropertyDefBean)
                .isSystemProvided(Boolean.TRUE.equals(attributeMatch.getIsSystemProvided()));

        return Optional.of(result);
    }

    private SiAttributeDefinitionType createAttributeDefinition(
            ItemPath path,
            PrismPropertyDefinition<?> definition,
            PrismObjectDefinition<?> objectDefinition) {
        return new SiAttributeDefinitionType()
                .name(DescriptiveItemPath.of(path, objectDefinition).asString())
                .type(getTypeName(definition))
                .minOccurs(definition.getMinOccurs())
                .maxOccurs(definition.getMaxOccurs());
    }

    public GenericObjectType getLatestObjectTypeSchemaMatch(String resourceOid, String kind, String intent, OperationResult parentResult)
            throws SchemaException {
        var result = parentResult.subresult(OP_GET_LATEST_OBJECT_TYPE_SCHEMA_MATCH)
                .addParam("resourceOid", resourceOid)
                .addParam("kind", kind)
                .addParam("intent", intent)
                .build();
        try {
            var objects = repositoryService.searchObjects(
                    GenericObjectType.class,
                    PrismContext.get().queryFor(GenericObjectType.class)
                            .item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_RESOURCE_OID)
                            .eq(resourceOid)
                            .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_KIND_NAME)
                            .eq(kind)
                            .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_INTENT_NAME)
                            .eq(intent)
                            .build(),
                    null,
                    result);

            var latestSchemaMatch = objects.stream()
                    .map(o -> o.asObjectable())
                    .filter(o ->
                            ObjectTypeUtil.getExtensionItemRealValue(
                                    o.getExtension(), MODEL_EXTENSION_OBJECT_TYPE_SCHEMA_MATCH) != null)
                    .max(Comparator.comparing(
                            o -> toMillis(ShadowObjectTypeUtil.getObjectTypeSchemaMatchRequired(o).getTimestamp())))
                    .orElse(null);

            return deleteIfExpired(latestSchemaMatch, resourceOid, kind, intent, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    //TODO this is a nasty hack for MCM demo, remove later.
    private boolean isLdapResource(ResourceType resource) {
        var connectorRef = resource.getConnectorRef();
        if (connectorRef == null) {
            return false;
        }
        if (connectorRef.getObject() != null && connectorRef.getObject().getRealValue() instanceof ConnectorType connector) {
            String bundle = connector.getConnectorBundle();
            String type = connector.getConnectorType();
            return (bundle != null && bundle.toLowerCase().contains("ldap"))
                    || (type != null && type.toLowerCase().contains("ldap"));
        }
        return false;
    }

    /**
     * Saves the schema match result as a generic object. Deletes any existing schema match objects
     * for the same resource/kind/intent before saving the new one.
     *
     * @return OID of the newly created schema match object
     */
    public String saveSchemaMatch(String resourceOid, String kind, String intent,
            SchemaMatchResultType schemaMatch, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException {
        var result = parentResult.subresult(OP_SAVE_SCHEMA_MATCH)
                .addParam("resourceOid", resourceOid)
                .addParam("kind", kind)
                .addParam("intent", intent)
                .build();
        try {
            deleteSchemaMatchObjects(resourceOid, kind, intent, result);
            var schemaMatchObject = ShadowObjectTypeUtil.createObjectTypeSchemaMatchObject(resourceOid, kind, intent, schemaMatch);
            LOGGER.debug("Adding schema match object:\n{}", schemaMatchObject.debugDump(1));
            var oid = repositoryService.addObject(schemaMatchObject.asPrismObject(), null, result);
            LOGGER.debug("Saved schema match object with OID {}", oid);
            return oid;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private void deleteSchemaMatchObjects(String resourceOid, String kind, String intent,
            OperationResult result) throws SchemaException {
        var objects = repositoryService.searchObjects(
                GenericObjectType.class,
                PrismContext.get().queryFor(GenericObjectType.class)
                        .item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_RESOURCE_OID)
                        .eq(resourceOid)
                        .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_KIND_NAME)
                        .eq(kind)
                        .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_INTENT_NAME)
                        .eq(intent)
                        .build(),
                null,
                result);
        for (var obj : objects) {
            if (ObjectTypeUtil.getExtensionItemRealValue(
                    obj.asObjectable().getExtension(), MODEL_EXTENSION_OBJECT_TYPE_SCHEMA_MATCH) != null) {
                deleteSchemaMatchObject(obj.getOid(), result);
            }
        }
    }

    private void deleteSchemaMatchObject(String oid, OperationResult result) {
        try {
            repositoryService.deleteObject(GenericObjectType.class, oid, result);
            LOGGER.debug("Deleted schema match object {}", oid);
        } catch (Exception e) {
            LOGGER.warn("Failed to delete schema match object {}: {}", oid, e.getMessage(), e);
        }
    }

    /**
     * Retrieves the configured TTL for schema match objects from system configuration.
     * Falls back to default 24 hours if not configured.
     */
    private Duration getConfiguredTTL(OperationResult result) {
        try {
            return Optional.ofNullable(systemObjectCache.getSystemConfiguration(result))
                    .map(o -> o.asObjectable().getSmartIntegration())
                    .map(SmartIntegrationConfigurationType::getSchemaMatchTtl)
                    .map(ttl -> { LOGGER.debug("Using configured TTL for schema match: {}", ttl); return ttl; })
                    .orElse(DEFAULT_SCHEMA_MATCH_TTL);
        } catch (Exception e) {
            LOGGER.warn("Failed to retrieve configured schema match TTL, using default: {}", e.getMessage());
        }
        return DEFAULT_SCHEMA_MATCH_TTL;
    }

    /**
     * Deletes the schema match object if it has expired based on the configured TTL.
     * Returns null if the schema match was expired and deleted, otherwise returns the original object.
     */
    private GenericObjectType deleteIfExpired(GenericObjectType schemaMatchObject, String resourceOid,
            String kind, String intent, OperationResult result) {
        if (schemaMatchObject == null) {
            return null;
        }
        var schemaMatch = ShadowObjectTypeUtil.getObjectTypeSchemaMatchRequired(schemaMatchObject);
        if (isSchemaMatchExpired(schemaMatch.getTimestamp(), result)) {
            LOGGER.info("Schema match for resource {}/{}/{} expired, deleting",
                    resourceOid, kind, intent);
            deleteSchemaMatchObject(schemaMatchObject.getOid(), result);
            return null;
        }
        return schemaMatchObject;
    }

    /**
     * Checks if a schema match object has expired based on the configured TTL.
     */
    private boolean isSchemaMatchExpired(XMLGregorianCalendar timestamp, OperationResult result) {
        if (timestamp == null) {
            return true;
        }
        Duration ttl = getConfiguredTTL(result);
        XMLGregorianCalendar expirationTime = XmlTypeConverter.addDuration(timestamp, ttl);
        return XmlTypeConverter.isBeforeNow(expirationTime);
    }

    private QName getTypeName(@NotNull PrismPropertyDefinition<?> propertyDefinition) {
        if (propertyDefinition.isEnum()) {
            return DOMUtil.XSD_STRING;
        }
        var typeName = propertyDefinition.getTypeName();
        if (QNameUtil.match(PolyStringType.COMPLEX_TYPE, typeName)) {
            return DOMUtil.XSD_STRING;
        } else if (QNameUtil.match(ProtectedStringType.COMPLEX_TYPE, typeName)) {
            return DOMUtil.XSD_STRING;
        } else {
            return typeName;
        }
    }
}
