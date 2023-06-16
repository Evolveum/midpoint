/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.mining.analyse.tools.utils;

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

public class ClusterObjectUtils {

    public static void importClusterTypeObject(OperationResult result, @NotNull PageBase pageBase, @NotNull PrismObject<ClusterType> cluster) {
        Task task = pageBase.createSimpleTask("Import ClusterType object");
        pageBase.getModelService().importObject(cluster, null, task, result);
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

}
