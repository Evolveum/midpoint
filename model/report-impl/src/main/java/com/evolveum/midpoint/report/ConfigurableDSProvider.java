package com.evolveum.midpoint.report;

import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataSourceProvider;
import net.sf.jasperreports.engine.JasperReport;

public interface ConfigurableDSProvider extends JRDataSourceProvider {
	
	public JRDataSource create(JasperReport report, Map<?, ?> parameters);
	

}
