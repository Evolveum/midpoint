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

import com.evolveum.midpoint.certification.api.CertificationManager;
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
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn.BUTTON_COLOR_CLASS;
import com.evolveum.midpoint.web.component.data.column.MultiButtonColumn;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseDtoProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns.OP_CLOSE_CAMPAIGN;
import static com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns.OP_CLOSE_STAGE;
import static com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns.OP_OPEN_NEXT_STAGE;
import static com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns.OP_START_CAMPAIGN;
import static com.evolveum.midpoint.web.page.admin.certification.PageCertCampaigns.OP_START_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_NOT_DECIDE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_REDUCE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_REDUCE_AND_REMEDIED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_REVOKE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_REVOKE_AND_REMEDIED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_WITHOUT_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REDUCE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certificationCampaign",
		action = {
				@AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
						label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
						description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION) })
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

	private static final String ID_DECISIONS_TABLE = "decisionsTable";

	private LoadableModel<AccessCertificationCasesStatisticsType> statModel;
	private LoadableModel<CertCampaignDto> campaignModel;

	CertDecisionHelper helper = new CertDecisionHelper();

	public PageCertCampaign(PageParameters parameters) {
		this(parameters, null);
	}

	public PageCertCampaign(PageParameters parameters, PageTemplate previousPage) {
		setPreviousPage(previousPage);
		getPageParameters().overwriteWith(parameters);
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
		campaignModel = new LoadableModel<CertCampaignDto>(false) {
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
			stat = getCertificationManager().getCampaignStatistics(getCampaignOid(), false, task, result);
			result.recordSuccessIfUnknown();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get campaign statistics", ex);
			result.recordFatalError("Couldn't get campaign statistics.", ex);
		}
		result.recomputeStatus();

		if (!WebMiscUtil.isSuccessOrHandledError(result)) {
			showResult(result);
		}
		return stat;
	}

	private CertCampaignDto loadCampaign() {
		OperationResult result = new OperationResult("dummy");  // todo
		AccessCertificationCampaignType campaign = null;
		try {
			Task task = createSimpleTask("dummy");  // todo
			PrismObject<AccessCertificationCampaignType> campaignObject =
					WebModelUtils.loadObject(AccessCertificationCampaignType.class, getCampaignOid(), result, PageCertCampaign.this);
			if (campaignObject != null) {
				campaign = campaignObject.asObjectable();
			}
			result.recordSuccessIfUnknown();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get campaign", ex);
			result.recordFatalError("Couldn't get campaign.", ex);
		}
		result.recomputeStatus();

		if (!WebMiscUtil.isSuccessOrHandledError(result)) {
			showResult(result);
		}
		return new CertCampaignDto(campaign, result, this);
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
				return formatDuration(dto.getStageStart(), dto.getStageEnd());
			}
		}));
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
		provider.setCampaignOid(getCampaignOid());
		TablePanel table = new TablePanel<>(ID_DECISIONS_TABLE, provider, initColumns());
		table.setShowPaging(true);
		table.setOutputMarkupId(true);
		mainForm.add(table);
	}

	private List<IColumn<CertCaseDto, String>> initColumns() {
		List<IColumn<CertCaseDto, String>> columns = new ArrayList<>();
		
		IColumn column;

		column = helper.createObjectNameColumn(this, "PageCertCampaign.table.objectName");
		columns.add(column);

		column = helper.createTargetTypeColumn(this);
		columns.add(column);

		column = helper.createTargetNameColumn(this, "PageCertCampaign.table.targetName");
		columns.add(column);

		column = helper.createDetailedInfoColumn(this);
		columns.add(column);

		column = new PropertyColumn(createStringResource("PageCertCampaign.table.reviewedAt"), CertCaseDto.F_REVIEWED_AT);
		columns.add(column);

		column = new PropertyColumn(createStringResource("PageCertCampaign.table.reviewedBy"), CertCaseDto.F_REVIEWED_BY);
		columns.add(column);

		column = new PropertyColumn(createStringResource("PageCertCampaign.table.reviewedInStage"), CertCaseDto.F_CURRENT_RESPONSE_STAGE_NUMBER);
		columns.add(column);

		column = new MultiButtonColumn<CertCaseDto>(new Model(), 6) {

			private final String[] captionKeys = {
					"PageCertCampaign.menu.accept",
					"PageCertCampaign.menu.revoke",
					"PageCertCampaign.menu.reduce",
					"PageCertCampaign.menu.notDecided",
					"PageCertCampaign.menu.delegate",
					"PageCertCampaign.menu.noResponse"
			};

			private final AccessCertificationResponseType[] responses = {
					ACCEPT, REVOKE, REDUCE, NOT_DECIDED, DELEGATE, NO_RESPONSE
			};

			@Override
			public String getCaption(int id) {
				return PageCertCampaign.this.createStringResource(captionKeys[id]).getString();
			}

			@Override
			public boolean isButtonEnabled(int id, IModel<CertCaseDto> model) {
				return false;
			}

			@Override
			public String getButtonColorCssClass(int id) {
				return getDecisionButtonColor(getRowModel(), responses[id]);
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
		return model.getObject().getCurrentResponse() == response;
	}

	private void initStatisticsLayout(Form mainForm) {
		mainForm.add(createStatLabel(ID_STAT_ACCEPT, F_MARKED_AS_ACCEPT));
		mainForm.add(createStatLabel(ID_STAT_REVOKE, F_MARKED_AS_REVOKE));
		mainForm.add(createStatLabel(ID_STAT_REVOKE_REMEDIED, F_MARKED_AS_REVOKE_AND_REMEDIED));
		mainForm.add(createStatLabel(ID_STAT_REDUCE, F_MARKED_AS_REDUCE));
		mainForm.add(createStatLabel(ID_STAT_REDUCE_REMEDIED, F_MARKED_AS_REDUCE_AND_REMEDIED));
		mainForm.add(createStatLabel(ID_STAT_DELEGATE, F_MARKED_AS_DELEGATE));
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
				goBack(PageCertCampaigns.class);
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
			CertificationManager cm = getCertificationManager();
			int currentStage = campaignModel.getObject().getCurrentStageNumber();
			Task task;
			switch (action) {
				case OP_START_CAMPAIGN:
				case OP_OPEN_NEXT_STAGE:
					task = createSimpleTask(OPERATION_OPEN_NEXT_STAGE);
					cm.openNextStage(getCampaignOid(), currentStage + 1, task, result);
					break;
				case OP_CLOSE_STAGE:
					task = createSimpleTask(OPERATION_CLOSE_STAGE);
					cm.closeCurrentStage(getCampaignOid(), currentStage, task, result);
					break;
				case OP_START_REMEDIATION:
					task = createSimpleTask(OPERATION_START_REMEDIATION);
					cm.startRemediation(getCampaignOid(), task, result);
					break;
				case OP_CLOSE_CAMPAIGN:
					task = createSimpleTask(OPERATION_CLOSE_CAMPAIGN);
					cm.closeCampaign(getCampaignOid(), task, result);
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
		target.add(getDecisionsTable());		// ???
		target.add(getFeedbackPanel());
	}


	private ObjectQuery createCaseQuery() {
		ObjectQuery query = new ObjectQuery();
		return query;
	}

	private TablePanel getDecisionsTable() {
		return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_DECISIONS_TABLE));
	}

	private String getCampaignOid() {
		StringValue campaignOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
		return campaignOid != null ? campaignOid.toString() : null;
	}

	public String getCampaignHandlerUri() {
		return campaignModel.getObject().getHandlerUri();
	}
}
