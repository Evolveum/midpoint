/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

public class TaskBasicWizardPanel extends AbstractFormWizardStepPanel<TaskDetailsModel> {

    private static final String BASIC_PANEL_TYPE = "tw-basic";

    public TaskBasicWizardPanel(TaskDetailsModel model) {
        super(model);
    }

    @Override
    protected String getPanelType() {
        return BASIC_PANEL_TYPE;
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(TaskType.F_NAME)
                    || wrapper.getItemName().equals(TaskType.F_DESCRIPTION)
                    || wrapper.getItemName().equals(TaskType.F_DOCUMENTATION)
                    || wrapper.getItemName().equals(TaskType.F_OWNER_REF)
                    || wrapper.getItemName().equals(TaskType.F_CATEGORY)){
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        };
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageTask.wizard.step.basic");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageTask.wizard.step.basic.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageTask.wizard.step.basic.subText");
    }

    @Override
    public String getStepId() {
        return BASIC_PANEL_TYPE;
    }

    @Override
    public boolean onBackPerformed(AjaxRequestTarget target) {
        getPageBase().redirectBack();
        return super.onBackPerformed(target);
    }
}
