package com.evolveum.midpoint.model.impl.mining;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.FrequencyItem;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectStatus;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import static java.util.Collections.singleton;

public class RoleAnalysisServiceUtils {

    private RoleAnalysisServiceUtils() {
    }

    @NotNull
    protected static RoleAnalysisSessionStatisticType prepareRoleAnalysisSessionStatistic(
            @NotNull AnalysisClusterStatisticType clusterStatistics,
            @NotNull RoleAnalysisSessionStatisticType sessionStatistic,
            int deletedClusterMembersCount) {
        Double membershipDensity = clusterStatistics.getMembershipDensity();
        Integer processedObjectCount = sessionStatistic.getProcessedObjectCount();
        Double meanDensity = sessionStatistic.getMeanDensity();
        Integer clusterCount = sessionStatistic.getClusterCount();

        int newClusterCount = clusterCount - 1;

        RoleAnalysisSessionStatisticType recomputeSessionStatistic = new RoleAnalysisSessionStatisticType();

        if (newClusterCount == 0) {
            recomputeSessionStatistic.setMeanDensity(0.0);
            recomputeSessionStatistic.setProcessedObjectCount(0);
        } else {
            double recomputeMeanDensity = ((meanDensity * clusterCount) - (membershipDensity)) / newClusterCount;
            int recomputeProcessedObjectCount = processedObjectCount - deletedClusterMembersCount;
            recomputeSessionStatistic.setMeanDensity(recomputeMeanDensity);
            recomputeSessionStatistic.setProcessedObjectCount(recomputeProcessedObjectCount);
        }
        recomputeSessionStatistic.setClusterCount(newClusterCount);
        return recomputeSessionStatistic;
    }

    //TODO check it and think about better impl solution
    protected static void resolveTablePatternChunk(
            RoleAnalysisProcessModeType processMode,
            MiningOperationChunk basicChunk,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            List<List<String>> detectedPatternsRoles,
            List<String> candidateRolesIds,
            List<MiningUserTypeChunk> miningUserTypeChunks,
            List<List<String>> detectedPatternsUsers) {

        if (processMode == RoleAnalysisProcessModeType.ROLE) {
            resolveRoleModeChunkPattern(basicChunk,
                    miningRoleTypeChunks,
                    detectedPatternsRoles,
                    candidateRolesIds,
                    miningUserTypeChunks,
                    detectedPatternsUsers);
        } else {
            resolveUserModeChunkPattern(basicChunk,
                    miningRoleTypeChunks,
                    detectedPatternsRoles,
                    candidateRolesIds,
                    miningUserTypeChunks,
                    detectedPatternsUsers);
        }
    }

    private static void resolveUserModeChunkPattern(
            MiningOperationChunk basicChunk,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            List<List<String>> detectedPatternsRoles,
            List<String> candidateRolesIds,
            List<MiningUserTypeChunk> miningUserTypeChunks,
            List<List<String>> detectedPatternsUsers) {

        for (MiningRoleTypeChunk role : miningRoleTypeChunks) {
            FrequencyItem frequencyItem = role.getFrequencyItem();
            double frequency = frequencyItem.getFrequency();

            for (int i = 0; i < detectedPatternsRoles.size(); i++) {
                List<String> detectedPatternsRole = detectedPatternsRoles.get(i);
                List<String> chunkRoles = role.getRoles();
                resolvePatternChunk(basicChunk, candidateRolesIds, role, detectedPatternsRole, chunkRoles, i, frequency);
            }
        }

        for (MiningUserTypeChunk user : miningUserTypeChunks) {
            for (int i = 0; i < detectedPatternsUsers.size(); i++) {
                List<String> detectedPatternsUser = detectedPatternsUsers.get(i);
                List<String> chunkUsers = user.getUsers();
                resolveMemberPatternChunk(candidateRolesIds, user, detectedPatternsUser, chunkUsers, i);
            }
        }
    }

    private static void resolveRoleModeChunkPattern(
            MiningOperationChunk basicChunk,
            @NotNull List<MiningRoleTypeChunk> miningRoleTypeChunks,
            List<List<String>> detectedPatternsRoles,
            List<String> candidateRolesIds,
            @NotNull List<MiningUserTypeChunk> miningUserTypeChunks,
            List<List<String>> detectedPatternsUsers) {
        for (MiningUserTypeChunk user : miningUserTypeChunks) {
            FrequencyItem frequencyItem = user.getFrequencyItem();
            double frequency = frequencyItem.getFrequency();

            for (int i = 0; i < detectedPatternsUsers.size(); i++) {
                List<String> detectedPatternsUser = detectedPatternsUsers.get(i);
                List<String> chunkUsers = user.getUsers();
                resolvePatternChunk(basicChunk, candidateRolesIds, user, detectedPatternsUser, chunkUsers, i, frequency);
            }
        }

        for (MiningRoleTypeChunk role : miningRoleTypeChunks) {
            for (int i = 0; i < detectedPatternsRoles.size(); i++) {
                List<String> detectedPatternsRole = detectedPatternsRoles.get(i);
                List<String> chunkRoles = role.getRoles();
                resolveMemberPatternChunk(candidateRolesIds, role, detectedPatternsRole, chunkRoles, i);
            }
        }
    }

    private static void resolveMemberPatternChunk(
            List<String> candidateRolesIds,
            MiningBaseTypeChunk memberChunk,
            List<String> detectedPatternsMembers,
            List<String> chunkMembers,
            int i) {
        if (new HashSet<>(detectedPatternsMembers).containsAll(chunkMembers)) {
            RoleAnalysisObjectStatus objectStatus = memberChunk.getObjectStatus();
            objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
            objectStatus.addContainerId(candidateRolesIds.get(i));
            detectedPatternsMembers.removeAll(chunkMembers);
        } else if (!memberChunk.getStatus().isInclude()) {
            memberChunk.setStatus(RoleAnalysisOperationMode.EXCLUDE);
        }
    }

    private static void resolvePatternChunk(MiningOperationChunk basicChunk,
            List<String> candidateRolesIds,
            MiningBaseTypeChunk chunk,
            List<String> detectedPatternsMembers,
            List<String> chunkMembers,
            int i,
            double frequency) {
        if (new HashSet<>(detectedPatternsMembers).containsAll(chunkMembers)) {
            RoleAnalysisObjectStatus objectStatus = chunk.getObjectStatus();
            objectStatus.setRoleAnalysisOperationMode(RoleAnalysisOperationMode.INCLUDE);
            objectStatus.addContainerId(candidateRolesIds.get(i));
            detectedPatternsMembers.removeAll(chunkMembers);
        } else if (basicChunk.getMinFrequency() > frequency && frequency < basicChunk.getMaxFrequency()
                && !chunk.getStatus().isInclude()) {
            chunk.setStatus(RoleAnalysisOperationMode.DISABLE);
        } else if (!chunk.getStatus().isInclude()) {
            chunk.setStatus(RoleAnalysisOperationMode.EXCLUDE);
        }
    }

    protected static void addAdditionalObject(
            @NotNull RoleAnalysisService roleAnalysisService,
            String candidateRoleId,
            @NotNull List<String> detectedPatternUsers,
            @NotNull List<String> detectedPatternRoles,
            @NotNull List<MiningUserTypeChunk> users,
            @NotNull List<MiningRoleTypeChunk> roles,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisObjectStatus roleAnalysisObjectStatus = new RoleAnalysisObjectStatus(RoleAnalysisOperationMode.INCLUDE);
        roleAnalysisObjectStatus.setContainerId(singleton(candidateRoleId));

        if (!detectedPatternRoles.isEmpty()) {
            Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
            ListMultimap<String, String> mappedMembers = roleAnalysisService.extractUserTypeMembers(
                    userExistCache, null, new HashSet<>(detectedPatternRoles), task, result);

            for (String detectedPatternRole : detectedPatternRoles) {
                List<String> properties = new ArrayList<>(mappedMembers.get(detectedPatternRole));
                PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(detectedPatternRole, task, result);
                String chunkName = "Unknown";
                String iconColor = null;
                if (roleTypeObject != null) {
                    chunkName = roleTypeObject.getName().toString();
                    iconColor = roleAnalysisService.resolveFocusObjectIconColor(roleTypeObject.asObjectable(), task, result);
                }

                MiningRoleTypeChunk miningRoleTypeChunk = new MiningRoleTypeChunk(
                        Collections.singletonList(detectedPatternRole),
                        properties,
                        chunkName,
                        new FrequencyItem(100.0),
                        roleAnalysisObjectStatus);
                if (iconColor != null) {
                    miningRoleTypeChunk.setIconColor(iconColor);
                }
                roles.add(miningRoleTypeChunk);
            }

        }

        if (!detectedPatternUsers.isEmpty()) {
            for (String detectedPatternUser : detectedPatternUsers) {
                PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(detectedPatternUser, task, result);
                List<String> properties = new ArrayList<>();
                String chunkName = "Unknown";
                String iconColor = null;
                if (userTypeObject != null) {
                    chunkName = userTypeObject.getName().toString();
                    properties = getRolesOidAssignment(userTypeObject.asObjectable());
                    iconColor = roleAnalysisService.resolveFocusObjectIconColor(userTypeObject.asObjectable(), task, result);
                }

                MiningUserTypeChunk miningUserTypeChunk = new MiningUserTypeChunk(
                        Collections.singletonList(detectedPatternUser),
                        properties,
                        chunkName,
                        new FrequencyItem(100.0),
                        roleAnalysisObjectStatus);

                if (iconColor != null) {
                    miningUserTypeChunk.setIconColor(iconColor);
                }

                users.add(miningUserTypeChunk);
            }
        }
    }

    protected static void executeBusinessRoleMigrationTask(
            @NotNull ModelInteractionService modelInteractionService,
            @NotNull ActivityDefinitionType activityDefinition,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull TaskType taskObject) {
        try {

            modelInteractionService.submit(
                    activityDefinition,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(taskObject)
                            .withArchetypes(
                                    SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()),
                    task, result);
        } catch (CommonException e) {
            throw new SystemException("Couldn't execute business role migration activity: ", e);
        }
    }

    protected static void switchRoleToActiveLifeState(
            @NotNull ModelService modelService,
            @NotNull PrismObject<RoleType> roleObject,
            @NotNull Trace logger,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleType.class)
                    .item(ObjectType.F_LIFECYCLE_STATE).replace("active")
                    .asObjectDelta(roleObject.getOid());

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

            modelService.executeChanges(deltas, null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException |
                ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException |
                SecurityViolationException e) {
            logger.error("Couldn't update lifecycle state of object RoleType {}", roleObject, e);
        }
    }

    protected static @Nullable ActivityDefinitionType createMigrationActivity(
            @NotNull List<FocusType> roleMembersOid,
            @NotNull String roleOid,
            @NotNull Trace logger,
            @NotNull OperationResult result) {
        if (roleMembersOid.isEmpty()) {
            result.recordWarning("Couldn't start migration. There are no members to migrate.");
            logger.warn("Couldn't start migration. There are no members to migrate.");
            return null;
        }

        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setType(RoleType.COMPLEX_TYPE);
        objectReferenceType.setOid(roleOid);

        RoleMembershipManagementWorkDefinitionType roleMembershipManagementWorkDefinitionType = new RoleMembershipManagementWorkDefinitionType();
        roleMembershipManagementWorkDefinitionType.setRoleRef(objectReferenceType);

        ObjectSetType members = new ObjectSetType();
        for (FocusType member : roleMembersOid) {
            ObjectReferenceType memberRef = new ObjectReferenceType();
            memberRef.setOid(member.getOid());
            memberRef.setType(FocusType.COMPLEX_TYPE);
            members.getObjectRef().add(memberRef);
        }
        roleMembershipManagementWorkDefinitionType.setMembers(members);

        return new ActivityDefinitionType()
                .work(new WorkDefinitionsType()
                        .roleMembershipManagement(roleMembershipManagementWorkDefinitionType));
    }

    protected static void cleanClusterDetectedPatterns(
            @NotNull RepositoryService repositoryService,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull Trace logger,
            @NotNull OperationResult result) {
        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTED_PATTERN).replace(Collections.emptyList())
                    .asItemDelta());

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_CLUSTER_STATISTICS, AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC)
                    .replace(0.0)
                    .asItemDelta());

            repositoryService.modifyObject(RoleAnalysisClusterType.class, cluster.getOid(), modifications, result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            logger.error("Couldn't execute migration recompute RoleAnalysisClusterDetectionOptions {}", cluster.getOid(), e);
        }
    }

    @NotNull
    protected static AssignmentType getAssignmentTo(String unassignedRole) {
        return createAssignmentTo(unassignedRole, ObjectTypes.ROLE);
    }

    protected static double calculateDensity(@NotNull List<RoleAnalysisAttributeAnalysis> attributeAnalysisList) {
        double totalDensity = 0.0;
        for (RoleAnalysisAttributeAnalysis attributeAnalysis : attributeAnalysisList) {
            Double density = attributeAnalysis.getDensity();
            if (density != null) {
                totalDensity += density;
            }
        }
        return totalDensity;
    }

    protected static @Nullable Set<String> extractCorrespondingOutlierValues(
            @NotNull RoleAnalysisAttributeAnalysisResult outlierCandidateAttributeAnalysisResult, String itemPath) {
        List<RoleAnalysisAttributeAnalysis> outlier = outlierCandidateAttributeAnalysisResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis outlierAttribute : outlier) {
            if (outlierAttribute.getItemPath().equals(itemPath)) {
                Set<String> outlierValues = new HashSet<>();
                for (RoleAnalysisAttributeStatistics attributeStatistic : outlierAttribute.getAttributeStatistics()) {
                    outlierValues.add(attributeStatistic.getAttributeValue());
                }
                return outlierValues;
            }
        }
        return null;
    }

    @Nullable
    public static DetectedPattern findPatternWithBestConfidence(@NotNull List<DetectedPattern> detectedPatterns) {
        double maxOverallConfidence = 0;
        DetectedPattern topDetectedPattern = null;
        for (DetectedPattern detectedPattern : detectedPatterns) {
            double itemsConfidence = detectedPattern.getItemsConfidence();
            double reductionFactorConfidence = detectedPattern.getReductionFactorConfidence();
            double overallConfidence = itemsConfidence + reductionFactorConfidence;
            if (overallConfidence > maxOverallConfidence) {
                maxOverallConfidence = overallConfidence;
                topDetectedPattern = detectedPattern;
            }
        }
        return topDetectedPattern;
    }

    @Nullable
    protected static DetectedPattern findMultiplePatternWithBestConfidence(
            @NotNull List<DetectedPattern> topDetectedPatterns) {
        DetectedPattern detectedPattern = null;
        for (DetectedPattern topDetectedPattern : topDetectedPatterns) {
            if (detectedPattern == null) {
                detectedPattern = topDetectedPattern;
                continue;
            }
            double itemsConfidence = detectedPattern.getItemsConfidence();
            double reductionFactorConfidence = detectedPattern.getReductionFactorConfidence();
            double overallConfidence = itemsConfidence + reductionFactorConfidence;

            double itemsConfidenceTop = topDetectedPattern.getItemsConfidence();
            double reductionFactorConfidenceTop = topDetectedPattern.getReductionFactorConfidence();
            double overallConfidenceTop = itemsConfidenceTop + reductionFactorConfidenceTop;

            if (overallConfidenceTop > overallConfidence) {
                detectedPattern = topDetectedPattern;
            }
        }
        return detectedPattern;
    }

    protected static @NotNull List<AssignmentPathMetadataType> computeAssignmentPaths(
            @NotNull ObjectReferenceType roleMembershipRef) {
        List<AssignmentPathMetadataType> assignmentPaths = new ArrayList<>();
        List<ProvenanceMetadataType> metadataValues = collectProvenanceMetadata(roleMembershipRef.asReferenceValue());
        if (metadataValues == null) {
            return assignmentPaths;
        }
        for (ProvenanceMetadataType metadataType : metadataValues) {
            assignmentPaths.add(metadataType.getAssignmentPath());
        }
        return assignmentPaths;
    }

    protected static <PV extends PrismValue> List<ProvenanceMetadataType> collectProvenanceMetadata(PV rowValue) {
        List<ValueMetadataType> valueMetadataValues = collectValueMetadata(rowValue);
        return valueMetadataValues.stream()
                .map(ValueMetadataType::getProvenance)
                .collect(Collectors.toList());

    }

    protected static <PV extends PrismValue> @NotNull List<ValueMetadataType> collectValueMetadata(@NotNull PV rowValue) {
        PrismContainer<ValueMetadataType> valueMetadataContainer = rowValue.getValueMetadataAsContainer();
        return (List<ValueMetadataType>) valueMetadataContainer.getRealValues();
    }
}
