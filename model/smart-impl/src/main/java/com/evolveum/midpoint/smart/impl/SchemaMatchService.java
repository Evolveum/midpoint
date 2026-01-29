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

import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowObjectTypeStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.smart.impl.wellknownschemas.WellKnownSchemaService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
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

    private final RepositoryService repositoryService;
    private final ServiceClientFactory clientFactory;
    private final WellKnownSchemaService wellKnownSchemaService;

    public SchemaMatchService(
            @Qualifier("cacheRepositoryService") RepositoryService repositoryService,
            ServiceClientFactory clientFactory,
            WellKnownSchemaService wellKnownSchemaService) {
        this.repositoryService = repositoryService;
        this.clientFactory = clientFactory;
        this.wellKnownSchemaService = wellKnownSchemaService;
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
            var ctx = TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, null, task, result);
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

        var applicationAttrDefBean = createAttributeDefinition(
                shadowAttrPath, shadowAttrDef, ctx.getShadowDefinition());
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

    public GenericObjectType getLatestObjectTypeSchemaMatch(String resourceOid, String kind, String intent, Task task, OperationResult parentResult)
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
            return objects.stream()
                    .map(o -> o.asObjectable())
                    .filter(o ->
                            ObjectTypeUtil.getExtensionItemRealValue(
                                    o.getExtension(), MODEL_EXTENSION_OBJECT_TYPE_SCHEMA_MATCH) != null)
                    .max(Comparator.comparing(
                            o -> toMillis(ShadowObjectTypeStatisticsTypeUtil.getObjectTypeSchemaMatchRequired(o).getTimestamp())))
                    .orElse(null);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
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
