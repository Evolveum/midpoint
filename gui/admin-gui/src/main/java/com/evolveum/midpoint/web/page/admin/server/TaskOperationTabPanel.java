/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.Collection;
import java.util.Collections;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.util.ModelContextUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensContextType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusDto;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author semancik
 */
public class TaskOperationTabPanel extends BasePanel<PrismContainerWrapper<LensContextType>> implements TaskTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_MODEL_OPERATION_STATUS_PANEL = "modelOperationStatusPanel";
    private static final String DOT_CLASS = TaskOperationTabPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SCENE_DTO = DOT_CLASS + "loadSceneDto";

    private static final Trace LOGGER = TraceManager.getTrace(TaskOperationTabPanel.class);



    public TaskOperationTabPanel(String id, IModel<PrismContainerWrapper<LensContextType>> modelContextModel) {
        super(id, modelContextModel);
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();

    }

    private void initLayout() {

        final LoadableModel<ModelOperationStatusDto> operationStatusModel = new LoadableModel<ModelOperationStatusDto>() {
            @Override
            protected ModelOperationStatusDto load() {
                Task task = getPageBase().createSimpleTask(OPERATION_LOAD_SCENE_DTO);
                OperationResult result = task.getResult();

                ModelContext ctx;
                try {
                    LensContextType lensContextType = getModelObject().getValue().getRealValue();
                    ctx = ModelContextUtil.unwrapModelContext(lensContextType, getPageBase().getModelInteractionService(), task, result);
                } catch (SchemaException | ObjectNotFoundException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Unexpected error, cannot get real value for model context", e, e.getMessage());
                    return null;
                }

                return new ModelOperationStatusDto(ctx, getPageBase().getModelInteractionService(), task, result);
            }
        };
        VisibleEnableBehaviour modelOpBehaviour = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return operationStatusModel.getObject() != null;
            }
        };
        ModelOperationStatusPanel panel = new ModelOperationStatusPanel(ID_MODEL_OPERATION_STATUS_PANEL, operationStatusModel);
        panel.add(modelOpBehaviour);
        add(panel);
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.<Component>singleton(this);
    }

}
