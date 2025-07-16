/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.Collection;
import java.util.function.Supplier;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

@Service
public class SmartIntegrationServiceImpl implements SmartIntegrationService {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationServiceImpl.class);

    private static final String OP_SUGGEST_OBJECT_TYPES = "suggestObjectTypes";
    private static final String OP_SUGGEST_FOCUS_TYPE = "suggestFocusType";
    private static final String OP_SUGGEST_MAPPINGS = "suggestMappings";
    private static final String OP_SUGGEST_ASSOCIATIONS = "suggestAssociations";

    /** Supplies a mock service client for testing purposes. */
    @TestOnly
    @Nullable private Supplier<ServiceClient> serviceClientSupplier;

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ModelService modelService;
    @Autowired private Clock clock;

    @Override
    public ObjectTypesSuggestionType suggestObjectTypes(
            String resourceOid, QName objectClassName, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_SUGGEST_OBJECT_TYPES)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();
        try {
            LOGGER.debug("Suggesting object types for resourceOid {}, objectClassName {}", resourceOid, objectClassName);
            try (var serviceClient = getServiceClient(result)) {
                var resource = modelService.getObject(ResourceType.class, resourceOid, null, task, result);
                var shadowObjectClassStatistics = new ShadowObjectClassStatisticsType()
                        .size(1000)
                        .coverage(1.0f)
                        .timestamp(clock.currentTimeXMLGregorianCalendar())
                        .attribute(new ShadowAttributeStatisticsType()
                                .ref(ICFS_NAME)
                                .missingValueCount(0))
                        .attribute(new ShadowAttributeStatisticsType()
                                .ref(new QName(NS_RI, "gender"))
                                .valueCount(new ShadowAttributeValueCountType()
                                        .value("male")
                                        .count(490))
                                .valueCount(new ShadowAttributeValueCountType()
                                        .value("female")
                                        .count(500))
                                .missingValueCount(10));
//                        .attributeTuple(new ShadowAttributeTupleStatisticsType()
//                                .ref(new QName(NS_RI, "city"))
//                                .ref(new QName(NS_RI, "gender"))
//                                .tupleCount(new ShadowAttributeTupleCountType()
//                                        .value("Bratislava")
//                                        .value("male")
//                                        .count(200))
//                                .tupleCount(new ShadowAttributeTupleCountType()
//                                        .value("Bratislava")
//                                        .value("female")
//                                        .count(210)));

                var objectClassDef = Resource.of(resource)
                        .getCompleteSchemaRequired()
                        .findObjectClassDefinitionRequired(objectClassName);
                var objectTypes = serviceClient.suggestObjectTypes(objectClassDef, shadowObjectClassStatistics, task, result);
                LOGGER.debug("Suggested object types: {}", objectTypes);
                return objectTypes;
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public QName suggestFocusType(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        var result = parentResult.subresult(OP_SUGGEST_FOCUS_TYPE)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeIdentification", typeIdentification)
                .build();
        try {
            LOGGER.debug("Suggesting focus type for resourceOid {}, typeIdentification {}", resourceOid, typeIdentification);
            try (var serviceClient = getServiceClient(result)) {
                var resource = modelService.getObject(ResourceType.class, resourceOid, null, task, result);
                var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
                var objectTypeDef = resourceSchema.getObjectTypeDefinitionRequired(typeIdentification);
                var objectClassDef = resourceSchema.findObjectClassDefinitionRequired(objectTypeDef.getObjectClassName());
                var type = serviceClient.suggestFocusType(
                        typeIdentification, objectClassDef, objectTypeDef.getDelineation(), task, result);
                LOGGER.debug("Suggested focus type: {}", type);
                return type;
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public MappingsSuggestionType suggestMappings(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            QName focusTypeName,
            @Nullable MappingsSuggestionFiltersType filters,
            MappingsSuggestionInteractionMetadataType interactionMetadata,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_SUGGEST_MAPPINGS)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeIdentification", typeIdentification)
                .addParam("focusTypeName", focusTypeName)
                .build();
        try {
            var resource = modelService.getObject(ResourceType.class, resourceOid, null, task, result);
            // ...
            return new MappingsSuggestionType(); // TODO replace with real implementation
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public AssociationsSuggestionType suggestAssociations(
            String resourceOid,
            Collection<ResourceObjectTypeIdentification> subjectTypeIdentifications,
            Collection<ResourceObjectTypeIdentification> objectTypeIdentifications,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_SUGGEST_ASSOCIATIONS)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectCollectionAsContext("subjectTypeIdentifications", subjectTypeIdentifications)
                .addArbitraryObjectCollectionAsContext("objectTypeIdentifications", objectTypeIdentifications)
                .build();
        try {
            var resource = modelService.getObject(ResourceType.class, resourceOid, null, task, result);
            var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
            var nativeSchema = resourceSchema.getNativeSchema();
            // ...
            return new AssociationsSuggestionType(); // TODO replace with real implementation
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private ServiceClient getServiceClient(OperationResult result) throws SchemaException, ConfigurationException {
        if (serviceClientSupplier != null) {
            return serviceClientSupplier.get();
        }
        var smartIntegrationConfiguration =
                SystemConfigurationTypeUtil.getSmartIntegrationConfiguration(
                        systemObjectCache.getSystemConfigurationBean(result));
        return new DefaultServiceClientImpl(smartIntegrationConfiguration);
    }

    public void setServiceClientSupplier(@Nullable Supplier<ServiceClient> serviceClientSupplier) {
        this.serviceClientSupplier = serviceClientSupplier;
    }
}
