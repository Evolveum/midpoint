package com.evolveum.midpoint.report.ds.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.BindingProvider;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.base.JRBaseParameter;
import net.sf.jasperreports.engine.query.JRAbstractQueryExecuter;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParamsType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportService;

public class MidPointRemoteQueryExecutor extends JRAbstractQueryExecuter{
	
	// Configuration
	public static final String ADM_USERNAME = "administrator";
	public static final String ADM_PASSWORD = "5ecr3t";
	private static final String DEFAULT_ENDPOINT_URL = "http://localhost:8080/midpoint/ws/report-3";

	private String query;
	private ReportPortType reportPort;
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointRemoteQueryExecutor.class);
	
	
	public String getQuery() {
		return query;
	}
	
	
	@Override
	protected void parseQuery() {
		query = getStringQuery();

//		ParamsType params = getParameters();
//		LOGGER.trace("Report query: " + queryString);
//		ObjectQuery q;
//		if (StringUtils.isEmpty(queryString)) {
//			q = null;
//		} else {
//			query = reportPort.parseQuery(queryString, params);
//		}
	}
	
	
	private boolean containsExpression(ObjectFilter subFilter){
		if (subFilter instanceof PropertyValueFilter){
			return ((PropertyValueFilter) subFilter).getExpression() != null;
		} else if (subFilter instanceof InOidFilter){
			return ((InOidFilter) subFilter).getExpression() != null;
		}
		
		return false;
	}
	

	protected MidPointRemoteQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parametersMap) {
		super(jasperReportsContext, dataset, parametersMap);
		reportPort = createReportPort(null);
		parseQuery();
	}

	@Override
	public JRDataSource createDatasource() throws JRException {
			SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
			SelectorQualifiedGetOptionType option = new SelectorQualifiedGetOptionType();
			GetOperationOptionsType getOptions = new GetOperationOptionsType();
			getOptions.setRaw(Boolean.TRUE);
			option.setOptions(getOptions);
			
			options.getOption().add(option);
			
			String queryString = getStringQuery();
			
			ObjectListType results = null;
			if (queryString.startsWith("<filter")){
				 results = reportPort.searchObjects(query, getParameters(), options);
			} else if (queryString.startsWith("<code")){
				String normalized = queryString.replace("<code>", "");
				String script = normalized.replace("</code>", "");
				ParamsType parameters = getParameters();
				results = reportPort.evaluateScript(script, parameters);
				 
			} else {
				throw new IllegalArgumentException("Neither query nor filter defined in query");
			}
		
		MidPointDataSource mds = new MidPointDataSource(results);
		
		return mds;
	}
	
	
	@Override
	public void close() {
//		throw new UnsupportedOperationException("QueryExecutor.close() not supported");
		//nothing to DO
	}

	@Override
	public boolean cancelQuery() throws JRException {
		 throw new UnsupportedOperationException("QueryExecutor.cancelQuery() not supported");
	}

	@Override
	protected String getParameterReplacement(String parameterName) {
		 throw new UnsupportedOperationException("QueryExecutor.getParameterReplacement() not supported");
	}
	
	private static ReportPortType createReportPort(String[] args) {
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
	
	private String getStringQuery(){
		if (dataset.getQuery() == null){
//			query = null;
			return null;
		}
		return dataset.getQuery().getText();
	}

	private ParamsType getParameters(){
		JRParameter[] params = dataset.getParameters();
		ParamsType parameters = new ParamsType();
		for (JRParameter param : params){
			if (param.isSystemDefined()){
				continue;
			}
			LOGGER.trace(((JRBaseParameter)param).getName());
			try{
				Serializable v = (Serializable) getParameterValue(param.getName());
				EntryType entry = new EntryType();
				entry.setKey(param.getName());
				entry.setEntryValue(new JAXBElement<Serializable>(SchemaConstantsGenerated.C_PARAM_VALUE, Serializable.class, v));
				parameters.getEntry().add(entry);
				LOGGER.trace("p.val: {}", v);
			} catch (Exception e){
				//just skip properties that are not important for midpoint
			}
			
			
		}
		return parameters;
	}

}
