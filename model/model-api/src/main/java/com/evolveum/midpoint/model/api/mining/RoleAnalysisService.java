/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.mining;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChannelMode;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RoleAnalysisService {

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
    void replaceDetectionPattern(
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
            @NotNull List<AssignmentType> assignmentTypes,
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
    RoleAnalysisProcessModeType resolveClusterProcessMode(
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
     * Method for preparing an expanded mining structure for role analysis.
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
    @NotNull MiningOperationChunk prepareExpandedMiningStructure(
            @NotNull RoleAnalysisClusterType cluster,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task);

    /**
     * Retrieves a RoleType PrismObject from a cache or, if not present,
     * fetches it from the ModelService and stores it in the cache.
     *
     * @param roleExistCache A cache storing previously fetched RoleType PrismObjects.
     * @param roleOid The OID of the RoleType PrismObject to retrieve.
     * @param task The task associated with the operation.
     * @param result The operation result.
     * @return The RoleType PrismObject fetched from the cache or ModelService, or null if not found.
     */
    @Nullable
    PrismObject<RoleType> cacheRoleTypeObject(
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull String roleOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Retrieves a UserType PrismObject from a cache or, if not present,
     * fetches it from the ModelService and stores it in the cache.
     *
     * @param userExistCache A cache storing previously fetched UserType PrismObjects.
     * @param userOid The OID of the UserType PrismObject to retrieve.
     * @param task The task associated with the operation.
     * @param result The operation result.
     * @return The UserType PrismObject fetched from the cache or ModelService, or null if not found.
     */
    @Nullable
    PrismObject<UserType> cacheUserTypeObject(
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull String userOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * This method is used to execute a migration task.
     * It replaces the role assignment with business role assignment.
     *
     * @param cluster The cluster under which the migration task is executed.
     * @param activityDefinition The activity definition.
     * @param roleObject The role object for migration.
     * @param taskOid The OID of the task.
     * @param taskName The name of the task.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void executeMigrationTask(
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
     * @param cluster The cluster under which the detection task is executed.
     * @param taskOid The OID of the task.
     * @param taskName The name of the task.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void executeDetectionTask(
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * This method is used to execute a clustering task.
     * It creates a new cluster and stores it in the session.
     *
     * @param session The session under which the clustering task is executed.
     * @param taskOid The OID of the task.
     * @param taskName The name of the task.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void executeClusteringTask(
            @NotNull PrismObject<RoleAnalysisSessionType> session,
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * This method is used to update the cluster detected patterns.
     * Currently, it is used to update the cluster detected patterns
     * after the migration task in the cluster.
     *
     * @param clusterRefOid The cluster OID.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void updateClusterPatterns(
            @NotNull String clusterRefOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Recompute and resolve the cluster operation status.
     * This method also update the cluster operation status if detect some changes.
     *
     * @param clusterPrismObject The cluster for recompute and resolve.
     * @param channelMode The channel mode.
     * @param result The operation result.
     * @param task The task associated with this operation.
     * @return The cluster operation status.
     */
    @NotNull String recomputeAndResolveClusterOpStatus(
            @NotNull PrismObject<RoleAnalysisClusterType> clusterPrismObject,
            @NotNull RoleAnalysisChannelMode channelMode,
            @NotNull OperationResult result,
            @NotNull Task task);

    /**
     * Recompute and resolve the cluster operation status.
     * This method also update the cluster operation status if detect some changes.
     *
     * @param clusterPrismObject The cluster for recompute and resolve.
     * @param channelMode The channel mode.
     * @param result The operation result.
     * @param task The task associated with this operation.
     * @return The cluster operation status.
     */
    @NotNull String recomputeAndResolveSessionOpStatus(
            @NotNull PrismObject<RoleAnalysisSessionType> clusterPrismObject,
            @NotNull RoleAnalysisChannelMode channelMode,
            @NotNull OperationResult result,
            @NotNull Task task);

    /**
     * This method is used to update the cluster operation status.
     *
     * @param object The assignment holder object.
     * @param taskOid The OID of the task.
     * @param operationResultStatusType The operation result status type.
     * @param message The message to set.
     * @param channelMode The channel mode.
     * @param result The operation result.
     * @param task The task associated with this operation.
     */
    <T extends AssignmentHolderType & Objectable> void setOpStatus(
            @NotNull PrismObject<T> object,
            @NotNull String taskOid,
            OperationResultStatusType operationResultStatusType,
            String message,
            @NotNull RoleAnalysisChannelMode channelMode,
            @NotNull OperationResult result,
            @NotNull Task task);

    /**
     * This method is used to check if the role analysis object is under activity.
     *
     * @param <T> The assignment holder type.
     * @param object The assignment holder object.
     * @param channelMode The channel mode.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The operation execution status.
     */
    <T extends AssignmentHolderType & Objectable> boolean isUnderActivity(
            @NotNull PrismObject<T> object,
            @NotNull RoleAnalysisChannelMode channelMode,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * This method is used to retrive the task object for specific roleAnalysisChannelMode.
     *
     * @param operationExecution The operation execution list.
     * @param channelMode The channel mode.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The task object.
     */
    @Nullable PrismObject<TaskType> resolveTaskObject(
            @NotNull List<OperationExecutionType> operationExecution,
            @NotNull RoleAnalysisChannelMode channelMode,
            @NotNull Task task,
            @NotNull OperationResult result);
}
