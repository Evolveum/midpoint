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
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.data.column.SingleButtonColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
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
@PageDescriptor(url = "/admin/certificationCampaigns", encoder = OnePageParameterEncoder.class, action = { @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL, label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL, description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION) })
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
	private static final String DIALOG_CONFIRM_MULTIPLE_CLOSESTAGE = "confirmMultipleCloseStagePopup";
	private static final String DIALOG_CONFIRM_CLOSESTAGE = "confirmCloseStagePopup";
	private AccessCertificationCampaignType campaign;

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
				PrismObject<AccessCertificationDefinitionType> definitionPrismObject = WebModelUtils
						.loadObject(AccessCertificationDefinitionType.class,
								definitionOid, new OperationResult("dummy"),
								PageCertCampaigns.this);
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
				DIALOG_CONFIRM_MULTIPLE_CLOSESTAGE,
				createStringResource("PageCertCampaigns.title.confirmCloseStage"),
				createCloseStageConfirmString()) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				closeSelectedCampaignsPerformed(target);
			}
		});

		add(new ConfirmationDialog(
				DIALOG_CONFIRM_CLOSESTAGE,
				createStringResource("PageCertCampaigns.dialog.title.confirmCloseStage"),
				createCloseStageConfirmString()) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				OperationResult result = new OperationResult(
						OPERATION_ADVANCE_LIFECYCLE);

				try {
					Task task;
					task = createSimpleTask(OPERATION_CLOSE_STAGE);
					int currentStage = campaign.getCurrentStageNumber();
					CertificationManager cm = getCertificationManager();
					close(target);
					cm.closeCurrentStage(campaign.getOid(), currentStage, task,
							result);
				} catch (Exception ex) {
					result.recordFatalError(ex);
				} finally {
					result.computeStatusIfUnknown();
				}
				
				showResult(result);
				target.add(getCampaignsTable());
				target.add(getFeedbackPanel());
			}
		});

		CertCampaignListItemDtoProvider provider = createProvider();
		TablePanel<CertCampaignListItemDto> table = new TablePanel<>(
				ID_CAMPAIGNS_TABLE, provider, initColumns());
		table.setShowPaging(true);
		table.setOutputMarkupId(true);
		mainForm.add(table);
	}

	private IModel<String> createCloseStageConfirmString() {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (campaign == null) {
					return createStringResource(
							"PageCertCampaigns.message.closeStageConfirmMultiple",
							WebMiscUtil.getSelectedData(getTable()).size())
							.getString();
				} else {
					return createStringResource(
							"PageCertCampaigns.message.closeStageConfirmSingle",
							campaign.getName()).getString();
				}
			}
		};
	}

	private TablePanel getTable() {
		return (TablePanel) get(createComponentPath(ID_MAIN_FORM,
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
				executeCampaignStateOperation(target, model.getObject()
						.getCampaign());
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
						// closeSelectedCampaignsPerformed(target);
						closeMultipleStageConfirmation(target);
					}
				}));
		items.add(new InlineMenuItem(
				createStringResource("PageCertCampaigns.menu.deleteSelected"),
				false, new HeaderMenuAction(this) {
					@Override
					public void onClick(AjaxRequestTarget target) {
						deleteSelectedCampaignsPerformed(target);
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
								closeMultipleStageConfirmation(target);
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
								deleteCampaignsPerformed(target, Arrays
										.asList(getRowModel().getObject()));
							}
						}));
	}

	private TablePanel getCampaignsTable() {
		return (TablePanel) get(createComponentPath(ID_MAIN_FORM,
				ID_CAMPAIGNS_TABLE));
	}

	// endregion

	// region Actions

	private void deleteSelectedCampaignsPerformed(AjaxRequestTarget target) {
		deleteCampaignsPerformed(target,
				(List) WebMiscUtil.getSelectedData(getCampaignsTable()));
	}

	private void closeSelectedCampaignsPerformed(AjaxRequestTarget target) {
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

	private void closeMultipleStageConfirmation(AjaxRequestTarget target) {

		ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_MULTIPLE_CLOSESTAGE);
		dialog.show(target);
	}

	private void closeStageConfirmation(AjaxRequestTarget target) {

		ModalWindow dialog = (ModalWindow) get(DIALOG_CONFIRM_CLOSESTAGE);
		dialog.show(target);
	}

	private void executeCampaignStateOperation(AjaxRequestTarget target,
			AccessCertificationCampaignType campaign) {
		LOGGER.debug("Advance/close certification campaign performed for {}",
				campaign.asPrismObject());

		OperationResult result = new OperationResult(
				OPERATION_ADVANCE_LIFECYCLE);
		try {
			int currentStage = campaign.getCurrentStageNumber();
			CertificationManager cm = getCertificationManager();
			String action = determineAction(campaign);
			Task task;
			switch (action) {
			case OP_START_CAMPAIGN:
			case OP_OPEN_NEXT_STAGE:
				task = createSimpleTask(OPERATION_OPEN_NEXT_STAGE);
				cm.openNextStage(campaign.getOid(), currentStage + 1, task,
						result);
				break;
			case OP_CLOSE_STAGE:
				task = createSimpleTask(OPERATION_CLOSE_STAGE);
				closeStageConfirmation(target);
				this.campaign = campaign;
				// xcm.closeCurrentStage(campaign.getOid(), currentStage, task,
				// result);
				break;
			case OP_START_REMEDIATION:
				task = createSimpleTask(OPERATION_START_REMEDIATION);
				cm.startRemediation(campaign.getOid(), task, result);
				break;
			case OP_CLOSE_CAMPAIGN: // not used
				task = createSimpleTask(OPERATION_CLOSE_CAMPAIGN);
				cm.closeCampaign(campaign.getOid(), task, result);
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
		target.add(getCampaignsTable());
		target.add(getFeedbackPanel());
	}

	private void closeCampaign(AjaxRequestTarget target,
			AccessCertificationCampaignType campaign) {
		LOGGER.debug("Close certification campaign performed for {}",
				campaign.asPrismObject());

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
		target.add(getCampaignsTable());
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

		TablePanel campaignsTable = getCampaignsTable();
		ObjectDataProvider provider = (ObjectDataProvider) campaignsTable
				.getDataTable().getDataProvider();
		provider.clearCache();

		showResult(result);
		target.add(getFeedbackPanel(), campaignsTable);
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
				int currentStage = campaign.getCurrentStageNumber();
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
		target.add(getFeedbackPanel(), getCampaignsTable());
	}
	// endregion

}
