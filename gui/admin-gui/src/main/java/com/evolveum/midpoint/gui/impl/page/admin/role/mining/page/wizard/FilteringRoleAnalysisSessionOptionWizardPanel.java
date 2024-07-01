/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public class FilteringRoleAnalysisSessionOptionWizardPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String WORK_PANEL_TYPE = "rm-option-filtering";

    public FilteringRoleAnalysisSessionOptionWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    private void setNewValue(PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> sessionType,
            ItemName itemName, Object realValue) throws SchemaException {

        sessionType.findProperty(itemName).getValue().setRealValue(realValue);

    }

    @Override
    protected IModel<? extends PrismContainerWrapper<AbstractAnalysisSessionOptionType>> getContainerFormModel() {
        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
        RoleAnalysisOptionType processModeObject = objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
        RoleAnalysisProcessModeType processMode = processModeObject.getProcessMode();
        PrismContainerWrapperModel<RoleAnalysisSessionType, AbstractAnalysisSessionOptionType> containerWrapperModel;
        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            containerWrapperModel = PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                    ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS));
        } else {
            containerWrapperModel = PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                    ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS));
        }
        containerWrapperModel.getObject().setExpanded(true);
        return containerWrapperModel;
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        ItemName itemName = itemWrapper.getItemName();
        return itemName.equivalent(AbstractAnalysisSessionOptionType.F_QUERY)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_IS_INDIRECT)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE);
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            ItemName itemName = wrapper.getItemName();
            if (itemName.equals(AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_ANALYSIS_ATTRIBUTE_SETTING)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_DETAILED_ANALYSIS)) {
                return ItemVisibility.HIDDEN;
            }

            if (itemName.equals(AbstractAnalysisSessionOptionType.F_IS_INDIRECT)) {
                LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
                RoleAnalysisOptionType option = objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
                RoleAnalysisProcessModeType processMode = option.getProcessMode();
                if (!processMode.equals(RoleAnalysisProcessModeType.USER)) {
                    return ItemVisibility.HIDDEN;
                } else {
                    return ItemVisibility.AUTO;

                }
            }

            return ItemVisibility.AUTO;
        };
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
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.filtering.option");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.filtering.option.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.filtering.option.subText");
    }

    @Override
    public String getStepId() {
        return WORK_PANEL_TYPE;
    }
}
