/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import java.util.Collection;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certification/definition",
		action = {
				@AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
						label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
						description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION) })
public class PageCertDefinition extends PageAdminCertification {

	private static final Trace LOGGER = TraceManager.getTrace(PageCertDefinition.class);

	private static final String DOT_CLASS = PageCertDefinition.class.getName() + ".";

	private static final String ID_MAIN_FORM = "mainForm";

	private static final String ID_NAME = "name";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_OWNER = "owner";
	private static final String ID_NUMBER_OF_STAGES = "numberOfStages";
	private static final String ID_ACE_EDITOR = "aceEditor";

	private static final String ID_BACK_BUTTON = "backButton";
	private static final String ID_SAVE_BUTTON = "saveButton";

	private static final String OPERATION_SAVE_DEFINITION = DOT_CLASS + "saveDefinition";

	private LoadableModel<CertDefinitionDto> definitionModel;

	CertDecisionHelper helper = new CertDecisionHelper();

	public PageCertDefinition(PageParameters parameters) {
		this(parameters, null);
	}

	public PageCertDefinition(PageParameters parameters, PageTemplate previousPage) {
		setPreviousPage(previousPage);
		getPageParameters().overwriteWith(parameters);
		initModels();
		initLayout();
	}

	//region Data
	private void initModels() {
		definitionModel = new LoadableModel<CertDefinitionDto>(false) {
			@Override
			protected CertDefinitionDto load() {
				return loadDefinition();
			}
		};
	}

	private CertDefinitionDto loadDefinition() {
		Task task = createSimpleTask("dummy");
		OperationResult result = task.getResult();
		AccessCertificationDefinitionType definition = null;
		CertDefinitionDto definitionDto = null;
		try {
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
			PrismObject<AccessCertificationDefinitionType> definitionObject =
					WebModelUtils.loadObject(AccessCertificationDefinitionType.class, getDefinitionOid(), options, 
							PageCertDefinition.this, task, result);
			if (definitionObject != null) {
				definition = definitionObject.asObjectable();
			}
			definitionDto = new CertDefinitionDto(definition, this, task, result);
			result.recordSuccessIfUnknown();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get definition", ex);
			result.recordFatalError("Couldn't get definition.", ex);
		}
		result.recomputeStatus();

		if (!WebMiscUtil.isSuccessOrHandledError(result)) {
			showResult(result);
		}
		return definitionDto;
	}

	private String getDefinitionOid() {
		StringValue campaignOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
		return campaignOid != null ? campaignOid.toString() : null;
	}
	//endregion

	//region Layout
	private void initLayout() {
		Form mainForm = new Form(ID_MAIN_FORM);
		add(mainForm);

		initBasicInfoLayout(mainForm);

		AceEditor editor = new AceEditor(ID_ACE_EDITOR, new PropertyModel<String>(definitionModel, CertDefinitionDto.F_XML));
		mainForm.add(editor);

		initButtons(mainForm);
	}

	private void initBasicInfoLayout(Form mainForm) {
		mainForm.add(new Label(ID_NAME, new PropertyModel<>(definitionModel, CertDefinitionDto.F_NAME)));
		mainForm.add(new Label(ID_DESCRIPTION, new PropertyModel<>(definitionModel, CertDefinitionDto.F_DESCRIPTION)));
		mainForm.add(new Label(ID_OWNER, new PropertyModel<>(definitionModel, CertDefinitionDto.F_OWNER_NAME)));
		mainForm.add(new Label(ID_NUMBER_OF_STAGES, new PropertyModel<>(definitionModel, CertDefinitionDto.F_NUMBER_OF_STAGES)));
	}

	private void initButtons(final Form mainForm) {
		AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON, createStringResource("PageCertDefinition.button.back")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				goBack(PageCertDefinitions.class);
			}
		};
		mainForm.add(backButton);

		AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_SAVE_BUTTON,
				createStringResource("PageCertDefinition.button.save")) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}
		};
		mainForm.add(saveButton);
	}
	//endregion

	//region Actions
	public void savePerformed(AjaxRequestTarget target) {
		CertDefinitionDto dto = definitionModel.getObject();
		if (StringUtils.isEmpty(dto.getXml())) {
			error(getString("CertDefinitionPage.message.cantSaveEmpty"));
			target.add(getFeedbackPanel());
			return;
		}

		Task task = createSimpleTask(OPERATION_SAVE_DEFINITION);
		OperationResult result = task.getResult();
		try {

			PrismObject<AccessCertificationDefinitionType> oldObject = dto.getDefinition().asPrismObject();
			oldObject.revive(getPrismContext());

			Holder<PrismObject<AccessCertificationDefinitionType>> objectHolder = new Holder<>(null);
			validateObject(definitionModel.getObject().getXml(), objectHolder, false, result);

			if (result.isAcceptable()) {
				PrismObject<AccessCertificationDefinitionType> newObject = objectHolder.getValue();

				// TODO implement better
				CertCampaignTypeUtil.checkStageDefinitionConsistency(newObject.asObjectable().getStageDefinition());

				ObjectDelta<AccessCertificationDefinitionType> delta = oldObject.diff(newObject, true, true);

				if (delta.getPrismContext() == null) {
					LOGGER.warn("No prism context in delta {} after diff, adding it", delta);
					delta.revive(getPrismContext());
				}

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Delta to be applied:\n{}", delta.debugDump());
				}

				Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(delta);
				ModelExecuteOptions options = new ModelExecuteOptions();
				options.setRaw(true);
				getModelService().executeChanges(deltas, options, task, result);
				result.computeStatus();
			}
		} catch (Exception ex) {
			result.recordFatalError("Couldn't save object: " + ex.getMessage(), ex);
		}

		if (result.isError()) {
			showResult(result);
			target.add(getFeedbackPanel());
		} else {
			showResultInSession(result);
			setResponsePage(PageCertDefinitions.class);
		}
	}
	//endregion
}
