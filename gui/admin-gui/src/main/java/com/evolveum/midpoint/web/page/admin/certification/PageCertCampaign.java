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
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn.BUTTON_COLOR_CLASS;
import com.evolveum.midpoint.web.component.data.column.MultiButtonColumn;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseDtoProvider;
import com.evolveum.midpoint.web.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.web.page.admin.certification.CertDecisionHelper.WhichObject.OBJECT;
import static com.evolveum.midpoint.web.page.admin.certification.CertDecisionHelper.WhichObject.TARGET;
import static com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.*;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certification/campaign",
		action = {
				@AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
						label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
						description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
				@AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGN,
						label = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGN_LABEL,
						description = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGN_DESCRIPTION)
		})
public class PageCertCampaign extends PageAdminCertification {

	private static final Trace LOGGER = TraceManager.getTrace(PageCertCampaign.class);

	private static final String DOT_CLASS = PageCertCampaign.class.getName() + ".";

	private static final String ID_MAIN_FORM = "mainForm";

	private static final String ID_STAT_ACCEPT = "statAccept";
	private static final String ID_STAT_REVOKE = "statRevoke";
	private static final String ID_STAT_REVOKE_REMEDIED = "statRevokeRemedied";
	private static final String ID_STAT_REDUCE = "statReduce";
	private static final String ID_STAT_REDUCE_REMEDIED = "statReduceRemedied";
	private static final String ID_STAT_DELEGATE = "statDelegate";
	private static final String ID_STAT_NO_DECISION = "statNoDecision";
	private static final String ID_STAT_NO_RESPONSE = "statNoResponse";

	private static final String ID_CAMPAIGN_NAME = "campaignName";
	private static final String ID_CAMPAIGN_DESCRIPTION = "campaignDescription";
	private static final String ID_CAMPAIGN_OWNER = "campaignOwner";
	private static final String ID_CAMPAIGN_NUMBER_OF_STAGES = "campaignNumberOfStages";
	private static final String ID_CAMPAIGN_CURRENT_STATE = "campaignCurrentState";
	private static final String ID_CAMPAIGN_TIME = "campaignTime";
	private static final String ID_STAGE_TIME = "stageTime";
	private static final String ID_ESCALATION_LEVEL_INFO_CONTAINER = "escalationLevelInfoContainer";
	private static final String ID_ESCALATION_LEVEL_INFO = "escalationLevelInfo";

	private static final String ID_BACK_BUTTON = "backButton";
	private static final String ID_START_CAMPAIGN_BUTTON = "startCampaignButton";
	private static final String ID_CLOSE_STAGE_BUTTON = "closeStageButton";
	private static final String ID_OPEN_NEXT_STAGE_BUTTON = "openNextStageButton";
	private static final String ID_START_REMEDIATION_BUTTON = "startRemediationButton";
	private static final String ID_CLOSE_CAMPAIGN_BUTTON = "closeCampaignButton";

	private static final String OPERATION_ADVANCE_LIFECYCLE = DOT_CLASS + "advanceLifecycle";
	private static final String OPERATION_OPEN_NEXT_STAGE = DOT_CLASS + "openNextStage";
	private static final String OPERATION_CLOSE_STAGE = DOT_CLASS + "closeStage";
	private static final String OPERATION_CLOSE_CAMPAIGN = DOT_CLASS + "closeCampaign";
	private static final String OPERATION_START_REMEDIATION = DOT_CLASS + "startRemediation";

	private static final String ID_OUTCOMES_TABLE = "outcomesTable";

	private LoadableModel<AccessCertificationCasesStatisticsType> statModel;
	private NonEmptyLoadableModel<CertCampaignDto> campaignModel;

	private String campaignOid;

	CertDecisionHelper helper = new CertDecisionHelper();

	public PageCertCampaign(PageParameters parameters) {
		StringValue campaignOidValue = parameters.get(OnePageParameterEncoder.PARAMETER);
		if (campaignOidValue != null) {
			campaignOid = campaignOidValue.toString();
		}
		initModels();
		initLayout();
	}

	private void initModels() {
		statModel = new LoadableModel<AccessCertificationCasesStatisticsType>(false) {
			@Override
			protected AccessCertificationCasesStatisticsType load() {
				return loadStatistics();
			}
		};
		campaignModel = new NonEmptyLoadableModel<CertCampaignDto>(false) {
			@NotNull
			@Override
			protected CertCampaignDto load() {
				return loadCampaign();
			}
		};

	}

	private AccessCertificationCasesStatisticsType loadStatistics() {
		OperationResult result = new OperationResult("dummy");  // todo
		AccessCertificationCasesStatisticsType stat = null;
		try {
			Task task = createSimpleTask("dummy");  // todo
			stat = getCertificationService().getCampaignStatistics(campaignOid, false, task, result);
			result.recordSuccessIfUnknown();
		} catch (Exception ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get campaign statistics", ex);
			result.recordFatalError("Couldn't get campaign statistics.", ex);
		}
		result.recomputeStatus();

		if (!WebComponentUtil.isSuccessOrHandledError(result)) {
			showResult(result);
		}
		return stat;
	}

	@NotNull
	private CertCampaignDto loadCampaign() {
		Task task = createSimpleTask("dummy");  // todo
		OperationResult result = task.getResult();
		AccessCertificationCampaignType campaign = null;
		try {
			PrismObject<AccessCertificationCampaignType> campaignObject =
					WebModelServiceUtils.loadObject(AccessCertificationCampaignType.class, campaignOid, PageCertCampaign.this, task, result);
			if (campaignObject != null) {
				campaign = campaignObject.asObjectable();
			}
			result.recordSuccessIfUnknown();
		} catch (Exception ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get campaign", ex);
			result.recordFatalError("Couldn't get campaign.", ex);
		}
		result.recomputeStatus();

		if (!WebComponentUtil.isSuccessOrHandledError(result)) {
			showResult(result);
		}
		if (campaign != null) {
			return new CertCampaignDto(campaign, this, task, result);
		} else {
			throw redirectBackViaRestartResponseException();
		}
	}

	private void initLayout() {
		Form mainForm = new Form(ID_MAIN_FORM);
		add(mainForm);

		initBasicInfoLayout(mainForm);
		initStatisticsLayout(mainForm);
		initTableLayout(mainForm);
		initButtons(mainForm);
	}

	private void initBasicInfoLayout(Form mainForm) {
		mainForm.add(new Label(ID_CAMPAIGN_NAME, new PropertyModel<>(campaignModel, CertCampaignDto.F_NAME)));
		mainForm.add(new Label(ID_CAMPAIGN_DESCRIPTION, new PropertyModel<>(campaignModel, CertCampaignDto.F_DESCRIPTION)));
		mainForm.add(new Label(ID_CAMPAIGN_OWNER, new PropertyModel<>(campaignModel, CertCampaignDto.F_OWNER_NAME)));
		mainForm.add(new Label(ID_CAMPAIGN_NUMBER_OF_STAGES, new PropertyModel<>(campaignModel, CertCampaignDto.F_NUMBER_OF_STAGES)));
		mainForm.add(new Label(ID_CAMPAIGN_CURRENT_STATE, new PropertyModel<>(campaignModel, CertCampaignDto.F_CURRENT_STATE)));
		mainForm.add(new Label(ID_CAMPAIGN_TIME, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				CertCampaignDto dto = campaignModel.getObject();
				return formatDuration(dto.getCampaignStart(), dto.getCampaignEnd());
			}
		}));
		mainForm.add(new Label(ID_STAGE_TIME, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				CertCampaignDto dto = campaignModel.getObject();
				return formatStageDuration(dto.getStageStart(), dto.getStageDeadline(), dto.getStageEnd());
			}
		}));
		WebMarkupContainer escalationLevelInfoContainer = new WebMarkupContainer(ID_ESCALATION_LEVEL_INFO_CONTAINER);
		mainForm.add(escalationLevelInfoContainer);
		escalationLevelInfoContainer.add(new Label(ID_ESCALATION_LEVEL_INFO, new PropertyModel<String>(campaignModel, CertCampaignDto.F_ESCALATION_LEVEL_INFO)));
		escalationLevelInfoContainer.add(new VisibleBehaviour(() -> campaignModel.getObject().getEscalationLevelInfo() != null));
	}

	// TODO implement seriously
	private String formatStageDuration(String start, String deadline, String end) {
		final String showAsEnd = end != null ? end : deadline;
		return formatDuration(start, showAsEnd);
	}

	// TODO implement seriously
	private String formatDuration(String start, String end) {
		if (start != null && end != null) {
			return start + " - " + end;
		} else if (start == null && end != null) {
			return "? - " + end;		// should not occur
		} else if (start != null && end == null) {
			return start + " -";
		} else {
			return null;
		}
	}

	private void initTableLayout(Form mainForm) {
		CertCaseDtoProvider provider = new CertCaseDtoProvider(PageCertCampaign.this);
		provider.setQuery(createCaseQuery());
		provider.setCampaignOid(campaignOid);
		provider.setSort(AccessCertificationCaseType.F_OBJECT_REF.getLocalPart(), SortOrder.ASCENDING);        // default sorting
		int itemsPerPage = (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_CERT_CAMPAIGN_OUTCOMES_PANEL);
		BoxedTablePanel table = new BoxedTablePanel<>(ID_OUTCOMES_TABLE, provider, initColumns(),
				UserProfileStorage.TableId.PAGE_CERT_CAMPAIGN_OUTCOMES_PANEL, itemsPerPage);
		table.setShowPaging(true);
		table.setOutputMarkupId(true);
		table.setItemsPerPage(itemsPerPage);
		mainForm.add(table);
	}

	private List<IColumn<CertCaseDto, String>> initColumns() {
		List<IColumn<CertCaseDto, String>> columns = new ArrayList<>();
		
		IColumn column;

		column = helper.createTypeColumn(OBJECT, this);
		columns.add(column);

		column = helper.createObjectNameColumn(this, "PageCertCampaign.table.objectName");
		columns.add(column);

		column = helper.createTypeColumn(TARGET, this);
		columns.add(column);

		column = helper.createTargetNameColumn(this, "PageCertCampaign.table.targetName");
		columns.add(column);

		column = helper.createDetailedInfoColumn(this);
		columns.add(column);

		if (AccessCertificationApiConstants.EXCLUSION_HANDLER_URI.equals(campaignModel.getObject().getHandlerUri())) {
			column = helper.createConflictingNameColumn(this, "PageCertCampaign.table.conflictingTargetName");
			columns.add(column);
		}

		column = new PropertyColumn(createStringResource("PageCertCampaign.table.reviewers"), CertCaseDto.F_CURRENT_REVIEWERS);
		columns.add(column);

		column = new PropertyColumn(createStringResource("PageCertCampaign.table.reviewedAt"), CertCaseDto.F_REVIEWED_AT);
		columns.add(column);

		column = new PropertyColumn(createStringResource("PageCertCampaign.table.reviewedBy"), CertCaseDto.F_REVIEWED_BY);
		columns.add(column);

		column = new PropertyColumn(createStringResource("PageCertCampaign.table.reviewedInStage"), CertCaseDto.F_CURRENT_RESPONSE_STAGE_NUMBER);
		columns.add(column);

		final AvailableResponses availableResponses = new AvailableResponses(getPage());
		final int responses = availableResponses.getCount();

		column = new MultiButtonColumn<CertCaseDto>(new Model(), responses+1) {

			@Override
			public String getCaption(int id) {
				return availableResponses.getCaption(id);
			}

			@Override
			public boolean isButtonEnabled(int id, IModel<CertCaseDto> model) {
				return false;
			}

			@Override
			public boolean isButtonVisible(int id, IModel<CertCaseDto> model) {
				if (id < responses) {
					return true;
				} else {
					return !availableResponses.isAvailable(model.getObject().getOverallOutcome());
				}
			}

			@Override
			public String getButtonColorCssClass(int id) {
				if (id < responses) {
					return getDecisionButtonColor(getRowModel(), availableResponses.getResponseValues().get(id));
				} else {
					return BUTTON_COLOR_CLASS.DANGER.toString();
				}
			}
		};
		columns.add(column);

		column = new PropertyColumn(createStringResource("PageCertCampaign.table.comments"), CertCaseDto.F_COMMENTS);
		columns.add(column);

		column = new PropertyColumn(createStringResource("PageCertCampaign.table.remediedAt"), CertCaseDto.F_REMEDIED_AT);
		columns.add(column);

		return columns;
	}

	private String getDecisionButtonColor(IModel<CertCaseDto> model, AccessCertificationResponseType response) {
		if (decisionEquals(model, response)) {
			return BUTTON_COLOR_CLASS.PRIMARY.toString();
		} else {
			return BUTTON_COLOR_CLASS.DEFAULT.toString();
		}
	}

	private boolean decisionEquals(IModel<CertCaseDto> model, AccessCertificationResponseType response) {
		return model.getObject().getOverallOutcome() == response;
	}

	private void initStatisticsLayout(Form mainForm) {
		mainForm.add(createStatLabel(ID_STAT_ACCEPT, F_MARKED_AS_ACCEPT));
		mainForm.add(createStatLabel(ID_STAT_REVOKE, F_MARKED_AS_REVOKE));
		mainForm.add(createStatLabel(ID_STAT_REVOKE_REMEDIED, F_MARKED_AS_REVOKE_AND_REMEDIED));
		mainForm.add(createStatLabel(ID_STAT_REDUCE, F_MARKED_AS_REDUCE));
		mainForm.add(createStatLabel(ID_STAT_REDUCE_REMEDIED, F_MARKED_AS_REDUCE_AND_REMEDIED));
		mainForm.add(createStatLabel(ID_STAT_NO_DECISION, F_MARKED_AS_NOT_DECIDE));
		mainForm.add(createStatLabel(ID_STAT_NO_RESPONSE, F_WITHOUT_RESPONSE));
	}

	private Label createStatLabel(String id, QName property) {
		return new Label(id, new PropertyModel<Integer>(statModel, property.getLocalPart()));
	}

	private void initButtons(final Form mainForm) {
		AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON, createStringResource("PageCertCampaign.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				redirectBack();
			}
		};
		mainForm.add(backButton);

		AjaxSubmitButton startCampaignButton = new AjaxSubmitButton(ID_START_CAMPAIGN_BUTTON,
				createStringResource("PageCertCampaign.button.startCampaign")) {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				executeCampaignStateOperation(target, OP_OPEN_NEXT_STAGE);
			}
		};
		startCampaignButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return campaignModel.getObject().getState() == AccessCertificationCampaignStateType.CREATED;
			}
		});
		mainForm.add(startCampaignButton);

		AjaxButton nextStageButton = new AjaxButton(ID_OPEN_NEXT_STAGE_BUTTON, createStringResource("PageCertCampaign.button.openNextStage")) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				executeCampaignStateOperation(target, OP_OPEN_NEXT_STAGE);
			}
		};
		nextStageButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return campaignModel.getObject().getState() == AccessCertificationCampaignStateType.REVIEW_STAGE_DONE
						&& campaignModel.getObject().getCurrentStageNumber() < campaignModel.getObject().getNumberOfStages();
			}
		});
		mainForm.add(nextStageButton);

		AjaxButton closeStageButton = new AjaxButton(ID_CLOSE_STAGE_BUTTON,
				createStringResource("PageCertCampaign.button.closeStage")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				executeCampaignStateOperation(target, OP_CLOSE_STAGE);
			}
		};
		closeStageButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return campaignModel.getObject().getState() == AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
			}
		});
		mainForm.add(closeStageButton);

		AjaxButton startRemediationButton = new AjaxButton(ID_START_REMEDIATION_BUTTON,
				createStringResource("PageCertCampaign.button.startRemediation")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				executeCampaignStateOperation(target, OP_START_REMEDIATION);
			}
		};
		startRemediationButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return campaignModel.getObject().getState() == AccessCertificationCampaignStateType.REVIEW_STAGE_DONE
						&& campaignModel.getObject().getCurrentStageNumber() == campaignModel.getObject().getNumberOfStages();
			}
		});
		mainForm.add(startRemediationButton);

		// TODO reenable when confirmation window is implemented
//		AjaxButton closeCampaignButton = new AjaxButton(ID_CLOSE_CAMPAIGN_BUTTON,
//				createStringResource("PageCertCampaign.button.closeCampaign")) {
//
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				executeCampaignStateOperation(target, OP_CLOSE_CAMPAIGN);
//			}
//		};
//		closeCampaignButton.add(new VisibleEnableBehaviour() {
//			@Override
//			public boolean isVisible() {
//				return campaignModel.getObject().getState() != AccessCertificationCampaignStateType.CLOSED;
//			}
//		});
//		mainForm.add(closeCampaignButton);
	}

	private void executeCampaignStateOperation(AjaxRequestTarget target, String action) {
		OperationResult result = new OperationResult(OPERATION_ADVANCE_LIFECYCLE);
		try {
			AccessCertificationService acs = getCertificationService();
			int currentStage = campaignModel.getObject().getCurrentStageNumber();
			Task task;
			switch (action) {
				case OP_START_CAMPAIGN:
				case OP_OPEN_NEXT_STAGE:
					task = createSimpleTask(OPERATION_OPEN_NEXT_STAGE);
					acs.openNextStage(campaignOid, currentStage + 1, task, result);
					break;
				case OP_CLOSE_STAGE:
					task = createSimpleTask(OPERATION_CLOSE_STAGE);
					acs.closeCurrentStage(campaignOid, currentStage, task, result);
					break;
				case OP_START_REMEDIATION:
					task = createSimpleTask(OPERATION_START_REMEDIATION);
					acs.startRemediation(campaignOid, task, result);
					break;
				case OP_CLOSE_CAMPAIGN:
					task = createSimpleTask(OPERATION_CLOSE_CAMPAIGN);
					acs.closeCampaign(campaignOid, task, result);
					break;
				default:
					throw new IllegalStateException("Unknown action: " + action);
			}
		} catch (Exception ex) {
			result.recordFatalError(ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		showResult(result);
		statModel.reset();
		campaignModel.reset();
		target.add(get(createComponentPath(ID_MAIN_FORM)));
		target.add((Component) getOutcomesTable());		// ???
		target.add(getFeedbackPanel());
	}


	private ObjectQuery createCaseQuery() {
		ObjectQuery query = new ObjectQuery();
		return query;
	}

	private Table getOutcomesTable() {
		return (Table) get(createComponentPath(ID_MAIN_FORM, ID_OUTCOMES_TABLE));
	}

	String getCampaignHandlerUri() {
		return campaignModel.getObject().getHandlerUri();
	}
}
