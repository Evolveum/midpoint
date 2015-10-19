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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.data.column.SingleButtonColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignListItemDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignListItemDtoProvider;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certification/campaigns", encoder = OnePageParameterEncoder.class, action = { @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL, label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL, description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION) })
public class PageCertCampaigns extends PageAdminCertification {

	private static final Trace LOGGER = TraceManager
			.getTrace(PageCertCampaigns.class);

	private static final String DOT_CLASS = "PageCertCampaigns" + ".";
	private static final String OPERATION_DELETE_CAMPAIGNS = DOT_CLASS
			+ "deleteCampaigns";
	private static final String OPERATION_ADVANCE_LIFECYCLE = DOT_CLASS
			+ "advanceLifecycle";
	private static final String OPERATION_OPEN_NEXT_STAGE = DOT_CLASS
			+ "openNextStage";
	private static final String OPERATION_CLOSE_STAGE = DOT_CLASS
			+ "closeStage";
	private static final String OPERATION_CLOSE_CAMPAIGN = DOT_CLASS
			+ "closeCampaign";
	private static final String OPERATION_START_CAMPAIGN = DOT_CLASS
			+ "startCampaign";
	private static final String OPERATION_START_REMEDIATION = DOT_CLASS
			+ "startRemediation";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_CAMPAIGNS_TABLE = "campaignsTable";
	public static final String OP_START_CAMPAIGN = "PageCertCampaigns.button.startCampaign";
	public static final String OP_CLOSE_CAMPAIGN = "PageCertCampaigns.button.closeCampaign";
	public static final String OP_CLOSE_STAGE = "PageCertCampaigns.button.closeStage";
	public static final String OP_OPEN_NEXT_STAGE = "PageCertCampaigns.button.openNextStage";
	public static final String OP_START_REMEDIATION = "PageCertCampaigns.button.startRemediation";
	private static final String DIALOG_CONFIRM_CLOSE_STAGE = "confirmCloseStagePopup";
    private static final String DIALOG_CONFIRM_DELETE_CAMPAIGN ="confirmDeleteCampaignPopup";
	private static final String DIALOG_CONFIRM_DELETE_MULTIPLE_CAMPAIGNS = "confirmDeleteMultipleCampaignsPopup";
    private static final String DIALOG_CONFIRM_CLOSE_CAMPAIGN ="confirmCloseCampaignPopup";
    private static final String DIALOG_CONFIRM_CLOSE_MULTIPLE_CAMPAIGNS ="confirmCloseMultipleCampaignsPopup";

	// campaign on which close-stage/close-campaign/delete has to be executed (if chosen directly from row menu)
	private CertCampaignListItemDto relevantCampaign;

	public PageCertCampaigns(PageParameters parameters) {
		getPageParameters().overwriteWith(parameters);
		initLayout();
	}

	// region Data management
	private CertCampaignListItemDtoProvider createProvider() {
		CertCampaignListItemDtoProvider provider = new CertCampaignListItemDtoProvider(
				this) {
			@Override
			public CertCampaignListItemDto createDataObjectWrapper(
					PrismObject<AccessCertificationCampaignType> obj) {
				CertCampaignListItemDto dto = super
						.createDataObjectWrapper(obj);
				createInlineMenuForItem(dto);
				return dto;
			}
		};
		provider.setQuery(createQuery());
		provider.setOptions(null);
		return provider;
	}

	private ObjectQuery createQuery() {
		// TODO filtering based on e.g. campaign state/stage (not started,
		// active, finished)
		ObjectQuery query = new ObjectQuery();
		String definitionOid = getDefinitionOid();
		if (definitionOid != null) {
			ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(
					definitionOid, ObjectTypes.ACCESS_CERTIFICATION_DEFINITION);
			ObjectFilter filter;
			try {
				filter = RefFilter.createReferenceEqual(new ItemPath(
						AccessCertificationCampaignType.F_DEFINITION_REF),
						AccessCertificationCampaignType.class,
						getPrismContext(), ref.asReferenceValue());
			} catch (SchemaException e) {
				throw new SystemException("Unexpected schema exception: "
						+ e.getMessage(), e);
			}
			query = ObjectQuery.createObjectQuery(filter);
		}
		return query;
	}

	private String getDefinitionOid() {
		StringValue definitionOid = getPageParameters().get(
				OnePageParameterEncoder.PARAMETER);
		return definitionOid != null ? definitionOid.toString() : null;
	}

	// endregion

	// region Layout

	@Override
	protected IModel<String> createPageSubTitleModel() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				String definitionOid = getDefinitionOid();
				if (definitionOid == null) {
					return null;
				}
				Task task = createSimpleTask("dummy");
				PrismObject<AccessCertificationDefinitionType> definitionPrismObject = WebModelUtils
						.loadObject(AccessCertificationDefinitionType.class,
								definitionOid, PageCertCampaigns.this, task, task.getResult());
				if (definitionPrismObject == null) {
					return null;
				}
				return definitionPrismObject.asObjectable().getName().getOrig();
			}
		};
	}

	private void initLayout() {
		Form mainForm = new Form(ID_MAIN_FORM);
		add(mainForm);

		add(new ConfirmationDialog(
				DIALOG_CONFIRM_CLOSE_STAGE,
				createStringResource("PageCertCampaigns.dialog.title.confirmCloseStage"),
				createCloseStageConfirmString()) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				closeStageConfirmedPerformed(target, relevantCampaign);
			}
		});

		add(new ConfirmationDialog(
				DIALOG_CONFIRM_CLOSE_CAMPAIGN,
				createStringResource("PageCertCampaigns.dialog.title.confirmCloseCampaign"),
				createCloseCampaignConfirmString()) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				closeCampaignConfirmedPerformed(target, relevantCampaign);
			}
		});
		
		add(new ConfirmationDialog(
				DIALOG_CONFIRM_CLOSE_MULTIPLE_CAMPAIGNS,
				createStringResource("PageCertCampaigns.dialog.title.confirmCloseCampaign"),
				createCloseSelectedCampaignsConfirmString()) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				closeSelectedCampaignsConfirmedPerformed(target);
			}
		});
			
		add(new ConfirmationDialog(
				DIALOG_CONFIRM_DELETE_CAMPAIGN,
				createStringResource("PageCertCampaigns.dialog.title.confirmDeleteCampaign"),
				createDeleteCampaignConfirmString()) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteCampaignConfirmedPerformed(target);
			}
		});

		add(new ConfirmationDialog(
				DIALOG_CONFIRM_DELETE_MULTIPLE_CAMPAIGNS,
				createStringResource("PageCertCampaigns.dialog.title.confirmDeleteCampaign"),
				createDeleteSelectedCampaignsConfirmString()) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteSelectedCampaignsConfirmedPerformed(target);
			}
		});

		CertCampaignListItemDtoProvider provider = createProvider();
		BoxedTablePanel<CertCampaignListItemDto> table = new BoxedTablePanel<>(
				ID_CAMPAIGNS_TABLE, provider, initColumns());
		table.setShowPaging(true);
		table.setOutputMarkupId(true);
		mainForm.add(table);
	}

	private IModel<String> createCloseStageConfirmString() {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {

				return createStringResource(
						"PageCertCampaigns.message.closeStageConfirmSingle",
						relevantCampaign.getName()).getString();
			}
		};
	}
	
	private IModel<String> createCloseCampaignConfirmString() {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {

				return createStringResource(
						"PageCertCampaigns.message.closeCampaignConfirmSingle",
						relevantCampaign.getName()).getString();
			}
		};
	}
	
	private IModel<String> createCloseSelectedCampaignsConfirmString() {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {

				final List<Selectable> selectedData = WebMiscUtil.getSelectedData(getCampaignsTable());
				if (selectedData.size() > 1) {
					return createStringResource(
							"PageCertCampaigns.message.closeCampaignConfirmMultiple", selectedData.size())
							.getString();
				} else if (selectedData.size() == 1) {
					return createStringResource(
							"PageCertCampaigns.message.closeCampaignConfirmSingle", ((CertCampaignListItemDto) selectedData.get(0)).getName())
							.getString();
				} else {
					return "EMPTY";
				}
			}
		};
	}

	private IModel<String> createDeleteCampaignConfirmString() {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return createStringResource(
						"PageCertCampaigns.message.deleteCampaignConfirmSingle",
						relevantCampaign.getName()).getString();
			}
		};
	}

	private IModel<String> createDeleteSelectedCampaignsConfirmString() {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				final List<Selectable> selectedData = WebMiscUtil.getSelectedData(getCampaignsTable());
				if (selectedData.size() > 1) {
					return createStringResource(
							"PageCertCampaigns.message.deleteCampaignConfirmMultiple", selectedData.size())
							.getString();
				} else if (selectedData.size() == 1) {
					return createStringResource(
							"PageCertCampaigns.message.deleteCampaignConfirmSingle", ((CertCampaignListItemDto) selectedData.get(0)).getName())
									.getString();
				} else {
					return "EMPTY";
				}
			}
		};
	}

	private Table getTable() {
		return (Table) get(createComponentPath(ID_MAIN_FORM,
				ID_CAMPAIGNS_TABLE));
	}

	private List<IColumn<CertCampaignListItemDto, String>> initColumns() {
		List<IColumn<CertCampaignListItemDto, String>> columns = new ArrayList<>();

		IColumn column;

		column = new CheckBoxHeaderColumn<>();
		columns.add(column);

		column = new LinkColumn<CertCampaignListItemDto>(
				createStringResource("PageCertCampaigns.table.name"),
				AccessCertificationCampaignType.F_NAME.getLocalPart(),
				CertCampaignListItemDto.F_NAME) {
			@Override
			public void onClick(AjaxRequestTarget target,
					IModel<CertCampaignListItemDto> rowModel) {
				campaignDetailsPerformed(target, rowModel.getObject().getOid());
			}
		};
		columns.add(column);

		column = new PropertyColumn(
				createStringResource("PageCertCampaigns.table.description"),
				CertCampaignListItemDto.F_DESCRIPTION);
		columns.add(column);

		column = new EnumPropertyColumn(
				createStringResource("PageCertCampaigns.table.state"),
				CertCampaignListItemDto.F_STATE) {
			@Override
			protected String translate(Enum en) {
				return createStringResourceStatic(getPage(), en).getString();
			}
		};
		columns.add(column);

		column = new PropertyColumn(
				createStringResource("PageCertCampaigns.table.stage"),
				CertCampaignListItemDto.F_CURRENT_STAGE_NUMBER);
		columns.add(column);

		column = new PropertyColumn(
				createStringResource("PageCertCampaigns.table.stages"),
				CertCampaignListItemDto.F_NUMBER_OF_STAGES);
		columns.add(column);

		column = new PropertyColumn(
				createStringResource("PageCertCampaigns.table.deadline"),
				CertCampaignListItemDto.F_DEADLINE_AS_STRING);
		columns.add(column);

		column = new SingleButtonColumn<CertCampaignListItemDto>(new Model(),
				null) {

			@Override
			public boolean isButtonEnabled(IModel<CertCampaignListItemDto> model) {
				final AccessCertificationCampaignType campaign = model
						.getObject().getCampaign();
				String button = determineAction(campaign);
				return button != null;
			}

			@Override
			public boolean isButtonVisible(IModel<CertCampaignListItemDto> model) {
				final AccessCertificationCampaignType campaign = model
						.getObject().getCampaign();

				return campaign.getState() != AccessCertificationCampaignStateType.IN_REMEDIATION
						&& campaign.getState() != AccessCertificationCampaignStateType.CLOSED;
			}

			@Override
			public String getCaption() {
				AccessCertificationCampaignType campaign = getRowModel()
						.getObject().getCampaign();
				String button = determineAction(campaign);
				if (button != null) {
					return PageCertCampaigns.this.createStringResource(button)
							.getString();
				} else {
					return "";
				}
			}

			@Override
			public String getButtonCssColorClass() {
				return DoubleButtonColumn.BUTTON_COLOR_CLASS.PRIMARY.toString();
			}

			@Override
			public String getButtonCssSizeClass() {
				return DoubleButtonColumn.BUTTON_SIZE_CLASS.SMALL.toString();
			}

			@Override
			public void clickPerformed(AjaxRequestTarget target,
					IModel<CertCampaignListItemDto> model) {
				AccessCertificationCampaignType campaign = model.getObject().getCampaign();
				String action = determineAction(campaign);
				switch (action) {
					case OP_START_CAMPAIGN:
					case OP_OPEN_NEXT_STAGE:
						openNextStagePerformed(target, campaign);
						break;
					case OP_CLOSE_STAGE:
						closeStageConfirmation(target, model.getObject());
						break;
					case OP_START_REMEDIATION:
						startRemediationPerformed(target, campaign);
						break;
					case OP_CLOSE_CAMPAIGN: // not used
					default:
						throw new IllegalStateException("Unknown action: " + action);
				}
			}
		};
		columns.add(column);

		columns.add(new InlineMenuHeaderColumn(createInlineMenu()));

		return columns;
	}

	private List<InlineMenuItem> createInlineMenu() {
		List<InlineMenuItem> items = new ArrayList<>();
		items.add(new InlineMenuItem(
				createStringResource("PageCertCampaigns.menu.startSelected"),
				false, new HeaderMenuAction(this) {
					@Override
					public void onClick(AjaxRequestTarget target) {
						startSelectedCampaignsPerformed(target);
					}
				}));
		items.add(new InlineMenuItem(
				createStringResource("PageCertCampaigns.menu.closeSelected"),
				false, new HeaderMenuAction(this) {
					@Override
					public void onClick(AjaxRequestTarget target) {
						closeSelectedCampaignsConfirmation(target);
					}
				}));
		items.add(new InlineMenuItem(
				createStringResource("PageCertCampaigns.menu.deleteSelected"),
				false, new HeaderMenuAction(this) {
					@Override
					public void onClick(AjaxRequestTarget target) {
						deleteSelectedCampaignsConfirmation(target);
					}
				}));
		return items;
	}

	private void createInlineMenuForItem(final CertCampaignListItemDto dto) {

		dto.getMenuItems().clear();
		dto.getMenuItems().add(
				new InlineMenuItem(
						createStringResource("PageCertCampaigns.menu.close"),
						new ColumnMenuAction<CertCampaignListItemDto>() {
							@Override
							public void onClick(AjaxRequestTarget target) {								
								closeCampaignConfirmation(target, dto);
							}
						}) {
					@Override
					public IModel<Boolean> getEnabled() {
						return new AbstractReadOnlyModel<Boolean>() {
							@Override
							public Boolean getObject() {
								return dto.getState() != AccessCertificationCampaignStateType.CLOSED;
							}
						};
					}
				});
		dto.getMenuItems().add(
				new InlineMenuItem(
						createStringResource("PageCertCampaigns.menu.delete"),
						new ColumnMenuAction<CertCampaignListItemDto>() {

							@Override
							public void onClick(AjaxRequestTarget target) {
								deleteCampaignConfirmation(target, dto);
							}
						}));
	}

	private Table getCampaignsTable() {
		return (Table) get(createComponentPath(ID_MAIN_FORM,
				ID_CAMPAIGNS_TABLE));
	}

	// endregion

	// region Actions

	// first, actions requiring confirmations are listed here (state = before confirmation)
	// These actions are responsible for setting/unsetting relevantCampaign field.

	// multi-item versions

	private void closeSelectedCampaignsConfirmation(AjaxRequestTarget target) {
		this.relevantCampaign = null;
		if (!ensureSomethingIsSelected(target)) {
			return;
		}
		ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_CLOSE_MULTIPLE_CAMPAIGNS);
		dialog.show(target);
	}

	private void deleteSelectedCampaignsConfirmation(AjaxRequestTarget target) {
		this.relevantCampaign = null;
		if (!ensureSomethingIsSelected(target)) {
			return;
		}
		ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_DELETE_MULTIPLE_CAMPAIGNS);
		dialog.show(target);
	}

	private boolean ensureSomethingIsSelected(AjaxRequestTarget target) {
		if (relevantCampaign != null) {
			return true;
		} else if (!WebMiscUtil.getSelectedData(getTable()).isEmpty()) {
			return true;
		} else {
			warn(getString("PageCertCampaigns.message.noCampaignsSelected"));
			target.add(getFeedbackPanel());
			return false;
		}
	}

	// single-item versions

	private void closeStageConfirmation(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
		this.relevantCampaign = campaignDto;
		ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_CLOSE_STAGE);
		dialog.show(target);
	}

	private void closeCampaignConfirmation(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
		this.relevantCampaign = campaignDto;
		ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_CLOSE_CAMPAIGN);
		dialog.show(target);
	}

	private void deleteCampaignConfirmation(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
		this.relevantCampaign = campaignDto;
		ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_DELETE_CAMPAIGN);
		dialog.show(target);
	}

	// actions after confirmation (single and multiple versions mixed)

	private void deleteCampaignConfirmedPerformed(AjaxRequestTarget target) {
		deleteCampaignsPerformed(target, Arrays.asList(relevantCampaign));
	}

	private void deleteSelectedCampaignsConfirmedPerformed(AjaxRequestTarget target) {
		deleteCampaignsPerformed(target, (List) WebMiscUtil.getSelectedData(getCampaignsTable()));
	}

	private void closeSelectedCampaignsConfirmedPerformed(AjaxRequestTarget target) {
		actOnCampaignsPerformed(target, OPERATION_CLOSE_CAMPAIGN,
				(List) WebMiscUtil.getSelectedData(getCampaignsTable()));
	}

	private void startSelectedCampaignsPerformed(AjaxRequestTarget target) {
		actOnCampaignsPerformed(target, OPERATION_START_CAMPAIGN,
				(List) WebMiscUtil.getSelectedData(getCampaignsTable()));
	}

	protected String determineAction(AccessCertificationCampaignType campaign) {
		int currentStage = campaign.getCurrentStageNumber();
		int numOfStages = CertCampaignTypeUtil.getNumberOfStages(campaign);
		AccessCertificationCampaignStateType state = campaign.getState();
		String button;
		switch (state) {
		case CREATED:
			button = numOfStages > 0 ? OP_START_CAMPAIGN : null;
			break;
		case IN_REVIEW_STAGE:
			button = OP_CLOSE_STAGE;
			break;
		case REVIEW_STAGE_DONE:
			button = currentStage < numOfStages ? OP_OPEN_NEXT_STAGE
					: OP_START_REMEDIATION;
			break;
		case IN_REMEDIATION:
		case CLOSED:
		default:
			button = null;
			break;
		}
		return button;
	}

	private void startRemediationPerformed(AjaxRequestTarget target, AccessCertificationCampaignType campaign) {
		LOGGER.debug("Start remediation performed for {}", campaign.asPrismObject());
		OperationResult result = new OperationResult(OPERATION_START_REMEDIATION);
		CertificationManager cm = getCertificationManager();
		try {
			Task task = createSimpleTask(OPERATION_START_REMEDIATION);
			cm.startRemediation(campaign.getOid(), task, result);
		} catch (Exception ex) {
			result.recordFatalError(ex);
		} finally {
			result.computeStatusIfUnknown();
		}
		showResult(result);
		target.add((Component) getCampaignsTable());
		target.add(getFeedbackPanel());
	}

	private void openNextStagePerformed(AjaxRequestTarget target, AccessCertificationCampaignType campaign) {
		LOGGER.debug("Start campaign / open next stage performed for {}", campaign.asPrismObject());
		OperationResult result = new OperationResult(OPERATION_OPEN_NEXT_STAGE);
		CertificationManager cm = getCertificationManager();
		try {
			Task task = createSimpleTask(OPERATION_OPEN_NEXT_STAGE);
			int currentStage = campaign.getCurrentStageNumber();
			cm.openNextStage(campaign.getOid(), currentStage + 1, task, result);
		} catch (Exception ex) {
			result.recordFatalError(ex);
		} finally {
			result.computeStatusIfUnknown();
		}
		showResult(result);
		target.add((Component) getCampaignsTable());
		target.add(getFeedbackPanel());
	}

	private void closeCampaignConfirmedPerformed(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
		AccessCertificationCampaignType campaign = campaignDto.getCampaign();
		LOGGER.debug("Close certification campaign performed for {}", campaign.asPrismObject());

		OperationResult result = new OperationResult(OPERATION_CLOSE_CAMPAIGN);
		try {
			CertificationManager cm = getCertificationManager();
			Task task = createSimpleTask(OPERATION_CLOSE_CAMPAIGN);
			cm.closeCampaign(campaign.getOid(), task, result);
		} catch (Exception ex) {
			result.recordFatalError(ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		showResult(result);
		target.add((Component) getCampaignsTable());
		target.add(getFeedbackPanel());
	}
	
	private void closeStageConfirmedPerformed(AjaxRequestTarget target, CertCampaignListItemDto campaignDto) {
		AccessCertificationCampaignType campaign = campaignDto.getCampaign();
		LOGGER.debug("Close certification stage performed for {}", campaign.asPrismObject());

		OperationResult result = new OperationResult(OPERATION_CLOSE_STAGE);
		try {
			CertificationManager cm = getCertificationManager();
			Task task = createSimpleTask(OPERATION_CLOSE_STAGE);
			cm.closeCurrentStage(campaign.getOid(), campaign.getCurrentStageNumber(), task, result);
		}catch (Exception ex) {
			result.recordFatalError(ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		showResult(result);
		target.add((Component) getCampaignsTable());
		target.add(getFeedbackPanel());
	}

	private void campaignDetailsPerformed(AjaxRequestTarget target, String oid) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, oid);
		setResponsePage(new PageCertCampaign(parameters, PageCertCampaigns.this));
	}

	private void deleteCampaignsPerformed(AjaxRequestTarget target,
			List<CertCampaignListItemDto> itemsToDelete) {
		if (itemsToDelete.isEmpty()) {
			warn(getString("PageCertCampaigns.message.noCampaignsSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		OperationResult result = new OperationResult(OPERATION_DELETE_CAMPAIGNS);
		for (CertCampaignListItemDto itemToDelete : itemsToDelete) {
			try {
				Task task = createSimpleTask(OPERATION_DELETE_CAMPAIGNS);
				ObjectDelta<AccessCertificationCampaignType> delta = ObjectDelta
						.createDeleteDelta(
								AccessCertificationCampaignType.class,
								itemToDelete.getOid(), getPrismContext());
				getModelService().executeChanges(
						WebMiscUtil.createDeltaCollection(delta), null, task,
						result);
			} catch (Exception ex) {
				result.recordPartialError("Couldn't delete campaign.", ex);
				LoggingUtils.logException(LOGGER, "Couldn't delete campaign",
						ex);
			}
		}

		result.recomputeStatus();
		if (result.isSuccess()) {
			result.recordStatus(OperationResultStatus.SUCCESS,
					"The campaign(s) have been successfully deleted.");
		}

		Table campaignsTable = getCampaignsTable();
		ObjectDataProvider provider = (ObjectDataProvider) campaignsTable
				.getDataTable().getDataProvider();
		provider.clearCache();

		showResult(result);
		target.add(getFeedbackPanel(), (Component) campaignsTable);
	}

	private void actOnCampaignsPerformed(AjaxRequestTarget target,
			String operationName, List<CertCampaignListItemDto> items) {
		int processed = 0;
		CertificationManager cm = getCertificationManager();

		OperationResult result = new OperationResult(operationName);
		for (CertCampaignListItemDto item : items) {
			try {
				AccessCertificationCampaignType campaign = item.getCampaign();
				Task task = createSimpleTask(operationName);
				switch (operationName) {
				case OPERATION_START_CAMPAIGN:
					if (campaign.getState() == AccessCertificationCampaignStateType.CREATED) {
						cm.openNextStage(campaign.getOid(), 1, task, result);
						processed++;
					}
					break;
				case OPERATION_CLOSE_CAMPAIGN:
					if (campaign.getState() != AccessCertificationCampaignStateType.CLOSED) {
						cm.closeCampaign(campaign.getOid(), task, result);
						processed++;
					}
					break;
				default:
					throw new IllegalStateException("Unknown action: "
							+ operationName);
				}
			} catch (Exception ex) {
				result.recordPartialError("Couldn't process campaign.", ex);
				LoggingUtils.logException(LOGGER, "Couldn't process campaign",
						ex);
			}
		}

		if (processed == 0) {
			warn(getString("PageCertCampaigns.message.noCampaignsSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		result.recomputeStatus();
		if (result.isSuccess()) {
			result.recordStatus(OperationResultStatus.SUCCESS, processed
					+ " campaign(s) have been successfully processed.");
		}

		showResult(result);
		target.add(getFeedbackPanel(), (Component) getCampaignsTable());
	}
	// endregion

}
