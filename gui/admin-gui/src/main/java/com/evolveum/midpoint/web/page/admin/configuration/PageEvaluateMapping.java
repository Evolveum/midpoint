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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyWrapperModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ExecuteMappingDto;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingEvaluationRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingEvaluationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/config/evaluateMapping", action = {
		@AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
				label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_EVALUATE_MAPPING_URL,
                label = "PageEvaluateMapping.auth.mapping.label", description = "PageEvaluateMapping.auth.mapping.description")
})
public class PageEvaluateMapping extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageEvaluateMapping.class);

    private static final String DOT_CLASS = PageEvaluateMapping.class.getName() + ".";

    private static final String OPERATION_EXECUTE_MAPPING = DOT_CLASS + "evaluateMapping";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_EXECUTE = "execute";
	private static final String ID_EDITOR_REQUEST = "editorRequest";
	private static final String ID_EDITOR_MAPPING = "editorMapping";
    private static final String ID_RESULT_TEXT = "resultText";
	private static final String ID_MAPPING_SAMPLE = "mappingSample";

	private static final String SAMPLES_DIR = "mapping-samples";
	private static final List<String> SAMPLES = Arrays.asList(
			"FullName_NoDelta",
			"FullName_Delta",
			"FullName_Delta_Ref",
			"FullName_Delta_Cond",
			"OrgName"
	);

	private final NonEmptyModel<ExecuteMappingDto> model = new NonEmptyWrapperModel<>(new Model<>(new ExecuteMappingDto()));

    public PageEvaluateMapping() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

		AceEditor editorMapping = new AceEditor(
				ID_EDITOR_MAPPING, new PropertyModel<String>(model, ExecuteMappingDto.F_MAPPING));
		editorMapping.setHeight(400);
		editorMapping.setResizeToMaxHeight(false);
        mainForm.add(editorMapping);

		AceEditor editorRequest = new AceEditor(
				ID_EDITOR_REQUEST, new PropertyModel<String>(model, ExecuteMappingDto.F_REQUEST));
		editorRequest.setHeight(430);
		editorRequest.setResizeToMaxHeight(false);
		mainForm.add(editorRequest);

		AjaxSubmitButton evaluateMapping = new AjaxSubmitButton(ID_EXECUTE, createStringResource("PageEvaluateMapping.button.evaluateMapping")) {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                executeMappingPerformed(target);
            }
        };
        mainForm.add(evaluateMapping);

		final DropDownChoice<String> sampleChoice = new DropDownChoice<>(ID_MAPPING_SAMPLE,
				Model.of(""),
				new AbstractReadOnlyModel<List<String>>() {
					@Override
					public List<String> getObject() {
						return SAMPLES;
					}
				},
				new StringResourceChoiceRenderer("PageEvaluateMapping.sample"));
		sampleChoice.setNullValid(true);
		sampleChoice.add(new OnChangeAjaxBehavior() {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				String sampleName = sampleChoice.getModelObject();
				if (StringUtils.isEmpty(sampleName)) {
					return;
				}
				model.getObject().setMapping(readResource(SAMPLES_DIR + "/" + sampleName + ".map.xml.data"));
				model.getObject().setRequest(readResource(SAMPLES_DIR + "/" + sampleName + ".req.xml.data"));
				model.getObject().setResultText("");
				target.add(PageEvaluateMapping.this);
			}

			private String readResource(String name) {
				InputStream is = PageEvaluateMapping.class.getResourceAsStream(name);
				if (is != null) {
					try {
						return IOUtils.toString(is, "UTF-8");
					} catch (IOException e) {
						LoggingUtils.logUnexpectedException(LOGGER, "Couldn't read sample from resource {}", e, name);
					} finally {
						IOUtils.closeQuietly(is);
					}
				} else {
					LOGGER.warn("Resource {} containing sample couldn't be found", name);
				}
				return null;
			}
		});
		mainForm.add(sampleChoice);

		AceEditor resultText = new AceEditor(ID_RESULT_TEXT, new PropertyModel<String>(model, ExecuteMappingDto.F_RESULT_TEXT));
		resultText.setReadonly(true);
		resultText.setHeight(300);
		resultText.setResizeToMaxHeight(false);
		resultText.setMode(null);
		mainForm.add(resultText);

	}

	private void executeMappingPerformed(AjaxRequestTarget target) {
		Task task = createSimpleTask(OPERATION_EXECUTE_MAPPING);
		OperationResult result = new OperationResult(OPERATION_EXECUTE_MAPPING);

		ExecuteMappingDto dto = model.getObject();
		if (StringUtils.isBlank(dto.getMapping())) {
			warn(getString("PageEvaluateMapping.message.emptyString"));
			target.add(getFeedbackPanel());
			return;
		}
        try {
			MappingEvaluationRequestType request;
			if (StringUtils.isNotBlank(dto.getRequest())) {
				request = getPrismContext().parserFor(dto.getRequest()).xml().parseRealValue(MappingEvaluationRequestType.class);
			} else {
				request = new MappingEvaluationRequestType();
			}

			if (StringUtils.isNotBlank(dto.getMapping())) {
				request.setMapping(getPrismContext().parserFor(dto.getMapping()).xml().parseRealValue(MappingType.class));
			}

			MappingEvaluationResponseType response = getModelDiagnosticService().evaluateMapping(request, task, result);
			dto.setResultText(response.getResponse());

        } catch (CommonException | RuntimeException e) {
            result.recordFatalError("Couldn't execute mapping", e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute mapping", e);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.close();
            dto.setResultText(sw.toString());
        } finally {
            result.computeStatus();
        }

        showResult(result);
        target.add(this);
    }


}
