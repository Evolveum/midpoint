package com.evolveum.midpoint.model.api.mining;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
     * @param repositoryService The RepositoryService for accessing role analysis data.
     * @param clusterRefOid The cluster OID.
     * @param roleRefOid The role OID.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    void clusterObjectMigrationRecompute(
            @NotNull RepositoryService repositoryService,
            @NotNull String clusterRefOid,
            @NotNull String roleRefOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    @NotNull MiningOperationChunk prepareCompressedMiningStructure(
            @NotNull RoleAnalysisClusterType cluster,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task);

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

}
