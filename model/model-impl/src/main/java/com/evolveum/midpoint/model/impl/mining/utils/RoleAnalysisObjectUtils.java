/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.utils;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getCurrentXMLGregorianCalendar;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.loadIntersections;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_MODIFY_TIMESTAMP;

import java.util.*;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisObjectUtils {

    public static PrismObject<UserType> getUserTypeObject(@NotNull RepositoryService repoService, String oid,
            OperationResult result) {
        try {
            return repoService.getObject(UserType.class, oid, null, result);
        } catch (ObjectNotFoundException ignored) {
//            LOGGER.warn("Object not found. User UUID {} cannot be resolved", oid, e);
            return null;
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    public static PrismObject<RoleType> getRoleTypeObject(@NotNull RepositoryService repoService, String oid,
            OperationResult result) {
        try {
            return repoService.getObject(RoleType.class, oid, null, result);
        } catch (ObjectNotFoundException ignored) {
//            LOGGER.warn("Object not found. Role UUID {} cannot be resolved", oid, ignored);
            return null;
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    public static PrismObject<RoleAnalysisClusterType> getClusterTypeObject(@NotNull RepositoryService repoService,
            OperationResult result, String oid) {
        try {
            return repoService.getObject(RoleAnalysisClusterType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Object not found. RoleAnalysisCluster UUID {} cannot be resolved", oid, e);
            return null;
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    public static PrismObject<RoleAnalysisSessionType> getSessionTypeObject(@NotNull RepositoryService repoService,
            OperationResult result, String oid) {
        try {
            return repoService.getObject(RoleAnalysisSessionType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Object not found. RoleAnalysisSession UUID {} cannot be resolved", oid, e);
            return null;
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }
    }

    public static void importRoleAnalysisClusterObject(OperationResult result, @NotNull RepositoryService repoService,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster, ObjectReferenceType parentRef,
            RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption) {
        cluster.asObjectable().setRoleAnalysisSessionRef(parentRef);
        cluster.asObjectable().setDetectionOption(roleAnalysisSessionDetectionOption);
        try {
            repoService.addObject(cluster, null, result);
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<PrismObject<UserType>> extractRoleMembers(ObjectFilter userFilter, OperationResult result,
            RepositoryService repoService, String objectId) {

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
            return repoService
                    .searchObjects(UserType.class, query, null, result);
        } catch (CommonException e) {
            throw new RuntimeException("Failed to search role member objects: " + e);
        }
    }

    public static void modifySessionAfterClustering(ObjectReferenceType sessionRef,
            RoleAnalysisSessionStatisticType sessionStatistic,
            RepositoryService repoService, OperationResult result) {

        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_SESSION_STATISTIC)
                    .replace(sessionStatistic)
                    .asItemDelta());

            repoService.modifyObject(RoleAnalysisSessionType.class, sessionRef.getOid(), modifications, result);

        } catch (Throwable e) {
            LOGGER.error("Error while modify  RoleAnalysisSessionType {}, {}", sessionRef, e.getMessage(), e);
        }

    }

    public static void replaceRoleAnalysisClusterDetectionPattern(String clusterOid,
            RepositoryService repoService, OperationResult result, List<DetectedPattern> detectedPatterns,
            RoleAnalysisProcessModeType processMode) {

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
                processedObjectComplexType, propertiesComplexType);

        Collection<PrismContainerValue<?>> collection = new ArrayList<>();
        for (RoleAnalysisDetectionPatternType clusterDetectionType : roleAnalysisClusterDetectionTypes) {
            collection.add(clusterDetectionType.asPrismContainerValue());
        }

        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTED_PATTERN).replace(collection)
                    .asItemDelta());

            modifications.add(PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .asItemDelta());

            repoService.modifyObject(RoleAnalysisClusterType.class, clusterOid, modifications, result);
        } catch (Throwable e) {
            LOGGER.error("Error while Import new RoleAnalysisDetectionOption {}, {}", clusterOid, e.getMessage(), e);
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
}
