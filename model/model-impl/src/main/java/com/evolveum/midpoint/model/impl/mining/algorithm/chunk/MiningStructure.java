/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.chunk;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.handler.Handler;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

public interface MiningStructure {
    MiningOperationChunk prepareRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, ModelService modelService,
            OperationResult result, Handler handler, Task task);
    MiningOperationChunk prepareUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, ModelService modelService,
            OperationResult result, Handler handler, Task task);
    MiningOperationChunk preparePartialRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, ModelService modelService,
            OperationResult result, Handler state, Task task);
    MiningOperationChunk preparePartialUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, ModelService modelService,
            OperationResult result, Handler handler, Task task);

}
