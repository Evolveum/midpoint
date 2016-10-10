package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
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


        final DatePanel from = new DatePanel(ID_FROM,
                new IModel<XMLGregorianCalendar>() {
                    @Override
                    public XMLGregorianCalendar getObject() {
                        return null;
                    }

                    @Override
                    public void setObject(XMLGregorianCalendar date) {

                    }

                    @Override
                    public void detach() {

                    }
                });
        from.setOutputMarkupId(true);
        parametersPanel.add(from);
    }

    private void initTable(Form mainForm){
        AuditEventRecordProvider provider = new AuditEventRecordProvider(PageAuditLogViewer.this);
        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider,
                initColumns(),
                UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER,
                (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER)) {

        };
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }


    private List<IColumn<SelectableBean<AuditEventRecordType>, String>> initColumns() {
        List<IColumn<SelectableBean<AuditEventRecordType>, String>> columns = new ArrayList<>();

        IColumn column;
        column = new PropertyColumn(
                createStringResource("PageAuditLogViewer.column.time"), "timestamp");
        columns.add(column);

        //TODO add columns

        return columns;
    }

}
