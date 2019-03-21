/*
 * Copyright (c) 2010-2018 Evolveum
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.ibm.icu.text.ChineseDateFormat.Field;

import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRExpressionChunk;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.fill.ExpressionValues;
import net.sf.jasperreports.engine.fill.FillExpressionDefaultValues;
import net.sf.jasperreports.engine.fill.FillExpressionEstimatedValues;
import net.sf.jasperreports.engine.fill.FillExpressionOldValues;
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
	
	private JasperReport jasperReport;
	private JRDataset dataset;
	
	private Map<String, JRFillParameter> parametersMap; 
	private Map<String, JRFillField> fieldsMap;
	private Map<String, JRFillVariable> variablesMap;
	
	
	public JRMidpointEvaluator(Serializable compileData, String unitName) {
		this.compileData = compileData;
		this.unitName = unitName;
	}
	
	public JRMidpointEvaluator(JasperReport jasperReprot, JRDataset dataset) {
		this.jasperReport = jasperReprot;
		this.dataset = dataset;
	}
	
	public JRMidpointEvaluator(JasperReport jasperReprot) {
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
				
		reportService = SpringApplicationContext.getBean(ReportService.class);
	}

	@Override
	public Object evaluate(JRExpression expression) throws JRExpressionEvalException {
		if (expression == null) {
			return null;
		}
		JRExpressionChunk[] ch = expression.getChunks();
		
		Map<QName, Object> parameters = new HashMap<>();
		
		String groovyCode = "";
		
		for (JRExpressionChunk chunk : expression.getChunks()) {
			if (chunk == null) {
				break;
			}
			
			switch (chunk.getType()){
				case JRExpressionChunk.TYPE_FIELD:
					groovyCode += chunk.getText();
					JRFillField field = fieldsMap.get(chunk.getText());
					parameters.put(new QName(field.getName()), field.getValue());
					break;
				case JRExpressionChunk.TYPE_PARAMETER:
					groovyCode += chunk.getText();
					JRFillParameter param = parametersMap.get(chunk.getText());
					parameters.put(new QName(param.getName()), param.getValue());
					break;
				case JRExpressionChunk.TYPE_VARIABLE:
					groovyCode += chunk.getText();
					JRFillVariable var = variablesMap.get(chunk.getText());
					parameters.put(new QName(var.getName()), var.getValue());
					break;
				case JRExpressionChunk.TYPE_TEXT:
					groovyCode += chunk.getText();
					break;
				default :
					groovyCode += chunk.getText();
						
			}
			
		}
		
		
		if (reportService != null) {
			try {
				return reportService.evaluate(groovyCode, parameters);
			} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
					| ConfigurationException | SecurityViolationException e) {
				throw new JRRuntimeException(e.getMessage(), e);
			}
		}
		
		byte type = ch[0].getType();
		return "tralalal";
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
