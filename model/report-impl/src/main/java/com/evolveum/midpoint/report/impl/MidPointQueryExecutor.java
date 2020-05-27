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

import com.evolveum.midpoint.prism.*;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.query.JRAbstractQueryExecuter;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class MidPointQueryExecutor extends JRAbstractQueryExecuter {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointLocalQueryExecutor.class);

    private Object query;
    private String script;
    private Class type;

    public String getScript() {
        return script;
    }
    public Object getQuery() {
        return query;
    }
    public Class getType() {
        return type;
    }

    protected abstract <T> TypedValue<T> createTypedPropertyValue(T realValue, Class<T> valueClass);

    protected VariablesMap getParameters(){
        JRParameter[] params = dataset.getParameters();
        VariablesMap expressionParameters = new VariablesMap();
        for (JRParameter param : params){
            if (param.isSystemDefined()){
                continue;
            }
            //LOGGER.trace(((JRBaseParameter)param).getName());
            Object v = getParameterValue(param.getName());
            try{
            expressionParameters.put(param.getName(), createTypedPropertyValue(v, (Class)param.getValueClass()));
            } catch (Exception e){
                //just skip properties that are not important for midpoint
            }

            LOGGER.trace("p.val: {}", v);
        }
        return expressionParameters;
    }

    protected VariablesMap getPromptingParameters() {
        JRParameter[] params = dataset.getParameters();
        VariablesMap expressionParameters = new VariablesMap();
        for (JRParameter param : params) {
            if (param.isSystemDefined()) {
                continue;
            }
            if (!param.isForPrompting()) {
                continue;
            }
            //LOGGER.trace(((JRBaseParameter)param).getName());
            Object v = getParameterValue(param.getName());
            try{
            expressionParameters.put(param.getName(), createTypedPropertyValue(v, (Class)param.getValueClass()));
            } catch (Exception e){
                //just skip properties that are not important for midpoint
            }

            LOGGER.trace("p.val: {}", v);
        }
        return expressionParameters;
    }

    protected abstract Object getParsedQuery(String query, VariablesMap expressionParameters) throws  SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    protected String getParsedScript(String script){
        String normalized = script.replace("<code>", "");
        return normalized.replace("</code>", "");
    }

        protected MidPointQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
            Map<String, ? extends JRValueParameter> parametersMap) {
        super(jasperReportsContext, dataset, parametersMap);
    }

    protected abstract <O extends ObjectType> Collection<PrismObject<O>> searchObjects(Object query, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    protected abstract Collection<PrismContainerValue<? extends Containerable>> evaluateScript(String script, VariablesMap parameters) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    protected abstract Collection<AuditEventRecord> searchAuditRecords(String script, VariablesMap parameters) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException;

    protected abstract JRDataSource createDataSourceFromObjects(Collection<PrismObject<? extends ObjectType>> results);

    protected abstract JRDataSource createDataSourceFromContainerValues(Collection<PrismContainerValue<? extends Containerable>> results);

    @Override
    protected void parseQuery() {
        try {

            String s = dataset.getQuery().getText();
            LOGGER.trace("query: " + s);
            if (StringUtils.isEmpty(s)) {
                query = null;
            } else {
                if (s.startsWith("<filter")) {
                    query = getParsedQuery(s, getParameters());
                    // getParsedQuery(s, expressionParameters);
                } else if (s.startsWith("<code")) {
                    script = getParsedScript(s);
                }
            }
        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            // TODO Auto-generated catch block
            throw new SystemException(e.getMessage(), e);
        }

    }

    @Override
    public JRDataSource createDatasource() throws JRException {
        try {
            if (query == null && script == null){
                throw new JRException("Neither query, nor script defined in the report.");
            }

            if (query != null) {
                Collection<PrismObject<? extends ObjectType>> results;
                results = (Collection) searchObjects(query, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
                return createDataSourceFromObjects(results);
            } else {
                if (isAuditReport()) {
                    Collection<AuditEventRecord> auditEventRecords = searchAuditRecords(script, getPromptingParameters());
                    Collection<AuditEventRecordType> auditEventRecordsType = new ArrayList<>();
                    for (AuditEventRecord aer : auditEventRecords) {
                        AuditEventRecordType aerType = aer.createAuditEventRecordType(true);
                        auditEventRecordsType.add(aerType);
                    }
                    return new JRBeanCollectionDataSource(auditEventRecordsType);
                } else {
                    Collection<PrismContainerValue<? extends Containerable>> results;
                    results = evaluateScript(script, getParameters());
                    return createDataSourceFromContainerValues(results);
                }
            }
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException
                | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            // TODO Auto-generated catch block
            throw new JRException(e);
        }
    }

    protected abstract boolean isAuditReport();

    @Override
    public void close() {
//        throw new UnsupportedOperationException("QueryExecutor.close() not supported");
        //nothing to DO
    }

    @Override
    public boolean cancelQuery() throws JRException {
         throw new UnsupportedOperationException("QueryExecutor.cancelQuery() not supported");
    }

    @Override
    protected String getParameterReplacement(String parameterName) {
         throw new UnsupportedOperationException("QueryExecutor.getParameterReplacement() not supported");
    }



}
