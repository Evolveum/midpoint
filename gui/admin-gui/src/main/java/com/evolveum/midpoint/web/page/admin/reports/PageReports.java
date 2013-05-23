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

package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ajaxDownload.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.data.RepositoryObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugObjectItem;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditPopup;
import com.evolveum.midpoint.web.page.admin.reports.component.ReconciliationPopup;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.UserFilterDto;
import com.evolveum.midpoint.web.page.admin.users.component.ResourcesPopup;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import groovy.model.PropertyModel;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.query.JRHibernateQueryExecuterFactory;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.servlet.ServletContext;
import javax.xml.namespace.QName;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mserbak
 */
public class PageReports extends PageAdminReports {

    private static final Trace LOGGER = TraceManager.getTrace(PageReports.class);

    private static final ArrayList<ReportDto> REPORTS = new ArrayList<ReportDto>();

    static {
        REPORTS.add(new ReportDto("PageReports.report.auditName", "PageReports.report.auditDescription"));
        REPORTS.add(new ReportDto("PageReports.report.reconciliationName", "PageReports.report.reconciliationDescription"));
        REPORTS.add(new ReportDto("PageReports.report.usersName", "PageReports.report.usersDescription"));
    }

    private static final String DOT_CLASS = PageReports.class.getName() + ".";
    private static final String OPERATION_CREATE_RESOURCE_LIST = DOT_CLASS + "createResourceList";
    private static final String OPERATION_SEARCH_OBJECT = DOT_CLASS + "getObjects";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_AUDIT_POPUP = "auditPopup";
    private static final String ID_RECONCILIATION_POPUP = "reconciliationPopup";
    private static final String ID_REPORTS_TABLE = "reportsTable";

    private AjaxDownloadBehaviorFromStream ajaxDownloadBehavior;

    @SpringBean(name = "sessionFactory")
    private SessionFactory sessionFactory;

    public PageReports() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream(true) {

            @Override
            protected byte[] initStream() {
                return createReport();
            }
        };
        ajaxDownloadBehavior.setContentType("application/pdf; charset=UTF-8");
        mainForm.add(ajaxDownloadBehavior);

//        ajaxDownloadBehavior.initiate(target); in button onClick

        TablePanel table = new TablePanel<ReportDto>(ID_REPORTS_TABLE,
                new ListDataProvider<ReportDto>(this, new Model(REPORTS)), initColumns());
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        mainForm.add(table);


        ModalWindow auditPopup = createModalWindow(ID_AUDIT_POPUP,
                createStringResource("PageReports.title.auditPopup"), 400, 300);
        auditPopup.setContent(new AuditPopup(auditPopup.getContentId()) {

            @Override
            protected void onRunPerformed(AjaxRequestTarget target) {
                //todo implement
            }
        });
        mainForm.add(auditPopup);

        ModalWindow reconciliationPopup = createModalWindow(ID_RECONCILIATION_POPUP,
                createStringResource("PageReports.title.reconciliationPopup"), 400, 300);
        reconciliationPopup.setContent(new ReconciliationPopup(reconciliationPopup.getContentId()) {

            @Override
            protected void onRunPerformed(AjaxRequestTarget target) {
                //todo implement
            }
        });
        mainForm.add(reconciliationPopup);
    }

    private List<IColumn<ReportDto, String>> initColumns() {
        List<IColumn<ReportDto, String>> columns = new ArrayList<IColumn<ReportDto, String>>();

        IColumn column = new LinkColumn<ReportDto>(createStringResource("PageReports.table.name")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ReportDto> rowModel) {
                reportClickPerformed(target, rowModel.getObject());
            }

            @Override
            protected IModel<String> createLinkModel(IModel<ReportDto> rowModel) {
                ReportDto dto = rowModel.getObject();

                return createStringResource(dto.getName());
            }
        };
        columns.add(column);

        column = new PropertyColumn<ReportDto, String>(createStringResource("PageReports.table.description"), null) {

            @Override
            public IModel<Object> getDataModel(IModel<ReportDto> rowModel) {
                ReportDto dto = rowModel.getObject();

                return (IModel) createStringResource(dto.getDescription());
            }
        };
        columns.add(column);

        return columns;
    }

    private void showModalWindow(String id, AjaxRequestTarget target) {
        ModalWindow window = (ModalWindow) get(id);
        window.show(target);
    }

    private void reportClickPerformed(AjaxRequestTarget target, ReportDto report) {
        //todo implement
    }

    private byte[] createReport() {
        // User Report
        // return createUserListReport();

        // Audit Report
        // Timestamp dateTo = new Timestamp(new Date().getTime());
        // Long time = (dateTo.getTime() - 3 * 3600000);
        // Timestamp dateFrom = new Timestamp(time);
        // return createAuditLogReport(dateFrom, dateTo);

        // Reconciliation Report
        QName objectClass = new QName("http://midpoint.evolveum.com/xml/ns/public/resource/instance-2", "AccountObjectClass");
        return createReconciliationReport("some oid", objectClass, "default");
    }

    private byte[] createAuditLogReport(Timestamp dateFrom, Timestamp dateTo) {
        Map params = new HashMap();
        params.put("DATE_FROM", dateFrom);
        params.put("DATE_TO", dateTo);

        return createReport("/reports/reportAuditLogs.jrxml", params);
    }

    private byte[] createReconciliationReport(String resourceOid, QName objectClass, String intent) {
        Map params = new HashMap();

        params.put("RESOURCE_OID", resourceOid);
        params.put("CLASS", objectClass);
        params.put("INTENT", intent);

        return createReport("/reports/reportReconciliation.jrxml", params);
    }

    private byte[] createUserListReport() {
        return createReport("/reports/reportUserList.jrxml", new HashMap());
    }

    protected byte[] createReport(String jrxmlPath, Map params) {
        ServletContext servletContext = getMidpointApplication().getServletContext();
        params.put("LOGO_PATH", servletContext.getRealPath("/reports/logo.jpg"));

        byte[] generatedReport = null;
        Session session = null;
        try {
            // Loading template
            JasperDesign design = JRXmlLoader.load(servletContext.getRealPath(jrxmlPath));
            JasperReport report = JasperCompileManager.compileReport(design);

            session = sessionFactory.openSession();
            session.beginTransaction();

            params.put(JRHibernateQueryExecuterFactory.PARAMETER_HIBERNATE_SESSION, session);
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params);
            generatedReport = JasperExportManager.exportReportToPdf(jasperPrint);

            session.getTransaction().commit();
        } catch (JRException ex) {
            session.getTransaction().rollback();

            getSession().error(getString("pageReports.message.jasperError") + " " + ex.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create jasper report.", ex);
            throw new RestartResponseException(PageReports.class);
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return generatedReport;
    }
}
