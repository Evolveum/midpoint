/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDistributionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@PanelType(name = "distribution", defaultContainerPath = "activity/distribution", defaultType = ActivityDistributionDefinitionType.class)
@PanelInstance(identifier = "distribution", applicableForType = TaskType.class, childOf = TaskActivityPanel.class,
        display = @PanelDisplay(label = "ActivityDefinitionType.distribution", icon = GuiStyleConstants.CLASS_TASK_DISTRIBUTION_ICON, order = 30))
public class TaskDistributionPanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskDistributionPanel.class);
    private static final String ID_MAIN_PANEL = "main";
    private static final String ID_HANDLER = "handler";

    private static final String DOT_CLASS = TaskDistributionPanel.class.getName() + ".";
    private static final String OPERATION_UPDATE_WRAPPER = DOT_CLASS + "updateWrapper";

    public TaskDistributionPanel(String id, TaskDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel activityDefinitionPanel = new SingleContainerPanel(ID_MAIN_PANEL, getObjectWrapperModel(), getPanelConfiguration());
        add(activityDefinitionPanel);
    }
}
