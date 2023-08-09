/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.ClusterAlgorithmUtils.loadIntersections;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_MODIFY_TIMESTAMP;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterStatistic;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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

    public enum DETECT implements Serializable {
        FULL("full"),
        PARTIAL("skip large"),
        NONE("none");

        private final String displayString;

        DETECT(String displayString) {
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
            @NotNull PrismObject<RoleAnalysisClusterType> cluster, ObjectReferenceType parentRef,
            RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption) {
        cluster.asObjectable().setRoleAnalysisSessionRef(parentRef);
        cluster.asObjectable().setDetectionOption(roleAnalysisSessionDetectionOption);
        pageBase.getModelService().importObject(cluster, null, task, result);
    }

    public static void deleteSingleRoleAnalysisSession(OperationResult result, RoleAnalysisSessionType roleAnalysisSessionType,
            @NotNull PageBase pageBase) {

//        List<ObjectReferenceType> roleAnalysisClusterRef = roleAnalysisSessionType.getRoleAnalysisClusterRef();
//
//        try {
//            for (ObjectReferenceType objectReferenceType : roleAnalysisClusterRef) {
//                System.out.println("cl"+objectReferenceType);
//                String oid = objectReferenceType.getOid();
//                pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, oid, result);
//            }
//        } catch (ObjectNotFoundException e) {
//            throw new RuntimeException(e);
//        }

        try {
            pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, roleAnalysisSessionType.getOid(), result);
        } catch (ObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteSingleRoleAnalysisCluster(OperationResult result,
            @NotNull RoleAnalysisClusterType roleAnalysisClusterType, @NotNull PageBase pageBase) {
        try {

            String clusterOid = roleAnalysisClusterType.getOid();
            PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(pageBase, roleAnalysisClusterType.getRoleAnalysisSessionRef().getOid());

            List<ObjectReferenceType> roleAnalysisClusterRef = sessionTypeObject.asObjectable().getRoleAnalysisClusterRef();

            List<PrismReferenceValue> recompute = new ArrayList<>();
            for (ObjectReferenceType referenceType : roleAnalysisClusterRef) {
                if (referenceType.getOid().equals(clusterOid)) {
                    continue;
                }

                ObjectReferenceType objectReferenceType1 = new ObjectReferenceType();
                objectReferenceType1.setOid(referenceType.getOid());
                objectReferenceType1.setTargetName(referenceType.getTargetName());
                objectReferenceType1.setType(referenceType.getType());
                recompute.add(objectReferenceType1.asReferenceValue());
            }

            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_ROLE_ANALYSIS_CLUSTER_REF).replace(recompute)
                    .asItemDelta());

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .asItemDelta());

            pageBase.getRepositoryService().modifyObject(RoleAnalysisSessionType.class, sessionTypeObject.getOid(), modifications, result);

            pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, clusterOid, result);

            recomputeSessionStatic(result, sessionTypeObject.getOid(), pageBase);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
    }

    public static void recomputeSessionStatic(OperationResult result, String sessionOid, @NotNull PageBase pageBase) {
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(pageBase, sessionOid);

        List<ObjectReferenceType> roleAnalysisClusterRef = sessionTypeObject.asObjectable().getRoleAnalysisClusterRef();

        int sessionClustersCount = roleAnalysisClusterRef.size();

        double recomputeMeanDensity = 0;
        int recomputeProcessedObjectCount = 0;
        for (ObjectReferenceType referenceType : roleAnalysisClusterRef) {
            RoleAnalysisClusterType clusterTypeObject = getClusterTypeObject(pageBase, referenceType.getOid()).asObjectable();
            recomputeMeanDensity += clusterTypeObject.getClusterStatistic().getPropertiesDensity();
            recomputeProcessedObjectCount += clusterTypeObject.getClusterStatistic().getMemberCount();
        }

        RoleAnalysisSessionStatisticType recomputeSessionStatistic = new RoleAnalysisSessionStatisticType();
        recomputeSessionStatistic.setMeanDensity(recomputeMeanDensity / sessionClustersCount);
        recomputeSessionStatistic.setProcessedObjectCount(recomputeProcessedObjectCount);

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

    public static @NotNull ObjectReferenceType importRoleAnalysisSessionObject(OperationResult result, @NotNull PageBase pageBase,
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
                repositoryService.getObject(FocusType.class, item, null, operationResult);
                existingObjectOid.add(item);
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Object not found" + e);
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }

        }
        return existingObjectOid;
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

    public static @NotNull PrismObject<RoleAnalysisSessionType> getSessionTypeObject(@NotNull PageBase pageBase, String oid) {
        OperationResult operationResult = new OperationResult("GetSession");
        try {
            return pageBase.getRepositoryService().getObject(RoleAnalysisSessionType.class, oid, null, operationResult);
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
                clusterOptions.getMinMembersOccupancy(),
                clusterOptions.getMinPropertiesOccupancy(),
                clusterOptions.getDetectionMode(),
                clusterOptions.getJaccardSimilarityThreshold()
        );
    }

    public static List<String> getRolesOidAssignment(AssignmentHolderType object) {
        List<String> oidList;
        List<AssignmentType> assignments = object.getAssignment();
        oidList = assignments.stream().map(AssignmentType::getTargetRef).filter(
                        targetRef -> targetRef.getType().equals(RoleType.COMPLEX_TYPE))
                .map(AbstractReferencable::getOid).sorted()
                .collect(Collectors.toList());
        return oidList;
    }

    public static List<String> getRolesOidInducements(PrismObject<RoleType> object) {
        List<String> oidList;
        List<AssignmentType> assignments = object.asObjectable().getInducement();
        oidList = assignments.stream().map(AssignmentType::getTargetRef).filter(
                        targetRef -> targetRef.getType().equals(AbstractRoleType.COMPLEX_TYPE))
                .map(AbstractReferencable::getOid).sorted()
                .collect(Collectors.toList());
        return oidList;
    }

    public static void recomputeRoleAnalysisClusterDetectionOptions(String clusterOid, PageBase pageBase,
            DetectionOption detectionOption, OperationResult result) {

        RoleAnalysisDetectionOptionType roleAnalysisDetectionOptionType = new RoleAnalysisDetectionOptionType();
        roleAnalysisDetectionOptionType.setJaccardSimilarityThreshold(detectionOption.getJaccardSimilarityThreshold());
        roleAnalysisDetectionOptionType.setMinFrequencyThreshold(detectionOption.getMinFrequencyThreshold());
        roleAnalysisDetectionOptionType.setMaxFrequencyThreshold(detectionOption.getMaxFrequencyThreshold());
        roleAnalysisDetectionOptionType.setDetectionMode(detectionOption.getSearchMode());
        roleAnalysisDetectionOptionType.setMinMembersOccupancy(detectionOption.getMinOccupancy());
        roleAnalysisDetectionOptionType.setMinPropertiesOccupancy(detectionOption.getMinPropertiesOverlap());

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

    public static void replaceRoleAnalysisClusterDetection(String clusterOid,
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

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .asItemDelta());

            modifications.add(pageBase.getPrismContext().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .asItemDelta());

            pageBase.getRepositoryService().modifyObject(RoleAnalysisClusterType.class, clusterOid, modifications, result);
        } catch (Throwable e) {
            LOGGER.error("Error while Import new RoleAnalysisDetectionOption {}, {}", clusterOid, e.getMessage(), e);
        }

        recomputeRoleAnalysisClusterDetectionOptions(clusterOid, pageBase, detectionOption, result);
    }

    private static XMLGregorianCalendar getCurrentXMLGregorianCalendar() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        DatatypeFactory datatypeFactory;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        return datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
    }

    public static String resolveDateAndTime(XMLGregorianCalendar xmlGregorianCalendar) {

        int year = xmlGregorianCalendar.getYear();
        int month = xmlGregorianCalendar.getMonth();
        int day = xmlGregorianCalendar.getDay();
        int hours = xmlGregorianCalendar.getHour();
        int minutes = xmlGregorianCalendar.getMinute();

        String dateString = String.format("%04d:%02d:%02d", year, month, day);

        String amPm = (hours < 12) ? "AM" : "PM";
        hours = hours % 12;
        if (hours == 0) {
            hours = 12;
        }
        String timeString = String.format("%02d:%02d %s", hours, minutes, amPm);

        return dateString + ", " + timeString;
    }

    public static @NotNull PrismObject<RoleType> generateBusinessRole(PageBase pageBase, List<AssignmentType> assignmentTypes,String name) {

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

        return roleTypePrismObject;
    }

    public void generateUserAssignmentDeltas(){

    }
}
