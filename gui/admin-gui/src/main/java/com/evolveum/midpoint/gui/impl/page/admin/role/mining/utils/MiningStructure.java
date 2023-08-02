/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningOperationChunk;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

public interface MiningStructure {
    MiningOperationChunk prepareRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state);
    MiningOperationChunk prepareUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state);
    MiningOperationChunk preparePartialRoleBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state);
    MiningOperationChunk preparePartialUserBasedStructure(@NotNull RoleAnalysisClusterType cluster, PageBase pageBase,
            OperationResult result, String state);

}
