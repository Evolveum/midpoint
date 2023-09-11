/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static java.util.Collections.singleton;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getCurrentXMLGregorianCalendar;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_MODIFY_TIMESTAMP;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisObjectUtils {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisObjectUtils.class);

    public static PrismObject<UserType> getUserTypeObject(@NotNull ModelService modelService, String oid,
            Task task, OperationResult result) {

        try {
            return modelService.getObject(UserType.class, oid, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get UserType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    public static PrismObject<FocusType> getFocusTypeObject(@NotNull ModelService modelService, String oid,
            Task task, OperationResult result) {

        try {
            return modelService.getObject(FocusType.class, oid, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get FocusType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    public static PrismObject<RoleType> getRoleTypeObject(@NotNull ModelService modelService, String oid,
            Task task, OperationResult result) {

        try {
            return modelService.getObject(RoleType.class, oid, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get RoleType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    public static PrismObject<RoleType> getRoleTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {

        Task task = pageBase.createSimpleTask("getRoleTypeObject");
        try {
            return pageBase.getModelService().getObject(RoleType.class, oid, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get RoleType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    public static PrismObject<UserType> getUserTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {

        Task task = pageBase.createSimpleTask("getRoleTypeObject");
        try {
            return pageBase.getModelService().getObject(UserType.class, oid, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get UserType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    public static PrismObject<FocusType> getFocusTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {

        Task task = pageBase.createSimpleTask("getFocusTypeObject");

        try {
            return pageBase.getModelService().getObject(FocusType.class, oid, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get FocusType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    public static PrismObject<RoleAnalysisClusterType> getClusterTypeObject(@NotNull ModelService modelService, String oid,
            Task task, OperationResult result) {

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

    public static PrismObject<RoleAnalysisSessionType> getSessionTypeObject(@NotNull ModelService modelService,
            String oid, Task task, OperationResult result) {

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

    public static List<PrismObject<UserType>> extractRoleMembers(ModelService modelService, ObjectFilter userFilter,
            String objectId, Task task, OperationResult result) {

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
            return modelService.searchObjects(UserType.class, query, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Failed to search role member objects:", ex);
        } finally {
            result.recomputeStatus();
        }

        return null;
    }

    public static Integer countRoleMembers(PageBase pageBase, ObjectFilter userFilter, String objectId, OperationResult result) {

        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .exists(AssignmentHolderType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(objectId)
                .endBlock().build();

        if (userFilter != null) {
            query.addFilter(userFilter);
        }

        Task task = pageBase.createSimpleTask("countRoleMembers");

        try {
            return pageBase.getModelService().countObjects(UserType.class, query, null, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Failed to search role member objects:", ex);
        } finally {
            result.recomputeStatus();
        }

        return null;
    }

    public static @NotNull PrismObject<RoleType> generateBusinessRole(PageBase pageBase, List<AssignmentType> assignmentTypes,
            String name) {

        PrismObject<RoleType> roleTypePrismObject = null;
        try {
            roleTypePrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while finding object definition by compile time class ClusterType object: {}", e.getMessage(), e);
        }

        assert roleTypePrismObject != null;

        RoleType role = roleTypePrismObject.asObjectable();
        role.setName(PolyStringType.fromOrig(name));
        role.getInducement().addAll(assignmentTypes);

        role.getAssignment().add(ObjectTypeUtil.createAssignmentTo(SystemObjectsType.ARCHETYPE_BUSINESS_ROLE.value(),
                ObjectTypes.ARCHETYPE));

        return roleTypePrismObject;
    }

    public static void deleteSingleRoleAnalysisCluster(@NotNull PageBase pageBase,
            @NotNull RoleAnalysisClusterType roleAnalysisClusterType, OperationResult result) {
        try {

            Task task = pageBase.createSimpleTask("deleteSingleRoleAnalysisCluster");
            String clusterOid = roleAnalysisClusterType.getOid();
            PrismObject<RoleAnalysisSessionType> sessionObject = getSessionTypeObject(pageBase.getModelService(),
                    roleAnalysisClusterType.getRoleAnalysisSessionRef().getOid(), task, result
            );

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .asItemDelta());

            if (sessionObject == null) {
                return;
            }

            pageBase.getRepositoryService().modifyObject(RoleAnalysisSessionType.class, sessionObject.getOid(),
                    modifications, result);

            pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, clusterOid, result);

            recomputeSessionStatic(pageBase.getModelService(), sessionObject.getOid(), roleAnalysisClusterType, task, result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteSingleRoleAnalysisSession(@NotNull PageBase pageBase, String sessionOid, OperationResult result) {
        try {
            searchAndDeleteCluster(pageBase, sessionOid, result);
            pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, sessionOid, result);
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void searchAndDeleteCluster(PageBase pageBase, String sessionOid, OperationResult result) {

        ResultHandler<RoleAnalysisClusterType> resultHandler = (object, parentResult) -> {
            try {
                deleteSingleRoleAnalysisCluster(pageBase, object.asObjectable(), result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        ObjectQuery query = pageBase.getPrismContext().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF).ref(sessionOid)
                .build();

        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder();
        RepositoryService repositoryService = pageBase.getRepositoryService();

        try {
            repositoryService.searchObjectsIterative(RoleAnalysisClusterType.class, query, resultHandler, optionsBuilder.build(),
                    true, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

    }

    public static RoleAnalysisProcessModeType resolveClusterProcessMode(@NotNull ModelService modelService,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster, Task task, @NotNull OperationResult result) {

        RoleAnalysisClusterType clusterObject = cluster.asObjectable();
        ObjectReferenceType roleAnalysisSessionRef = clusterObject.getRoleAnalysisSessionRef();
        String sessionRefOid = roleAnalysisSessionRef.getOid();

        PrismObject<RoleAnalysisSessionType> session = getSessionTypeObject(modelService, sessionRefOid, task, result);

        if (session == null) {
            LOGGER.error("Failed to resolve processMode from RoleAnalysisSession object: {}", sessionRefOid);
            return null;
        }

        RoleAnalysisSessionType sessionObject = session.asObjectable();
        return sessionObject.getProcessMode();
    }

    public static void recomputeSessionStatic(@NotNull ModelService modelService, String sessionOid,
            @NotNull RoleAnalysisClusterType roleAnalysisClusterType, Task task, OperationResult result) {
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(modelService, sessionOid, task, result);

        assert sessionTypeObject != null;
        RoleAnalysisSessionType session = sessionTypeObject.asObjectable();

        int deletedClusterMembersCount = roleAnalysisClusterType.getMember().size();
        Double membershipDensity = roleAnalysisClusterType.getClusterStatistics().getMembershipDensity();

        Integer processedObjectCount = session.getSessionStatistic().getProcessedObjectCount();
        Double meanDensity = session.getSessionStatistic().getMeanDensity();
        Integer clusterCount = session.getSessionStatistic().getClusterCount();

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

    public static void recomputeRoleAnalysisClusterDetectionOptions(ModelService modelService, String clusterOid,
            DetectionOption detectionOption, Task task, OperationResult result) {

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

    public static void clusterMigrationRecompute(@NotNull PageBase pageBase, @NotNull String clusterRefOid, String roleRefOid,
            Task task, OperationResult result) {

        PrismObject<RoleAnalysisClusterType> cluster = getClusterTypeObject(pageBase.getModelService(), clusterRefOid,
                task, result);
        if (cluster == null) {
            LOGGER.error("Failed to resolve RoleAnalysisCluster OBJECT from UUID: {}", clusterRefOid);
            return;
        }
        RoleAnalysisClusterType clusterObject = cluster.asObjectable();

        ItemName fClusterUserBasedStatistic = RoleAnalysisClusterType.F_CLUSTER_STATISTICS;

        RoleAnalysisProcessModeType processMode = resolveClusterProcessMode(pageBase.getModelService(), cluster, task, result);
        if (processMode == null) {
            LOGGER.error("Failed to resolve processMode from RoleAnalysisCluster object: {}", clusterRefOid);
            return;
        }

        ItemName fMember = AnalysisClusterStatisticType.F_USERS_COUNT;
        Integer memberCount = clusterObject.getClusterStatistics().getUsersCount();
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            fMember = AnalysisClusterStatisticType.F_ROLES_COUNT;
            memberCount = clusterObject.getClusterStatistics().getRolesCount();
        }

        PrismObject<RoleType> object = getRoleTypeObject(pageBase.getModelService(), roleRefOid, task, result);
        if (object == null) {
            return;
        }

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(object.getOid());
        ref.setType(RoleType.COMPLEX_TYPE);

        try {

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_RESOLVED_PATTERN).add(ref)
                    .asItemDelta());

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTED_PATTERN).replace(new RoleAnalysisDetectionPatternType())
                    .asItemDelta());

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_CLUSTER_STATISTICS, AnalysisClusterStatisticType.F_DETECTED_REDUCTION_METRIC)
                    .replace(0)
                    .asItemDelta());

            ref = new ObjectReferenceType();
            ref.setOid(object.getOid());
            ref.setType(RoleType.COMPLEX_TYPE);

            if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                        .item(RoleAnalysisClusterType.F_MEMBER).add(ref)
                        .asItemDelta());

                modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                        .item(fClusterUserBasedStatistic, fMember).replace(memberCount + 1)
                        .asItemDelta());
            }

            pageBase.getRepositoryService().modifyObject(RoleAnalysisClusterType.class, clusterRefOid, modifications, result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            LOGGER.error("Couldn't execute migration recompute RoleAnalysisClusterDetectionOptions {}", clusterRefOid, e);
        }

    }

}
