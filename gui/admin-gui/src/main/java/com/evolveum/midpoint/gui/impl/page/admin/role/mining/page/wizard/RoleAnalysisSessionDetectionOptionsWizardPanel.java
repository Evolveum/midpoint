/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import static com.evolveum.midpoint.model.api.expr.MidpointFunctions.LOGGER;

public class RoleAnalysisSessionDetectionOptionsWizardPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String WORK_PANEL_TYPE = "rm-detection";

    private static final double DEFAULT_MIN_FREQUENCY = 30.0;
    private static final double DEFAULT_MAX_FREQUENCY = 100.0;

    public RoleAnalysisSessionDetectionOptionsWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        updateNewValues();

        super.onInitialize();
    }

    private void updateNewValues() {
        try {
            RoleAnalysisSessionType realValue = getDetailsModel().getObjectWrapper().getValue().getRealValue();
            PrismContainerValueWrapper<RoleAnalysisDetectionOptionType> sessionType = getContainerFormModel().getObject()
                    .getValue();

            RoleAnalysisOptionType analysisOption = realValue.getAnalysisOption();
            if (analysisOption.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
                setNewValue(sessionType, RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY, realValue.getRoleModeOptions().getMinPropertiesOverlap());
                setNewValue(sessionType, RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY, realValue.getRoleModeOptions().getMinMembersCount());
            } else {
                setNewValue(sessionType, RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY, realValue.getUserModeOptions().getMinMembersCount());
                setNewValue(sessionType, RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY, realValue.getUserModeOptions().getMinPropertiesOverlap());
            }

            setNewValue(sessionType, RoleAnalysisDetectionOptionType.F_DETECTION_PROCESS_MODE, RoleAnalysisDetectionProcessType.PARTIAL);

            if (analysisOption.getAnalysisCategory().equals(RoleAnalysisCategoryType.OUTLIERS)) {
                setNewValue(sessionType, RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE, new RangeType()
                        .min(2.0)
                        .max(2.0));
            } else {
                setNewValue(sessionType, RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE, new RangeType()
                        .min(DEFAULT_MIN_FREQUENCY)
                        .max(DEFAULT_MAX_FREQUENCY));
            }
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private void setNewValue(PrismContainerValueWrapper<RoleAnalysisDetectionOptionType> sessionType,
            ItemName itemName, Object realValue) throws SchemaException {

        if (sessionType.findProperty(itemName) != null) {
            sessionType.findProperty(itemName).getValue().setRealValue(realValue);
        } else {
            LOGGER.warn("Property not found: " + itemName);
        }
    }

    @Override
    protected IModel<? extends PrismContainerWrapper<RoleAnalysisDetectionOptionType>> getContainerFormModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION));
    }

    @Override
    protected String getPanelType() {
        return WORK_PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.filter.options");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.filter.options.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.filter.options.subText");
    }

    @Override
    public String getStepId() {
        return WORK_PANEL_TYPE;
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        ItemName itemName = itemWrapper.getItemName();
        if (itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY)
                || itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY)
                || itemName.equivalent(RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {

        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
        RoleAnalysisOptionType option = objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
        RoleAnalysisCategoryType analysisCategory = option.getAnalysisCategory();

        return wrapper -> {
            ItemName itemName = wrapper.getItemName();

            if (itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY)
                    || itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY)) {

                if (analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS)) {
                    return ItemVisibility.HIDDEN;
                }
            }

            return ItemVisibility.AUTO;
        };
    }
}
