/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.model.IModel;

public class TaskDistributionWizardPanel extends AbstractFormWizardStepPanel<TaskDetailsModel> {

    private static final String DISTRIBUTION_PANEL_TYPE = "tw-distribution";

    public TaskDistributionWizardPanel(TaskDetailsModel model) {
        super(model);
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(), ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_DISTRIBUTION));
    }

    @Override
    protected String getPanelType() {
        return DISTRIBUTION_PANEL_TYPE;
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return super.getVisibilityHandler();
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("TaskWizardPanel.wizard.step.distribution");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("TaskWizardPanel.wizard.step.distribution.help");
    }

    @Override
    public String getStepId() {
        return DISTRIBUTION_PANEL_TYPE;
    }

}
