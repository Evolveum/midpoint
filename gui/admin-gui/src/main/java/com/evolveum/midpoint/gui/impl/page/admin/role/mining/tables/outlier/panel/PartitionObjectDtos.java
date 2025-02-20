/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.PartitionObjectDto.buildPartitionObjectList;

/**
 * A Data Transfer Object representing a role analysis partition details and its associated outlier.
 *
 * <p>Used in session/cluster outlier tables.</p>
 */
public class PartitionObjectDtos implements Serializable {

    transient List<PartitionObjectDto> partitionObjectDtoList;

    public PartitionObjectDtos(
            @NotNull AssignmentHolderType object,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result) {
        initModel(object, roleAnalysisService, task, result);
    }

    private void initModel(
            @NotNull AssignmentHolderType object,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task,
            @NotNull OperationResult result) {
        if (object instanceof RoleAnalysisSessionType sessionObject) {
            partitionObjectDtoList = buildPartitionObjectList(
                    roleAnalysisService, sessionObject, getLimit(), matchOutlierCategory(), task, result);
        } else if (object instanceof RoleAnalysisClusterType clusterObject) {
            partitionObjectDtoList = buildPartitionObjectList(
                    roleAnalysisService, clusterObject, getLimit(), matchOutlierCategory(), task, result);
        }
    }

    public Integer getLimit() {
        return null;
    }

    public OutlierCategoryType matchOutlierCategory() {
        return null;
    }

    public Collection<PartitionObjectDto> getPartitionObjectDtoList() {
        return partitionObjectDtoList;
    }

}
