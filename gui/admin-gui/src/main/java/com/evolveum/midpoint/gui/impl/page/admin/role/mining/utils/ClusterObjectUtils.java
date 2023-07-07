/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.openjson.JSONObject;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.ClusteringObjectMapped;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ClusterObjectUtils {

    public static void importClusterTypeObject(OperationResult result, @NotNull PageBase pageBase,
            @NotNull PrismObject<ClusterType> cluster) {
        Task task = pageBase.createSimpleTask("Import ClusterType object");
        pageBase.getModelService().importObject(cluster, null, task, result);
    }

    public static void importParentClusterTypeObject(OperationResult result, @NotNull PageBase pageBase,
            double density, int counsist, List<String> childRef, JSONObject options) {
        Task task = pageBase.createSimpleTask("Import ParentClusterType object");

        PrismObject<ParentClusterType> parentClusterTypePrismObject = generateParentClusterObject(pageBase, density, counsist,
                childRef, options);
        pageBase.getModelService().importObject(parentClusterTypePrismObject, null, task, result);
    }

    public static PrismObject<ParentClusterType> generateParentClusterObject(PageBase pageBase, double density, int consist,
            List<String> childRef, JSONObject options) {

        PrismObject<ParentClusterType> parentClusterObject = null;
        try {
            parentClusterObject = pageBase.getPrismContext()
                    .getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ParentClusterType.class).instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Error while generating ParentClusterType object: {}", e.getMessage(), e);
        }
        assert parentClusterObject != null;

        UUID uuid = UUID.randomUUID();
        ParentClusterType clusterType = parentClusterObject.asObjectable();
        String name = options.getString("name");
        if (name == null) {
            clusterType.setName(PolyStringType.fromOrig(options.getString("identifier")));
        } else {
            clusterType.setName(PolyStringType.fromOrig(name));

        }
        clusterType.setOid(String.valueOf(uuid));

        clusterType.getClustersRef().addAll(childRef);
        clusterType.setDensity(String.format("%.3f", density));
        clusterType.setConsist(consist);
        clusterType.setIdentifier(options.getString("identifier"));
        clusterType.setMode(options.getString("mode"));
        clusterType.setOptions(String.valueOf(options));

        return parentClusterObject;
    }

    public static void cleanBeforeClustering(OperationResult result, @NotNull PageBase pageBase, String identifier) {
        try {
            deleteSpecCluster(result, pageBase, identifier);
            deleteSpecParentCluster(result, pageBase, identifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteSpecParentCluster(OperationResult result, @NotNull PageBase pageBase, String identifier)
            throws Exception {
        ResultHandler<AssignmentHolderType> handler = (object, parentResult) -> {

            try {
                pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, object.getOid(), result);
            } catch (ObjectNotFoundException e) {
                throw new RuntimeException(e);
            }

            return true;
        };

        ModelService service = pageBase.getModelService();
        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder()
                .raw()
                .resolveNames();

        ObjectFilter filter;
        if (identifier == null) {
            filter = pageBase.getPrismContext().queryFor(ParentClusterType.class).buildFilter();
        } else {
            filter = pageBase.getPrismContext().queryFor(ParentClusterType.class)
                    .item(ParentClusterType.F_IDENTIFIER).eq(identifier)
                    .buildFilter();
        }

        ObjectQuery queryType = pageBase.getPrismContext().queryFor(AssignmentHolderType.class)
                .type(ParentClusterType.class).filter(filter).build();

        service.searchObjectsIterative(AssignmentHolderType.class, queryType, handler, optionsBuilder.build(),
                pageBase.createSimpleTask("Search iterative ParentClusterType objects"), result);

    }

    public static void deleteSpecCluster(OperationResult result, @NotNull PageBase pageBase, String identifier)
            throws Exception {
        ResultHandler<AssignmentHolderType> handler = (object, parentResult) -> {

            try {
                pageBase.getRepositoryService().deleteObject(AssignmentHolderType.class, object.getOid(), result);
            } catch (ObjectNotFoundException e) {
                throw new RuntimeException(e);
            }

            return true;
        };

        ModelService service = pageBase.getModelService();
        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder()
                .raw()
                .resolveNames();

        ObjectFilter filter;
        if (identifier == null) {
            filter = pageBase.getPrismContext().queryFor(ClusterType.class).buildFilter();
        } else {
            filter = pageBase.getPrismContext().queryFor(ClusterType.class)
                    .item(ClusterType.F_IDENTIFIER).eq(identifier).buildFilter();
        }

        ObjectQuery queryType = pageBase.getPrismContext().queryFor(AssignmentHolderType.class)
                .type(ClusterType.class).filter(filter).build();

        service.searchObjectsIterative(AssignmentHolderType.class, queryType, handler, optionsBuilder.build(),
                pageBase.createSimpleTask("Search iterative ClusterType objects"), result);

    }

    public static List<PrismObject<UserType>> extractRoleMembers(PageBase pageBase, String objectId) {
        String getMembers = DOT_CLASS + "getRolesMembers";
        OperationResult result = new OperationResult(getMembers);

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

    public static @NotNull List<ClusteringObjectMapped> generateClusterUserMapped(@NotNull ClusterType cluster,
            PageBase pageBase, OperationResult operationResult) {
        List<String> members = cluster.getElements();
        List<ClusteringObjectMapped> mappedUsers = new ArrayList<>();

        List<String> prevRoles = new ArrayList<>();
        int prevIndex = -1;
        for (String groupOid : members) {
            PrismObject<UserType> user = getUserTypeObject(pageBase, groupOid, operationResult);
            List<String> rolesOid = getRolesOid(user.asObjectable());
            Collections.sort(rolesOid);
            if (prevRoles.equals(rolesOid)) {
                ClusteringObjectMapped clusteringObjectMapped = mappedUsers.get(prevIndex);
                clusteringObjectMapped.getElements().add(groupOid);
            } else {
                mappedUsers.add(new ClusteringObjectMapped(groupOid, rolesOid,
                        new ArrayList<>(Collections.singletonList(groupOid))));
                prevRoles = rolesOid;
                prevIndex++;
            }
        }
        return mappedUsers;
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

    public enum Mode {
        ROLE("ROLE"),
        USER("USER");

        private final String displayString;

        Mode(String displayString) {
            this.displayString = displayString;
        }

        public String getDisplayString() {
            return displayString;
        }

    }

    public static List<String> extractOid(List<PrismObject<UserType>> roleMembers) {
        List<String> membersOids = new ArrayList<>();
        for (PrismObject<UserType> roleMember : roleMembers) {
            membersOids.add(roleMember.getOid());
        }

        return membersOids;

    }

    public static @NotNull PrismObject<ParentClusterType> getParentClusterByIdentifier(@NotNull PageBase pageBase,
            String identifier, OperationResult result) {
        try {

            ObjectQuery query = pageBase.getPrismContext()
                    .queryFor(ParentClusterType.class)
                    .item(ParentClusterType.F_IDENTIFIER).eq(identifier)
                    .build();

            SearchResultList<PrismObject<ParentClusterType>> prismObjects = pageBase.getRepositoryService()
                    .searchObjects(ParentClusterType.class, query, null, result);
            return pageBase.getRepositoryService()
                    .getObject(ParentClusterType.class, prismObjects.get(0).getOid(), null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull PrismObject<UserType> getUserTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(UserType.class, oid, null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull PrismObject<FocusType> getFocusTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(FocusType.class, oid, null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull PrismObject<RoleType> getRoleTypeObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(RoleType.class, oid, null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull PrismObject<ClusterType> getClusterTypeObject(@NotNull PageBase pageBase, String oid) {
        OperationResult operationResult = new OperationResult("GetCluster");
        try {
            return pageBase.getRepositoryService().getObject(ClusterType.class, oid, null, operationResult);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static int countParentClusterTypeObjects(@NotNull PageBase pageBase) {
        OperationResult operationResult = new OperationResult("countClusters");
        try {
            return pageBase.getRepositoryService().countObjects(ParentClusterType.class, null, null, operationResult);
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
