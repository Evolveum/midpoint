/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.chunk;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.handler.Handler;
import com.evolveum.midpoint.repo.api.RepositoryService;

import org.jetbrains.annotations.NotNull;


import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

public interface MiningStructure {
    MiningOperationChunk prepareRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, RepositoryService repoService,
            OperationResult result, Handler handler);
    MiningOperationChunk prepareUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, RepositoryService repoService,
            OperationResult result, Handler handler);
    MiningOperationChunk preparePartialRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, RepositoryService repoService,
            OperationResult result, Handler state);
    MiningOperationChunk preparePartialUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, RepositoryService repoService,
            OperationResult result, Handler handler);

}
