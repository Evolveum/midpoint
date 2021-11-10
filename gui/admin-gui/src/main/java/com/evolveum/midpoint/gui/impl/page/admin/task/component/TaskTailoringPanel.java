/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitiesTailoringType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@PanelType(name = "tailoring", defaultContainerPath = "activity/tailoring", defaultType = ActivitiesTailoringType.class)
@PanelInstance(identifier = "tailoring", applicableForType = TaskType.class, childOf = TaskActivityPanel.class,
        display = @PanelDisplay(label = "ActivityDefinitionType.tailoring", order = 40))
public class TaskTailoringPanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> {

    private static final String ID_MAIN_PANEL = "main";

    public TaskTailoringPanel(String id, TaskDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel activityDefinitionPanel = new SingleContainerPanel(ID_MAIN_PANEL, getObjectWrapperModel(), getPanelConfiguration());
        add(activityDefinitionPanel);
    }
}
