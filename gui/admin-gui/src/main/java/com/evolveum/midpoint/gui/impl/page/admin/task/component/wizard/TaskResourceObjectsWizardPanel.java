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
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReconciliationWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.model.IModel;

public class TaskResourceObjectsWizardPanel extends AbstractFormWizardStepPanel<TaskDetailsModel> {

    private static final String WORK_PANEL_TYPE = "tw-work";

    private final ItemName activityName;

    public TaskResourceObjectsWizardPanel(ItemName activityName, TaskDetailsModel model) {
        super(model);
        this.activityName = activityName;
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK,
                        activityName, ReconciliationWorkDefinitionType.F_RESOURCE_OBJECTS));
    }

    @Override
    protected String getPanelType() {
        return WORK_PANEL_TYPE;
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ResourceObjectSetType.F_QUERY)
                    || wrapper.getItemName().equals(ResourceObjectSetType.F_QUERY_APPLICATION)){
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageTask.wizard.step.work.resourceObjects");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageTask.wizard.step.work.resourceObjects.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageTask.wizard.step.work.resourceObjects.subText");
    }

    @Override
    public String getStepId() {
        return WORK_PANEL_TYPE;
    }
}
