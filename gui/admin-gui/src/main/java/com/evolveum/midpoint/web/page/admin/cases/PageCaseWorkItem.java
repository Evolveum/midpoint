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
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.admin.cases.dto.CaseDto;
import com.evolveum.midpoint.web.page.admin.cases.dto.CaseWorkItemDto;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Collection;

import static com.evolveum.midpoint.schema.GetOperationOptions.resolveItemsNamed;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_OBJECT_REF;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/caseWorkItem", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALL,
				label = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALL_LABEL,
				description = PageAdminCaseWorkItems.AUTH_CASE_WORK_ITEMS_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASE_WORK_ITEM_URL,
				label = "PageCaseWorkItem.auth.caseWorkItem.label",
				description = "PageCaseWorkItem.auth.caseWorkItem.description")})
public class PageCaseWorkItem extends PageAdminCaseWorkItems {

	private static final String DOT_CLASS = PageCaseWorkItem.class.getName() + ".";
	private static final String OPERATION_LOAD_CASE = DOT_CLASS + "loadCase";
    private static final String OPERATION_SAVE_CASE_WORK_ITEM = DOT_CLASS + "saveCaseWorkItem";
	private static final String PARAMETER_CASE_ID = "caseId";
	private static final String PARAMETER_CASE_WORK_ITEM_ID = "caseWorkItemId";

    private static final Trace LOGGER = TraceManager.getTrace(PageCaseWorkItem.class);
	private static final String ID_WORK_ITEM_PANEL = "workItemPanel";
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_CASE_NAME = "caseName";
	private static final String ID_CASE_DESCRIPTION = "caseDescription";
	private static final String ID_CASE_RESOURCE = "caseResource";
	private static final String ID_CASE_EVENT = "caseEvent";
	private static final String ID_CASE_OUTCOME = "caseOutcome";
	private static final String ID_CASE_CLOSE_TIMESTAMP = "caseCloseTimestamp";
	private static final String ID_CASE_STATE = "caseState";
	private static final String ID_CASE_WORK_ITEM_NAME = "caseWorkItemName";
	private static final String ID_CASE_WORK_ITEM_ASSIGNEES = "caseWorkItemAssignees";
	private static final String ID_BACK_BUTTON = "backButton";

	private LoadableModel<CaseDto> caseDtoModel;
	private LoadableModel<CaseWorkItemDto> caseWorkItemDtoModel;
	private String caseId;
	private String caseWorkItemId;

    public PageCaseWorkItem(PageParameters parameters) {

		caseId = parameters.get(PARAMETER_CASE_ID).toString();
		LOGGER.debug("CASE ID: {}", caseId);
		if (caseId == null) {
			throw new IllegalStateException("Case ID not specified.");
		}

		caseWorkItemId = parameters.get(PARAMETER_CASE_WORK_ITEM_ID).toString();
		LOGGER.debug("CASE WORK ITEM ID: {}", caseWorkItemId);
		if (caseWorkItemId == null) {
			throw new IllegalStateException("Case work item ID not specified.");
		}

		caseDtoModel = new LoadableModel<CaseDto>(false) {
			@Override
			protected CaseDto load() {
				return loadCaseDtoIfNecessary();
			}
		};

		caseWorkItemDtoModel = new LoadableModel<CaseWorkItemDto>(false) {
			@Override
			protected CaseWorkItemDto load() {
				return loadCaseWorkItemDtoIfNecessary();
			}
		};

        initLayout();
    }

	@Override
	protected void createBreadcrumb() {
		createInstanceBreadcrumb();			// to preserve page state (e.g. approver's comment)
	}

	private CaseDto loadCaseDtoIfNecessary() {
		if (caseDtoModel.isLoaded()) {
			return caseDtoModel.getObject();
		}
		Task task = createSimpleTask(OPERATION_LOAD_CASE);
		OperationResult result = task.getResult();
		CaseDto caseDto = null;
		try {
			final ObjectQuery query = QueryBuilder.queryFor(CaseType.class, getPrismContext())
					.id(caseId)
					.build();
			final Collection<SelectorOptions<GetOperationOptions>> options =
					resolveItemsNamed(F_OBJECT_REF);
			PrismObject<CaseType> caseObject = WebModelServiceUtils.loadObject(CaseType.class, caseId, options,
					PageCaseWorkItem.this, task, result);
			final CaseType caseInstance = caseObject.asObjectable();
			caseDto = new CaseDto(caseInstance);
			result.recordSuccessIfUnknown();
		} catch (RestartResponseException e) {
			throw e;	// already processed
		} catch (NullPointerException ex) {
			result.recordFatalError(getString("PageCaseWorkItem.couldNotGetCase"), ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get case because it does not exist. (It might have been already completed or deleted.)", ex);
		} catch (RuntimeException ex) {
			result.recordFatalError("Couldn't get case.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get case.", ex);
		}
		showResult(result, false);
		if (!result.isSuccess()) {
			throw redirectBackViaRestartResponseException();
		}
		return caseDto;
	}

	private CaseWorkItemDto loadCaseWorkItemDtoIfNecessary() {
		if (caseWorkItemDtoModel.isLoaded()) {
			return caseWorkItemDtoModel.getObject();
		}
		CaseWorkItemDto caseWorkItemDto = null;
		try {
			CaseWorkItemType caseWorkItem = caseDtoModel.getObject().getWorkItem(Long.parseLong(caseWorkItemId));
			if (caseWorkItem == null) {
				throw new ObjectNotFoundException("No case work item found for id " + caseWorkItemId);
			}
			caseWorkItemDto = new CaseWorkItemDto(caseWorkItem);
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get case work item because it does not exist. (It might have been already completed or deleted.)", ex);
		} catch (NumberFormatException ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse case work item id.", ex);
		}
		return caseWorkItemDto;
	}

    private void initLayout() {
    	LOGGER.trace("BEGIN PageCaseWorkItem::initLayout");

        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

		// Case Details
		mainForm.add(new Label(ID_CASE_NAME, new PropertyModel<>(caseDtoModel, CaseDto.F_NAME)));
		mainForm.add(new Label(ID_CASE_DESCRIPTION, new PropertyModel<>(caseDtoModel, CaseDto.F_DESCRIPTION)));
		mainForm.add(new Label(ID_CASE_RESOURCE, new PropertyModel<>(caseDtoModel, CaseDto.F_OBJECT_NAME)));
		mainForm.add(new Label(ID_CASE_EVENT, new PropertyModel<>(caseDtoModel, CaseDto.F_EVENT)));
		mainForm.add(new Label(ID_CASE_OUTCOME, new PropertyModel<>(caseDtoModel, CaseDto.F_OUTCOME)));
		mainForm.add(new Label(ID_CASE_CLOSE_TIMESTAMP, new PropertyModel<>(caseDtoModel, CaseDto.F_CLOSE_TIMESTAMP)));
		mainForm.add(new Label(ID_CASE_STATE, new PropertyModel<>(caseDtoModel, CaseDto.F_STATE)));

		// Case Work Item Details
		mainForm.add(new Label(ID_CASE_WORK_ITEM_NAME, new PropertyModel<>(caseWorkItemDtoModel, CaseWorkItemDto.F_NAME)));
		mainForm.add(new Label(ID_CASE_WORK_ITEM_ASSIGNEES, new PropertyModel<>(caseWorkItemDtoModel, CaseWorkItemDto.F_ASSIGNEES)));

        initButtons(mainForm);
		LOGGER.trace("END PageCaseWorkItem::initLayout");
    }

    public WorkItemPanel getWorkItemPanel() {
    	return (WorkItemPanel) get(ID_MAIN_FORM).get(ID_WORK_ITEM_PANEL);
	}

    private void initButtons(Form mainForm) {
		AjaxButton back = new AjaxButton(ID_BACK_BUTTON, createStringResource("pageCase.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				cancelPerformed(target);
			}
		};
		mainForm.add(back);
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

    private void savePerformed(AjaxRequestTarget target, boolean approved) {
//        OperationResult result = new OperationResult(OPERATION_SAVE_WORK_ITEM);
//        try {
//			WorkItemDto dto = workItemDtoModel.getObject();
//			if (approved) {
//				boolean requiredFieldsPresent = getWorkItemPanel().checkRequiredFields();
//				if (!requiredFieldsPresent) {
//					target.add(getFeedbackPanel());
//					return;
//				}
//			}
//			ObjectDelta delta = getWorkItemPanel().getDeltaFromForm();
//            getWorkflowService().completeWorkItem(dto.getWorkItemId(), approved, dto.getApproverComment(), delta, result);
//        } catch (Exception ex) {
//            result.recordFatalError("Couldn't save work item.", ex);
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save work item", ex);
//        }
//		processResult(target, result, false);
	}
}
