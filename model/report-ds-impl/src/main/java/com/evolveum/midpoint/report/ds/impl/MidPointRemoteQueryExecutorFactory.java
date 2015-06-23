package com.evolveum.midpoint.report.ds.impl;

import java.util.Map;

import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.query.AbstractQueryExecuterFactory;
import net.sf.jasperreports.engine.query.JRQueryExecuter;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportService;
import com.sun.tools.xjc.model.Model;



public class MidPointRemoteQueryExecutorFactory extends AbstractQueryExecuterFactory{


	public final static String PARAMETER_MIDPOINT_CONNECTION = "MIDPOINT_CONNECTION";
	
	
	private final static Object[] MIDPOINT_BUILTIN_PARAMETERS = {
		PARAMETER_MIDPOINT_CONNECTION, "midpoint.connection"
		};
	
	
	@Override
	public Object[] getBuiltinParameters() {
		return null;
//		return MIDPOINT_BUILTIN_PARAMETERS;
	}

	@Override
	public JRQueryExecuter createQueryExecuter(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parameters) throws JRException {
		return new MidPointRemoteQueryExecutor(jasperReportsContext, dataset, parameters);
	}

	@Override
	public boolean supportsQueryParameterType(String className) {
		return true;
	}
	
	

}
