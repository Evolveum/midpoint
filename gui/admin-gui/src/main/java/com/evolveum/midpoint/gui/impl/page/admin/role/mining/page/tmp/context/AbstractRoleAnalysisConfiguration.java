/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public abstract class AbstractRoleAnalysisConfiguration implements RoleAnalysisConfigurator {

    private RoleAnalysisProcessModeType processMode;
    private AbstractAnalysisSessionOptionType analysisSessionOption;
    private RoleAnalysisDetectionOptionType detectionOption;
    private ItemVisibilityHandler visibilityHandler;
    private RoleAnalysisSessionType object;

    public AbstractRoleAnalysisConfiguration(RoleAnalysisSessionType objectWrapper) {
        this.object = objectWrapper;
        RoleAnalysisOptionType analysisOption = object.getAnalysisOption();
        processMode = analysisOption.getProcessMode();

        if (processMode != null) {
            if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                this.analysisSessionOption = object.getRoleModeOptions();
            } else {
                this.analysisSessionOption = object.getUserModeOptions();
            }
        }

        this.detectionOption = object.getDefaultDetectionOption();
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

    public RoleAnalysisDetectionOptionType getDetectionOption() {
        return detectionOption;
    }

    public ItemVisibilityHandler getVisibilityHandler() {
        return visibilityHandler;
    }

    public void setVisibilityHandler(ItemVisibilityHandler visibilityHandler) {
        this.visibilityHandler = visibilityHandler;
    }

    //TODO load from system config
    public AnalysisAttributeSettingType getDefaultAnalysisAttributes() {
        //TODO parent org ref is multivalued. How to handle it (org root)?
        //TODO it make duplicate values in xml
        AnalysisAttributeSettingType value = new AnalysisAttributeSettingType();
        value.getPath().add(UserType.F_TITLE.toBean());
        value.getPath().add(UserType.F_PARENT_ORG_REF.toBean());
        value.getPath().add(UserType.F_ARCHETYPE_REF.toBean());
        value.getPath().add(UserType.F_LOCALITY.toBean());

        return value;
    }

    protected AbstractAnalysisSessionOptionType getPrimaryOptionContainerFormModel() {
        if (RoleAnalysisProcessModeType.ROLE.equals(getProcessMode())) {
            RoleAnalysisSessionOptionType roleModeOption = object.getRoleModeOptions();
            if (roleModeOption == null) {
                roleModeOption = new RoleAnalysisSessionOptionType();
                object.setRoleModeOptions(roleModeOption);
            }
            return roleModeOption;
        }

        UserAnalysisSessionOptionType userModeOption = object.getUserModeOptions();
        if (userModeOption == null) {
            userModeOption = new UserAnalysisSessionOptionType();
            object.setUserModeOptions(userModeOption);
        }
        return userModeOption;
    }

    public void updatePrimaryOptions(
            SearchFilterType userSearchFilter,
            SearchFilterType roleSearchFilter,
            SearchFilterType assignmentSearchFilter,
            boolean isIndirect,
            AnalysisAttributeSettingType analysisAttributeSetting,
            ClusteringAttributeSettingType clusteringAttributeSetting,
            Double similarityThreshold,
            Integer minMembersCount,
            Integer minPropertiesOverlap,
            boolean detailedAnalysis) {

//        try {
            AbstractAnalysisSessionOptionType sessionOptions = getPrimaryOptionContainerFormModel();
            sessionOptions.isIndirect(isIndirect)
                    .userAnalysisAttributeSetting(analysisAttributeSetting)
                    .clusteringAttributeSetting(clusteringAttributeSetting)
                    .similarityThreshold(similarityThreshold)
                    .minMembersCount(minMembersCount)
                    .minPropertiesOverlap(minPropertiesOverlap)
                    .detailedAnalysis(detailedAnalysis);
            if (userSearchFilter != null) {
                sessionOptions.setUserSearchFilter(userSearchFilter);
            }

            if (roleSearchFilter != null) {
                sessionOptions.setRoleSearchFilter(roleSearchFilter);
            }

            if (assignmentSearchFilter != null) {
                sessionOptions.setAssignmentSearchFilter(assignmentSearchFilter);
            }

    }

    public void updateDetectionOptions(
            Integer minRolesOccupancy,
            Integer minUserOccupancy,
            Double sensitivity,
            RangeType frequencyRange,
            RoleAnalysisDetectionProcessType detectionProcessMode,
            RangeType standardDeviation,
            Double frequencyThreshold) {

            RoleAnalysisDetectionOptionType primaryOptions = object.getDefaultDetectionOption();
            if (primaryOptions == null) {
                primaryOptions = new RoleAnalysisDetectionOptionType();
                object.setDefaultDetectionOption(primaryOptions);
            }
            primaryOptions.minRolesOccupancy(minRolesOccupancy)
                    .minUserOccupancy(minUserOccupancy)
                    .sensitivity(sensitivity)
                    .frequencyRange(frequencyRange)
                    .standardDeviation(standardDeviation)
                    .frequencyThreshold(frequencyThreshold)
                    .detectionProcessMode(detectionProcessMode);
    }
}
