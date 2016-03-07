/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.model.delta.ContainerValuePanel;
import com.evolveum.midpoint.web.component.model.delta.DeltaPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDetailedDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemNewDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.GeneralChangeApprovalWorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.createResolve;
import static com.evolveum.midpoint.schema.GetOperationOptions.resolveItemsNamed;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_REQUESTER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemNewType.*;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/workItem", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL,
                label = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL_LABEL,
                description = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEM_URL,
                label = "PageWorkItem.auth.workItem.label",
                description = "PageWorkItem.auth.workItem.description")})
public class PageWorkItem extends PageAdminWorkItems {

    private static final String DOT_CLASS = PageWorkItem.class.getName() + ".";
    private static final String OPERATION_LOAD_WORK_ITEM = DOT_CLASS + "loadWorkItem";
    private static final String OPERATION_LOAD_PROCESS_INSTANCE = DOT_CLASS + "loadProcessInstance";
    private static final String OPERATION_SAVE_WORK_ITEM = DOT_CLASS + "saveWorkItem";
    private static final String OPERATION_CLAIM_WORK_ITEM = DOT_CLASS + "claimWorkItem";
    private static final String OPERATION_RELEASE_WORK_ITEM = DOT_CLASS + "releaseWorkItem";

    private static final String ID_WORK_ITEM_PANEL = "workItemPanel";

    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItem.class);

    private PageParameters parameters;

    private LoadableModel<WorkItemNewDto> workItemDtoModel;

    public PageWorkItem() {
        this(new PageParameters(), null);
    }

    public PageWorkItem(PageParameters parameters, PageBase previousPage) {
        this(parameters, previousPage, false);
    }

    public PageWorkItem(PageParameters parameters, PageBase previousPage, boolean reinitializePreviousPage) {

        this.parameters = parameters;
        setPreviousPage(previousPage);
        setReinitializePreviousPages(reinitializePreviousPage);

        workItemDtoModel = new LoadableModel<WorkItemNewDto>(false) {
            @Override
            protected WorkItemNewDto load() {
                return loadWorkItemDtoIfNecessary();
            }
        };

        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                return new PropertyModel<String>(workItemDtoModel, "name").getObject();
            }
        };
    }

    private WorkItemNewDto loadWorkItemDtoIfNecessary() {
        if (workItemDtoModel.isLoaded()) {
            return workItemDtoModel.getObject();
        }
        Task task = createSimpleTask(OPERATION_LOAD_WORK_ITEM);
        OperationResult result = task.getResult();
        WorkItemNewDto workItemDto = null;
        try {
            String id = parameters.get(OnePageParameterEncoder.PARAMETER).toString();
            final ObjectQuery query = QueryBuilder.queryFor(WorkItemNewType.class, getPrismContext())
                    .item(F_WORK_ITEM_ID).eq(id)
                    .build();
			final Collection<SelectorOptions<GetOperationOptions>> options = resolveItemsNamed(
					F_ASSIGNEE_REF,
					new ItemPath(F_TASK_REF, F_WORKFLOW_CONTEXT, F_REQUESTER_REF));
			List<WorkItemNewType> workItems = getModelService().searchContainers(WorkItemNewType.class, query, options, task, result);
            if (workItems.size() > 1) {
                throw new SystemException("More than one work item with ID of " + id);
            } else if (workItems.size() == 0) {
                throw new SystemException("No work item with ID of " + id);
            }
            workItemDto = new WorkItemNewDto(workItems.get(0));
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get work item.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get work item.", ex);
        }
        showResult(result, false);
        if (!result.isSuccess()) {
            throw getRestartResponseException(PageDashboard.class);
        }
        return workItemDto;
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        mainForm.setMultiPart(true);
        add(mainForm);

        mainForm.add(new WorkItemPanel(ID_WORK_ITEM_PANEL, workItemDtoModel));

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {

        VisibleEnableBehaviour isAllowedToSubmit = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getWorkflowManager().isCurrentUserAuthorizedToSubmit(workItemDtoModel.getObject().getWorkItem());
            }
        };

        VisibleEnableBehaviour isAllowedToClaim = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return workItemDtoModel.getObject().getWorkItem().getAssigneeRef() == null &&
                        getWorkflowManager().isCurrentUserAuthorizedToClaim(workItemDtoModel.getObject().getWorkItem());
            }
        };

        VisibleEnableBehaviour isAllowedToRelease = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                WorkItemNewType workItem = workItemDtoModel.getObject().getWorkItem();
                MidPointPrincipal principal;
                try {
                    principal = (MidPointPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                } catch (ClassCastException e) {
                    return false;
                }
                String principalOid = principal.getOid();
                if (workItem.getAssigneeRef() == null || !workItem.getAssigneeRef().getOid().equals(principalOid)) {
                    return false;
                }
                return !workItem.getCandidateUsersRef().isEmpty() || !workItem.getCandidateRolesRef().isEmpty();
            }
        };

        AjaxSubmitButton claim = new AjaxSubmitButton("claim", createStringResource("pageWorkItem.button.claim")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                claimPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        claim.add(isAllowedToClaim);
        mainForm.add(claim);

        AjaxSubmitButton release = new AjaxSubmitButton("release", createStringResource("pageWorkItem.button.release")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                releasePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        release.add(isAllowedToRelease);
        mainForm.add(release);

        AjaxSubmitButton approve = new AjaxSubmitButton("approve", createStringResource("pageWorkItem.button.approve")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target, true);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        approve.add(isAllowedToSubmit);
        mainForm.add(approve);

        AjaxSubmitButton reject = new AjaxSubmitButton("reject", createStringResource("pageWorkItem.button.reject")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target, false);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        reject.add(isAllowedToSubmit);
        mainForm.add(reject);

        AjaxButton cancel = new AjaxButton("cancel", createStringResource("pageWorkItem.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        goBack(PageWorkItems.class);
    }

    private void savePerformed(AjaxRequestTarget target, boolean decision) {
        LOGGER.debug("Saving work item changes.");

        OperationResult result = new OperationResult(OPERATION_SAVE_WORK_ITEM);

        try {
//            reviveModels();
//            getWorkflowService().approveOrRejectWorkItemWithDetails(workItemDtoModel.getObject().getWorkItem().getWorkItemId(), object, decision, result);
            setReinitializePreviousPages(true);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't save work item.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save work item", ex);
        }

        result.computeStatusIfUnknown();

        if (!result.isSuccess()) {
            showResult(result, false);
            target.add(getFeedbackPanel());
        } else {
        	showResult(result);
            goBack(PageWorkItems.class);
        }
    }

    private void claimPerformed(AjaxRequestTarget target) {

        OperationResult result = new OperationResult(OPERATION_CLAIM_WORK_ITEM);
        WorkflowService workflowService = getWorkflowService();
        try {
            workflowService.claimWorkItem(workItemDtoModel.getObject().getWorkItemId(), result);
            setReinitializePreviousPages(true);
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't claim work item due to an unexpected exception.", e);
        }
        result.computeStatusIfUnknown();

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
        	showResult(result);
            goBack(PageWorkItems.class);
        }
    }

    private void releasePerformed(AjaxRequestTarget target) {

        OperationResult result = new OperationResult(OPERATION_RELEASE_WORK_ITEM);
        WorkflowService workflowService = getWorkflowService();
        try {
            workflowService.releaseWorkItem(workItemDtoModel.getObject().getWorkItem().getWorkItemId(), result);
            setReinitializePreviousPages(true);
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't release work item due to an unexpected exception.", e);
        }
        result.computeStatusIfUnknown();

        if (!result.isSuccess()) {
            showResult(result);
            target.add(getFeedbackPanel());
        } else {
        	showResult(result);
            goBack(PageWorkItems.class);
        }
    }

    @Override
    public PageBase reinitialize() {
        return new PageWorkItem(parameters, getPreviousPage(), true);
    }

}
