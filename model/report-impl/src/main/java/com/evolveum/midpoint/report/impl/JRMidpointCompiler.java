/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.io.File;
import java.io.Serializable;

import net.sf.jasperreports.crosstabs.JRCrosstab;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.design.JRAbstractCompiler;
import net.sf.jasperreports.engine.design.JRCompilationSourceCode;
import net.sf.jasperreports.engine.design.JRCompilationUnit;
import net.sf.jasperreports.engine.design.JRSourceCompileTask;
import net.sf.jasperreports.engine.fill.JREvaluator;

import com.evolveum.midpoint.schema.util.ReportTypeUtil;

/**
 * Custom expression compiler for JasperReports. This class is used to direct all expression execution
 * to our custom evaluator (JRMidpointEvaluator).
 * This compiler is not really compiling anything. It just fakes everything.
 *
 * @author katkav
 */
public class JRMidpointCompiler extends JRAbstractCompiler {

    public JRMidpointCompiler(JasperReportsContext jasperReportsContext) {
        super(jasperReportsContext, false);
    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.design.JRCompiler#loadEvaluator(net.sf.jasperreports.engine.JasperReport)
     */
    @Override
    public JREvaluator loadEvaluator(JasperReport jasperReport) throws JRException {
        return new JRMidpointEvaluator(jasperReport);
    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.design.JRCompiler#loadEvaluator(net.sf.jasperreports.engine.JasperReport, net.sf.jasperreports.crosstabs.JRCrosstab)
     */
    @Override
    public JREvaluator loadEvaluator(JasperReport jasperReport, JRCrosstab crosstab) throws JRException {
        return new JRMidpointEvaluator(jasperReport);
    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.design.JRCompiler#loadEvaluator(net.sf.jasperreports.engine.JasperReport, net.sf.jasperreports.engine.JRDataset)
     */
    @Override
    public JREvaluator loadEvaluator(JasperReport jasperReport, JRDataset dataset) throws JRException {
        return new JRMidpointEvaluator(jasperReport, dataset);
    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.design.JRAbstractCompiler#loadEvaluator(java.io.Serializable, java.lang.String)
     */
    @Override
    protected JREvaluator loadEvaluator(Serializable compileData, String unitName) throws JRException {
        return new JRMidpointEvaluator(compileData, unitName);

    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.design.JRAbstractCompiler#checkLanguage(java.lang.String)
     */
    @Override
    protected void checkLanguage(String language) throws JRException {
        if (!ReportTypeUtil.REPORT_LANGUAGE.equals(language)) {
            throw new JRException("Expression language '"+language+" is not supported");
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.design.JRAbstractCompiler#generateSourceCode(net.sf.jasperreports.engine.design.JRSourceCompileTask)
     */
    @Override
    protected JRCompilationSourceCode generateSourceCode(JRSourceCompileTask sourceTask) throws JRException {
//        return new JRDefaultCompilationSourceCode("FAKE", null);
        return null;
    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.design.JRAbstractCompiler#compileUnits(net.sf.jasperreports.engine.design.JRCompilationUnit[], java.lang.String, java.io.File)
     */
    @Override
    protected String compileUnits(JRCompilationUnit[] units, String classpath, File tempDirFile) throws JRException {
        // just pretend compilation, do nothing
        return null;
    }

    /* (non-Javadoc)
     * @see net.sf.jasperreports.engine.design.JRAbstractCompiler#getSourceFileName(java.lang.String)
     */
    @Override
    protected String getSourceFileName(String unitName) {
        return unitName;
    }

}
