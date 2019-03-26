/*
 * Copyright (c) 2010-2019 Evolveum
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
import java.util.Map;

import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

public class MidPointLocalQueryExecutor extends MidPointQueryExecutor {

	private static final Trace LOGGER = TraceManager.getTrace(MidPointLocalQueryExecutor.class);
	private ObjectQuery query;
	private String script;
	private Class type;
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
		report = getParameterValue(parametersMap, ReportCreateTaskHandler.PARAMETER_REPORT_OBJECT);
		task = getParameterValue(parametersMap, ReportCreateTaskHandler.PARAMETER_TASK);

		// The PARAMETER_OPERATION_RESULT will not make it here. It is properly set in the task, but it won't arrive here.
		// No idea why.
//		operationResult = getParameterValue(parametersMap, ReportCreateTaskHandler.PARAMETER_OPERATION_RESULT);
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
	protected Collection<PrismObject<? extends ObjectType>> searchObjects(Object query, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException{
		return reportService.searchObjects((ObjectQuery) query, SelectorOptions.createCollection(GetOperationOptions.createRaw()), task, operationResult);
	}

	@Override
	protected Collection<PrismContainerValue<? extends Containerable>> evaluateScript(String script, VariablesMap parameters)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return reportService.evaluateScript(report, script, getParameters(), task, operationResult);
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


	public String getScript() {
		return script;
	}
	public ObjectQuery getQuery() {
		return query;
	}
	public Class getType() {
		return type;
	}

//	private Object getObjectQueryFromParameters(){
//		JRParameter[] params = dataset.getParameters();
//		Map<QName, Object> expressionParameters = new HashMap<QName, Object>();
//		for (JRParameter param : params){
//			if ("finalQuery".equals(param.getName())){
//				return getParameterValue(param.getName());
//			}
//		}
//		return null;
//	}
//
//	private Map<QName, Object> getParameters(){
//		JRParameter[] params = dataset.getParameters();
//		Map<QName, Object> expressionParameters = new HashMap<QName, Object>();
//		for (JRParameter param : params){
//			LOGGER.trace(((JRBaseParameter)param).getName());
//			Object v = getParameterValue(param.getName());
//			try{
//			expressionParameters.put(new QName(param.getName()), new PrismPropertyValue(v));
//			} catch (Exception e){
//				//just skip properties that are not important for midpoint
//			}
//
//			LOGGER.trace("p.val: {}", v);
//		}
//		return expressionParameters;
//	}
//
//	private Map<QName, Object> getPromptingParameters(){
//		JRParameter[] params = dataset.getParameters();
//		Map<QName, Object> expressionParameters = new HashMap<QName, Object>();
//		for (JRParameter param : params){
//			if (param.isSystemDefined()){
//				continue;
//			}
//			if (!param.isForPrompting()){
//				continue;
//			}
//			LOGGER.trace(((JRBaseParameter)param).getName());
//			Object v = getParameterValue(param.getName());
//			try{
//			expressionParameters.put(new QName(param.getName()), new PrismPropertyValue(v));
//			} catch (Exception e){
//				//just skip properties that are not important for midpoint
//			}
//
//			LOGGER.trace("p.val: {}", v);
//		}
//		return expressionParameters;
//	}
//
//	@Override
//	protected void parseQuery() {
//		// TODO Auto-generated method stub
//
//
//
//		String s = dataset.getQuery().getText();
//
//		JRBaseParameter p = (JRBaseParameter) dataset.getParameters()[0];
//
//		Map<QName, Object> expressionParameters = getParameters();
//		LOGGER.info("query: " + s);
//		if (StringUtils.isEmpty(s)){
//			query = null;
//		} else {
//			try {
//			if (s.startsWith("<filter")){
//
//				Object queryParam = getObjectQueryFromParameters();
//				if (queryParam != null){
//					if (queryParam instanceof String){
//						s = (String) queryParam;
//					} else if (queryParam instanceof ObjectQuery){
//						query = (ObjectQuery) queryParam;
//					}
//				}
//
//				if (query == null){
//					query = reportService.parseQuery(s, expressionParameters);
//				}
//			} else if (s.startsWith("<code")){
//				String normalized = s.replace("<code>", "");
//				script = normalized.replace("</code>", "");
//
//			}
//			} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//	}
//
	//	@Override
//	public JRDataSource createDatasource() throws JRException {
//		Collection<PrismObject<? extends ObjectType>> results = new ArrayList<>();
//
//		try {
//			if (query == null && script == null){
//				throw new JRException("Neither query, nor script defined in the report.");
//			}
//
//			if (query != null){
//				results = reportService.searchObjects(query, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
//			} else {
//				if (script.contains("AuditEventRecord")){
//					Collection<AuditEventRecord> audtiEventRecords = reportService.evaluateAuditScript(script, getPromptingParameters());
//					return new JRBeanCollectionDataSource(audtiEventRecords);
//				} else {
//					results = reportService.evaluateScript(script, getParameters());
//				}
//			}
//		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
//				| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
//			// TODO Auto-generated catch block
//			throw new JRException(e);
//		}
//
//		MidPointDataSource mds = new MidPointDataSource(results);
//
//		return mds;
//	}
//
//
//	@Override
//	public void close() {
////		throw new UnsupportedOperationException("QueryExecutor.close() not supported");
//		//nothing to DO
//	}
//
//	@Override
//	public boolean cancelQuery() throws JRException {
//		 throw new UnsupportedOperationException("QueryExecutor.cancelQuery() not supported");
//	}
//
//	@Override
//	protected String getParameterReplacement(String parameterName) {
//		 throw new UnsupportedOperationException("QueryExecutor.getParameterReplacement() not supported");
//	}






}
