/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getCurrentXMLGregorianCalendar;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_MODIFY_TIMESTAMP;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisObjectUtils {

    public static PrismObject<UserType> getUserTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(UserType.class, oid, null, result);
        } catch (ObjectNotFoundException ignored) {
//            LOGGER.warn("Object not found. User UUID {} cannot be resolved", oid, e);
            return null;
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    public static PrismObject<FocusType> getFocusTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(FocusType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Object not found. Focus UUID {} cannot be resolved", oid, e);
            return null;
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    public static PrismObject<RoleType> getRoleTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(RoleType.class, oid, null, result);
        } catch (ObjectNotFoundException ignored) {
//            LOGGER.warn("Object not found. Role UUID {} cannot be resolved", oid, ignored);
            return null;
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    public static PrismObject<RoleAnalysisClusterType> getClusterTypeObject(@NotNull PageBase pageBase,
            OperationResult result, String oid) {
        try {
            return pageBase.getRepositoryService().getObject(RoleAnalysisClusterType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Object not found. RoleAnalysisCluster UUID {} cannot be resolved", oid, e);
            return null;
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    public static PrismObject<RoleAnalysisSessionType> getSessionTypeObject(@NotNull PageBase pageBase,
            OperationResult result, String oid) {
        try {
            return pageBase.getRepositoryService().getObject(RoleAnalysisSessionType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Object not found. RoleAnalysisSession UUID {} cannot be resolved", oid, e);
            return null;
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    public static @NotNull PrismObject<RoleAnalysisSessionType> getParentClusterByOid(@NotNull PageBase pageBase,
            String oid, OperationResult result) {
        try {
            return pageBase.getRepositoryService()
                    .getObject(RoleAnalysisSessionType.class, oid, null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<PrismObject<UserType>> extractRoleMembers(ObjectFilter userFilter, OperationResult result,
            PageBase pageBase, String objectId) {

        ObjectQuery query = pageBase.getPrismContext().queryFor(UserType.class)
                .exists(AssignmentHolderType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(objectId)
                .endBlock().build();

        if (userFilter != null) {
            query.addFilter(userFilter);
        }
        try {
            return pageBase.getMidpointApplication().getRepositoryService()
                    .searchObjects(UserType.class, query, null, result);
        } catch (CommonException e) {
            throw new RuntimeException("Failed to search role member objects: " + e);
        }
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

        role.getAssignment().add(ObjectTypeUtil.createAssignmentTo(SystemObjectsType.ARCHETYPE_BUSINESS_ROLE.value(), ObjectTypes.ARCHETYPE));

        return roleTypePrismObject;
    }

    public static void deleteSingleRoleAnalysisCluster(OperationResult result,
            @NotNull RoleAnalysisClusterType roleAnalysisClusterType, @NotNull PageBase pageBase) {
        try {

            String clusterOid = roleAnalysisClusterType.getOid();
            PrismObject<RoleAnalysisSessionType> sessionObject = getSessionTypeObject(pageBase, result,
                    roleAnalysisClusterType.getRoleAnalysisSessionRef().getOid());

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .asItemDelta());

            if (sessionObject == null) {
                return;
            }

            pageBase.getRepositoryService().modifyObject(RoleAnalysisSessionType.class, sessionObject.getOid(), modifications, result);

            pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, clusterOid, result);

            recomputeSessionStatic(result, sessionObject.getOid(), roleAnalysisClusterType, pageBase);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteSingleRoleAnalysisSession(OperationResult result, String sessionOid,
            @NotNull PageBase pageBase) {
        try {
            searchAndDeleteCluster(pageBase, result, sessionOid);
            pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, sessionOid, result);
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void searchAndDeleteCluster(PageBase pageBase, OperationResult result, String sessionOid) {

        ResultHandler<RoleAnalysisClusterType> resultHandler = (object, parentResult) -> {
            try {
                deleteSingleRoleAnalysisCluster(result, object.asObjectable(), pageBase);
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

    public static RoleAnalysisProcessModeType resolveClusterProcessMode(@NotNull PageBase pageBase, @NotNull OperationResult result,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster) {

        RoleAnalysisClusterType clusterObject = cluster.asObjectable();
        ObjectReferenceType roleAnalysisSessionRef = clusterObject.getRoleAnalysisSessionRef();
        String sessionRefOid = roleAnalysisSessionRef.getOid();

        PrismObject<RoleAnalysisSessionType> session = getSessionTypeObject(pageBase, result, sessionRefOid);

        if (session == null) {
            LOGGER.error("Failed to resolve processMode from RoleAnalysisSession object: {}", sessionRefOid);
            return null;
        }

        RoleAnalysisSessionType sessionObject = session.asObjectable();
        return sessionObject.getProcessMode();
    }

    public static void recomputeSessionStatic(OperationResult result, String sessionOid,
            @NotNull RoleAnalysisClusterType roleAnalysisClusterType, @NotNull PageBase pageBase) {
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(pageBase, result, sessionOid);

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

        List<ItemDelta<?, ?>> modifications = new ArrayList<>();

        try {
            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_SESSION_STATISTIC).replace(recomputeSessionStatistic.asPrismContainerValue())
                    .asItemDelta());
            pageBase.getRepositoryService().modifyObject(RoleAnalysisSessionType.class, sessionOid, modifications, result);

        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
            throw new RuntimeException(e);
        }

    }

    public static void recomputeRoleAnalysisClusterDetectionOptions(String clusterOid, PageBase pageBase,
            DetectionOption detectionOption, OperationResult result) {

        RoleAnalysisDetectionOptionType roleAnalysisDetectionOptionType = new RoleAnalysisDetectionOptionType();
        roleAnalysisDetectionOptionType.setFrequencyRange(new RangeType()
                .max(detectionOption.getMaxFrequencyThreshold())
                .min(detectionOption.getMinFrequencyThreshold()));
        roleAnalysisDetectionOptionType.setMinUserOccupancy(detectionOption.getMinUsers());
        roleAnalysisDetectionOptionType.setMinRolesOccupancy(detectionOption.getMinRoles());

        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTION_OPTION)
                    .replace(roleAnalysisDetectionOptionType.asPrismContainerValue())
                    .asItemDeltas());
            pageBase.getRepositoryService().modifyObject(RoleAnalysisClusterType.class, clusterOid, modifications, result);

        } catch (Throwable e) {
            LOGGER.error("Error while Import new RoleAnalysisDetectionOption {}, {}", clusterOid, e.getMessage(), e);
        }

    }

    public static void clusterMigrationRecompute(OperationResult result,
            @NotNull String clusterRefOid, String roleRefOid, @NotNull PageBase pageBase) {
        try {

            PrismObject<RoleAnalysisClusterType> cluster = getClusterTypeObject(pageBase, result, clusterRefOid);
            if (cluster == null) {
                LOGGER.error("Failed to resolve RoleAnalysisCluster OBJECT from UUID: {}", clusterRefOid);
                return;
            }
            RoleAnalysisClusterType clusterObject = cluster.asObjectable();

            ItemName fClusterUserBasedStatistic = RoleAnalysisClusterType.F_CLUSTER_STATISTICS;

            RoleAnalysisProcessModeType processMode = resolveClusterProcessMode(pageBase, result, cluster);
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

            PrismObject<RoleType> object = getRoleTypeObject(pageBase, roleRefOid, result);
            if (object == null) {
                return;
            }

            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(object.getOid());
            ref.setType(RoleType.COMPLEX_TYPE);

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_RESOLVED_PATTERN).add(ref)
                    .asItemDelta());

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTED_PATTERN).replace(new RoleAnalysisDetectionPatternType())
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
            throw new RuntimeException(e);
        }
    }

}
