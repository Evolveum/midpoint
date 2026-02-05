/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.smart.api.synchronization.SourceSynchronizationAnswers;
import com.evolveum.midpoint.smart.api.synchronization.SynchronizationConfigurationScenario;
import com.evolveum.midpoint.smart.api.synchronization.TargetSynchronizationAnswers;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Provides methods for suggesting parts of the integration solution, like inbound/outbound mappings.
 */
public interface SmartIntegrationService {

    /**
     * Creates a new resource with the given connector and the given connector configuration.
     *
     * @return OID of the created resource (if the resource was created successfully)
     */
    @Nullable String createNewResource(
            PolyStringType name,
            ObjectReferenceType connectorRef,
            ConnectorConfigurationType connectorConfiguration,
            Task task,
            OperationResult result);

    /**
     * Estimates the number of objects of the given class on the given resource.
     *
     * NOTE: Maybe this method could reside right in `ModelService`.
     *
     * @param resourceOid OID of the resource to estimate the size for
     * @param objectClassName Name of the object class to estimate the size for
     * @param maxSizeForEstimation When trying to estimate the size, the implementation may use a sample of objects - at most
     * this many objects.
     */
    ObjectClassSizeEstimationType estimateObjectClassSize(
            String resourceOid, QName objectClassName, int maxSizeForEstimation, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /** Returns the object holding last known statistics for the given resource and object class. */
    GenericObjectType getLatestStatistics(
            String resourceOid, QName objectClassName, OperationResult result)
            throws SchemaException;


    /** Deletes all statistics objects for the given resource and object class. */
    void deleteStatisticsForResource(
            String resourceOid, QName objectClassName, OperationResult result)
            throws SchemaException;

    /** Returns the object holding last known schema match for the given resource, kind and intent. */
    public GenericObjectType getLatestObjectTypeSchemaMatch(
            String resourceOid, String kind, String intent, Task task, OperationResult parentResult)
            throws SchemaException;

    /** Computes schema match pairs for the given resource and object type. */
    SchemaMatchResultType computeSchemaMatch(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            boolean useAiService,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /** Submits "suggest object types" request. Returns a token used to query the status. */
    String submitSuggestObjectTypesOperation(String resourceOid, QName objectClassName, Task task, OperationResult result)
            throws CommonException;

    /**
     * List statuses of all relevant "suggest object types" requests (for given resource OID).
     * They are sorted by finished time, then by started time.
     */
    List<StatusInfo<ObjectTypesSuggestionType>> listSuggestObjectTypesOperationStatuses(
            String resourceOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /** Checks the status of the "suggest object types" request. */
    StatusInfo<ObjectTypesSuggestionType> getSuggestObjectTypesOperationStatus(
            String token, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /**
     * Submits "suggest focus type" request. Returns a token used to query the status.
     */
    String submitSuggestFocusTypeOperation(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult result)
            throws CommonException;

    /**
     * List statuses of all relevant "suggest focus type" requests (for given resource OID).
     * They are sorted by finished time, then by started time.
     */
    List<StatusInfo<FocusTypeSuggestionType>> listSuggestFocusTypeOperationStatuses(
            String resourceOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /** Checks the status of the "suggest focus type" request. */
    StatusInfo<FocusTypeSuggestionType> getSuggestFocusTypeOperationStatus(
            String token, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /** Invokes the service client to suggest object types for the given resource and object class. */
    ObjectTypesSuggestionType suggestObjectTypes(
            String resourceOid,
            QName objectClassName,
            ShadowObjectClassStatisticsType statistics,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /** Suggests a discrete focus type for the application (resource) object type. */
    FocusTypeSuggestionType suggestFocusType(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /** Suggests a discrete focus type for the application (resource) object type which is not yet defined in the resource. */
    FocusTypeSuggestionType suggestFocusType(
            String resourceOid, ResourceObjectTypeDefinitionType typeDefBean, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Suggests correlation rules for the given resource object type and focus type.
     * The method returns the correlation rules along with any missing mappings that are needed for the rules to work.
     *
     * The ability to find correlation rules is limited by the information available. In particular:
     *
     * . TODO
     */
    CorrelationSuggestionsType suggestCorrelation(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            SchemaMatchResultType schemaMatch,
            @Nullable Object interactionMetadata,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Submits "suggest correlation" request. Returns a token used to query the status.
     *
     * Interaction metadata will be added later.
     */
    String submitSuggestCorrelationOperation(
            String resourceOid, ResourceObjectTypeIdentification typeIdentification, Task task, OperationResult result)
            throws CommonException;

    /**
     * List statuses of all relevant "suggest correlation" requests (for given resource OID).
     * They are sorted by finished time, then by started time.
     */
    List<StatusInfo<CorrelationSuggestionsType>> listSuggestCorrelationOperationStatuses(
            String resourceOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /** Checks the status of the "suggest correlation" request. */
    StatusInfo<CorrelationSuggestionsType> getSuggestCorrelationOperationStatus(
            String token, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /**
     * Suggests inbound/outbound mappings for the given resource object type and focus type.
     *
     * The ability to find mappings is limited by the information available. In particular, if there are
     * no correlated data (between resource and midPoint), it cannot suggest any mappings other than "as-is" ones.
     *
     * Hence, mappings and correlation suggestions should be created hand in hand. These two methods can be called
     * in alternation, most probably in a loop, until the suggestions stabilize. User should review the suggestions
     * during the process.
     *
     * @param activityState State of the current activity, if running within a task. It is used for progress reporting.
     */
    MappingsSuggestionType suggestMappings(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            SchemaMatchResultType schemaMatch,
            Boolean isInbound,
            @Nullable List<ItemPath> targetPathsToIgnore,
            @Nullable CurrentActivityState<?> activityState,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException, ActivityInterruptedException;

    /**
     * Submits a "suggest mappings" request.
     * <p>
     * Returns a token that can be used to query operation status.
     *
     * @param targetPathsToIgnore
     *         Item paths representing mapping targets that should be ignored
     *         when generating suggestions. The interpretation of these paths
     *         depends on the {@code isInbound} parameter:
     *         <p>
     *         <ul>
     *             <li><b>Inbound mappings</b>: paths on the midpoint side
     *                 (i.e. where inbound mapping results would be stored)</li>
     *             <li><b>Outbound mappings</b>: paths of resource attributes
     *                 (i.e. where outbound mapping results would be stored)</li>
     *         </ul>
     *         Any mapping whose target resolves to one of these paths will not
     *         be suggested.
     */
    String submitSuggestMappingsOperation(
            String resourceOid,
            ResourceObjectTypeIdentification typeIdentification,
            Boolean isInbound,
            List<ItemPathType> targetPathsToIgnore,
            Task task,
            OperationResult result)
            throws CommonException;

    /**
     * List statuses of all relevant "suggest mappings" requests (for given resource OID).
     * They are sorted by finished time, then by started time.
     */
    List<StatusInfo<MappingsSuggestionType>> listSuggestMappingsOperationStatuses(
            String resourceOid,
            ResourceObjectTypeIdentification objectTypeIdentification,
            Boolean isInbound,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /** Checks the status of the "suggest mappings" request. */
    StatusInfo<MappingsSuggestionType> getSuggestMappingsOperationStatus(
            String token, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /**
     * Suggests association type definitions for the given resource. (Either for all object types, or with some restrictions.)
     *
     * NOTE: Interaction metadata will be added later.
     */
    AssociationsSuggestionType suggestAssociations(
            String resourceOid,
            boolean isInbound,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException;

    /**
     * Submits "suggest associations" request. Returns a token used to query the status.
     */
    String submitSuggestAssociationsOperation(
            String resourceOid,
            Task task,
            OperationResult result)
            throws CommonException;

    /**
     * Lists statuses of all relevant "suggest associations" requests (for the given resource OID).
     * They are sorted by finished time, then by started time.
     */
    List<StatusInfo<AssociationsSuggestionType>> listSuggestAssociationsOperationStatuses(
            String resourceOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /** Checks the status of the "suggest associations" request. */
    StatusInfo<AssociationsSuggestionType> getSuggestAssociationsOperationStatus(
            String token, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException;

    /**
     * Cancels the request with the given token.
     *
     * Currently it is implemented by suspending the task that is executing the request. The user must have the authorization
     * to suspend the task.
     *
     * @param token Token of the request to cancel
     * @param timeToWait How long to wait for the task to suspend. If the time is exceeded, the method returns (a value of false)
     * and the task will stop eventually.
     * @return true if the request was successfully cancelled, false if it was not possible to cancel it in the time given.
     */
    boolean cancelRequest(String token, long timeToWait, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException,
            SecurityViolationException, CommunicationException;

    /**
     * Returns predefined synchronization reactions for the given direction scenario.
     * For TARGET scenarios, a DISPUTED reaction with a default "create correlation case" action is included
     * only when {@code includeCorrelationCaseAction} is true.
     * SOURCE scenarios do not include DISPUTED.
     */
    SynchronizationReactionsType getPredefinedSynchronizationReactions(
            SynchronizationConfigurationScenario scenario,
            boolean includeCorrelationCaseAction);

    /** Builds synchronization reactions from SOURCE scenario answers. */
    SynchronizationReactionsType buildSourceSynchronizationReactionsFromAnswers(
            SourceSynchronizationAnswers answers);

    /** Builds synchronization reactions from TARGET scenario answers. */
    SynchronizationReactionsType buildTargetSynchronizationReactionsFromAnswers(
            TargetSynchronizationAnswers answers);
}
