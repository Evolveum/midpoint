/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.impl.controller.ModelInteractionServiceImpl;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.TaskManager;
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
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class SmartIntegrationServiceImpl implements SmartIntegrationService {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationServiceImpl.class);

    private static final String OP_SUGGEST_OBJECT_TYPES = "suggestObjectTypes";
    private static final String OP_SUBMIT_SUGGEST_OBJECT_TYPES_OPERATION = "suggestObjectTypesOperation";
    private static final String OP_GET_SUGGEST_OBJECT_TYPES_OPERATION_STATUS = "getSuggestObjectTypesOperationStatus";

    private static final String OP_SUGGEST_FOCUS_TYPE = "suggestFocusType";
    private static final String OP_SUGGEST_MAPPINGS = "suggestMappings";
    private static final String OP_SUGGEST_ASSOCIATIONS = "suggestAssociations";

    /** Auto cleanup time for background tasks created by the service. Will be shorter, probably. */
    private static final Duration AUTO_CLEANUP_TIME = XmlTypeConverter.createDuration("P1D");

    /** Supplies a mock service client for testing purposes. */
    @TestOnly
    @Nullable private Supplier<ServiceClient> serviceClientSupplier;

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ModelService modelService;
    @Autowired private ModelInteractionServiceImpl modelInteractionService;
    @Autowired private Clock clock;
    @Autowired private TaskManager taskManager;
    @Autowired private TaskActivityManager activityManager;

    @Override
    public String submitSuggestObjectTypesOperation(
            String resourceOid, QName objectClassName, Task task, OperationResult parentResult)
            throws CommonException {
        var result = parentResult.subresult(OP_SUBMIT_SUGGEST_OBJECT_TYPES_OPERATION)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();
        try {
            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .objectTypesSuggestion(new ObjectTypesSuggestionWorkDefinitionType()
                                            .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                                            .objectclass(objectClassName))),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name("Suggest object types for " + objectClassName.getLocalPart() + " on " + resourceOid)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task, result);
            LOGGER.debug("Submitted suggest object types operation for resourceOid {}, objectClassName {}: {}",
                    resourceOid, objectClassName, oid);
            return oid;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public StatusInformation<ObjectTypesSuggestionType> getSuggestObjectTypesOperationStatus(
            String token, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        var result = parentResult.subresult(OP_GET_SUGGEST_OBJECT_TYPES_OPERATION_STATUS)
                .addParam("token", token)
                .build();
        try {
            var bgTask = taskManager.getTaskPlain(token, result);
            LOGGER.trace("Task:\n{}", bgTask.debugDump(1));

            var taskStatus = Objects.requireNonNullElse(
                    OperationResultStatus.parseStatusType(bgTask.getResultStatus()),
                    OperationResultStatus.IN_PROGRESS);
            if (taskStatus == OperationResultStatus.IN_PROGRESS) {
                var activity = activityManager.getActivity(bgTask, ActivityPath.empty());
                // TODO message
                return new StatusInformation<>(taskStatus, null, null);
            }
            // We should have the state by now.
            var state = ActivityState.getActivityStateDownwards(
                    ActivityPath.empty(),
                    bgTask,
                    ObjectTypesSuggestionWorkStateType.COMPLEX_TYPE,
                    CommonTaskBeans.get(),
                    result);
            var suggestions = state.getWorkStateItemRealValueClone(
                    ObjectTypesSuggestionWorkStateType.F_RESULT, ObjectTypesSuggestionType.class);
            return new StatusInformation<>(taskStatus, null, suggestions);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /** Invokes the service client to suggest object types for the given resource and object class. */
    public ObjectTypesSuggestionType suggestObjectTypes(
            String resourceOid,
            QName objectClassName,
            ShadowObjectClassStatisticsType statistics,
            Task task,
            OperationResult parentResult)
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
                var objectClassDef = Resource.of(resource)
                        .getCompleteSchemaRequired()
                        .findObjectClassDefinitionRequired(objectClassName);
                var objectTypes = serviceClient.suggestObjectTypes(objectClassDef, statistics, task, result);
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
