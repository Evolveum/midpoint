/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AbstractAnalysisOption;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public class StandardModeConfiguration extends AbstractAnalysisOption {

    RoleAnalysisService service;
    Task task;
    OperationResult result;
    PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper;

    public StandardModeConfiguration(
            RoleAnalysisService service,
            PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper,
            Task task,
            OperationResult result) {
        super(objectWrapper);
        this.service = service;
        this.task = task;
        this.result = result;
        this.objectWrapper = objectWrapper;
    }



    @Override
    public AbstractAnalysisSessionOptionType getAnalysisSessionOption() {
        AbstractAnalysisSessionOptionType analysisSessionOption = new AbstractAnalysisSessionOptionType();
        analysisSessionOption.setSimilarityThreshold(100.0);
        analysisSessionOption.setIsIndirect(false);
        analysisSessionOption.setMinMembersCount(2);
        analysisSessionOption.setMinPropertiesOverlap(1);
        analysisSessionOption.setPropertiesRange(new RangeType()
                .min(1.0)
                .max(Double.valueOf(getMaxPropertyCount())));
        return analysisSessionOption;
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

    public @NotNull Integer getMinPropertyCount(Integer maxPropertiesObjects) {
        return maxPropertiesObjects < 10 ? 1 : 10;
    }
}
