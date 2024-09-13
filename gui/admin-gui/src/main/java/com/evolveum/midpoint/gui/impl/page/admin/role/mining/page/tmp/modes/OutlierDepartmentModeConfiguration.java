/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AbstractRoleAnalysisConfiguration;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public class OutlierDepartmentModeConfiguration extends AbstractRoleAnalysisConfiguration {

    RoleAnalysisService service;
    Task task;
    OperationResult result;
    LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper;

    public OutlierDepartmentModeConfiguration(
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
        RangeType propertyRange = createPropertyRange();
        ClusteringAttributeSettingType clusteringSetting = createClusteringSetting();

        updatePrimaryOptions(null,
                false,
                propertyRange,
                getDefaultAnalysisAttributes(),
                clusteringSetting,
                80.0,
                5,
                2,
                false);

        updateDetectionOptions(2,
                2,
                0.7,
                createDetectionRange(),
                RoleAnalysisDetectionProcessType.SKIP);
    }

    private RangeType createPropertyRange() {
        double minPropertyCount = 2.0;
        double maxPropertyCount = Double.valueOf(getMaxPropertyCount());
        return new RangeType().min(minPropertyCount).max(maxPropertyCount);
    }

    private @NotNull ClusteringAttributeSettingType createClusteringSetting() {
        RoleAnalysisAttributeDef orgAssignment = RoleAnalysisAttributeDefUtils.getOrgAssignment();
        ClusteringAttributeSettingType clusteringSetting = new ClusteringAttributeSettingType();
        ClusteringAttributeRuleType rule = new ClusteringAttributeRuleType()
                .attributeIdentifier(orgAssignment.getDisplayValue())
                .isMultiValue(true)
                .weight(1.0)
                .similarity(100.0);
        clusteringSetting.getClusteringAttributeRule().add(rule);
        return clusteringSetting;
    }

    // TODO: We should probably use department mode for discovery of department roles.
    //  For example roles that cover 90%+ of users in a department should be used as department inducement.
    //  Also these structured classes should be used for migration process specification.
    private RangeType createDetectionRange() {
        return new RangeType().min(2.0).max(2.0);
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
