/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AbstractRoleAnalysisConfiguration;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public class OutlierModeConfiguration extends AbstractRoleAnalysisConfiguration {

    RoleAnalysisService service;
    Task task;
    OperationResult result;
    LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper;

    public OutlierModeConfiguration(
            RoleAnalysisService service,
            LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper,
            Task task,
            OperationResult result) {
        super(objectWrapper);
        this.service = service;
        this.task = task;
        this.result = result;
        this.objectWrapper = objectWrapper;
    }

    @Override
    public void updateConfiguration() {
        RangeType propertyRange = new RangeType()
                .min(5.0)
                .max(Double.valueOf(getMaxPropertyCount()));

        //TODO after implementing use isIndirect
        boolean isIndirect = getProcessMode().equals(RoleAnalysisProcessModeType.USER);

        updatePrimaryOptions(null,
                false,
                propertyRange,
                getDefaultAnalysisAttributes(),
                null,
                90.0,
                10,
                2,
                false);

        updateDetectionOptions(2,
                2,
                20.0,
                new RangeType()
                        .min(2.0)
                        .max(2.0),
                RoleAnalysisDetectionProcessType.SKIP);
    }

    @Override
    public AbstractAnalysisSessionOptionType getAnalysisSessionOption() {
        return super.getAnalysisSessionOption();
    }

    @Override
    public RoleAnalysisDetectionOptionType getDetectionOption() {
        return super.getDetectionOption();
    }

    @Override
    public ItemVisibilityHandler getVisibilityHandler() {
        return super.getVisibilityHandler();
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
