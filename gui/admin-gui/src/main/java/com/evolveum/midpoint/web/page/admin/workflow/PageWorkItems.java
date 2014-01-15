/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.admin.workflow.dto.*;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/workItems")
public class PageWorkItems extends PageAdminWorkItems {

    private static final Trace LOGGER = TraceManager.getTrace(PageWorkItems.class);
    private static final String DOT_CLASS = PageWorkItems.class.getName() + ".";
    private static final String OPERATION_APPROVE_OR_REJECT_ITEMS = DOT_CLASS + "approveOrRejectItems";
    private static final String OPERATION_APPROVE_OR_REJECT_ITEM = DOT_CLASS + "approveOrRejectItem";
    private static final String OPERATION_REJECT_ITEMS = DOT_CLASS + "rejectItems";
    private static final String OPERATION_CLAIM_ITEMS = DOT_CLASS + "claimItems";
    private static final String OPERATION_CLAIM_ITEM = DOT_CLASS + "claimItem";
    private static final String OPERATION_RELEASE_ITEMS = DOT_CLASS + "releaseItems";
    private static final String OPERATION_RELEASE_ITEM = DOT_CLASS + "releaseItem";

    public PageWorkItems() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

//        List<IColumn<WorkItemDto>> unassignedItemColumns = initUnassignedItemColumns();
//        TablePanel<WorkItemDto> unassignedItemTable = new TablePanel<WorkItemDto>("unassignedItemTable", new WorkItemDtoProvider(PageWorkItems.this, false),
//                unassignedItemColumns);
//        unassignedItemTable.setOutputMarkupId(true);
//        mainForm.add(unassignedItemTable);

        List<IColumn<WorkItemDto, String>> assignedItemColumns = initAssignedItemColumns();
        TablePanel<WorkItemDto> assignedItemTable = new TablePanel<WorkItemDto>("assignedItemTable", new WorkItemDtoProvider(PageWorkItems.this, true),
                assignedItemColumns);
        assignedItemTable.setOutputMarkupId(true);
        mainForm.add(assignedItemTable);

        initItemButtons(mainForm);
    }

    private List<IColumn<WorkItemDto, String>> initUnassignedItemColumns() {
        List<IColumn<WorkItemDto, String>> columns = new ArrayList<IColumn<WorkItemDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<TaskType>();
        columns.add(column);

        column = new LinkColumn<WorkItemDto>(createStringResource("pageWorkItems.item.name"), "name", "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
                WorkItemDto workItemDto = rowModel.getObject();
                itemDetailsPerformed(target, workItemDto.getWorkItem().getWorkItemId());
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("pageWorkItems.item.candidates"), "candidates"));
        return columns;
    }

    private List<IColumn<WorkItemDto, String>> initAssignedItemColumns() {
        List<IColumn<WorkItemDto, String>> columns = new ArrayList<IColumn<WorkItemDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<TaskType>();
        columns.add(column);

        column = new LinkColumn<WorkItemDto>(createStringResource("pageWorkItems.item.name"), "name", "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<WorkItemDto> rowModel) {
                WorkItemDto workItemDto = rowModel.getObject();
                itemDetailsPerformed(target, workItemDto.getWorkItem().getWorkItemId());
            }
        };
        columns.add(column);

        columns.add(new AbstractColumn<WorkItemDto, String>(createStringResource("pageWorkItems.item.created")) {

            @Override
            public void populateItem(Item<ICellPopulator<WorkItemDto>> item, String componentId,
                                     final IModel<WorkItemDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        WorkItemDto pi = rowModel.getObject();
                        Date started = XmlTypeConverter.toDate(pi.getWorkItem().getMetadata().getCreateTimestamp());
                        if (started == null) {
                            return "?";
                        } else {
                            return WebMiscUtil.formatDate(started);
                        }
                    }
                }));
            }
        });


        return columns;
    }

    private void initItemButtons(Form mainForm) {
//        AjaxLinkButton claim = new AjaxLinkButton("claim",
//                createStringResource("pageWorkItems.button.claim")) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                claimWorkItemsPerformed(target);
//            }
//        };
//        mainForm.add(claim);
//
//        AjaxLinkButton release = new AjaxLinkButton("release",
//                createStringResource("pageWorkItems.button.release")) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                releaseWorkItemsPerformed(target);
//            }
//        };
//        mainForm.add(release);

        AjaxButton approve = new AjaxButton("approve",
                createStringResource("pageWorkItems.button.approve")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                approveOrRejectWorkItemsPerformed(target, true);
            }
        };
        mainForm.add(approve);

        AjaxButton reject = new AjaxButton("reject",
                createStringResource("pageWorkItems.button.reject")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                approveOrRejectWorkItemsPerformed(target, false);
            }
        };
        mainForm.add(reject);
    }

    private TablePanel getAssignedItemTable() {
        return (TablePanel) get("mainForm:assignedItemTable");
    }

    private TablePanel getUnassignedItemTable() {
        return (TablePanel) get("mainForm:unassignedItemTable");
    }

    private List<WorkItemDto> getSelectedUnassignedItems() {
        DataTable table = getUnassignedItemTable().getDataTable();
        WorkItemDtoProvider provider = (WorkItemDtoProvider) table.getDataProvider();

        List<WorkItemDto> selected = new ArrayList<WorkItemDto>();
        for (WorkItemDto row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row);
            }
        }

        return selected;
    }

    private List<WorkItemDto> getSelectedAssignedItems() {
        DataTable table = getAssignedItemTable().getDataTable();
        WorkItemDtoProvider provider = (WorkItemDtoProvider) table.getDataProvider();

        List<WorkItemDto> selected = new ArrayList<WorkItemDto>();
        for (WorkItemDto row : provider.getAvailableData()) {
            if (row.isSelected()) {
                selected.add(row);
            }
        }

        return selected;
    }

    private boolean isSomeItemSelected(List<WorkItemDto> tasks, AjaxRequestTarget target) {
        if (!tasks.isEmpty()) {
            return true;
        }

        warn(getString("pageWorkItems.message.noItemSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    private void itemDetailsPerformed(AjaxRequestTarget target, String taskid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, taskid);
        setResponsePage(new PageWorkItem(parameters, this));
    }

    private void approveOrRejectWorkItemsPerformed(AjaxRequestTarget target, boolean approve) {
        List<WorkItemDto> workItemDtoList = getSelectedAssignedItems();
        if (!isSomeItemSelected(workItemDtoList, target)) {
            return;
        }

        OperationResult mainResult = new OperationResult(OPERATION_APPROVE_OR_REJECT_ITEMS);
        WorkflowManager workflowManagerImpl = getWorkflowManager();
        for (WorkItemDto workItemDto : workItemDtoList) {
            OperationResult result = mainResult.createSubresult(OPERATION_APPROVE_OR_REJECT_ITEM);
            try {
                //wfDataAccessor.approveOrRejectWorkItem(workItemDto.getWorkItem().getTaskId(), WorkItemDtoProvider.currentUser(), approve, result);
                workflowManagerImpl.approveOrRejectWorkItem(workItemDto.getWorkItem().getWorkItemId(), approve, result);
                result.computeStatus();
            } catch (Exception e) {
                result.recordPartialError("Couldn't approve/reject work item due to an unexpected exception.", e);
            }
        }
        if (mainResult.isUnknown()) {
            mainResult.recomputeStatus();
        }

        if (mainResult.isSuccess()) {
            mainResult.recordStatus(OperationResultStatus.SUCCESS, "The work item(s) have been successfully " + (approve ? "approved." : "rejected."));
        }

        showResult(mainResult);

        //refresh feedback and table
        target.add(getFeedbackPanel());
//        target.add(getUnassignedItemTable());
        target.add(getAssignedItemTable());
    }

//    private void claimWorkItemsPerformed(AjaxRequestTarget target) {
//        List<WorkItemDto> workItemDtoList = getSelectedUnassignedItems();
//        if (!isSomeItemSelected(workItemDtoList, target)) {
//            return;
//        }
//
//        OperationResult mainResult = new OperationResult(OPERATION_CLAIM_ITEMS);
//        WfDataAccessor wfDataAccessor = getWorkflowManager();
//        for (WorkItemDto workItemDto : workItemDtoList) {
//            OperationResult result = mainResult.createSubresult(OPERATION_CLAIM_ITEM);
//            try {
//                wfDataAccessor.claimWorkItem(workItemDto.getWorkItem(), WorkItemDtoProvider.currentUser(), result);
//            } catch (Exception e) {
//                result.recordPartialError("Couldn't claim work item due to an unexpected exception.", e);
//            }
//        }
//        if (mainResult.isUnknown()) {
//            mainResult.recomputeStatus();
//        }
//
//        if (mainResult.isSuccess()) {
//            mainResult.recordStatus(OperationResultStatus.SUCCESS, "The work item(s) have been successfully claimed.");
//        }
//
//        showResult(mainResult);
//
//        //refresh feedback and table
//        target.add(getFeedbackPanel());
//        target.add(getUnassignedItemTable());
//        target.add(getAssignedItemTable());
//    }
//
//    private void releaseWorkItemsPerformed(AjaxRequestTarget target) {
//        List<WorkItemDto> workItemDtoList = getSelectedAssignedItems();
//        if (!isSomeItemSelected(workItemDtoList, target)) {
//            return;
//        }
//
//        OperationResult mainResult = new OperationResult(OPERATION_RELEASE_ITEMS);
//        WfDataAccessor wfDataAccessor = getWorkflowManager();
//        for (WorkItemDto workItemDto : workItemDtoList) {
//            OperationResult result = mainResult.createSubresult(OPERATION_RELEASE_ITEM);
//            try {
//                wfDataAccessor.releaseWorkItem(workItemDto.getWorkItem(), result);
//            } catch (Exception e) {
//                result.recordPartialError("Couldn't release work item due to an unexpected exception.", e);
//            }
//        }
//        if (mainResult.isUnknown()) {
//            mainResult.recomputeStatus();
//        }
//
//        if (mainResult.isSuccess()) {
//            mainResult.recordStatus(OperationResultStatus.SUCCESS, "The work item(s) have been successfully released.");
//        }
//
//        showResult(mainResult);
//
//        //refresh feedback and table
//        target.add(getFeedbackPanel());
//        target.add(getUnassignedItemTable());
//        target.add(getAssignedItemTable());
//    }

}
