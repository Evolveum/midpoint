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

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditReportDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReconciliationReportDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportSearchDto;
import com.evolveum.midpoint.web.session.ReportsStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.SearchFormEnterBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import javax.servlet.ServletContext;
import javax.xml.namespace.QName;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/reports", action = {
        PageAdminReports.AUTHORIZATION_REPORTS_ALL,
        AuthorizationConstants.NS_AUTHORIZATION + "#reports"})
public class PageReports extends PageAdminReports {

    private static final Trace LOGGER = TraceManager.getTrace(PageReports.class);

    private static final String DOT_CLASS = PageReports.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runReport";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_REPORTS_TABLE = "reportsTable";

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_SUBREPORTS = "subReportCheckbox";
    private static final String ID_BUTTON_SEARCH = "searchButton";
    private static final String ID_BUTTON_CLEAR_SEARCH = "searchClear";

    private final IModel reportParamsModel = new Model();
    private IModel<ReportSearchDto> searchModel;

    @SpringBean
    private transient ReportManager reportManager;

    public PageReports() {
        searchModel = new LoadableModel<ReportSearchDto>() {

            @Override
            protected ReportSearchDto load() {
                ReportsStorage storage = getSessionStorage().getReports();
                ReportSearchDto dto = storage.getReportSearch();

                if(dto == null){
                    dto = new ReportSearchDto();
                }

                return dto;
            }
        };

        initLayout();
    }

    @Override
    protected IModel<String> createPageSubTitleModel(){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("page.subTitle").getString();
            }
        };
    }
   
    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);
        initSearchForm(searchForm);


        //TablePanel table = new TablePanel<ReportDto>(ID_REPORTS_TABLE,
        //        new ListDataProvider<ReportDto>(this, new Model(REPORTS)), initColumns(ajaxDownloadBehavior));
        TablePanel table = new TablePanel<>(ID_REPORTS_TABLE, new ObjectDataProvider(PageReports.this, ReportType.class),
                initColumns());
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private void initSearchForm(Form<?> form){

        final AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_BUTTON_SEARCH,
                createStringResource("PageBase.button.search")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                searchPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(getFeedbackPanel());
            }
        };
        form.add(searchButton);

        final TextField searchText = new TextField(ID_SEARCH_TEXT, new PropertyModel<String>(searchModel, ReportSearchDto.F_SEARCH_TEXT));
        searchText.add(new SearchFormEnterBehavior(searchButton));
        form.add(searchText);

        CheckBox showSubreports = new CheckBox(ID_SUBREPORTS,
                new PropertyModel(searchModel, ReportSearchDto.F_PARENT));
        showSubreports.add(createFilterAjaxBehaviour());
        form.add(showSubreports);

        AjaxSubmitButton clearButton = new AjaxSubmitButton(ID_BUTTON_CLEAR_SEARCH) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                clearSearchPerformed(target);
            }
        };
        form.add(clearButton);
    }

    private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
        return new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                searchPerformed(target);
            }
        };
    }

    private List<IColumn<ReportType, String>> initColumns() {
        List<IColumn<ReportType, String>> columns = new ArrayList<IColumn<ReportType, String>>();

        IColumn column;
        column = new LinkColumn<SelectableBean<ReportType>>(createStringResource("PageReports.table.name"),
                ReportType.F_NAME.getLocalPart(), "value.name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> rowModel){
                ReportType report = rowModel.getObject().getValue();
                reportTypeFilterPerformed(target, report.getOid());
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageReports.table.description"), "value.description");
        columns.add(column);

        column = new DoubleButtonColumn<SelectableBean<ReportType>>(new Model(), null){

            @Override
            public String getFirstCap(){
                return PageReports.this.createStringResource("PageReports.button.run").getString();
            }

            @Override
            public String getSecondCap(){
                return PageReports.this.createStringResource("PageReports.button.configure").getString();
            }

            @Override
            public String getFirstColorCssClass(){
                return BUTTON_COLOR_CLASS.PRIMARY.toString();
            }

            @Override
            public void firstClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> model){
                runReportPerformed(target, model.getObject().getValue());
            }

            @Override
            public void secondClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> model){
                configurePerformed(target, model.getObject().getValue());
            }
        };
        columns.add(column);

        return columns;
    }

    private void reportTypeFilterPerformed(AjaxRequestTarget target, String oid){
        //TODO - navigate to CreatedReportsPage and set report type filter.
    }

    private void runReportPerformed(AjaxRequestTarget target, ReportType report){
        LOGGER.debug("Run report performed for {}", new Object[]{report.asPrismObject()});

        OperationResult result = new OperationResult(OPERATION_RUN_REPORT);
        try {
            Task task = createSimpleTask(OPERATION_RUN_REPORT);
            reportManager.runReport(report.asPrismObject(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void configurePerformed(AjaxRequestTarget target, ReportType report){
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, report.getOid());
        setResponsePage(PageReport.class, params);
    }

    private byte[] createReport() {
        Object object = reportParamsModel.getObject();
        if (object == null) {
            return createUserListReport();
        }

        if (object instanceof AuditReportDto) {
            AuditReportDto dto = (AuditReportDto) object;
            return createAuditLogReport(dto.getDateFrom(), dto.getDateTo(), dto.getAuditEventType());
        } else if (object instanceof ReconciliationReportDto) {
            ReconciliationReportDto dto = (ReconciliationReportDto) object;

            QName objectClass = getObjectClass(dto.getResourceOid());
            return createReconciliationReport(dto.getResourceOid(), dto.getResourceName(), objectClass, "default");
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

    private byte[] createAuditLogReport(Timestamp dateFrom, Timestamp dateTo,  AuditEventType auditEventType) {
        LOGGER.debug("Creating audit log report from {} to {}, type {}.", new Object[]{dateFrom, dateTo, auditEventType != null ? auditEventType.name() : ""});

        int auditEventTypeId = auditEventType != null ? auditEventType.ordinal() : -1;

        String auditEventTypeName = auditEventType == null ? getString("AuditEventType.null") :
                WebMiscUtil.createLocalizedModelForEnum(auditEventType, this).getObject();

        Map params = new HashMap();
        params.put("DATE_FROM", dateFrom);
        params.put("DATE_TO", dateTo);
        params.put("EVENT_TYPE", auditEventTypeId);
        params.put("EVENT_TYPE_DESC", auditEventTypeName);
        //String theQuery = auditEventTypeId != -1 ? "select aer.timestamp as timestamp, aer.initiatorName as initiator, aer.eventType as eventType, aer.eventStage as eventStage, aer.targetName as targetName, aer.targetType as targetType, aer.targetOwnerName as targetOwnerName, aer.outcome as outcome, aer.message as message from RAuditEventRecord as aer where aer.eventType = $P{EVENT_TYPE} and aer.timestamp >= $P{DATE_FROM} and aer.timestamp <= $P{DATE_TO} order by aer.timestamp" : "select aer.timestamp as timestamp, aer.initiatorName as initiator, aer.eventType as eventType, aer.eventStage as eventStage, aer.targetName as targetName, aer.targetType as targetType, aer.targetOwnerName as targetOwnerName, aer.outcome as outcome, aer.message as message from RAuditEventRecord as aer where aer.timestamp >= $P{DATE_FROM} and aer.timestamp <= $P{DATE_TO} order by aer.timestamp";;
        String theQuery = auditEventTypeId != -1 ? 
	    		"select aer.timestamp as timestamp, " +
	        	"aer.initiatorName as initiator, " +
	        	"aer.eventType as eventType, " +
	        	"aer.eventStage as eventStage, " +
	        	"aer.targetName as targetName, " +
	        	"aer.targetType as targetType, " +
	        	"aer.targetOwnerName as targetOwnerName, " +
	        	"aer.outcome as outcome, " +
	        	"aer.message as message, " +
	        	"odo.delta as delta " +
	        	"from RObjectDeltaOperation as odo " +
	        	"join odo.record as aer " +
	        	"where aer.eventType = $P{EVENT_TYPE} and aer.timestamp >= $P{DATE_FROM} and aer.timestamp <= $P{DATE_TO} " +
	        	"order by aer.timestamp" 
	        	: 
	        	"select aer.timestamp as timestamp, " +
	        	"aer.initiatorName as initiator, " +
	        	"aer.eventType as eventType, " +
	        	"aer.eventStage as eventStage, " +
	        	"aer.targetName as targetName, " +
	        	"aer.targetType as targetType, " +
	        	"aer.targetOwnerName as targetOwnerName, " +
	        	"aer.outcome as outcome, " +
	        	"aer.message as message, " +
	        	"odo.delta as delta " +
	        	"from RObjectDeltaOperation as odo " +
	        	"join odo.record as aer " +
	        	"where aer.timestamp >= $P{DATE_FROM} and aer.timestamp <= $P{DATE_TO} " +
	        	"order by aer.timestamp";
        params.put("QUERY_STRING", theQuery);        
        return createReport("/reports/reportAuditLogs.jrxml", params);
    }

    private byte[] createReconciliationReport(String resourceOid, String resourceName, QName objectClass,
                                              String intent) {
        LOGGER.debug("Creating reconciliation report for resource {} with object class {} and intent {}.",
                new Object[]{resourceOid, objectClass, intent});
        Map params = new HashMap();

        params.put("RESOURCE_OID", resourceOid);
        params.put("RESOURCE_NAME", resourceName);
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
            
            JasperDesign designAccounts = JRXmlLoader.load(servletContext.getRealPath("/reports/reportUserAccounts.jrxml"));
            JasperReport reportAccounts = JasperCompileManager.compileReport(designAccounts);
            params.put("accountReport", reportAccounts);
            
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't create jasper subreport.", ex);
            throw new RestartResponseException(PageReports.class);
        }

        return createReport("/reports/reportUserList.jrxml", params);
    }

    protected byte[] createReport(String jrxmlPath, Map params) {
        ServletContext servletContext = getMidpointApplication().getServletContext();
        params.put("LOGO_PATH", servletContext.getRealPath("/reports/logo.jpg"));
        params.put("BaseTemplateStyles", servletContext.getRealPath("/styles/midpoint_base_styles.jrtx"));
        
        byte[] generatedReport = new byte[]{};
//        Session session = null;
//        try {
//            // Loading template
//            JasperDesign design = JRXmlLoader.load(servletContext.getRealPath(jrxmlPath));
//            JasperReport report = JasperCompileManager.compileReport(design);
//
//            session = sessionFactory.openSession();
//            session.beginTransaction();
//
//            params.put(JRHibernateQueryExecuterFactory.PARAMETER_HIBERNATE_SESSION, session);
//            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params);
//            generatedReport = JasperExportManager.exportReportToPdf(jasperPrint);
//
//            session.getTransaction().commit();
//        } catch (Exception ex) {
//            if (session != null && session.getTransaction().isActive()) {
//                session.getTransaction().rollback();
//            }
//
//            getSession().error(getString("PageReports.message.jasperError") + " " + ex.getMessage());
//            LoggingUtils.logException(LOGGER, "Couldn't create jasper report.", ex);
//            throw new RestartResponseException(PageReports.class);
//        } finally {
//            if (session != null) {
//                session.close();
//            }
//        }

        return generatedReport;
    }

    private ObjectDataProvider getDataProvider(){
        DataTable table = getReportTable().getDataTable();
        return (ObjectDataProvider) table.getDataProvider();
    }

    private TablePanel getReportTable(){
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_REPORTS_TABLE));
    }

    private void searchPerformed(AjaxRequestTarget target){
        ObjectQuery query = createQuery(target);
        ObjectDataProvider provider = getDataProvider();
        provider.setQuery(query);

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportSearch(searchModel.getObject());

        TablePanel table = getReportTable();
        target.add(table);
        target.add(getFeedbackPanel());
    }

    private ObjectQuery createQuery(AjaxRequestTarget target){
        ReportSearchDto dto = searchModel.getObject();
        String text = dto.getText();
        Boolean parent = !dto.isParent();
        ObjectQuery query = new ObjectQuery();

        if(!StringUtils.isEmpty(text)){
//            try{
                PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
                String normalizedText = normalizer.normalize(text);

                ObjectFilter substring = SubstringFilter.createSubstring(ReportType.F_NAME, ReportType.class,
                        getPrismContext(), PolyStringNormMatchingRule.NAME, normalizedText);

                if(parent == true){
                    EqualsFilter boolFilter = EqualsFilter.createEqual(ReportType.F_PARENT, ReportType.class,
                            getPrismContext(), null, parent);

//                    EqualsFilter nullFilter = EqualsFilter.createEqual(ReportType.F_PARENT, ReportType.class,
//                            getPrismContext(), null, null);

//                    OrFilter or = OrFilter.createOr(boolFilter, nullFilter);

                    query.setFilter(AndFilter.createAnd(substring, boolFilter));
                } else {
                    query.setFilter(substring);
                }

//            }catch (Exception e){
//                LoggingUtils.logException(LOGGER, "Couldn't create query filter", e);
//                error(getString("PageReports.message.queryError", e.getMessage()));
//                target.add(getFeedbackPanel());
//            }
        } else{
            if(parent == true){
                EqualsFilter boolFilter = EqualsFilter.createEqual(ReportType.F_PARENT, ReportType.class,
                        getPrismContext(), null, parent);

//                EqualsFilter nullFilter = EqualsFilter.createEqual(ReportType.F_PARENT, ReportType.class,
//                        getPrismContext(), null, null);

//                query.setFilter(OrFilter.createOr(boolFilter, nullFilter));
                query.setFilter(boolFilter);
            } else{
                query = null;
            }
        }

        return query;
    }


    private void clearSearchPerformed(AjaxRequestTarget target){
        searchModel.setObject(new ReportSearchDto());

        TablePanel panel = getReportTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(null);

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportSearch(searchModel.getObject());
        panel.setCurrentPage(storage.getReportsPaging());

        target.add(get(ID_SEARCH_FORM));
        target.add(panel);
    };
}
