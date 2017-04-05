/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SystemUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceVisualizationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.commons.configuration.Configuration;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/resources/visualization", action = {
		@AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL,
				label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL,
				description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_EDIT_URL,
				label = "PageResourceWizard.auth.resource.label",
				description = "PageResourceWizard.auth.resource.description")})
public class PageResourceVisualization extends PageAdmin {

	private static final Trace LOGGER = TraceManager.getTrace(PageResourceVisualization.class);

	private static final String DOT_CONFIGURATION = "midpoint.dot";
	private static final String RENDERER = "renderer";
	private static final String DEFAULT_RENDERER = "dot";

	private static final String ID_FORM = "form";
	private static final String ID_DOT_CONTAINER = "dotContainer";
	private static final String ID_DOT = "dot";
	private static final String ID_ERROR = "error";
	private static final String ID_SVG = "svg";
	private static final String ID_BACK = "back";

	private static final String OPERATION_EXPORT_DATA_MODEL = PageResourceVisualization.class.getName() + ".exportDataModel";

	@NotNull private final PrismObject<ResourceType> resourceObject;
	@NotNull private final NonEmptyLoadableModel<ResourceVisualizationDto> visualizationModel;

	public PageResourceVisualization(@NotNull PrismObject<ResourceType> resourceObject) {
		this.resourceObject = resourceObject;
		this.visualizationModel = new NonEmptyLoadableModel<ResourceVisualizationDto>(false) {
			@NotNull
			@Override
			protected ResourceVisualizationDto load() {
				return loadVisualizationDto();
			}
		};
		initLayout();
	}


	@Override
	protected IModel<String> createPageTitleModel() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return getString("PageResourceVisualization.title", resourceObject.getName());
			}
		};
	}

	@NotNull
	private ResourceVisualizationDto loadVisualizationDto() {

		Task task = createSimpleTask(OPERATION_EXPORT_DATA_MODEL);
		OperationResult result = task.getResult();
		String dot;
		try {
			resourceObject.revive(getPrismContext());
			dot = getModelDiagnosticService().exportDataModel(resourceObject.asObjectable(), task, result);
		} catch (CommonException|RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't export the data model for {}", e, ObjectTypeUtil.toShortString(resourceObject));
			showResult(result);
			throw redirectBackViaRestartResponseException();
		}

		String renderer = DEFAULT_RENDERER;
		Configuration dotConfig = getMidpointConfiguration().getConfiguration(DOT_CONFIGURATION);
		if (dotConfig != null) {
			renderer = dotConfig.getString(RENDERER, renderer);
		}

		renderer += " -Tsvg";
		StringBuilder output = new StringBuilder();
		try {
			SystemUtil.executeCommand(renderer, dot, output);
			return new ResourceVisualizationDto(dot, output.toString(), null);
		} catch (IOException|RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute SVG renderer command: {}", e, renderer);
			return new ResourceVisualizationDto(dot, null, e);
		}
	}

	private void initLayout() {
		Form form = new Form(ID_FORM);
		add(form);

		WebMarkupContainer dotContainer = new WebMarkupContainer(ID_DOT_CONTAINER);
		dotContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return visualizationModel.getObject().getSvg() == null;
			}
		});
		form.add(dotContainer);

		TextArea<String> dot = new TextArea<>(ID_DOT, new PropertyModel<String>(visualizationModel, ResourceVisualizationDto.F_DOT));
		dotContainer.add(dot);

		Label error = new Label(ID_ERROR, new PropertyModel<String>(visualizationModel, ResourceVisualizationDto.F_EXCEPTION_AS_STRING));
		dotContainer.add(error);

		Label svg = new Label(ID_SVG, new PropertyModel<String>(visualizationModel, ResourceVisualizationDto.F_SVG));
		svg.setEscapeModelStrings(false);
		svg.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return visualizationModel.getObject().getSvg() != null;
			}
		});
		form.add(svg);

		AjaxSubmitButton back = new AjaxSubmitButton(ID_BACK) {
			@Override
			public void onSubmit(AjaxRequestTarget ajaxRequestTarget, org.apache.wicket.markup.html.form.Form<?> form) {
				redirectBack();
			}

			@Override
			protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		form.add(back);
	}


}
