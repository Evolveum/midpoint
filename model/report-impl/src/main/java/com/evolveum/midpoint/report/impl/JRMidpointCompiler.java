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
import net.sf.jasperreports.engine.design.JRCompiler;
import net.sf.jasperreports.engine.design.JRSourceCompileTask;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.fill.JREvaluator;

/**
 * @author katka
 *
 */
public class JRMidpointCompiler extends JRAbstractCompiler {
	
	
	/**
	 * @param jasperReportsContext
	 * @param needsSourceFiles
	 */
	public JRMidpointCompiler(JasperReportsContext jasperReportsContext) {
		super(jasperReportsContext, false);
		// TODO Auto-generated constructor stub
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
		if (!"midPoint".equals(language)) {
			throw new JRException("asdasd");
		}
			
	}

	/* (non-Javadoc)
	 * @see net.sf.jasperreports.engine.design.JRAbstractCompiler#generateSourceCode(net.sf.jasperreports.engine.design.JRSourceCompileTask)
	 */
	@Override
	protected JRCompilationSourceCode generateSourceCode(JRSourceCompileTask sourceTask) throws JRException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.sf.jasperreports.engine.design.JRAbstractCompiler#compileUnits(net.sf.jasperreports.engine.design.JRCompilationUnit[], java.lang.String, java.io.File)
	 */
	@Override
	protected String compileUnits(JRCompilationUnit[] units, String classpath, File tempDirFile) throws JRException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.sf.jasperreports.engine.design.JRAbstractCompiler#getSourceFileName(java.lang.String)
	 */
	@Override
	protected String getSourceFileName(String unitName) {
		// TODO Auto-generated method stub
		return unitName;
	}

}
