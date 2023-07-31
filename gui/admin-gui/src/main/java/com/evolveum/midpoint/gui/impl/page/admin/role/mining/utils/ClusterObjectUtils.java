/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.ModelService;
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

public class ClusterObjectUtils {

    public enum SORT {
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

    public enum Status {
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
            @NotNull PrismObject<RoleAnalysisClusterType> cluster, ObjectReferenceType parentRef) {
        cluster.asObjectable().setRoleAnalysisSessionRef(parentRef);
        pageBase.getModelService().importObject(cluster, null, task, result);
    }

    public static void deleteAllRoleAnalysisObjects(OperationResult result, @NotNull PageBase pageBase) {
        deleteAllRoleAnalysisCluster(result, pageBase);
        deleteAllRoleAnalysisSession(result, pageBase);
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
            RoleAnalysisSessionClusterOptionType roleAnalysisSessionClusterOption,
            RoleAnalysisSessionDetectionOptionType roleAnalysisSessionDetectionOption,
            RoleAnalysisSessionStatisticType roleAnalysisSessionStatisticType,
            List<ObjectReferenceType> roleAnalysisClusterRef,
            String name) {
        Task task = pageBase.createSimpleTask("Import RoleAnalysisSessionOption object");

        PrismObject<RoleAnalysisSessionType> roleAnalysisSessionPrismObject = generateParentClusterObject(pageBase,
                roleAnalysisSessionClusterOption, roleAnalysisSessionDetectionOption, roleAnalysisClusterRef,
                roleAnalysisSessionStatisticType, name);

        ModelService modelService = pageBase.getModelService();
        modelService.importObject(roleAnalysisSessionPrismObject, null, task, result);

        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setOid(roleAnalysisSessionPrismObject.getOid());
        objectReferenceType.setType(RoleAnalysisSessionType.COMPLEX_TYPE);
        return objectReferenceType;
    }

    public static PrismObject<RoleAnalysisSessionType> generateParentClusterObject(PageBase pageBase,
            RoleAnalysisSessionClusterOptionType roleAnalysisSessionClusterOption,
            RoleAnalysisSessionDetectionOptionType roleAnalysisSessionDetectionOption,
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
        roleAnalysisSession.setPatternDetectionOption(roleAnalysisSessionDetectionOption);
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

    public static List<String> getRolesOid(AssignmentHolderType object) {
        List<String> oidList;
        List<AssignmentType> assignments = object.getAssignment();
        oidList = assignments.stream().map(AssignmentType::getTargetRef).filter(
                        targetRef -> targetRef.getType().equals(RoleType.COMPLEX_TYPE))
                .map(AbstractReferencable::getOid).sorted()
                .collect(Collectors.toList());
        return oidList;
    }

}
