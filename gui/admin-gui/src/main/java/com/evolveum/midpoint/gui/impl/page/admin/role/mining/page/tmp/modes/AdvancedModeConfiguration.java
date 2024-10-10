/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AbstractRoleAnalysisConfiguration;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public class AdvancedModeConfiguration extends AbstractRoleAnalysisConfiguration {

    RoleAnalysisService service;
    Task task;
    OperationResult result;

    public AdvancedModeConfiguration(
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
        if (getProcessMode() == null) {
            return;
        }
        RangeType propertyRange = new RangeType()
                .min(5.0)
                .max(Double.valueOf(getMaxPropertyCount()));

        updatePrimaryOptions(null, null, null,
                false,
                propertyRange,
                getDefaultAnalysisAttributes(),
                null,
                80.0,
                10, 5, false);

        updateDetectionOptions(5,
                5,
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
        if (getProcessMode().equals(RoleAnalysisProcessModeType.USER)) {
            propertiesClass = RoleType.class;
        }

        Integer maxPropertiesObjects;

        maxPropertiesObjects = service.countObjects(propertiesClass, null, null, task, result);

        if (maxPropertiesObjects == null) {
            maxPropertiesObjects = 1000000;
        }
        return maxPropertiesObjects;
    }

}
