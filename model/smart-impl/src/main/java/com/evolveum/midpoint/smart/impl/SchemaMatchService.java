/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.toMillis;

import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.SmartIntegrationArtifactUtil;
import com.evolveum.midpoint.util.exception.*;

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
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
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
    private final StatisticsService statisticsService;

    public SchemaMatchService(
            @Qualifier("cacheRepositoryService") RepositoryService repositoryService,
            ServiceClientFactory clientFactory,
            WellKnownSchemaService wellKnownSchemaService,
            SystemObjectCache systemObjectCache,
            StatisticsService statisticsService) {
        this.repositoryService = repositoryService;
        this.clientFactory = clientFactory;
        this.wellKnownSchemaService = wellKnownSchemaService;
        this.systemObjectCache = systemObjectCache;
        this.statisticsService = statisticsService;
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
                    .getObject(SmartIntegrationArtifactType.class, schemaMatchOid, null, result)
                    .asObjectable();
            return SmartIntegrationArtifactUtil.getObjectTypeSchemaMatchRequired(schemaMatchObject);
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
            ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
        var result = parentResult.subresult("computeSchemaMatch")
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeIdentification", typeIdentification)
                .build();
        try (var serviceClient = this.clientFactory.getServiceClient(result)) {
            var options = GetOperationOptionsBuilder.create()
                    .item(ResourceType.F_CONNECTOR_REF).resolve()
                    .build();
            var ctx = TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, options, null, task, result);
            var objectTypeStatistics = loadObjectTypeStats(resourceOid, typeIdentification, ctx.resource, ctx.typeDefinition, result);
            return doComputeSchemaMatch(
                    serviceClient, ctx.objectClassDefinition, ctx.getFocusTypeDefinition(), ctx.resource, useAiService,
                    objectTypeStatistics, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Computes schema match at the object class level using an explicit focus type name,
     * without requiring the object type (kind/intent) to be configured on the resource.
     * Useful for pre-loading schema matching during object type suggestions, before types are saved.
     */
    public SchemaMatchResultType computeSchemaMatchByObjectClass(
            String resourceOid,
            QName objectClassName,
            QName focusTypeName,
            boolean useAiService,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
        var result = parentResult.subresult("computeSchemaMatchByObjectClass")
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .addParam("focusTypeName", focusTypeName)
                .build();
        try (var serviceClient = this.clientFactory.getServiceClient(result)) {
            var options = GetOperationOptionsBuilder.create()
                    .item(ResourceType.F_CONNECTOR_REF).resolve()
                    .build();
            var ctx = OperationContext.init(serviceClient, resourceOid, objectClassName, options, task, result);
            var focusTypeDefinition = PrismContext.get().getSchemaRegistry()
                    .findObjectDefinitionByType(focusTypeName);
            if (focusTypeDefinition == null) {
                throw new SchemaException("Focus type definition not found for " + focusTypeName);
            }
            return doComputeSchemaMatch(
                    serviceClient, ctx.objectClassDefinition, focusTypeDefinition, ctx.resource, useAiService, null, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private SchemaMatchResultType doComputeSchemaMatch(
            ServiceClient serviceClient,
            ResourceObjectClassDefinition objectClassDef,
            PrismObjectDefinition<?> focusTypeDefinition,
            ResourceType resource,
            boolean useAiService,
            ObjectSetStatisticsType objectTypeStatistics,
            Task task,
            OperationResult result) {
        var matchingOp = new SchemaMatchingOperation(serviceClient, wellKnownSchemaService, useAiService, task, result);
        var match = matchingOp.matchSchema(objectClassDef, focusTypeDefinition, resource);

        SchemaMatchResultType schemaMatchResult = new SchemaMatchResultType()
                .timestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));

        var detectedSchemaType = matchingOp.getDetectedSchemaType();
        if (detectedSchemaType != null) {
            schemaMatchResult.setWellKnownSchemaType(detectedSchemaType.name());
            LOGGER.debug("Stored known schema type: {} for resource {}", detectedSchemaType, resource.getOid());
        }

        for (var attributeMatch : match.getAttributeMatch()) {
            processAttributeMatch(attributeMatch, matchingOp, objectClassDef, resource, focusTypeDefinition)
                    .ifPresent(schemaMatchResult.getSchemaMatchResult()::add);
        }
        new PostSchemaMatchHeuristics(focusTypeDefinition, objectTypeStatistics).applyAll(schemaMatchResult);
        return schemaMatchResult;
    }

    private Optional<SchemaMatchOneResultType> processAttributeMatch(
            SiAttributeMatchSuggestionType attributeMatch,
            SchemaMatchingOperation matchingOp,
            ResourceObjectClassDefinition objectClassDef,
            ResourceType resource,
            PrismObjectDefinition<?> focusTypeDefinition) {
        var shadowAttrPath = matchingOp.getApplicationItemPath(attributeMatch.getApplicationAttribute());
        if (shadowAttrPath.size() != 2 || !shadowAttrPath.startsWith(ShadowType.F_ATTRIBUTES)) {
            LOGGER.warn("Ignoring attribute {}. It is not a traditional attribute.", shadowAttrPath);
            return Optional.empty();
        }

        var shadowAttrName = shadowAttrPath.rest().asSingleNameOrFail();
        var shadowAttrDef = objectClassDef.findSimpleAttributeDefinition(shadowAttrName);
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

        var shadowAttrDescriptivePath = DescriptiveItemPath.empty()
                .append(ShadowType.F_ATTRIBUTES, false)
                .append(shadowAttrName, shadowAttrDef.isMultiValue());
        var applicationAttrDefBean = new SiAttributeDefinitionType()
                .name(shadowAttrDescriptivePath.asString())
                .type(getTypeName(shadowAttrDef))
                .minOccurs(shadowAttrDef.getMinOccurs())
                .maxOccurs(shadowAttrDef.getMaxOccurs());
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

    public SmartIntegrationArtifactType getLatestObjectTypeSchemaMatch(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, OperationResult parentResult)
            throws SchemaException {
        var result = parentResult.subresult(OP_GET_LATEST_OBJECT_TYPE_SCHEMA_MATCH)
                .addParam("resourceOid", resourceOid)
                .addParam("type", typeIdentification)
                .build();
        try {
            var objects = repositoryService.searchObjects(
                    SmartIntegrationArtifactType.class,
                    PrismContext.get().queryFor(SmartIntegrationArtifactType.class)
                            .item(SmartIntegrationArtifactUtil.PATH_SCOPE_RESOURCE_REF).ref(resourceOid)
                            .and().item(SmartIntegrationArtifactUtil.PATH_SCOPE_KIND).eq(typeIdentification.getKind())
                            .and().item(SmartIntegrationArtifactUtil.PATH_SCOPE_INTENT).eq(typeIdentification.getIntent())
                            .and().item(AssignmentHolderType.F_ARCHETYPE_REF)
                            .ref(SystemObjectsType.ARCHETYPE_SMART_INTEGRATION_SCHEMA_MATCH.value())
                            .build(),
                    null,
                    result);

            var latestSchemaMatch = objects.stream()
                    .map(o -> o.asObjectable())
                    .filter(o -> o.getSchemaMatch() != null)
                    .max(Comparator.comparing(
                            o -> toMillis(SmartIntegrationArtifactUtil.getObjectTypeSchemaMatchRequired(o).getTimestamp())))
                    .orElse(null);

            return deleteIfExpired(latestSchemaMatch, resourceOid, typeIdentification, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }


    /**
     * Saves the schema match result as a smart integration artifact. Deletes any existing schema match objects
     * for the same resource/kind/intent before saving the new one.
     *
     * @return OID of the newly created schema match object
     */
    public String saveSchemaMatch(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            SchemaMatchResultType schemaMatch,
            OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException {
        var result = parentResult.subresult(OP_SAVE_SCHEMA_MATCH)
                .addParam("resourceOid", resourceOid)
                .addParam("type", typeIdentification)
                .build();
        try {
            deleteSchemaMatchObjects(resourceOid, typeIdentification, result);
            var schemaMatchObject = SmartIntegrationArtifactUtil.createSchemaMatchArtifact(
                    resourceOid, typeIdentification, schemaMatch);
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

    private void deleteSchemaMatchObjects(String resourceOid, ResourceObjectTypeIdentification type, OperationResult result)
            throws SchemaException {
        var objects = repositoryService.searchObjects(
                SmartIntegrationArtifactType.class,
                PrismContext.get().queryFor(SmartIntegrationArtifactType.class)
                        .item(SmartIntegrationArtifactUtil.PATH_SCOPE_RESOURCE_REF).ref(resourceOid)
                        .and().item(SmartIntegrationArtifactUtil.PATH_SCOPE_KIND).eq(type.getKind())
                        .and().item(SmartIntegrationArtifactUtil.PATH_SCOPE_INTENT).eq(type.getIntent())
                        .and().item(AssignmentHolderType.F_ARCHETYPE_REF)
                        .ref(SystemObjectsType.ARCHETYPE_SMART_INTEGRATION_SCHEMA_MATCH.value())
                        .build(),
                null,
                result);
        for (var obj : objects) {
            deleteSchemaMatchObject(obj.getOid(), result);
        }
    }

    private void deleteSchemaMatchObject(String oid, OperationResult result) {
        try {
            repositoryService.deleteObject(SmartIntegrationArtifactType.class, oid, result);
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
    private SmartIntegrationArtifactType deleteIfExpired(
            SmartIntegrationArtifactType schemaMatchObject,
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            OperationResult result) {
        if (schemaMatchObject == null) {
            return null;
        }
        var schemaMatch = SmartIntegrationArtifactUtil.getObjectTypeSchemaMatchRequired(schemaMatchObject);
        if (isSchemaMatchExpired(schemaMatch.getTimestamp(), result)) {
            LOGGER.info("Schema match for resource {}/{} expired, deleting", resourceOid, typeIdentification);
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

    private ObjectSetStatisticsType loadObjectTypeStats(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            ResourceType resource,
            ResourceObjectTypeDefinition typeDefinition,
            OperationResult result) {
        try {
            var statsObj = statisticsService.getLatestObjectTypeStatistics(resourceOid, typeIdentification, result);
            if (statsObj != null) {
                return SmartIntegrationArtifactUtil.getStatisticsRequired(statsObj);
            }
            LOGGER.info("No object type statistics found for {}/{}/{}; computing synchronously",
                    resourceOid, typeIdentification.getKind().value(), typeIdentification.getIntent());
            return statisticsService.computeObjectTypeStatisticsSync(
                    resourceOid,
                    resource.getName() != null ? resource.getName().getOrig() : null,
                    typeIdentification,
                    typeDefinition,
                    result);
        } catch (Exception e) {
            LOGGER.warn("Failed to load object type statistics for uniqueness filter: {}", e.getMessage());
            return null;
        }
    }

    static QName getTypeName(@NotNull PrismPropertyDefinition<?> propertyDefinition) {
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
