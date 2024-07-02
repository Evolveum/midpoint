/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.model.api.expr.MidpointFunctions.LOGGER;

public abstract class AbstractRoleAnalysisConfiguration implements RoleAnalysisConfigurator {

    RoleAnalysisProcessModeType processMode;
    AbstractAnalysisSessionOptionType analysisSessionOption;
    RoleAnalysisDetectionOptionType detectionOption;
    ItemVisibilityHandler visibilityHandler;
    LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper;

    public AbstractRoleAnalysisConfiguration(LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper) {
        this.objectWrapper = objectWrapper;
        PrismObject<RoleAnalysisSessionType> object = objectWrapper.getObject().getObject();
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

    public LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> getObjectWrapper() {
        return objectWrapper;
    }

    public void setObjectWrapper(LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper) {
        this.objectWrapper = objectWrapper;
    }

    public AnalysisAttributeSettingType getDefaultAnalysisAttributes() {
        AnalysisAttributeSettingType value = new AnalysisAttributeSettingType();
        List<AnalysisAttributeRuleType> analysisAttributeRule = new ArrayList<>();
        RoleAnalysisAttributeDef title = RoleAnalysisAttributeDefUtils.getTitle();
        RoleAnalysisAttributeDef archetypeRef = RoleAnalysisAttributeDefUtils.getArchetypeRef();
        RoleAnalysisAttributeDef locality = RoleAnalysisAttributeDefUtils.getLocality();
        RoleAnalysisAttributeDef orgAssignment = RoleAnalysisAttributeDefUtils.getOrgAssignment();

        analysisAttributeRule
                .add(new AnalysisAttributeRuleType()
                        .attributeIdentifier(title.getDisplayValue())
                        .propertyType(UserType.COMPLEX_TYPE));
        analysisAttributeRule
                .add(new AnalysisAttributeRuleType()
                        .attributeIdentifier(archetypeRef.getDisplayValue())
                        .propertyType(UserType.COMPLEX_TYPE));
        analysisAttributeRule
                .add(new AnalysisAttributeRuleType()
                        .attributeIdentifier(locality.getDisplayValue())
                        .propertyType(UserType.COMPLEX_TYPE));
        analysisAttributeRule
                .add(new AnalysisAttributeRuleType()
                        .attributeIdentifier(orgAssignment.getDisplayValue())
                        .propertyType(UserType.COMPLEX_TYPE));
        analysisAttributeRule
                .add(new AnalysisAttributeRuleType()
                        .attributeIdentifier(archetypeRef.getDisplayValue())
                        .propertyType(RoleType.COMPLEX_TYPE));

        value.getAnalysisAttributeRule().addAll(analysisAttributeRule);

        return value;
    }

    protected IModel<? extends PrismContainerWrapper<AbstractAnalysisSessionOptionType>> getPrimaryOptionContainerFormModel(
            @NotNull LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel) {
        if (getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            return PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                    ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS));
        }
        return PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS));
    }

    protected IModel<? extends PrismContainerWrapper<RoleAnalysisDetectionOptionType>> getDetectionOptionFormModel(
            @NotNull LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel
    ) {
        return PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION));
    }

    private void setNewPrimaryOptionValue(@NotNull PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> sessionType,
            ItemName itemName, Object realValue) throws SchemaException {

        sessionType.findProperty(itemName).getValue().setRealValue(realValue);

    }

    private void setNewDetectionOptionValue(@NotNull PrismContainerValueWrapper<RoleAnalysisDetectionOptionType> sessionType,
            ItemName itemName, Object realValue) throws SchemaException {

        if (sessionType.findProperty(itemName) != null) {
            sessionType.findProperty(itemName).getValue().setRealValue(realValue);
        } else {
            LOGGER.warn("Property not found: " + itemName);
        }
    }

    public void updatePrimaryOptions(
            QueryType query,
            boolean isIndirect,
            RangeType propertiesRange,
            AnalysisAttributeSettingType analysisAttributeSetting,
            ClusteringAttributeSettingType clusteringAttributeSetting,
            Double similarityThreshold,
            Integer minMembersCount,
            Integer minPropertiesOverlap,
            boolean detailedAnalysis) {

        try {
            PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> primaryOptions = getPrimaryOptionContainerFormModel(
                    objectWrapper).getObject().getValue();
            setNewPrimaryOptionValue(primaryOptions, AbstractAnalysisSessionOptionType.F_IS_INDIRECT, isIndirect);
            setNewPrimaryOptionValue(primaryOptions, AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE, propertiesRange);
            setNewPrimaryOptionValue(primaryOptions, AbstractAnalysisSessionOptionType.F_ANALYSIS_ATTRIBUTE_SETTING, analysisAttributeSetting);
            setNewPrimaryOptionValue(primaryOptions, AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING, clusteringAttributeSetting);
            setNewPrimaryOptionValue(primaryOptions, AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD, similarityThreshold);
            setNewPrimaryOptionValue(primaryOptions, AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT, minMembersCount);
            setNewPrimaryOptionValue(primaryOptions, AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP, minPropertiesOverlap);
            setNewPrimaryOptionValue(primaryOptions, AbstractAnalysisSessionOptionType.F_QUERY, query);
            setNewPrimaryOptionValue(primaryOptions, AbstractAnalysisSessionOptionType.F_DETAILED_ANALYSIS, detailedAnalysis);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDetectionOptions(
            Integer minRolesOccupancy,
            Integer minUserOccupancy,
            Double sensitivity,
            RangeType frequencyRange,
            RoleAnalysisDetectionProcessType detectionProcessMode) {

        try {
            PrismContainerValueWrapper<RoleAnalysisDetectionOptionType> primaryOptions = getDetectionOptionFormModel(
                    objectWrapper).getObject().getValue();
            setNewDetectionOptionValue(primaryOptions, RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY, minRolesOccupancy);
            setNewDetectionOptionValue(primaryOptions, RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY, minUserOccupancy);
            if (sensitivity != null) {
                setNewDetectionOptionValue(primaryOptions, RoleAnalysisDetectionOptionType.F_SENSITIVITY, sensitivity);
            }
            setNewDetectionOptionValue(primaryOptions, RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE, frequencyRange);
            setNewDetectionOptionValue(primaryOptions, RoleAnalysisDetectionOptionType.F_DETECTION_PROCESS_MODE, detectionProcessMode);

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }
}
