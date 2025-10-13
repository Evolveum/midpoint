/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AbstractRoleAnalysisConfiguration;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

        updatePrimaryOptions(null, null, null,
                false,
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

}
