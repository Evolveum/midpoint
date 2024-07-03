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

    public RoleAnalysisSessionDetectionOptionsWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
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

    @Override
    protected IModel<? extends PrismContainerWrapper<RoleAnalysisDetectionOptionType>> getContainerFormModel() {
        PrismContainerWrapperModel<RoleAnalysisSessionType, RoleAnalysisDetectionOptionType> containerWrapperModel =
                PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                        ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION));
        containerWrapperModel.getObject().setExpanded(true);
        return containerWrapperModel;
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

        boolean isOutlierSession = analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS);
        return wrapper -> {
            ItemName itemName = wrapper.getItemName();

            if (itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY)
                    || itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY)) {

                if (isOutlierSession) {
                    return ItemVisibility.HIDDEN;
                }
            }

            if (itemName.equivalent(RoleAnalysisDetectionOptionType.F_SENSITIVITY)) {
                if (!isOutlierSession) {
                    return ItemVisibility.HIDDEN;
                }
            }

            return ItemVisibility.AUTO;
        };
    }
}
