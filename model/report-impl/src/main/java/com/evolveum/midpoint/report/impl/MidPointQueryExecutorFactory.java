package com.evolveum.midpoint.report.impl;

import java.util.Map;

import com.evolveum.midpoint.report.api.ReportService;

import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.query.AbstractQueryExecuterFactory;
import net.sf.jasperreports.engine.query.JRQueryExecuter;


public class MidPointQueryExecutorFactory extends AbstractQueryExecuterFactory{


//	public final static String PARAMETER_MIDPOINT_CONNECTION = "MIDPOINT_CONNECTION";
//	public final static String PARAMETER_PRISM_CONTEXT = "PRISM_CONTEXT";
//	public final static String PARAMETER_TASK_MANAGER = "TASK_MANAGER";
//	public final static String PARAMETER_EXPRESSION_FACTORY = "EXPRESSION_FACTORY";
//	public final static String PARAMETER_OBJECT_RESOLVER = "OBJECT_RESOLVER";
//	public final static String PARAMETER_MIDPOINT_FUNCTION = "MIDPOINT_FUNCTION";
//	public final static String PARAMETER_AUDIT_SERVICE = "AUDIT_SERVICE";
//	public final static String PARAMETER_REPORT_FUNCTIONS = "reportFunctions";


	private final static Object[] MIDPOINT_BUILTIN_PARAMETERS = {
		ReportService.PARAMETER_REPORT_SERVICE, "midpoint.connection"
		};


	@Override
	public Object[] getBuiltinParameters() {
		return MIDPOINT_BUILTIN_PARAMETERS;
	}

	@Override
	public JRQueryExecuter createQueryExecuter(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parameters) throws JRException {

		return new MidPointLocalQueryExecutor(jasperReportsContext, dataset, parameters);
	}

	@Override
	public boolean supportsQueryParameterType(String className) {
		return true;
	}



}
