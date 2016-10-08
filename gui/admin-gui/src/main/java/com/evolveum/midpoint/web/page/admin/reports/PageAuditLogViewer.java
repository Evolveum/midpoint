package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskAddDto;
import com.evolveum.midpoint.web.session.ReportsStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/auditLogViewer", action = {
        @AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_AUDIT_LOG_VIEWER_URL,
                label = "PageAuditLogViewer.auth.auditLogViewer.label",
                description = "PageAuditLogViewer.auth.auditLogViewer.description")})
public class PageAuditLogViewer extends PageBase{
    private List<AuditEventRecord> auditEventRecordList;

    Map<String, Object> params = new HashMap<>();

    private static final String ID_PARAMETERS_PANEL = "parametersPanel";
    private static final String ID_TABLE = "table";
    private static final String ID_FROM = "fromField";
    private static final String ID_MAIN_FORM = "mainForm";

    public PageAuditLogViewer(){
        initLayout();
    }
     private void initLayout(){
         Form mainForm = new Form(ID_MAIN_FORM);
         add(mainForm);

         initParametersPanel(mainForm);
         initTable(mainForm);
     }

    private void initParametersPanel(Form mainForm){
        WebMarkupContainer parametersPanel = new WebMarkupContainer(ID_PARAMETERS_PANEL);
        parametersPanel.setOutputMarkupId(true);
        mainForm.add(parametersPanel);


        final DateTimeField from = new DateTimeField(ID_FROM,
                new IModel<Date>() {
                    @Override
                    public Date getObject() {
                        return null;
                    }

                    @Override
                    public void setObject(Date date) {

                    }

                    @Override
                    public void detach() {

                    }
                }) {
            @Override
            protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) {
                return DateTextField.forDatePattern(id, dateFieldModel, "dd/MMM/yyyy"); // todo i18n
            }
        };
        from.setOutputMarkupId(true);
        parametersPanel.add(from);
    }

    private void initTable(Form mainForm){
        IModel<List<AuditEventRecordDto>> model = new IModel<List<AuditEventRecordDto>>() {
            @Override
            public List<AuditEventRecordDto> getObject() {
                return getAuditEventRecordList();
            }

            @Override
            public void setObject(List<AuditEventRecordDto> auditEventRecord) {

            }

            @Override
            public void detach() {

            }
        };
        ListDataProvider provider = new ListDataProvider<AuditEventRecordDto>(PageAuditLogViewer.this, model) {

        };
        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider,
                initColumns(),
                UserProfileStorage.TableId.PAGE_CREATED_REPORTS_PANEL,
                (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_CREATED_REPORTS_PANEL)) {

//            @Override
//            protected WebMarkupContainer createHeader(String headerId) {
//                return new SearchFragment(headerId, ID_TABLE_HEADER, Page.this, searchModel);
//            }
        };
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private List<AuditEventRecordDto> getAuditEventRecordList(){
        List<AuditEventRecord> auditRecords = getAuditService().listRecords("from RAuditEventRecord as aer where 1=1 order by aer.timestamp asc", params);
        if (auditRecords == null){
            auditRecords = new ArrayList<>();
        }
        List<AuditEventRecordDto> auditRecordDtoList = new ArrayList<>();
        for (AuditEventRecord record : auditRecords){
            AuditEventRecordDto dto = new AuditEventRecordDto(record);
            auditRecordDtoList.add(dto);
        }
        return auditRecordDtoList;
    }

    private List<IColumn<SelectableBean<AuditEventRecordDto>, String>> initColumns() {
        List<IColumn<SelectableBean<AuditEventRecordDto>, String>> columns = new ArrayList<>();

        IColumn column;

        column = new AbstractColumn<SelectableBean<AuditEventRecordDto>, String>(
                createStringResource("pageCreatedReports.table.time"),
                "timestamp") {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AuditEventRecordDto>>> cellItem,
                                     String componentId, final IModel<SelectableBean<AuditEventRecordDto>> rowModel) {
                cellItem.add(new DateLabelComponent(componentId, new AbstractReadOnlyModel<Date>() {

                    @Override
                    public Date getObject() {
                        Object object = rowModel.getObject();
                        return new Date(((AuditEventRecordDto)object).getTimestamp());                   }
                }, DateLabelComponent.LONG_MEDIUM_STYLE));
            }
        };
        columns.add(column);

        return columns;
    }

}
