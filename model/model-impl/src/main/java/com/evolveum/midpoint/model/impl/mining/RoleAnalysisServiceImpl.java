/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.createAttributeMap;

import static java.util.Collections.singleton;

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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisCacheOption;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectState;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
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
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
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

    @Autowired ModelService modelService;
    @Autowired RepositoryService repositoryService;

    @Override
    public @Nullable PrismObject<UserType> getUserTypeObject(
            @NotNull String oid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return modelService.getObject(UserType.class, oid, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get UserType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
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
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get FocusType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
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
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get RoleType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
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
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                    "Couldn't get object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
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
        analysisClusterStatisticType.setMembershipRange(clusterStatistics.getMembershipRange());
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
            deleteSessionClustersMembers(sessionOid, task, result);

            ObjectDelta<AssignmentHolderType> deleteDelta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(AssignmentHolderType.class, sessionOid);

            modelService.executeChanges(singleton(deleteDelta), null, task, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't delete RoleAnalysisSessionType {}", sessionOid, e);
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
    public void updateClusterPatterns(
            @NotNull String clusterRefOid,
            @NotNull Task task,
            @NotNull OperationResult result) {
        PrismObject<RoleAnalysisClusterType> cluster = getClusterTypeObject(clusterRefOid,
                task, result);
        if (cluster == null) {
            LOGGER.error("Failed to resolve RoleAnalysisCluster OBJECT from UUID: {}", clusterRefOid);
            return;
        }
        RoleAnalysisClusterType clusterObject = cluster.asObjectable();

        RoleAnalysisProcessModeType processMode = resolveClusterOptionType(cluster, task, result).getProcessMode();

        if (processMode == null) {
            LOGGER.error("Failed to resolve processMode from RoleAnalysisCluster object: {}", clusterRefOid);
            return;
        }

        List<ObjectReferenceType> clusterMember = clusterObject.getMember();
        Set<String> clusterMembersOid = clusterMember.stream()
                .map(ObjectReferenceType::getOid)
                .collect(Collectors.toSet());

        Double reductionMetric = 0.0;

        Collection<RoleAnalysisDetectionPatternType> detectedPattern = clusterObject.getDetectedPattern();

        Set<String> clusterPropertiesOid = new HashSet<>();

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
            List<ObjectReferenceType> resolvedPattern = clusterObject.getResolvedPattern();

            ListMultimap<String, String> map = extractUserTypeMembers(userExistCache,
                    null, clusterMembersOid, task, result);

            clusterPropertiesOid.addAll(userExistCache.keySet());

            reductionMetric = removeRedundantPatterns(
                    this,
                    detectedPattern,
                    clusterPropertiesOid,
                    clusterMembersOid,
                    map,
                    resolvedPattern,
                    task,
                    result);

        } else if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            for (String userOid : clusterMembersOid) {
                PrismObject<UserType> userTypeObject = getUserTypeObject(userOid, task, result);

                if (userTypeObject == null) {
                    continue;
                }

                UserType user = userTypeObject.asObjectable();
                List<AssignmentType> assignment = user.getAssignment();
                for (AssignmentType assignmentType : assignment) {
                    if (assignmentType.getTargetRef() != null) {
                        clusterPropertiesOid.add(assignmentType.getTargetRef().getOid());
                    }
                }
            }

            Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();

            List<ObjectReferenceType> resolvedPattern = clusterObject.getResolvedPattern();
            Set<String> resolvedPatternOid = resolvedPattern.stream()
                    .map(ObjectReferenceType::getOid)
                    .collect(Collectors.toSet());

            ListMultimap<String, String> map = extractUserTypeMembers(userExistCache,
                    null, resolvedPatternOid, task, result);

            reductionMetric = removeRedundantPatterns(this,
                    detectedPattern,
                    clusterMembersOid,
                    clusterPropertiesOid,
                    map,
                    resolvedPattern,
                    task,
                    result);

        }

        Collection<PrismContainerValue<?>> collection = new ArrayList<>();

        for (RoleAnalysisDetectionPatternType clusterDetectionType : detectedPattern) {
            collection.add(clusterDetectionType.asPrismContainerValue().clone());
        }

        try {

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTED_PATTERN).replace(collection)
                    .asItemDelta());

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_CLUSTER_STATISTICS, AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC)
                    .replace(reductionMetric)
                    .asItemDelta());

            repositoryService.modifyObject(RoleAnalysisClusterType.class, clusterRefOid, modifications, result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            LOGGER.error("Couldn't execute migration recompute RoleAnalysisClusterDetectionOptions {}", clusterRefOid, e);
        }
    }

    @Override
    public void executeClusteringTask(
            @NotNull ModelInteractionService modelInteractionService, @NotNull PrismObject<RoleAnalysisSessionType> session,
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result) {

        String state = recomputeAndResolveSessionOpStatus(session, result, task);

        if (!RoleAnalysisObjectState.isStable(state)) {
            result.recordWarning("Couldn't start clustering. Some process is already in progress.");
            LOGGER.warn("Couldn't start clustering. Some process is already in progress.: " + session.getOid());
            return;
        }

        try {
            ObjectReferenceType objectReferenceType = new ObjectReferenceType()
                    .oid(session.getOid())
                    .type(RoleAnalysisSessionType.COMPLEX_TYPE);

            RoleAnalysisClusteringWorkDefinitionType rdw = new RoleAnalysisClusteringWorkDefinitionType();
            rdw.setSessionRef(objectReferenceType);

            ActivityDefinitionType activity = new ActivityDefinitionType()
                    .work(new WorkDefinitionsType()
                            .roleAnalysisClustering(rdw));

            TaskType taskObject = new TaskType();

            taskObject.setName(Objects.requireNonNullElseGet(
                    taskName, () -> PolyStringType.fromOrig("Session clustering  (" + session + ")")));

            if (taskOid != null) {
                taskObject.setOid(taskOid);
            } else {
                taskOid = UUID.randomUUID().toString();
                taskObject.setOid(taskOid);
            }

            taskObject.setOid(taskOid);
            modelInteractionService.submit(
                    activity,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(taskObject)
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
            @NotNull OperationResult result) {

        String state = recomputeAndResolveClusterOpStatus(cluster.getOid(), result, task);

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
            @NotNull ModelInteractionService modelInteractionService, @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull ActivityDefinitionType activityDefinition,
            @NotNull PrismObject<RoleType> roleObject,
            @Nullable String taskOid,
            @Nullable PolyStringType taskName,
            @NotNull Task task,
            @NotNull OperationResult result) {

        String state = recomputeAndResolveClusterOpStatus(cluster.getOid(), result, task);

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

        } catch (CommonException e) {
            LOGGER.error("Failed to execute role {} migration activity: ", roleObject.getOid(), e);
        }
    }

    public @NotNull String recomputeAndResolveClusterOpStatus(
            @NotNull String clusterOid,
            @NotNull OperationResult result,
            @NotNull Task task) {

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

        Collection<PrismContainerValue<?>> collection = new ArrayList<>();
        for (RoleAnalysisOperationStatus roleAnalysisOperationStatus : operationStatus) {
            RoleAnalysisOperationStatus newStatus = updateRoleAnalysisOperationStatus(
                    repositoryService, roleAnalysisOperationStatus, false, LOGGER, result
            );

            if (newStatus != null) {
                collection.add(newStatus.clone().asPrismContainerValue());
                requestUpdateOp = true;
                OperationResultStatusType progresStatus = newStatus.getStatus();
                if (progresStatus != null && progresStatus.equals(OperationResultStatusType.IN_PROGRESS)) {
                    stateString = RoleAnalysisObjectState.PROCESSING.getDisplayString();
                } else if (progresStatus != null && progresStatus.equals(OperationResultStatusType.SUCCESS)) {
                    requestUpdatePattern = true;
                }

            } else {
                collection.add(roleAnalysisOperationStatus.clone().asPrismContainerValue());
                OperationResultStatusType progresStatus = roleAnalysisOperationStatus.getStatus();
                if (progresStatus != null && progresStatus.equals(OperationResultStatusType.IN_PROGRESS)) {
                    stateString = RoleAnalysisObjectState.PROCESSING.getDisplayString();
                }
            }

            if (requestUpdateOp) {
                try {
                    ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                            .item(RoleAnalysisClusterType.F_OPERATION_STATUS).replace(collection)
                            .asObjectDelta(clusterOid);

                    Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

                    modelService.executeChanges(deltas, null, task, result);

                } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException |
                        ExpressionEvaluationException |
                        CommunicationException | ConfigurationException | PolicyViolationException |
                        SecurityViolationException e) {
                    LOGGER.error("Couldn't modify RoleAnalysisClusterType {}", cluster.getOid(), e);
                }
            }

            if (requestUpdatePattern) {
                updateClusterPatterns(cluster.getOid(), task, result);
            }

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
            @NotNull List<RoleAnalysisAttributeDef> attributeRoleDefSet,
            @NotNull List<RoleAnalysisAttributeDef> attributeUserDefSet) {

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

            List<AttributeAnalysisStructure> userAttributeAnalysisStructures = this
                    .userTypeAttributeAnalysis(users, 100.0, task, result, attributeUserDefSet);
            List<AttributeAnalysisStructure> roleAttributeAnalysisStructures = this
                    .roleTypeAttributeAnalysis(roles, 100.0, task, result, attributeRoleDefSet);

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

        List<RoleAnalysisAttributeAnalysis> attributeAnalysis;

        if (processModeType.equals(RoleAnalysisProcessModeType.ROLE)) {
            RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = clusterStatistics.getRoleAttributeAnalysisResult();
            if (roleAttributeAnalysisResult == null) {
                return "0.0";
            }
            attributeAnalysis = roleAttributeAnalysisResult.getAttributeAnalysis();
        } else {
            RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = clusterStatistics.getUserAttributeAnalysisResult();
            if (userAttributeAnalysisResult == null) {
                return "0.0";
            }
            attributeAnalysis = userAttributeAnalysisResult.getAttributeAnalysis();
        }

        if (attributeAnalysis == null || attributeAnalysis.isEmpty()) {
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

}
