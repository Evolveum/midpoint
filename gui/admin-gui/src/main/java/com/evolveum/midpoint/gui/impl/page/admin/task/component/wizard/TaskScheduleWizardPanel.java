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
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.model.IModel;

public class TaskScheduleWizardPanel extends AbstractFormWizardStepPanel<TaskDetailsModel> {

    private static final String SCHEDULE_PANEL_TYPE = "tw-schedule";

    public TaskScheduleWizardPanel(TaskDetailsModel model) {
        super(model);
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(), TaskType.F_SCHEDULE);
    }

    @Override
    protected String getPanelType() {
        return SCHEDULE_PANEL_TYPE;
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ScheduleType.F_EARLIEST_START_TIME)
                    || wrapper.getItemName().equals(ScheduleType.F_LATEST_START_TIME)
                    || wrapper.getItemName().equals(ScheduleType.F_MISFIRE_ACTION)
                    || wrapper.getItemName().equals(ScheduleType.F_RECURRENCE)){
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
        //cron-like pattern - better help, maybe some example how to configure it
    }

    @Override
    protected String getIcon() {
        return "fa fa-calendar";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("TaskWizardPanel.wizard.step.schedule");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("TaskWizardPanel.wizard.step.schedule.help");
    }

    @Override
    public String getStepId() {
        return SCHEDULE_PANEL_TYPE;
    }
}
