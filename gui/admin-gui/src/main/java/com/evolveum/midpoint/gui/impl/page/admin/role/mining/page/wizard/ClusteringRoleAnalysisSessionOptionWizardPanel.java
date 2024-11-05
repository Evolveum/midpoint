/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession.getSessionOptionContainer;

public class ClusteringRoleAnalysisSessionOptionWizardPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String WORK_PANEL_TYPE = "rm-option-clustering";

    public ClusteringRoleAnalysisSessionOptionWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);

    }

    @Override
    protected IModel<? extends PrismContainerWrapper<AbstractAnalysisSessionOptionType>> getContainerFormModel() {
        AssignmentHolderDetailsModel<RoleAnalysisSessionType> detailsModel = getDetailsModel();
        return getSessionOptionContainer(detailsModel);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected boolean checkMandatory(@NotNull ItemWrapper itemWrapper) {
        ItemName itemName = itemWrapper.getItemName();
        if (itemName.equivalent(AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_USER_ANALYSIS_ATTRIBUTE_SETTING)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING)) {
            return false;
        }
        return itemName.equivalent(AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP);
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        AssignmentHolderDetailsModel<RoleAnalysisSessionType> detailsModel = getDetailsModel();
        RoleAnalysisSessionType session = detailsModel.getObjectType();
        RoleAnalysisProcessModeType processMode = session.getAnalysisOption().getProcessMode();

        String orig = detailsModel.getObjectType().getName().getOrig();
        boolean applyAdvanceSetting = orig.startsWith("advs_");

        return wrapper -> {
            ItemName itemName = wrapper.getItemName();
            if (processMode.equals(RoleAnalysisProcessModeType.ROLE)
                    && itemName.equals(AbstractAnalysisSessionOptionType.F_MIN_ACCESS_POPULARITY)) {
                return ItemVisibility.HIDDEN;
            }

            if (processMode.equals(RoleAnalysisProcessModeType.USER)
                    && itemName.equals(AbstractAnalysisSessionOptionType.F_MIN_USERS_POPULARITY)) {
                return ItemVisibility.HIDDEN;
            }

            if (!applyAdvanceSetting && (itemName.equals(AbstractAnalysisSessionOptionType.F_MAX_ACCESS_POPULARITY)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_MAX_USERS_POPULARITY))) {
                return ItemVisibility.HIDDEN;
            }

            if (itemName.equals(AbstractAnalysisSessionOptionType.F_USER_SEARCH_FILTER)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_ROLE_SEARCH_FILTER)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_ASSIGNMENT_SEARCH_FILTER)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_IS_INDIRECT)
            ) {
                return ItemVisibility.HIDDEN;
            }

            if (itemName.equals(AbstractAnalysisSessionOptionType.F_DETAILED_ANALYSIS)) {
                if (getDetailsModel().getObjectType() == null) {
                    return ItemVisibility.HIDDEN;
                }
                RoleAnalysisOptionType analysisOption = getDetailsModel().getObjectType().getAnalysisOption();
                if (analysisOption == null || analysisOption.getAnalysisCategory() == null) {
                    return ItemVisibility.HIDDEN;
                }
                if (!analysisOption.getAnalysisProcedureType().equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
                    return ItemVisibility.HIDDEN;
                }
            }

            return ItemVisibility.AUTO;
        };
    }

    @Override
    protected boolean isVisibleSubContainer(PrismContainerWrapper c) {
        return true;
    }

    @Override
    protected boolean isShowEmptyButtonVisible() {
        return false;
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
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.clustering.option");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.clustering.option.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.clustering.option.subText");
    }

    @Override
    public String getStepId() {
        return WORK_PANEL_TYPE;
    }

}
