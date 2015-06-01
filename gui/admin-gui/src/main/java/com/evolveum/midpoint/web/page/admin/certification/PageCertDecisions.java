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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.DirectlyEditablePropertyColumn;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn.BUTTON_COLOR_CLASS;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REDUCE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/certificationDecisions", action = { @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL, label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL, description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION) })
public class PageCertDecisions extends PageAdminWorkItems {

	private static final Trace LOGGER = TraceManager
			.getTrace(PageCertDecisions.class);

	private static final String DOT_CLASS = PageCertDecisions.class.getName()
			+ ".";
	private static final String OPERATION_RECORD_ACTION = DOT_CLASS
			+ "recordAction";
	private static final String OPERATION_RECORD_ACTION_SELECTED = DOT_CLASS + "recordActionSelected";
	
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_DECISIONS_TABLE = "decisionsTable";
	private PrismContainerValue decision;

	public PageCertDecisions() {
		initLayout();
	}

	private void initLayout() {
		Form mainForm = new Form(ID_MAIN_FORM);
		add(mainForm);
		CertDecisionDtoProvider provider = new CertDecisionDtoProvider(
				PageCertDecisions.this);
		provider.setQuery(createCaseQuery());
		provider.setCampaignQuery(createCampaignQuery());
		provider.setReviewerOid(getCurrentUserOid());
		TablePanel table = new TablePanel<>(ID_DECISIONS_TABLE, provider,
				initColumns());
		table.setShowPaging(true);
		table.setOutputMarkupId(true);
		mainForm.add(table);
	}

	private String getCurrentUserOid() {
		try {
			return getSecurityEnforcer().getPrincipal().getOid();
		} catch (SecurityViolationException e) {
			// TODO handle more cleanly
			throw new SystemException("Couldn't get currently logged user OID",
					e);
		}
	}

	private List<IColumn<CertDecisionDto, String>> initColumns() {
		List<IColumn<CertDecisionDto, String>> columns = new ArrayList<>();
		
		IColumn column;

		column = new CheckBoxHeaderColumn<>();
		columns.add(column);

		column = new LinkColumn<CertDecisionDto>(createStringResource("PageCertDecisions.table.subjectName"),
				AccessCertificationCaseType.F_SUBJECT_REF.getLocalPart(), CertDecisionDto.F_SUBJECT_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<CertDecisionDto> rowModel) {
				CertDecisionDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getCertCase().getSubjectRef());
			}
		};
		columns.add(column);

		column = new LinkColumn<CertDecisionDto>(createStringResource("PageCertDecisions.table.targetName"),
				AccessCertificationCaseType.F_TARGET_REF.getLocalPart(), CertDecisionDto.F_TARGET_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<CertDecisionDto> rowModel) {
				CertDecisionDto dto = rowModel.getObject();
				dispatchToObjectDetailsPage(dto.getCertCase().getTargetRef());
			}
		};
		columns.add(column);

		column = new PropertyColumn(
				createStringResource("PageCertDecisions.table.targetType"),
				CertDecisionDto.F_TARGET_TYPE);
		columns.add(column);


		column = new LinkColumn<CertDecisionDto>(
				createStringResource("PageCertDecisions.table.campaignName"),
				AccessCertificationCaseType.F_CAMPAIGN_REF.getLocalPart(), CertDecisionDto.F_CAMPAIGN_NAME) {
			@Override
			public void populateItem(Item<ICellPopulator<CertDecisionDto>> item, String componentId, IModel<CertDecisionDto> rowModel) {
				super.populateItem(item, componentId, rowModel);
				AccessCertificationCampaignType campaign = rowModel.getObject().getCampaign();
				if (campaign != null && campaign.getDescription() != null) {
					item.add(AttributeModifier.replace("title", campaign.getDescription()));
					item.add(new TooltipBehavior());
				}
			}
			@Override
			public void onClick(AjaxRequestTarget target, IModel<CertDecisionDto> rowModel) {
				CertDecisionDto dto = rowModel.getObject();
				PageParameters parameters = new PageParameters();
				parameters.add(OnePageParameterEncoder.PARAMETER, dto.getCampaignRef().getOid());
				setResponsePage(PageCertCampaignStatistics.class, parameters);
			}
		};
		columns.add(column);

		column = new AbstractColumn<CertDecisionDto,String>(
				createStringResource("PageCertDecisions.table.campaignStage")) {
			@Override
			public void populateItem(Item<ICellPopulator<CertDecisionDto>> item, String componentId, final IModel<CertDecisionDto> rowModel) {
				item.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
					@Override
					public String getObject() {
						CertDecisionDto dto = rowModel.getObject();
						return dto.getCampaignStageNumber()+"/"+dto.getCampaignStageCount();
					}
				}));
				String stageName = rowModel.getObject().getCurrentStageName();
				if (stageName != null) {
					item.add(AttributeModifier.replace("title", stageName));
					item.add(new TooltipBehavior());
				}
			}
		};
		columns.add(column);

		column = new PropertyColumn<CertDecisionDto,String>(
				createStringResource("PageCertDecisions.table.requested"),
				AccessCertificationCaseType.F_REVIEW_REQUESTED_TIMESTAMP.getLocalPart(),
				CertDecisionDto.F_REVIEW_REQUESTED) {
			@Override
			public void populateItem(Item<ICellPopulator<CertDecisionDto>> item, String componentId, IModel<CertDecisionDto> rowModel) {
				super.populateItem(item, componentId, rowModel);
				CertDecisionDto dto = rowModel.getObject();
				Date started = dto.getStageStarted();
				if (started != null) {
					item.add(AttributeModifier.replace("title", WebMiscUtil.formatDate(started)));
					item.add(new TooltipBehavior());
				}
			}
		};
		columns.add(column);

		column = new AbstractColumn<CertDecisionDto,String>(createStringResource("PageCertDecisions.table.deadline"),
				AccessCertificationCaseType.F_REVIEW_DEADLINE.getLocalPart()) {
			@Override
			public void populateItem(Item<ICellPopulator<CertDecisionDto>> item, String componentId, final IModel<CertDecisionDto> rowModel) {
				item.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
					@Override
					public String getObject() {
						return deadline(rowModel);
					}
				}));
				XMLGregorianCalendar deadline = rowModel.getObject().getCertCase().getReviewDeadline();
				if (deadline != null) {
					item.add(AttributeModifier.replace("title", WebMiscUtil.formatDate(deadline)));
					item.add(new TooltipBehavior());
				}
			}
		};
		columns.add(column);

		column = new DoubleButtonColumn<CertDecisionDto>(new Model(), null) {
			
			@Override
			public String getFirstCap() {
				return PageCertDecisions.this.createStringResource(
						"PageCertDecisions.menu.accept").getString();
			}

			@Override
			public String getSecondCap() {
				return PageCertDecisions.this.createStringResource(
						"PageCertDecisions.menu.revoke").getString();
			}

			@Override
			public boolean isFirstButtonEnabled(IModel<CertDecisionDto> model) {
				return !decisionEquals(model, ACCEPT);
		    }

			@Override
			public String getFirstColorCssClass() {
				return getDecisionButtonColor(getRowModel(), ACCEPT);
			}

			@Override
			public boolean isSecondButtonEnabled(IModel<CertDecisionDto> model) {
				return !decisionEquals(model, AccessCertificationResponseType.REVOKE);
			}

			@Override
			public String getSecondColorCssClass() {
				return getDecisionButtonColor(getRowModel(), AccessCertificationResponseType.REVOKE);
			}

			@Override
			public void firstClicked(AjaxRequestTarget target,
					IModel<CertDecisionDto> model) {
				recordActionPerformed(target, model.getObject(), ACCEPT);
			}

			@Override
			public void secondClicked(AjaxRequestTarget target,
					IModel<CertDecisionDto> model) {
				// TODO revoke the roles
				recordActionPerformed(target, model.getObject(), AccessCertificationResponseType.REVOKE);
			}
		};
		columns.add(column);
		column = new DoubleButtonColumn<CertDecisionDto>(new Model(), null) {

			@Override
			public String getFirstCap() {
				return PageCertDecisions.this.createStringResource(
						"PageCertDecisions.menu.reduce").getString();
			}

			@Override
			public String getSecondCap() {
				return PageCertDecisions.this.createStringResource(
						"PageCertDecisions.menu.notDecided").getString();
			}

			@Override
			public boolean isFirstButtonEnabled(IModel<CertDecisionDto> model) {
				return !decisionEquals(model, AccessCertificationResponseType.REDUCE);
			}

			@Override
			public String getFirstColorCssClass() {
				return getDecisionButtonColor(getRowModel(), AccessCertificationResponseType.REDUCE);
			}

			@Override
			public boolean isSecondButtonEnabled(IModel<CertDecisionDto> model) {
				return !decisionEquals(model, AccessCertificationResponseType.NOT_DECIDED);
			}

			@Override
			public String getSecondColorCssClass() {
				return getDecisionButtonColor(getRowModel(), AccessCertificationResponseType.NOT_DECIDED);
			}

			@Override
			public void firstClicked(AjaxRequestTarget target,
					IModel<CertDecisionDto> model) {
				recordActionPerformed(target, model.getObject(), AccessCertificationResponseType.REDUCE);
			}

			@Override
			public void secondClicked(AjaxRequestTarget target,
					IModel<CertDecisionDto> model) {
				// TODO
				recordActionPerformed(target, model.getObject(), AccessCertificationResponseType.NOT_DECIDED);
			}
		};
		columns.add(column);
		column = new DoubleButtonColumn<CertDecisionDto>(new Model(), null) {

			@Override
			public String getFirstCap() {
				return PageCertDecisions.this.createStringResource(
						"PageCertDecisions.menu.delegate").getString();
			}

			@Override
			public String getSecondCap() {
				return PageCertDecisions.this.createStringResource(
						"PageCertDecisions.menu.noResponse").getString();
			}

			@Override
			public boolean isFirstButtonEnabled(IModel<CertDecisionDto> model) {
				return !decisionEquals(model, AccessCertificationResponseType.DELEGATE);
			}

			@Override
			public String getFirstColorCssClass() {
				return getDecisionButtonColor(getRowModel(), AccessCertificationResponseType.DELEGATE);
			}

			@Override
			public boolean isSecondButtonEnabled(IModel<CertDecisionDto> model) {
				return !decisionEquals(model, AccessCertificationResponseType.NO_RESPONSE);
			}

			@Override
			public String getSecondColorCssClass() {
				return getDecisionButtonColor(getRowModel(), AccessCertificationResponseType.NO_RESPONSE);
			}

			@Override
			public void firstClicked(AjaxRequestTarget target,
					IModel<CertDecisionDto> model) {
				recordActionPerformed(target, model.getObject(), AccessCertificationResponseType.DELEGATE);
			}

			@Override
			public void secondClicked(AjaxRequestTarget target,
					IModel<CertDecisionDto> model) {
				// TODO
				recordActionPerformed(target, model.getObject(), AccessCertificationResponseType.NO_RESPONSE);
			}
		};
		columns.add(column);

		column = new DirectlyEditablePropertyColumn(
				createStringResource("PageCertDecisions.table.comment"),
				CertDecisionDto.F_COMMENT) {
			@Override
			public void onBlur(AjaxRequestTarget target, IModel model) {
				// TODO determine somehow if the model.comment was really changed
				recordActionPerformed(target, (CertDecisionDto) model.getObject(), null);
			}
		};
		columns.add(column);

		columns.add(new InlineMenuHeaderColumn(createInlineMenu()));

		return columns;
	}

	private List<InlineMenuItem> createInlineMenu() {
		List<InlineMenuItem> items = new ArrayList<>();
		items.add(createMenu("PageCertDecisions.menu.acceptSelected", ACCEPT));
		items.add(createMenu("PageCertDecisions.menu.revokeSelected", REVOKE));
		items.add(createMenu("PageCertDecisions.menu.reduceSelected", REDUCE));
		items.add(createMenu("PageCertDecisions.menu.notDecidedSelected", NOT_DECIDED));
		items.add(createMenu("PageCertDecisions.menu.delegateSelected", DELEGATE));
		items.add(createMenu("PageCertDecisions.menu.noResponseSelected", NO_RESPONSE));
		return items;
	}

	private InlineMenuItem createMenu(String titleKey, final AccessCertificationResponseType response) {
		return new InlineMenuItem(createStringResource(titleKey), false,
				new HeaderMenuAction(this) {
					@Override
					public void onClick(AjaxRequestTarget target) {
						recordActionOnSelected(response, target);
					}
				});
	}

	protected void dispatchToObjectDetailsPage(ObjectReferenceType objectRef) {
		if (objectRef == null) {
			return;		// should not occur
		}
		QName type = objectRef.getType();
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, objectRef.getOid());
		if (RoleType.COMPLEX_TYPE.equals(type)) {
            setResponsePage(PageRole.class, parameters);
        } else if (OrgType.COMPLEX_TYPE.equals(type)) {
            setResponsePage(PageOrgUnit.class, parameters);
        } else if (UserType.COMPLEX_TYPE.equals(type)) {
            setResponsePage(PageUser.class, parameters);
        } else {
            // nothing to do
        }
	}

	private String deadline(IModel<CertDecisionDto> decisionModel) {
		CertDecisionDto decisionDto = decisionModel.getObject();
		AccessCertificationCaseType _case = decisionDto.getCertCase();
		XMLGregorianCalendar deadline = _case.getReviewDeadline();

		if (deadline == null) {
			return "";
		} else {
			long delta = XmlTypeConverter.toMillis(deadline) - System.currentTimeMillis();

			// round to hours; we always round down
			long precision = 3600000L;      // 1 hour
			if (Math.abs(delta) > precision) {
				delta = (delta / precision) * precision;
			}

			//todo i18n
			if (delta > 0) {
				return new StringResourceModel("PageCertDecisions.in", this, null, null,
						DurationFormatUtils.formatDurationWords(delta, true, true)).getString();
			} else if (delta < 0) {
				return new StringResourceModel("PageCertDecisions.ago", this, null, null,
						DurationFormatUtils.formatDurationWords(-delta, true, true)).getString();
			} else {
				return getString("PageCertDecisions.now");
			}
		}
	}


	private String getDecisionButtonColor(IModel<CertDecisionDto> model, AccessCertificationResponseType response) {
		if (decisionEquals(model, response)) {
			return BUTTON_COLOR_CLASS.PRIMARY.toString();
		} else {
			return BUTTON_COLOR_CLASS.DEFAULT.toString();
		}
	}

	private boolean decisionEquals(IModel<CertDecisionDto> model, AccessCertificationResponseType response) {
		return model.getObject().getResponse() == response;
	}

	private void recordActionOnSelected(AccessCertificationResponseType response, AjaxRequestTarget target) {
		List<CertDecisionDto> certDecisionDtoList = WebMiscUtil.getSelectedData(getDecisionsTable());
		if (certDecisionDtoList.isEmpty()) {
			warn(getString("PageCertDecisions.message.noItemSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		PrismContext prismContext = getPrismContext();

		OperationResult result = new OperationResult(OPERATION_RECORD_ACTION_SELECTED);
		Task task = createSimpleTask(OPERATION_RECORD_ACTION_SELECTED);
		for (CertDecisionDto certDecisionDto : certDecisionDtoList) {
			OperationResult resultOne = result.createSubresult(OPERATION_RECORD_ACTION);
			AccessCertificationDecisionType newDecision = new AccessCertificationDecisionType(prismContext);
			newDecision.setResponse(response);
			newDecision.setStageNumber(0);
			newDecision.setComment(certDecisionDto.getComment());
			try {
				getCertificationManager().recordDecision(
						certDecisionDto.getCampaignRef().getOid(),
						certDecisionDto.getCaseId(), newDecision, task, resultOne);
			} catch (Exception ex) {
				resultOne.recordFatalError(ex);
			} finally {
				resultOne.computeStatusIfUnknown();
			}
		}
		result.computeStatus();
		showResult(result);

		target.add(getFeedbackPanel());
		target.add(getDecisionsTable());
	}


	// if response is null this means keep the current one in decisionDto
	private void recordActionPerformed(AjaxRequestTarget target,
			CertDecisionDto decisionDto, AccessCertificationResponseType response) {
		PrismContext prismContext = getPrismContext();
		AccessCertificationDecisionType newDecision = new AccessCertificationDecisionType(prismContext);
		if (response != null) {
			newDecision.setResponse(response);
		} else {
			newDecision.setResponse(decisionDto.getResponse());
		}
		newDecision.setStageNumber(0);
		newDecision.setComment(decisionDto.getComment());
		System.out.println("\n\n" + newDecision.toString());
		OperationResult result = new OperationResult(OPERATION_RECORD_ACTION);
		try {
			Task task = createSimpleTask(OPERATION_RECORD_ACTION);
			getCertificationManager().recordDecision(
					decisionDto.getCampaignRef().getOid(),
					decisionDto.getCaseId(), newDecision, task, result);
		} catch (Exception ex) {
			result.recordFatalError(ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		showResult(result);

		target.add(getDecisionsTable());
		target.add(getFeedbackPanel());
	}
	

	private ObjectQuery createCaseQuery() {
		ObjectQuery query = new ObjectQuery();
		return query;
	}

	private ObjectQuery createCampaignQuery() {
		ObjectQuery query = new ObjectQuery();
		return query;
	}

	private TablePanel getDecisionsTable() {
		return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_DECISIONS_TABLE));
	}

}
