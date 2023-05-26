/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.apache.wicket.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.model.IModel;

@PanelType(name = "taskBasic", defaultContainerPath = "empty")
@PanelInstance(identifier = "taskBasic", applicableForType = TaskType.class,
        display = @PanelDisplay(label = "pageAdminFocus.basic", order = 10))
public class TaskBasicPanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> implements RefreshableTabPanel {

    private static final String ID_MAIN_PANEL = "main";

    private static final String[] DEPRECATED_VIRTUAL_CONTAINERS = {
            "resource-objects",
            "reconciliation-options",
            "objects-to-recompute",
            "recompute-options",
            "objects-to-import",
            "import-options",
            "objects-to-synchronize",
            "synchronization-options",
            "async-options",
            "cleanup-options",
            "report-options",
            "objects-to-process",
            "bulk-action"
    };

    public TaskBasicPanel(String id, TaskDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        SingleContainerPanel mainPanel = new SingleContainerPanel(ID_MAIN_PANEL, getObjectWrapperModel(), getPanelConfiguration()) {

            @Override
            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                return getBasicTabVisibility(itemWrapper.getPath());
            }

            @Override
            protected ItemEditabilityHandler getEditabilityHandler() {
                return wrapper -> getBasicTabEditability(wrapper.getPath());
            }

            @Override
            protected IModel<PrismContainerWrapper> createVirtualContainerModel(VirtualContainersSpecificationType virtualContainer) {
                if (isDeprecatedVirtualContainer(virtualContainer)) {
                    return null;
                }
                return super.createVirtualContainerModel(virtualContainer);
            }
        };
        add(mainPanel);
    }

    private boolean isDeprecatedVirtualContainer(VirtualContainersSpecificationType virtualContainer) {
        String identifier = virtualContainer.getIdentifier();
        if (identifier == null) {
            return false;
        }

        if (Arrays.asList(DEPRECATED_VIRTUAL_CONTAINERS).contains(identifier)) {
            return true;
        }

        return false;
    }

    private ItemVisibility getBasicTabVisibility(ItemPath path) {
        if (TaskType.F_SUBTASK_REF.equivalent(path)) {
            return ItemVisibility.HIDDEN;
        }

        if (TaskType.F_SUBTYPE.equivalent(path)) {
            return ItemVisibility.HIDDEN;
        }

        if (TaskType.F_LIFECYCLE_STATE.equivalent(path)) {
            return ItemVisibility.HIDDEN;
        }

        if (TaskType.F_DIAGNOSTIC_INFORMATION.equivalent(path)) {
            return ItemVisibility.HIDDEN;
        }

        if (TaskType.F_RESULT.equivalent(path)) {
            return ItemVisibility.HIDDEN;
        }

        if (TaskType.F_EXTENSION.isSubPath(path)) {
            return ItemVisibility.HIDDEN;
        }

        return ItemVisibility.AUTO;
    }

    private boolean getBasicTabEditability(ItemPath path) {
        if (WebComponentUtil.isRunningTask(getTask())) {
            return false;
        }

        List<ItemPath> pathsToHide = Arrays.asList(TaskType.F_EXECUTION_STATE, TaskType.F_NODE, TaskType.F_NODE_AS_OBSERVED, TaskType.F_RESULT_STATUS,
                TaskType.F_RESULT, TaskType.F_NEXT_RUN_START_TIMESTAMP, TaskType.F_NEXT_RETRY_TIMESTAMP, TaskType.F_UNPAUSE_ACTION, TaskType.F_TASK_IDENTIFIER,
                TaskType.F_PARENT, TaskType.F_WAITING_REASON, TaskType.F_STATE_BEFORE_SUSPEND,
                TaskType.F_CHANNEL, TaskType.F_DEPENDENT_TASK_REF, TaskType.F_LAST_RUN_START_TIMESTAMP, TaskType.F_LAST_RUN_FINISH_TIMESTAMP, TaskType.F_COMPLETION_TIMESTAMP
        );

        for (ItemPath pathToHide : pathsToHide) {
            if (pathToHide.equivalent(path)) {
                return false;
            }
        }

        return true;

    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(this);
    }

    private TaskType getTask() {
        return getObjectDetailsModels().getObjectType();
    }

}
