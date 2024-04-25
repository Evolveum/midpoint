/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AbstractAnalysisOption;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionOptionType;

public class OutlierModeConfiguration extends AbstractAnalysisOption {

    RoleAnalysisService service;
    Task task;
    OperationResult result;

    public OutlierModeConfiguration(RoleAnalysisService service, Task task, OperationResult result) {
        this.service = service;
        this.task = task;
        this.result = result;
    }

    public OutlierModeConfiguration() {
        super();
    }

    @Override
    public void setAnalysisSessionOption(AbstractAnalysisSessionOptionType analysisSessionOption) {
        super.setAnalysisSessionOption(analysisSessionOption);
    }

    @Override
    public void setDetectionOption(RoleAnalysisDetectionOptionType detectionOption) {
        super.setDetectionOption(detectionOption);
    }

    @Override
    public void setVisibilityHandler(ItemVisibilityHandler visibilityHandler) {
        super.setVisibilityHandler(visibilityHandler);
    }

}
