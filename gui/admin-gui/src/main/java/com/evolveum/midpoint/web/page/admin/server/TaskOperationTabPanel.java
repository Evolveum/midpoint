/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.Collection;
import java.util.Collections;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusDto;
import com.evolveum.midpoint.web.component.model.operationStatus.ModelOperationStatusPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 */
public class TaskOperationTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_MODEL_OPERATION_STATUS_PANEL = "modelOperationStatusPanel";

	private static final Trace LOGGER = TraceManager.getTrace(TaskOperationTabPanel.class);

	private IModel<TaskDto> taskDtoModel = null;
	
	public TaskOperationTabPanel(String id, Form mainForm,
			LoadableModel<PrismObjectWrapper<TaskType>> taskWrapperModel,
			IModel<TaskDto> taskDtoModel, PageBase pageBase) {
		super(id, mainForm, taskWrapperModel);
		this.taskDtoModel = taskDtoModel;
		setOutputMarkupId(true);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
		
	}

	private void initLayout() {

		final PropertyModel<ModelOperationStatusDto> operationStatusModel = new PropertyModel<>(taskDtoModel, TaskDto.F_MODEL_OPERATION_STATUS);
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
