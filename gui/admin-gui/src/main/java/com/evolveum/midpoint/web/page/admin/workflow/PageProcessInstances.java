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

import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDtoProvider;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.util.ApprovalUtils;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
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
 * @author mederly
 */
public abstract class PageProcessInstances extends PageAdminWorkItems {

    public static final String ID_STOP = "stop";
    public static final String ID_BACK = "back";

    boolean requestedBy;        // true if we want to show process instances requested BY a user
    boolean requestedFor;       // true if we want to show instances requested FOR a user

    private static final Trace LOGGER = TraceManager.getTrace(PageProcessInstances.class);
    private static final String DOT_CLASS = PageProcessInstances.class.getName() + ".";
    private static final String OPERATION_STOP_PROCESS_INSTANCES = DOT_CLASS + "stopProcessInstances";


    public PageProcessInstances(boolean requestedBy, boolean requestedFor) {
        this.requestedBy = requestedBy;
        this.requestedFor = requestedFor;
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        List<IColumn<ProcessInstanceDto, String>> columns = initColumns();
        TablePanel<ProcessInstanceDto> table = new TablePanel<ProcessInstanceDto>("processInstancesTable", new ProcessInstanceDtoProvider(PageProcessInstances.this, requestedBy, requestedFor, false),
                columns);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        List<IColumn<ProcessInstanceDto, String>> finishedColumns = initFinishedColumns();
        TablePanel<ProcessInstanceDto> finishedTable = new TablePanel<ProcessInstanceDto>("finishedProcessInstancesTable", new ProcessInstanceDtoProvider(PageProcessInstances.this, requestedBy, requestedFor, true),
                finishedColumns);
        finishedTable.setOutputMarkupId(true);
        mainForm.add(finishedTable);

        initItemButtons(mainForm);
    }

    private List<IColumn<ProcessInstanceDto, String>> initColumns() {
        List<IColumn<ProcessInstanceDto, String>> columns = new ArrayList<IColumn<ProcessInstanceDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<ProcessInstanceDto>();
        columns.add(column);

        column = new LinkColumn<ProcessInstanceDto>(createStringResource("pageProcessInstances.item.name"), "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ProcessInstanceDto> rowModel) {
                ProcessInstanceDto piDto = rowModel.getObject();
                itemDetailsPerformed(target, false, piDto.getProcessInstance().getProcessInstanceId());
            }
        };
        columns.add(column);

        columns.add(new AbstractColumn<ProcessInstanceDto, String>(createStringResource("pageProcessInstances.item.started")) {

            @Override
            public void populateItem(Item<ICellPopulator<ProcessInstanceDto>> item, String componentId,
                                     final IModel<ProcessInstanceDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        ProcessInstanceDto pi = rowModel.getObject();
                        Date started = XmlTypeConverter.toDate(pi.getProcessInstance().getStartTimestamp());
                        if (started == null) {
                            return "?";
                        } else {
                            // todo i18n
                            return DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - started.getTime(), true, true) + " ago";
                        }
                    }
                }));
            }
        });

        return columns;
    }

    private List<IColumn<ProcessInstanceDto, String>> initFinishedColumns() {
        List<IColumn<ProcessInstanceDto, String>> columns = new ArrayList<IColumn<ProcessInstanceDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<ProcessInstanceDto>();
        columns.add(column);

        column = new LinkColumn<ProcessInstanceDto>(createStringResource("pageProcessInstances.item.name"), "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ProcessInstanceDto> rowModel) {
                ProcessInstanceDto piDto = rowModel.getObject();
                itemDetailsPerformed(target, true, piDto.getProcessInstance().getProcessInstanceId());
            }
        };
        columns.add(column);

        columns.add(new AbstractColumn<ProcessInstanceDto, String>(createStringResource("pageProcessInstances.item.result")) {

            @Override
            public void populateItem(Item<ICellPopulator<ProcessInstanceDto>> item, String componentId,
                                     final IModel<ProcessInstanceDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        ProcessInstanceDto pi = rowModel.getObject();
                        Boolean result = ApprovalUtils.approvalBooleanValue(pi.getAnswer());
                        if (result == null) {
                            return "";
                        } else {
                            return result ? "APPROVED" : "REJECTED";        // todo i18n
                        }
                    }
                }));
            }
        });

        columns.add(new AbstractColumn<ProcessInstanceDto, String>(createStringResource("pageProcessInstances.item.started")) {

            @Override
            public void populateItem(Item<ICellPopulator<ProcessInstanceDto>> item, String componentId,
                                     final IModel<ProcessInstanceDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        ProcessInstanceDto pi = rowModel.getObject();
                        Date started = XmlTypeConverter.toDate(pi.getProcessInstance().getStartTimestamp());
                        if (started == null) {
                            return "?";
                        } else {
                        	return WebMiscUtil.formatDate(started);
                        }
                    }
                }));
            }
        });

        columns.add(new AbstractColumn<ProcessInstanceDto, String>(createStringResource("pageProcessInstances.item.finished")) {

            @Override
            public void populateItem(Item<ICellPopulator<ProcessInstanceDto>> item, String componentId,
                                     final IModel<ProcessInstanceDto> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        ProcessInstanceDto pi = rowModel.getObject();
                        Date finished = XmlTypeConverter.toDate(pi.getProcessInstance().getEndTimestamp());
                        if (finished == null) {
                            return getString("pageProcessInstances.notYet");
                        } else {
                            return WebMiscUtil.formatDate(finished);
                        }
                    }
                }));
            }
        });

        return columns;
    }

    private void initItemButtons(Form mainForm) {

        AjaxButton stop = new AjaxButton(ID_STOP, createStringResource("pageProcessInstances.button.stop")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                stopProcessInstancesPerformed(target);
            }

            @Override
            public boolean isVisible() {
                return !requestedFor;
            }
        };
        mainForm.add(stop);

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("pageProcessInstances.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                goBack(PageDashboard.class);
            }
        };
        mainForm.add(back);

    }

    private TablePanel getTable() {
        return (TablePanel) get("mainForm:processInstancesTable");
    }

    private TablePanel getFinishedTable() {
        return (TablePanel) get("mainForm:finishedProcessInstancesTable");
    }

//    private List<ProcessInstanceDto> getSelectedItems() {
//        DataTable table = getTable().getDataTable();
//        ProcessInstanceDtoProvider provider = (ProcessInstanceDtoProvider) table.getDataProvider();
//
//        List<ProcessInstanceDto> selected = new ArrayList<ProcessInstanceDto>();
//        for (ProcessInstanceDto row : provider.getAvailableData()) {
//            if (row.isSelected()) {
//                selected.add(row);
//            }
//        }
//
//        return selected;
//    }

    private boolean isSomeItemSelected(List<ProcessInstanceDto> instances, List<ProcessInstanceDto> finished, AjaxRequestTarget target) {
        if (!instances.isEmpty() || !finished.isEmpty()) {
            return true;
        }

        warn(getString("pageProcessInstances.message.noItemSelected"));
        target.add(getFeedbackPanel());
        return false;
    }

    private void itemDetailsPerformed(AjaxRequestTarget target, boolean finished, String pid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, pid);
        parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_FINISHED, finished);
        setResponsePage(new PageProcessInstance(parameters, this));
    }

    private void stopProcessInstancesPerformed(AjaxRequestTarget target) {

    	MidPointPrincipal user = SecurityUtils.getPrincipalUser();

        List<ProcessInstanceDto> processInstanceDtoList = WebMiscUtil.getSelectedData(getTable());
        List<ProcessInstanceDto> finishedProcessInstanceDtoList = WebMiscUtil.getSelectedData(getFinishedTable());

        if (!isSomeItemSelected(processInstanceDtoList, finishedProcessInstanceDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_STOP_PROCESS_INSTANCES);

        WorkflowService workflowService = getWorkflowService();
        for (ProcessInstanceDto processInstanceDto : processInstanceDtoList) {
            try {
                workflowService.stopProcessInstance(processInstanceDto.getInstanceId(),
                        WebMiscUtil.getOrigStringFromPoly(user.getName()), result);
            } catch (Exception ex) {    // todo
                result.createSubresult("stopProcessInstance").recordPartialError("Couldn't stop process instance " + processInstanceDto.getName(), ex);
            }
        }
        for (ProcessInstanceDto processInstanceDto : finishedProcessInstanceDtoList) {
            try {
                workflowService.deleteProcessInstance(processInstanceDto.getInstanceId(), result);
            } catch (Exception ex) {    // todo
                result.createSubresult("deleteProcessInstance").recordPartialError("Couldn't delete process instance " + processInstanceDto.getName(), ex);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus();
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "Selected process instance(s) have been successfully stopped and/or deleted.");
        }

        showResult(result);

        ProcessInstanceDtoProvider provider = (ProcessInstanceDtoProvider) getTable().getDataTable().getDataProvider();
        provider.clearCache();
        ProcessInstanceDtoProvider provider2 = (ProcessInstanceDtoProvider) getFinishedTable().getDataTable().getDataProvider();
        provider2.clearCache();

        //refresh feedback and table
        target.add(getFeedbackPanel());
        target.add(getTable());
        target.add(getFinishedTable());

        setReinitializePreviousPages(true);
    }

}
