/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes;

import org.jetbrains.annotations.NotNull;

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
        RangeType propertyRange = createPropertyRange();
//        ClusteringAttributeSettingType clusteringSetting = createClusteringSetting();

        updatePrimaryOptions(null,
                false,
                propertyRange,
                getDefaultAnalysisAttributes(),
                null,
                80.0,
                5,
                5,
                false);

        updateDetectionOptions(5,
                5,
                null,
                createDetectionRange(),
                RoleAnalysisDetectionProcessType.FULL,
                null,
                null);
    }

    private RangeType createPropertyRange() {
        double minPropertyCount = 5;
        double maxPropertyCount = getMaxPropertyCount();
        return new RangeType().min(minPropertyCount).max(maxPropertyCount);
    }

    //TODO let the user choose the archetype or root for departmnet structre
    private @NotNull ClusteringAttributeSettingType createClusteringSetting() {
        ClusteringAttributeSettingType clusteringSetting = new ClusteringAttributeSettingType();
        ClusteringAttributeRuleType rule = new ClusteringAttributeRuleType()
                .path(UserType.F_TITLE.toBean())
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
        return new RangeType().min(90.0).max(100.0);
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
