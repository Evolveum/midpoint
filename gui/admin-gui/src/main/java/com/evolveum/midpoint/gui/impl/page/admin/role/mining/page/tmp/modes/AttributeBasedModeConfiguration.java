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

public class AttributeBasedModeConfiguration extends AbstractRoleAnalysisConfiguration {

    RoleAnalysisService service;
    Task task;
    OperationResult result;

    public AttributeBasedModeConfiguration(
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
        updatePrimaryOptions(null, null, null,
                false,
                getDefaultAnalysisAttributes(),
                null,
                80.0,
                5, 5, false);

        updateDetectionOptions(5,
                5,
                null,
                createDetectionRange(),
                RoleAnalysisDetectionProcessType.FULL,
                null,
                null);
    }

    // TODO: We should probably use department mode for discovery of department roles.
    //  For example roles that cover 90%+ of users in a department should be used as department inducement.
    //  Also these structured classes should be used for migration process specification.
    private RangeType createDetectionRange() {
        return new RangeType().min(90.0).max(100.0);
    }

}
