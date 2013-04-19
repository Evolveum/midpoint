/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDtoProvider;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.WorkflowService;
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
                itemDetailsPerformed(target, false, piDto.getProcessInstance().getProcessId());
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
                        Date started = pi.getProcessInstance().getStartTime();
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
                itemDetailsPerformed(target, true, piDto.getProcessInstance().getProcessId());
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
                        Boolean result = pi.getAnswer();
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
                        Date started = pi.getProcessInstance().getStartTime();
                        if (started == null) {
                            return "?";
                        } else {
                        	return WebMiscUtil.getFormatedDate(started);
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
                        Date finished = pi.getProcessInstance().getEndTime();
                        if (finished == null) {
                            return getString("pageProcessInstances.notYet");
                        } else {
                            return WebMiscUtil.getFormatedDate(finished);
                        }
                    }
                }));
            }
        });

        return columns;
    }

    private void initItemButtons(Form mainForm) {

        AjaxLinkButton stop = new AjaxLinkButton("stop",
                createStringResource("pageProcessInstances.button.stop")) {

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
        parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_ID, pid);
        parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_FINISHED, finished);
        if (requestedBy) {
            parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_BACK, PageProcessInstance.PARAM_PROCESS_INSTANCE_BACK_REQUESTED_BY);
        } else if (requestedFor) {
            parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_BACK, PageProcessInstance.PARAM_PROCESS_INSTANCE_BACK_REQUESTED_FOR);
        } else {
            parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_BACK, PageProcessInstance.PARAM_PROCESS_INSTANCE_BACK_ALL);
        }

//        System.out.println("Sending page parameters: ");
//        for (PageParameters.NamedPair np : parameters.getAllNamed()) {
//            System.out.println(" - " + np.getKey() + " = " + np.getValue());
//        }
        //setResponsePage(PageProcessInstance.class, parameters);       // does not work
        setResponsePage(new PageProcessInstance(parameters));
    }

    private void stopProcessInstancesPerformed(AjaxRequestTarget target) {

    	MidPointPrincipal user = SecurityUtils.getPrincipalUser();

        List<ProcessInstanceDto> processInstanceDtoList = WebMiscUtil.getSelectedData(getTable());
        List<ProcessInstanceDto> finishedProcessInstanceDtoList = WebMiscUtil.getSelectedData(getFinishedTable());

        if (!isSomeItemSelected(processInstanceDtoList, finishedProcessInstanceDtoList, target)) {
            return;
        }

        OperationResult result = new OperationResult(OPERATION_STOP_PROCESS_INSTANCES);

        WorkflowService workflowServiceImpl = getWorkflowService();
        for (ProcessInstanceDto processInstanceDto : processInstanceDtoList) {
            try {
                workflowServiceImpl.stopProcessInstance(processInstanceDto.getInstanceId(),
                        WebMiscUtil.getOrigStringFromPoly(user.getName()), result);
            } catch (Exception ex) {    // todo
                result.createSubresult("stopProcessInstance").recordPartialError("Couldn't stop process instance " + processInstanceDto.getName(), ex);
            }
        }
        for (ProcessInstanceDto processInstanceDto : finishedProcessInstanceDtoList) {
            try {
                workflowServiceImpl.deleteProcessInstance(processInstanceDto.getInstanceId(), result);
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
    }

}
