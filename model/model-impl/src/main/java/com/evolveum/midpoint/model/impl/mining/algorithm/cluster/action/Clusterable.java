/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action;

import java.util.List;

import com.evolveum.midpoint.common.mining.objects.handler.RoleAnalysisProgressIncrement;
import com.evolveum.midpoint.model.api.ModelService;

import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public interface Clusterable {
    List<PrismObject<RoleAnalysisClusterType>> executeClustering(@NotNull RoleAnalysisSessionType session,
            ModelService modelService, RoleAnalysisProgressIncrement handler, Task task, OperationResult result);
}
