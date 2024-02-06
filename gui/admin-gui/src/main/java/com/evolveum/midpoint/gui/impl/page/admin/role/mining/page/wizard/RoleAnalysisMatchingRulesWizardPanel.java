/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public class RoleAnalysisMatchingRulesWizardPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String WORK_PANEL_TYPE = "rm-rules";

    public RoleAnalysisMatchingRulesWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);

    }

    @Override
    protected IModel<? extends PrismContainerWrapper<AbstractAnalysisSessionOptionType>> getContainerFormModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                ItemPath.create(RoleAnalysisSessionType.F_MATCHING_RULE));
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
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.detection.option");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.detection.option.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.detection.option.subText");
    }

    @Override
    public String getStepId() {
        return WORK_PANEL_TYPE;
    }
}
