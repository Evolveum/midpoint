package com.evolveum.midpoint.report.ds.impl;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportService;

public class MidPointClientConfiguration {

	private String username;
	private String password;
	private String endpoint;
	
	public MidPointClientConfiguration() {
		System.out.println("client configuration init");
	}
	
	
	public ReportPortType createReportPort(PrismContext prismContext) {
		System.out.println("creating endpoint with credentials " + username + ": " + password);
		System.out.println("Endpoint URL: "+ endpoint);
		ClientPasswordHandler.setPassword(password);

        // uncomment this if you want to use Fiddler or any other proxy
        //ProxySelector.setDefault(new MyProxySelector("127.0.0.1", 8888));
		
		ReportService reportService = new ReportService();
		ReportPortType reportPort = reportService.getReportPort();
		BindingProvider bp = (BindingProvider)reportPort;
		Map<String, Object> requestContext = bp.getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
		
		org.apache.cxf.endpoint.Client client = ClientProxy.getClient(reportPort);
		org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();	
		
		Map<String,Object> outProps = new HashMap<String,Object>();
		
		outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		outProps.put(WSHandlerConstants.USER, username);
		outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
		outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());
		
		WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
		cxfEndpoint.getOutInterceptors().add(wssOut);
//		cxfEndpoint.getOutInterceptors().add(new CustomWrappedOutInterceptor(prismContext));
        // enable the following to get client-side logging of outgoing requests and incoming responses
        cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor());
		return reportPort;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getEndpoint() {
		return endpoint;
	}
	
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

}
