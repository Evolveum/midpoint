/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.io.Serializable;
import java.util.Map;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.fill.*;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

/**
 * @author katka
 */
public class JRMidpointEvaluator extends JREvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(JRMidpointEvaluator.class);

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
    public void customizedInit(Map<String, JRFillParameter> parametersMap, Map<String, JRFillField> fieldsMap,
            Map<String, JRFillVariable> variablesMap) throws JRException {
        LOGGER.trace("customizedInit: ");
        LOGGER.trace("  parametersMap : {}", parametersMap);
        LOGGER.trace("  fieldsMap : {}", fieldsMap);
        LOGGER.trace("  variablesMap : {}", variablesMap);

        this.parametersMap = parametersMap;
        this.fieldsMap = fieldsMap;
        this.variablesMap = variablesMap;

        PrismObject<ReportType> midPointReportObject = (PrismObject<ReportType>) parametersMap.get(ReportTypeUtil.PARAMETER_REPORT_OBJECT).getValue();
        LOGGER.trace("midPointReportObject : {}", midPointReportObject);

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
//            return null;
            return new OperationResult("Evaluate report script"); //temporary fix because of training, MID-5457
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
        return evaluateExpression(expression, Mode.DEFAULT);
    }

    @Override
    public Object evaluateOld(JRExpression expression) throws JRExpressionEvalException {
        return evaluateExpression(expression, Mode.OLD);
    }

    @Override
    public Object evaluateEstimated(JRExpression expression) throws JRExpressionEvalException {
        return evaluateExpression(expression, Mode.ESTIMATED);
    }

    private void logEvaluate(Mode mode, JRExpression expression) {
        LOGGER.trace("JasperReport expression: evaluate({}): {} (type:{})", mode, expression, expression == null ? null : expression.getType());
    }

    private Object evaluateExpression(JRExpression expression, Mode mode) {
        logEvaluate(mode, expression);
        if (expression == null) {
            return null;
        }
        JRExpressionChunk[] ch = expression.getChunks();

        VariablesMap variables = new VariablesMap();

        StringBuilder groovyCode = new StringBuilder();

        for (JRExpressionChunk chunk : expression.getChunks()) {
            if (chunk == null) {
                break;
            }

            groovyCode.append(chunk.getText());
            switch (chunk.getType()) {
                case JRExpressionChunk.TYPE_FIELD:
                    JRFillField field = fieldsMap.get(chunk.getText());
                    variables.put(field.getName(), getFieldValue(chunk.getText(), mode), field.getValueClass());
                    break;
                case JRExpressionChunk.TYPE_PARAMETER:
                    JRFillParameter param = parametersMap.get(chunk.getText());
                    // Mode does not influence this one
                    variables.put(param.getName(), param.getValue(), param.getValueClass());
                    break;
                case JRExpressionChunk.TYPE_VARIABLE:
                    JRFillVariable var = variablesMap.get(chunk.getText());
                    variables.put(var.getName(), var.getValue(), var.getValueClass());
                    break;
                case JRExpressionChunk.TYPE_TEXT:
                    break;
                default:
                    LOGGER.trace("nothing to do for chunk type {}", chunk.getType());
            }
        }

        if (reportService == null) {
            throw new JRRuntimeException("No report service");
        }

        try {
            Object evaluationResult = reportService.evaluate(getReport(), groovyCode.toString(), variables, getTask(), getOperationResult());

            traceEvaluationSuccess(mode, variables, groovyCode.toString(), evaluationResult);
            return evaluationResult;
        } catch (Throwable e) {
            traceEvaluationFailure(mode, variables, groovyCode.toString(), e);
            throw new JRRuntimeException(e.getMessage(), e);
        }
    }

    private void traceEvaluationSuccess(Mode mode, VariablesMap variables, String code, Object result) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        StringBuilder sb = traceEvaluationHead(mode, variables, code);
        sb.append("Result: ").append(result).append("\n");
        traceEvaluationTail(sb);
    }

    private void traceEvaluationFailure(Mode mode, VariablesMap variables, String code, Throwable e) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        StringBuilder sb = traceEvaluationHead(mode, variables, code);
        sb.append("Error: ").append(e).append("\n");
        traceEvaluationTail(sb);
    }

    private StringBuilder traceEvaluationHead(Mode mode, VariablesMap variables, String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("---[ JasperReport expression evaluation ]---\n");
        sb.append("Report: ").append(getReport()).append("\n");
        sb.append("Mode: ").append(mode).append("\n");
        sb.append("Variables:\n");
        sb.append(variables.debugDump(1)).append("\n");
        sb.append("Code:\n");
        sb.append(code).append("\n");
        return sb;
    }

    private void traceEvaluationTail(StringBuilder sb) {
        sb.append("---------------------------------------------");
        LOGGER.trace("\n{}", sb.toString());
    }

    private Object getFieldValue(String name, Mode mode) {
        JRFillField field = fieldsMap.get(name);
        if (field == null) {
            return null;
        }
        switch (mode) {
            case DEFAULT:
            case ESTIMATED:
                return field.getValue();
            case OLD:
                return field.getOldValue();
            default:
                throw new IllegalArgumentException("Wrong mode " + mode);
        }
    }

    private Object getVariableValue(String name, Mode mode) {
        JRFillVariable var = variablesMap.get(name);
        if (var == null) {
            return null;
        }
        switch (mode) {
            case DEFAULT:
                return var.getValue();
            case ESTIMATED:
                return var.getEstimatedValue();
            case OLD:
                return var.getOldValue();
            default:
                throw new IllegalArgumentException("Wrong mode " + mode);
        }
    }

    enum Mode {DEFAULT, OLD, ESTIMATED}

    // NOT USED

    @Override
    protected Object evaluate(int id) throws Throwable {
        // Not used. Not even invoked.
        throw new UnsupportedOperationException("Boom! This code should not be reached");
    }

    @Override
    protected Object evaluateOld(int id) throws Throwable {
        // Not used. Not even invoked.
        throw new UnsupportedOperationException("Boom! This code should not be reached");
    }

    @Override
    protected Object evaluateEstimated(int id) throws Throwable {
        // Not used. Not even invoked.
        throw new UnsupportedOperationException("Boom! This code should not be reached");
    }
}
