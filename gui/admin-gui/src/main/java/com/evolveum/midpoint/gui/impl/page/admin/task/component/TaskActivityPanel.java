/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

@PanelType(name = "activity", defaultContainerPath = "activity")
@PanelInstance(identifier = "activity", applicableForType = TaskType.class,
        display = @PanelDisplay(label = "TaskType.activity", icon = GuiStyleConstants.CLASS_TASK_ACTIVITY_ICON, order = 15))
public class TaskActivityPanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskActivityPanel.class);
    private static final String ID_MAIN_PANEL = "main";
    private static final String ID_HANDLER = "handler";

    private static final String DOT_CLASS = TaskActivityPanel.class.getName() + ".";
    private static final String OPERATION_UPDATE_WRAPPER = DOT_CLASS + "updateWrapper";

    public TaskActivityPanel(String id, TaskDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel activityDefinitionPanel = new SingleContainerPanel(ID_MAIN_PANEL, getObjectWrapperModel(), getPanelConfiguration()) {

            @Override
            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                if (itemWrapper instanceof PrismContainerWrapper) {
                    return ItemVisibility.HIDDEN;
                }
                return super.getVisibility(itemWrapper);
            }
        };
        add(activityDefinitionPanel);

    }
}
