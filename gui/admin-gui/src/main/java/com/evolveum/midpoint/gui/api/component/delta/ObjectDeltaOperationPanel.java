/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.api.component.delta;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

public class ObjectDeltaOperationPanel extends BasePanel<ObjectDeltaOperationType> {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaOperationPanel.class);

	private static final String ID_PARAMETERS_DELTA = "delta";
	private static final String ID_PARAMETERS_EXECUTION_RESULT = "executionResult";
	private static final String ID_PARAMETERS_OBJECT_NAME = "objectName";
	private static final String ID_PARAMETERS_RESOURCE_NAME = "resourceName";

	private static final String ID_DELTA_PANEL = "deltaPanel";
	private static final String ID_OBJECT_DELTA_OPERATION_MARKUP = "objectDeltaOperationMarkup";
	PageBase parentPage;

	public ObjectDeltaOperationPanel(String id, IModel<ObjectDeltaOperationType> model, PageBase parentPage) {
		super(id, model);
		this.parentPage = parentPage;
		initLayout();
	}

	private void initLayout() {
		// ObjectDeltaType od = getModel().getObjectDelta();
		WebMarkupContainer objectDeltaOperationMarkup = new WebMarkupContainer(ID_OBJECT_DELTA_OPERATION_MARKUP);
		objectDeltaOperationMarkup.setOutputMarkupId(true);

		objectDeltaOperationMarkup.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getBoxCssClass();
			}

		}));
		add(objectDeltaOperationMarkup);

		Label executionResult = new Label(ID_PARAMETERS_EXECUTION_RESULT,
				new PropertyModel(getModel(), "executionResult.status"));
		executionResult.setOutputMarkupId(true);
		objectDeltaOperationMarkup.add(executionResult);

		Label resourceName = new Label(ID_PARAMETERS_RESOURCE_NAME,
				new PropertyModel(getModel(), ObjectDeltaOperationType.F_RESOURCE_NAME.getLocalPart()));
		resourceName.setOutputMarkupId(true);
		objectDeltaOperationMarkup.add(resourceName);

		Label objectName = new Label(ID_PARAMETERS_OBJECT_NAME,
				new PropertyModel(getModel(), ObjectDeltaOperationType.F_OBJECT_NAME.getLocalPart()));
		objectName.setOutputMarkupId(true);
		objectDeltaOperationMarkup.add(objectName);
		final SceneDto sceneDto;
		try {
			sceneDto = loadSceneForDelta();
		} catch (SchemaException | ExpressionEvaluationException e) {
			OperationResult result = new OperationResult(ObjectDeltaOperationPanel.class.getName() + ".loadSceneForDelta");
			result.recordFatalError("Couldn't fetch or visualize the delta: " + e.getMessage(), e);
			parentPage.showResult(result);
			throw parentPage.redirectBackViaRestartResponseException();
		}
		IModel<SceneDto> deltaModel = new AbstractReadOnlyModel<SceneDto>() {
			private static final long serialVersionUID = 1L;

			@Override
            public SceneDto getObject() {
				return sceneDto;
			}

		};
		ScenePanel deltaPanel = new ScenePanel(ID_DELTA_PANEL, deltaModel) {
			@Override
			public void headerOnClickPerformed(AjaxRequestTarget target, IModel<SceneDto> model) {
				super.headerOnClickPerformed(target, model);
//				model.getObject().setMinimized(!model.getObject().isMinimized());
				target.add(ObjectDeltaOperationPanel.this);
			}
		};
		deltaPanel.setOutputMarkupId(true);
		objectDeltaOperationMarkup.add(deltaPanel);

	}

	private String getBoxCssClass() {
		if (getModel().getObject() == null) {
			return " box-primary";
		}

		if (getModel().getObject().getExecutionResult() == null) {
			return " box-primary";
		}

		if (getModel().getObject().getExecutionResult().getStatus() == null) {
			return " box-primary";
		}

		OperationResultStatusType status = getModel().getObject().getExecutionResult().getStatus();
		switch (status) {
			case PARTIAL_ERROR :
			case FATAL_ERROR : return " box-danger";
			case WARNING :
			case UNKNOWN :
			case HANDLED_ERROR : return " box-warning";
			case IN_PROGRESS : return " box-primary";
			case NOT_APPLICABLE : return " box-primary";
			case SUCCESS : return " box-success";

		}
		return " box-primary";

	}

	private SceneDto loadSceneForDelta() throws SchemaException, ExpressionEvaluationException {
		Scene scene;

		ObjectDelta<? extends ObjectType> delta;
		ObjectDeltaType deltaType = getModel().getObject().getObjectDelta();
		try {
			delta = DeltaConvertor.createObjectDelta(deltaType,
					parentPage.getPrismContext());
		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "SchemaException while converting delta:\n{}", e, deltaType);
			throw e;
		}
		try {
			scene = parentPage.getModelInteractionService().visualizeDelta(delta,
					parentPage.createSimpleTask(ID_PARAMETERS_DELTA),
					new OperationResult(ID_PARAMETERS_DELTA));
		} catch (SchemaException | ExpressionEvaluationException e) {
			LoggingUtils.logException(LOGGER, "SchemaException while visualizing delta:\n{}",
					e, DebugUtil.debugDump(delta));
			throw e;
		}
		SceneDto deltaSceneDto = new SceneDto(scene);
		deltaSceneDto.setMinimized(true);
		return deltaSceneDto;

	}

}
