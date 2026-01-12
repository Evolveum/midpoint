/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.toMillis;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowObjectTypeStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.synchronization.SourceSynchronizationAnswers;
import com.evolveum.midpoint.smart.api.synchronization.SynchronizationConfigurationScenario;
import com.evolveum.midpoint.smart.api.synchronization.TargetSynchronizationAnswers;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
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
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsSimulateType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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

    public SmartIntegrationServiceImpl(ModelService modelService,
            TaskService taskService, ModelInteractionServiceImpl modelInteractionService, TaskManager taskManager,
            @Qualifier("cacheRepositoryService") RepositoryService repositoryService,
            ServiceClientFactory clientFactory, MappingSuggestionOperationFactory mappingSuggestionOperationFactory,
            ObjectTypesSuggestionOperationFactory objectTypesSuggestionOperationFactory,
            StatisticsService statisticsService) {
        this.modelService = modelService;
        this.taskService = taskService;
        this.modelInteractionService = modelInteractionService;
        this.taskManager = taskManager;
        this.repositoryService = repositoryService;
        this.clientFactory = clientFactory;
        this.mappingSuggestionOperationFactory = mappingSuggestionOperationFactory;
        this.objectTypesSuggestionOperationFactory = objectTypesSuggestionOperationFactory;
        this.statisticsService = statisticsService;
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

    private QName getTypeName(@NotNull PrismPropertyDefinition<?> propertyDefinition) {
        if (propertyDefinition.isEnum()) {
            // We don't want to bother Python microservice with enums; maybe later.
            // It should work with the values as with simple strings.
            return DOMUtil.XSD_STRING;
        }
        var typeName = propertyDefinition.getTypeName();
        if (QNameUtil.match(PolyStringType.COMPLEX_TYPE, typeName)) {
            return DOMUtil.XSD_STRING; // We don't want to bother Python microservice with polystrings.
        } else if (QNameUtil.match(ProtectedStringType.COMPLEX_TYPE, typeName)) {
            return DOMUtil.XSD_STRING; // the same
        } else {
            return typeName;
        }
    }

    @Override
    public SchemaMatchResultType computeSchemaMatch(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
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
            var matchingOp = new SchemaMatchingOperation(ctx);
            var match = matchingOp.matchSchema(ctx.typeDefinition, focusTypeDefinition, ctx.resource);

            SchemaMatchResultType schemaMatchResult = new SchemaMatchResultType()
                    .timestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
            for (var attributeMatch : match.getAttributeMatch()) {
                var shadowAttrPath = matchingOp.getApplicationItemPath(attributeMatch.getApplicationAttribute());
                if (shadowAttrPath.size() != 2 || !shadowAttrPath.startsWith(ShadowType.F_ATTRIBUTES)) {
                    LOGGER.warn("Ignoring attribute {}. It is not a traditional attribute.", shadowAttrPath);
                    continue; // TODO implement support for activation etc
                }
                var shadowAttrName = shadowAttrPath.rest().asSingleNameOrFail();
                var shadowAttrDef = ctx.typeDefinition.findSimpleAttributeDefinition(shadowAttrName);
                if (shadowAttrDef == null) {
                    LOGGER.warn("No shadow attribute definition found for {}. Skipping schema match record.", shadowAttrName);
                    continue;
                }
                var focusPropPath = matchingOp.getFocusItemPath(attributeMatch.getMidPointAttribute());
                var focusPropDef = focusTypeDefinition.findPropertyDefinition(focusPropPath);
                if (focusPropDef == null) {
                    LOGGER.warn("No focus property definition found for {}. Skipping schema match record.", focusPropPath);
                    continue;
                }
                var applicationAttrDefBean = new SiAttributeDefinitionType()
                        .name(DescriptiveItemPath.of(shadowAttrPath, ctx.getShadowDefinition()).asString())
                        .type(getTypeName(shadowAttrDef))
                        .minOccurs(shadowAttrDef.getMinOccurs())
                        .maxOccurs(shadowAttrDef.getMaxOccurs());
                var midPointPropertyDefBean = new SiAttributeDefinitionType()
                        .name(DescriptiveItemPath.of(focusPropPath, focusTypeDefinition).asString())
                        .type(getTypeName(focusPropDef))
                        .minOccurs(focusPropDef.getMinOccurs())
                        .maxOccurs(focusPropDef.getMaxOccurs());

                schemaMatchResult.getSchemaMatchResult().add(new SchemaMatchOneResultType()
                        .shadowAttributePath(shadowAttrPath.toStringStandalone())
                        .shadowAttribute(applicationAttrDefBean)
                        .focusPropertyPath(focusPropPath.toStringStandalone())
                        .focusProperty(midPointPropertyDefBean));
            }
            return schemaMatchResult;
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
    public GenericObjectType getLatestStatistics(String resourceOid, QName objectClassName, OperationResult parentResult)
            throws SchemaException {
        return statisticsService.getLatestStatistics(resourceOid, objectClassName, parentResult);
    }

    @Override
    public GenericObjectType getLatestObjectTypeStatistics(String resourceOid, String kind, String intent, OperationResult parentResult)
            throws SchemaException {
        return statisticsService.getLatestObjectTypeStatistics(resourceOid, kind, intent, parentResult);
    }

    @Override
    public void deleteStatisticsForResource(String resourceOid, QName objectClassName, OperationResult result)
            throws SchemaException {
        statisticsService.deleteStatisticsForResource(resourceOid, objectClassName, result);
    }

    @Override
    public void deleteObjectTypeStatistics(String resourceOid, String kind, String intent, OperationResult result)
            throws SchemaException {
        statisticsService.deleteObjectTypeStatistics(resourceOid, kind, intent, result);
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
                    // consider only objects that actually contain schema match
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
            throws SchemaException {
        var result = parentResult.subresult(OP_LIST_SUGGEST_OBJECT_TYPES_OPERATION_STATUSES)
                .addParam("resourceOid", resourceOid)
                .build();
        try {
            var tasks = taskManager.searchObjects(
                    TaskType.class,
                    queryForActivityType(resourceOid, SchemaConstantsGenerated.C_OBJECT_TYPES_SUGGESTION),
                    taskRetrievalOptions(),
                    result);
            var resultingList = new ArrayList<StatusInfo<ObjectTypesSuggestionType>>();
            for (PrismObject<TaskType> t : tasks) {
                resultingList.add(
                        new StatusInfoImpl<>(
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
            Task task,
            OperationResult parentResult) throws CommonException {
        var result = parentResult.subresult(OP_SUBMIT_SUGGEST_FOCUS_TYPE_OPERATION)
                .addParam("resourceOid", resourceOid)
                .addParam("typeIdentification", typeIdentification)
                .build();
        try {
            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .focusTypeSuggestion(new FocusTypeSuggestionWorkDefinitionType()
                                            .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                                            .kind(typeIdentification.getKind())
                                            .intent(typeIdentification.getIntent()))),
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
                    serviceClient, resourceOid, objectClassName, task, result);
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
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        LOGGER.debug("Suggesting focus type for resourceOid {}, typeIdentification {}", resourceOid, typeIdentification);
        var result = parentResult.subresult(OP_SUGGEST_FOCUS_TYPE)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeIdentification", typeIdentification)
                .build();
        try {
            try (var serviceClient = this.clientFactory.getServiceClient(result)) {
                var suggestion = new FocusTypeSuggestionOperation(
                        TypeOperationContext.init(serviceClient, resourceOid, typeIdentification, null, task, result))
                        .suggestFocusType();
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
            String resourceOid, ResourceObjectTypeDefinitionType typeDefBean, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        LOGGER.debug("Suggesting focus type for resourceOid {}, typeDefinition {}", resourceOid, typeDefBean);
        var result = parentResult.subresult(OP_SUGGEST_FOCUS_TYPE)
                .addParam("resourceOid", resourceOid)
                .addArbitraryObjectAsParam("typeDefBean", typeDefBean) // todo reconsider (too much text)
                .build();
        try {
            try (var serviceClient = this.clientFactory.getServiceClient(result)) {
                var suggestion = new FocusTypeSuggestionOperation(
                        OperationContext.init(serviceClient, resourceOid, typeDefBean.getDelineation().getObjectClass(), task, result))
                        .suggestFocusType(typeDefBean);
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
            ShadowObjectClassStatisticsType statistics,
            SchemaMatchResultType schemaMatch,
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
                    .suggestCorrelation(result, statistics, schemaMatch);
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
            @Nullable MappingsSuggestionInteractionMetadataType interactionMetadata,
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
            var mappings = this.mappingSuggestionOperationFactory.create(serviceClient, resourceOid,
                    typeIdentification, activityState, isInbound, task, result)
                    .suggestMappings(result, schemaMatch);
            LOGGER.debug("Suggested mappings:\n{}", mappings.debugDumpLazily(1));
            return mappings;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public String submitSuggestCorrelationOperation(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult parentResult)
            throws CommonException {
        var result = parentResult.subresult(OP_SUBMIT_SUGGEST_CORRELATION_OPERATION)
                .addParam("resourceOid", resourceOid)
                .addParam("typeIdentification", typeIdentification)
                .build();
        try {
            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .correlationSuggestion(new CorrelationSuggestionWorkDefinitionType()
                                            .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                                            .objectType(typeIdentification.asBean()))),
                    ActivitySubmissionOptions.create().withTaskTemplate(new TaskType()
                            .name("Suggest correlation for " + typeIdentification + " on " + resourceOid)
                            .cleanupAfterCompletion(AUTO_CLEANUP_TIME)),
                    task, result);
            LOGGER.debug("Submitted suggest correlation operation for resourceOid {}, object type {}: {}",
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
    public List<StatusInfo<CorrelationSuggestionsType>> listSuggestCorrelationOperationStatuses(
            String resourceOid, Task task, OperationResult parentResult)
            throws SchemaException {
        var result = parentResult.subresult(OP_LIST_SUGGEST_CORRELATION_OPERATION_STATUSES)
                .addParam("resourceOid", resourceOid)
                .build();
        try {
            var tasks = taskManager.searchObjects(
                    TaskType.class,
                    queryForActivityType(resourceOid, SchemaConstantsGenerated.C_CORRELATION_SUGGESTION),
                    taskRetrievalOptions(),
                    result);
            var resultingList = new ArrayList<StatusInfo<CorrelationSuggestionsType>>();
            for (PrismObject<TaskType> t : tasks) {
                resultingList.add(
                        new StatusInfoImpl<>(
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
            Task task,
            OperationResult parentResult) throws CommonException {
        var result = parentResult.subresult(OP_SUBMIT_SUGGEST_MAPPINGS_OPERATION)
                .addParam("resourceOid", resourceOid)
                .addParam("typeIdentification", typeIdentification)
                .build();
        try {
            var oid = modelInteractionService.submit(
                    new ActivityDefinitionType()
                            .work(new WorkDefinitionsType()
                                    .mappingsSuggestion(new MappingsSuggestionWorkDefinitionType()
                                            .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                                            .objectType(typeIdentification.asBean())
                                            .inbound(isInbound))),
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
            Task task, OperationResult parentResult)
            throws SchemaException {
        var result = parentResult.subresult(OP_LIST_SUGGEST_MAPPINGS_OPERATION_STATUSES)
                .addParam("resourceOid", resourceOid)
                .build();
        try {
            var tasks = taskManager.searchObjects(
                    TaskType.class,
                    queryForActivityType(resourceOid, SchemaConstantsGenerated.C_MAPPINGS_SUGGESTION),
                    taskRetrievalOptions(),
                    result);
            var resultingList = new ArrayList<StatusInfo<MappingsSuggestionType>>();
            for (PrismObject<TaskType> t : tasks) {
                TaskType tt = t.asObjectable();

                ActivityDefinitionType activityDef = tt.getActivity();
                if (activityDef == null || activityDef.getWork() == null || activityDef.getWork().getMappingsSuggestion() == null) {
                    resultingList.add(new StatusInfoImpl<>(tt, MappingsSuggestionWorkStateType.F_RESULT, MappingsSuggestionType.class));
                    continue;
                }
                var workDef = activityDef.getWork().getMappingsSuggestion();
                if (objectTypeIdentification != null && workDef.getObjectType() != null) {
                    if (!Objects.equals(workDef.getObjectType().getKind(), objectTypeIdentification.getKind())
                            || !Objects.equals(workDef.getObjectType().getIntent(), objectTypeIdentification.getIntent())) {
                        continue;
                    }
                }
                if (isInbound != workDef.isInbound()) {
                    continue;
                }
                resultingList.add(new StatusInfoImpl<>(tt, MappingsSuggestionWorkStateType.F_RESULT, MappingsSuggestionType.class));
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

    @Override
    public AssociationsSuggestionType suggestAssociations(
            String resourceOid,
            boolean isInbound,
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

            return new SmartAssociationImpl().suggestSmartAssociation(resource.asObjectable(), isInbound);
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
