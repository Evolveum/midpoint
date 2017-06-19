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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.WorkItemsPanel;
import com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalHistoryPanel;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.TaskChangesPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * @author mederly
 */
public class WorkItemPanel extends BasePanel<WorkItemDto> {

    private static final Trace LOGGER = TraceManager.getTrace(WorkItemPanel.class);

    private static final String ID_PRIMARY_INFO_COLUMN = "primaryInfoColumn";
    private static final String ID_ADDITIONAL_INFO_COLUMN = "additionalInfoColumn";

//    private static final String ID_REQUESTED_BY = "requestedBy";
//    private static final String ID_REQUESTED_BY_FULL_NAME = "requestedByFullName";
//    private static final String ID_REQUESTED_ON = "requestedOn";
    private static final String ID_WORK_ITEM_CREATED_ON = "workItemCreatedOn";
    private static final String ID_WORK_ITEM_DEADLINE = "workItemDeadline";
    private static final String ID_ORIGINALLY_ALLOCATED_TO = "originallyAllocatedTo";
    private static final String ID_CURRENTLY_ALLOCATED_TO = "currentlyAllocatedTo";
    private static final String ID_CANDIDATES = "candidates";
    private static final String ID_STAGE_INFO_CONTAINER = "stageInfoContainer";
    private static final String ID_STAGE_INFO = "stageInfo";
    private static final String ID_ESCALATION_LEVEL_INFO_CONTAINER = "escalationLevelInfoContainer";
    private static final String ID_ESCALATION_LEVEL_INFO = "escalationLevelInfo";
    private static final String ID_DELTAS_TO_BE_APPROVED = "deltasToBeApproved";
    private static final String ID_HISTORY_CONTAINER = "historyContainer";
    private static final String ID_HISTORY = "history";
    private static final String ID_HISTORY_HELP = "approvalHistoryHelp";
    private static final String ID_RELATED_WORK_ITEMS_CONTAINER = "relatedWorkItemsContainer";
    private static final String ID_RELATED_WORK_ITEMS = "relatedWorkItems";
	private static final String ID_RELATED_WORK_ITEMS_HELP = "otherWorkItemsHelp";
    private static final String ID_RELATED_REQUESTS_CONTAINER = "relatedRequestsContainer";
    private static final String ID_RELATED_REQUESTS = "relatedRequests";
    private static final String ID_RELATED_REQUESTS_HELP = "relatedRequestsHelp";
    private static final String ID_ADDITIONAL_INFORMATION = "additionalInformation";
    private static final String ID_CUSTOM_FORM = "customForm";
    private static final String ID_APPROVER_COMMENT = "approverComment";
	private static final String ID_SHOW_REQUEST = "showRequest";
	private static final String ID_SHOW_REQUEST_HELP = "showRequestHelp";
	private static final String ID_REQUESTER_COMMENT_CONTAINER = "requesterCommentContainer";
	private static final String ID_REQUESTER_COMMENT_MESSAGE = "requesterCommentMessage";
	private static final String ID_ADDITIONAL_ATTRIBUTES = "additionalAttributes";
	
	private static final String DOT_CLASS = WorkItemPanel.class.getName() + ".";
	private static final String OPERATION_LOAD_CUSTOM_FORM = DOT_CLASS + "loadCustomForm";


	public WorkItemPanel(String id, IModel<WorkItemDto> model, Form mainForm, PageBase pageBase) {
        super(id, model);
        initLayout(mainForm, pageBase);
    }

    protected void initLayout(Form mainForm, PageBase pageBase) {
		WebMarkupContainer additionalInfoColumn = new WebMarkupContainer(ID_ADDITIONAL_INFO_COLUMN);

		WebMarkupContainer historyContainer = new WebMarkupContainer(ID_HISTORY_CONTAINER);
        historyContainer.add(new ItemApprovalHistoryPanel(ID_HISTORY,
				new PropertyModel<>(getModel(), WorkItemDto.F_WORKFLOW_CONTEXT),
				UserProfileStorage.TableId.PAGE_WORK_ITEM_HISTORY_PANEL, (int) pageBase.getItemsPerPage(UserProfileStorage.TableId.PAGE_WORK_ITEM_HISTORY_PANEL)));
		final VisibleEnableBehaviour historyContainerVisible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().hasHistory();
			}
		};
		historyContainer.add(historyContainerVisible);
		historyContainer.add(WebComponentUtil.createHelp(ID_HISTORY_HELP));
		additionalInfoColumn.add(historyContainer);

		WebMarkupContainer relatedWorkItemsContainer = new WebMarkupContainer(ID_RELATED_WORK_ITEMS_CONTAINER);
		final IModel<List<WorkItemDto>> relatedWorkItemsModel = new PropertyModel<>(getModel(), WorkItemDto.F_OTHER_WORK_ITEMS);
		final ISortableDataProvider<WorkItemDto, String> relatedWorkItemsProvider = new ListDataProvider<>(this, relatedWorkItemsModel);
		relatedWorkItemsContainer.add(new WorkItemsPanel(ID_RELATED_WORK_ITEMS, relatedWorkItemsProvider, null, 10, WorkItemsPanel.View.ITEMS_FOR_PROCESS));
		final VisibleEnableBehaviour relatedWorkItemsContainerVisible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !relatedWorkItemsModel.getObject().isEmpty();
			}
		};
		relatedWorkItemsContainer.add(relatedWorkItemsContainerVisible);
		relatedWorkItemsContainer.add(WebComponentUtil.createHelp(ID_RELATED_WORK_ITEMS_HELP));
		additionalInfoColumn.add(relatedWorkItemsContainer);

		final WebMarkupContainer relatedWorkflowRequestsContainer = new WebMarkupContainer(ID_RELATED_REQUESTS_CONTAINER);
		final IModel<List<ProcessInstanceDto>> relatedWorkflowRequestsModel = new PropertyModel<>(getModel(), WorkItemDto.F_RELATED_WORKFLOW_REQUESTS);
		final ISortableDataProvider<ProcessInstanceDto, String> relatedWorkflowRequestsProvider = new ListDataProvider<>(this, relatedWorkflowRequestsModel);
		relatedWorkflowRequestsContainer.add(
				new ProcessInstancesPanel(ID_RELATED_REQUESTS, relatedWorkflowRequestsProvider, null, 10,
						ProcessInstancesPanel.View.TASKS_FOR_PROCESS,
						new PropertyModel<>(getModel(), WorkItemDto.F_PROCESS_INSTANCE_ID)));
		final VisibleEnableBehaviour relatedWorkflowRequestsContainerVisible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !relatedWorkflowRequestsModel.getObject().isEmpty();
			}
		};
		relatedWorkflowRequestsContainer.add(relatedWorkflowRequestsContainerVisible);
		relatedWorkflowRequestsContainer.add(WebComponentUtil.createHelp(ID_RELATED_REQUESTS_HELP));
		additionalInfoColumn.add(relatedWorkflowRequestsContainer);
		final VisibleEnableBehaviour additionalInfoColumnVisible = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return historyContainerVisible.isVisible() || relatedWorkItemsContainerVisible.isVisible() || relatedWorkflowRequestsContainerVisible
						.isVisible();
			}
		};
		additionalInfoColumn.add(additionalInfoColumnVisible);
		add(additionalInfoColumn);

		WebMarkupContainer primaryInfoColumn = new WebMarkupContainer(ID_PRIMARY_INFO_COLUMN);

//		primaryInfoColumn.add(new Label(ID_REQUESTED_BY, new PropertyModel(getModel(), WorkItemDto.F_REQUESTER_NAME)));
//		primaryInfoColumn.add(new Label(ID_REQUESTED_BY_FULL_NAME, new PropertyModel(getModel(), WorkItemDto.F_REQUESTER_FULL_NAME)));
//		primaryInfoColumn.add(new Label(ID_REQUESTED_ON, new PropertyModel(getModel(), WorkItemDto.F_STARTED_FORMATTED_FULL)));
		primaryInfoColumn.add(new Label(ID_WORK_ITEM_CREATED_ON, new PropertyModel(getModel(), WorkItemDto.F_CREATED_FORMATTED_FULL)));
		primaryInfoColumn.add(new Label(ID_WORK_ITEM_DEADLINE, new PropertyModel(getModel(), WorkItemDto.F_DEADLINE_FORMATTED_FULL)));
		primaryInfoColumn.add(new Label(ID_ORIGINALLY_ALLOCATED_TO, new PropertyModel(getModel(), WorkItemDto.F_ORIGINAL_ASSIGNEE_FULL)));
		primaryInfoColumn.add(new Label(ID_CURRENTLY_ALLOCATED_TO, new PropertyModel(getModel(), WorkItemDto.F_CURRENT_ASSIGNEES_FULL)));
		primaryInfoColumn.add(new Label(ID_CANDIDATES, new PropertyModel(getModel(), WorkItemDto.F_CANDIDATES)));

		WebMarkupContainer stageInfoContainer = new WebMarkupContainer(ID_STAGE_INFO_CONTAINER);
		primaryInfoColumn.add(stageInfoContainer);
		stageInfoContainer.add(new Label(ID_STAGE_INFO, new PropertyModel<String>(getModel(), WorkItemDto.F_STAGE_INFO)));
		stageInfoContainer.add(new VisibleBehaviour(() -> getModelObject().getStageInfo() != null));

		WebMarkupContainer escalationLevelInfoContainer = new WebMarkupContainer(ID_ESCALATION_LEVEL_INFO_CONTAINER);
		primaryInfoColumn.add(escalationLevelInfoContainer);
		escalationLevelInfoContainer.add(new Label(ID_ESCALATION_LEVEL_INFO, new PropertyModel<String>(getModel(), WorkItemDto.F_ESCALATION_LEVEL_INFO)));
		escalationLevelInfoContainer.add(new VisibleBehaviour(() -> getModelObject().getEscalationLevelInfo() != null));

		WebMarkupContainer requesterCommentContainer = new WebMarkupContainer(ID_REQUESTER_COMMENT_CONTAINER);
		requesterCommentContainer.setOutputMarkupId(true);
		primaryInfoColumn.add(requesterCommentContainer);
		requesterCommentContainer.add(new Label(ID_REQUESTER_COMMENT_MESSAGE, new PropertyModel<String>(getModel(), WorkItemDto.F_REQUESTER_COMMENT)));

		//primaryInfoColumn.add(new ScenePanel(ID_DELTAS_TO_BE_APPROVED, new PropertyModel<SceneDto>(getModel(), WorkItemDto.F_DELTAS)));
		primaryInfoColumn.add(new TaskChangesPanel(ID_DELTAS_TO_BE_APPROVED, new PropertyModel<>(getModel(), WorkItemDto.F_CHANGES)));
		primaryInfoColumn.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return additionalInfoColumnVisible.isVisible() ? "col-md-5" : "col-md-12";
			}
		}));
		add(primaryInfoColumn);

		add(new AjaxFallbackLink(ID_SHOW_REQUEST) {
			public void onClick(AjaxRequestTarget target) {
				String oid = WorkItemPanel.this.getModelObject().getTaskOid();
				if (oid != null) {
					PageParameters parameters = new PageParameters();
					parameters.add(OnePageParameterEncoder.PARAMETER, oid);
					getPageBase().navigateToNext(PageTaskEdit.class, parameters);
				}
			}
		});
		add(WebComponentUtil.createHelp(ID_SHOW_REQUEST_HELP));

		WebMarkupContainer additionalInformation = new InformationListPanel(ID_ADDITIONAL_INFORMATION,
				new PropertyModel<>(getModel(), WorkItemDto.F_ADDITIONAL_INFORMATION));
		add(additionalInformation);

		WorkItemDto dto = null;
		try {
			dto = getModelObject();
		} catch (Throwable t) {
			// We don't want to process the exception in the constructor (e.g. because breadcrumbs are not initialized,
			// so we are not able to return to the previous page, etc - see MID-3799.9). But we need to have the object
			// here, if at all possible, to include dynamic form. So the hack is: if there's an error, just ignore it here.
			// It will repeat when trying to draw the page. And then we will process it correctly.
			LOGGER.debug("Ignoring getModelObject exception because we're in constructor. It will occur later and will be correctly processed then.", t);
			getSession().getFeedbackMessages().clear();
		}
		ApprovalStageDefinitionType level = dto != null ? WfContextUtil.getCurrentStageDefinition(dto.getWorkflowContext()) : null;
		WebMarkupContainer additionalAttributes = new WebMarkupContainer(ID_ADDITIONAL_ATTRIBUTES);
		add(additionalAttributes);
		additionalAttributes.add(new VisibleEnableBehaviour() {
		
			private static final long serialVersionUID = 1L;

			public boolean isVisible() {
				return (level != null && level.getFormRef() != null && level.getFormRef().getOid() != null);
			};
		});
		
		if (level != null && level.getFormRef() != null && level.getFormRef().getOid() != null) {
			String formOid = level.getFormRef().getOid();
			ObjectType focus = dto.getFocus(pageBase);
			if (focus == null) {
				focus = new UserType(pageBase.getPrismContext());		// TODO FIXME (this should not occur anyway)
			}
			Task task = pageBase.createSimpleTask(OPERATION_LOAD_CUSTOM_FORM);
			DynamicFormPanel<?> customForm = new DynamicFormPanel<>(ID_CUSTOM_FORM,
					focus.asPrismObject(), formOid, mainForm, task, pageBase, false);
			additionalAttributes.add(customForm);
		} else {
			additionalAttributes.add(new Label(ID_CUSTOM_FORM));
		}

        add(new TextArea<>(ID_APPROVER_COMMENT, new PropertyModel<String>(getModel(), WorkItemDto.F_APPROVER_COMMENT)));
    }

	ObjectDelta getDeltaFromForm() throws SchemaException {
		Component formPanel = getFormPanel();
		if (formPanel instanceof DynamicFormPanel) {
			return ((DynamicFormPanel<?>) formPanel).getObjectDelta();
		} else {
			return null;
		}
	}

	// true means OK
	boolean checkRequiredFields() {
		Component formPanel = getFormPanel();
		if (formPanel instanceof DynamicFormPanel) {
			return ((DynamicFormPanel<?>) formPanel).checkRequiredFields(getPageBase());
		} else {
			return true;
		}
	}

	private Component getFormPanel() {
		return get(createComponentPath(ID_ADDITIONAL_ATTRIBUTES, ID_CUSTOM_FORM));
	}
}
