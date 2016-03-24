/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
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
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.DefinitionScopeDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import javax.xml.namespace.QName;
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

	private static final String ID_MAIN_FORM = "mainForm";

	private static final String ID_NAME = "name";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_OWNER = "owner";
	private static final String ID_NUMBER_OF_STAGES = "numberOfStages";
	private static final String ID_REVIEW_STAGE_CAMPAIGNS = "campaignsInReviewStage";
	private static final String ID_CAMPAIGNS_TOTAL = "campaignsTotal";
	private static final String ID_LAST_STARTED = "campaignLastStarted";
	private static final String ID_LAST_CLOSED = "campaignLastClosed";
//	private static final String ID_OWNER_VALUE_CONTAINER = "ownerValueContainer";
//	private static final String ID_OWNER_INPUT = "ownerInput";
	private static final String ID_OWNER_REF_CHOOSER = "ownerRefChooser";
	private static final String ID_REMEDIATION = "remediation";
	private static final String ID_OUTCOME_STRATEGY = "outcomeStrategy";
	private static final String ID_STOP_REVIEW_ON = "stopReviewOn";

	private static final String ID_BACK_BUTTON = "backButton";
	private static final String ID_SAVE_BUTTON = "saveButton";

	private static final String OPERATION_SAVE_DEFINITION = DOT_CLASS + "saveDefinition";
	private static final String ID_TAB_PANEL = "tabPanel";

	private LoadableModel<CertDefinitionDto> definitionModel;

	CertDecisionHelper helper = new CertDecisionHelper();

	public PageCertDefinition(PageParameters parameters) {
		this(parameters, null);
	}

	public PageCertDefinition(PageParameters parameters, PageBase previousPage) {
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
				String definitionOid = getDefinitionOid();
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
		AccessCertificationDefinitionType definition = null;
		CertDefinitionDto definitionDto = null;
		try {
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
			PrismObject<AccessCertificationDefinitionType> definitionObject =
					WebModelServiceUtils.loadObject(AccessCertificationDefinitionType.class, definitionOid, options,
							PageCertDefinition.this, task, result);
			if (definitionObject != null) {
				definition = definitionObject.asObjectable();
			}
			definitionDto = new CertDefinitionDto(definition, this, getPrismContext());
			result.recordSuccessIfUnknown();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get definition", ex);
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
		CertDefinitionDto definitionDto = new CertDefinitionDto(definition, PageCertDefinition.this,
				getPrismContext());
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
		initTabs(mainForm);
		initButtons(mainForm);
	}

	private void initTabs(Form mainForm) {

		List<ITab> tabs = new ArrayList<>();
		tabs.add(new AbstractTab(createStringResource("PageCertDefinition.scopeDefinition")) {
            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new DefinitionScopePanel(panelId, new PropertyModel<DefinitionScopeDto>(definitionModel, CertDefinitionDto.F_SCOPE_DEFINITION));
            }
        });
		tabs.add(new AbstractTab(createStringResource("PageCertDefinition.stagesDefinition")) {
            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new DefinitionStagesPanel(panelId, new PropertyModel<List<StageDefinitionDto>>(definitionModel, CertDefinitionDto.F_STAGE_DEFINITION));
            }
        });

//		tabs.add(new AbstractTab(createStringResource("PageCertDefinition.campaigns")) {
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
//                // TODO campaigns panel (extract from PageCertCampaigns)
//                return new WebMarkupContainer(panelId);
//            }
//        });
		tabs.add(new AbstractTab(createStringResource("PageCertDefinition.xmlDefinition")) {
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new DefinitionXmlPanel(panelId, definitionModel);
			}
		});

		TabbedPanel tabPanel = new TabbedPanel(ID_TAB_PANEL, tabs) {
			@Override
			protected WebMarkupContainer newLink(String linkId, final int index) {
				return new AjaxSubmitLink(linkId) {

					@Override
					protected void onError(AjaxRequestTarget target,
										   org.apache.wicket.markup.html.form.Form<?> form) {
						super.onError(target, form);
						target.add(getFeedbackPanel());
					}

					@Override
					protected void onSubmit(AjaxRequestTarget target,
											org.apache.wicket.markup.html.form.Form<?> form) {
						super.onSubmit(target, form);

						setSelectedTab(index);
						if (target != null) {
							target.add(findParent(TabbedPanel.class));
						}
					}

				};
			}
		};
		tabPanel.setOutputMarkupId(true);

		mainForm.add(tabPanel);
	}

	private void initBasicInfoLayout(Form mainForm) {
        final TextField nameField = new TextField(ID_NAME, new PropertyModel<>(definitionModel, CertDefinitionDto.F_NAME));
        nameField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        mainForm.add(nameField);

		final TextArea descriptionField = new TextArea(ID_DESCRIPTION, new PropertyModel<>(definitionModel, CertDefinitionDto.F_DESCRIPTION));
        descriptionField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        mainForm.add(descriptionField);

        final WebMarkupContainer ownerRefChooser = createOwnerRefChooser(ID_OWNER_REF_CHOOSER);
        ownerRefChooser.setOutputMarkupId(true);
        mainForm.add(ownerRefChooser);

        mainForm.add(new Label(ID_NUMBER_OF_STAGES, new PropertyModel<>(definitionModel, CertDefinitionDto.F_NUMBER_OF_STAGES)));

        DropDownChoice remediation = new DropDownChoice<>(ID_REMEDIATION, new Model<AccessCertificationRemediationStyleType>() {

            @Override
            public AccessCertificationRemediationStyleType getObject() {
                return definitionModel.getObject().getRemediationStyle();
            }

            @Override
            public void setObject(AccessCertificationRemediationStyleType object) {
                definitionModel.getObject().setRemediationStyle(object);
            }
        }, WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationRemediationStyleType.class),
                new EnumChoiceRenderer<AccessCertificationRemediationStyleType>(this));
        mainForm.add(remediation);

		DropDownChoice outcomeStrategy =
				new DropDownChoice<>(ID_OUTCOME_STRATEGY,
						new PropertyModel<AccessCertificationCaseOutcomeStrategyType>(definitionModel, CertDefinitionDto.F_OUTCOME_STRATEGY),
						WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationCaseOutcomeStrategyType.class),
				new EnumChoiceRenderer<AccessCertificationCaseOutcomeStrategyType>(this));
		mainForm.add(outcomeStrategy);

		Label stopReviewOn = new Label(ID_STOP_REVIEW_ON, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				List<AccessCertificationResponseType> stopOn = definitionModel.getObject().getStopReviewOn();
				return CertMiscUtil.getStopReviewOnText(stopOn, PageCertDefinition.this);
			}
		});
		mainForm.add(stopReviewOn);

//        mainForm.add(new Label(ID_REVIEW_STAGE_CAMPAIGNS, new PropertyModel<>(definitionModel, CertDefinitionDto.F_NUMBER_OF_STAGES)));
//        mainForm.add(new Label(ID_CAMPAIGNS_TOTAL, new PropertyModel<>(definitionModel, CertDefinitionDto.F_NUMBER_OF_STAGES)));
        mainForm.add(new Label(ID_LAST_STARTED, new PropertyModel<>(definitionModel, CertDefinitionDto.F_LAST_STARTED)));
        mainForm.add(new Label(ID_LAST_CLOSED, new PropertyModel<>(definitionModel, CertDefinitionDto.F_LAST_CLOSED)));
	}

	private WebMarkupContainer createOwnerRefChooser(String id) {
		ChooseTypePanel tenantRef = new ChooseTypePanel(id,
				new PropertyModel<ObjectViewDto>(definitionModel, CertDefinitionDto.F_OWNER)) {

			@Override
			protected boolean isSearchEnabled() {
				return true;
			}

			@Override
			protected QName getSearchProperty() {
				return UserType.F_NAME;
			}
		};

		return tenantRef;
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

		if (result.isError()) {
			showResult(result);
			target.add(getFeedbackPanel());
		} else {
			showResult(result);
			setResponsePage(PageCertDefinitions.class);
		}
	}
	//endregion
}
