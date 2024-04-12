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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityExecutionModeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.model.IModel;

public class TaskExecutionWizardPanel extends AbstractFormWizardStepPanel<TaskDetailsModel> {

    private static final String SIMULATE_PANEL_TYPE = "tw-execution";

    public TaskExecutionWizardPanel(TaskDetailsModel model) {
        super(model);
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(), ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_EXECUTION));
    }

    @Override
    protected String getPanelType() {
        return SIMULATE_PANEL_TYPE;
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
        return createStringResource("PageTask.wizard.step.execution");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageTask.wizard.step.execution.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource(!isShadowSimulation() ? "PageTask.wizard.step.execution.subText" : "PageTask.wizard.step.execution.subText.shadow");
    }

    protected boolean isShadowSimulation() {
        return false;
    }

    @Override
    public String getStepId() {
        return SIMULATE_PANEL_TYPE;
    }

    @Override
    protected boolean isVisibleSubContainer(PrismContainerWrapper c) {
        if (c != null && c.getItemName().equivalent(ActivityExecutionModeDefinitionType.F_CONFIGURATION_TO_USE)) {
            return true;
        }
        return false;
    }
}
