/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParentClusterType;
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

import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

public class ClusterObjectUtils {

    public static void importClusterTypeObject(OperationResult result, @NotNull PageBase pageBase,
            @NotNull PrismObject<ClusterType> cluster) {
        Task task = pageBase.createSimpleTask("Import ClusterType object");
        pageBase.getModelService().importObject(cluster, null, task, result);
    }

    public static void importParentClusterTypeObject(OperationResult result, @NotNull PageBase pageBase,
            double density, int counsist, List<String> childRef, String identifier) {
        Task task = pageBase.createSimpleTask("Import ClusterType object");

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

    public static void deleteClusterObjects(OperationResult result, @NotNull PageBase pageBase) throws Exception {
        ResultHandler<ClusterType> handler = (object, parentResult) -> {

            try {
                pageBase.getRepositoryService().deleteObject(ClusterType.class, object.getOid(), result);
            } catch (ObjectNotFoundException e) {
                throw new RuntimeException(e);
            }

            return true;
        };

        ModelService service = pageBase.getModelService();
        GetOperationOptionsBuilder optionsBuilder = pageBase.getSchemaService().getOperationOptionsBuilder()
                .raw()
                .resolveNames();
        service.searchObjectsIterative(ClusterType.class, null, handler, optionsBuilder.build(),
                pageBase.createSimpleTask("Search iterative cluster objects"), result);
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

}
