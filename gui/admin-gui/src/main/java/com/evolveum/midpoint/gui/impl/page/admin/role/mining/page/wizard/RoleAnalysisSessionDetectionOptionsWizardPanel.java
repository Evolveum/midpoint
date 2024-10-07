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
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

public class RoleAnalysisSessionDetectionOptionsWizardPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String WORK_PANEL_TYPE = "rm-detection";

    public RoleAnalysisSessionDetectionOptionsWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);
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
        if (getRoleAnalysisOption().getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            return createStringResource("PageRoleAnalysisSession.wizard.step.work.anomaly.detection.options");
        }

        return createStringResource("PageRoleAnalysisSession.wizard.step.work.role.detection.options");
    }

    @Override
    protected IModel<String> getTextModel() {
        if (getRoleAnalysisOption().getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            return createStringResource("PageRoleAnalysisSession.wizard.step.work.anomaly.detection.options.text");
        }

        return createStringResource("PageRoleAnalysisSession.wizard.step.work.role.detection.options.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        if (getRoleAnalysisOption().getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            return createStringResource("PageRoleAnalysisSession.wizard.step.work.anomaly.detection.options.subText");
        }

        return createStringResource("PageRoleAnalysisSession.wizard.step.work.role.detection.options.subText");
    }

    @Override
    public String getStepId() {
        return WORK_PANEL_TYPE;
    }

    @Override
    protected IModel<? extends PrismContainerWrapper<RoleAnalysisDetectionOptionType>> getContainerFormModel() {
        PrismContainerWrapperModel<RoleAnalysisSessionType, RoleAnalysisDetectionOptionType> containerWrapperModel =
                PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                        ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION));
        containerWrapperModel.getObject().setExpanded(true);
        return containerWrapperModel;
    }

    @SuppressWarnings("rawtypes")
    @Override
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

        RoleAnalysisOptionType analysisOption = getRoleAnalysisOption();

        boolean isOutlierSession = analysisOption.getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION);
        return wrapper -> {
            ItemName itemName = wrapper.getItemName();

            if(!isOutlierSession){
                if(itemName.equivalent(RoleAnalysisDetectionOptionType.F_STANDARD_DEVIATION)){
                    return ItemVisibility.HIDDEN;
                }

                if(itemName.equivalent(RoleAnalysisDetectionOptionType.F_FREQUENCY_THRESHOLD)){
                    return ItemVisibility.HIDDEN;
                }
            }else {
                if(itemName.equivalent(RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE)){
                    return ItemVisibility.HIDDEN;
                }
            }

            if ((itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY)
                    || itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY)) && isOutlierSession) {
                return ItemVisibility.HIDDEN;
            }

            if (itemName.equivalent(RoleAnalysisDetectionOptionType.F_SENSITIVITY) && !isOutlierSession) {
                return ItemVisibility.HIDDEN;
            }

            return ItemVisibility.AUTO;
        };
    }

    private RoleAnalysisOptionType getRoleAnalysisOption() {
        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
        return objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
    }
}
