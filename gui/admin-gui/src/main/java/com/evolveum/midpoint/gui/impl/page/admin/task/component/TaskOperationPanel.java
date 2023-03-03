/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import java.util.Collection;
import java.util.Collections;

import org.apache.wicket.Component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.util.ModelContextUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusDto;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 */

//TODO implement correctly
@PanelType(name = "operation")
@PanelInstance(identifier = "operation",
        display = @PanelDisplay(label = "pageTaskEdit.operation", order = 60))  //not visible for all tasks, only for WF related tasks
public class TaskOperationPanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_MODEL_OPERATION_STATUS_PANEL = "modelOperationStatusPanel";
    private static final String DOT_CLASS = TaskOperationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SCENE_DTO = DOT_CLASS + "loadSceneDto";

    private static final Trace LOGGER = TraceManager.getTrace(TaskOperationPanel.class);

    public TaskOperationPanel(String id, TaskDetailsModel modelContextModel, ContainerPanelConfigurationType config) {
        super(id, modelContextModel, config);
        setOutputMarkupId(true);
    }

       private LensContextType getLensContextType() {
            LensContextType lensContextType = getObjectWrapper().getObject().asObjectable().getLensContext();
            if (lensContextType == null || lensContextType.getState() == null) {
                return null;
            }
            return lensContextType;

    }

    protected void initLayout() {
        final LensContextType lensContextType = getLensContextType();

        LoadableModel<ModelOperationStatusDto> operationStatusModel = null;
        if (lensContextType != null) {
            operationStatusModel = new LoadableModel<ModelOperationStatusDto>() {
                @Override
                protected ModelOperationStatusDto load() {
                    Task task = getPageBase().createSimpleTask(OPERATION_LOAD_SCENE_DTO);
                    OperationResult result = task.getResult();

                    ModelContext ctx;
                    try {
                        ctx = ModelContextUtil.unwrapModelContext(lensContextType, getPageBase().getModelInteractionService(), task, result);
                    } catch (ObjectNotFoundException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Unexpected error, cannot get real value for model context", e, e.getMessage());
                        return null;
                    }

                    return new ModelOperationStatusDto(ctx, getPageBase().getModelInteractionService(), task, result);
                }
            };
        }

        ModelOperationStatusPanel panel = new ModelOperationStatusPanel(ID_MODEL_OPERATION_STATUS_PANEL, operationStatusModel);
        panel.add(new VisibleBehaviour(() -> lensContextType != null));
        add(panel);
    }

    //TODO correc visibility
    @Override
    public boolean isVisible() {
        return getLensContextType() != null && getLensContextType().getState() != null;
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(this);
    }

}
