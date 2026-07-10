/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;

import com.evolveum.midpoint.schema.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.impl.controller.ModelInteractionServiceImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.InsufficientPermissionsException;
import com.evolveum.midpoint.smart.api.RegenerateMode;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.AiInfo;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.smart.api.synchronization.SourceSynchronizationAnswers;
import com.evolveum.midpoint.smart.api.synchronization.SynchronizationConfigurationScenario;
import com.evolveum.midpoint.smart.api.synchronization.TargetSynchronizationAnswers;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsSimulateType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAffectedObjectsType.F_RESOURCE_OBJECTS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAffectedObjectsType.F_ACTIVITY;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_AFFECTED_OBJECTS;

@Service("smartIntegrationService")
public class SmartIntegrationServiceImpl implements SmartIntegrationService {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationServiceImpl.class);

    private static final String OP_CREATE_NEW_RESOURCE = "createNewResource";
    private static final String OP_ESTIMATE_OBJECT_CLASS_SIZE = "estimateObjectClassSize";
    private static final String OP_GET_LATEST_OBJECT_TYPE_SCHEMA_MATCH = "getLatestObjectTypeSchemaMatch";
    private static final String OP_SUGGEST_OBJECT_TYPES = "suggestObjectTypes";
    private static final String OP_SUBMIT_SUGGEST_OBJECT_TYPES_OPERATION = "suggestObjectTypesOperation";
    private static final String OP_GET_SUGGEST_OBJECT_TYPES_OPERATION_STATUS = "getSuggestObjectTypesOperationStatus";
    private static final String OP_LIST_SUGGEST_OBJECT_TYPES_OPERATION_STATUSES = "listSuggestObjectTypesOperationStatuses";
    private static final String OP_SUBMIT_SUGGEST_FOCUS_TYPE_OPERATION = "submitSuggestFocusTypeOperation";
    private static final String OP_GET_SUGGEST_FOCUS_TYPE_OPERATION_STATUS = "getSuggestFocusTypeOperationStatus";
    private static final String OP_LIST_SUGGEST_FOCUS_TYPE_OPERATION_STATUSES = "listSuggestFocusTypeOperationStatuses";

    private static final String CLASS_DOT = SmartIntegrationService.class.getName() + ".";
    private static final String OP_SUGGEST_FOCUS_TYPE = CLASS_DOT + "suggestFocusType";
    private static final String OP_SUGGEST_MAPPINGS = CLASS_DOT + "suggestMappings";
    private static final String OP_SUBMIT_SUGGEST_MAPPINGS_OPERATION = "suggestMappingsOperation";
    private static final String OP_GET_SUGGEST_MAPPINGS_OPERATION_STATUS = "getSuggestMappingsOperationStatus";
    private static final String OP_LIST_SUGGEST_MAPPINGS_OPERATION_STATUSES = "listSuggestMappingsOperationStatuses";
    private static final String OP_SUGGEST_CORRELATION = CLASS_DOT + "suggestCorrelation";
    private static final String OP_SUBMIT_SUGGEST_CORRELATION_OPERATION = "suggestCorrelationOperation";
    private static final String OP_GET_SUGGEST_CORRELATION_OPERATION_STATUS = "getSuggestCorrelationOperationStatus";
    private static final String OP_LIST_SUGGEST_CORRELATION_OPERATION_STATUSES = "listSuggestCorrelationOperationStatuses";
    private static final String OP_SUBMIT_SCHEMA_MATCH_PRELOAD = "submitSchemaMatchPreload";
    private static final String OP_SUGGEST_ASSOCIATIONS = CLASS_DOT + "suggestAssociations";
    private static final String OP_SUBMIT_SUGGEST_ASSOCIATIONS_OPERATION = "submitSuggestAssociationsOperation";
    private static final String OP_GET_SUGGEST_ASSOCIATIONS_OPERATION_STATUS = "getSuggestAssociationsOperationStatus";
    private static final String OP_LIST_SUGGEST_ASSOCIATIONS_OPERATION_STATUSES = "listSuggestAssociationsOperationStatuses";

    /** Auto cleanup time for background tasks created by the service. Will be shorter, probably. */
    private static final Duration AUTO_CLEANUP_TIME = XmlTypeConverter.createDuration("P1D");

    private final ModelService modelService;
    private final TaskService taskService;
    private final ModelInteractionServiceImpl modelInteractionService;
    private final TaskManager taskManager;
    private final RepositoryService repositoryService;
    private final ServiceClientFactory clientFactory;
    private final MappingSuggestionOperationFactory mappingSuggestionOperationFactory;
    private final ObjectTypesSuggestionOperationFactory objectTypesSuggestionOperationFactory;
    private final StatisticsService statisticsService;
    private final SchemaMatchService schemaMatchService;
    private final SystemObjectCache systemObjectCache;

    public SmartIntegrationServiceImpl(ModelService modelService,
            TaskService taskService, ModelInteractionServiceImpl modelInteractionService, TaskManager taskManager,
            @Qualifier("cacheRepositoryService") RepositoryService repositoryService,
            ServiceClientFactory clientFactory, MappingSuggestionOperationFactory mappingSuggestionOperationFactory,
            ObjectTypesSuggestionOperationFactory objectTypesSuggestionOperationFactory,
            StatisticsService statisticsService, SchemaMatchService schemaMatchService,
            SystemObjectCache systemObjectCache) {
        this.modelService = modelService;
        this.taskService = taskService;
        this.modelInteractionService = modelInteractionService;
        this.taskManager = taskManager;
        this.repositoryService = repositoryService;
        this.clientFactory = clientFactory;
        this.mappingSuggestionOperationFactory = mappingSuggestionOperationFactory;
        this.objectTypesSuggestionOperationFactory = objectTypesSuggestionOperationFactory;
        this.statisticsService = statisticsService;
        this.schemaMatchService = schemaMatchService;
        this.systemObjectCache = systemObjectCache;
    }

    @Override
    public Optional<AiInfo> getAiInfo() {
        try (var client = clientFactory.getServiceClient(new OperationResult("getAiInfo"))) {
            return client.getAiInfo();
        } catch (Exception e) {
            throw new SystemException("Failed to retrieve AI info: " + e.getMessage(), e);
        }
    }

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
    public SynchronizationReactionsType getPredefinedSynchronizationReactions(
            SynchronizationConfigurationScenario scenario, boolean includeCorrelationCaseAction) {
        return SynchronizationConfigurationScenarioHandler.getPredefinedSynchronizationReactions(
                scenario, includeCorrelationCaseAction);
    }

    @Override
    public SynchronizationReactionsType buildSourceSynchronizationReactionsFromAnswers(
            SourceSynchronizationAnswers answers) {
        return SynchronizationConfigurationScenarioHandler.getSynchronizationReactionsFromSource(answers);
    }

    @Override
    public SynchronizationReactionsType buildTargetSynchronizationReactionsFromAnswers(
            TargetSynchronizationAnswers answers) {
        return SynchronizationConfigurationScenarioHandler.getSynchronizationReactionsFromTarget(answers);
    }

    @Override
    public SchemaMatchResultType computeSchemaMatch(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            boolean useAiService,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return schemaMatchService.computeSchemaMatch(resourceOid, typeIdentification, useAiService, task, parentResult);
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
    public GenericObjectType getLatestObjectTypeStatistics(String resourceOid, String kind, String intent, OperationResult parentResult)
            throws SchemaException {
        return statisticsService.getLatestObjectTypeStatistics(resourceOid, kind, intent, parentResult);
    }

    @Override
    public void deleteObjectTypeStatistics(String resourceOid, String kind, String intent, OperationResult result)
            throws SchemaException {
        statisticsService.deleteObjectTypeStatistics(resourceOid, kind, intent, result);
    }

    @Override
    public GenericObjectType getLatestObjectClassStatistics(String resourceOid, QName objectClassName, OperationResult parentResult)
            throws SchemaException {
        return statisticsService.getLatestObjectClassStatistics(resourceOid, objectClassName, parentResult);
    }

    @Override
    public String regenerateObjectClassStatistics(
            String resourceOid,
            QName objectClassName,
            Task task,
            OperationResult parentResult)
            throws CommonException {
        return statisticsService.regenerateObjectClassStatistics(resourceOid, objectClassName, task, parentResult);
    }

    @Override
    public String regenerateObjectTypeStatistics(
            String resourceOid,
            ResourceObjectTypeIdentification resourceObjectTypeIdentification,
            Task task,
            OperationResult result) throws CommonException {
        return statisticsService.regenerateObjectTypeStatistics(resourceOid, resourceObjectTypeIdentification, task, result);
    }

    @Override
    public void deleteStatisticsForResource(String resourceOid, QName objectClassName, OperationResult result)
            throws SchemaException {
        statisticsService.deleteStatisticsForResource(resourceOid, objectClassName, result);
    }

    @Override
    public GenericObjectType getLatestFocusObjectStatistics(
            QName objectTypeName,
            String resourceOid,
            ShadowKindType kind,
            String intent,
            OperationResult parentResult)
            throws SchemaException {
        return statisticsService.getLatestFocusObjectStatistics(objectTypeName, resourceOid, kind, intent, parentResult);
    }

    @Override
    public void deleteFocusObjectStatistics(
            QName objectTypeName,
            String resourceOid,
            ShadowKindType kind,
            String intent,
            OperationResult result)
            throws SchemaException {
        statisticsService.deleteFocusObjectStatistics(objectTypeName, resourceOid, kind, intent, result);
    }

    @Override
    public String regenerateFocusObjectStatistics(
            QName objectTypeName,
            String resourceOid,
            ShadowKindType kind,
            String intent,
            Task task,
            OperationResult result)
            throws CommonException {
        return statisticsService.regenerateFocusObjectStatistics(objectTypeName, resourceOid, kind, intent, task, result);
    }

    @Override
    public GenericObjectType getLatestObjectTypeSchemaMatch(String resourceOid, String kind, String intent, OperationResult parentResult)
            throws SchemaException {
        return schemaMatchService.getLatestObjectTypeSchemaMatch(resourceOid, kind, intent, parentResult);
    }

    @Override
    public String submitSuggestObjectTypesOperation(
            String resourceOid,
            QName objectClassName,
            List<DataAccessPermissionType> permissions,
            @Nullable RegenerateMode regenerateMode,
            @Nullable List<ResourceObjectTypeDefinitionType> previousObjectTypes,
            Task task,
            OperationResult parentResult)
            throws CommonException {

        var result = parentResult.subresult(OP_SUBMIT_SUGGEST_OBJECT_TYPES_OPERATION)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();

        try {
            var workDef = new ObjectTypesSuggestionWorkDefinitionType()
                    .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                    .objectclass(objectClassName);

            workDef.getPermissions().addAll(permissions);

            if (regenerateMode != null) {
                workDef.setRegenerateMode(regenerateMode.name());
            }

            if (previousObjectTypes != null && !previousObjectTypes.isEmpty()) {
                for (var objectType : previousObjectTypes) {
                    workDef.getPreviousDelineation().add(
                            (ResourceObjectTypeDefinitionType) objectType.asPrismContainerValue()
                                    .clone()
                                    .asContainerable());
                }
            }

            ActivityDefinitionType activity = new ActivityDefinitionType()
                    .work(new WorkDefinitionsType()
                            .objectTypesSuggestion(workDef));

            var oid = modelInteractionService.submit(
                    activity,
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name("Suggest object types for " + objectClassName.getLocalPart() + " on " + resourceOid)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task,
                    result);

            LOGGER.debug("Submitted suggest object types operation for resourceOid {}, objectClassName {}, "
                            + "permissions {}, regenerateMode {}: {}",
                    resourceOid, objectClassName, permissions, regenerateMode, oid);

            return oid;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    public void submitSchemaMatchPreload(
            String resourceOid, QName objectClassName, List<DataAccessPermissionType> permissions,
            Task task, OperationResult parentResult)
            throws CommonException {
        var result = parentResult.subresult(OP_SUBMIT_SCHEMA_MATCH_PRELOAD)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();
        try {
            var workDef = new SchemaMatchPreloadWorkDefinitionType()
                    .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                    .objectclass(objectClassName)
                    .sourceTaskRef(task.getOid(), TaskType.COMPLEX_TYPE);
            workDef.getPermissions().addAll(permissions);
            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .schemaMatchPreload(workDef)),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name("Schema match preload for " + resourceOid)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task, result);
            LOGGER.debug("Submitted schema match preload for resourceOid {}, objectClassName {}: {}",
                    resourceOid, objectClassName, oid);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public List<StatusInfo<ObjectTypesSuggestionType>> listSuggestObjectTypesOperationStatuses(
            String resourceOid,
            @Nullable ResourceObjectTypeIdentification objectTypeIdentification,
            @Nullable QName objectClass,
            Task task, OperationResult parentResult)
            throws SchemaException {

        var result = parentResult.subresult(OP_LIST_SUGGEST_OBJECT_TYPES_OPERATION_STATUSES)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClass", objectClass)
                .addParam("kind", objectTypeIdentification != null ? objectTypeIdentification.getKind().value() : null)
                .addParam("intent", objectTypeIdentification != null ? objectTypeIdentification.getIntent() : null)
                .build();

        try {
            var tasks = listObjectTypeRelatedSuggestionTasks(
                    objectTypeIdentification,
                    resourceOid,
                    objectClass,
                    List.of(SchemaConstantsGenerated.C_OBJECT_TYPES_SUGGESTION),
                    result);

            var resultingList = new ArrayList<StatusInfo<ObjectTypesSuggestionType>>();
            for (PrismObject<TaskType> t : tasks) {
                resultingList.add(new StatusInfoImpl<>(
                        t.asObjectable(),
                        ObjectTypesSuggestionWorkStateType.F_RESULT,
                        ObjectTypesSuggestionType.class));
            }

            sortByFinishAndStartTime(resultingList);
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
                .item(TaskType.F_ACTIVITY_STATE).retrieve()
                .build();
    }

    @Override
    public StatusInfo<ObjectTypesSuggestionType> getSuggestObjectTypesOperationStatus(
            String token, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_GET_SUGGEST_OBJECT_TYPES_OPERATION_STATUS)
                .addParam("token", token)
                .build();
        try {
            return new StatusInfoImpl<>(
                    getTask(token, result),
                    ObjectTypesSuggestionWorkStateType.F_RESULT,
                    ObjectTypesSuggestionType.class);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private @NotNull TaskType getTask(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        return taskManager
                .getObject(TaskType.class, oid, taskRetrievalOptions(), result)
                .asObjectable();
    }

    @Override
    public String submitSuggestFocusTypeOperation(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            List<DataAccessPermissionType> permissions,
            Task task,
            OperationResult parentResult) throws CommonException {
        var result = parentResult.subresult(OP_SUBMIT_SUGGEST_FOCUS_TYPE_OPERATION)
                .addParam("resourceOid", resourceOid)
                .addParam("typeIdentification", typeIdentification)
                .build();
        try {
            var workDef = new FocusTypeSuggestionWorkDefinitionType()
                    .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                    .kind(typeIdentification.getKind())
                    .intent(typeIdentification.getIntent());
            workDef.getPermissions().addAll(permissions);
            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .focusTypeSuggestion(workDef)),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name("Suggest focus type for " + typeIdentification + " on " + resourceOid)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task, result);
            LOGGER.debug("Submitted suggest focus type operation for resourceOid {}, typeIdentification {}: {}",
                    resourceOid, typeIdentification, oid);
            return oid;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public List<StatusInfo<FocusTypeSuggestionType>> listSuggestFocusTypeOperationStatuses(
            String resourceOid, Task task, OperationResult parentResult)
            throws SchemaException {
        var result = parentResult.subresult(OP_LIST_SUGGEST_FOCUS_TYPE_OPERATION_STATUSES)
                .addParam("resourceOid", resourceOid)
                .build();
        try {
            var tasks = taskManager.searchObjects(
                    TaskType.class,
                    queryForActivityType(resourceOid, WorkDefinitionsType.F_FOCUS_TYPE_SUGGESTION),
                    taskRetrievalOptions(),
                    result);
            var resultingList = new ArrayList<StatusInfo<FocusTypeSuggestionType>>();
            for (PrismObject<TaskType> t : tasks) {
                resultingList.add(
                        new StatusInfoImpl<>(
                                t.asObjectable(),
                                FocusTypeSuggestionWorkStateType.F_RESULT,
                                FocusTypeSuggestionType.class));
            }
            sortByFinishAndStartTime(resultingList);
            return resultingList;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public StatusInfo<FocusTypeSuggestionType> getSuggestFocusTypeOperationStatus(
            String token, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_GET_SUGGEST_FOCUS_TYPE_OPERATION_STATUS)
                .addParam("token", token)
                .build();
        try {
            return new StatusInfoImpl<>(
                    getTask(token, result),
                    FocusTypeSuggestionWorkStateType.F_RESULT,
                    FocusTypeSuggestionType.class);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public ObjectTypesSuggestionType suggestObjectTypes(
            String resourceOid,
            QName objectClassName,
            ShadowObjectClassStatisticsType statistics,
            @Nullable RegenerateMode regenerateMode,
            @Nullable List<ResourceObjectTypeDefinitionType> previousObjectTypes,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        LOGGER.debug("Suggesting object types for resourceOid {}, objectClassName {}", resourceOid, objectClassName);
        var result = parentResult.subresult(OP_SUGGEST_OBJECT_TYPES)
                .addParam("resourceOid", resourceOid)
                .addParam("objectClassName", objectClassName)
                .build();
        try (var serviceClient = this.clientFactory.getServiceClient(result)) {
            var op = this.objectTypesSuggestionOperationFactory.create(
                    serviceClient, resourceOid, objectClassName, regenerateMode, previousObjectTypes, task, result);
            var types = op.suggestObjectTypes(statistics, result);
            LOGGER.debug("Object types suggestion:\n{}", types.debugDump(1));
            return types;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public FocusTypeSuggestionType suggestFocusType(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification,
            List<DataAccessPermissionType> permissions, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, InsufficientPermissionsException {
        LOGGER.debug("Suggesting focus type for resourceOid {}, typeIdentification {}", resourceOid, typeIdentification);
        var result = parentResult.subresult(OP_SUGGEST_FOCUS_TYPE)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeIdentification", typeIdentification)
                .build();
        try {
            try (var serviceClient = this.clientFactory.getServiceClient(result)) {
                var suggestion = new FocusTypeSuggestionOperation(
                        TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, null, task, result))
                        .suggestFocusType(permissions);
                LOGGER.debug("Suggested focus type: {}", suggestion.getFocusType());
                return suggestion;
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public FocusTypeSuggestionType suggestFocusType(
            String resourceOid, ResourceObjectTypeDefinitionType typeDefBean,
            List<DataAccessPermissionType> permissions, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, InsufficientPermissionsException {
        LOGGER.debug("Suggesting focus type for resourceOid {}, typeDefinition {}", resourceOid, typeDefBean);
        var result = parentResult.subresult(OP_SUGGEST_FOCUS_TYPE)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeDefBean", typeDefBean) // todo reconsider (too much text)
                .build();
        try {
            try (var serviceClient = this.clientFactory.getServiceClient(result)) {
                var suggestion = new FocusTypeSuggestionOperation(
                        OperationContext.init(serviceClient, resourceOid, typeDefBean.getDelineation().getObjectClass(), task, result))
                        .suggestFocusType(typeDefBean, permissions);
                LOGGER.debug("Suggested focus type: {}", suggestion.getFocusType());
                return suggestion;
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public CorrelationSuggestionsType suggestCorrelation(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            SchemaMatchResultType schemaMatch,
            @Nullable List<ItemPath> targetPathsToIgnore,
            @Nullable Object interactionMetadata,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        LOGGER.debug("Suggesting correlation for resourceOid {}, typeIdentification {}", resourceOid, typeIdentification);
        var result = parentResult.subresult(OP_SUGGEST_CORRELATION)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeIdentification", typeIdentification)
                .build();
        try (var serviceClient = this.clientFactory.getServiceClient(result)) {
            var correlation = new CorrelationSuggestionOperation(
                    TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, null, task, result))
                    .suggestCorrelation(result, schemaMatch, targetPathsToIgnore);
            LOGGER.debug("Suggested correlation:\n{}", correlation.debugDump(1));
            return correlation;
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
            SchemaMatchResultType schemaMatch,
            Boolean isInbound,
            Boolean useAiService,
            @Nullable ShadowObjectClassStatisticsType objectTypeStatistics,
            @Nullable List<ItemPath> targetPathsToIgnore,
            @Nullable CurrentActivityState<?> activityState,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException, ActivityInterruptedException {
        LOGGER.debug("Suggesting mappings for resourceOid {}, typeIdentification {}", resourceOid, typeIdentification);
        var result = parentResult.subresult(OP_SUGGEST_MAPPINGS)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeIdentification", typeIdentification)
                .build();
        try (var serviceClient = this.clientFactory.getServiceClient(result)) {
            int retryCount = getConfiguredRetryCount(result);
            var mappings = this.mappingSuggestionOperationFactory.create(serviceClient, resourceOid,
                            typeIdentification, activityState, isInbound, useAiService, objectTypeStatistics, retryCount, task, result)
                    .suggestMappings(result, schemaMatch, targetPathsToIgnore);
            LOGGER.debug("Suggested mappings:\n{}", mappings.debugDumpLazily(1));
            return mappings;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Retrieves the configured retry count for AI mapping suggestions from system configuration.
     * Falls back to default of 0 (no retry) if not configured.
     */
    private int getConfiguredRetryCount(OperationResult result) {
        try {
            var configuredRetryCount = Optional.ofNullable(systemObjectCache.getSystemConfigurationBean(result))
                    .map(SystemConfigurationType::getSmartIntegration)
                    .map(SmartIntegrationConfigurationType::getMappingSuggestionRetryCount)
                    .filter(count -> count >= 0);
            configuredRetryCount.ifPresent(
                    count -> LOGGER.debug("Using configured retry count for mapping suggestions: {}", count));
            return configuredRetryCount.orElse(0);
        } catch (SchemaException e) {
            LOGGER.warn("Failed to retrieve configured retry count, using default", e);
        }
        return 0;
    }

    @Override
    public String submitSuggestCorrelationOperation(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification,
            List<DataAccessPermissionType> permissions,
            boolean forceRecomputeSchemaMatch,
            Task task, OperationResult parentResult)
            throws CommonException {
        var result = parentResult.subresult(OP_SUBMIT_SUGGEST_CORRELATION_OPERATION)
                .addParam("resourceOid", resourceOid)
                .addParam("typeIdentification", typeIdentification)
                .build();
        try {
            var workDef = new CorrelationSuggestionWorkDefinitionType()
                    .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                    .objectType(typeIdentification.asBean());
            workDef.getPermissions().addAll(permissions);
            if (forceRecomputeSchemaMatch) {
                workDef.setForceRecomputeSchemaMatch(true);
            }
            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .correlationSuggestion(workDef)),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name("Suggest correlation for " + typeIdentification + " on " + resourceOid)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task, result);
            LOGGER.debug("Submitted suggest correlation operation for resourceOid {}, object type {}, permissions {}: {}",
                    resourceOid, typeIdentification, permissions, oid);
            return oid;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public List<StatusInfo<CorrelationSuggestionsType>> listSuggestCorrelationOperationStatuses(
            String resourceOid,
            @Nullable ResourceObjectTypeIdentification objectTypeIdentification,
            Task task,
            OperationResult parentResult)
            throws SchemaException {

        var result = parentResult.subresult(OP_LIST_SUGGEST_CORRELATION_OPERATION_STATUSES)
                .addParam("resourceOid", resourceOid)
                .addParam("kind", objectTypeIdentification != null
                        ? objectTypeIdentification.getKind().value()
                        : null)
                .addParam("intent", objectTypeIdentification != null ? objectTypeIdentification.getIntent() : null)
                .build();

        try {
            var tasks = listObjectTypeRelatedSuggestionTasks(
                    objectTypeIdentification,
                    resourceOid,
                    null,
                    List.of(SchemaConstantsGenerated.C_CORRELATION_SUGGESTION),
                    result);

            var resultingList = new ArrayList<StatusInfo<CorrelationSuggestionsType>>();
            for (PrismObject<TaskType> t : tasks) {
                resultingList.add(new StatusInfoImpl<>(
                        t.asObjectable(),
                        CorrelationSuggestionWorkStateType.F_RESULT,
                        CorrelationSuggestionsType.class));
            }

            sortByFinishAndStartTime(resultingList);
            return resultingList;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public StatusInfo<CorrelationSuggestionsType> getSuggestCorrelationOperationStatus(
            String token, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_GET_SUGGEST_CORRELATION_OPERATION_STATUS)
                .addParam("token", token)
                .build();
        try {
            return new StatusInfoImpl<>(
                    getTask(token, result),
                    CorrelationSuggestionWorkStateType.F_RESULT,
                    CorrelationSuggestionsType.class);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public String submitSuggestMappingsOperation(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            Boolean isInbound,
            List<ItemPathType> targetPathsToIgnore,
            List<DataAccessPermissionType> permissions,
            boolean forceRecomputeSchemaMatch,
            Task task,
            OperationResult parentResult) throws CommonException {
        var result = parentResult.subresult(OP_SUBMIT_SUGGEST_MAPPINGS_OPERATION)
                .addParam("resourceOid", resourceOid)
                .addParam("typeIdentification", typeIdentification)
                .build();
        try {

            MappingsSuggestionWorkDefinitionType mappingsSuggestionWorkDefinition = new MappingsSuggestionWorkDefinitionType()
                    .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                    .objectType(typeIdentification.asBean())
                    .inbound(isInbound);
            permissions.forEach(mappingsSuggestionWorkDefinition::permissions);
            if (forceRecomputeSchemaMatch) {
                mappingsSuggestionWorkDefinition.setForceRecomputeSchemaMatch(true);
            }

            if (targetPathsToIgnore != null) {
                mappingsSuggestionWorkDefinition.getTargetPathsToIgnore().addAll(targetPathsToIgnore);
            }

            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .mappingsSuggestion(mappingsSuggestionWorkDefinition)),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name("Suggest mappings for " + typeIdentification + " on " + resourceOid)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task, result);
            LOGGER.debug("Submitted suggest mappings operation for resourceOid {}, object type {}: {}",
                    resourceOid, typeIdentification, oid);
            return oid;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public List<StatusInfo<MappingsSuggestionType>> listSuggestMappingsOperationStatuses(
            String resourceOid,
            ResourceObjectTypeIdentification objectTypeIdentification,
            Boolean isInbound,
            Task task,
            OperationResult parentResult)
            throws SchemaException {

        var result = parentResult.subresult(OP_LIST_SUGGEST_MAPPINGS_OPERATION_STATUSES)
                .addParam("resourceOid", resourceOid)
                .addParam("kind", objectTypeIdentification != null
                        ? objectTypeIdentification.getKind().value()
                        : null)
                .addParam("intent", objectTypeIdentification != null ? objectTypeIdentification.getIntent() : null)
                .addParam("isInbound", isInbound)
                .build();

        try {
            var tasks = listObjectTypeRelatedSuggestionTasks(
                    objectTypeIdentification,
                    resourceOid,
                    null,
                    List.of(SchemaConstantsGenerated.C_MAPPINGS_SUGGESTION),
                    result);

            var resultingList = new ArrayList<StatusInfo<MappingsSuggestionType>>();
            for (PrismObject<TaskType> t : tasks) {
                TaskType tt = t.asObjectable();

                ActivityDefinitionType activityDef = tt.getActivity();
                if (activityDef == null
                        || activityDef.getWork() == null
                        || activityDef.getWork().getMappingsSuggestion() == null) {
                    resultingList.add(new StatusInfoImpl<>(
                            tt,
                            MappingsSuggestionWorkStateType.F_RESULT,
                            MappingsSuggestionType.class));
                    continue;
                }

                var workDef = activityDef.getWork().getMappingsSuggestion();
                if (isInbound != workDef.isInbound()) {
                    continue;
                }

                resultingList.add(new StatusInfoImpl<>(
                        tt,
                        MappingsSuggestionWorkStateType.F_RESULT,
                        MappingsSuggestionType.class));
            }

            sortByFinishAndStartTime(resultingList);
            return resultingList;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public StatusInfo<MappingsSuggestionType> getSuggestMappingsOperationStatus(
            String token, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_GET_SUGGEST_MAPPINGS_OPERATION_STATUS)
                .addParam("token", token)
                .build();
        try {
            return new StatusInfoImpl<>(
                    getTask(token, result),
                    MappingsSuggestionWorkStateType.F_RESULT,
                    MappingsSuggestionType.class);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private static void sortByFinishAndStartTime(List<? extends StatusInfo<?>> resultingList) {
        resultingList.sort(
                Comparator
                        .comparing(
                                (StatusInfo<?> info) -> XmlTypeConverter.toMillisNullable(info.getRealizationEndTimestamp()),
                                Comparator.nullsFirst(Comparator.reverseOrder()))
                        .thenComparing(
                                (StatusInfo<?> info) -> XmlTypeConverter.toMillisNullable(info.getRealizationStartTimestamp()),
                                Comparator.nullsFirst(Comparator.reverseOrder())));
    }

    private static ObjectQuery queryForActivityType(String resourceOid, ItemName activityType) {
        return PrismContext.get().queryFor(TaskType.class)
                .item(TaskType.F_AFFECTED_OBJECTS, TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_RESOURCE_OBJECTS, ResourceObjectSetType.F_RESOURCE_REF)
                .ref(resourceOid)
                .and()
                .item(TaskType.F_AFFECTED_OBJECTS, TaskAffectedObjectsType.F_ACTIVITY, ActivityAffectedObjectsType.F_ACTIVITY_TYPE)
                .eq(activityType)
                .build();
    }

    public @NotNull SearchResultList<PrismObject<TaskType>> listObjectTypeRelatedSuggestionTasks(
            @Nullable ResourceObjectTypeIdentification objectTypeIdentification,
            @NotNull String resourceOid,
            @Nullable QName objectClass,
            @NotNull List<ItemName> activityTypes,
            @NotNull OperationResult result)
            throws SchemaException {
        ObjectQuery query = createQueryForObjectTypeSuggestionTasks(
                objectTypeIdentification, resourceOid, objectClass, activityTypes);

        return taskManager.searchObjects(TaskType.class, query, taskRetrievalOptions(), result);
    }

    public static @NotNull ObjectQuery createQueryForObjectTypeSuggestionTasks(
            @Nullable ResourceObjectTypeIdentification typeIdentification,
            @NotNull String resourceOid,
            @Nullable QName objectClass,
            @NotNull List<ItemName> activityTypes) {

        var query = PrismContext.get()
                .queryFor(TaskType.class)
                .item(createResourceObjectPath(BasicResourceObjectSetType.F_RESOURCE_REF))
                .ref(resourceOid);

        if (typeIdentification != null) {
            query = query.and()
                    .item(createResourceObjectPath(BasicResourceObjectSetType.F_KIND))
                    .eq(typeIdentification.getKind());

            query = query.and()
                    .item(createResourceObjectPath(BasicResourceObjectSetType.F_INTENT))
                    .eq(typeIdentification.getIntent());
        }

        if (objectClass != null) {
            query = query.and()
                    .item(createResourceObjectPath(BasicResourceObjectSetType.F_OBJECTCLASS))
                    .eq(objectClass);
        }

        if (!activityTypes.isEmpty()) {
            S_FilterEntry block = query.and().block();

            boolean first = true;
            S_FilterExit filter = query;

            for (ItemName activityType : activityTypes) {
                filter = first
                        ? addActivityTypeRule(block, activityType)
                        : addActivityTypeRule(filter.or(), activityType);
                first = false;
            }

            return filter.endBlock().build();
        }

        return query.build();
    }

    public static @NotNull ItemPath createResourceObjectPath(ItemName subPath) {
        return ItemPath.create(F_AFFECTED_OBJECTS, F_ACTIVITY, F_RESOURCE_OBJECTS, subPath);
    }

    protected static @NotNull S_FilterExit addActivityTypeRule(@NotNull S_FilterEntry filter, @NotNull ItemName activityType) {
        return filter.item(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_ACTIVITY_TYPE)
                .eq(activityType);
    }

    @Override
    public AssociationsSuggestionType suggestAssociations(
            String resourceOid,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var result = parentResult.subresult(OP_SUGGEST_ASSOCIATIONS)
                .addParam("resourceOid", resourceOid)
                .build();
        try {
            var resource = modelService.getObject(ResourceType.class, resourceOid, null, task, result);

            LOGGER.trace("Suggesting associations for resourceOid {}", resourceOid);

            return new SmartAssociationImpl().suggestSmartAssociation(resource.asObjectable());
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public String submitSuggestAssociationsOperation(
            String resourceOid,
            Task task,
            OperationResult parentResult) throws CommonException {

        var result = parentResult.subresult(OP_SUBMIT_SUGGEST_ASSOCIATIONS_OPERATION)
                .addParam("resourceOid", resourceOid)
                .build();

        try {
            var workDef = new AssociationSuggestionWorkDefinitionType()
                    .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE);

            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .associationsSuggestion(workDef)),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name("Suggest associations on " + resourceOid)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task, result);

            LOGGER.debug("Submitted suggest associations operation for resourceOid: {}, odi: {}", resourceOid, oid);
            return oid;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public List<StatusInfo<AssociationsSuggestionType>> listSuggestAssociationsOperationStatuses(
            String resourceOid, Task task, OperationResult parentResult)
            throws SchemaException {

        var result = parentResult.subresult(OP_LIST_SUGGEST_ASSOCIATIONS_OPERATION_STATUSES)
                .addParam("resourceOid", resourceOid)
                .build();
        try {
            var tasks = taskManager.searchObjects(
                    TaskType.class,
                    queryForActivityType(resourceOid, SchemaConstantsGenerated.C_ASSOCIATIONS_SUGGESTION),
                    taskRetrievalOptions(),
                    result);

            var resultingList = new ArrayList<StatusInfo<AssociationsSuggestionType>>();
            for (PrismObject<TaskType> t : tasks) {
                resultingList.add(
                        new StatusInfoImpl<>(
                                t.asObjectable(),
                                AssociationSuggestionWorkStateType.F_RESULT,
                                AssociationsSuggestionType.class));
            }

            sortByFinishAndStartTime(resultingList);
            return resultingList;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public StatusInfo<AssociationsSuggestionType> getSuggestAssociationsOperationStatus(
            String token, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {

        var result = parentResult.subresult(OP_GET_SUGGEST_ASSOCIATIONS_OPERATION_STATUS)
                .addParam("token", token)
                .build();
        try {
            return new StatusInfoImpl<>(
                    getTask(token, result),
                    AssociationSuggestionWorkStateType.F_RESULT,
                    AssociationsSuggestionType.class);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public boolean cancelRequest(String token, long timeToWait, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException,
            SecurityViolationException, CommunicationException {
        return taskService.suspendTask(token, timeToWait, task, result);
    }
}
