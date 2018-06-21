/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.certification.api.AccessCertificationApiConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certification/definition",
		action = {
				@AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
						label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
						description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
				@AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_DEFINITION,
						label = PageAdminCertification.AUTH_CERTIFICATION_DEFINITION_LABEL,
						description = PageAdminCertification.AUTH_CERTIFICATION_DEFINITION_DESCRIPTION)
				})
public class PageCertDefinition extends PageAdminCertification {

	private static final Trace LOGGER = TraceManager.getTrace(PageCertDefinition.class);

	private static final String DOT_CLASS = PageCertDefinition.class.getName() + ".";

	private static final String OPERATION_LOAD_DEFINITION = DOT_CLASS + "loadDefinition";

	private static final String ID_SUMMARY_PANEL = "summaryPanel";
	private static final String ID_MAIN_FORM = "mainForm";

	private static final String ID_BACK_BUTTON = "backButton";
	private static final String ID_SAVE_BUTTON = "saveButton";

	private static final String OPERATION_SAVE_DEFINITION = DOT_CLASS + "saveDefinition";
	private static final String ID_TAB_PANEL = "tabPanel";

	private LoadableModel<CertDefinitionDto> definitionModel;
	private String definitionOid;

	public PageCertDefinition(PageParameters parameters) {
		definitionOid = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
		initModels();
		initLayout();
	}

	//region Data
	private void initModels() {
		definitionModel = new LoadableModel<CertDefinitionDto>(false) {
			@Override
			protected CertDefinitionDto load() {
				if (definitionOid != null) {
					return loadDefinition(definitionOid);
				} else {
					try {
						return createDefinition();
					} catch (SchemaException e) {
						throw new SystemException(e.getMessage(), e);
					}
				}
			}
		};
	}

	private CertDefinitionDto loadDefinition(String definitionOid) {
		Task task = createSimpleTask(OPERATION_LOAD_DEFINITION);
		OperationResult result = task.getResult();
		CertDefinitionDto definitionDto = null;
		try {
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
			PrismObject<AccessCertificationDefinitionType> definitionObject =
					WebModelServiceUtils.loadObject(AccessCertificationDefinitionType.class, definitionOid, options,
							PageCertDefinition.this, task, result);
			AccessCertificationDefinitionType definition = PrismObjectValue.asObjectable(definitionObject);
			definitionDto = new CertDefinitionDto(definition, this, getPrismContext());
			result.recordSuccessIfUnknown();
		} catch (Exception ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get definition", ex);
			result.recordFatalError("Couldn't get definition.", ex);
		}
		result.recomputeStatus();

		if (!WebComponentUtil.isSuccessOrHandledError(result)) {
			showResult(result);
		}
		return definitionDto;
	}

	private CertDefinitionDto createDefinition() throws SchemaException {
		AccessCertificationDefinitionType definition = getPrismContext().createObjectable(AccessCertificationDefinitionType.class);
		definition.setHandlerUri(AccessCertificationApiConstants.DIRECT_ASSIGNMENT_HANDLER_URI);
		AccessCertificationStageDefinitionType stage = new AccessCertificationStageDefinitionType(getPrismContext());
		stage.setName("Stage 1");
		stage.setNumber(1);
		stage.setReviewerSpecification(new AccessCertificationReviewerSpecificationType(getPrismContext()));
		definition.getStageDefinition().add(stage);
		return new CertDefinitionDto(definition, this, getPrismContext());
	}
	//endregion

	//region Layout
	private void initLayout() {
		CertDefinitionSummaryPanel summaryPanel = new CertDefinitionSummaryPanel(ID_SUMMARY_PANEL,
				new PropertyModel<>(definitionModel, CertDefinitionDto.F_PRISM_OBJECT), this);
		add(summaryPanel);

		Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
		add(mainForm);

		initTabs(mainForm);
		initButtons(mainForm);
	}

	private void initTabs(Form mainForm) {

		List<ITab> tabs = new ArrayList<>();
		tabs.add(new AbstractTab(createStringResource("PageCertDefinition.basic")) {
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new DefinitionBasicPanel(panelId, definitionModel);
			}
		});
		tabs.add(new AbstractTab(createStringResource("PageCertDefinition.scopeDefinition")) {
            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new DefinitionScopePanel(panelId,
						new PropertyModel<>(definitionModel, CertDefinitionDto.F_SCOPE_DEFINITION));
            }
        });
		tabs.add(new CountablePanelTab(createStringResource("PageCertDefinition.stagesDefinition")) {
			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new DefinitionStagesPanel(panelId,
						new PropertyModel<>(definitionModel, CertDefinitionDto.F_STAGE_DEFINITION), PageCertDefinition.this);
			}
			@Override
			public String getCount() {
				return String.valueOf(definitionModel.getObject().getNumberOfStages());
			}
		});
		tabs.add(new AbstractTab(createStringResource("PageCertDefinition.xmlDefinition")) {
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new DefinitionXmlPanel(panelId, definitionModel);
			}
		});
		TabbedPanel tabPanel = WebComponentUtil.createTabPanel(ID_TAB_PANEL, this, tabs, null);
		mainForm.add(tabPanel);
	}

	public TabbedPanel getTabPanel() {
		return (TabbedPanel) get(createComponentPath(ID_MAIN_FORM, ID_TAB_PANEL));
	}

	private void initButtons(final Form mainForm) {
		AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON, createStringResource("PageCertDefinition.button.back")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				redirectBack();
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
//		if (StringUtils.isEmpty(dto.getXml())) {
//			error(getString("CertDefinitionPage.message.cantSaveEmpty"));
//			target.add(getFeedbackPanel());
//			return;
//		}

        if (StringUtils.isEmpty(dto.getName())) {
            error(getString("CertDefinitionPage.message.cantSaveEmptyName"));
            target.add(getFeedbackPanel());
            return;
        }

        Task task = createSimpleTask(OPERATION_SAVE_DEFINITION);
		OperationResult result = task.getResult();
		try {
			AccessCertificationDefinitionType oldObject = dto.getOldDefinition();
			oldObject.asPrismObject().revive(getPrismContext());

			AccessCertificationDefinitionType newObject = dto.getUpdatedDefinition(getPrismContext());
			newObject.asPrismObject().revive(getPrismContext());

			ObjectDelta<AccessCertificationDefinitionType> delta;
			if (oldObject.getOid() != null) {
				delta = DiffUtil.diff(oldObject, newObject);
			} else {
				delta = ObjectDelta.createAddDelta(newObject.asPrismObject());
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Access definition delta:\n{}", delta.debugDump());
			}
			delta.normalize();
			if (delta != null && !delta.isEmpty()) {
				getPrismContext().adopt(delta);
				ModelExecuteOptions options = new ModelExecuteOptions();
				options.setRaw(true);
				getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), options, task, result);
			}
			result.computeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't save object: " + ex.getMessage(), ex);
		}

		showResult(result);

		if (result.isError()) {
			target.add(getFeedbackPanel());
		} else {
			redirectBack();
		}
	}
	//endregion
}
