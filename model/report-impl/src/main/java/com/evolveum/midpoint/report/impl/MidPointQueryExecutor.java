/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
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
	private ReportService reportService;
	
	public String getScript() {
		return script;
	}
	public Object getQuery() {
		return query;
	}
	public Class getType() {
		return type;
	}
		
	protected Map<QName, Object> getParameters(){
		JRParameter[] params = dataset.getParameters();
		Map<QName, Object> expressionParameters = new HashMap<QName, Object>();
		for (JRParameter param : params){
			if (param.isSystemDefined()){
				continue;
			}
			//LOGGER.trace(((JRBaseParameter)param).getName());
			Object v = getParameterValue(param.getName());
			try{ 
			expressionParameters.put(new QName(param.getName()), new PrismPropertyValue(v));
			} catch (Exception e){
				//just skip properties that are not important for midpoint
			}
			
			LOGGER.trace("p.val: {}", v);
		}
		return expressionParameters;
	}
	
	protected Map<QName, Object> getPromptingParameters(){
		JRParameter[] params = dataset.getParameters();
		Map<QName, Object> expressionParameters = new HashMap<QName, Object>();
		for (JRParameter param : params){
			if (param.isSystemDefined()){
				continue;
			}
			if (!param.isForPrompting()){
				continue;
			}
			//LOGGER.trace(((JRBaseParameter)param).getName());
			Object v = getParameterValue(param.getName());
			try{ 
			expressionParameters.put(new QName(param.getName()), new PrismPropertyValue(v));
			} catch (Exception e){
				//just skip properties that are not important for midpoint
			}
			
			LOGGER.trace("p.val: {}", v);
		}
		return expressionParameters;
	}
	
	protected abstract Object getParsedQuery(String query, Map<QName, Object> expressionParameters) throws  SchemaException, ObjectNotFoundException, ExpressionEvaluationException;
	
	protected String getParsedScript(String script){
		String normalized = script.replace("<code>", "");
		return normalized.replace("</code>", "");
	}
	
		protected MidPointQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parametersMap) {
		super(jasperReportsContext, dataset, parametersMap);
	}
	
	protected abstract Collection<PrismObject<? extends ObjectType>> searchObjects(Object query, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

	protected abstract Collection<PrismContainerValue<? extends Containerable>> evaluateScript(String script, Map<QName, Object> parameters) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
	
	protected abstract Collection<AuditEventRecord> searchAuditRecords(String script, Map<QName, Object> parameters) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException;
	
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
		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException e) {
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
				results = searchObjects(query, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
				return createDataSourceFromObjects(results);
			} else {
				if (script.contains("AuditEventRecord")){
					Collection<AuditEventRecord> audtiEventRecords = searchAuditRecords(script, getPromptingParameters());
					Collection<AuditEventRecordType> auditEventRecordsType = new ArrayList<>();
					for (AuditEventRecord aer : audtiEventRecords){
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

	@Override
	public void close() {
//		throw new UnsupportedOperationException("QueryExecutor.close() not supported");
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
