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

public class OutlierModeConfiguration extends AbstractRoleAnalysisConfiguration {

    RoleAnalysisService service;
    Task task;
    OperationResult result;

    public OutlierModeConfiguration(
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
        //TODO after implementing use isIndirect
//        boolean isIndirect = getProcessMode().equals(RoleAnalysisProcessModeType.USER);

        updatePrimaryOptions(null, null, null,
                false,
                getDefaultAnalysisAttributes(),
                null,
                80.0,
                5, 2, false);

        //TODO there is inconsistency with role mining detection options (TBD)
        updateDetectionOptions(2,
                2,
                70.0,
                null,
                RoleAnalysisDetectionProcessType.SKIP,
                new RangeType()
                        .min(2.0)
                        .max(2.0),
                50.0);
    }
}
