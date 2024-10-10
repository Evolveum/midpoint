/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AbstractRoleAnalysisConfiguration;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class BalancedCoverageModeConfiguration extends AbstractRoleAnalysisConfiguration {

    RoleAnalysisService service;
    Task task;
    OperationResult result;

    public BalancedCoverageModeConfiguration(
            RoleAnalysisService service,
            RoleAnalysisSessionType objectWrapper,
            Task task,
            OperationResult result) {
        super(objectWrapper);
        this.service = service;
        this.task = task;
        this.result = result;
    }

    @Override
    public void updateConfiguration() {
        RangeType propertyRange = new RangeType()
                .min(2.0)
                .max(Double.valueOf(getMaxPropertyCount()));

        updatePrimaryOptions(null,null, null,
                false,
                propertyRange,
                getDefaultAnalysisAttributes(),
                null,
                80.0,
                2, 2, false);

        updateDetectionOptions(2,
                2,
                null,
                new RangeType()
                        .min(10.0)
                        .max(100.0),
                RoleAnalysisDetectionProcessType.FULL,
                null,
                null);
    }

    public @NotNull Integer getMaxPropertyCount() {
        Class<? extends ObjectType> propertiesClass = UserType.class;
        if (RoleAnalysisProcessModeType.USER.equals(getProcessMode())) {
            propertiesClass = RoleType.class;
        }

        Integer maxPropertiesObjects;

        maxPropertiesObjects = service.countObjects(propertiesClass, null, null, task, result);

        if (maxPropertiesObjects == null) {
            maxPropertiesObjects = 1000000;
        }
        return maxPropertiesObjects;
    }

    public @NotNull Integer getMinPropertyCount(Integer maxPropertiesObjects) {
        return maxPropertiesObjects < 10 ? 1 : 10;
    }
}
