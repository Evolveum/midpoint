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

import java.io.Serializable;
import java.util.Map;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportService;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRExpressionChunk;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.fill.JREvaluator;
import net.sf.jasperreports.engine.fill.JRExpressionEvalException;
import net.sf.jasperreports.engine.fill.JRFillField;
import net.sf.jasperreports.engine.fill.JRFillParameter;
import net.sf.jasperreports.engine.fill.JRFillVariable;

/**
 * @author katka
 *
 */
public class JRMidpointEvaluator extends JREvaluator {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(JRMidpointEvaluator.class);

	private Serializable compileData = null;
	private String unitName = null;
	
	private ReportService reportService;
	
	private PrismObject<ReportType> report;
	
	private JasperReport jasperReport;
	private JRDataset dataset;
	
	private Map<String, JRFillParameter> parametersMap; 
	private Map<String, JRFillField> fieldsMap;
	private Map<String, JRFillVariable> variablesMap;
	
	
	public JRMidpointEvaluator(Serializable compileData, String unitName) {
		LOGGER.info("NEW1: {}, {}", compileData, unitName);
		this.compileData = compileData;
		this.unitName = unitName;
	}
	
	public JRMidpointEvaluator(JasperReport jasperReprot, JRDataset dataset) {
		LOGGER.info("NEW2: {}, {}", jasperReprot, dataset);
		this.jasperReport = jasperReprot;
		this.dataset = dataset;
	}
	
	public JRMidpointEvaluator(JasperReport jasperReprot) {
		LOGGER.info("NEW3: {}", jasperReprot);
		this.jasperReport = jasperReprot;
	}
	
	@Override
	protected void customizedInit(Map<String, JRFillParameter> parametersMap, Map<String, JRFillField> fieldsMap,
			Map<String, JRFillVariable> variablesMap) throws JRException {
		LOGGER.info("cutomized init: ");
		LOGGER.info("parametersMap : {}", parametersMap);
		LOGGER.info("fieldsMap : {}", fieldsMap);
		LOGGER.info("variablesMap : {}", variablesMap);
		
		this.parametersMap = parametersMap;
		this.fieldsMap = fieldsMap;
		this.variablesMap = variablesMap;
		
		PrismObject<ReportType> midPointReportObject = (PrismObject<ReportType>) parametersMap.get(ReportTypeUtil.PARAMETER_REPORT_OBJECT).getValue();
		LOGGER.info("midPointReportObject : {}", midPointReportObject);
				
		reportService = SpringApplicationContext.getBean(ReportService.class);
		
	}
	
	private Task getTask() {
		JRFillParameter taskParam = parametersMap.get(ReportTypeUtil.PARAMETER_TASK);
		if (taskParam == null) {
			//TODO throw exception??
			return null;
		}
		
		return (Task) taskParam.getValue();
	}
	
	private OperationResult getOperationResult() {
		JRFillParameter resultParam = parametersMap.get(ReportTypeUtil.PARAMETER_OPERATION_RESULT);
		if (resultParam == null) {
			//TODO throw exception???
			return null;
		}
		return (OperationResult) resultParam.getValue();
	}
	
	private PrismObject<ReportType> getReport() {
		JRFillParameter resultParam = parametersMap.get(ReportTypeUtil.PARAMETER_REPORT_OBJECT);
		if (resultParam == null) {
			//TODO throw exception???
			return null;
		}
		return (PrismObject<ReportType>) resultParam.getValue();
	}

	@Override
	public Object evaluate(JRExpression expression) throws JRExpressionEvalException {
		LOGGER.info("evaluate expression: {}", expression);
		if (expression == null) {
			return null;
		}
		JRExpressionChunk[] ch = expression.getChunks();
		
		VariablesMap parameters = new VariablesMap();
		
		String groovyCode = "";
		
		for (JRExpressionChunk chunk : expression.getChunks()) {
			if (chunk == null) {
				break;
			}
			
			groovyCode += chunk.getText();
			switch (chunk.getType()){
				case JRExpressionChunk.TYPE_FIELD:
					JRFillField field = fieldsMap.get(chunk.getText());
					parameters.put(field.getName(), field.getValue(), field.getValueClass());
					break;
				case JRExpressionChunk.TYPE_PARAMETER:
					JRFillParameter param = parametersMap.get(chunk.getText());
					parameters.put(param.getName(), param.getValue(), param.getValueClass());
					break;
				case JRExpressionChunk.TYPE_VARIABLE:
					JRFillVariable var = variablesMap.get(chunk.getText());
					parameters.put(var.getName(), var.getValue(), var.getValueClass());
					break;
				case JRExpressionChunk.TYPE_TEXT:
					break;
				default :
					LOGGER.trace("nothing to do.");
						
			}
			
		}
		
		LOGGER.info("### EVALUATE ###\nParameters:\n{}\nCode:\n  {}\n################", parameters.debugDump(1), groovyCode);
		
		if (reportService == null) {
			throw new JRRuntimeException("No report service");
		}
		
		try {
			// TODO:
			
			Object evaluationResult = reportService.evaluate(getReport(), groovyCode, parameters, getTask(), getOperationResult());
			
			LOGGER.info("### evaluation result: {}", evaluationResult);
			
			return evaluationResult;
			
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
				| ConfigurationException | SecurityViolationException e) {
			throw new JRRuntimeException(e.getMessage(), e);
		}

	}

	@Override
	protected Object evaluate(int id) throws Throwable {
		LOGGER.info("evaluate: {}", id);
		return null;
		
	}

	@Override
	protected Object evaluateOld(int id) throws Throwable {
		LOGGER.info("evaluateOld: {}", id);
		return null;
	}

	@Override
	protected Object evaluateEstimated(int id) throws Throwable {
		LOGGER.info("evaluateEstimated: {}", id);
		return null;
	}

}
