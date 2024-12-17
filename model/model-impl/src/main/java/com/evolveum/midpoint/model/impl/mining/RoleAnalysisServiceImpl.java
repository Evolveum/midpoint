/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining;

import static com.evolveum.midpoint.model.impl.mining.RoleAnalysisDataServiceUtils.*;
import static com.evolveum.midpoint.model.impl.mining.RoleAnalysisServiceUtils.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ASSIGNMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

import static java.util.Collections.singleton;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.*;
import static com.evolveum.midpoint.common.mining.utils.algorithm.JaccardSorter.jacquardSimilarity;
import static com.evolveum.midpoint.model.impl.mining.analysis.AttributeAnalysisUtil.*;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisUtils.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_MODIFY_TIMESTAMP;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.mining.objects.analysis.cache.ObjectCategorisationCache;
import com.evolveum.midpoint.common.mining.objects.detection.BasePattern;
import com.evolveum.midpoint.common.mining.objects.statistic.UserAccessDistribution;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
import com.evolveum.midpoint.prism.delta.ChangeType;

import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.query.builder.S_QueryExit;
import com.evolveum.midpoint.repo.api.AggregateQuery;

import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.analysis.cache.AttributeAnalysisCache;
import com.evolveum.midpoint.common.mining.objects.chunk.*;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.PatternDetectionOption;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisCacheOption;
import com.evolveum.midpoint.common.mining.utils.values.*;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.impl.mining.chunk.CompressedMiningStructure;
import com.evolveum.midpoint.model.impl.mining.chunk.ExpandedMiningStructure;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Utility methods for working with role analysis objects in the Midpoint system.
 */
@Component
public class RoleAnalysisServiceImpl implements RoleAnalysisService {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisServiceImpl.class);

    transient @Autowired ModelService modelService;
    transient @Autowired RepositoryService repositoryService;
    transient @Autowired SchemaService schemaService;
    //poc
    transient @Autowired RelationRegistry relationRegistry;

    private static final Integer RM_ITERATIVE_SEARCH_PAGE_SIZE = 10000;

    @Override
    public @Nullable PrismObject<UserType> getUserTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return modelService.getObject(UserType.class, oid, null, task, result);
        } catch (ObjectNotFoundException ox) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "User object not found", ox);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get UserType object, Probably not set yet", ex);
        }

        return null;
    }

    @Override
    public @Nullable PrismObject<FocusType> getFocusTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return modelService.getObject(FocusType.class, oid, null, task, result);
        } catch (ObjectNotFoundException ox) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Focus object not found", ox);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get FocusType object, Probably not set yet", ex);
        }

        return null;
    }

    @Override
    public @Nullable PrismObject<RoleType> getRoleTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return modelService.getObject(RoleType.class, oid, null, task, result);
        } catch (ObjectNotFoundException ox) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Role object not found", ox);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get RoleType object, Probably not set yet", ex);
        }
        return null;
    }

    @Override
    public @Nullable PrismObject<RoleAnalysisClusterType> getClusterTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return modelService.getObject(RoleAnalysisClusterType.class, oid, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                    "Couldn't get RoleAnalysisClusterType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    @Override
    public @Nullable PrismObject<RoleAnalysisSessionType> getSessionTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return modelService.getObject(RoleAnalysisSessionType.class, oid, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                    "Couldn't get RoleAnalysisSessionType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    @Override
    public @Nullable <T extends ObjectType> PrismObject<T> getObject(
            @NotNull Class<T> objectTypeClass,
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return modelService.getObject(objectTypeClass, oid, null, task, result);
        } catch (ObjectNotFoundException ox) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                    "Object not found", ox);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                    "Couldn't get object, Probably not set yet", ex);
        }

        return null;
    }

    @Override
    public @NotNull Integer countSessionTypeObjects(
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return modelService.countObjects(RoleAnalysisSessionType.class, null, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                    "Couldn't count RoleAnalysisSessionType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return 0;
    }

    @Override
    public @NotNull ListMultimap<String, String> extractUserTypeMembers(
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @Nullable SearchFilterType userFilter,
            @NotNull Set<String> clusterMembers,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ListMultimap<String, String> roleMemberCache = ArrayListMultimap.create();
//TODO try this
//        ObjectQuery query = PrismContext.get().
//                queryFor(AssignmentType.class).ownedBy(UserType.class).
//                and()
//                .item(AssignmentType.F_TARGET_REF).ref(clusterMembers.toArray(new String[0])).build();

        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .exists(F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(clusterMembers.toArray(new String[0]))
                .endBlock().build();

        if (userFilter != null) {
            try {
                ObjectFilter objectFilter = PrismContext.get().getQueryConverter()
                        .createObjectFilter(UserType.class, userFilter);
                if (objectFilter != null) {
                    query.addFilter(objectFilter);
                }
            } catch (SchemaException e) {
                throw new SystemException("Couldn't create object filter", e);
            }
        }

        ResultHandler<UserType> resultHandler = (userObject, lResult) -> {
            try {
                boolean shouldCacheUser = false;
                List<AssignmentType> assignments = userObject.asObjectable().getAssignment();

                for (AssignmentType assignment : assignments) {
                    ObjectReferenceType targetRef = assignment.getTargetRef();
                    if (targetRef != null && clusterMembers.contains(targetRef.getOid())) {
                        roleMemberCache.put(targetRef.getOid(), userObject.getOid());
                        shouldCacheUser = true;
                    }
                }

                if (shouldCacheUser) {
                    userExistCache.put(userObject.getOid(), userObject);
                }
            } catch (Exception e) {
                String errorMessage = "Cannot resolve role members: " + toShortString(userObject.asObjectable())
                        + ": " + e.getMessage();
                throw new SystemException(errorMessage, e);
            }

            return true;
        };

        try {
            modelService.searchObjectsIterative(UserType.class, query, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Failed to search role member objects:", ex);
        } finally {
            result.recomputeStatus();
        }

        return roleMemberCache;
    }

    /**
     * Extracts the members of a given role based on the specified filter.
     *
     * @param filter An optional filter to apply to the user search.
     * @param role The role for which members are to be extracted.
     * @param task The task in the context of which the operation is executed.
     * @param result The result of the operation.
     * @return A list of FocusType objects representing the members of the role.
     */
    public @NotNull List<FocusType> extractRoleMembers(
            @Nullable SearchFilterType filter,
            @NotNull RoleType role,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<FocusType> roleMembers = new ArrayList<>();

        ObjectQuery query = PrismContext.get().queryFor(FocusType.class)
                .exists(F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(role.getOid())
                .endBlock().build();

        if (filter != null) {
            try {
                ObjectFilter objectFilter = PrismContext.get().getQueryConverter()
                        .createObjectFilter(FocusType.class, filter);
                if (objectFilter != null) {
                    query.addFilter(objectFilter);
                }
            } catch (SchemaException e) {
                throw new SystemException("Couldn't create object filter", e);
            }
        }

        ResultHandler<FocusType> resultHandler = (object, lResult) -> {
            try {
                roleMembers.add(object.asObjectable());
            } catch (Exception e) {
                String errorMessage = "Cannot resolve role members: " + toShortString(object.asObjectable())
                        + ": " + e.getMessage();
                throw new SystemException(errorMessage, e);
            }

            return true;
        };

        try {
            modelService.searchObjectsIterative(FocusType.class, query, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Failed to search role member objects:", ex);
        } finally {
            result.recomputeStatus();
        }

        return roleMembers;
    }

    @Override //experiment
    public int countUserTypeMembers(
            @Nullable ObjectFilter userFilter,
            @NotNull Set<String> clusterMembers,
            @NotNull Task task,
            @NotNull OperationResult result) {
        int memberCount = 0;
        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .exists(F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(clusterMembers.toArray(new String[0]))
                .endBlock().build();

        if (userFilter != null) {
            query.addFilter(userFilter);
        }

        try {
            Integer count = modelService.countObjects(UserType.class, query, null,
                    task, result);

            if (count != null) {
                memberCount = count;
            }
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Failed to search role member objects:", ex);
        } finally {
            result.recomputeStatus();
        }

        return memberCount;
    }

    @Override
    public void importCluster(
            @NotNull PrismObject<RoleAnalysisClusterType> clusterPrismObject,
            @NotNull RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption,
            @NotNull ObjectReferenceType parentRef,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisClusterType clusterObject = clusterPrismObject.asObjectable();
        clusterObject.setRoleAnalysisSessionRef(parentRef);
        clusterObject.setDetectionOption(roleAnalysisSessionDetectionOption);
        try {
            repositoryService.addObject(clusterPrismObject, null, result);
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            LOGGER.error("Couldn't import RoleAnalysisClusterType object {}", clusterPrismObject, e);
        }
    }

    @Override
    public void updateSessionStatistics(
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisSessionStatisticType sessionStatistic,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            ObjectDelta<RoleAnalysisSessionType> delta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_SESSION_STATISTIC)
                    .replace(sessionStatistic.clone())
                    .asObjectDelta(session.getOid());

            modelService.executeChanges(singleton(delta), null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't update role analysis session statistic {}", session, e);
        }
    }

    @Override
    public void updateSessionIdentifiedCharacteristics(
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_IDENTIFIED_CHARACTERISTICS)
                    .replace(identifiedCharacteristics.clone())
                    .asItemDelta());

            repositoryService.modifyObject(RoleAnalysisSessionType.class, session.getOid(), modifications, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException e) {
            LOGGER.error("Couldn't update role analysis session identifiedCharacteristics {}", session, e);
        }
    }

    @Override
    public void anylseAttributesAndReplaceDetectionPattern(
            @NotNull String clusterOid,
            @NotNull List<DetectedPattern> detectedPatterns,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypes = loadIntersections(detectedPatterns);

        double max = 0;
        Collection<PrismContainerValue<?>> collection = new ArrayList<>();

        for (RoleAnalysisDetectionPatternType clusterDetectionType : roleAnalysisClusterDetectionTypes) {
            collection.add(clusterDetectionType.asPrismContainerValue());
            max = Math.max(max, clusterDetectionType.getReductionCount());
        }

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();

        PrismObject<RoleAnalysisClusterType> clusterTypeObject = getClusterTypeObject(clusterOid, task, result);

        if (clusterTypeObject == null) {
            return;
        }

        RoleAnalysisClusterType cluster = clusterTypeObject.asObjectable();
        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();

        PrismObject<RoleAnalysisSessionType> sessionPrismObject = this.getSessionTypeObject(
                roleAnalysisSessionRef.getOid(), task, result);

        if (sessionPrismObject == null) {
            return;
        }
        RoleAnalysisSessionType session = sessionPrismObject.asObjectable();

        List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = this.resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
        List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = this.resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);
        if (userAnalysisAttributeDef != null && roleAnalysisAttributeDef != null) {
            resolveDetectedPatternsAttributes(roleAnalysisClusterDetectionTypes, userExistCache, roleExistCache, task, result,
                    roleAnalysisAttributeDef, userAnalysisAttributeDef);
        }

        AnalysisClusterStatisticType clusterStatistics = clusterTypeObject.asObjectable().getClusterStatistics();

        AnalysisClusterStatisticType analysisClusterStatisticType = getUpdatedAnalysisClusterStatistic(max, clusterStatistics);

        try {

            ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTED_PATTERN).replace(collection)
                    .item(RoleAnalysisClusterType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .item(RoleAnalysisClusterType.F_CLUSTER_STATISTICS).replace(analysisClusterStatisticType
                            .asPrismContainerValue())
                    .asObjectDelta(clusterOid);

            modelService.executeChanges(singleton(delta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't modify RoleAnalysisClusterType {}", clusterOid, e);
        }
    }

    @Override
    public @NotNull AnalysisClusterStatisticType getUpdatedAnalysisClusterStatistic(
            double maxReduction,
            @NotNull AnalysisClusterStatisticType clusterStatistics) {
        AnalysisClusterStatisticType analysisClusterStatisticType = new AnalysisClusterStatisticType();
        analysisClusterStatisticType.setDetectedReductionMetric(maxReduction);
        analysisClusterStatisticType.setMembershipDensity(clusterStatistics.getMembershipDensity());
        analysisClusterStatisticType.setRolesCount(clusterStatistics.getRolesCount());
        analysisClusterStatisticType.setUsersCount(clusterStatistics.getUsersCount());
        analysisClusterStatisticType.setMembershipMean(clusterStatistics.getMembershipMean());
        analysisClusterStatisticType.setMembershipRange(clusterStatistics.getMembershipRange().clone());
        //TODO consider update
        if (clusterStatistics.getRoleAttributeAnalysisResult() != null) {
            analysisClusterStatisticType.setRoleAttributeAnalysisResult(clusterStatistics.getRoleAttributeAnalysisResult().clone());
        }
        if (clusterStatistics.getUserAttributeAnalysisResult() != null) {
            analysisClusterStatisticType.setUserAttributeAnalysisResult(clusterStatistics.getUserAttributeAnalysisResult().clone());
        }
        return analysisClusterStatisticType;
    }

    @Override
    public @NotNull Set<ObjectReferenceType> generateObjectReferences(
            @NotNull Set<String> objects,
            @NotNull QName complexType,
            @NotNull Task task,
            @NotNull OperationResult operationResult) {
        Set<ObjectReferenceType> objectReferenceList = new HashSet<>();
        for (String item : objects) {

            PrismObject<FocusType> object = getFocusTypeObject(item, task, operationResult);
            ObjectReferenceType objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setType(complexType);
            objectReferenceType.setOid(item);
            if (object != null) {
                objectReferenceType.setTargetName(PolyStringType.fromOrig(object.getName().toString()));
            }
            objectReferenceList.add(objectReferenceType);

        }
        return objectReferenceList;
    }

    @Override
    public void deleteSessionClustersMembers(
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result,
            boolean recomputeStatistics) {
        ResultHandler<RoleAnalysisClusterType> resultHandler = (object, parentResult) -> {
            try {
                deleteCluster(object.asObjectable(), task, result, recomputeStatistics);
            } catch (Exception e) {
                throw new SystemException(e);
            }
            return true;
        };

        ObjectQuery query = PrismContext.get().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF).ref(sessionOid)
                .build();

        try {
            modelService.searchObjectsIterative(RoleAnalysisClusterType.class, query, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't deleteRoleAnalysisSessionClusters", ex);
        } finally {
            result.recomputeStatus();
        }
    }

    @Override
    public void deleteClusterOutlierOrPartition(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ResultHandler<RoleAnalysisOutlierType> resultHandler = (object, parentResult) -> {
            RoleAnalysisOutlierType outlierObject = object.asObjectable();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getPartition();

            try {

                if (outlierPartitions == null || (outlierPartitions.size() == 1
                        && outlierPartitions.get(0).getClusterRef().getOid().equals(cluster.getOid()))) {
                    repositoryService.deleteObject(RoleAnalysisOutlierType.class, outlierObject.getOid(), result);
                } else {
                    RoleAnalysisOutlierPartitionType partitionToDelete = null;

                    double overallConfidence = 0;
                    double anomalyObjectsConfidence = 0;

                    for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                        if (outlierPartition.getClusterRef().getOid().equals(cluster.getOid())) {
                            partitionToDelete = outlierPartition;
                        } else {
                            overallConfidence += outlierPartition.getPartitionAnalysis().getOverallConfidence();
                            anomalyObjectsConfidence += outlierPartition.getPartitionAnalysis().getAnomalyObjectsConfidence();
                        }
                    }

                    int partitionCount = outlierPartitions.size();
                    if (partitionToDelete != null) {
                        partitionCount--;
                    }

                    overallConfidence = overallConfidence / partitionCount;
                    anomalyObjectsConfidence = anomalyObjectsConfidence / partitionCount;

                    if (partitionToDelete == null) {
                        return true;
                    }

                    List<ItemDelta<?, ?>> modifications = new ArrayList<>();
                    var finalPartitionToDelete = new RoleAnalysisOutlierPartitionType()
                            .id(partitionToDelete.getId());
                    modifications.add(PrismContext.get().deltaFor(RoleAnalysisOutlierType.class)
                            .item(RoleAnalysisOutlierType.F_PARTITION).delete(
                                    finalPartitionToDelete)
                            .asItemDelta());

                    modifications.add(PrismContext.get().deltaFor(RoleAnalysisOutlierType.class)
                            .item(RoleAnalysisOutlierType.F_OVERALL_CONFIDENCE).replace(overallConfidence).asItemDelta());

                    modifications.add(PrismContext.get().deltaFor(RoleAnalysisOutlierType.class)
                            .item(RoleAnalysisOutlierType.F_ANOMALY_OBJECTS_CONFIDENCE)
                            .replace(anomalyObjectsConfidence).asItemDelta());

                    repositoryService.modifyObject(RoleAnalysisOutlierType.class, outlierObject.getOid(), modifications, result);

                }
            } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
                LOGGER.error("Couldn't update RoleAnalysisOutlierType {}", outlierObject, e);
            }
            return true;
        };

        ObjectQuery objectQuery = PrismContext.get().queryFor(RoleAnalysisOutlierType.class)
                .item(RoleAnalysisOutlierType.F_PARTITION, RoleAnalysisOutlierPartitionType.F_CLUSTER_REF)
                .ref(cluster.getOid()).build();

        try {
            modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, objectQuery, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't deleteRoleAnalysisSessionClusters", ex);
        } finally {
            result.recomputeStatus();
        }
    }

    @Override
    public void deleteCluster(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result,
            boolean recomputeStatistics) {
        String clusterOid = cluster.getOid();
        PrismObject<RoleAnalysisSessionType> sessionObject = getSessionTypeObject(
                cluster.getRoleAnalysisSessionRef().getOid(), task, result
        );

        RoleAnalysisProcedureType procedureType = null;
        if (sessionObject != null) {
            RoleAnalysisSessionType session = sessionObject.asObjectable();
            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
            procedureType = analysisOption.getAnalysisProcedureType();
        }

        if (procedureType == null || procedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            deleteClusterOutlierOrPartition(cluster, task, result);
        }

        try {
            ObjectDelta<RoleAnalysisClusterType> deleteDelta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(RoleAnalysisClusterType.class, clusterOid);

            modelService.executeChanges(singleton(deleteDelta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't delete RoleAnalysisClusterType {}", clusterOid, e);
        }

        if (recomputeStatistics) {
            try {
                if (sessionObject == null) {
                    return;
                }
                // FIXME
                ObjectDelta<RoleAnalysisSessionType> delta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                        .item(RoleAnalysisSessionType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                        .asObjectDelta(sessionObject.getOid());

                modelService.executeChanges(singleton(delta), null, task, result);

                recomputeSessionStatics(sessionObject.getOid(), cluster, task, result);

            } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                    CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
                LOGGER.error("Couldn't recompute RoleAnalysisSessionStatistic {}", sessionObject, e);
            }
        }
    }

    @Override
    public void recomputeSessionStatics(
            @NotNull String sessionOid,
            @NotNull RoleAnalysisClusterType roleAnalysisClusterType,
            @NotNull Task task,
            @NotNull OperationResult result) {
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(sessionOid, task, result
        );

        assert sessionTypeObject != null;
        RoleAnalysisSessionType session = sessionTypeObject.asObjectable();

        int deletedClusterMembersCount = roleAnalysisClusterType.getMember().size();
        AnalysisClusterStatisticType clusterStatistics = roleAnalysisClusterType.getClusterStatistics();
        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        if (sessionStatistic == null || clusterStatistics == null) {
            LOGGER.error("Couldn't recompute RoleAnalysisSessionStatistic {}. "
                    + "Statistic container is null:{},{}", sessionOid, sessionStatistic, clusterStatistics);
            return;
        }

        RoleAnalysisSessionStatisticType recomputeSessionStatistic = prepareRoleAnalysisSessionStatistic(clusterStatistics,
                sessionStatistic, deletedClusterMembersCount);

        try {

            ObjectDelta<RoleAnalysisSessionType> delta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_SESSION_STATISTIC).replace(recomputeSessionStatistic.asPrismContainerValue())
                    .asObjectDelta(sessionOid);

            modelService.executeChanges(singleton(delta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't recompute RoleAnalysisSessionStatistic {}", sessionOid, e);
        }
    }

    @Override
    public @Nullable PrismObject<RoleType> cacheRoleTypeObject(
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull String roleOid,
            @NotNull Task task,
            @NotNull OperationResult result, @Nullable RoleAnalysisCacheOption option) {
        PrismObject<RoleType> role = roleExistCache.get(roleOid);
        if (role == null) {
            role = getRoleTypeObject(roleOid, task, result);
            if (role == null) {
                return null;
            }

            if (option != null) {
                try {
                    PrismObject<RoleType> cacheRole = new RoleType().asPrismObject();
                    List<RoleAnalysisAttributeDef> itemDef = option.getItemDef();
                    for (RoleAnalysisAttributeDef roleAnalysisAttributeDef : itemDef) {
                        ItemPath path = roleAnalysisAttributeDef.getPath();
                        boolean isContainer = roleAnalysisAttributeDef.isContainer();

                        if (isContainer) {
                            PrismContainer<Containerable> container = role.findContainer(path);
                            if (container != null) {
                                cacheRole.add(container.clone());
                            }
                        } else {
                            Item<PrismValue, ItemDefinition<?>> property = role.findItem(path);
                            if (property != null) {
                                cacheRole.add(property.clone());
                            }
                        }
                    }
                    roleExistCache.put(roleOid, cacheRole);
                    return cacheRole;
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't prepare role for cache", e);
                }
            }

            roleExistCache.put(roleOid, role);
        }
        return role;
    }

    @Override
    public @Nullable PrismObject<UserType> cacheUserTypeObject(
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull String userOid,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable RoleAnalysisCacheOption option) {
        PrismObject<UserType> user = userExistCache.get(userOid);
        if (user != null) {
            return user;
        }

        user = getUserTypeObject(userOid, task, result);
        if (user == null) {
            return null;
        }

        if (option != null) {
            try {
                PrismObject<UserType> cacheUser = new UserType().asPrismObject();
                List<RoleAnalysisAttributeDef> itemDef = option.getItemDef();
                for (RoleAnalysisAttributeDef roleAnalysisAttributeDef : itemDef) {
                    ItemPath path = roleAnalysisAttributeDef.getPath();
                    boolean isContainer = roleAnalysisAttributeDef.isContainer();

                    if (isContainer) {
                        PrismContainer<Containerable> container = user.findContainer(path);
                        if (container != null) {
                            cacheUser.add(container.clone());
                        }
                    } else {
                        Item<PrismValue, ItemDefinition<?>> property = user.findItem(path);
                        if (property != null) {
                            cacheUser.add(property.clone());
                        }
                    }
                }
                userExistCache.put(userOid, cacheUser);
                return cacheUser;
            } catch (SchemaException e) {
                throw new SystemException("Couldn't prepare user for cache", e);
            }
        }

        userExistCache.put(userOid, user);

        return user;
    }

    @Override
    public @NotNull Integer countUserTypeMembers(
            @Nullable ObjectFilter userFilter,
            @NotNull String objectId,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .exists(F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(objectId)
                .endBlock().build();

        if (userFilter != null) {
            query.addFilter(userFilter);
        }
        try {
            return modelService.countObjects(UserType.class, query, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Failed to search role member objects:", ex);
        } finally {
            result.recomputeStatus();
        }

        return 0;
    }

    @Override
    public void deleteSession(
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            PrismObject<RoleAnalysisSessionType> prismSession = this.getSessionTypeObject(sessionOid, task, result);
            if (prismSession == null) {
                return;
            }

            deleteSessionClustersMembers(sessionOid, task, result, false);

            ObjectDelta<AssignmentHolderType> deleteDelta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(AssignmentHolderType.class, sessionOid);

            modelService.executeChanges(singleton(deleteDelta), null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't delete RoleAnalysisSessionType {}", sessionOid, e);
        }
    }

    @Override
    public void deleteOutlier(
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull Task task,
            @NotNull OperationResult result) {
        String outlierOid = outlier.getOid();
        try {
            ObjectDelta<RoleAnalysisOutlierType> deleteDelta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(RoleAnalysisOutlierType.class, outlierOid);

            modelService.executeChanges(singleton(deleteDelta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't delete RoleAnalysisOutlierType {}", outlierOid, e);
        }
    }

    @Override
    public RoleAnalysisOptionType resolveClusterOptionType(
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisClusterType clusterObject = cluster.asObjectable();
        ObjectReferenceType roleAnalysisSessionRef = clusterObject.getRoleAnalysisSessionRef();
        String sessionRefOid = roleAnalysisSessionRef.getOid();

        PrismObject<RoleAnalysisSessionType> session = getSessionTypeObject(
                sessionRefOid, task, result);

        if (session == null) {
            LOGGER.error("Failed to resolve processMode from RoleAnalysisSession object: {}", sessionRefOid);
            return null;
        }

        RoleAnalysisSessionType sessionObject = session.asObjectable();
        return sessionObject.getAnalysisOption();
    }

    @Override
    public void recomputeClusterDetectionOptions(
            @NotNull String clusterOid,
            @NotNull PatternDetectionOption detectionOption,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisDetectionOptionType roleAnalysisDetectionOptionType = new RoleAnalysisDetectionOptionType();
        roleAnalysisDetectionOptionType.setFrequencyRange(new RangeType()
                .max(detectionOption.getMaxFrequencyThreshold())
                .min(detectionOption.getMinFrequencyThreshold()));
        roleAnalysisDetectionOptionType.setMinUserOccupancy(detectionOption.getMinUsers());
        roleAnalysisDetectionOptionType.setMinRolesOccupancy(detectionOption.getMinRoles());

        try {

            ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTION_OPTION)
                    .replace(roleAnalysisDetectionOptionType.asPrismContainerValue())
                    .asObjectDelta(clusterOid);

            modelService.executeChanges(singleton(delta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't recompute RoleAnalysisClusterDetectionOptions {}", clusterOid, e);
            result.recordPartialError(e);
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    @Override
    public @NotNull MiningOperationChunk prepareCompressedMiningStructure(
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task) {
        return new CompressedMiningStructure().executeOperation(this, cluster,
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                fullProcess, processMode, result, task);
    }

    @Override
    public MiningOperationChunk prepareBasicChunkStructure(
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull DisplayValueOption option,
            @NotNull RoleAnalysisProcessModeType processMode,
            @Nullable List<DetectedPattern> detectedPatterns,
            @NotNull OperationResult result,
            @NotNull Task task) {
        RoleAnalysisChunkMode chunkMode = option.getChunkMode();
        List<MiningUserTypeChunk> miningUserTypeChunks;
        List<MiningRoleTypeChunk> miningRoleTypeChunks;

        MiningOperationChunk chunk;
        switch (chunkMode) {
            case EXPAND_ROLE -> {
                miningRoleTypeChunks = new ExpandedMiningStructure()
                        .executeOperation(this, cluster,
                                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                                true, processMode, result, task, option)
                        .getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);
                miningUserTypeChunks = new CompressedMiningStructure()
                        .executeOperation(this, cluster,
                                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                                true, processMode, result, task)
                        .getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
                chunk = new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
            }
            case EXPAND_USER -> {
                miningRoleTypeChunks = new CompressedMiningStructure()
                        .executeOperation(this, cluster,
                                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                                true, processMode, result, task)
                        .getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);
                miningUserTypeChunks = new ExpandedMiningStructure()
                        .executeOperation(this, cluster,
                                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                                true, processMode, result, task, option)
                        .getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
                chunk = new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
            }
            case COMPRESS -> {
                chunk = new CompressedMiningStructure()
                        .executeOperation(this, cluster,
                                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                                true, processMode, result, task);
            }
            default -> {
                chunk = new ExpandedMiningStructure()
                        .executeOperation(this, cluster,
                                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                                true, processMode, result, task, option);
            }
        }
        chunk.setProcessMode(processMode);
        RoleAnalysisDetectionOptionType detectionOption = cluster.getDetectionOption();
        RangeType frequencyRange = detectionOption.getFrequencyRange();

        if (frequencyRange != null) {
            chunk.setMinFrequency(frequencyRange.getMin() / 100);
            chunk.setMaxFrequency(frequencyRange.getMax() / 100);
        }

        //TODO think about it
        if (detectedPatterns != null && !detectedPatterns.isEmpty()) {
            updateChunkWithPatterns(chunk, processMode, detectedPatterns, task, result);
        }

        chunk.setSortMode(option.getSortMode());
        return chunk;
    }

    @Override
    public @NotNull MiningOperationChunk prepareMiningStructure(
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull DisplayValueOption option,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull List<DetectedPattern> detectedPatterns,
            @NotNull OperationResult result,
            @NotNull Task task) {

        MiningOperationChunk basicChunk = prepareBasicChunkStructure(cluster,
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                option, processMode, detectedPatterns, result, task);

        RoleAnalysisDetectionOptionType detectionOption = cluster.getDetectionOption();
        RangeType frequencyRange = detectionOption.getStandardDeviation();
        Double frequencyThreshold = detectionOption.getFrequencyThreshold();
        Double sensitivity = detectionOption.getSensitivity();
        resolveOutliersZScore(basicChunk.getMiningRoleTypeChunks(), frequencyRange, sensitivity, frequencyThreshold);

        return basicChunk;
    }

    @Override
    public void updateChunkWithPatterns(@NotNull MiningOperationChunk basicChunk,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull List<DetectedPattern> detectedPatterns,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<MiningRoleTypeChunk> miningRoleTypeChunks = basicChunk.getMiningRoleTypeChunks();//basicChunk.getMiningRoleTypeChunks(option.getSortMode());
        List<MiningUserTypeChunk> miningUserTypeChunks = basicChunk.getMiningUserTypeChunks();

        List<List<String>> detectedPatternsRoles = new ArrayList<>();
        List<List<String>> detectedPatternsUsers = new ArrayList<>();
        List<String> candidateRolesIds = new ArrayList<>();

        for (DetectedPattern detectedPattern : detectedPatterns) {
            boolean isOutlierPattern = detectedPattern.getPatternType() == BasePattern.PatternType.OUTLIER;
            if (isOutlierPattern) {
                return;
            }

            detectedPatternsRoles.add(new ArrayList<>(detectedPattern.getRoles()));
            detectedPatternsUsers.add(new ArrayList<>(detectedPattern.getUsers()));
            candidateRolesIds.add(detectedPattern.getIdentifier());
        }

        resolveTablePatternChunk(processMode, basicChunk, miningRoleTypeChunks, detectedPatternsRoles, candidateRolesIds, miningUserTypeChunks, detectedPatternsUsers);

        int size = detectedPatternsUsers.size();

        IntStream.range(0, size).forEach(i -> {
            List<String> detectedPatternRoles = detectedPatternsRoles.get(i);
            List<String> detectedPatternUsers = detectedPatternsUsers.get(i);
            String candidateRoleId = candidateRolesIds.get(i);
            addAdditionalObject(this, candidateRoleId, detectedPatternUsers, detectedPatternRoles,
                    miningUserTypeChunks,
                    miningRoleTypeChunks,
                    task, result);
        });
    }

    @Override
    public @NotNull MiningOperationChunk prepareExpandedMiningStructure(
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task,
            @Nullable DisplayValueOption option) {
        return new ExpandedMiningStructure().executeOperation(this, cluster,
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                fullProcess, processMode, result, task, option);
    }

    @Override
    public void clusterObjectMigrationRecompute(
            @NotNull String clusterRefOid,
            @NotNull String roleRefOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        PrismObject<RoleAnalysisClusterType> cluster = getClusterTypeObject(clusterRefOid,
                task, result);
        if (cluster == null) {
            LOGGER.error("Failed to resolve RoleAnalysisCluster OBJECT from UUID: {}", clusterRefOid);
            return;
        }
        RoleAnalysisClusterType clusterObject = cluster.asObjectable();

        ItemName fClusterUserBasedStatistic = RoleAnalysisClusterType.F_CLUSTER_STATISTICS;
        RoleAnalysisProcessModeType processMode = resolveClusterOptionType(cluster, task, result).getProcessMode();

        if (processMode == null) {
            LOGGER.error("Failed to resolve processMode from RoleAnalysisCluster object: {}", clusterRefOid);
            return;
        }

        ItemName fMember = processMode.equals(RoleAnalysisProcessModeType.ROLE)
                ? AnalysisClusterStatisticType.F_ROLES_COUNT
                : AnalysisClusterStatisticType.F_USERS_COUNT;

        Integer memberCount = processMode.equals(RoleAnalysisProcessModeType.ROLE)
                ? clusterObject.getClusterStatistics().getRolesCount()
                : clusterObject.getClusterStatistics().getUsersCount();

        PrismObject<RoleType> object = getRoleTypeObject(roleRefOid, task, result);
        if (object == null) {
            return;
        }

        ObjectReferenceType objectRef = new ObjectReferenceType()
                .oid(roleRefOid)
                .type(RoleType.COMPLEX_TYPE);

        try {

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_RESOLVED_PATTERN).add(objectRef)
                    .asItemDelta());

            objectRef = new ObjectReferenceType();
            objectRef.setOid(object.getOid());
            objectRef.setType(RoleType.COMPLEX_TYPE);

            if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                        .item(RoleAnalysisClusterType.F_MEMBER).add(objectRef)
                        .asItemDelta());

                modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                        .item(fClusterUserBasedStatistic, fMember).replace(memberCount + 1)
                        .asItemDelta());
            }

            repositoryService.modifyObject(RoleAnalysisClusterType.class, clusterRefOid, modifications, result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            LOGGER.error("Couldn't execute migration recompute RoleAnalysisClusterDetectionOptions {}", clusterRefOid, e);
        }
    }

    @Override
    public void executeClusteringTask(
            @NotNull ModelInteractionService modelInteractionService,
            @NotNull PrismObject<RoleAnalysisSessionType> session,
            @NotNull Task task,
            @NotNull OperationResult result) {

        String state = recomputeAndResolveSessionOpStatus(session, result, task);

        if (!RoleAnalysisObjectState.isStable(state)) {
            result.recordWarning("Couldn't start clustering. Some process is already in progress.");
            LOGGER.warn("Couldn't start clustering. Some process is already in progress.: " + session.getOid());
            return;
        }

        this.updateSessionMarkRef(session, result, task);

        try {
            ObjectReferenceType objectReferenceType = new ObjectReferenceType()
                    .oid(session.getOid())
                    .type(RoleAnalysisSessionType.COMPLEX_TYPE)
                    .targetName(String.valueOf(session.getName()));

            RoleAnalysisClusteringWorkDefinitionType rdw = new RoleAnalysisClusteringWorkDefinitionType();
            rdw.setSessionRef(objectReferenceType);

            ActivityDefinitionType activity = new ActivityDefinitionType()
                    .work(new WorkDefinitionsType()
                            .roleAnalysisClustering(rdw));

            TaskType processingTask = new TaskType();
            processingTask.setName(PolyStringType.fromOrig("Session clustering  (" + session + ")"));

            String taskOid = UUID.randomUUID().toString(); //TODO is this really needed here?
            processingTask.setOid(taskOid);

            modelInteractionService.submit(
                    activity,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(processingTask)
                            .withArchetypes(
                                    SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()),
                    task, result);

            MidPointPrincipal user = AuthUtil.getPrincipalUser();
            FocusType focus = user.getFocus();
            submitSessionOperationStatus(modelService,
                    session,
                    taskOid,
                    focus, LOGGER, task, result
            );

        } catch (CommonException e) {
            LOGGER.error("Couldn't execute clustering task for session {}", session, e);
        }
    }

    @Override
    public void executeDetectionTask(
            @NotNull ModelInteractionService modelInteractionService, @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result,
            String state) {

        if (!RoleAnalysisObjectState.isStable(state)) {
            result.recordWarning("Couldn't start detection. Some process is already in progress.");
            LOGGER.warn("Couldn't start detection. Some process is already in progress.: " + cluster.getOid());
            return;
        }

        try {
            ObjectReferenceType objectReferenceType = new ObjectReferenceType()
                    .type(RoleAnalysisClusterType.COMPLEX_TYPE)
                    .oid(cluster.getOid());

            RoleAnalysisPatternDetectionWorkDefinitionType rdw = new RoleAnalysisPatternDetectionWorkDefinitionType();
            rdw.setClusterRef(objectReferenceType);

            ActivityDefinitionType activity = new ActivityDefinitionType()
                    .work(new WorkDefinitionsType()
                            .roleAnalysisPatternDetection(rdw));

            TaskType taskObject = new TaskType();

            taskObject.setName(Objects.requireNonNullElseGet(
                    taskName, () -> PolyStringType.fromOrig("Pattern detection  (" + cluster + ")")));

            if (taskOid != null) {
                taskObject.setOid(taskOid);
            } else {
                taskOid = UUID.randomUUID().toString();
                taskObject.setOid(taskOid);
            }

            modelInteractionService.submit(
                    activity,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(taskObject)
                            .withArchetypes(
                                    SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()),
                    task, result);

            MidPointPrincipal user = AuthUtil.getPrincipalUser();
            FocusType focus = user.getFocus();

            submitClusterOperationStatus(modelService,
                    cluster,
                    taskOid,
                    RoleAnalysisOperation.DETECTION,
                    focus,
                    LOGGER,
                    task,
                    result
            );

        } catch (CommonException e) {
            LOGGER.error("Couldn't execute Cluster Detection Task {}", cluster, e);
            result.recordPartialError(e);
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    @Override
    public void executeRoleAnalysisRoleMigrationTask(
            @NotNull ModelInteractionService modelInteractionService,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull ActivityDefinitionType activityDefinition,
            @NotNull PrismObject<RoleType> roleObject,
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result) {

        String state = recomputeAndResolveClusterOpStatus(cluster.getOid(), result, task, true, modelInteractionService);

        if (!RoleAnalysisObjectState.isStable(state)) {
            result.recordWarning("Couldn't start migration. Some process is already in progress.");
            LOGGER.warn("Couldn't start migration. Some process is already in progress.: {}", cluster.getOid());
            return;
        }

        TaskType taskObject = new TaskType();
        // Copy channel from task, this allows GUI to set user channel (scripting could have different channel)
        taskObject.channel(task.getChannel());
        String roleIdentifier;
        if (roleObject.getName() != null) {
            roleIdentifier = roleObject.getName().toString();
        } else {
            roleIdentifier = roleObject.getOid();
        }
        taskObject.setName(PolyStringType.fromOrig("Role migration: " + roleIdentifier));

        if (taskOid != null) {
            taskObject.setOid(taskOid);
        } else {
            taskOid = UUID.randomUUID().toString();
            taskObject.setOid(taskOid);
        }

        executeBusinessRoleMigrationTask(modelInteractionService, activityDefinition, task, result, taskObject);

        MidPointPrincipal user = AuthUtil.getPrincipalUser();
        FocusType focus = user.getFocus();

        submitClusterOperationStatus(modelService,
                cluster,
                taskOid,
                RoleAnalysisOperation.MIGRATION, focus, LOGGER, task, result
        );

        switchRoleToActiveLifeState(modelService, roleObject, LOGGER, task, result);
        cleanClusterDetectedPatterns(repositoryService, cluster, LOGGER, result);
    }

    @Override
    public void executeRoleMigrationProcess(
            @NotNull ModelInteractionService modelInteractionService,
            @NotNull PrismObject<RoleType> roleObject,
            @NotNull Task task,
            @NotNull OperationResult result) {

        TaskType taskObject = new TaskType();

        String roleIdentifier;
        if (roleObject.getName() != null) {
            roleIdentifier = roleObject.getName().toString();
        } else {
            roleIdentifier = roleObject.getOid();
        }

        taskObject.setName(PolyStringType.fromOrig("Role migration: " + roleIdentifier));
        taskObject.setOid(UUID.randomUUID().toString());
        // Copy channel from task which executed this code, this ensures that channel is kept correctly.
        taskObject.setChannel(task.getChannel());
        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setType(RoleType.COMPLEX_TYPE);
        objectReferenceType.setOid(roleObject.getOid());

        List<FocusType> roleMembers = extractRoleMembers(null, roleObject.asObjectable(), task, result);
        ActivityDefinitionType activityDefinition = createMigrationActivity(roleMembers, roleObject.getOid(), LOGGER, result);
        if (activityDefinition == null) {
            return;
        }
        executeBusinessRoleMigrationTask(modelInteractionService, activityDefinition, task, result, taskObject);

        switchRoleToActiveLifeState(modelService, roleObject, LOGGER, task, result);
    }

    public @NotNull String recomputeAndResolveClusterOpStatus(
            @NotNull String clusterOid,
            @NotNull OperationResult result,
            @NotNull Task task,
            boolean onlyStatusUpdate,
            @Nullable ModelInteractionService modelInteractionService) {

        PrismObject<RoleAnalysisClusterType> clusterPrism = getClusterTypeObject(clusterOid, task, result);

        if (clusterPrism == null) {
            return RoleAnalysisObjectState.STABLE.getDisplayString();
        }

        RoleAnalysisClusterType cluster = clusterPrism.asObjectable();
        List<RoleAnalysisOperationStatus> operationStatus = cluster.getOperationStatus();

        if (operationStatus == null || operationStatus.isEmpty()) {
            return RoleAnalysisObjectState.STABLE.getDisplayString();
        }
        String stateString = RoleAnalysisObjectState.STABLE.getDisplayString();

        boolean requestUpdatePattern = false;
        boolean requestUpdateOp = false;

        for (RoleAnalysisOperationStatus roleAnalysisOperationStatus : operationStatus) {
            Collection<PrismContainerValue<?>> collection = new ArrayList<>();

            RoleAnalysisOperationStatus newStatus = updateRoleAnalysisOperationStatus(
                    repositoryService, roleAnalysisOperationStatus, false, LOGGER, result
            );

            if (newStatus != null) {
                collection.add(newStatus.clone().asPrismContainerValue().clone());
                requestUpdateOp = true;
                OperationResultStatusType progressStatus = newStatus.getStatus();
                if (progressStatus != null && progressStatus.equals(OperationResultStatusType.IN_PROGRESS)) {
                    stateString = RoleAnalysisObjectState.PROCESSING.getDisplayString();
                } else if (progressStatus != null && progressStatus.equals(OperationResultStatusType.SUCCESS)) {
                    if (newStatus.getOperationChannel().equals(RoleAnalysisOperation.MIGRATION)) {
                        requestUpdatePattern = true;
                    }
                }

            } else {
                collection.add(roleAnalysisOperationStatus.clone().asPrismContainerValue().clone());
                OperationResultStatusType progressStatus = roleAnalysisOperationStatus.getStatus();
                if (progressStatus != null && progressStatus.equals(OperationResultStatusType.IN_PROGRESS)) {
                    stateString = RoleAnalysisObjectState.PROCESSING.getDisplayString();
                }
            }

            if (requestUpdateOp && !collection.isEmpty()) {
                try {

                    //TODO change status handling (temporary - there might be just one container but this is not correct solution)
                    //USING replace in one executeChange cause reset container error (500)
                    var candidateRoleToDelete = new RoleAnalysisCandidateRoleType().id(roleAnalysisOperationStatus.getId());
                    ObjectDelta<RoleAnalysisClusterType> deltaClear = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                            .item(RoleAnalysisClusterType.F_OPERATION_STATUS).delete(candidateRoleToDelete)
                            .asObjectDelta(clusterOid);
                    Collection<ObjectDelta<? extends ObjectType>> deltasClear = MiscSchemaUtil.createCollection(deltaClear);
                    modelService.executeChanges(deltasClear, null, task, result);

                    ObjectDelta<RoleAnalysisClusterType> deltaAdd = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                            .item(RoleAnalysisClusterType.F_OPERATION_STATUS).add(CloneUtil.cloneCollectionMembers(collection))
                            .asObjectDelta(clusterOid);

                    Collection<ObjectDelta<? extends ObjectType>> deltasAdd = MiscSchemaUtil.createCollection(deltaAdd);

                    modelService.executeChanges(deltasAdd, null, task, result);

                } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException |
                        ExpressionEvaluationException |
                        CommunicationException | ConfigurationException | PolicyViolationException |
                        SecurityViolationException e) {
                    LOGGER.warn("Couldn't modify RoleAnalysisClusterType {}", cluster.getOid(), e);
                }
            }

        }

        if (requestUpdatePattern && !onlyStatusUpdate && modelInteractionService != null) {
//                updateClusterPatterns(cluster.getOid(), task, result);
            this.executeDetectionTask(modelInteractionService, cluster.asPrismObject(), null,
                    null, task, result, stateString);
        }

        return stateString;
    }

    public @NotNull String recomputeAndResolveSessionOpStatus(
            @NotNull PrismObject<RoleAnalysisSessionType> sessionPrismObject,
            @NotNull OperationResult result,
            @NotNull Task task) {

        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(sessionPrismObject.getOid(), task, result);
        if (sessionTypeObject == null) {
            return RoleAnalysisObjectState.STABLE.getDisplayString();
        }

        RoleAnalysisSessionType session = sessionTypeObject.asObjectable();

        RoleAnalysisOperationStatus operationStatus = session.getOperationStatus();
        if (operationStatus == null) {
            return RoleAnalysisObjectState.STABLE.getDisplayString();
        }

        RoleAnalysisOperationStatus newStatus = updateRoleAnalysisOperationStatus(repositoryService, operationStatus, true, LOGGER, result);

        if (newStatus != null) {

            try {
                ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                        .item(RoleAnalysisSessionType.F_OPERATION_STATUS).replace(newStatus.clone())
                        .asObjectDelta(session.getOid());

                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

                modelService.executeChanges(deltas, null, task, result);

            } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException |
                    ExpressionEvaluationException |
                    CommunicationException | ConfigurationException | PolicyViolationException |
                    SecurityViolationException e) {
                LOGGER.error("Couldn't modify RoleAnalysisSessionType {}", session.getOid(), e);
            }

            return newStatus.getMessage();

        }

        return operationStatus.getMessage();

    }

    public @NotNull String recomputeAndResolveClusterCandidateRoleOpStatus(
            @NotNull PrismObject<RoleAnalysisClusterType> clusterPrismObject,
            @NotNull RoleAnalysisCandidateRoleType candidateRole,
            @NotNull OperationResult result, Task task) {
        RoleAnalysisOperationStatus operationStatus = candidateRole.getOperationStatus();

        if (operationStatus == null) {
            return RoleAnalysisObjectState.STABLE.getDisplayString();
        }

        ObjectReferenceType taskRef = operationStatus.getTaskRef();
        String stateString = operationStatus.getMessage();
        RoleAnalysisOperation operationChannel = operationStatus.getOperationChannel();
        PrismObject<TaskType> object = null;

        boolean taskExist = true;

        if (taskRef != null && taskRef.getOid() != null) {
            try {
                object = repositoryService.getObject(TaskType.class, taskRef.getOid(), null, result);
            } catch (ObjectNotFoundException | SchemaException e) {
                LOGGER.warn("Error retrieving TaskType object for oid: {}", taskRef.getOid(), e);
                taskExist = false;
            }

            if (!taskExist) {
                if (stateString != null && !stateString.isEmpty()) {
                    return stateString;
                } else {
                    return RoleAnalysisObjectState.STABLE.getDisplayString();
                }
            }

            TaskType taskObject = object.asObjectable();
            OperationResultStatusType resultStatus = taskObject.getResultStatus();

            stateString = updateClusterStateMessage(taskObject);

            if (resultStatus != null) {
                setCandidateRoleOpStatus(clusterPrismObject, candidateRole, object.getOid(),
                        resultStatus, stateString, result, task, operationChannel, null);
            }

        }

        return stateString == null || stateString.isEmpty() ? RoleAnalysisObjectState.STABLE.getDisplayString() : stateString;
    }

    @Override
    public int[] getTaskProgressIfExist(
            @Nullable RoleAnalysisOperationStatus operationStatus,
            @NotNull OperationResult result) {
        if (operationStatus == null) {
            return new int[0];
        }

        ObjectReferenceType taskRef = operationStatus.getTaskRef();

        if (taskRef == null || taskRef.getOid() == null) {
            return new int[] { 0, 0 };
        }

        PrismObject<TaskType> object;
        try {
            object = repositoryService.getObject(TaskType.class, taskRef.getOid(), null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            return new int[] { 0, 0 };
        }

        TaskType taskObject = object.asObjectable();

        TaskExecutionStateType executionState = taskObject.getExecutionState();

        TaskActivityStateType activityState = taskObject.getActivityState();
        Integer expectedTotal = 0;
        if (activityState != null
                && activityState.getActivity() != null
                && activityState.getActivity().getProgress() != null) {
            expectedTotal = activityState.getActivity().getProgress().getExpectedTotal();
            if (expectedTotal == null) {
                expectedTotal = 0;
            }
        }
        Long actual = 0L;
        if (taskObject.getProgress() != null) {
            actual = taskObject.getProgress();
            if (executionState == null) {
                actual = 0L;
            }
        }

        return new int[] { expectedTotal, actual.intValue() };
    }

    public void setCandidateRoleOpStatus(
            @NotNull PrismObject<RoleAnalysisClusterType> clusterPrism,
            @NotNull RoleAnalysisCandidateRoleType candidateRoleContainer,
            @NotNull String taskOid,
            @Nullable OperationResultStatusType operationResultStatusType,
            @Nullable String message,
            @NotNull OperationResult result,
            @NotNull Task task,
            @NotNull RoleAnalysisOperation operationType,
            @Nullable FocusType focus) {

        XMLGregorianCalendar createTimestamp = null;
        RoleAnalysisOperationStatus opsOld = candidateRoleContainer.getOperationStatus();
        if (opsOld != null
                && opsOld.getStatus() != null
                && opsOld.getMessage() != null
                && opsOld.getTaskRef() != null) {
            String oldTaskOid = opsOld.getTaskRef().getOid();
            OperationResultStatusType oldStatus = opsOld.getStatus();
            String oldMessage = opsOld.getMessage();
            createTimestamp = opsOld.getCreateTimestamp();

            if (oldTaskOid.equals(taskOid)
                    && oldStatus.equals(operationResultStatusType)
                    && oldMessage.equals(message)) {
                return;
            }
        }

        @NotNull RoleAnalysisOperationStatus operationStatus = buildOpExecution(
                taskOid, operationResultStatusType, message, operationType, createTimestamp, focus);
        try {
            ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_CANDIDATE_ROLES.append(
                            candidateRoleContainer.getId(), RoleAnalysisCandidateRoleType.F_OPERATION_STATUS))
                    .replace(operationStatus.clone())
                    .asObjectDelta(clusterPrism.getOid());

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

            modelService.executeChanges(deltas, null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't modify RoleAnalysisClusterType {}", clusterPrism.getOid(), e);
        }
    }

    @Override
    public void addCandidateRole(
            @NotNull String clusterRefOid,
            @NotNull RoleAnalysisCandidateRoleType candidateRole,
            @NotNull Task task,
            @NotNull OperationResult result) {

        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();
            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_CANDIDATE_ROLES).add(candidateRole.clone())
                    .asItemDelta());

            repositoryService.modifyObject(RoleAnalysisClusterType.class, clusterRefOid, modifications, result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            LOGGER.error("Couldn't update detection pattern {}", clusterRefOid, e);
        }

    }

    public void deleteSingleCandidateRole(
            @NotNull PrismObject<RoleAnalysisClusterType> clusterPrism,
            @NotNull RoleAnalysisCandidateRoleType candidateRoleBean,
            @NotNull OperationResult result, Task task) {

        try {
            var candidateRoleToDelete = new RoleAnalysisCandidateRoleType().id(candidateRoleBean.getId());
            ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_CANDIDATE_ROLES)
                    .delete(candidateRoleToDelete)
                    .asObjectDelta(clusterPrism.getOid());

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

            modelService.executeChanges(deltas, null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't delete candidate role container {}", clusterPrism.getOid(), e);
        }
    }

    public void executeChangesOnCandidateRole(@NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull RoleAnalysisCandidateRoleType roleAnalysisCandidateRoleType,
            @NotNull Set<PrismObject<UserType>> members,
            @NotNull Set<AssignmentType> inducements,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ObjectReferenceType candidateRoleRef = roleAnalysisCandidateRoleType.getCandidateRoleRef();

        PrismObject<RoleType> roleTypeObject = getRoleTypeObject(candidateRoleRef.getOid(), task, result);
        if (roleTypeObject == null) {
            LOGGER.error("Couldn't get candidate role object{}", candidateRoleRef.getOid());
            return;
        }

        Set<String> inducementsOid = inducements.stream().map(AssignmentType::getTargetRef)
                .filter(Objects::nonNull)
                .map(AbstractReferencable::getOid)
                .collect(Collectors.toSet());

        Collection<PrismReferenceValue> memberscCollection = new ArrayList<>();
        for (PrismObject<UserType> member : members) {
            ObjectReferenceType objectReferenceType = new ObjectReferenceType()
                    .oid(member.getOid())
                    .type(UserType.COMPLEX_TYPE);
            memberscCollection.add(objectReferenceType.asReferenceValue());
        }

        RoleType role = roleTypeObject.asObjectable();
        List<AssignmentType> inducement = role.getInducement();
        Set<String> unassignedRoles = new HashSet<>();
        for (AssignmentType assignmentType : inducement) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                QName type = targetRef.getType();
                if (type != null && type.equals(RoleType.COMPLEX_TYPE)) {

                    String oid = targetRef.getOid();
                    if (!inducementsOid.contains(oid)) {
                        unassignedRoles.add(oid);
                    } else {
                        inducementsOid.remove(oid);
                    }

                }
            }
        }

        try {
            String candidateRoleOid = roleTypeObject.getOid();

            for (String inducementForAdd : inducementsOid) {
                ObjectDelta<RoleType> roleD = PrismContext.get().deltaFor(RoleType.class)
                        .item(AbstractRoleType.F_INDUCEMENT)
                        .add(getAssignmentTo(inducementForAdd))
                        .asObjectDelta(candidateRoleOid);
                Collection<ObjectDelta<? extends ObjectType>> deltas2 = MiscSchemaUtil.createCollection(roleD);
                modelService.executeChanges(deltas2, null, task, result);

            }

            for (String unassignedRole : unassignedRoles) {
                ObjectDelta<RoleType> roleD = PrismContext.get().deltaFor(RoleType.class)
                        .item(AbstractRoleType.F_INDUCEMENT)
                        .delete(getAssignmentTo(unassignedRole))
                        .asObjectDelta(candidateRoleOid);
                Collection<ObjectDelta<? extends ObjectType>> deltas2 = MiscSchemaUtil.createCollection(roleD);
                modelService.executeChanges(deltas2, null, task, result);
            }

            Long id = roleAnalysisCandidateRoleType.getId();
            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
            ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_CANDIDATE_ROLES.append(id), RoleAnalysisCandidateRoleType.F_CANDIDATE_MEMBERS)
                    .replace(memberscCollection)
                    .asObjectDelta(cluster.getOid());

            deltas.add(delta);
            modelService.executeChanges(deltas, null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't modify candidate role container {}", cluster.getOid(), e);
        }
    }

    public <T extends ObjectType> void loadSearchObjectIterative(
            @NotNull ModelService modelService,
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull List<T> modifyList,
            @NotNull Task task,
            @NotNull OperationResult parentResult) {
        try {
            Set<String> existingOidSet = modifyList.stream()
                    .map(ObjectType::getOid)
                    .collect(Collectors.toSet());

            ResultHandler<RoleType> resultHandler = (role, lResult) -> {
                try {
                    if (!existingOidSet.contains(role.getOid())) {
                        //noinspection unchecked
                        modifyList.add((T) role.asObjectable());
                    }
                } catch (Exception e) {
                    String errorMessage = "Cannot resolve role: " + toShortString(role.asObjectable())
                            + ": " + e.getMessage();
                    throw new SystemException(errorMessage, e);
                }

                return true;
            };

            modelService.searchObjectsIterative(RoleType.class, query, resultHandler, null,
                    task, parentResult);

        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Couldn't search  search and load object iterative {}", type, e);
        }
    }

    //NOTE used only outside session build process. Inside session build process is used userTypeAttributeAnalysisCached
    @Override
    public List<AttributeAnalysisStructure> userTypeAttributeAnalysis(
            @NotNull Set<PrismObject<UserType>> prismUsers,
            Double membershipDensity,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet) {
        List<AttributeAnalysisStructure> attributeAnalysisStructures = new ArrayList<>();
        runUserAttributeAnalysis(this, prismUsers, attributeAnalysisStructures, task, result, attributeDefSet);
        return attributeAnalysisStructures;
    }

    @Override
    public List<AttributeAnalysisStructure> userTypeAttributeAnalysisCached(
            @NotNull Set<PrismObject<UserType>> prismUsers,
            Double membershipDensity,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet, @NotNull Task task,
            @NotNull OperationResult result) {
        List<AttributeAnalysisStructure> attributeAnalysisStructures = new ArrayList<>();
        runUserAttributeAnalysisCached(this, prismUsers, attributeAnalysisStructures,
                userAnalysisCache, task, result, attributeDefSet);
        return attributeAnalysisStructures;
    }

    @Override
    public List<AttributeAnalysisStructure> roleTypeAttributeAnalysis(
            @NotNull Set<PrismObject<RoleType>> prismRoles,
            Double membershipDensity,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeRoleDefSet) {
        List<AttributeAnalysisStructure> attributeAnalysisStructures = new ArrayList<>();

        runRoleAttributeAnalysis(this, prismRoles, attributeAnalysisStructures, task, result, attributeRoleDefSet);
        return attributeAnalysisStructures;
    }

    @Override
    public List<AttributeAnalysisStructure> roleMembersAttributeAnalysis(
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet,
            @NotNull String objectOid,
            @NotNull Task task,
            @NotNull OperationResult result) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        this.extractUserTypeMembers(
                userExistCache, null,
                new HashSet<>(singleton(objectOid)),
                task, result);

        Set<PrismObject<UserType>> prismUsers = new HashSet<>(userExistCache.values());
        userExistCache.clear();

        return userTypeAttributeAnalysis(prismUsers, 100.0, task, result, attributeDefSet);
    }

    @Override
    public List<AttributeAnalysisStructure> userRolesAttributeAnalysis(
            @NotNull List<RoleAnalysisAttributeDef> attributeRoleDefSet,
            @NotNull String objectOid,
            @NotNull Task task,
            @NotNull OperationResult result) {

        PrismObject<UserType> userTypeObject = getUserTypeObject(objectOid, task, result);
        if (userTypeObject == null) {
            return Collections.emptyList();
        }
        List<String> rolesOidAssignment = getRolesOidAssignment(userTypeObject.asObjectable());
        //TODO wouldn't search with inOid filter be more efficient?
        Set<PrismObject<RoleType>> prismRolesSet = fetchPrismRoles(this, new HashSet<>(rolesOidAssignment), task, result);
        return roleTypeAttributeAnalysis(prismRolesSet, 100.0, task, result, attributeRoleDefSet);
    }

    public void resolveDetectedPatternsAttributes(
            @NotNull List<RoleAnalysisDetectionPatternType> detectedPatterns,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @Nullable List<RoleAnalysisAttributeDef> attributeRoleDefSet,
            @Nullable List<RoleAnalysisAttributeDef> attributeUserDefSet) {

        for (RoleAnalysisDetectionPatternType detectedPattern : detectedPatterns) {

            List<ObjectReferenceType> userOccupancy = detectedPattern.getUserOccupancy();
            List<ObjectReferenceType> roleOccupancy = detectedPattern.getRolesOccupancy();
            Set<PrismObject<UserType>> users;
            Set<PrismObject<RoleType>> roles;

            users = userOccupancy.stream().map(objectReferenceType -> this
                            .cacheUserTypeObject(userExistCache, objectReferenceType.getOid(), task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());

            roles = roleOccupancy.stream().map(objectReferenceType -> this
                            .cacheRoleTypeObject(roleExistCache, objectReferenceType.getOid(), task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());

            List<AttributeAnalysisStructure> userAttributeAnalysisStructures = null;
            if (attributeUserDefSet != null) {
                userAttributeAnalysisStructures = this
                        .userTypeAttributeAnalysis(users, 100.0, task, result, attributeUserDefSet);
            }

            List<AttributeAnalysisStructure> roleAttributeAnalysisStructures = null;
            if (attributeRoleDefSet != null) {
                roleAttributeAnalysisStructures = this
                        .roleTypeAttributeAnalysis(roles, 100.0, task, result, attributeRoleDefSet);
            }

            double totalDensity = 0.0;
            int totalCount = 0;

            if (userAttributeAnalysisStructures != null) {
                RoleAnalysisAttributeAnalysisResult userAnalysis = new RoleAnalysisAttributeAnalysisResult();
                for (AttributeAnalysisStructure userAttributeAnalysisStructure : userAttributeAnalysisStructures) {
                    double density = userAttributeAnalysisStructure.getDensity();
                    if (density == 0) {
                        continue;
                    }

                    totalDensity += density;

                    RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = userAttributeAnalysisStructure
                            .buildRoleAnalysisAttributeAnalysisContainer();

                    userAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
                }

                detectedPattern.setUserAttributeAnalysisResult(userAnalysis);
                totalCount += userAnalysis.getAttributeAnalysis().size();
            }

            if (roleAttributeAnalysisStructures != null) {
                RoleAnalysisAttributeAnalysisResult roleAnalysis = new RoleAnalysisAttributeAnalysisResult();
                for (AttributeAnalysisStructure roleAttributeAnalysisStructure : roleAttributeAnalysisStructures) {
                    double density = roleAttributeAnalysisStructure.getDensity();
                    if (density == 0) {
                        continue;
                    }

                    totalDensity += density;

                    RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = roleAttributeAnalysisStructure
                            .buildRoleAnalysisAttributeAnalysisContainer();
                    roleAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
                }

                detectedPattern.setRoleAttributeAnalysisResult(roleAnalysis);
                totalCount += roleAnalysis.getAttributeAnalysis().size();
            }

            RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = detectedPattern.getRoleAttributeAnalysisResult();
            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = detectedPattern.getUserAttributeAnalysisResult();

            int itemCount = (roleAttributeAnalysisResult != null
                    ? roleAttributeAnalysisResult.getAttributeAnalysis().size() : 0)
                    + (userAttributeAnalysisResult != null ? userAttributeAnalysisResult.getAttributeAnalysis().size() : 0);

            double itemsConfidence = (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / itemCount : 0.0;
            detectedPattern.setItemConfidence(itemsConfidence);
        }
    }

    @Override
    public void resolveDetectedPatternsAttributesCached(
            @NotNull List<RoleAnalysisDetectionPatternType> detectedPatterns,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull Map<String, PrismObject<RoleType>> roleExistCache,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @Nullable List<RoleAnalysisAttributeDef> attributeRoleDefSet,
            @Nullable List<RoleAnalysisAttributeDef> attributeUserDefSet,
            @NotNull Task task,
            @NotNull OperationResult result) {

        long start = System.currentTimeMillis();
        for (RoleAnalysisDetectionPatternType detectedPattern : detectedPatterns) {

            List<ObjectReferenceType> userOccupancy = detectedPattern.getUserOccupancy();
            List<ObjectReferenceType> roleOccupancy = detectedPattern.getRolesOccupancy();
            Set<PrismObject<UserType>> users;
            Set<PrismObject<RoleType>> roles;

            users = userOccupancy.stream().map(objectReferenceType -> this
                            .cacheUserTypeObject(userExistCache, objectReferenceType.getOid(), task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());

            roles = roleOccupancy.stream().map(objectReferenceType -> this
                            .cacheRoleTypeObject(roleExistCache, objectReferenceType.getOid(), task, result, null))
                    .filter(Objects::nonNull).collect(Collectors.toSet());

            List<AttributeAnalysisStructure> userAttributeAnalysisStructures = null;
            if (attributeUserDefSet != null) {
                userAttributeAnalysisStructures = this.userTypeAttributeAnalysis(
                        users, 100.0, task, result, attributeUserDefSet);
            }

            List<AttributeAnalysisStructure> roleAttributeAnalysisStructures = null;
            if (attributeRoleDefSet != null) {
                roleAttributeAnalysisStructures = this
                        .roleTypeAttributeAnalysis(roles, 100.0, task, result, attributeRoleDefSet);
            }

            double totalDensity = 0.0;
            int totalCount = 0;

            if (userAttributeAnalysisStructures != null) {
                RoleAnalysisAttributeAnalysisResult userAnalysis = new RoleAnalysisAttributeAnalysisResult();
                for (AttributeAnalysisStructure userAttributeAnalysisStructure : userAttributeAnalysisStructures) {
                    double density = userAttributeAnalysisStructure.getDensity();
                    if (density == 0) {
                        continue;
                    }

                    totalDensity += density;

                    RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = userAttributeAnalysisStructure
                            .buildRoleAnalysisAttributeAnalysisContainer();
                    userAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
                }

                detectedPattern.setUserAttributeAnalysisResult(userAnalysis);
                totalCount += userAnalysis.getAttributeAnalysis().size();
            }

            if (roleAttributeAnalysisStructures != null) {
                RoleAnalysisAttributeAnalysisResult roleAnalysis = new RoleAnalysisAttributeAnalysisResult();
                for (AttributeAnalysisStructure roleAttributeAnalysisStructure : roleAttributeAnalysisStructures) {
                    double density = roleAttributeAnalysisStructure.getDensity();
                    if (density == 0) {
                        continue;
                    }

                    totalDensity += density;

                    RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = roleAttributeAnalysisStructure
                            .buildRoleAnalysisAttributeAnalysisContainer();
                    roleAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
                }

                detectedPattern.setRoleAttributeAnalysisResult(roleAnalysis);
                totalCount += roleAnalysis.getAttributeAnalysis().size();
            }

            RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = detectedPattern.getRoleAttributeAnalysisResult();
            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = detectedPattern.getUserAttributeAnalysisResult();

            int itemCount = (roleAttributeAnalysisResult != null
                    ? roleAttributeAnalysisResult.getAttributeAnalysis().size() : 0)
                    + (userAttributeAnalysisResult != null ? userAttributeAnalysisResult.getAttributeAnalysis().size() : 0);

            double itemsConfidence = (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / itemCount : 0.0;
            detectedPattern.setItemConfidence(itemsConfidence);
        }
        long end = System.currentTimeMillis();
        LOGGER.debug("Time to resolve detected patterns attributes: {} ms", (end - start));
    }

    @Override
    public List<PrismObject<RoleAnalysisClusterType>> searchSessionClusters(
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ObjectQuery query = PrismContext.get().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                .ref(session.getOid()).build();

        try {
            return modelService.searchObjects(RoleAnalysisClusterType.class, query, null,
                    task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Failed to search role member objects:", ex);
        } finally {
            result.recomputeStatus();
        }

        return Collections.emptyList();
    }

    //TODO temporary
    public String resolveFocusObjectIconColor(@NotNull FocusType focusObject, @NotNull Task task, @NotNull OperationResult result) {
        String color = null;
        List<ObjectReferenceType> archetypeRef = focusObject.getArchetypeRef();
        if (archetypeRef != null && !archetypeRef.isEmpty()) {
            PrismObject<ArchetypeType> object = this.getObject(ArchetypeType.class, archetypeRef.get(0).getOid(), task, result);
            if (object != null) {
                ArchetypePolicyType archetypePolicy = object.asObjectable().getArchetypePolicy();
                if (archetypePolicy != null) {
                    DisplayType display = archetypePolicy.getDisplay();
                    if (display != null) {
                        IconType icon = display.getIcon();
                        if (icon != null && icon.getColor() != null) {
                            color = icon.getColor();
                        }
                    }

                }
            }
        }
        return color;
    }

    @Override
    public <T extends ObjectType> Integer countObjects(@NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult parentResult) {
        try {
            return modelService.countObjects(type, query, options, task, parentResult);
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | ConfigurationException |
                CommunicationException | ExpressionEvaluationException e) {
            throw new SystemException("Couldn't count objects of type " + type + ": " + e.getMessage(), e);
        }
    }

    @Override
    public int countUserOwnedRoleAssignment(OperationResult result) {
        int aggregateResult = 0;
        var spec = AggregateQuery.forType(AssignmentType.class);

        Collection<QName> memberRelations = relationRegistry
                .getAllRelationsFor(RelationKindType.MEMBER);

        S_FilterExit filter = this.buildStatisticsAssignmentSearchFilter(memberRelations);

        try {
            spec.count(F_NAME, ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(), F_NAME))
                    .filter(filter.buildFilter())
                    .count(F_ASSIGNMENT, ItemPath.SELF_PATH);

            aggregateResult = repositoryService.countAggregate(spec, result);

        } catch (SchemaException e) {
            LOGGER.error("Cloud not count user owned role assignment", e);
        }
        return aggregateResult;
    }

    @Override
    public String calculateAttributeConfidence(
            @NotNull RoleAnalysisProcessModeType processModeType,
            @NotNull AnalysisClusterStatisticType clusterStatistics) {

        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = new ArrayList<>();

        RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();
        if (roleAttributeAnalysisResult != null) {
            attributeAnalysis.addAll(roleAttributeAnalysisResult.getAttributeAnalysis());
        }

        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
        if (userAttributeAnalysisResult != null) {
            attributeAnalysis.addAll(userAttributeAnalysisResult.getAttributeAnalysis());
        }

        if (attributeAnalysis.isEmpty()) {
            return "0.0";
        }

        double attributeConfidence = 0;
        for (RoleAnalysisAttributeAnalysis analysis : attributeAnalysis) {
            Double density = analysis.getDensity();
            if (density != null) {
                attributeConfidence += density;
            }
        }

        attributeConfidence /= attributeAnalysis.size();
        return String.format("%.2f", attributeConfidence);
    }

    @Override
    public @Nullable List<RoleAnalysisAttributeDef> resolveAnalysisAttributes(
            @NotNull RoleAnalysisSessionType session,
            @NotNull QName complexType) {

        //TODO remove later. It temporary disable role attribute analysis
        if (complexType == RoleType.COMPLEX_TYPE) {
            return null;
        }

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null) {
            return null;
        }
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        if (processMode == null) {
            return null;
        }

        AbstractAnalysisSessionOptionType options = resolveModeOptions(processMode, session);
        if (options == null) {
            return null;
        }

        //TODO user vs. role attribute settings?
        AnalysisAttributeSettingType analysisAttributeSetting = options.getUserAnalysisAttributeSetting();//getAnalysisAttributeSetting();
        if (analysisAttributeSetting == null) {
            return null;
        }

        List<RoleAnalysisAttributeDef> attributeDefs = RoleAnalysisAttributeDefUtils.createAttributeList(analysisAttributeSetting);

//        List<RoleAnalysisAttributeDef> attributeDefs = new ArrayList<>();
//
//        PrismObjectDefinition<UserType> userDefinition = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
//
//        PrismContainerDefinition<AssignmentType> assignmentDefinition = userDefinition.findContainerDefinition(F_ASSIGNMENT);
//        List<AnalysisAttributeRuleType> assignmentRules = analysisAttributeSetting.getAssignmentRule();
//        for (AnalysisAttributeRuleType rule : assignmentRules) {
//            RoleAnalysisAttributeDef attributeDef = new RoleAnalysisAssignmentAttributeDef(F_ASSIGNMENT, assignmentDefinition, rule);
//            attributeDefs.add(attributeDef);
//        }
//
//        List<ItemPathType> analysisAttributeRule = analysisAttributeSetting.getPath();
//
//        if (analysisAttributeRule.isEmpty()) {
//            return attributeDefs;
//        }
//
//
//
//        for (ItemPathType itemPathType : analysisAttributeRule) {
//            if (itemPathType == null) {
//                continue;
//            }
//            ItemPath path = itemPathType.getItemPath();
//            ItemDefinition<?> itemDefinition = userDefinition.findItemDefinition(path);
//            if (itemDefinition instanceof PrismContainerDefinition<?>) {
//                LOGGER.debug("Skipping {} because container items are not supported for attribute analysis.", itemDefinition);
//                continue;
//            }
//            //TODO reference vs. property
//            RoleAnalysisAttributeDef attributeDef = new RoleAnalysisAttributeDef(path, itemDefinition);
//            attributeDefs.add(attributeDef);
//        }

//        Map<String, RoleAnalysisAttributeDef> attributeMap = createAttributeMap();

//        for (AnalysisAttributeRuleType rule : analysisAttributeRule) {
//            if (!rule.getPropertyType().equals(complexType)) {
//                continue;
//            }
//
//            String key = rule.getAttributeIdentifier();
//            RoleAnalysisAttributeDef attributeDef = attributeMap.get(key);
//            if (attributeDef != null) {
//                attributeDefs.add(attributeDef);
//            }
//        }
        return attributeDefs;
    }

    private AbstractAnalysisSessionOptionType resolveModeOptions(RoleAnalysisProcessModeType processMode, RoleAnalysisSessionType session) {
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return session.getRoleModeOptions();
        }
        return session.getUserModeOptions();
    }

    @Override
    public @Nullable RoleAnalysisAttributeAnalysisResult resolveSimilarAspect(
            @NotNull RoleAnalysisAttributeAnalysisResult compared,
            @NotNull RoleAnalysisAttributeAnalysisResult comparison) {
        Objects.requireNonNull(compared);
        Objects.requireNonNull(comparison);

        RoleAnalysisAttributeAnalysisResult outlierAttributeAnalysisResult = new RoleAnalysisAttributeAnalysisResult();
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = comparison.getAttributeAnalysis();

        for (RoleAnalysisAttributeAnalysis analysisItem : attributeAnalysis) {
            ItemPathType clusterItemPath = analysisItem.getItemPath();
            Set<String> outlierValues = extractCorrespondingOutlierValues(compared, clusterItemPath);
            if (outlierValues == null) {
                continue;
            }

            RoleAnalysisAttributeAnalysis correspondingAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
            correspondingAttributeAnalysis.setItemPath(clusterItemPath);
            correspondingAttributeAnalysis.setParentType(analysisItem.getParentType());
            Double attributeDensity = analysisItem.getDensity();
            double attributeWeight = attributeDensity != null ? attributeDensity * 0.01 : 0.0;
            correspondingAttributeAnalysis.setWeight(attributeWeight);

            int counter = 0;
            int sum = 0;
            List<RoleAnalysisAttributeStatistics> attributeStatistics = analysisItem.getAttributeStatistics();
            for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                String clusterAttributeValue = attributeStatistic.getAttributeValue();
                Integer inGroup = attributeStatistic.getInGroup();
                sum += inGroup != null ? inGroup : 0;
                if (outlierValues.contains(clusterAttributeValue)) {
                    counter += inGroup != null ? inGroup : 0;
                    correspondingAttributeAnalysis.getAttributeStatistics().add(attributeStatistic.clone());
                }
            }

            double newDensity = sum != 0 ? ((double) counter / sum * 100) : 0.0;
            correspondingAttributeAnalysis.setDensity(newDensity);
            correspondingAttributeAnalysis.setAnalysedObjectCount(analysisItem.getAnalysedObjectCount());

            outlierAttributeAnalysisResult.getAttributeAnalysis().add(correspondingAttributeAnalysis.clone());
        }

        double weightedItemFactorConfidence = getWeightedItemFactorConfidence(outlierAttributeAnalysisResult);
        outlierAttributeAnalysisResult.setScore(weightedItemFactorConfidence);

        return outlierAttributeAnalysisResult;
    }

    private double getWeightedItemFactorConfidence(@Nullable RoleAnalysisAttributeAnalysisResult compareAttributeResult) {
        if (compareAttributeResult == null) {
            return 0;
        }

        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = compareAttributeResult.getAttributeAnalysis();
        if (attributeAnalysis.isEmpty()) {
            return 0;
        }

        double totalWeightedDensity = 0.0;
        double totalWeight = 0.0;
        for (RoleAnalysisAttributeAnalysis analysisItem : attributeAnalysis) {
            Double density = analysisItem.getDensity();
            Double weight = analysisItem.getWeight();

            totalWeightedDensity += density * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? totalWeightedDensity / totalWeight : 0.0;
    }

    @NotNull
    public RoleAnalysisAttributeAnalysisResult resolveUserAttributes(
            @NotNull PrismObject<UserType> prismUser,
            @NotNull List<RoleAnalysisAttributeDef> attributesForUserAnalysis) {
        RoleAnalysisAttributeAnalysisResult outlierCandidateAttributeAnalysisResult = new RoleAnalysisAttributeAnalysisResult();

        for (RoleAnalysisAttributeDef item : attributesForUserAnalysis) {
            RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
            roleAnalysisAttributeAnalysis.setItemPath(item.getPath().toBean());
            roleAnalysisAttributeAnalysis.setParentType(UserType.COMPLEX_TYPE);
            List<RoleAnalysisAttributeStatistics> attributeStatistics = roleAnalysisAttributeAnalysis.getAttributeStatistics();

            ItemPath path = item.getPath();
            boolean isContainer = item.isContainer();

            if (isContainer) {
                Set<String> values = item.resolveMultiValueItem(prismUser, path);
                for (String value : values) {
                    RoleAnalysisAttributeStatistics attributeStatistic = new RoleAnalysisAttributeStatistics();
                    attributeStatistic.setAttributeValue(value);
                    attributeStatistics.add(attributeStatistic);
                }
            } else {
                String value = item.resolveSingleValueItem(prismUser, path);
                if (value != null) {
                    RoleAnalysisAttributeStatistics attributeStatistic = new RoleAnalysisAttributeStatistics();
                    attributeStatistic.setAttributeValue(value);
                    attributeStatistics.add(attributeStatistic);
                }
            }
            outlierCandidateAttributeAnalysisResult.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis.clone());
        }
        return outlierCandidateAttributeAnalysisResult;
    }

    private static @Nullable Set<String> extractCorrespondingOutlierValues(
            @NotNull RoleAnalysisAttributeAnalysisResult outlierCandidateAttributeAnalysisResult, ItemPathType itemPath) {
        List<RoleAnalysisAttributeAnalysis> outlier = outlierCandidateAttributeAnalysisResult.getAttributeAnalysis();
        for (RoleAnalysisAttributeAnalysis outlierAttribute : outlier) {
            if (outlierAttribute.getItemPath().equals(itemPath)) { //TODO equivalent
                Set<String> outlierValues = new HashSet<>();
                for (RoleAnalysisAttributeStatistics attributeStatistic : outlierAttribute.getAttributeStatistics()) {
                    outlierValues.add(attributeStatistic.getAttributeValue());
                }
                return outlierValues;
            }
        }
        return null;
    }

    @Override
    public RoleAnalysisAttributeAnalysisResult resolveRoleMembersAttribute(
            @NotNull String objectOid,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        this.extractUserTypeMembers(
                userExistCache, null,
                new HashSet<>(singleton(objectOid)),
                task, result);

        Set<PrismObject<UserType>> users = new HashSet<>(userExistCache.values());
        userExistCache.clear();

        List<AttributeAnalysisStructure> userAttributeAnalysisStructures = this
                .userTypeAttributeAnalysis(users, 100.0, task, result, attributeDefSet);

        RoleAnalysisAttributeAnalysisResult userAnalysis = new RoleAnalysisAttributeAnalysisResult();
        for (AttributeAnalysisStructure userAttributeAnalysisStructure : userAttributeAnalysisStructures) {
            double density = userAttributeAnalysisStructure.getDensity();
            if (density == 0) {
                continue;
            }
            RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = userAttributeAnalysisStructure
                    .buildRoleAnalysisAttributeAnalysisContainer();
            userAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
        }

        return userAnalysis;
    }

    @Override
    public RoleAnalysisAttributeAnalysisResult resolveRoleMembersAttributeCached(
            @NotNull String objectOid,
            @NotNull AttributeAnalysisCache userAnalysisCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet) {

        ListMultimap<String, String> roleMemberCache = userAnalysisCache.getRoleMemberCache();
        List<String> usersOidList = roleMemberCache.get(objectOid);
        Set<PrismObject<UserType>> users;

        if (usersOidList == null) {
            Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
            this.extractUserTypeMembers(
                    userExistCache, null,
                    new HashSet<>(singleton(objectOid)),
                    task, result);

            users = new HashSet<>(userExistCache.values());
            userExistCache.clear();
        } else {
            users = new HashSet<>();
            for (String userOid : usersOidList) {
                PrismObject<UserType> userTypeObject = getUserTypeObject(userOid, task, result);
                if (userTypeObject != null) {
                    users.add(userTypeObject);
                }
            }
        }

        List<AttributeAnalysisStructure> userAttributeAnalysisStructures = this
                .userTypeAttributeAnalysisCached(users, 100.0, userAnalysisCache, attributeDefSet, task, result);

        RoleAnalysisAttributeAnalysisResult userAnalysis = new RoleAnalysisAttributeAnalysisResult();
        for (AttributeAnalysisStructure userAttributeAnalysisStructure : userAttributeAnalysisStructures) {
            double density = userAttributeAnalysisStructure.getDensity();
            if (density == 0) {
                continue;
            }
            RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = userAttributeAnalysisStructure
                    .buildRoleAnalysisAttributeAnalysisContainer();
            userAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
        }

        return userAnalysis;
    }

    public <T extends MiningBaseTypeChunk> ZScoreData resolveOutliersZScore(
            @NotNull List<T> data,
            @Nullable RangeType range,
            @Nullable Double sensitivity,
            @Nullable Double frequencyThreshold) {
        double defaultMaxFrequency = frequencyThreshold != null ? frequencyThreshold * 0.01 : 0.5;

        if (sensitivity == null) {
            sensitivity = 0.0;
        }

        if (range == null) {
            range = new RangeType();
            range.setMin(2.0);
            range.setMax(2.0);
        }

        RangeType tunedRange = calculateOutlierThresholdRange(sensitivity, range);

        double negativeThreshold = tunedRange.getMin();
        double positiveThreshold = tunedRange.getMax();

        double sum = 0;
        int dataSize = 0;
        for (T item : data) {
            FrequencyItem frequencyItem = item.getFrequencyItem();
            int memberCount = item.getMembers().size();
            // We target importance for the lower frequency values. The higher the frequency the less important.
            int weightSize = (int) (frequencyItem.getFrequency() * 100);
            int size = memberCount * weightSize;
            dataSize += size;
            sum += (item.getFrequencyValue()) * size;
        }
        double mean = sum / dataSize;

        double sumSquaredDiff = 0;
        for (T item : data) {
            FrequencyItem frequencyItem = item.getFrequencyItem();
            int memberCount = item.getMembers().size();
            int weightSize = (int) (frequencyItem.getFrequency() * 100);
            int size = memberCount * weightSize;
            sumSquaredDiff += (Math.pow((item.getFrequencyValue()) - mean, 2)) * size;
        }

        // n-1 Bessel's correction is preferable for outlier detection or n TODO check more details
        double variance = sumSquaredDiff / (dataSize);
        double stdDev = Math.sqrt(variance);
        ZScoreData zScoreData = new ZScoreData(sum, dataSize, mean, sumSquaredDiff, variance, stdDev);
        for (T item : data) {
            double zScore = ((item.getFrequencyValue()) - mean) / stdDev;
            //TODO thing about it
            zScore = Math.round(zScore * 100.0) / 100.0;

            double confidence = calculateZScoreConfidence(item, zScoreData);
            item.getFrequencyItem().setConfidence(confidence);
            item.getFrequencyItem().setzScore(zScore);
            // -1 OR -2 should it be fixed or configurable. Now it is good to identify unusual values like TODO
            //Temporary disable thing about it later
//            if (zScore <= -negativeThreshold) {
//                item.getFrequencyItem().setNegativeExclude();
//            } else if (zScore >= positiveThreshold) {
//                item.getFrequencyItem().setPositiveExclude();
//            } else {
//                item.getFrequencyItem().setInclude();
//            }

            double frequencyValue = item.getFrequencyValue();
            if (zScore <= -negativeThreshold && frequencyValue <= defaultMaxFrequency) {
                item.getFrequencyItem().setNegativeExclude();
            } else {
                item.getFrequencyItem().setInclude();
            }
        }

        //TODO experiment
//        resolveNeighbours(data, defaultMaxFrequency);
        return zScoreData;
    }

    /**
     * Resolves neighbors for the given list of data chunks based on their properties and frequency values.
     *
     * @param <T> The type of the data chunks, which must extend MiningBaseTypeChunk.
     * @param data The list of data chunks to process.
     * @param maxFrequency The maximum frequency value to consider for negative exclusion.
     */
    public <T extends MiningBaseTypeChunk> void resolveNeighbours(@NotNull List<T> data, double maxFrequency) {
//        List<T> negativeExcludeChunks = new ArrayList<>();
        ListMultimap<FrequencyItem.Status, T> itemMap = ArrayListMultimap.create();
        for (T chunk : data) {
            double frequency = chunk.getFrequencyValue();
            double status = chunk.getFrequencyItem().getzScore();
            if (status <= -1 && frequency <= maxFrequency) {
                itemMap.put(FrequencyItem.Status.NEGATIVE_EXCLUDE, chunk);
            }
//            if (status.equals(FrequencyItem.Status.NEGATIVE_EXCLUDE)) {
//                negativeExcludeChunks.add(chunk);
//            }
        }

        List<T> negativeExcludeChunks = itemMap.get(FrequencyItem.Status.NEGATIVE_EXCLUDE);

        if (negativeExcludeChunks.size() < 2) {
            return;
        }

        for (int i = 0; i < negativeExcludeChunks.size(); i++) {
            T firstItem = negativeExcludeChunks.get(i);
            List<String> properties = firstItem.getProperties();
            for (int j = i + 1; j < negativeExcludeChunks.size(); j++) {
                T secondItem = negativeExcludeChunks.get(j);
                List<String> properties2 = secondItem.getProperties();

                if (properties.size() != properties2.size()) {
                    continue;
                }

                if (new HashSet<>(properties).containsAll(properties2)) {
                    for (String member : secondItem.getMembers()) {
                        FrequencyItem.Neighbour neighbour = new FrequencyItem.Neighbour(
                                new ObjectReferenceType()
                                        .type(RoleType.COMPLEX_TYPE)
                                        .oid(member),
                                1);
                        firstItem.getFrequencyItem().addNeighbour(neighbour);
                    }
                    for (String member : firstItem.getMembers()) {
                        FrequencyItem.Neighbour neighbour = new FrequencyItem.Neighbour(
                                new ObjectReferenceType()
                                        .type(RoleType.COMPLEX_TYPE)
                                        .oid(member),
                                1);
                        secondItem.getFrequencyItem().addNeighbour(neighbour);
                    }
                }
            }
        }
    }

    /**
     * Calculate the confidence of the Z-Score
     *
     * @param item the item to calculate the confidence
     * @param zScoreData the Z-Score data
     * @param <T> the type of the item
     * @return the confidence of the Z-Score 0 to 1 if 1 is 100% confidence
     */
    @Override

    public <T extends MiningBaseTypeChunk> double calculateZScoreConfidence(@NotNull T item, ZScoreData zScoreData) {
        //Range from 0 to 1
        double zScore = ((item.getFrequencyValue()) - zScoreData.getMean()) / zScoreData.getStdDev();
        double standardDeviation = zScoreData.getStdDev();
//        NormalDistribution normalDistribution = new NormalDistribution(zScoreData.getMean(), zScoreData.getStdDev());
        NormalDistribution normalDistribution = new NormalDistribution(0, 1);
        double cumulativeProbability = normalDistribution.cumulativeProbability(zScore);
        cumulativeProbability = Math.min(100, Math.max(0.0, cumulativeProbability));
        return 1 - cumulativeProbability;
    }

    @Override
    public @Nullable Set<String> resolveUserValueToMark(
            @NotNull PrismObject<UserType> prismUser,
            @NotNull List<RoleAnalysisAttributeDef> itemDef) {

        Set<String> valueToMark = new HashSet<>();

        for (RoleAnalysisAttributeDef item : itemDef) {
            ItemPath path = item.getPath();
            boolean isContainer = item.isContainer();

            if (isContainer) {
                Set<String> values = item.resolveMultiValueItem(prismUser, path);
                valueToMark.addAll(values);
            } else {
                String value = item.resolveSingleValueItem(prismUser, path);
                if (value != null) {
                    valueToMark.add(value);
                }
            }

        }

        if (valueToMark.isEmpty()) {
            return null;
        } else {
            return valueToMark;
        }
    }

    @Override
    public @Nullable Set<String> resolveUserValueToMark(@NotNull RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult) {
        Set<String> valueToMark = new HashSet<>();
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
        if (attributeAnalysis.isEmpty()) {
            return Collections.emptySet();
        }

        for (RoleAnalysisAttributeAnalysis analysisItem : attributeAnalysis) {
            List<RoleAnalysisAttributeStatistics> attributeStatistics = analysisItem.getAttributeStatistics();
            if (attributeStatistics.isEmpty()) {
                continue;
            }
            for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                String attributeValue = attributeStatistic.getAttributeValue();
                valueToMark.add(attributeValue);
            }
        }

        return valueToMark;
    }

    @Override
    public @Nullable Set<String> resolveRoleValueToMark(
            @NotNull PrismObject<RoleType> prismRole,
            @NotNull List<RoleAnalysisAttributeDef> itemDef) {

        Set<String> valueToMark = new HashSet<>();

        for (RoleAnalysisAttributeDef item : itemDef) {
            ItemPath path = item.getPath();
            boolean isContainer = item.isContainer();

            if (isContainer) {
                Set<String> values = item.resolveMultiValueItem(prismRole, path);
                valueToMark.addAll(values);
            } else {
                String value = item.resolveSingleValueItem(prismRole, path);
                if (value != null) {
                    valueToMark.add(value);
                }
            }

        }

        if (valueToMark.isEmpty()) {
            return null;
        } else {
            return valueToMark;
        }
    }

    @Override
    public void importOutlier(@NotNull RoleAnalysisOutlierType outlier, @NotNull Task task, @NotNull OperationResult result) {
        MetadataType metadata = new MetadataType();
        metadata.setCreateTimestamp(getCurrentXMLGregorianCalendar());
        outlier.setMetadata(metadata);
        ObjectDelta<RoleAnalysisOutlierType> outlierDelta = PrismContext.get().deltaFactory().object().create(RoleAnalysisOutlierType.class, ChangeType.ADD);
        outlierDelta.setObjectToAdd(outlier.asPrismObject());

        //TODO exception handler
        try {
            repositoryService.addObject(outlier.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            throw new SystemException("Couldn't import outlier", e);
        }

    }

    public void resolveOutliers(
            @NotNull RoleAnalysisOutlierType roleAnalysisOutlierType,
            @NotNull Task task,
            @NotNull OperationResult result) {
        //TODO TARGET OBJECT REF IS NECESSARY (check git history)

        ObjectReferenceType targetObjectRef = roleAnalysisOutlierType.getObjectRef();
        PrismObject<FocusType> object = this
                .getObject(FocusType.class, targetObjectRef.getOid(), task, result);

        roleAnalysisOutlierType.setName(object != null && object.getName() != null
                ? object.asObjectable().getName()
                : PolyStringType.fromOrig("outlier_" + UUID.randomUUID()));

        this.importOutlier(roleAnalysisOutlierType, task, result);

    }

    @Override
    public void deleteSessionTask(
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {

            PrismObject<RoleAnalysisSessionType> sessionTypeObject = this.getSessionTypeObject(sessionOid, task, result);
            if (sessionTypeObject == null) {
                return;
            }

            RoleAnalysisOperationStatus operationStatus = sessionTypeObject.asObjectable().getOperationStatus();
            if (operationStatus == null) {
                return;
            }

            ObjectReferenceType taskRef = operationStatus.getTaskRef();
            if (taskRef == null) {
                return;
            }

            String taskOid = taskRef.getOid();
            if (taskOid == null) {
                return;
            }

            ObjectDelta<TaskType> deleteDelta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(TaskType.class, taskOid);

            modelService.executeChanges(singleton(deleteDelta), null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't delete RoleAnalysisSessionType Task {}", sessionOid, e);
        }
    }

    @Override
    public @Nullable PrismObject<TaskType> getSessionTask(
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {

            PrismObject<RoleAnalysisSessionType> sessionTypeObject = this.getSessionTypeObject(sessionOid, task, result);
            if (sessionTypeObject == null) {
                return null;
            }

            RoleAnalysisOperationStatus operationStatus = sessionTypeObject.asObjectable().getOperationStatus();
            if (operationStatus == null) {
                return null;
            }

            ObjectReferenceType taskRef = operationStatus.getTaskRef();
            if (taskRef == null) {
                return null;
            }

            String taskOid = taskRef.getOid();
            if (taskOid == null) {
                return null;
            }

            return repositoryService.getObject(TaskType.class, taskOid, null, result);
        } catch (SchemaException | ObjectNotFoundException e) {
            LOGGER.error("Couldn't delete RoleAnalysisSessionType Task {}", sessionOid, e);
        }
        return null;
    }

    @Override
    public void deleteSessionTask(
            @NotNull TaskType taskToDelete,
            @NotNull OperationResult result) {
        try {
            repositoryService.deleteObject(TaskType.class, taskToDelete.getOid(), result);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Couldn't delete RoleAnalysisSessionType Task {}", taskToDelete.getOid(), e);
        }
    }

    @Override
    public void replaceSessionMarkRef(
            @NotNull PrismObject<RoleAnalysisSessionType> session,
            @NotNull ObjectReferenceType newMarkRef,
            @NotNull OperationResult result,
            @NotNull Task task) {

        try {
            List<ObjectReferenceType> effectiveMarkRef = session.asObjectable().getEffectiveMarkRef();

            if (effectiveMarkRef != null && !effectiveMarkRef.isEmpty()) {
                ObjectDelta<RoleAnalysisSessionType> clearDelta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                        .item(ObjectType.F_EFFECTIVE_MARK_REF)
                        .delete(effectiveMarkRef.get(0).asReferenceValue().clone())
                        .asObjectDelta(session.getOid());
                Collection<ObjectDelta<? extends ObjectType>> collection = MiscSchemaUtil.createCollection(clearDelta);
                modelService.executeChanges(collection, null, task, result);
            }

            ObjectDelta<RoleAnalysisSessionType> addDelta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(ObjectType.F_EFFECTIVE_MARK_REF).add(newMarkRef.asReferenceValue().clone())
                    .asObjectDelta(session.getOid());
            Collection<ObjectDelta<? extends ObjectType>> collection = MiscSchemaUtil.createCollection(addDelta);
            modelService.executeChanges(collection, null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't modify RoleAnalysisClusterType {}", session.getOid(), e);
        }
    }

    @Override
    public void updateSessionMarkRef(
            @NotNull PrismObject<RoleAnalysisSessionType> session,
            @NotNull OperationResult result,
            @NotNull Task task) {

        try {
            List<ObjectReferenceType> effectiveMarkRef = session.asObjectable().getEffectiveMarkRef();

            if (effectiveMarkRef != null && !effectiveMarkRef.isEmpty()) {
                ObjectDelta<RoleAnalysisSessionType> clearDelta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                        .item(ObjectType.F_EFFECTIVE_MARK_REF).delete(effectiveMarkRef.get(0).asReferenceValue().clone())
                        .asObjectDelta(session.getOid());
                Collection<ObjectDelta<? extends ObjectType>> collectionClear = MiscSchemaUtil.createCollection(clearDelta);
                modelService.executeChanges(collectionClear, null, task, result);

                ObjectReferenceType mark = new ObjectReferenceType().oid("00000000-0000-0000-0000-000000000801")
                        .type(MarkType.COMPLEX_TYPE)
                        .description("First run");

                ObjectDelta<RoleAnalysisSessionType> addDelta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                        .item(ObjectType.F_EFFECTIVE_MARK_REF).add(mark.asReferenceValue().clone())
                        .asObjectDelta(session.getOid());
                Collection<ObjectDelta<? extends ObjectType>> collectionAdd = MiscSchemaUtil.createCollection(addDelta);
                modelService.executeChanges(collectionAdd, null, task, result);
            }

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't modify RoleAnalysisClusterType {}", session.getOid(), e);
        }
    }

    //TODO this is wrong, make beter structured object for this case
    @Override
    public List<RoleAnalysisOutlierType> getSessionOutliers(
            @NotNull String sessionOid,
            @Nullable OutlierClusterCategoryType category,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ObjectQuery objectQuery = PrismContext.get().queryFor(RoleAnalysisOutlierType.class)
                .item(RoleAnalysisOutlierType.F_PARTITION, RoleAnalysisOutlierPartitionType.F_CLUSTER_REF,
                        PrismConstants.T_OBJECT_REFERENCE,
                        RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                .ref(sessionOid).build();

        Map<RoleAnalysisOutlierType, Double> outlierMap = new HashMap<>();
        ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {

            RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getPartition();
            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                ObjectReferenceType targetSessionRef = outlierPartition.getTargetSessionRef();
                String oid = targetSessionRef.getOid();
                if (sessionOid.equals(oid)) {
                    if (category == null) {
                        Double overallConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
                        outlierMap.put(outlier.asObjectable(), overallConfidence);
                    } else {
                        RoleAnalysisPartitionAnalysisType partitionAnalysis = outlierPartition.getPartitionAnalysis();
                        OutlierCategoryType outlierCategory = partitionAnalysis.getOutlierCategory();
                        if (category.equals(outlierCategory.getOutlierClusterCategory())) {
                            Double overallConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
                            outlierMap.put(outlier.asObjectable(), overallConfidence);
                        }
                    }
                    break;
                }
            }

            return true;
        };

        GetOperationOptionsBuilder getOperationOptionsBuilder = schemaService.getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();

        try {
            modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, objectQuery, resultHandler,
                    getOperationOptionsBuilder.build(), task, result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't search session outliers", ex);
        }

        List<Map.Entry<RoleAnalysisOutlierType, Double>> sortedEntries = new ArrayList<>(outlierMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        List<RoleAnalysisOutlierType> topOutliers = new ArrayList<>();
        for (Map.Entry<RoleAnalysisOutlierType, Double> entry : sortedEntries) {
            topOutliers.add(entry.getKey());
        }

        return topOutliers;
    }

    @Override
    public List<RoleAnalysisOutlierType> getTopOutliers(
            @Nullable Integer limit,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ObjectQuery objectQuery = PrismContext.get().queryFor(RoleAnalysisOutlierType.class)
                .desc(RoleAnalysisOutlierType.F_OVERALL_CONFIDENCE)
                .build();

        if (limit != null) {
            objectQuery = PrismContext.get().queryFor(RoleAnalysisOutlierType.class)
                    .desc(RoleAnalysisOutlierType.F_OVERALL_CONFIDENCE)
                    .maxSize(limit)
                    .build();
        }

        List<RoleAnalysisOutlierType> outliers = new ArrayList<>();

        ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {
            outliers.add(outlier.asObjectable());
            return true;
        };

        try {
            modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, objectQuery,
                    resultHandler, null, task, result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't search outliers", ex);
        }
        return outliers;
    }

    @Override
    public @Nullable ListMultimap<Double, String> findJaccardCloseObject(
            @NotNull String userOid,
            @NotNull ListMultimap<List<String>, String> chunkMap,
            @NotNull MutableDouble usedFrequency,
            @NotNull List<String> outliersMembers,
            double minThreshold,
            int minMembers,
            @NotNull Task task,
            @NotNull OperationResult result) {
        PrismObject<UserType> userTypeObject = this.getUserTypeObject(userOid, task, result);
        if (userTypeObject == null) {
            return null;
        }

        ListMultimap<Double, String> similarityStats = ArrayListMultimap.create();

        UserType userObject = userTypeObject.asObjectable();
        List<String> userRolesToCompare = getRolesOidAssignment(userObject);

        for (List<String> points : chunkMap.keySet()) {
            double jacquardSimilarity = jacquardSimilarity(userRolesToCompare, points);
            jacquardSimilarity = Math.floor(jacquardSimilarity * 10) / 10.0;
            List<String> elements = chunkMap.get(points);
            if (jacquardSimilarity >= minThreshold) {
                for (String element : elements) {
                    similarityStats.put(jacquardSimilarity, element);
                }
            }
        }

        return similarityStats;
    }

    @NotNull
    public static List<String> resolveJaccardCloseObjectResult(
            int minMembers,
            @NotNull MutableDouble usedFrequency,
            @Nullable ListMultimap<Double, String> similarityStats) {
        if (similarityStats == null) {
            return new ArrayList<>();
        }
        List<Double> sortedKeys = new ArrayList<>(similarityStats.keySet());
        sortedKeys.sort(Collections.reverseOrder());

        List<String> elements = new ArrayList<>();
        for (Double similarityScore : sortedKeys) {
            List<String> thresholdElements = similarityStats.get(similarityScore);
            elements.addAll(thresholdElements);
            if (elements.size() >= minMembers) {
                usedFrequency.setValue(similarityScore);
                return elements;
            }
        }

        return new ArrayList<>();
    }

    @Override
    public ListMultimap<List<String>, String> loadUserForOutlierComparison(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull List<String> outliersMembers,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @Nullable SearchFilterType userSearchFilter,
            @Nullable SearchFilterType roleSearchFilter,
            @Nullable SearchFilterType assignmentSearchFilter,
            @NotNull OperationResult result,
            @NotNull Task task,
            @NotNull RoleAnalysisSessionType sessionObject) {
        ListMultimap<List<String>, String> listStringListMultimap = this.prepareAssignmentChunkMapRolesAsKey(
                userSearchFilter, roleSearchFilter, userSearchFilter, RoleAnalysisProcessModeType.USER,
                false, null, objectCategorisationCache, task, result, sessionObject);

        Iterator<Map.Entry<List<String>, String>> iterator = listStringListMultimap.entries().iterator();
        while (iterator.hasNext()) {
            Map.Entry<List<String>, String> entry = iterator.next();
            List<String> key = entry.getKey();

            //TODO is it correct way to remove the outliers cluster members for comparison?
            for (String member : key) {
                if (outliersMembers.contains(member)) {
                    iterator.remove();
                    break;
                }
            }
        }

        return listStringListMultimap;
    }

    @Override
    public @NotNull RangeType calculateOutlierThresholdRange(Double sensitivity, @NotNull RangeType range) {
        if (sensitivity < 0.0 || sensitivity > 100) {
            sensitivity = 0.0;
        }

        if (range.getMin() == null || range.getMax() == null) {
            range.setMin(2.0);
            range.setMax(2.0);
        }

        //TODO careful with the range now is both values positive
        Double min = range.getMin();
        Double max = range.getMax();

        double thresholdMin = min * (1 + (sensitivity * 0.01));
        double thresholdMax = max * (1 + (sensitivity * 0.01));

        RangeType tunedRange = new RangeType();
        tunedRange.setMin(thresholdMin);
        tunedRange.setMax(thresholdMax);
        return range;
    }

    @Override
    public double calculateOutlierConfidenceRequired(double sensitivity) {
        //TODO check the formula
        if (sensitivity < 0.0 || sensitivity > 100) {
            return 0.0;
        }

        return sensitivity * 0.01;
    }

    //TODO
    @NotNull
    public List<RoleAnalysisOutlierType> findClusterOutliers(
            @NotNull RoleAnalysisClusterType cluster,
            @Nullable OutlierSpecificCategoryType category,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<RoleAnalysisOutlierType> searchResultList = new ArrayList<>();
        String clusterOid = cluster.getOid();

        ObjectQuery objectQuery = PrismContext.get().queryFor(RoleAnalysisOutlierType.class)
                .item(RoleAnalysisOutlierType.F_PARTITION, RoleAnalysisOutlierPartitionType.F_CLUSTER_REF)
                .ref(clusterOid).build();

        ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {

            RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getPartition();
            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                ObjectReferenceType targetClusterRef = outlierPartition.getClusterRef();
                String oid = targetClusterRef.getOid();
                if (clusterOid.equals(oid)) {
                    if (category != null) {
                        RoleAnalysisPartitionAnalysisType partitionAnalysis = outlierPartition.getPartitionAnalysis();
                        OutlierCategoryType outlierCategory = partitionAnalysis.getOutlierCategory();
                        OutlierSpecificCategoryType outlierSpecificCategory = outlierCategory.getOutlierSpecificCategory();
                        if (outlierSpecificCategory == category) {
                            searchResultList.add(outlier.asObjectable());
                        }
                    } else {
                        searchResultList.add(outlier.asObjectable());
                    }
                }
            }
            return true;
        };

        try {
            modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, objectQuery, resultHandler,
                    null, task, result);

        } catch (Exception ex) {
            throw new SystemException("Couldn't search outliers", ex);
        }
        return searchResultList;
    }

    @Override
    public PrismObject<RoleAnalysisOutlierType> searchOutlierObjectByUserOid(
            @NotNull String userOid,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ObjectQuery query = PrismContext.get().queryFor(RoleAnalysisOutlierType.class)
                .item(RoleAnalysisOutlierType.F_OBJECT_REF)
                .ref(userOid).build();

        try {
            //TODO there should be only one outlier object per user
            SearchResultList<PrismObject<RoleAnalysisOutlierType>> prismObjects = modelService.searchObjects(RoleAnalysisOutlierType.class, query, null,
                    task, result);
            if (prismObjects == null || prismObjects.size() != 1) {
                return null;
            }

            return prismObjects.get(0);
        } catch (SchemaException | ConfigurationException | CommunicationException | SecurityViolationException |
                ExpressionEvaluationException e) {
            throw new SystemException("Couldn't search outlier object associated for user with oid: " + userOid, e);
        } catch (ObjectNotFoundException e) {
            return null;
        }

    }

    @Override
    public void addOutlierPartition(
            @NotNull String outlierOid,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            double overallConfidence,
            double anomalyConfidence,
            @NotNull OperationResult result) {

        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();
            modifications.add(PrismContext.get().deltaFor(RoleAnalysisOutlierType.class)
                    .item(RoleAnalysisOutlierType.F_PARTITION).add(partition.clone())
                    .asItemDelta());

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisOutlierType.class)
                    .item(RoleAnalysisOutlierType.F_OVERALL_CONFIDENCE).replace(overallConfidence).asItemDelta());

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisOutlierType.class)
                    .item(RoleAnalysisOutlierType.F_ANOMALY_OBJECTS_CONFIDENCE).replace(anomalyConfidence).asItemDelta());

            repositoryService.modifyObject(RoleAnalysisOutlierType.class, outlierOid, modifications, result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            LOGGER.error("Couldn't update RoleAnalysisOutlierType {}", outlierOid, e);
        }

    }

    @Override
    public UserAccessDistribution resolveUserAccessDistribution(
            @NotNull PrismObject<UserType> prismUser,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<ObjectReferenceType> directAssignments = new ArrayList<>();
        List<ObjectReferenceType> indirectAssignments = new ArrayList<>();
        List<ObjectReferenceType> duplicates = new ArrayList<>();

        UserType user = prismUser.asObjectable();
        List<ObjectReferenceType> refsToRoles = user.getRoleMembershipRef()
                .stream()
                .filter(ref -> QNameUtil.match(ref.getType(), RoleType.COMPLEX_TYPE)) //TODO maybe also check relation?
                .toList();

        for (ObjectReferenceType ref : refsToRoles) {
            List<AssignmentPathMetadataType> metadataPaths = computeAssignmentPaths(ref);
            if (metadataPaths.size() == 1) {
                List<AssignmentPathSegmentMetadataType> segments = metadataPaths.get(0).getSegment();
                if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                    directAssignments.add(ref);
                } else {
                    indirectAssignments.add(ref);
                }
            } else {
                boolean foundDirect = false;
                boolean foundIndirect = false;
                for (AssignmentPathMetadataType metadata : metadataPaths) {
                    List<AssignmentPathSegmentMetadataType> segments = metadata.getSegment();
                    if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                        foundDirect = true;
                        if (foundIndirect) {
                            indirectAssignments.remove(ref);
                            duplicates.add(ref);
                        } else {
                            directAssignments.add(ref);
                        }

                    } else {
                        foundIndirect = true;
                        if (foundDirect) {
                            directAssignments.remove(ref);
                            duplicates.add(ref);
                        } else {
                            indirectAssignments.add(ref);
                        }
                    }
                }
            }

        }

        UserAccessDistribution userAccessDistribution = new UserAccessDistribution(
                directAssignments, indirectAssignments, duplicates);
        userAccessDistribution.setAllAccessCount(refsToRoles.size());
        return userAccessDistribution;
    }

    @Override
    public @NotNull List<PrismObject<FocusType>> getAsFocusObjects(
            @Nullable List<ObjectReferenceType> references,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<PrismObject<FocusType>> members = new ArrayList<>();
        if (references == null) {
            return members;
        }
        for (ObjectReferenceType ref : references) {
            if (ref != null && ref.getOid() != null) {
                String oid = ref.getOid();
                PrismObject<FocusType> focusObject = this.getFocusTypeObject(oid,
                        task, result);
                members.add(focusObject);
            }
        }
        return members;
    }

    @Override
    public int[] computeResolvedAndCandidateRoles(@NotNull Task task, @NotNull OperationResult result) {
        SearchResultList<PrismObject<RoleAnalysisClusterType>> searchResultList = null;
        try {
            searchResultList = modelService.searchObjects(
                    RoleAnalysisClusterType.class, null, null, task, result);
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException |
                ConfigurationException | ExpressionEvaluationException e) {
            LOGGER.error("Couldn't search RoleAnalysisClusterType objects", e);
        }

        if (searchResultList == null || searchResultList.isEmpty()) {
            return new int[] { 0, 0 };
        }

        int resolvedPatternCount = 0;
        int candidateRolesCount = 0;

        for (PrismObject<RoleAnalysisClusterType> prismCluster : searchResultList) {
            RoleAnalysisClusterType cluster = prismCluster.asObjectable();
            List<ObjectReferenceType> resolvedPattern = cluster.getResolvedPattern();
            int resolvedPatterns = 0;
            if (resolvedPattern != null) {
                resolvedPatterns = resolvedPattern.size();
                resolvedPatternCount += resolvedPatterns;
            }

            List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();
            if (candidateRoles != null) {
                candidateRolesCount += candidateRoles.size();
                //there is no possibility to remove candidate roles from cluster so we can subtract resolved patterns.
                // If it changes then we need to change this logic
                candidateRolesCount -= resolvedPatterns;
            }
        }

        return new int[] { resolvedPatternCount, candidateRolesCount };
    }

    /**
     * Calculates the possible reduction in role assignments for a given session.
     * This method calculates the total reduction in role assignments by summing up the detected reduction metrics
     * for each cluster in the session.
     * It then calculates the total system percentage reduction by dividing the total reduction by the total
     * number of role assignments to users.
     *
     * @param session The RoleAnalysisSessionType object for which the possible assignment reduction is to be calculated.
     * @param task The task in which the operation is performed.
     * @param result The operation result.
     * @return The total system percentage reduction in role assignments. If there are no clusters in the session
     * or no role assignments to users, it returns 0.
     */
    @Override
    public double calculatePossibleAssignmentReduction(RoleAnalysisSessionType session, Task task, OperationResult result) {
        List<PrismObject<RoleAnalysisClusterType>> sessionClusters = this.searchSessionClusters(session, task, result);
        if (sessionClusters == null || sessionClusters.isEmpty()) {
            return 0;
        }

        int totalReduction = 0;

        for (PrismObject<RoleAnalysisClusterType> prismCluster : sessionClusters) {
            RoleAnalysisClusterType cluster = prismCluster.asObjectable();
            AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
            totalReduction += clusterStatistics.getDetectedReductionMetric();
        }

        int totalAssignmentRoleToUser = this.countUserOwnedRoleAssignment(result);
        double totalSystemPercentageReduction = 0;
        if (totalReduction != 0 && totalAssignmentRoleToUser != 0) {
            totalSystemPercentageReduction = ((double) totalReduction / totalAssignmentRoleToUser) * 100;
            BigDecimal bd = BigDecimal.valueOf(totalSystemPercentageReduction);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            totalSystemPercentageReduction = bd.doubleValue();
        }

        return totalSystemPercentageReduction;
    }

    public List<RoleAnalysisClusterType> getSessionClustersByType(
            @NotNull String sessionOid,
            @NotNull RoleAnalysisClusterCategory clusterType,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<RoleAnalysisClusterType> clusters = new ArrayList<>();
        ObjectQuery query = PrismContext.get().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF).ref(sessionOid)
                .build();

        try {
            SearchResultList<PrismObject<RoleAnalysisClusterType>> searchResultList = modelService.searchObjects(
                    RoleAnalysisClusterType.class, query, null, task, result);
            if (searchResultList == null || searchResultList.isEmpty()) {
                return new ArrayList<>();
            }

            for (PrismObject<RoleAnalysisClusterType> prismCluster : searchResultList) {
                RoleAnalysisClusterType cluster = prismCluster.asObjectable();
                RoleAnalysisClusterCategory category = cluster.getCategory();
                if (category == clusterType) {
                    clusters.add(cluster);
                }
            }
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException |
                ConfigurationException | ExpressionEvaluationException e) {
            LOGGER.error("Couldn't search RoleAnalysisClusterType objects", e);
        }
        return clusters;
    }

    @Override
    public SearchResultList<PrismObject<RoleAnalysisOutlierType>> searchOutliersRepo(
            @Nullable ObjectQuery query,
            @NotNull OperationResult result) {
        try {
            return repositoryService
                    .searchObjects(RoleAnalysisOutlierType.class, null, null, result);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't search RoleAnalysisOutlierType objects", e);
        }
    }

    @Override
    public ListMultimap<String, String> membershipSearch(
            @Nullable ObjectFilter userObjectFiler,
            @Nullable ObjectFilter roleObjectFilter,
            @Nullable ObjectFilter assignmentFilter,
            boolean loadAndUpdateStatistics,
            @NotNull RoleAnalysisProcessModeType processMode,
            @Nullable AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisSessionType sessionObject) {

        RoleAnalysisSessionStatisticType sessionStatistic = sessionObject.getSessionStatistic();
        if (sessionStatistic == null) {
            sessionStatistic = new RoleAnalysisSessionStatisticType();
            sessionObject.setSessionStatistic(sessionStatistic);
        }

        Collection<SelectorOptions<GetOperationOptions>> options = getDefaultRmIterativeSearchPageSizeOptions();
        ObjectQuery membershipQuery = buildMembershipSearchObjectQuery(userObjectFiler, roleObjectFilter);

        ListMultimap<String, String> roleMembersMap = ArrayListMultimap.create();

        ObjectHandler<ObjectReferenceType> handler = (object, parentResult) -> {
            PrismReferenceValue referenceValue = object.asReferenceValue();
            if (referenceValue == null) {
                LOGGER.error("Couldn't get reference value during membership search");
                return true;
            }
            Objectable rootObjectable = referenceValue.getRootObjectable();
            if (rootObjectable == null) {
                LOGGER.error("Couldn't get root object during membership search");
                return true;
            }

            Object ownerOid = rootObjectable.getOid();
            String roleOid = referenceValue.getOid();

            if (ownerOid == null) {
                LOGGER.error("Owner oid retrieved null value during membership search");
                return true;
            }

            if (roleOid == null) {
                LOGGER.error("Target role oid retrieved null value during membership search");
                return true;
            }

            roleMembersMap.put(roleOid, ownerOid.toString());
            return true;
        };

        try {
            modelService.searchReferencesIterative(membershipQuery, handler, options, task, result);
        } catch (SchemaException | SecurityViolationException | ConfigurationException | ObjectNotFoundException |
                ExpressionEvaluationException | CommunicationException e) {
            throw new SystemException("Couldn't search assignments for role analysis", e);
        }

        //TODO clean up
        return prepareAnalysisData(this,
                sessionObject, loadAndUpdateStatistics, attributeAnalysisCache, objectCategorisationCache, processMode,
                roleMembersMap, task, result);
    }

    @Override
    public ListMultimap<String, String> assignmentSearch(
            @Nullable ObjectFilter userObjectFiler,
            @Nullable ObjectFilter roleObjectFilter,
            @Nullable ObjectFilter assignmentFilter,
            @NotNull RoleAnalysisProcessModeType processMode,
            boolean loadAndUpdateStatistics,
            @Nullable AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisSessionType sessionObject) {
        Collection<SelectorOptions<GetOperationOptions>> options = getDefaultRmIterativeSearchPageSizeOptions();

        ObjectQuery assignmentQuery = buildAssignmentSearchObjectQuery(userObjectFiler, roleObjectFilter, assignmentFilter);

        ListMultimap<String, String> roleMembersMap = ArrayListMultimap.create();

        ContainerableResultHandler<AssignmentType> handler;
        handler = (object, parentResult) -> {
            PrismContainerValue<?> prismContainerValue = object.asPrismContainerValue();

            if (prismContainerValue == null) {
                LOGGER.error("Couldn't get prism container value during assignment search");
                return true;
            }

            Map<String, Object> userData = prismContainerValue.getUserData();

            if (userData == null) {
                LOGGER.error("Couldn't get user data during assignment search");
                return true;
            }

            Object ownerOid = userData.get("ownerOid");
            ObjectReferenceType targetRef = object.getTargetRef();
            if (targetRef == null) {
                LOGGER.error("Couldn't get target reference during assignment search");
                return true;
            }

            String roleOid = targetRef.getOid();
            if (ownerOid == null) {
                LOGGER.error("Owner oid retrieved null value during assignment search");
                return true;
            }

            if (roleOid == null) {
                LOGGER.error("Target role oid retrieved null value during search");
                return true;
            }

            roleMembersMap.put(roleOid, ownerOid.toString());

            return true;
        };

        try {
            modelService.searchContainersIterative(AssignmentType.class, assignmentQuery, handler,
                    options, task, result);
        } catch (SchemaException | SecurityViolationException | ConfigurationException | ObjectNotFoundException |
                ExpressionEvaluationException | CommunicationException e) {
            throw new SystemException("Couldn't search assignments", e);
        }
        //TODO clean up
        return prepareAnalysisData(this,
                sessionObject, loadAndUpdateStatistics, attributeAnalysisCache, objectCategorisationCache, processMode,
                roleMembersMap, task, result);
    }

    @Override
    public ListMultimap<List<String>, String> prepareAssignmentChunkMapRolesAsKey(
            @Nullable SearchFilterType userSearchFiler,
            @Nullable SearchFilterType roleSearchFiler,
            @Nullable SearchFilterType assignmentSearchFiler,
            @NotNull RoleAnalysisProcessModeType processMode,
            boolean loadAndUpdateStatistics,
            @Nullable AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisSessionType sessionObject) {

        ObjectFilter userObjectFilter = transformSearchToObjectFilter(userSearchFiler, UserType.class);
        ObjectFilter roleObjectFilter = transformSearchToObjectFilter(roleSearchFiler, RoleType.class);
        ObjectFilter assignmentFilter = transformSearchToObjectFilter(assignmentSearchFiler, AssignmentType.class);

        ListMultimap<String, String> userRolesMap = assignmentSearch(
                userObjectFilter, roleObjectFilter, assignmentFilter, processMode, loadAndUpdateStatistics,
                attributeAnalysisCache, objectCategorisationCache, task, result, sessionObject);
        ListMultimap<List<String>, String> compressedUsers = ArrayListMultimap.create();

        for (String userOid : userRolesMap.keySet()) {
            List<String> rolesOids = userRolesMap.get(userOid);
            Collections.sort(rolesOids);
            compressedUsers.put(rolesOids, userOid);
        }
        return compressedUsers;
    }

    @Override
    public ListMultimap<List<String>, String> prepareMembershipChunkMapRolesAsKey(
            @Nullable SearchFilterType userSearchFiler,
            @Nullable SearchFilterType roleSearchFiler,
            @Nullable SearchFilterType assignmentSearchFiler,
            @NotNull RoleAnalysisProcessModeType processMode,
            boolean loadAndUpdateStatistics,
            @Nullable AttributeAnalysisCache attributeAnalysisCache,
            @NotNull ObjectCategorisationCache objectCategorisationCache,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisSessionType sessionObject) {

        ObjectFilter userObjectFilter = transformSearchToObjectFilter(userSearchFiler, UserType.class);
        ObjectFilter roleObjectFilter = transformSearchToObjectFilter(roleSearchFiler, RoleType.class);
        ObjectFilter assignmentFilter = transformSearchToObjectFilter(assignmentSearchFiler, AssignmentType.class);

        ListMultimap<String, String> userRolesMap = this.membershipSearch(
                userObjectFilter, roleObjectFilter, assignmentFilter, loadAndUpdateStatistics,
                processMode, attributeAnalysisCache, objectCategorisationCache, task, result, sessionObject);
        ListMultimap<List<String>, String> compressedUsers = ArrayListMultimap.create();

        for (String userOid : userRolesMap.keySet()) {
            List<String> rolesOids = userRolesMap.get(userOid);
            Collections.sort(rolesOids);
            compressedUsers.put(rolesOids, userOid);
        }
        return compressedUsers;
    }

    @Override
    public @Nullable ObjectFilter transformSearchToObjectFilter(
            @Nullable SearchFilterType userSearchFiler,
            @NotNull Class<?> objectClass) {
        if (userSearchFiler != null) {
            try {
                ObjectFilter objectFilter = PrismContext.get().getQueryConverter()
                        .createObjectFilter(objectClass, userSearchFiler);
                if (objectFilter != null) {
                    return objectFilter;
                }
            } catch (SchemaException e) {
                throw new SystemException("Couldn't create object filter", e);
            }
        }
        return null;
    }

    @Override
    public @NotNull ListMultimap<String, String> assignmentRoleMemberSearch(
            @Nullable SearchFilterType userSearchFiler,
            @Nullable SearchFilterType roleSearchFiler,
            @Nullable SearchFilterType assignmentSearchFiler,
            @NotNull Set<String> roleMembers,
            boolean roleAsKey,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisClusterType clusterObject) {

        RoleAnalysisSessionType sessionObject = null;
        if (clusterObject.getRoleAnalysisSessionRef() != null) {
            PrismObject<RoleAnalysisSessionType> prismSession = this.getSessionTypeObject(
                    clusterObject.getRoleAnalysisSessionRef().getOid(), task, result);
            if (prismSession != null) {
                sessionObject = prismSession.asObjectable();
            }
        }

        if (sessionObject == null) {
            throw new SystemException("Couldn't get session object");
        }

        Set<String> unwantedAccess = getManuallyUnwantedAccesses(this, sessionObject.getOid(), task, result);
        boolean unwantedAccessNotSet = unwantedAccess.isEmpty();

        Set<String> unwantedUsers = getManuallyUnwantedUsers(this, sessionObject.getOid(), task, result);
        boolean unwantedUsersNotSet = unwantedUsers.isEmpty();

        Collection<SelectorOptions<GetOperationOptions>> options = getDefaultRmIterativeSearchPageSizeOptions();

        ObjectFilter userObjectFilter = transformSearchToObjectFilter(userSearchFiler, UserType.class);
        ObjectFilter roleObjectFilter = transformSearchToObjectFilter(roleSearchFiler, RoleType.class);
        ObjectFilter assignmentFilter = transformSearchToObjectFilter(assignmentSearchFiler, AssignmentType.class);

        ListMultimap<String, String> roleMemberCache = ArrayListMultimap.create();

        ObjectQuery query = buildAssignmentRoleMemberSearchObjectQuery(roleMembers, userObjectFilter, roleObjectFilter, assignmentFilter);

        ContainerableResultHandler<AssignmentType> handler;
        handler = (object, parentResult) -> {
            PrismContainerValue<?> prismContainerValue = object.asPrismContainerValue();

            if (prismContainerValue == null) {
                LOGGER.error("Couldn't get prism container value during assignment role member search");
                return true;
            }

            Map<String, Object> userData = prismContainerValue.getUserData();

            if (userData == null) {
                LOGGER.error("Couldn't get user data during assignment role member search");
                return true;
            }

            Object ownerOid = userData.get("ownerOid");
            ObjectReferenceType targetRef = object.getTargetRef();
            if (targetRef == null) {
                LOGGER.error("Couldn't get target reference during assignment role member search");
                return true;
            }

            String roleOid = targetRef.getOid();

            if (ownerOid == null) {
                LOGGER.error("Owner oid retrieved null value during assignment role member search");
                return true;
            }

            if (roleOid == null) {
                LOGGER.error("Target role oid retrieved null value during role member search");
                return true;
            }

            if (!unwantedAccessNotSet && unwantedAccess.contains(roleOid)) {
                return true;
            }

            if (!unwantedUsersNotSet && unwantedUsers.contains(ownerOid.toString())) {
                return true;
            }

            roleMemberCache.put(roleOid, ownerOid.toString());

            return true;
        };

        try {
            modelService.searchContainersIterative(AssignmentType.class, query, handler,
                    options, task, result);
        } catch (SchemaException | SecurityViolationException | ConfigurationException | ObjectNotFoundException |
                ExpressionEvaluationException | CommunicationException e) {
            throw new SystemException("Couldn't search assignments", e);
        }
        if (roleAsKey) {
            return roleMemberCache;
        } else {
            ListMultimap<String, String> userRolesMap = ArrayListMultimap.create();
            for (Map.Entry<String, String> entry : roleMemberCache.entries()) {
                String roleOid = entry.getKey();
                String ownerOid = entry.getValue();
                userRolesMap.put(ownerOid, roleOid);
            }
            return userRolesMap;
        }
    }

    @Override
    public @NotNull ListMultimap<String, String> assignmentUserAccessSearch(
            @Nullable SearchFilterType userSearchFiler,
            @Nullable SearchFilterType roleSearchFiler,
            @Nullable SearchFilterType assignmentSearchFiler,
            @NotNull Set<String> userMembers,
            boolean userAsKey,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisClusterType clusterObject) {

        RoleAnalysisSessionType sessionObject = null;
        if (clusterObject.getRoleAnalysisSessionRef() != null) {
            PrismObject<RoleAnalysisSessionType> prismSession = this.getSessionTypeObject(
                    clusterObject.getRoleAnalysisSessionRef().getOid(), task, result);
            if (prismSession != null) {
                sessionObject = prismSession.asObjectable();
            }
        }

        if (sessionObject == null) {
            throw new SystemException("Couldn't get session object");
        }

        Set<String> unwantedAccess = getManuallyUnwantedAccesses(this, sessionObject.getOid(), task, result);
        boolean unwantedAccessNotSet = unwantedAccess.isEmpty();

        Set<String> unwantedUsers = getManuallyUnwantedUsers(this, sessionObject.getOid(), task, result);
        boolean unwantedUsersNotSet = unwantedUsers.isEmpty();

        Collection<SelectorOptions<GetOperationOptions>> options = getDefaultRmIterativeSearchPageSizeOptions();

        ObjectFilter userObjectFilter = transformSearchToObjectFilter(userSearchFiler, UserType.class);
        ObjectFilter roleObjectFilter = transformSearchToObjectFilter(roleSearchFiler, RoleType.class);
        ObjectFilter assignmentFilter = transformSearchToObjectFilter(assignmentSearchFiler, AssignmentType.class);

        ListMultimap<String, String> roleMemberCache = ArrayListMultimap.create();

        ObjectQuery query = buildAssignmentUserAccessSearchObjectQuery(userMembers, userObjectFilter, roleObjectFilter, assignmentFilter);

        ContainerableResultHandler<AssignmentType> handler;
        handler = (object, parentResult) -> {
            PrismContainerValue<?> prismContainerValue = object.asPrismContainerValue();

            if (prismContainerValue == null) {
                LOGGER.error("Couldn't get prism container value during assignment search");
                return true;
            }

            Map<String, Object> userData = prismContainerValue.getUserData();

            if (userData == null) {
                LOGGER.error("Couldn't get user data during assignment search");
                return true;
            }

            Object ownerOid = userData.get("ownerOid");
            ObjectReferenceType targetRef = object.getTargetRef();
            if (targetRef == null) {
                LOGGER.error("Couldn't get target reference during assignment search");
                return true;
            }

            String roleOid = targetRef.getOid();

            if (ownerOid == null) {
                LOGGER.error("Owner oid retrieved null value during assignment search");
                return true;
            }

            if (roleOid == null) {
                LOGGER.error("Target role oid retrieved null value during search");
                return true;
            }

            if (!unwantedAccessNotSet && unwantedAccess.contains(roleOid)) {
                return true;
            }

            if (!unwantedUsersNotSet && unwantedUsers.contains(ownerOid.toString())) {
                return true;
            }

            roleMemberCache.put(roleOid, ownerOid.toString());

            return true;
        };

        try {
            modelService.searchContainersIterative(AssignmentType.class, query, handler, options, task, result);
        } catch (SchemaException | SecurityViolationException | ConfigurationException | ObjectNotFoundException |
                ExpressionEvaluationException | CommunicationException e) {
            throw new SystemException("Couldn't search assignments", e);
        }

        if (!userAsKey) {
            return roleMemberCache;
        } else {
            ListMultimap<String, String> userRolesMap = ArrayListMultimap.create();
            for (Map.Entry<String, String> entry : roleMemberCache.entries()) {
                String roleOid = entry.getKey();
                String ownerOid = entry.getValue();
                userRolesMap.put(ownerOid, roleOid);
            }
            return userRolesMap;
        }
    }

    public List<DetectedPattern> getSessionRoleSuggestion(
            @NotNull String sessionOid,
            @Nullable Integer limit,
            @Nullable Boolean sortDescending,
            @NotNull OperationResult result) {

        S_QueryExit sQueryExit = PrismContext.get().queryFor(RoleAnalysisDetectionPatternType.class)
                .ownedBy(RoleAnalysisClusterType.class)
                .block()
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                .ref(sessionOid)
                .endBlock();

        if (sortDescending != null && sortDescending.equals(Boolean.TRUE)) {
            sQueryExit = sQueryExit.desc(RoleAnalysisDetectionPatternType.F_REDUCTION_COUNT);
        } else if (sortDescending != null && sortDescending.equals(Boolean.FALSE)) {
            sQueryExit = sQueryExit.asc(RoleAnalysisDetectionPatternType.F_REDUCTION_COUNT);
        }

        if (limit != null) {
            sQueryExit = sQueryExit.maxSize(limit);
        }

        ObjectQuery query = sQueryExit.build();

        List<DetectedPattern> detectedPatterns = new ArrayList<>();

        GetOperationOptionsBuilder getOperationOptionsBuilder = schemaService.getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder.build();

        Map<String, RoleAnalysisClusterType> mappedClusters = new HashMap<>();

        ContainerableResultHandler<RoleAnalysisDetectionPatternType> handler = (pattern, lResult) -> {
            loadDetectedPattern(repositoryService, pattern, mappedClusters, options, detectedPatterns, result);
            return true;
        };
        try {
            repositoryService.searchContainersIterative(RoleAnalysisDetectionPatternType.class, query, handler,
                    null, result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't search role suggestions", ex);
        }

        return detectedPatterns;
    }

    public List<DetectedPattern> getClusterRoleSuggestions(
            @NotNull String clusterOid,
            @Nullable Integer limit,
            @Nullable Boolean sortDescending,
            @NotNull OperationResult result) {

        S_QueryExit sQueryExit = PrismContext.get().queryFor(RoleAnalysisDetectionPatternType.class)
                .ownedBy(RoleAnalysisClusterType.class)
                .id(clusterOid);

        if (limit != null) {
            sQueryExit = sQueryExit.maxSize(limit);
        }

        if (sortDescending != null && sortDescending.equals(Boolean.TRUE)) {
            sQueryExit = sQueryExit.desc(RoleAnalysisDetectionPatternType.F_REDUCTION_COUNT);
        } else if (sortDescending != null && sortDescending.equals(Boolean.FALSE)) {
            sQueryExit = sQueryExit.asc(RoleAnalysisDetectionPatternType.F_REDUCTION_COUNT);
        }

        ObjectQuery query = sQueryExit.build();

        List<DetectedPattern> detectedPatterns = new ArrayList<>();

        GetOperationOptionsBuilder getOperationOptionsBuilder = schemaService.getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder.build();

        Map<String, RoleAnalysisClusterType> mappedClusters = new HashMap<>();

        ContainerableResultHandler<RoleAnalysisDetectionPatternType> handler = (pattern, lResult) -> {
            loadDetectedPattern(repositoryService, pattern, mappedClusters, options, detectedPatterns, result);
            return true;
        };
        try {
            repositoryService.searchContainersIterative(RoleAnalysisDetectionPatternType.class, query, handler,
                    null, result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't search session role suggestions", ex);
        }

        return detectedPatterns;
    }

    public List<DetectedPattern> getAllRoleSuggestions(
            @Nullable Integer limit,
            @Nullable Boolean sortDescending,
            @NotNull OperationResult result) {
        List<DetectedPattern> detectedPatterns = new ArrayList<>();

        S_QueryExit sQueryExit = PrismContext.get().queryFor(RoleAnalysisDetectionPatternType.class);

        if (limit != null) {
            sQueryExit = sQueryExit.maxSize(limit);
        }

        if (sortDescending != null && sortDescending.equals(Boolean.TRUE)) {
            sQueryExit = sQueryExit.desc(RoleAnalysisDetectionPatternType.F_REDUCTION_COUNT);
        } else if (sortDescending != null && sortDescending.equals(Boolean.FALSE)) {
            sQueryExit = sQueryExit.asc(RoleAnalysisDetectionPatternType.F_REDUCTION_COUNT);
        }

        ObjectQuery query = sQueryExit.build();

        GetOperationOptionsBuilder getOperationOptionsBuilder = schemaService.getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();
        getOperationOptionsBuilder.iterationPageSize(RM_ITERATIVE_SEARCH_PAGE_SIZE);
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder.build();

        Map<String, RoleAnalysisClusterType> mappedClusters = new HashMap<>();

        ContainerableResultHandler<RoleAnalysisDetectionPatternType> handler = (pattern, lResult) -> {
            loadDetectedPattern(repositoryService, pattern, mappedClusters, options, detectedPatterns, result);
            return true;
        };
        try {
            repositoryService.searchContainersIterative(RoleAnalysisDetectionPatternType.class, query, handler,
                    getDefaultRmIterativeSearchPageSizeOptions(), result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't search cluster role suggestions", ex);
        }

        return detectedPatterns;
    }

    public List<DetectedPattern> getSessionOutlierPartitionStructure(
            @NotNull String sessionOid,
            @Nullable Integer limit,
            @Nullable Boolean sortDescending,
            @NotNull OperationResult result) {

        S_QueryExit sQueryExit = PrismContext.get().queryFor(RoleAnalysisOutlierPartitionType.class)
                .item(RoleAnalysisOutlierPartitionType.F_CLUSTER_REF, PrismConstants.T_OBJECT_REFERENCE,
                        RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF).ref(sessionOid);

        if (sortDescending != null && sortDescending.equals(Boolean.TRUE)) {
            sQueryExit = sQueryExit.desc(RoleAnalysisPartitionAnalysisType.F_OVERALL_CONFIDENCE);
        } else if (sortDescending != null && sortDescending.equals(Boolean.FALSE)) {
            sQueryExit = sQueryExit.asc(RoleAnalysisPartitionAnalysisType.F_OVERALL_CONFIDENCE);
        }

        if (limit != null) {
            sQueryExit = sQueryExit.maxSize(limit);
        }

        ObjectQuery query = sQueryExit.build();

        List<DetectedPattern> detectedPatterns = new ArrayList<>();

        GetOperationOptionsBuilder getOperationOptionsBuilder = schemaService.getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder.build();

        Map<String, RoleAnalysisClusterType> mappedClusters = new HashMap<>();

        ContainerableResultHandler<RoleAnalysisDetectionPatternType> handler = (pattern, lResult) -> {
            loadDetectedPattern(repositoryService, pattern, mappedClusters, options, detectedPatterns, result);
            return true;
        };
        try {
            repositoryService.searchContainersIterative(RoleAnalysisDetectionPatternType.class, query, handler,
                    getDefaultRmIterativeSearchPageSizeOptions(), result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't search role suggestions", ex);
        }

        return detectedPatterns;
    }

    public Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> getOutlierPartitionsMap(
            @Nullable Integer limit,
            @Nullable Boolean sortDescending,
            @NotNull Task task,
            @NotNull OperationResult result) {

        S_QueryExit sQueryExit = PrismContext.get().queryFor(RoleAnalysisOutlierPartitionType.class);

        if (limit != null) {
            sQueryExit = sQueryExit.maxSize(limit);
        }

        if (sortDescending != null && sortDescending.equals(Boolean.TRUE)) {
            sQueryExit = sQueryExit.desc(RoleAnalysisOutlierPartitionType.F_PARTITION_ANALYSIS,
                    RoleAnalysisPartitionAnalysisType.F_OVERALL_CONFIDENCE);
        } else if (sortDescending != null && sortDescending.equals(Boolean.FALSE)) {
            sQueryExit = sQueryExit.asc(RoleAnalysisOutlierPartitionType.F_PARTITION_ANALYSIS,
                    RoleAnalysisPartitionAnalysisType.F_OVERALL_CONFIDENCE);
        }

        ObjectQuery query = sQueryExit.build();

        Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> partitionOutlierMap = new LinkedHashMap<>();
        ContainerableResultHandler<RoleAnalysisOutlierPartitionType> handler = (partition, lResult) -> {
            prepareOutlierPartitionMap(this, task, result, partition, partitionOutlierMap, LOGGER);
            return true;
        };
        try {
            repositoryService.searchContainersIterative(RoleAnalysisOutlierPartitionType.class, query, handler,
                    getDefaultRmIterativeSearchPageSizeOptions(), result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't search cluster role suggestions", ex);
        }

        return partitionOutlierMap;
    }

    public Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> getSessionOutlierPartitionsMap(
            @NotNull String sessionOid,
            @Nullable Integer limit,
            @Nullable Boolean sortDescending,
            @NotNull Task task,
            @NotNull OperationResult result) {

        S_QueryExit sQueryExit = PrismContext.get().queryFor(RoleAnalysisOutlierPartitionType.class).item(
                RoleAnalysisOutlierPartitionType.F_CLUSTER_REF, PrismConstants.T_OBJECT_REFERENCE,
                RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF).ref(sessionOid);

        if (limit != null) {
            sQueryExit = sQueryExit.maxSize(limit);
        }

        if (sortDescending != null && sortDescending.equals(Boolean.TRUE)) {
            sQueryExit = sQueryExit.desc(RoleAnalysisOutlierPartitionType.F_PARTITION_ANALYSIS,
                    RoleAnalysisPartitionAnalysisType.F_OVERALL_CONFIDENCE);
        } else if (sortDescending != null && sortDescending.equals(Boolean.FALSE)) {
            sQueryExit = sQueryExit.asc(RoleAnalysisOutlierPartitionType.F_PARTITION_ANALYSIS,
                    RoleAnalysisPartitionAnalysisType.F_OVERALL_CONFIDENCE);
        }

        ObjectQuery query = sQueryExit.build();

        Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> partitionOutlierMap = new LinkedHashMap<>();
        ContainerableResultHandler<RoleAnalysisOutlierPartitionType> handler = (partition, lResult) -> {
            prepareOutlierPartitionMap(this, task, result, partition, partitionOutlierMap, LOGGER);
            return true;
        };
        try {
            repositoryService.searchContainersIterative(RoleAnalysisOutlierPartitionType.class, query, handler,
                    getDefaultRmIterativeSearchPageSizeOptions(), result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't search cluster role suggestions", ex);
        }

        return partitionOutlierMap;
    }

    public Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> getClusterOutlierPartitionsMap(
            @NotNull String clusterOid,
            @Nullable Integer limit,
            @Nullable Boolean sortDescending,
            @NotNull Task task,
            @NotNull OperationResult result) {

        S_QueryExit sQueryExit = PrismContext.get().queryFor(RoleAnalysisOutlierPartitionType.class).item(
                RoleAnalysisOutlierPartitionType.F_CLUSTER_REF).ref(clusterOid);

        if (limit != null) {
            sQueryExit = sQueryExit.maxSize(limit);
        }

        if (sortDescending != null && sortDescending.equals(Boolean.TRUE)) {
            sQueryExit = sQueryExit.desc(RoleAnalysisOutlierPartitionType.F_PARTITION_ANALYSIS,
                    RoleAnalysisPartitionAnalysisType.F_OVERALL_CONFIDENCE);
        } else if (sortDescending != null && sortDescending.equals(Boolean.FALSE)) {
            sQueryExit = sQueryExit.asc(RoleAnalysisOutlierPartitionType.F_PARTITION_ANALYSIS,
                    RoleAnalysisPartitionAnalysisType.F_OVERALL_CONFIDENCE);
        }

        ObjectQuery query = sQueryExit.build();

        Map<RoleAnalysisOutlierPartitionType, RoleAnalysisOutlierType> partitionOutlierMap = new LinkedHashMap<>();
        ContainerableResultHandler<RoleAnalysisOutlierPartitionType> handler = (partition, lResult) -> {
            prepareOutlierPartitionMap(this, task, result, partition, partitionOutlierMap, LOGGER);
            return true;
        };
        try {
            repositoryService.searchContainersIterative(RoleAnalysisOutlierPartitionType.class, query, handler,
                    getDefaultRmIterativeSearchPageSizeOptions(), result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't search cluster role suggestions", ex);
        }

        return partitionOutlierMap;
    }

    private @NotNull Collection<SelectorOptions<GetOperationOptions>> getDefaultRmIterativeSearchPageSizeOptions() {
        GetOperationOptionsBuilder optionsBuilder = schemaService.getOperationOptionsBuilder();
        optionsBuilder.iterationPageSize(RM_ITERATIVE_SEARCH_PAGE_SIZE);
        return optionsBuilder.build();
    }

    public S_FilterExit buildStatisticsAssignmentSearchFilter(@NotNull Collection<QName> memberRelations) {
        return PrismContext.get().queryFor(AssignmentType.class)
                .ownedBy(UserType.class, F_ASSIGNMENT)
                .block().item(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).eq(ActivationStatusType.ENABLED)
                .endBlock()
                .and().item(AssignmentType.F_TARGET_REF).refRelation(memberRelations.toArray(new QName[0]))
                .and()
                .exists(AssignmentType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE)
                .type(RoleType.class)
                .block().item(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).eq(ActivationStatusType.ENABLED)
                .endBlock();
    }

    @Override
    public @Nullable List<RoleAnalysisObjectCategorizationType> getObjectRoleAnalysisObjectCategorization(
            @NotNull ObjectReferenceType objectRef,
            @NotNull String sessionOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        String targetObjectOid = objectRef.getOid();
        QName targetObjectType = objectRef.getType();
        Objects.requireNonNull(targetObjectOid, "ObjectRef oid must not be null");
        Objects.requireNonNull(targetObjectType, "ObjectRef type must not be null");

        @Nullable PrismObject<RoleAnalysisSessionType> sessionObject = getSessionTypeObject(sessionOid, task, result);
        if (sessionObject == null) {
            return null;
        }

        RoleAnalysisSessionType session = sessionObject.asObjectable();
        RoleAnalysisIdentifiedCharacteristicsType identifiedCharacteristics = session.getIdentifiedCharacteristics();

        if (targetObjectType.equals(UserType.COMPLEX_TYPE)) {
            RoleAnalysisIdentifiedCharacteristicsItemsType users = identifiedCharacteristics.getUsers();
            if (users == null) {
                return null;
            }

            for (RoleAnalysisIdentifiedCharacteristicsItemType user : users.getItem()) {
                if (user.getObjectRef().getOid().equals(targetObjectOid)) {
                    return user.getCategory();
                }
            }
        } else if (targetObjectType.equals(RoleType.COMPLEX_TYPE)) {
            RoleAnalysisIdentifiedCharacteristicsItemsType roles = identifiedCharacteristics.getRoles();
            if (roles == null) {
                return null;
            }

            for (RoleAnalysisIdentifiedCharacteristicsItemType role : roles.getItem()) {
                if (role.getObjectRef().getOid().equals(targetObjectOid)) {
                    return role.getCategory();
                }
            }
        }

        return null;
    }

    //TODO this is temporary solution

    /**
     * Prepares a temporary cluster for role analysis based on the provided outlier and partition.
     *
     * @param outlier The outlier object containing the detected outlier information.
     * @param partition The partition object containing the partition analysis data.
     * @param displayValueOption The display value options for the role analysis.
     * @param task The task in which the operation is performed.
     * @return A RoleAnalysisClusterType object representing the prepared temporary cluster,
     * or null if the similar object analysis is not available.
     */
    public @Nullable RoleAnalysisClusterType prepareTemporaryCluster(
            @NotNull RoleAnalysisOutlierType outlier,
            @NotNull RoleAnalysisOutlierPartitionType partition,
            @NotNull DisplayValueOption displayValueOption,
            @NotNull Task task) {
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        RoleAnalysisOutlierSimilarObjectsAnalysisResult similarObjectAnalysis = partitionAnalysis.getSimilarObjectAnalysis();
        if (similarObjectAnalysis == null) {
            return null;
        }
        List<ObjectReferenceType> similarObjects = similarObjectAnalysis.getSimilarObjects();
        Set<String> similarObjectOids = similarObjects.stream().map(ObjectReferenceType::getOid).collect(Collectors.toSet());
        String sessionOid = partition.getTargetSessionRef().getOid();
        String userOid = outlier.getObjectRef().getOid();

        OperationResult result = task.getResult();

        PrismObject<RoleAnalysisSessionType> sessionObject = this.getSessionTypeObject(sessionOid, task, result);

        if (sessionObject == null) {
            LOGGER.error("Session object is null");
            return null;
        }

        RoleAnalysisSessionType session = sessionObject.asObjectable();
        RoleAnalysisDetectionOptionType defaultDetectionOption = session.getDefaultDetectionOption();

        double minFrequency = 2;
        double maxFrequency = 2;

        if (defaultDetectionOption != null && defaultDetectionOption.getStandardDeviation() != null) {
            RangeType frequencyRange = defaultDetectionOption.getStandardDeviation();
            if (frequencyRange.getMin() != null) {
                minFrequency = frequencyRange.getMin().intValue();
            }
            if (frequencyRange.getMax() != null) {
                maxFrequency = frequencyRange.getMax().intValue();
            }
        }

        displayValueOption.setProcessMode(RoleAnalysisProcessModeType.USER);
        displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND);
        displayValueOption.setSortMode(RoleAnalysisSortMode.JACCARD);
        displayValueOption.setChunkAction(RoleAnalysisChunkAction.EXPLORE_DETECTION);
        RoleAnalysisClusterType cluster = new RoleAnalysisClusterType();
        for (String element : similarObjectOids) {
            cluster.getMember().add(new ObjectReferenceType()
                    .oid(element).type(UserType.COMPLEX_TYPE));
        }

        cluster.setRoleAnalysisSessionRef(
                new ObjectReferenceType()
                        .type(RoleAnalysisSessionType.COMPLEX_TYPE)
                        .oid(sessionOid)
                        .targetName(session.getName()));

        UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
        SearchFilterType userSearchFilter = userModeOptions.getUserSearchFilter();
        SearchFilterType roleSearchFilter = userModeOptions.getRoleSearchFilter();
        SearchFilterType assignmentSearchFilter = userModeOptions.getAssignmentSearchFilter();

        RoleAnalysisDetectionOptionType detectionOption = new RoleAnalysisDetectionOptionType();
        detectionOption.setStandardDeviation(new RangeType().min(minFrequency).max(maxFrequency));
        cluster.setDetectionOption(detectionOption);

        MiningOperationChunk miningOperationChunk = this.prepareBasicChunkStructure(cluster,
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                displayValueOption, RoleAnalysisProcessModeType.USER, null, result, task);

        RangeType standardDeviation = detectionOption.getStandardDeviation();
        Double sensitivity = detectionOption.getSensitivity();
        Double frequencyThreshold = detectionOption.getFrequencyThreshold();

        RoleAnalysisSortMode sortMode = displayValueOption.getSortMode();
        if (sortMode == null) {
            displayValueOption.setSortMode(RoleAnalysisSortMode.NONE);
            sortMode = RoleAnalysisSortMode.NONE;
        }

        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(sortMode);

        if (standardDeviation != null) {
            this.resolveOutliersZScore(roles, standardDeviation, sensitivity, frequencyThreshold);
        }

        cluster.setClusterStatistics(new AnalysisClusterStatisticType()
                .rolesCount(roles.size())
                .usersCount(similarObjectOids.size()));

        cluster.setDescription(userOid);
        return cluster;
    }

}

