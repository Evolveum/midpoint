/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JasperReportEngineConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JasperReportTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

public class MidPointLocalQueryExecutor extends MidPointQueryExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointLocalQueryExecutor.class);
    private ReportService reportService;
    private PrismObject<ReportType> report;
    private Task task;
    private OperationResult operationResult;


    public MidPointLocalQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
            Map<String, ? extends JRValueParameter> parametersMap, ReportService reportService){
        super(jasperReportsContext, dataset, parametersMap);
    }

    protected MidPointLocalQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
            Map<String, ? extends JRValueParameter> parametersMap) {
        super(jasperReportsContext, dataset, parametersMap);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating MidPointLocalQueryExecutor, params:\n{}", ReportUtils.dumpParams(parametersMap, 1));
        }

        //JRFillParameter fillparam = (JRFillParameter) parametersMap.get(JRParameter.REPORT_PARAMETERS_MAP);
        //Map reportParams = (Map) fillparam.getValue();
        reportService = getParameterValue(parametersMap, ReportService.PARAMETER_REPORT_SERVICE);
        report = getParameterValue(parametersMap, ReportTypeUtil.PARAMETER_REPORT_OBJECT);
        task = getParameterValue(parametersMap, ReportTypeUtil.PARAMETER_TASK);

        // The PARAMETER_OPERATION_RESULT will not make it here. It is properly set in the task, but it won't arrive here.
        // No idea why.
//        operationResult = getParameterValue(parametersMap, ReportCreateTaskHandler.PARAMETER_OPERATION_RESULT);
        operationResult = task.getResult(); // WORKAROUND

        parseQuery();
    }

    private <T> T getParameterValue(Map<String, ? extends JRValueParameter> parametersMap, String name) {
        JRValueParameter jrValueParameter = parametersMap.get(name);
        if (jrValueParameter == null) {
            throw new IllegalArgumentException("No parameter '"+name+"' in JasperReport parameters");
        }
        return (T) jrValueParameter.getValue();
    }

    @Override
    protected <T> TypedValue<T> createTypedPropertyValue(T realValue, Class<T> valueClass) {
        PrismPropertyValue<T> pval = reportService.getPrismContext().itemFactory().createPropertyValue(realValue);
        return new TypedValue<>(pval, valueClass);
    }

    @Override
    protected Object getParsedQuery(String query, VariablesMap expressionParameters) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return reportService.parseQuery(report, query, expressionParameters, task, operationResult);
    }

    @Override
    protected <O extends ObjectType> Collection<PrismObject<O>> searchObjects(Object query, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException{
        return reportService.searchObjects((ObjectQuery) query, SelectorOptions.createCollection(GetOperationOptions.createRaw()), task, operationResult);
    }

    @Override
    protected Collection<PrismContainerValue<? extends Containerable>> evaluateScript(String script, VariablesMap parameters)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return reportService.evaluateScript(report, script, getParameters(), task, operationResult);
    }

    @Override
    protected boolean isAuditReport() {
        JasperReportEngineConfigurationType jasperConfig = report.asObjectable().getJasper();
        if (jasperConfig != null) {
            JasperReportTypeType reportType = jasperConfig.getReportType();
            if (reportType != null) {
                return reportType.equals(JasperReportTypeType.AUDIT_SQL);
            }
        }
        // legacy
        return getScript().contains("AuditEventRecord") || getScript().contains("m_audit_event");
    }

    @Override
    protected Collection<AuditEventRecord> searchAuditRecords(String script, VariablesMap parameters) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        return reportService.evaluateAuditScript(report, script, parameters, task, operationResult);
    }

    @Override
    protected JRDataSource createDataSourceFromObjects(Collection<PrismObject<? extends ObjectType>> results) {
        return new MidPointDataSource(toPcvList(results));
    }

    private Collection<PrismContainerValue<? extends Containerable>> toPcvList(Collection<PrismObject<? extends ObjectType>> objects) {
        ArrayList<PrismContainerValue<? extends Containerable>> pcvList = new ArrayList<>(objects.size());
        for (PrismObject object : objects) {
            pcvList.add(object.asObjectable().asPrismContainerValue());
        }
        return pcvList;
    }

    @Override
    protected JRDataSource createDataSourceFromContainerValues(Collection<PrismContainerValue<? extends Containerable>> results) {
        return new MidPointDataSource(results);
    }



}
