/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining;

import static com.evolveum.midpoint.common.mining.utils.algorithm.JaccardSorter.jacquardSimilarity;

import static com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.ClusteringUtils.loadUserBasedMultimapData;

import static java.util.Collections.singleton;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.createAttributeMap;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.*;
import static com.evolveum.midpoint.model.impl.mining.analysis.AttributeAnalysisUtil.*;
import static com.evolveum.midpoint.model.impl.mining.utils.RoleAnalysisUtils.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_MODIFY_TIMESTAMP;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.*;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
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
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Utility methods for working with role analysis objects in the Midpoint system.
 */
@Component
public class RoleAnalysisServiceImpl implements RoleAnalysisService, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisServiceImpl.class);

    transient @Autowired ModelService modelService;
    transient @Autowired RepositoryService repositoryService;

    @Override
    public ModelService getModelService() {
        return modelService;
    }

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
            @Nullable ObjectFilter userFilter,
            @NotNull Set<String> clusterMembers,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ListMultimap<String, String> roleMemberCache = ArrayListMultimap.create();

        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .exists(AssignmentHolderType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(clusterMembers.toArray(new String[0]))
                .endBlock().build();

        if (userFilter != null) {
            query.addFilter(userFilter);
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
        modelService.importObject(clusterPrismObject, null, task, result);
    }

    @Override
    public void updateSessionStatistics(
            @NotNull ObjectReferenceType sessionRef,
            @NotNull RoleAnalysisSessionStatisticType sessionStatistic,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {

            ObjectDelta<RoleAnalysisSessionType> delta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_SESSION_STATISTIC)
                    .replace(sessionStatistic)
                    .asObjectDelta(sessionRef.getOid());

            modelService.executeChanges(singleton(delta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't modify  RoleAnalysisSessionType {}", sessionRef, e);
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
            max = Math.max(max, clusterDetectionType.getClusterMetric());
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
        analysisClusterStatisticType.setRoleAttributeAnalysisResult(clusterStatistics.getRoleAttributeAnalysisResult().clone());
        analysisClusterStatisticType.setUserAttributeAnalysisResult(clusterStatistics.getUserAttributeAnalysisResult().clone());
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
            @NotNull OperationResult result) {
        ResultHandler<RoleAnalysisClusterType> resultHandler = (object, parentResult) -> {
            try {
                deleteCluster(object.asObjectable(), task, result);
            } catch (Exception e) {
                throw new RuntimeException(e);
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
    public void deleteCluster(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result) {
        String clusterOid = cluster.getOid();
        PrismObject<RoleAnalysisSessionType> sessionObject = getSessionTypeObject(
                cluster.getRoleAnalysisSessionRef().getOid(), task, result
        );

        if (sessionObject == null) {
            return;
        }

        try {

            ObjectDelta<RoleAnalysisClusterType> deleteDelta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(RoleAnalysisClusterType.class, clusterOid);

            modelService.executeChanges(singleton(deleteDelta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't delete RoleAnalysisClusterType {}", clusterOid, e);
        }

        try {

            // FIXME
            ObjectDelta<RoleAnalysisSessionType> delta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .asObjectDelta(sessionObject.getOid());

            modelService.executeChanges(singleton(delta), null, task, result);

            recomputeSessionStatics(sessionObject.getOid(), cluster, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't recompute RoleAnalysisSessionStatistic {}", sessionObject.getOid(), e);
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

    @NotNull
    private static RoleAnalysisSessionStatisticType prepareRoleAnalysisSessionStatistic(
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
                    throw new RuntimeException("Couldn't prepare role for cache", e);
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
        if (user == null) {
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
                    throw new RuntimeException("Couldn't prepare user for cache", e);
                }
            }

            userExistCache.put(userOid, user);
        }

        return user;
    }

    @Override
    public @NotNull Integer countUserTypeMembers(
            @Nullable ObjectFilter userFilter,
            @NotNull String objectId,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .exists(AssignmentHolderType.F_ASSIGNMENT)
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
    public @NotNull PrismObject<RoleType> generateBusinessRole(
            @NotNull Set<AssignmentType> assignmentTypes,
            @NotNull PolyStringType name) {
        PrismObject<RoleType> roleTypePrismObject = null;
        try {
            roleTypePrismObject = modelService.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while finding object definition by compile time class ClusterType object: {}", e.getMessage(), e);
        }

        assert roleTypePrismObject != null;

        RoleType role = roleTypePrismObject.asObjectable();
        role.setName(name);

        if (!assignmentTypes.isEmpty()) {
            role.getInducement().addAll(assignmentTypes);
        }
        role.getAssignment().add(ObjectTypeUtil.createAssignmentTo(SystemObjectsType.ARCHETYPE_BUSINESS_ROLE.value(),
                ObjectTypes.ARCHETYPE));

        return roleTypePrismObject;
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
            RoleAnalysisSessionType session = prismSession.asObjectable();
            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
            RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
            if (analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS)) {
                deleteAllOutliers(task, result);
            }

            deleteSessionClustersMembers(sessionOid, task, result);

            ObjectDelta<AssignmentHolderType> deleteDelta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(AssignmentHolderType.class, sessionOid);

            modelService.executeChanges(singleton(deleteDelta), null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't delete RoleAnalysisSessionType {}", sessionOid, e);
        }
    }

    public void deleteAllOutliers(@NotNull Task task,
            @NotNull OperationResult result) {
        ResultHandler<RoleAnalysisOutlierType> resultHandler = (object, parentResult) -> {
            deleteOutlier(object.asObjectable(), task, result);
            return true;
        };

        try {
            modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, null, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't delete outliers", ex);
        } finally {
            result.recomputeStatus();
        }
    }

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
            @NotNull DetectionOption detectionOption,
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
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task) {
        return new CompressedMiningStructure().executeOperation(this,
                cluster, fullProcess, processMode, result, task);
    }

    @Override
    public @NotNull MiningOperationChunk prepareMiningStructure(@NotNull RoleAnalysisClusterType cluster,
            @NotNull DisplayValueOption option,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task) {

        RoleAnalysisChunkMode chunkMode = option.getChunkMode();
        List<MiningUserTypeChunk> miningUserTypeChunks;
        List<MiningRoleTypeChunk> miningRoleTypeChunks;

        if (chunkMode.equals(RoleAnalysisChunkMode.EXPAND_ROLE)) {
            miningRoleTypeChunks = new ExpandedMiningStructure()
                    .executeOperation(this, cluster, true, processMode, result, task, option)
                    .getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);
            miningUserTypeChunks = new CompressedMiningStructure()
                    .executeOperation(this, cluster, true, processMode, result, task)
                    .getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
        } else if (chunkMode.equals(RoleAnalysisChunkMode.EXPAND_USER)) {
            miningRoleTypeChunks = new CompressedMiningStructure()
                    .executeOperation(this, cluster, true, processMode, result, task)
                    .getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);
            miningUserTypeChunks = new ExpandedMiningStructure()
                    .executeOperation(this, cluster, true, processMode, result, task, option)
                    .getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
        } else if (chunkMode.equals(RoleAnalysisChunkMode.COMPRESS)) {
            return new CompressedMiningStructure()
                    .executeOperation(this, cluster, true, processMode, result, task);
        } else {
            return new ExpandedMiningStructure()
                    .executeOperation(this, cluster, true, processMode, result, task, option);
        }

        return new MiningOperationChunk(miningUserTypeChunks, miningRoleTypeChunks);
    }

    @Override
    public @NotNull MiningOperationChunk prepareExpandedMiningStructure(
            @NotNull RoleAnalysisClusterType cluster,
            boolean fullProcess,
            @NotNull RoleAnalysisProcessModeType processMode,
            @NotNull OperationResult result,
            @NotNull Task task,
            @Nullable DisplayValueOption option) {
        return new ExpandedMiningStructure().executeOperation(this, cluster, fullProcess,
                processMode, result, task, option);
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
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull TaskType processingTask) {

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
                    .type(RoleAnalysisSessionType.COMPLEX_TYPE);

            RoleAnalysisClusteringWorkDefinitionType rdw = new RoleAnalysisClusteringWorkDefinitionType();
            rdw.setSessionRef(objectReferenceType);

            ActivityDefinitionType activity = new ActivityDefinitionType()
                    .work(new WorkDefinitionsType()
                            .roleAnalysisClustering(rdw));

            processingTask.setName(Objects.requireNonNullElseGet(
                    taskName, () -> PolyStringType.fromOrig("Session clustering  (" + session + ")")));

            if (taskOid != null) {
                processingTask.setOid(taskOid);
            } else {
                taskOid = UUID.randomUUID().toString();
                processingTask.setOid(taskOid);
            }

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
    public void executeMigrationTask(
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
            LOGGER.warn("Couldn't start migration. Some process is already in progress.: " + cluster.getOid());
            return;
        }

        try {

            TaskType taskObject = new TaskType();

            taskObject.setName(Objects.requireNonNullElseGet(
                    taskName, () -> PolyStringType.fromOrig("Migration role (" + roleObject.getName().toString() + ")")));

            if (taskOid != null) {
                taskObject.setOid(taskOid);
            } else {
                taskOid = UUID.randomUUID().toString();
                taskObject.setOid(taskOid);
            }

            modelInteractionService.submit(
                    activityDefinition,
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
                    RoleAnalysisOperation.MIGRATION, focus, LOGGER, task, result
            );

            try {
                ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleType.class)
                        .item(RoleType.F_LIFECYCLE_STATE).replace("active")
                        .asObjectDelta(roleObject.getOid());

                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

                modelService.executeChanges(deltas, null, task, result);

            } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException |
                    ExpressionEvaluationException |
                    CommunicationException | ConfigurationException | PolicyViolationException |
                    SecurityViolationException e) {
                LOGGER.error("Couldn't update lifecycle state of object RoleType {}", cluster.getOid(), e);
            }

            try {
                List<ItemDelta<?, ?>> modifications = new ArrayList<>();

                modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                        .item(RoleAnalysisClusterType.F_DETECTED_PATTERN).replace(Collections.emptyList())
                        .asItemDelta());

                repositoryService.modifyObject(RoleAnalysisClusterType.class, cluster.getOid(), modifications, result);
            } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
                LOGGER.error("Couldn't execute migration recompute RoleAnalysisClusterDetectionOptions {}", cluster.getOid(), e);
            }

        } catch (CommonException e) {
            LOGGER.error("Failed to execute role {} migration activity: ", roleObject.getOid(), e);
        }
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
                        .item(RoleType.F_INDUCEMENT)
                        .add(getAssignmentTo(inducementForAdd))
                        .asObjectDelta(candidateRoleOid);
                Collection<ObjectDelta<? extends ObjectType>> deltas2 = MiscSchemaUtil.createCollection(roleD);
                modelService.executeChanges(deltas2, null, task, result);

            }

            for (String unassignedRole : unassignedRoles) {
                ObjectDelta<RoleType> roleD = PrismContext.get().deltaFor(RoleType.class)
                        .item(RoleType.F_INDUCEMENT)
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

    @NotNull
    private static AssignmentType getAssignmentTo(String unassignedRole) {
        return createAssignmentTo(unassignedRole, ObjectTypes.ROLE);
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
                new HashSet<>(Collections.singleton(objectOid)),
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
            if (userAttributeAnalysisStructures != null) {
                RoleAnalysisAttributeAnalysisResult userAnalysis = new RoleAnalysisAttributeAnalysisResult();
                for (AttributeAnalysisStructure userAttributeAnalysisStructure : userAttributeAnalysisStructures) {
                    double density = userAttributeAnalysisStructure.getDensity();
                    if (density == 0) {
                        continue;
                    }
                    RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
                    roleAnalysisAttributeAnalysis.setDensity(density);
                    roleAnalysisAttributeAnalysis.setItemPath(userAttributeAnalysisStructure.getItemPath());
                    roleAnalysisAttributeAnalysis.setIsMultiValue(userAttributeAnalysisStructure.isMultiValue());
                    roleAnalysisAttributeAnalysis.setDescription(userAttributeAnalysisStructure.getDescription());
                    List<RoleAnalysisAttributeStatistics> attributeStatistics = userAttributeAnalysisStructure.getAttributeStatistics();
                    for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                        roleAnalysisAttributeAnalysis.getAttributeStatistics().add(attributeStatistic);
                    }

                    userAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
                }

                detectedPattern.setUserAttributeAnalysisResult(userAnalysis);
            }

            if (roleAttributeAnalysisStructures != null) {
                RoleAnalysisAttributeAnalysisResult roleAnalysis = new RoleAnalysisAttributeAnalysisResult();
                for (AttributeAnalysisStructure roleAttributeAnalysisStructure : roleAttributeAnalysisStructures) {
                    double density = roleAttributeAnalysisStructure.getDensity();
                    if (density == 0) {
                        continue;
                    }
                    RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
                    roleAnalysisAttributeAnalysis.setDensity(density);
                    roleAnalysisAttributeAnalysis.setItemPath(roleAttributeAnalysisStructure.getItemPath());
                    roleAnalysisAttributeAnalysis.setIsMultiValue(roleAttributeAnalysisStructure.isMultiValue());
                    roleAnalysisAttributeAnalysis.setDescription(roleAttributeAnalysisStructure.getDescription());
                    List<RoleAnalysisAttributeStatistics> attributeStatistics = roleAttributeAnalysisStructure.getAttributeStatistics();
                    for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                        roleAnalysisAttributeAnalysis.getAttributeStatistics().add(attributeStatistic);
                    }
                    roleAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
                }

                detectedPattern.setRoleAttributeAnalysisResult(roleAnalysis);
            }
        }
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

        return null;
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
            throw new RuntimeException("Couldn't count objects of type " + type + ": " + e.getMessage(), e);
        }
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
        if (userAttributeAnalysisResult != null && processModeType.equals(RoleAnalysisProcessModeType.USER)) {
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
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null) {
            return null;
        }
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        if (processMode == null) {
            return null;
        }

        AnalysisAttributeSettingType analysisAttributeSetting = null;

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            RoleAnalysisSessionOptionType roleModeOptions = session.getRoleModeOptions();
            if (roleModeOptions == null) {
                return null;
            }
            analysisAttributeSetting = roleModeOptions.getAnalysisAttributeSetting();
        } else if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
            if (userModeOptions == null) {
                return null;
            }
            analysisAttributeSetting = userModeOptions.getAnalysisAttributeSetting();
        }

        if (analysisAttributeSetting == null) {
            return null;
        }

        List<AnalysisAttributeRuleType> analysisAttributeRule = analysisAttributeSetting.getAnalysisAttributeRule();

        if (analysisAttributeRule == null || analysisAttributeRule.isEmpty()) {
            return null;
        }

        Map<String, RoleAnalysisAttributeDef> attributeMap = createAttributeMap();
        List<RoleAnalysisAttributeDef> attributeDefs = new ArrayList<>();

        for (AnalysisAttributeRuleType rule : analysisAttributeRule) {
            if (!rule.getPropertyType().equals(complexType)) {
                continue;
            }

            String key = rule.getAttributeIdentifier();
            RoleAnalysisAttributeDef attributeDef = attributeMap.get(key);
            if (attributeDef != null) {
                attributeDefs.add(attributeDef);
            }
        }
        return attributeDefs;
    }

    @Override
    public @Nullable RoleAnalysisAttributeAnalysisResult resolveSimilarAspect(
            @NotNull RoleAnalysisAttributeAnalysisResult compared,
            @NotNull RoleAnalysisAttributeAnalysisResult comparison) {
        Objects.requireNonNull(compared);
        Objects.requireNonNull(comparison);

        RoleAnalysisAttributeAnalysisResult outlierAttributeAnalysisResult = new RoleAnalysisAttributeAnalysisResult();
        List<RoleAnalysisAttributeAnalysis> attributeAnalysis = comparison.getAttributeAnalysis();

        for (RoleAnalysisAttributeAnalysis clusterAnalysis : attributeAnalysis) {
            String clusterItemPath = clusterAnalysis.getItemPath();
            Set<String> outlierValues = extractCorrespondingOutlierValues(compared, clusterItemPath);
            if (outlierValues == null) {
                continue;
            }

            RoleAnalysisAttributeAnalysis correspondingAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
            correspondingAttributeAnalysis.setItemPath(clusterItemPath);

            int counter = 0;
            int sum = 0;
            List<RoleAnalysisAttributeStatistics> attributeStatistics = clusterAnalysis.getAttributeStatistics();
            for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                String clusterAttributeValue = attributeStatistic.getAttributeValue();
                Integer inGroup = attributeStatistic.getInGroup();
                sum += inGroup != null ? inGroup : 0;
                if (outlierValues.contains(clusterAttributeValue)) {
                    counter += inGroup != null ? inGroup : 0;
                    correspondingAttributeAnalysis.getAttributeStatistics().add(attributeStatistic.clone());
                }
            }
            double newDensity = (double) counter / sum * 100;
            correspondingAttributeAnalysis.setDensity(newDensity);

            outlierAttributeAnalysisResult.getAttributeAnalysis().add(correspondingAttributeAnalysis.clone());
        }

        return outlierAttributeAnalysisResult;
    }

    @NotNull
    public RoleAnalysisAttributeAnalysisResult resolveUserAttributes(
            @NotNull PrismObject<UserType> prismUser,
            @NotNull List<RoleAnalysisAttributeDef> attributesForUserAnalysis) {
        RoleAnalysisAttributeAnalysisResult outlierCandidateAttributeAnalysisResult = new RoleAnalysisAttributeAnalysisResult();

        for (RoleAnalysisAttributeDef item : attributesForUserAnalysis) {
            RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
            roleAnalysisAttributeAnalysis.setItemPath(item.getDisplayValue());
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

    @Override
    public RoleAnalysisAttributeAnalysisResult resolveRoleMembersAttribute(
            @NotNull String objectOid,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull List<RoleAnalysisAttributeDef> attributeDefSet) {

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        this.extractUserTypeMembers(
                userExistCache, null,
                new HashSet<>(Collections.singleton(objectOid)),
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
            RoleAnalysisAttributeAnalysis roleAnalysisAttributeAnalysis = new RoleAnalysisAttributeAnalysis();
            roleAnalysisAttributeAnalysis.setDensity(density);
            roleAnalysisAttributeAnalysis.setItemPath(userAttributeAnalysisStructure.getItemPath());
            roleAnalysisAttributeAnalysis.setIsMultiValue(userAttributeAnalysisStructure.isMultiValue());
            roleAnalysisAttributeAnalysis.setDescription(userAttributeAnalysisStructure.getDescription());
            List<RoleAnalysisAttributeStatistics> attributeStatistics = userAttributeAnalysisStructure.getAttributeStatistics();
            for (RoleAnalysisAttributeStatistics attributeStatistic : attributeStatistics) {
                roleAnalysisAttributeAnalysis.getAttributeStatistics().add(attributeStatistic);
            }

            userAnalysis.getAttributeAnalysis().add(roleAnalysisAttributeAnalysis);
        }

        return userAnalysis;
    }

    public <T extends MiningBaseTypeChunk> ZScoreData resolveOutliersZScore(
            @NotNull List<T> data,
            @Nullable RangeType range,
            @Nullable Double sensitivity) {

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
            if (zScore <= -negativeThreshold) {
                item.getFrequencyItem().setNegativeExclude();
            } else if (zScore >= positiveThreshold) {
                item.getFrequencyItem().setPositiveExclude();
            } else {
                item.getFrequencyItem().setInclude();
            }
        }

        //TODO experiment
        resolveNeighbours(data);
        return zScoreData;
    }

    public <T extends MiningBaseTypeChunk> void resolveNeighbours(@NotNull List<T> data) {
//        List<T> negativeExcludeChunks = new ArrayList<>();
        ListMultimap<FrequencyItem.Status, T> itemMap = ArrayListMultimap.create();
        for (T chunk : data) {
            double status = chunk.getFrequencyItem().getzScore();
            if (status <= -1) {
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
        modelService.importObject(outlier.asPrismObject(), null, task, result);
    }

    public void resolveOutliers(
            @NotNull RoleAnalysisOutlierType roleAnalysisOutlierType,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisSessionType session,
            @NotNull RoleAnalysisClusterType cluster,
            double requiredConfidence) {
        //TODO TARGET OBJECT REF IS NECESSARY (check git history)

        Double clusterConfidence = roleAnalysisOutlierType.getClusterConfidence();
        if (clusterConfidence == null) {
            clusterConfidence = 0.0;
        }
        if (clusterConfidence < requiredConfidence) {
            return;
        }

        roleAnalysisOutlierType.setTargetSessionRef(new ObjectReferenceType()
                .oid(session.getOid())
                .targetName(session.getName())
                .type(RoleAnalysisSessionType.COMPLEX_TYPE));

        roleAnalysisOutlierType.setTargetClusterRef(new ObjectReferenceType()
                .oid(cluster.getOid())
                .targetName(cluster.getName())
                .type(RoleAnalysisClusterType.COMPLEX_TYPE));

        ObjectReferenceType targetObjectRef = roleAnalysisOutlierType.getTargetObjectRef();
        PrismObject<FocusType> object = this
                .getObject(FocusType.class, targetObjectRef.getOid(), task, result);

        roleAnalysisOutlierType.setName(object != null && object.getName() != null
                ? PolyStringType.fromOrig(object.getName() + " (outlier)")
                : PolyStringType.fromOrig("outlier_" + session.getName() + "_" + UUID.randomUUID()));

        this.importOutlier(roleAnalysisOutlierType, task, result);

    }

    @Override
    @NotNull
    public List<DetectedPattern> findTopPatters(
            @NotNull Task task,
            @NotNull OperationResult result) {
        SearchResultList<PrismObject<RoleAnalysisClusterType>> clusterSearchResult;
        try {
            clusterSearchResult = modelService.searchObjects(RoleAnalysisClusterType.class, null, null, task, result);
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException |
                ConfigurationException |
                ExpressionEvaluationException e) {
            throw new RuntimeException(e);
        }
        if (clusterSearchResult == null) {
            return new ArrayList<>();
        }

        List<DetectedPattern> topDetectedPatterns = new ArrayList<>();
        for (PrismObject<RoleAnalysisClusterType> prismObject : clusterSearchResult) {
            List<DetectedPattern> detectedPatterns = transformDefaultPattern(prismObject.asObjectable());

            DetectedPattern topDetectedPattern = findPatternWithBestConfidence(detectedPatterns);
            if (topDetectedPattern != null) {
                topDetectedPatterns.add(topDetectedPattern);
            }

        }

        topDetectedPatterns.sort(Comparator.comparing(DetectedPattern::getMetric).reversed());
        return topDetectedPatterns;
    }

    @Nullable
    private static DetectedPattern findPatternWithBestConfidence(List<DetectedPattern> detectedPatterns) {
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
                        .item(RoleAnalysisSessionType.F_EFFECTIVE_MARK_REF)
                        .delete(effectiveMarkRef.get(0).asReferenceValue().clone())
                        .asObjectDelta(session.getOid());
                Collection<ObjectDelta<? extends ObjectType>> collection = MiscSchemaUtil.createCollection(clearDelta);
                modelService.executeChanges(collection, null, task, result);
            }

            ObjectDelta<RoleAnalysisSessionType> addDelta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_EFFECTIVE_MARK_REF).add(newMarkRef.asReferenceValue().clone())
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
                        .item(RoleAnalysisSessionType.F_EFFECTIVE_MARK_REF).delete(effectiveMarkRef.get(0).asReferenceValue().clone())
                        .asObjectDelta(session.getOid());
                Collection<ObjectDelta<? extends ObjectType>> collectionClear = MiscSchemaUtil.createCollection(clearDelta);
                modelService.executeChanges(collectionClear, null, task, result);

                ObjectReferenceType mark = new ObjectReferenceType().oid("00000000-0000-0000-0000-000000000801")
                        .type(MarkType.COMPLEX_TYPE)
                        .description("First run");

                ObjectDelta<RoleAnalysisSessionType> addDelta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                        .item(RoleAnalysisSessionType.F_EFFECTIVE_MARK_REF).add(mark.asReferenceValue().clone())
                        .asObjectDelta(session.getOid());
                Collection<ObjectDelta<? extends ObjectType>> collectionAdd = MiscSchemaUtil.createCollection(addDelta);
                modelService.executeChanges(collectionAdd, null, task, result);
            }

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't modify RoleAnalysisClusterType {}", session.getOid(), e);
        }
    }

    @Override
    public List<DetectedPattern> getTopSessionPattern(
            @NotNull RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result,
            boolean single) {
        List<PrismObject<RoleAnalysisClusterType>> sessionClusters = this.searchSessionClusters(session, task, result);
        if (sessionClusters == null || sessionClusters.isEmpty()) {
            return null;
        }

        List<DetectedPattern> topDetectedPatterns = new ArrayList<>();
        for (PrismObject<RoleAnalysisClusterType> prismObject : sessionClusters) {
            List<DetectedPattern> detectedPatterns = transformDefaultPattern(prismObject.asObjectable());

            DetectedPattern topDetectedPattern = findPatternWithBestConfidence(detectedPatterns);
            if (topDetectedPattern != null) {
                topDetectedPatterns.add(topDetectedPattern);
            }
        }

        if (!single) {
            return topDetectedPatterns;
        } else {
            DetectedPattern detectedPattern = findMultiplePatternWithBestConfidence(topDetectedPatterns);
            return Collections.singletonList(detectedPattern);
        }
    }

    @Nullable
    private static DetectedPattern findMultiplePatternWithBestConfidence(
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

    @Override
    public List<String> findJaccardCloseObject(
            @NotNull String userOid,
            @NotNull ListMultimap<List<String>, String> chunkMap,
            @NotNull MutableDouble usedFrequency,
            @NotNull List<String> outliersMembers,
            double minThreshold, int minMembers,
            @NotNull Task task, @NotNull OperationResult result) {
        PrismObject<UserType> userTypeObject = this.getUserTypeObject(userOid, task, result);
        if (userTypeObject == null) {
            return new ArrayList<>();
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

        List<Double> sortedKeys = new ArrayList<>(similarityStats.keySet());
        sortedKeys.sort(Collections.reverseOrder());

        for (Double similarityScore : sortedKeys) {
            List<String> elements = similarityStats.get(similarityScore);
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
            List<String> outliersMembers,
            int minRolesOccupancy,
            int maxRolesOccupancy,
            @Nullable SearchFilterType query,
            @NotNull OperationResult result,
            @NotNull Task task) {
        ListMultimap<List<String>, String> listStringListMultimap = loadUserBasedMultimapData(
                roleAnalysisService.getModelService(), minRolesOccupancy, maxRolesOccupancy, query, task, result);

        Iterator<Map.Entry<List<String>, String>> iterator = listStringListMultimap.entries().iterator();
        while (iterator.hasNext()) {
            Map.Entry<List<String>, String> entry = iterator.next();
            List<String> key = entry.getKey();

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

        return 1 - (sensitivity * 0.01);
    }

    @NotNull
    public List<RoleAnalysisOutlierType> findClusterOutliers(
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result) {
        List<RoleAnalysisOutlierType> searchResultList = new ArrayList<>();
        String clusterOid = cluster.getOid();
        ResultHandler<RoleAnalysisOutlierType> resultHandler = (outlier, lResult) -> {

            RoleAnalysisOutlierType outlierObject = outlier.asObjectable();
            ObjectReferenceType targetClusterRef = outlierObject.getTargetClusterRef();
            String oid = targetClusterRef.getOid();
            if (clusterOid.equals(oid)) {
                searchResultList.add(outlier.asObjectable());
            }
            return true;
        };

        try {
            modelService.searchObjectsIterative(RoleAnalysisOutlierType.class, null, resultHandler,
                    null, task, result);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't search outliers", ex);
        }
        return searchResultList;
    }

}

