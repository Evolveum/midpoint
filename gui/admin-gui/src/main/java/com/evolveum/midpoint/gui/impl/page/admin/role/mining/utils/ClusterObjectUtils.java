/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParentClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;

import java.util.*;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.getRolesOid;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

public class ClusterObjectUtils {

    public static void importClusterTypeObject(OperationResult result, @NotNull PageBase pageBase,
            @NotNull PrismObject<ClusterType> cluster) {
        Task task = pageBase.createSimpleTask("Import ClusterType object");
        pageBase.getModelService().importObject(cluster, null, task, result);
    }

    public static void importParentClusterTypeObject(OperationResult result, @NotNull PageBase pageBase,
            double density, int counsist, List<String> childRef, String identifier) {
        Task task = pageBase.createSimpleTask("Import ParentClusterType object");

        PrismObject<ParentClusterType> parentClusterTypePrismObject = generateClusterObject(pageBase, density, counsist,
                childRef, identifier);
        pageBase.getModelService().importObject(parentClusterTypePrismObject, null, task, result);
    }

    public static PrismObject<ParentClusterType> generateClusterObject(PageBase pageBase, double density, int consist,
            List<String> childRef, String identifier) {
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
        clusterType.setName(PolyStringType.fromOrig(identifier));
        clusterType.setOid(String.valueOf(uuid));

        clusterType.getClustersRef().addAll(childRef);
        clusterType.setDensity(String.format("%.3f", density));
        clusterType.setConsist(consist);
        clusterType.setIdentifier(identifier);

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

    public static Map<List<String>, List<String>> prepareExactUsersSetByRoles(OperationResult result, @NotNull PageBase pageBase,
            int minRolesCount, ObjectFilter userQuery) {

        Map<List<String>, List<String>> userTypeMap = new HashMap<>();

        ResultHandler<UserType> resultHandler = (object, parentResult) -> {
            try {

                List<String> rolesOid = getRolesOid(object.asObjectable());
                if (rolesOid.size() >= minRolesCount) {
                    Collections.sort(rolesOid);
                    userTypeMap.computeIfAbsent(rolesOid, k -> new ArrayList<>()).add(object.asObjectable().getOid());
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder();
        RepositoryService repositoryService = pageBase.getRepositoryService();
        ObjectQuery objectQuery = pageBase.getPrismContext().queryFactory().createQuery(userQuery);

        try {
            repositoryService.searchObjectsIterative(UserType.class, objectQuery, resultHandler, optionsBuilder.build(),
                    true, result);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        System.out.println(userTypeMap.size());
        return userTypeMap;
    }

    public static @NotNull PrismObject<UserType> getUserObject(@NotNull PageBase pageBase, String oid,
            OperationResult result) {
        try {
            return pageBase.getRepositoryService().getObject(UserType.class, oid, null, result);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e);
        }
    }
}
