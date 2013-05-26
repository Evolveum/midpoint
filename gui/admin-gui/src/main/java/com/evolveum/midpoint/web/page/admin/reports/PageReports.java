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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ajaxDownload.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.internal.dto.ResourceItemDto;
import com.evolveum.midpoint.web.page.admin.reports.component.AuditPopupPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReconciliationPopupPanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditReportDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReconciliationReportDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.query.JRHibernateQueryExecuterFactory;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.servlet.ServletContext;
import javax.xml.namespace.QName;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author lazyman
 */
public class PageReports extends PageAdminReports {

    private static final Trace LOGGER = TraceManager.getTrace(PageReports.class);

    private static final ArrayList<ReportDto> REPORTS = new ArrayList<ReportDto>();

    static {
        REPORTS.add(new ReportDto(ReportDto.Type.AUDIT, "PageReports.report.auditName",
                "PageReports.report.auditDescription"));
        REPORTS.add(new ReportDto(ReportDto.Type.RECONCILIATION, "PageReports.report.reconciliationName",
                "PageReports.report.reconciliationDescription"));
        REPORTS.add(new ReportDto(ReportDto.Type.USERS, "PageReports.report.usersName",
                "PageReports.report.usersDescription"));
    }

    private static final String DOT_CLASS = PageReports.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "loadResources";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_AUDIT_POPUP = "auditPopup";
    private static final String ID_RECONCILIATION_POPUP = "reconciliationPopup";
    private static final String ID_REPORTS_TABLE = "reportsTable";

    private final IModel<List<ResourceItemDto>> resources;
    private final IModel reportParamsModel = new Model();

    @SpringBean(name = "sessionFactory")
    private SessionFactory sessionFactory;

    public PageReports() {
        resources = new LoadableModel<List<ResourceItemDto>>(false) {

            @Override
            protected List<ResourceItemDto> load() {
                return loadResources();
            }
        };

        initLayout();
    }

    private List<ResourceItemDto> loadResources() {
        List<ResourceItemDto> resources = new ArrayList<ResourceItemDto>();

        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
        try {
            List<PrismObject<ResourceType>> objects = getModelService().searchObjects(ResourceType.class, null, null,
                    createSimpleTask(OPERATION_LOAD_RESOURCES), result);

            if (objects != null) {
                for (PrismObject<ResourceType> object : objects) {
                    resources.add(new ResourceItemDto(object.getOid(), WebMiscUtil.getName(object)));
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load resources", ex);
            result.recordFatalError("Couldn't load resources, reason: " + ex.getMessage(), ex);
        } finally {
            if (result.isUnknown()) {
                result.recomputeStatus();
            }
        }

        Collections.sort(resources);

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            throw new RestartResponseException(PageDashboard.class);
        }

        return resources;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream(true) {

            @Override
            protected byte[] initStream() {
                return createReport();
            }
        };
        ajaxDownloadBehavior.setContentType("application/pdf; charset=UTF-8");
        mainForm.add(ajaxDownloadBehavior);

        TablePanel table = new TablePanel<ReportDto>(ID_REPORTS_TABLE,
                new ListDataProvider<ReportDto>(this, new Model(REPORTS)), initColumns(ajaxDownloadBehavior));
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        ModalWindow auditPopup = createModalWindow(ID_AUDIT_POPUP,
                createStringResource("PageReports.title.auditPopup"), 570, 350);
        auditPopup.setContent(new AuditPopupPanel(auditPopup.getContentId(), reportParamsModel) {

            @Override
            protected void onRunPerformed(AjaxRequestTarget target) {
                ajaxDownloadBehavior.initiate(target);

                ModalWindow window = (ModalWindow) PageReports.this.get(ID_AUDIT_POPUP);
                window.close(target);
            }
        });
        add(auditPopup);

        ModalWindow reconciliationPopup = createModalWindow(ID_RECONCILIATION_POPUP,
                createStringResource("PageReports.title.reconciliationPopup"), 570, 350);
        reconciliationPopup.setContent(new ReconciliationPopupPanel(reconciliationPopup.getContentId(),
                reportParamsModel, resources) {

            @Override
            protected void onRunPerformed(AjaxRequestTarget target) {
                ajaxDownloadBehavior.initiate(target);

                ModalWindow window = (ModalWindow) PageReports.this.get(ID_RECONCILIATION_POPUP);
                window.close(target);
            }
        });
        add(reconciliationPopup);
    }

    private List<IColumn<ReportDto, String>> initColumns(final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior) {
        List<IColumn<ReportDto, String>> columns = new ArrayList<IColumn<ReportDto, String>>();

        IColumn column = new LinkColumn<ReportDto>(createStringResource("PageReports.table.name")) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ReportDto> rowModel) {
                reportClickPerformed(target, rowModel.getObject(), ajaxDownloadBehavior);
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

    private void reportClickPerformed(AjaxRequestTarget target, ReportDto report,
                                      AjaxDownloadBehaviorFromStream ajaxDownloadBehavior) {
        switch (report.getType()) {
            case AUDIT:
                if (!(reportParamsModel.getObject() instanceof AuditReportDto)) {
                    reportParamsModel.setObject(new AuditReportDto());
                }
                showModalWindow(ID_AUDIT_POPUP, target);
                break;
            case RECONCILIATION:
                if (!(reportParamsModel.getObject() instanceof ReconciliationReportDto)) {
                    reportParamsModel.setObject(new ReconciliationReportDto());
                }
                showModalWindow(ID_RECONCILIATION_POPUP, target);
                break;
            case USERS:
                reportParamsModel.setObject(null);
                ajaxDownloadBehavior.initiate(target);
                break;
            default:
                error(getString("PageReports.message.unknownReport"));
                target.add(getFeedbackPanel());
        }
    }

    private byte[] createReport() {
        Object object = reportParamsModel.getObject();
        if (object == null) {
            return createUserListReport();
        }

        if (object instanceof AuditReportDto) {
            AuditReportDto dto = (AuditReportDto) object;
            return createAuditLogReport(dto.getDateFrom(), dto.getDateTo());
        } else if (object instanceof ReconciliationReportDto) {
            ReconciliationReportDto dto = (ReconciliationReportDto) object;

            QName objectClass = getObjectClass(dto.getResourceOid());
            return createReconciliationReport(dto.getResourceOid(), objectClass, "default");
        }

        return new byte[]{};
    }

    private QName getObjectClass(String resourceOid) {
        if (StringUtils.isEmpty(resourceOid)) {
            getSession().error(getString("PageReports.message.resourceNotDefined"));
            throw new RestartResponseException(PageReports.class);
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
        try {
            Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
            PrismObject<ResourceType> resource = getModelService().getObject(ResourceType.class,
                    resourceOid, null, task, result);
            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
                    LayerType.PRESENTATION, getPrismContext());

            RefinedObjectClassDefinition def = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
            return def.getTypeName();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get default object class qname for resource oid '"
                    + resourceOid + "'.", ex);
            showResultInSession(result);

            LoggingUtils.logException(LOGGER, "Couldn't get default object class qname for resource oid {}",
                    ex, resourceOid);
            throw new RestartResponseException(PageReports.class);
        }
    }

    private byte[] createAuditLogReport(Timestamp dateFrom, Timestamp dateTo) {
        LOGGER.debug("Creating audit log report from {} to {}.", new Object[]{dateFrom, dateTo});

        Map params = new HashMap();
        params.put("DATE_FROM", dateFrom);
        params.put("DATE_TO", dateTo);

        return createReport("/reports/reportAuditLogs.jrxml", params);
    }

    private byte[] createReconciliationReport(String resourceOid, QName objectClass, String intent) {
        LOGGER.debug("Creating reconciliation report for resource {} with object class {} and intent {}.",
                new Object[]{resourceOid, objectClass, intent});
        Map params = new HashMap();

        params.put("RESOURCE_OID", resourceOid);
        params.put("CLASS", objectClass);
        params.put("INTENT", intent);

        return createReport("/reports/reportReconciliation.jrxml", params);
    }

    private byte[] createUserListReport() {
        LOGGER.debug("Creating user list report.");
        Map params = new HashMap();

        ServletContext servletContext = getMidpointApplication().getServletContext();
        try {
            JasperDesign designRoles = JRXmlLoader.load(servletContext.getRealPath("/reports/reportUserRoles.jrxml"));
            JasperReport reportRoles = JasperCompileManager.compileReport(designRoles);
            params.put("roleReport", reportRoles);

            JasperDesign designOrgs = JRXmlLoader.load(servletContext.getRealPath("/reports/reportUserOrgs.jrxml"));
            JasperReport reportOrgs = JasperCompileManager.compileReport(designOrgs);
            params.put("orgReport", reportOrgs);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't create jasper subreport.", ex);
            throw new RestartResponseException(PageReports.class);
        }

        return createReport("/reports/reportUserList.jrxml", params);
    }

    protected byte[] createReport(String jrxmlPath, Map params) {
        ServletContext servletContext = getMidpointApplication().getServletContext();
        params.put("LOGO_PATH", servletContext.getRealPath("/reports/logo.jpg"));

        byte[] generatedReport = new byte[]{};
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
        } catch (Exception ex) {
            if (session != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }

            getSession().error(getString("PageReports.message.jasperError") + " " + ex.getMessage());
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
