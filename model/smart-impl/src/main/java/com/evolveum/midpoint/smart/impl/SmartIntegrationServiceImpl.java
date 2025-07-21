/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.controller.ModelInteractionServiceImpl;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsSimulateType;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.toMillis;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.MODEL_EXTENSION_RESOURCE_OID;

@Service("smartIntegrationService")
public class SmartIntegrationServiceImpl implements SmartIntegrationService {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationServiceImpl.class);

    private static final String OP_CREATE_NEW_RESOURCE = "createNewResource";
    private static final String OP_ESTIMATE_OBJECT_CLASS_SIZE = "estimateObjectClassSize";
    private static final String OP_GET_LATEST_STATISTICS = "getLatestStatistics";
    private static final String OP_SUGGEST_OBJECT_TYPES = "suggestObjectTypes";
    private static final String OP_SUBMIT_SUGGEST_OBJECT_TYPES_OPERATION = "suggestObjectTypesOperation";
    private static final String OP_GET_SUGGEST_OBJECT_TYPES_OPERATION_STATUS = "getSuggestObjectTypesOperationStatus";
    private static final String OP_LIST_SUGGEST_OBJECT_TYPES_OPERATION_STATUSES = "listSuggestObjectTypesOperationStatuses";

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
    @Autowired private TaskManager taskManager;
    @Autowired private TaskActivityManager activityManager;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @Override
    public @Nullable String createNewResource(
            PolyStringType name,
            ObjectReferenceType connectorRef,
            ConnectorConfigurationType connectorConfiguration,
            Task task,
            OperationResult parentResult) {

        var result = parentResult.subresult(OP_CREATE_NEW_RESOURCE)
                .addArbitraryObjectAsParam("name", name)
                .build();
        try {
            var resource = new ResourceType()
                    .name(name)
                    .connectorRef(connectorRef)
                    .connectorConfiguration(connectorConfiguration)
                    .lifecycleState(SchemaConstants.LIFECYCLE_PROPOSED);

            // TODO consider setting full caching here
            var options = new ImportOptionsType()
                    .fetchResourceSchema(true); // this will execute "test connection" operation
            modelService.importObject(resource.asPrismObject(), options, task, result);

            return resource.getOid();
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public ObjectClassSizeEstimationType estimateObjectClassSize(
            String resourceOid, QName objectClassName, int maxSizeForEstimation, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_ESTIMATE_OBJECT_CLASS_SIZE)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();
        try {
            var resourceObject = modelService.getObject(ResourceType.class, resourceOid, null, task, result);
            var resource = Resource.of(resourceObject);
            var query = resource.queryFor(objectClassName).build();
            var objectClassSchema = resource.getCompleteSchemaRequired().findObjectClassDefinitionRequired(objectClassName);
            // Most probably the capability is not present, as - currently - it can be only enabled manually.
            // We can try to add automatic determination of the capability in the future.
            var capability =
                    objectClassSchema.getEnabledCapability(CountObjectsCapabilityType.class, resourceObject.asObjectable());
            if (capability != null) {
                var simulate = capability.getSimulate();
                if (simulate == null || simulate == CountObjectsSimulateType.PAGED_SEARCH_ESTIMATE) {
                    LOGGER.trace("Trying to estimate size of object class {} on {}; capability is present, simulate = {}",
                            objectClassName, resourceObject, simulate);
                    Integer count = null;
                    try {
                        count = modelService.countObjects(ShadowType.class, query, null, task, result);
                    } catch (Exception e) {
                        LOGGER.trace("Count of objects in object class {} on {} is not available: {}",
                                objectClassName, resourceObject, e.getMessage(), e);
                    }
                    if (count != null) {
                        LOGGER.trace("Approximate count of objects in object class {} on {}: {}",
                                objectClassName, resourceObject, count);
                        return new ObjectClassSizeEstimationType()
                                .value(count)
                                .precision(ObjectClassSizeEstimationPrecisionType.APPROXIMATELY);
                    }
                }
            }

            LOGGER.trace("Count is not available; trying to search for objects to estimate size");
            query.setPaging( // TODO will this work without sorting? We should test it thoroughly.
                    PrismContext.get().queryFactory().createPaging(0, maxSizeForEstimation));
            AtomicInteger counter = new AtomicInteger();
            ResultHandler<ShadowType> handler = (object, lResult) -> counter.incrementAndGet() < maxSizeForEstimation;
            var metadata = modelService.searchObjectsIterative(ShadowType.class, query, handler, null, task, result);

            int found = counter.get();
            if (found < maxSizeForEstimation) {
                LOGGER.trace("Found exactly {} object(s) of class {} on {}", found, objectClassName, resourceObject);
                return new ObjectClassSizeEstimationType()
                        .value(found)
                        .precision(ObjectClassSizeEstimationPrecisionType.EXACTLY);
            } else if (metadata != null
                    && metadata.getApproxNumberOfAllResults() != null
                    && metadata.getApproxNumberOfAllResults() > maxSizeForEstimation) {
                LOGGER.trace("Estimating approximately {} object(s) of class {} on {}", found, objectClassName, resourceObject);
                return new ObjectClassSizeEstimationType()
                        .value(metadata.getApproxNumberOfAllResults())
                        .precision(ObjectClassSizeEstimationPrecisionType.APPROXIMATELY);
            } else {
                LOGGER.trace("Found at least {} object(s) of class {} on {}", found, objectClassName, resourceObject);
                return new ObjectClassSizeEstimationType()
                        .value(found)
                        .precision(ObjectClassSizeEstimationPrecisionType.AT_LEAST);
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public GenericObjectType getLatestStatistics(String resourceOid, QName objectClassName, Task task, OperationResult parentResult)
            throws SchemaException {
        var result = parentResult.subresult(OP_GET_LATEST_STATISTICS)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();
        try {
            var objects = repositoryService.searchObjects(
                    GenericObjectType.class,
                    PrismContext.get().queryFor(GenericObjectType.class)
                            .item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_RESOURCE_OID)
                            .eq(resourceOid)
                            .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME)
                            .eq(objectClassName.getLocalPart())
                            .build(),
                    null,
                    result);
            return objects.stream()
                    .max(Comparator.comparing(
                            o -> toMillis(ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(o).getTimestamp())))
                    .map(o -> o.asObjectable())
                    .orElse(null);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

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
    public List<StatusInfo<ObjectTypesSuggestionType>> listSuggestObjectTypesOperationStatuses(
            String resourceOid, Task task, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_LIST_SUGGEST_OBJECT_TYPES_OPERATION_STATUSES)
                .addParam("resourceOid", resourceOid)
                .build();
        try {
            var tasks = taskManager.searchObjects(
                    TaskType.class,
                    PrismContext.get().queryFor(TaskType.class)
                            .item(TaskType.F_AFFECTED_OBJECTS, TaskAffectedObjectsType.F_ACTIVITY,
                                    ActivityAffectedObjectsType.F_RESOURCE_OBJECTS, ResourceObjectSetType.F_RESOURCE_REF)
                            .ref(resourceOid)
                            .and()
                            .item(TaskType.F_AFFECTED_OBJECTS, TaskAffectedObjectsType.F_ACTIVITY, ActivityAffectedObjectsType.F_ACTIVITY_TYPE)
                            .eq(SchemaConstantsGenerated.C_OBJECT_TYPES_SUGGESTION)
                            .build(),
                    taskRetrievalOptions(),
                    result);
            var resultingList = new ArrayList<StatusInfo<ObjectTypesSuggestionType>>();
            for (PrismObject<TaskType> t : tasks) {
                resultingList.add(
                        createStatusInformation(
                                taskManager.createTaskInstance(t, result),
                                result));
            }
            resultingList.sort(
                    Comparator
                            .comparing(
                                    (StatusInfo<ObjectTypesSuggestionType> info) -> XmlTypeConverter.toMillisNullable(info.finished()),
                                    Comparator.nullsFirst(Comparator.reverseOrder()))
                            .thenComparing(
                                    (StatusInfo<ObjectTypesSuggestionType> info) -> XmlTypeConverter.toMillisNullable(info.started()),
                                    Comparator.reverseOrder()));
            return resultingList;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private static @NotNull Collection<SelectorOptions<GetOperationOptions>> taskRetrievalOptions() {
        return GetOperationOptionsBuilder.create()
                .noFetch()
                .item(TaskType.F_RESULT).retrieve()
                .build();
    }

    @Override
    public StatusInfo<ObjectTypesSuggestionType> getSuggestObjectTypesOperationStatus(
            String token, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        var result = parentResult.subresult(OP_GET_SUGGEST_OBJECT_TYPES_OPERATION_STATUS)
                .addParam("token", token)
                .build();
        try {
            var bgTask = taskManager.getTaskPlain(token, taskRetrievalOptions(), result);
            return createStatusInformation(bgTask, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private StatusInfo<ObjectTypesSuggestionType> createStatusInformation(Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        LOGGER.trace("Task:\n{}", task.debugDump(1));
        var token = task.getOid();
        var taskStatus = Objects.requireNonNullElse(
                OperationResultStatus.parseStatusType(task.getResultStatus()),
                OperationResultStatus.IN_PROGRESS);
        var affectedResourceObjects = // FIXME implement more robustly
                task.getRawTaskObjectClonedIfNecessary().asObjectable().getAffectedObjects()
                        .getActivity().get(0).getResourceObjects();
        var taskResult = task.getResult();
        var message = taskResult != null ? taskResult.getMessage() : null;
        var localizableMessage = message != null ? LocalizableMessageBuilder.buildFallbackMessage(message) : null;
        var started = XmlTypeConverter.createXMLGregorianCalendar(task.getLastRunStartTimestamp());
        var finished = XmlTypeConverter.createXMLGregorianCalendar(task.getLastRunFinishTimestamp());
        if (taskStatus == OperationResultStatus.IN_PROGRESS) {
            var activity = activityManager.getActivity(task, ActivityPath.empty());
            return new StatusInfo<>(
                    token, taskStatus, localizableMessage, affectedResourceObjects, started, finished, null);
        }
        // We should have the state by now.
        var state = ActivityState.getActivityStateDownwards(
                ActivityPath.empty(),
                task,
                ObjectTypesSuggestionWorkStateType.COMPLEX_TYPE,
                CommonTaskBeans.get(),
                result);
        var suggestions = state.getWorkStateItemRealValueClone(
                ObjectTypesSuggestionWorkStateType.F_RESULT, ObjectTypesSuggestionType.class);
        return new StatusInfo<>(
                token, taskStatus, localizableMessage, affectedResourceObjects, started, finished, suggestions);
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
            var resource = modelService.getObject(ResourceType.class, resourceOid, null, task, result);
            var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
            var objectClassDef = resourceSchema.findObjectClassDefinitionRequired(objectClassName);
            try (var serviceClient = getServiceClient(result)) {
                return ServiceAdapter.create(serviceClient).suggestObjectTypes(objectClassDef, statistics, resourceSchema);
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
                var type = ServiceAdapter.create(serviceClient)
                        .suggestFocusType(typeIdentification, objectClassDef, objectTypeDef.getDelineation());
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
