/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.mining;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisCacheOption;
import com.evolveum.midpoint.common.mining.utils.values.ZScoreData;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public interface RoleAnalysisService {

    ModelService getModelService();

    /**
     * Retrieves a PrismObject of UserType object based on its OID.
     *
     * @param oid The OID of the UserType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of UserType object, or null if not found.
     */
    @Nullable PrismObject<UserType> getUserTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Retrieves a PrismObject of FocusType object based on its OID.
     *
     * @param oid The OID of the FocusType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of FocusType object, or null if not found.
     */
    @Nullable PrismObject<FocusType> getFocusTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Retrieves a PrismObject of RoleType object based on its OID.
     *
     * @param oid The OID of the RoleType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of RoleType object, or null if not found.
     */
    @Nullable PrismObject<RoleType> getRoleTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Retrieves a PrismObject of RoleAnalysisClusterType object based on its OID.
     *
     * @param oid The OID of the RoleAnalysisClusterType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of RoleAnalysisClusterType object, or null if not found.
     */
    @Nullable PrismObject<RoleAnalysisClusterType> getClusterTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Retrieves a PrismObject of RoleAnalysisSessionType object based on its OID.
     *
     * @param oid The OID of the RoleAnalysisSessionType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of RoleAnalysisSessionType object, or null if not found.
     */
    @Nullable PrismObject<RoleAnalysisSessionType> getSessionTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Retrieves a PrismObject based on its OID.
     *
     * @param oid The OID of the object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of object, or null if not found.
     */
    @Nullable <T extends ObjectType> PrismObject<T> getObject(
            @NotNull Class<T> objectTypeClass,
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Retrieves the number of RoleAnalysisSessionType objects in the system.
     *
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The number of RoleAnalysisSessionType objects in the system.
     */
    @NotNull Integer countSessionTypeObjects(
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Extracts a list of user members from set of RoleType object based on provided parameters.
     *
     * @param userExistCache The cache of user objects.
     * @param userFilter The UserType filter.
     * @param clusterMembers The set of cluster members.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return A list of user members.
     */
    @NotNull ListMultimap<String, String> extractUserTypeMembers(
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @Nullable ObjectFilter userFilter,
            @NotNull Set<String> clusterMembers,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Imports a RoleAnalysisClusterType object into the system.
     *
     * @param cluster The cluster for importing.
     * @param roleAnalysisSessionDetectionOption The session detection option.
     * @param parentRef The parent Role analysis session reference.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void importCluster(
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption,
            @NotNull ObjectReferenceType parentRef,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Modifies statistics of a RoleAnalysisSessionType object.
     *
     * @param sessionRef The session reference.
     * @param sessionStatistic The session statistic to modify.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void updateSessionStatistics(
            @NotNull ObjectReferenceType sessionRef,
            @NotNull RoleAnalysisSessionStatisticType sessionStatistic,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Replaces the detected patterns of a RoleAnalysisClusterType object.
     *
     * @param clusterOid The cluster OID.
     * @param detectedPatterns The detected patterns to replace.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void anylseAttributesAndReplaceDetectionPattern(
            @NotNull String clusterOid,
            @NotNull List<DetectedPattern> detectedPatterns,
            @NotNull Task task,
            @NotNull OperationResult result);

    @NotNull AnalysisClusterStatisticType getUpdatedAnalysisClusterStatistic(double maxReduction,
            @NotNull AnalysisClusterStatisticType clusterStatistics);

    /**
     * Generates a set of object references based on a provided parameters.
     *
     * @param objects The objects to create references for.
     * @param complexType The complex type of the objects.
     * @param task The task associated with this operation.
     * @param operationResult The operation result.
     * @return A set of object references.
     */
    @NotNull Set<ObjectReferenceType> generateObjectReferences(
            @NotNull Set<String> objects,
            @NotNull QName complexType,
            @NotNull Task task,
            @NotNull OperationResult operationResult);

    /**
     * Deletes all RoleAnalysisClusterType objects associated with a specific session.
     *
     * @param sessionOid The session OID.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void deleteSessionClustersMembers(
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Deletes a single RoleAnalysisClusterType object.
     *
     * @param cluster The cluster to delete.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void deleteCluster(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Recomputes the statistics of a RoleAnalysisSessionType object.
     *
     * @param sessionOid The session OID.
     * @param roleAnalysisClusterType The cluster to recompute statistics for.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void recomputeSessionStatics(
            @NotNull String sessionOid,
            @NotNull RoleAnalysisClusterType roleAnalysisClusterType,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Counts the number of members of a RoleType object.
     *
     * @param userFilter The UserType filter.
     * @param objectId The OID of the RoleType object.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The number of user members of a RoleType object.
     */
    @NotNull Integer countUserTypeMembers(
            @Nullable ObjectFilter userFilter,
            @NotNull String objectId,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Retrieves a RoleType object that represents a business role.
     *
     * @param assignmentTypes The assignment types that represent inducements of the business role.
     * @param name The name of the business role.
     * @return The PrismObject of RoleType object.
     */
    @NotNull PrismObject<RoleType> generateBusinessRole(
            @NotNull Set<AssignmentType> assignmentTypes,
            @NotNull PolyStringType name);

    /**
     * Deletes a single RoleAnalysisSessionType object.
     *
     * @param sessionOid The role analysis session OID.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void deleteSession(
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Resolves the process mode of a RoleAnalysisClusterType object based on role analysis session.
     *
     * @param cluster The cluster to resolve the process mode for.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The resolved process mode.
     */
    RoleAnalysisOptionType resolveClusterOptionType(
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Recompute the detection options of a RoleAnalysisClusterType object.
     *
     * @param clusterOid The cluster OID.
     * @param detectionOption The detection option to recompute.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void recomputeClusterDetectionOptions(
            @NotNull String clusterOid,
            @NotNull DetectionOption detectionOption,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Recompute role analysis cluster (RoleAnalysisClusterType) parameters.
     * This method should be called after migration to business role.
     *
     * @param clusterRefOid The cluster OID.
     * @param roleRefOid The role OID.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void clusterObjectMigrationRecompute(
            @NotNull String clusterRefOid,
            @NotNull String roleRefOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Method for preparing a compressed mining structure for role analysis.
     *
     * @param cluster The cluster for which the mining structure is prepared.
     * @param fullProcess The full process flag.
     * If true, the entire structure is prepared.
     * If false, only a partial structure (members) is prepared.
     * @param processMode The process mode.
     * @param result The operation result.
     * @param task The task associated with this operation.
     * @return A MiningOperationChunk containing user and role chunks for further processing.
     */
    @NotNull MiningOperationChunk prepareCompressedMiningStructure(
            @NotNull RoleAnalysisClusterType cluster,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task);

    /**
     * Method for preparing a mining structure for role analysis.
     *
     * @param cluster The cluster for which the mining structure is prepared.
     * @param option The display value option.
     * @param processMode The process mode.
     * @param result The operation result.
     * @param task The task associated with this operation.
     * @return A MiningOperationChunk containing user and role chunks for further processing.
     */
    @NotNull MiningOperationChunk prepareMiningStructure(
            @NotNull RoleAnalysisClusterType cluster,
            DisplayValueOption option,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task);

    /**
     * Method for preparing an expanded mining structure for role analysis.
     *
     * @param cluster The cluster for which the mining structure is prepared.
     * @param fullProcess The full process flag.
     * If true, the entire structure is prepared.
     * If false, only a partial structure (members) is prepared.
     * @param processMode The process mode.
     * @param result The operation result.
     * @param task The task associated with this operation.
     * @param option The display value option.
     * @return A MiningOperationChunk containing user and role chunks for further processing.
     */
    @NotNull MiningOperationChunk prepareExpandedMiningStructure(
            @NotNull RoleAnalysisClusterType cluster,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task,
            @Nullable DisplayValueOption option);

    /**
     * Retrieves a RoleType PrismObject from a cache or, if not present,
     * fetches it from the ModelService and stores it in the cache.
     *
     * @param roleExistCache A cache storing previously fetched RoleType PrismObjects.
     * @param roleOid The OID of the RoleType PrismObject to retrieve.
     * @param task The task associated with the operation.
     * @param result The operation result.
     * @param option The cache option.
     * @return The RoleType PrismObject fetched from the cache or ModelService, or null if not found.
     */
    @Nullable
    PrismObject<RoleType> cacheRoleTypeObject(
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull String roleOid,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable RoleAnalysisCacheOption option);

    /**
     * Retrieves a UserType PrismObject from a cache or, if not present,
     * fetches it from the ModelService and stores it in the cache.
     *
     * @param userExistCache A cache storing previously fetched UserType PrismObjects.
     * @param userOid The OID of the UserType PrismObject to retrieve.
     * @param task The task associated with the operation.
     * @param result The operation result.
     * @param option The cache option.
     * @return The UserType PrismObject fetched from the cache or ModelService, or null if not found.
     */
    @Nullable
    PrismObject<UserType> cacheUserTypeObject(
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull String userOid,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable RoleAnalysisCacheOption option);

    /**
     * This method is used to execute a migration task.
     * It replaces the role assignment with business role assignment.
     *
     * @param modelInteractionService The model interaction service.
     * @param cluster The cluster under which the migration task is executed.
     * @param activityDefinition The activity definition.
     * @param roleObject The role object for migration.
     * @param taskOid The OID of the task.
     * @param taskName The name of the task.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void executeMigrationTask(
            @NotNull ModelInteractionService modelInteractionService,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull ActivityDefinitionType activityDefinition,
            @NotNull PrismObject<RoleType> roleObject,
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * This method is used to execute a detection task.
     * Detected patterns are stored in the cluster.
     *
     * @param modelInteractionService The model interaction service.
     * @param cluster The cluster under which the detection task is executed.
     * @param taskOid The OID of the task.
     * @param taskName The name of the task.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @param state Cluster operation state
     */
    void executeDetectionTask(
            @NotNull ModelInteractionService modelInteractionService,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result,
            String state);

    /**
     * This method is used to execute a clustering task.
     * It creates a new cluster and stores it in the session.
     *
     * @param modelInteractionService The model interaction service.
     * @param session The session under which the clustering task is executed.
     * @param taskOid The OID of the task.
     * @param taskName The name of the task.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @param processingTask The processing task.
     */
    void executeClusteringTask(
            @NotNull ModelInteractionService modelInteractionService,
            @NotNull PrismObject<RoleAnalysisSessionType> session,
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull TaskType processingTask);

    /**
     * Recompute and resolve the cluster operation status.
     * This method also update the cluster operation status if detect some changes.
     *
     * @param clusterOid The cluster for recompute and resolve.
     * @param result The operation result.
     * @param task The task associated with this operation.
     * @param onlyStatusUpdate If set true pattern detection does not perform
     * @param modelInteractionService Model interactive service provider
     * @return The cluster operation status.
     */
    @NotNull String recomputeAndResolveClusterOpStatus(
            @NotNull String clusterOid,
            @NotNull OperationResult result,
            @NotNull Task task, boolean onlyStatusUpdate, @Nullable ModelInteractionService modelInteractionService);

    /**
     * Recompute and resolve the cluster operation status.
     * This method also update the cluster operation status if detect some changes.
     *
     * @param clusterPrismObject The cluster for recompute and resolve.
     * @param result The operation result.
     * @param task The task associated with this operation.
     * @return The cluster operation status.
     */
    @NotNull String recomputeAndResolveSessionOpStatus(
            @NotNull PrismObject<RoleAnalysisSessionType> clusterPrismObject,
            @NotNull OperationResult result,
            @NotNull Task task);

    /**
     * This method is used to add candidate roles to the cluster.
     *
     * @param clusterRefOid The cluster OID.
     * @param candidateRole The candidate role OID.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void addCandidateRole(
            @NotNull String clusterRefOid,
            @NotNull RoleAnalysisCandidateRoleType candidateRole,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Recomputes and resolves the operation status for the candidate role within the specified cluster.
     *
     * @param clusterPrismObject PrismObject representing the role analysis cluster.
     * @param candidateRole RoleAnalysisCandidateRoleType representing the candidate role.
     * @param result OperationResult containing the result of the operation.
     * @param task Task used for executing the operation.
     * @return A string representing the display status of the candidate role after recompute and resolution.
     */
    @NotNull String recomputeAndResolveClusterCandidateRoleOpStatus(
            @NotNull PrismObject<RoleAnalysisClusterType> clusterPrismObject,
            @NotNull RoleAnalysisCandidateRoleType candidateRole,
            @NotNull OperationResult result, Task task);

    /**
     * Deletes a single candidate role from the specified role analysis cluster.
     *
     * @param clusterPrism PrismObject representing the role analysis cluster.
     * @param candidateRoleBean RoleAnalysisCandidateRoleType representing the candidate role to be deleted.
     * @param result OperationResult containing the result of the deletion operation.
     * @param task Task used for executing the deletion operation.
     */
    void deleteSingleCandidateRole(
            @NotNull PrismObject<RoleAnalysisClusterType> clusterPrism,
            @NotNull RoleAnalysisCandidateRoleType candidateRoleBean,
            @NotNull OperationResult result, Task task);

    /**
     * Sets the operation status of a candidate role within the specified role analysis cluster.
     *
     * @param clusterPrism PrismObject representing the role analysis cluster.
     * @param candidateRoleContainer RoleAnalysisCandidateRoleType representing the candidate role container.
     * @param taskOid String representing the OID of the task associated with the operation status.
     * @param operationResultStatusType OperationResultStatusType representing the status of the operation.
     * @param message String containing the message associated with the operation status.
     * @param result OperationResult containing the result of the operation.
     * @param task Task used for executing the operation.
     * @param operationType RoleAnalysisOperation representing the type of operation.
     * @param focus FocusType representing the focus type associated with the operation status.
     */
    void setCandidateRoleOpStatus(
            @NotNull PrismObject<RoleAnalysisClusterType> clusterPrism,
            @NotNull RoleAnalysisCandidateRoleType candidateRoleContainer,
            @NotNull String taskOid,
            @Nullable OperationResultStatusType operationResultStatusType,
            @Nullable String message,
            @NotNull OperationResult result, Task task,
            @NotNull RoleAnalysisOperation operationType,
            @Nullable FocusType focus);

    /**
     * Executes changes on the candidate role within the specified role analysis cluster.
     *
     * @param cluster PrismObject representing the role analysis cluster.
     * @param roleAnalysisCandidateRoleType RoleAnalysisCandidateRoleType representing the candidate role container.
     * @param members Set of PrismObject representing the members to be assigned to the candidate role.
     * @param inducements Set of AssignmentType representing the inducements to be added to the candidate role.
     * @param task Task used for executing the operation.
     * @param result OperationResult containing the result of the operation.
     */
    void executeChangesOnCandidateRole(
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull RoleAnalysisCandidateRoleType roleAnalysisCandidateRoleType,
            @NotNull Set<PrismObject<UserType>> members,
            @NotNull Set<AssignmentType> inducements,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Loads objects iteratively from the repository based on the provided query and adds them to the modifyList
     * if they are not already present.
     *
     * @param modelService ModelService used for loading the objects from the repository.
     * @param type Class representing the type of objects to be loaded.
     * @param query ObjectQuery specifying the conditions for searching the objects. Can be null.
     * @param options Collection of SelectorOptions specifying additional options for the search operation. Can be null.
     * @param modifyList List of loaded objects will be added. Objects already present in this list will be skipped.
     * @param task Task used for executing the search operation.
     * @param parentResult OperationResult containing the result of the operation.
     * @param <T> Generic type extending ObjectType representing the type of objects to be loaded.
     */
    <T extends ObjectType> void loadSearchObjectIterative(
            @NotNull ModelService modelService,
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull List<T> modifyList,
            @NotNull Task task,
            @NotNull OperationResult parentResult);

    /**
     * Performs attribute analysis for user objects.
     *
     * @param prismUsers Set of PrismObject representing user objects to analyze.
     * @param membershipDensity The density of membership.
     * @param task Task used for processing the attribute analysis.
     * @param result OperationResult containing the result of the operation.
     * @param attributeDefSet List of RoleAnalysisAttributeDef containing the attribute definitions for user analysis.
     * @return List of AttributeAnalysisStructure containing the results of the attribute analysis.
     */
    List<AttributeAnalysisStructure> userTypeAttributeAnalysis(
            @NotNull Set<PrismObject<UserType>> prismUsers,
            Double membershipDensity,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet);

    /**
     * Performs attribute analysis for role objects.
     *
     * @param prismRoles Set of PrismObject representing role objects to analyze.
     * @param membershipDensity The density of membership.
     * @param task Task used for processing the attribute analysis.
     * @param result OperationResult containing the result of the operation.
     * @param attributeRoleDefSet List of RoleAnalysisAttributeDef containing the attribute definitions for role analysis.
     * @return List of AttributeAnalysisStructure containing the results of the attribute analysis.
     */
    List<AttributeAnalysisStructure> roleTypeAttributeAnalysis(
            @NotNull Set<PrismObject<RoleType>> prismRoles,
            Double membershipDensity,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeRoleDefSet);

    /**
     * Performs attribute analysis for role members.
     *
     * @param attributeDefSet List of RoleAnalysisAttributeDef containing the attribute definitions for analysis.
     * @param objectOid The OID of the object to analyze.
     * @param task Task used for processing the attribute analysis.
     * @param result OperationResult containing the result of the operation.
     * @return List of AttributeAnalysisStructure containing the results of the attribute analysis.
     */
    List<AttributeAnalysisStructure> roleMembersAttributeAnalysis(
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet,
            @NotNull String objectOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Performs attribute analysis for user roles.
     *
     * @param attributeRoleDefSet List of RoleAnalysisAttributeDef containing the attribute definitions for role analysis.
     * @param objectOid The OID of the object to analyze.
     * @param task Task used for processing the attribute analysis.
     * @param result OperationResult containing the result of the operation.
     * @return List of AttributeAnalysisStructure containing the results of the attribute analysis.
     */
    List<AttributeAnalysisStructure> userRolesAttributeAnalysis(
            @NotNull List<RoleAnalysisAttributeDef> attributeRoleDefSet,
            @NotNull String objectOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Processes attribute analysis for the detected patterns.
     * This method analyzes attribute usage patterns for both users and roles in the detected patterns.
     * It retrieves user and role occupancy information from the detected patterns, then performs attribute
     * analysis for both user and role types based on the specified attribute paths.
     *
     * @param detectedPatterns List of detected patterns to process.
     * @param userExistCache Map containing cached PrismObject of UserType for efficient retrieval.
     * @param roleExistCache Map containing cached PrismObject of RoleType for efficient retrieval.
     * @param task Task used for processing the attribute analysis.
     * @param result OperationResult containing the result of the operation.
     * Any errors or status information will be recorded here.
     * @param attributeRoleDefSet List of RoleAnalysisAttributeDef containing the attribute definitions for role analysis.
     * @param attributeUserDefSet List of RoleAnalysisAttributeDef containing the attribute definitions for user analysis.
     */
    void resolveDetectedPatternsAttributes(
            @NotNull List<RoleAnalysisDetectionPatternType> detectedPatterns,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable List<RoleAnalysisAttributeDef> attributeRoleDefSet,
            @Nullable List<RoleAnalysisAttributeDef> attributeUserDefSet);

    /**
     * Searches for clusters associated with a specific role analysis session.
     *
     * @param session RoleAnalysisSessionType representing the session for which clusters are being searched.
     * @param task Task used for executing the search operation.
     * @param result OperationResult containing the result of the search operation.
     * Any errors or status information will be recorded here.
     * @return List of PrismObject<RoleAnalysisClusterType> containing the clusters associated with the session.
     * If the search operation fails or no clusters are found, null is returned.
     */
    List<PrismObject<RoleAnalysisClusterType>> searchSessionClusters(
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Resolves the focus object icon color based on the provided focus object archetype.
     *
     * @param focusObject FocusType representing the focus object for which the icon color is being resolved.
     * @param task Task used for resolving the icon color.
     * @param result OperationResult containing the result of the operation.
     * Any errors or status information will be recorded here.
     * @return String representing the icon color of the focus object.
     */
    String resolveFocusObjectIconColor(
            @NotNull FocusType focusObject,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Retrieves the attribute definition for a specific attribute path.
     *
     * @param type The type of object for which the attribute definition is being retrieved.
     * @param query The query specifying the conditions for searching the object.
     * @param options Collection of SelectorOptions specifying additional options for the search operation.
     * @param task Task used for executing the search operation.
     * @param parentResult OperationResult containing the result of the search operation.
     * @return RoleAnalysisAttributeDef containing the attribute definition for the specified attribute path.
     */
    <T extends ObjectType> Integer countObjects(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult parentResult);

    /**
     * Calculates the confidence of an attribute based on the specified process mode and cluster statistics.
     *
     * @param processModeType The process mode type.
     * @param clusterStatistics The cluster statistics.
     * @return String representing the calculated attribute confidence.
     */
    String calculateAttributeConfidence(
            @NotNull RoleAnalysisProcessModeType processModeType,
            @NotNull AnalysisClusterStatisticType clusterStatistics);

    /**
     * Resolves the analysis attributes based on the provided session and complex type.
     *
     * @param session The RoleAnalysisSessionType object that contains the analysis options.
     * @param complexType The QName object that represents the complex type of the attribute.
     * @return A list of RoleAnalysisAttributeDef objects that match the provided complex type.
     * Returns null if no matching attributes are found or if the analysis option or process mode is not set in the session.
     */
    @Nullable List<RoleAnalysisAttributeDef> resolveAnalysisAttributes(
            @NotNull RoleAnalysisSessionType session,
            @NotNull QName complexType);

    /**
     * Imports a RoleAnalysisOutlierType object into the system.
     *
     * @param outlier The outlier for importing.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void importOutlier(
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull Task task,
            @NotNull OperationResult result);

    RoleAnalysisAttributeAnalysisResult resolveUserAttributes(
            @NotNull PrismObject<UserType> prismUser,
            @NotNull List<RoleAnalysisAttributeDef> attributesForUserAnalysis);

    @Nullable RoleAnalysisAttributeAnalysisResult resolveSimilarAspect(
            @NotNull RoleAnalysisAttributeAnalysisResult compared,
            @NotNull RoleAnalysisAttributeAnalysisResult comparison);

    RoleAnalysisAttributeAnalysisResult resolveRoleMembersAttribute(
            @NotNull String objectOid,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet);

    <T extends MiningBaseTypeChunk> ZScoreData resolveOutliersZScore(
            @NotNull List<T> data,
            @Nullable RangeType range,
            @Nullable Double sensitivity);
    <T extends MiningBaseTypeChunk> double calculateZScoreConfidence(@NotNull T item, ZScoreData zScoreData);

    @Nullable Set<String> resolveUserValueToMark(
            @NotNull PrismObject<UserType> prismUser,
            @NotNull List<RoleAnalysisAttributeDef> itemDef);

    /**
     * Resolve object attribute value.
     *
     * @param prismRole The role object.
     * @param itemDef The attribute definition.
     * @return Set of attribute values that role has.
     */
    @Nullable Set<String> resolveRoleValueToMark(
            @NotNull PrismObject<RoleType> prismRole,
            @NotNull List<RoleAnalysisAttributeDef> itemDef);

    /**
     * Resolves outliers for a given role analysis outlier type.
     * This method retrieves the target object reference from the provided outlier type and performs the following steps:
     * 1. Searches for existing outliers with the same target object reference.
     * 2. If no outliers are found, imports the provided outlier.
     * 3. If outliers are found, updates the existing outlier with new outlier descriptions and removes outdated descriptions.
     * <p>
     * This method is responsible for handling exceptions that may occur during the process and logs errors accordingly.
     *
     * @param roleAnalysisOutlierType The role analysis outlier type containing the outlier information.
     * @param task The task associated with the operation.
     * @param result The operation result.
     * @param session The role analysis session type containing the session information.
     * @param cluster The role analysis cluster type containing the cluster information.
     * @param requiredConfidence The required confidence for the outlier.
     */
    void resolveOutliers(
            @NotNull RoleAnalysisOutlierType roleAnalysisOutlierType,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisClusterType cluster,
            double requiredConfidence);

    /**
     * Search for the top detected patterns over all clusters
     *
     * @param task the task
     * @param result the operation result
     */
    @NotNull List<DetectedPattern> findTopPatters(
            @NotNull Task task,
            @NotNull OperationResult result);

    void replaceSessionMarkRef(
            @NotNull PrismObject<RoleAnalysisSessionType> session,
            @NotNull ObjectReferenceType newMarkRef,
            @NotNull OperationResult result,
            @NotNull Task task);

    void updateSessionMarkRef(
            @NotNull PrismObject<RoleAnalysisSessionType> session,
            @NotNull OperationResult result,
            @NotNull Task task);

    void deleteSessionTask(
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    void deleteSessionTask(
            @NotNull TaskType taskToDelete,
            @NotNull OperationResult result);

    @Nullable PrismObject<TaskType> getSessionTask(
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    List<DetectedPattern> getTopSessionPattern(
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result,
            boolean single);

    //TODO: replace this method (experiment)
    List<String> findJaccardCloseObject(
            @NotNull String userOid,
            @NotNull ListMultimap<List<String>, String> chunkMap,
            @NotNull MutableDouble usedFrequency,
            @NotNull List<String> outliersMembers,
            double minThreshold,
            int minMembers,
            @NotNull Task task,
            @NotNull OperationResult result);

    ListMultimap<List<String>, String> loadUserForOutlierComparison(
            @NotNull RoleAnalysisService roleAnalysisService,
            List<String> outliersMembers,
            int minRolesOccupancy,
            int maxRolesOccupancy,
            @Nullable SearchFilterType query,
            @NotNull OperationResult result,
            @NotNull Task task);

    /**
     * This method is used to calculate the threshold range for outlier detection.
     * The range is adjusted based on the provided sensitivity.
     *
     * @param sensitivity The sensitivity for outlier detection. It should be a value between 0.0 and 100.
     *                    If the provided value is outside this range, it will be set to 0.0.
     *                    The sensitivity is used to adjust the threshold for outlier detection.
     * @param range The initial range for outlier detection. It should be a RangeType object with min and max values.
     *              If the min or max values are null, they will be set to 2.0.
     *              Note: The range is expected to have both values positive.
     * @return The adjusted range for outlier detection. It's a RangeType object with the min and max values
     * adjusted based on the sensitivity.
     */
    RangeType calculateOutlierThresholdRange(Double sensitivity, @NotNull RangeType range);

    /**
     * Calculates the required confidence for outlier detection based on the provided sensitivity.
     * The sensitivity should be a value between 0.0 and 100. If the provided value is outside this range, the function will return 0.0.
     * The function uses the formula 1 - (sensitivity * 0.01) to calculate the required confidence.
     *
     * @param sensitivity The sensitivity for outlier detection. It should be a value between 0.0 and 100.
     * @return The required confidence for outlier detection. It's a value between 0.0 and 1.0.
     */
    double calculateOutlierConfidenceRequired(double sensitivity);

    /**
     * This method is used to find all outliers associated with a specific cluster.
     *
     * @param cluster The cluster for which to find associated outliers. It should be a RoleAnalysisClusterType object.
     * @param task The task in context. It should be a Task object.
     * @param result The operation result. It should be an OperationResult object.
     * @return A list of RoleAnalysisOutlierType objects that are associated with the provided cluster.
     */
    List<RoleAnalysisOutlierType> findClusterOutliers(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result);
}
