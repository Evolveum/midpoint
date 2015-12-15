package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceController;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceState;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon;
import com.evolveum.midpoint.web.page.admin.workflow.PageProcessInstance;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Kate on 03.10.2015.
 */
public class MyRequestsPanel extends SimplePanel<List<ProcessInstanceDto>> {
    private static final String ID_REQUESTS_TABLE = "requestsTable";

    public MyRequestsPanel(String id) {
        super(id, null);
    }

    public MyRequestsPanel(String id, IModel<List<ProcessInstanceDto>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        List<IColumn<ProcessInstanceDto, String>> columns = new ArrayList<IColumn<ProcessInstanceDto, String>>();
        if (WebMiscUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_WORK_ITEMS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_WORK_ITEMS_PROCESS_INSTANCE_URL)) {
            columns.add(new LinkColumn<ProcessInstanceDto>(createStringResource("MyRequestsPanel.name"), "name") {

                @Override
                public void onClick(AjaxRequestTarget target, IModel<ProcessInstanceDto> rowModel) {
                    ProcessInstanceDto piDto = rowModel.getObject();
                    itemDetailsPerformed(target, false, piDto.getProcessInstance().getProcessInstanceId());
                }
            });
        } else {
            columns.add(new AbstractColumn<ProcessInstanceDto, String>(createStringResource("MyRequestsPanel.name")) {
                @Override
                public void populateItem(Item<ICellPopulator<ProcessInstanceDto>> item, String componentId,
                                         final IModel<ProcessInstanceDto> rowModel) {
                    item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                        @Override
                        public Object getObject() {
                            ProcessInstanceDto pi = rowModel.getObject();
                            return pi.getName();
                        }
                    }));
                }
            });
        }
        columns.add(new IconColumn<ProcessInstanceDto>(createStringResource("pageProcessInstances.item.result")) {

            @Override
            protected IModel<String> createIconModel(final IModel<ProcessInstanceDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        ProcessInstanceDto dto = rowModel.getObject();
                        Boolean result = ApprovalUtils.approvalBooleanValue(dto.getAnswer());
                        if (result == null) {
                            return OperationResultStatusIcon
                                    .parseOperationalResultStatus(OperationResultStatusType.IN_PROGRESS).getIcon();
                        } else {
                            return result ?
                                    OperationResultStatusIcon
                                            .parseOperationalResultStatus(OperationResultStatusType.SUCCESS).getIcon()
                                    : OperationResultStatusIcon
                                    .parseOperationalResultStatus(OperationResultStatusType.FATAL_ERROR).getIcon();
                        }
                    }
                };
            }

            @Override
            protected IModel<String> createTitleModel(final IModel<ProcessInstanceDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        ProcessInstanceDto dto = rowModel.getObject();
                        Boolean result = ApprovalUtils.approvalBooleanValue(dto.getAnswer());
                        if (result == null) {
                            return MyRequestsPanel.this.getString(OperationResultStatus.class.getSimpleName() + "." +
                                    OperationResultStatus.IN_PROGRESS);
                        } else {
                            return result ?
                                    createStringResource("MyRequestsPanel.approved").getString()
                                    : createStringResource("MyRequestsPanel.rejected").getString();
                        }

                    }
                };
            }

        });

        columns.add(new AbstractColumn<ProcessInstanceDto, String>(createStringResource("MyRequestsPanel.started")) {

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

        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        TablePanel accountsTable = new TablePanel<ProcessInstanceDto>(ID_REQUESTS_TABLE, provider, columns);
        add(accountsTable);
    }

    private void itemDetailsPerformed(AjaxRequestTarget target, boolean finished, String pid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, pid);
        parameters.add(PageProcessInstance.PARAM_PROCESS_INSTANCE_FINISHED, finished);
        setResponsePage(new PageProcessInstance(parameters, this.getPageBase()));
    }
}
