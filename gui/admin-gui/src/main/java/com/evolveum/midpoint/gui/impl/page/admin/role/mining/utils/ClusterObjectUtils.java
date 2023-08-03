/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils.loadIntersections;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterStatistic;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class ClusterObjectUtils {

    public enum SORT implements Serializable {
        JACCARD("JACCARD"),
        FREQUENCY("FREQUENCY"),
        NONE("NONE");

        private final String displayString;

        SORT(String displayString) {
            this.displayString = displayString;
        }

        public String getDisplayString() {
            return displayString;
        }

    }

    public enum Status implements Serializable {
        NEUTRAL("fa fa-plus"),
        ADD("fa fa-minus"),
        REMOVE("fa fa-undo"),
        DISABLE("fa fa-ban");

        private final String displayString;

        Status(String displayString) {
            this.displayString = displayString;
        }

        public String getDisplayString() {
            return displayString;
        }
    }

    public static void importRoleAnalysisClusterObject(OperationResult result, Task task, @NotNull PageBase pageBase,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster, ObjectReferenceType parentRef, RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption) {
        cluster.asObjectable().setRoleAnalysisSessionRef(parentRef);
        cluster.asObjectable().setDetectionOption(roleAnalysisSessionDetectionOption);
        pageBase.getModelService().importObject(cluster, null, task, result);
    }

    public static void deleteAllRoleAnalysisObjects(OperationResult result, @NotNull PageBase pageBase) {
        deleteAllRoleAnalysisCluster(result, pageBase);
        deleteAllRoleAnalysisSession(result, pageBase);
    }

    public static void deleteSingleRoleAnalysisSession(OperationResult result, RoleAnalysisSessionType roleAnalysisSessionType, @NotNull PageBase pageBase) {

        List<ObjectReferenceType> roleAnalysisClusterRef = roleAnalysisSessionType.getRoleAnalysisClusterRef();

        try {
            for (ObjectReferenceType objectReferenceType : roleAnalysisClusterRef) {
                String oid = objectReferenceType.getOid();
                pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, oid, result);
            }
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, roleAnalysisSessionType.getOid(), result);
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteSingleRoleAnalysisCluster(OperationResult result, String oid, @NotNull PageBase pageBase) {
        try {
            pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, oid, result);
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteAllRoleAnalysisCluster(OperationResult result, @NotNull PageBase pageBase) {
        ResultHandler<AssignmentHolderType> handler = (object, parentResult) -> {

            try {
                pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, object.getOid(), result);
            } catch (ObjectNotFoundException e) {
                throw new RuntimeException(e);
            }

            return true;
        };

        ModelService service = pageBase.getModelService();
        ObjectQuery queryType = pageBase.getPrismContext().queryFor(AssignmentHolderType.class)
                .type(RoleAnalysisClusterType.class).build();

        try {
            service.searchObjectsIterative(AssignmentHolderType.class, queryType, handler, null,
                    pageBase.createSimpleTask("Search iterative ClusterType objects"), result);
        } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException |
                SecurityViolationException | ExpressionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteAllRoleAnalysisSession(OperationResult result, @NotNull PageBase pageBase) {
        ResultHandler<AssignmentHolderType> handler = (object, parentResult) -> {

            try {
                pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, object.getOid(), result);
            } catch (ObjectNotFoundException e) {
                throw new RuntimeException(e);
            }

            return true;
        };

        ModelService service = pageBase.getModelService();
        ObjectQuery queryType = pageBase.getPrismContext().queryFor(AssignmentHolderType.class)
                .type(RoleAnalysisSessionType.class).build();

        try {
            service.searchObjectsIterative(AssignmentHolderType.class, queryType, handler, null,
                    pageBase.createSimpleTask("Search iterative ClusterType objects"), result);
        } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException |
                SecurityViolationException | ExpressionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectReferenceType importRoleAnalysisSessionObject(OperationResult result, @NotNull PageBase pageBase,
            RoleAnalysisSessionOptionType roleAnalysisSessionClusterOption,
            RoleAnalysisSessionStatisticType roleAnalysisSessionStatisticType,
            List<ObjectReferenceType> roleAnalysisClusterRef,
            String name) {
        Task task = pageBase.createSimpleTask("Import RoleAnalysisSessionOption object");

        PrismObject<RoleAnalysisSessionType> roleAnalysisSessionPrismObject = generateParentClusterObject(pageBase,
                roleAnalysisSessionClusterOption, roleAnalysisClusterRef,
                roleAnalysisSessionStatisticType, name);

        ModelService modelService = pageBase.getModelService();
        modelService.importObject(roleAnalysisSessionPrismObject, null, task, result);

        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setOid(roleAnalysisSessionPrismObject.getOid());
        objectReferenceType.setType(RoleAnalysisSessionType.COMPLEX_TYPE);
        return objectReferenceType;
    }

    public static PrismObject<RoleAnalysisSessionType> generateParentClusterObject(PageBase pageBase,
            RoleAnalysisSessionOptionType roleAnalysisSessionClusterOption,
            List<ObjectReferenceType> roleAnalysisClusterRef,
            RoleAnalysisSessionStatisticType roleAnalysisSessionStatisticType,
            String name
    ) {

        PrismObject<RoleAnalysisSessionType> roleAnalysisSessionPrismObject = null;
        try {
            roleAnalysisSessionPrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleAnalysisSessionType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Failed to create RoleAnalysisSessionType object: {}", e.getMessage(), e);
        }

        assert roleAnalysisSessionPrismObject != null;

        RoleAnalysisSessionType roleAnalysisSession = roleAnalysisSessionPrismObject.asObjectable();
        roleAnalysisSession.setName(PolyStringType.fromOrig(name));
        roleAnalysisSession.getRoleAnalysisClusterRef().addAll(roleAnalysisClusterRef);
        roleAnalysisSession.setSessionStatistic(roleAnalysisSessionStatisticType);
        roleAnalysisSession.setClusterOptions(roleAnalysisSessionClusterOption);
        return roleAnalysisSessionPrismObject;
    }

//    public static PrismObject<RoleAnalysisSession> generateParentClusterObject(PageBase pageBase, double density, int consist,
//            List<String> childRef, JSONObject options) {
//
//        PrismObject<RoleAnalysisSession> parentClusterObject = null;
//        try {
//            parentClusterObject = pageBase.getPrismContext()
//                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleAnalysisSession.class).instantiate();
//        } catch (SchemaException e) {
//            LOGGER.error("Error while generating ParentClusterType object: {}", e.getMessage(), e);
//        }
//        assert parentClusterObject != null;
//
//        RoleAnalysisSessionType clusterType = parentClusterObject.asObjectable();
//        String name = options.getString("name");
//        if (name == null) {
//            clusterType.setName(PolyStringType.fromOrig(options.getString("identifier")));
//        } else {
//            clusterType.setName(PolyStringType.fromOrig(name));
//        }
//
//        clusterType.getRoleAnalysisClusterRef().addAll(childRef);
//        clusterType.setMeanDensity(String.format("%.3f", density));
//        clusterType.setElementConsist(consist);
//        clusterType.setProcessMode(options.getString("mode"));
//        clusterType.setOptions(String.valueOf(options));
//
//        return parentClusterObject;
//    }

    public static void deleteRoleAnalysisObjects(OperationResult result, @NotNull PageBase pageBase, String parentClusterOid,
            List<ObjectReferenceType> roleAnalysisClusterRef) {
        try {
            for (ObjectReferenceType roleAnalysisClusterOid : roleAnalysisClusterRef) {
                deleteRoleAnalysisCluster(result, pageBase, roleAnalysisClusterOid.getOid());
            }
            deleteRoleAnalysisSession(result, pageBase, parentClusterOid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteRoleAnalysisSession(@NotNull OperationResult result, @NotNull PageBase pageBase, @NotNull String oid)
            throws Exception {
        pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, oid, result);
    }

    public static void deleteRoleAnalysisCluster(@NotNull OperationResult result, @NotNull PageBase pageBase, @NotNull String oid)
            throws Exception {
        pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, oid, result);
    }

    public static List<PrismObject<UserType>> extractRoleMembers(OperationResult result, PageBase pageBase, String objectId) {

        ObjectQuery query = pageBase.getPrismContext().queryFor(UserType.class)
                .exists(AssignmentHolderType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(objectId)
                .endBlock().build();
        try {
            return pageBase.getMidpointApplication().getRepositoryService()
                    .searchObjects(UserType.class, query, null, result);
        } catch (CommonException e) {
            throw new RuntimeException("Failed to search role member objects: " + e);
        }
    }

    public static List<PrismObject<UserType>> extractRoleMembers2(OperationResult result, PageBase pageBase, String[] stringArray) {

        ObjectQuery query = pageBase.getPrismContext().queryFor(UserType.class)
                .exists(AssignmentHolderType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(stringArray)
                .endBlock().build();
        try {
            return pageBase.getMidpointApplication().getRepositoryService()
                    .searchObjects(UserType.class, query, null, result);
        } catch (CommonException e) {
            throw new RuntimeException("Failed to search role member objects: " + e);
        }
    }

    public static List<String> extractOid(List<PrismObject<UserType>> roleMembers) {
        List<String> membersOids = new ArrayList<>();
        for (PrismObject<UserType> roleMember : roleMembers) {
            membersOids.add(roleMember.getOid());
        }

        return membersOids;

    }

    public static PrismObject<RoleAnalysisSessionType> getParentClusterByOid(@NotNull PageBase pageBase,
            String oid, OperationResult result) {
        try {
            return pageBase.getRepositoryService()
                    .getObject(RoleAnalysisSessionType.class, oid, null, result);
        } catch (ObjectNotFoundException ignored) {
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static PrismObject<UserType> getUserTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(UserType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Object not found" + e);
            return null;
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrismObject<FocusType> getFocusTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(FocusType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Object not found" + e);
            return null;
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrismObject<RoleType> getRoleTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(RoleType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Object not found" + e);
            return null;
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull Set<ObjectReferenceType> createObjectReferences(Set<String> objects, QName complexType,
            RepositoryService repositoryService, OperationResult operationResult) {

        Set<ObjectReferenceType> objectReferenceList = new HashSet<>();
        for (String item : objects) {

            try {
                PrismObject<FocusType> object = repositoryService.getObject(FocusType.class, item, null, operationResult);
                ObjectReferenceType objectReferenceType = new ObjectReferenceType();
                objectReferenceType.setType(complexType);
                objectReferenceType.setOid(item);
                objectReferenceType.setTargetName(PolyStringType.fromOrig(object.getName().toString()));

                objectReferenceList.add(objectReferenceType);
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Object not found" + e);
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }

        }
        return objectReferenceList;
    }

    public static List<String> checkExist(Set<String> objects,
            RepositoryService repositoryService, OperationResult operationResult) {

        List<String> existingObjectOid = new ArrayList<>();
        for (String item : objects) {

            try {
                PrismObject<FocusType> object = repositoryService.getObject(FocusType.class, item, null, operationResult);
                existingObjectOid.add(item);
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Object not found" + e);
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }

        }
        return existingObjectOid;
    }

    public static @NotNull Set<ObjectReferenceType> createObjectReferences(Set<String> objects, QName complexType) {

        Set<ObjectReferenceType> objectReferenceList = new HashSet<>();
        for (String item : objects) {
            ObjectReferenceType objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setType(complexType);
            objectReferenceType.setOid(item);
            objectReferenceList.add(objectReferenceType);
        }
        return objectReferenceList;
    }

    public static RoleAnalysisClusterStatisticType createClusterStatisticType(ClusterStatistic clusterStatistic) {
        RoleAnalysisClusterStatisticType roleAnalysisClusterStatisticType = new RoleAnalysisClusterStatisticType();
        roleAnalysisClusterStatisticType.setMemberCount(clusterStatistic.getMembersCount());
        roleAnalysisClusterStatisticType.setPropertiesCount(clusterStatistic.getPropertiesCount());
        roleAnalysisClusterStatisticType.setPropertiesMean(clusterStatistic.getPropertiesMean());
        roleAnalysisClusterStatisticType.setPropertiesDensity(clusterStatistic.getPropertiesDensity());
        roleAnalysisClusterStatisticType.setPropertiesMinOccupancy(clusterStatistic.getMinVectorPoint());
        roleAnalysisClusterStatisticType.setPropertiesMaxOccupancy(clusterStatistic.getMaxVectorPoint());

        return roleAnalysisClusterStatisticType;
    }

    @Nullable
    public static PrismObject<RoleAnalysisClusterType> prepareClusterPrismObject(@NotNull PageBase pageBase) {
        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = null;
        try {
            clusterTypePrismObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleAnalysisClusterType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while finding object definition by compile time class ClusterType object: {}", e.getMessage(), e);
        }
        return clusterTypePrismObject;
    }

    public static @NotNull PrismObject<RoleAnalysisClusterType> getClusterTypeObject(@NotNull PageBase pageBase, String oid) {
        OperationResult operationResult = new OperationResult("GetCluster");
        try {
            return pageBase.getRepositoryService().getObject(RoleAnalysisClusterType.class, oid, null, operationResult);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static int countParentClusterTypeObjects(@NotNull PageBase pageBase) {
        OperationResult operationResult = new OperationResult("countClusters");
        try {
            return pageBase.getRepositoryService().countObjects(RoleAnalysisSessionType.class, null, null, operationResult);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static DetectionOption loadDetectionOption(@NotNull ClusterOptions clusterOptions) {
        int group = Math.min(clusterOptions.getDefaultOccupancySearch(), clusterOptions.getMinGroupSize());
        int intersection = Math.min(clusterOptions.getDefaultIntersectionSearch(), clusterOptions.getMinIntersections());

        return new DetectionOption(
                clusterOptions.getDefaultMinFrequency(),
                clusterOptions.getDefaultMaxFrequency(),
                group,
                intersection,
                clusterOptions.getSearchMode(),
                clusterOptions.getDefaultJaccardThreshold()
        );
    }

    @NotNull
    public static DetectionOption loadDetectionOption(@NotNull RoleAnalysisDetectionOptionType clusterOptions) {

        return new DetectionOption(
                clusterOptions.getMinFrequencyThreshold(),
                clusterOptions.getMaxFrequencyThreshold(),
                clusterOptions.getMinOccupancy(),
                clusterOptions.getMinPropertiesOverlap(),
                clusterOptions.getDetectionMode(),
                clusterOptions.getJaccardSimilarityThreshold()
        );
    }

    public static List<String> getRolesOid(AssignmentHolderType object) {
        List<String> oidList;
        List<AssignmentType> assignments = object.getAssignment();
        oidList = assignments.stream().map(AssignmentType::getTargetRef).filter(
                        targetRef -> targetRef.getType().equals(RoleType.COMPLEX_TYPE))
                .map(AbstractReferencable::getOid).sorted()
                .collect(Collectors.toList());
        return oidList;
    }

    public static void replaceRoleAnalysisClusterDetectionOption(String roleAnalysisClusterOid, PageBase pageBase,
            DetectionOption detectionOption, OperationResult result) {

        RoleAnalysisDetectionOptionType roleAnalysisDetectionOptionType = new RoleAnalysisDetectionOptionType();
        roleAnalysisDetectionOptionType.setJaccardSimilarityThreshold(detectionOption.getJaccardSimilarityThreshold());
        roleAnalysisDetectionOptionType.setMinFrequencyThreshold(detectionOption.getMinFrequencyThreshold());
        roleAnalysisDetectionOptionType.setMaxFrequencyThreshold(detectionOption.getMaxFrequencyThreshold());
        roleAnalysisDetectionOptionType.setDetectionMode(detectionOption.getSearchMode());
        roleAnalysisDetectionOptionType.setMinOccupancy(detectionOption.getMinOccupancy());
        roleAnalysisDetectionOptionType.setMinPropertiesOverlap(detectionOption.getMinPropertiesOverlap());

        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTION_OPTION)
                    .replace(roleAnalysisDetectionOptionType)
                    .asItemDeltas());
            pageBase.getRepositoryService().modifyObject(RoleAnalysisClusterType.class, roleAnalysisClusterOid, modifications, result);
        } catch (Throwable e) {
            LOGGER.error("Error while Import new RoleAnalysisDetectionOption {}, {}", roleAnalysisClusterOid, e.getMessage(), e);
        }

    }

    public static void replaceRoleAnalysisSessionDetectionOption(String roleAnalysisSessionOid, PageBase pageBase,
            DetectionOption detectionOption, OperationResult result) {

        RoleAnalysisDetectionOptionType roleAnalysisDetectionOptionType = new RoleAnalysisDetectionOptionType();
        roleAnalysisDetectionOptionType.setJaccardSimilarityThreshold(detectionOption.getJaccardSimilarityThreshold());
        roleAnalysisDetectionOptionType.setMinFrequencyThreshold(detectionOption.getMinFrequencyThreshold());
        roleAnalysisDetectionOptionType.setMaxFrequencyThreshold(detectionOption.getMaxFrequencyThreshold());
        roleAnalysisDetectionOptionType.setDetectionMode(detectionOption.getSearchMode());
        roleAnalysisDetectionOptionType.setMinOccupancy(detectionOption.getMinOccupancy());
        roleAnalysisDetectionOptionType.setMinPropertiesOverlap(detectionOption.getMinPropertiesOverlap());

        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>(pageBase.getPrismContext().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisClusterType.F_DETECTION_OPTION)
                    .replace(roleAnalysisDetectionOptionType)
                    .asItemDeltas());
            pageBase.getRepositoryService().modifyObject(RoleAnalysisClusterType.class, roleAnalysisSessionOid, modifications, result);
        } catch (Throwable e) {
            LOGGER.error("Error while Import new RoleAnalysisDetectionOption {}, {}", roleAnalysisSessionOid, e.getMessage(), e);
        }

    }

    public static void replaceRoleAnalysisClusterDetection(String roleAnalysisClusterOid,
            PageBase pageBase, OperationResult result, List<DetectedPattern> detectedPatterns,
            RoleAnalysisProcessModeType processMode, DetectionOption detectionOption) {

        QName processedObjectComplexType;
        QName propertiesComplexType;
        if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
            processedObjectComplexType = UserType.COMPLEX_TYPE;
            propertiesComplexType = RoleType.COMPLEX_TYPE;
        } else {
            processedObjectComplexType = RoleType.COMPLEX_TYPE;
            propertiesComplexType = UserType.COMPLEX_TYPE;
        }

        List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypes = loadIntersections(detectedPatterns,
                detectionOption.getSearchMode(), processedObjectComplexType, propertiesComplexType);

        Collection<PrismContainerValue<?>> collection = new ArrayList<>();
        for (RoleAnalysisDetectionPatternType clusterDetectionType : roleAnalysisClusterDetectionTypes) {
            collection.add(clusterDetectionType.asPrismContainerValue());
        }

        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTION_PATTERN).replace(collection)
                    .asItemDelta());

            pageBase.getRepositoryService().modifyObject(RoleAnalysisClusterType.class, roleAnalysisClusterOid, modifications, result);
        } catch (Throwable e) {
            LOGGER.error("Error while Import new RoleAnalysisDetectionOption {}, {}", roleAnalysisClusterOid, e.getMessage(), e);
        }

        replaceRoleAnalysisSessionDetectionOption(roleAnalysisClusterOid, pageBase, detectionOption, result);
    }

}
