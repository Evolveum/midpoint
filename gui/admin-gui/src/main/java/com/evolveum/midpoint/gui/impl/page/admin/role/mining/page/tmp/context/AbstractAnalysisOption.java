/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractAnalysisOption {

    RoleAnalysisProcessModeType processMode;
    AbstractAnalysisSessionOptionType analysisSessionOption;
    RoleAnalysisDetectionOptionType detectionOption;
    ItemVisibilityHandler visibilityHandler;
    PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper;

    public AbstractAnalysisOption(@NotNull PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper) {
        this.objectWrapper = objectWrapper;
        PrismObject<RoleAnalysisSessionType> object = objectWrapper.getObject();
        RoleAnalysisSessionType realValue = object.getRealValue();
        RoleAnalysisOptionType analysisOption = realValue.getAnalysisOption();
        this.processMode = analysisOption.getProcessMode();

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            this.analysisSessionOption = realValue.getRoleModeOptions();
        } else {
            this.analysisSessionOption = realValue.getUserModeOptions();
        }

        this.detectionOption = realValue.getDefaultDetectionOption();
    }

    public void applySettings() {
        PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper = getObjectWrapper();
        AbstractAnalysisSessionOptionType analysisSessionOption = getAnalysisSessionOption();
        RoleAnalysisProcessModeType processMode = getProcessMode();


        RoleAnalysisSessionType realValue = objectWrapper.getObject().getRealValue();
        realValue.getAnalysisOption().setAnalysisCategory(RoleAnalysisCategoryType.OUTLIERS);
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            realValue.setRoleModeOptions((RoleAnalysisSessionOptionType) analysisSessionOption);
        } else {
            realValue.setUserModeOptions((UserAnalysisSessionOptionType) analysisSessionOption);
        }

        realValue.setDefaultDetectionOption(getDetectionOption());
    }

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

    public PrismObjectWrapper<RoleAnalysisSessionType> getObjectWrapper() {
        return objectWrapper;
    }

    public void setObjectWrapper(PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper) {
        this.objectWrapper = objectWrapper;
    }

    protected PrismContainerValueWrapper<Containerable> getPrimaryOptionContainer(
            @NotNull PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper) {
        RoleAnalysisProcessModeType processMode = getProcessMode();
        try {
            if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                return objectWrapper.findContainerValue(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS);
            } else {
                return objectWrapper.findContainerValue(RoleAnalysisSessionType.F_USER_MODE_OPTIONS);
            }
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't find container form RoleAnalysisSessionType model for process mode: "
                    + processMode, e);
        }
    }

    protected PrismContainerWrapper<RoleAnalysisDetectionOptionType> getDetectionOptionContainer(
            @NotNull PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper) {
        try {
            return objectWrapper.findContainer(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION);
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't find container form RoleAnalysisSessionType model for detection options",
                    e);
        }
    }
}
