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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.web.component.data.column.EditablePropertyColumn;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn.BUTTON_COLOR_CLASS;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

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

		column = new PropertyColumn(
				createStringResource("PageCertDecisions.table.subjectName"),
				CertDecisionDto.F_SUBJECT_NAME);
		columns.add(column);

		column = new PropertyColumn(
				createStringResource("PageCertDecisions.table.targetName"),
				CertDecisionDto.F_TARGET_NAME);
		columns.add(column);

		column = new PropertyColumn(
				createStringResource("PageCertDecisions.table.targetType"),
				CertDecisionDto.F_TARGET_TYPE);
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
				return !decisionEquals(model, AccessCertificationResponseType.ACCEPT);
		    }

			@Override
			public String getFirstColorCssClass() {
				return getDecisionButtonColor(getRowModel(), AccessCertificationResponseType.ACCEPT);
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
				recordActionPerformed(target, model.getObject(), AccessCertificationResponseType.ACCEPT);
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
				CertDecisionDto.F_COMMENT);
		columns.add(column);

		return columns;
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


	//TODO : I create different method for every action even handled by one method, system will need different methods after all implementation will over
	private void recordActionPerformed(AjaxRequestTarget target,
			CertDecisionDto decisionDto, AccessCertificationResponseType response) {
		//decisionDto.setResponse(AccessCertificationResponseType.ACCEPT.value());
		PrismContext prismContext = getPrismContext();
		AccessCertificationDecisionType newDecision = new AccessCertificationDecisionType(prismContext);			
		newDecision.setResponse(response);
		newDecision.setStageNumber(0);
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
