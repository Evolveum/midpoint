/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

public abstract class AbstractAnalysisOption {

    RoleAnalysisProcessModeType processMode;
    AbstractAnalysisSessionOptionType analysisSessionOption;
    RoleAnalysisDetectionOptionType detectionOption;
    ItemVisibilityHandler visibilityHandler;

    public RoleAnalysisProcessModeType getProcessMode() {
        return processMode;
    }

    public void setProcessMode(RoleAnalysisProcessModeType processMode) {
        this.processMode = processMode;
    }

    public AbstractAnalysisSessionOptionType getAnalysisSessionOption() {
        return analysisSessionOption;
    }

    public void setAnalysisSessionOption(AbstractAnalysisSessionOptionType analysisSessionOption) {
        this.analysisSessionOption = analysisSessionOption;
    }

    public RoleAnalysisDetectionOptionType getDetectionOption() {
        return detectionOption;
    }

    public void setDetectionOption(RoleAnalysisDetectionOptionType detectionOption) {
        this.detectionOption = detectionOption;
    }

    public ItemVisibilityHandler getVisibilityHandler() {
        return visibilityHandler;
    }

    public void setVisibilityHandler(ItemVisibilityHandler visibilityHandler) {
        this.visibilityHandler = visibilityHandler;
    }

}
