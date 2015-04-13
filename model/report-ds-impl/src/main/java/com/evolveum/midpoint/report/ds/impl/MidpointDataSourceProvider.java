package com.evolveum.midpoint.report.ds.impl;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataSourceProvider;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.xml.JRXmlWriter;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportService;


public class MidpointDataSourceProvider implements JRDataSourceProvider {

	
	// Configuration
		public static final String ADM_USERNAME = "administrator";
		public static final String ADM_PASSWORD = "5ecr3t";
		private static final String DEFAULT_ENDPOINT_URL = "http://localhost:8080/midpoint/ws/report-3";
	
	public static ReportPortType createReportPort(String[] args) {
		String endpointUrl = DEFAULT_ENDPOINT_URL;
		
		if (args != null && args.length > 0) {
			endpointUrl = args[0];
		}

		System.out.println("Endpoint URL: "+endpointUrl);

        // uncomment this if you want to use Fiddler or any other proxy
        //ProxySelector.setDefault(new MyProxySelector("127.0.0.1", 8888));
		
		ReportService reportService = new ReportService();
		ReportPortType reportPort = reportService.getReportPort();
		BindingProvider bp = (BindingProvider)reportPort;
		Map<String, Object> requestContext = bp.getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
		
		org.apache.cxf.endpoint.Client client = ClientProxy.getClient(reportPort);
		org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();
		
		Map<String,Object> outProps = new HashMap<String,Object>();
		
		outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		outProps.put(WSHandlerConstants.USER, ADM_USERNAME);
		outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
		outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());
		
		WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
		cxfEndpoint.getOutInterceptors().add(wssOut);
        // enable the following to get client-side logging of outgoing requests and incoming responses
        cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor());

		return reportPort;
	}

	@Override
	public boolean supportsGetFieldsOperation() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JRField[] getFields(JasperReport report) throws JRException, UnsupportedOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JRDataSource create(JasperReport report) throws JRException {
		System.out.println("creating report port");
		try {
		ReportPortType reportPort = createReportPort(null);
		
		System.out.println("reportport created");
		String jrxml = JRXmlWriter.writeReport(report, "UTF-8");
		byte[] jrxmlBase64 = Base64.encodeBase64(jrxml.getBytes());
		ReportType reportType = new ReportType();
		reportType.setTemplate(jrxmlBase64);
		
		ObjectListType olt = reportPort.processReport(reportType);
		return	new MidPointDataSource(olt);
		} catch (Exception e){
			System.out.println("exception: " +e);
			e.printStackTrace(System.out);
			throw e;
		}
		
	}

	@Override
	public void dispose(JRDataSource dataSource) throws JRException {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) throws JRException {
		MidpointDataSourceProvider dsp = new MidpointDataSourceProvider();
		dsp.create(null);
	}
}
