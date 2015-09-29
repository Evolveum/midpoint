package com.evolveum.midpoint.report.ds.impl.test;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.cxf.wsdl.interceptors.BareOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.report.ds.impl.ClientPasswordHandler;
import com.evolveum.midpoint.report.ds.impl.CustomWrappedOutInterceptor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParameterType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParametersType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportService;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;


public class MidPointRemoteQueryExecuterTest {
	
	public Object getParsedQuery(PrismContext prismContext, ReportPortType reportPort) throws  SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		RemoteReportParametersType reportParameters = new RemoteReportParametersType();
		RemoteReportParameterType reportParameter = new RemoteReportParameterType();
		reportParameter.setParameterName("stringExample");
//		reportParameter.getAny().add("someString");
		
		reportParameters.getRemoteParameter().add(reportParameter);
		
		reportParameter = new RemoteReportParameterType();
		reportParameter.setParameterName("intExample");
//		reportParameter.getAny().add(11);
		
		reportParameters.getRemoteParameter().add(reportParameter);
		
		reportParameter = new RemoteReportParameterType();
		reportParameter.setParameterName("nullExample");
		
		reportParameters.getRemoteParameter().add(reportParameter);
			
		EqualFilter f = EqualFilter.createEqual(UserType.F_NAME, UserType.class, prismContext, PrismTestUtil.createPolyString("someName"));
		SearchFilterType filterType = QueryConvertor.createSearchFilterType(f, prismContext);
		QueryType q = new QueryType();
		q.setFilter(filterType);
		
		return null;
//		return reportPort.parseQuery(prismContext.serializeAtomicValue(filterType, SearchFilterType.COMPLEX_TYPE, PrismContext.LANG_XML), reportParameters);
//		return getStringQuery();
	}
	
	public static void main(String[] args) {
		PrismContext prismContext;
		try {
			
			PrismTestUtil.resetPrismContext(new MidPointPrismContextFactory());
		 
		MidPointRemoteQueryExecuterTest t = new MidPointRemoteQueryExecuterTest();
		ClassPathXmlApplicationContext  applicationContext = new ClassPathXmlApplicationContext("ctx-report-ds-context.xml");
		
		ReportPortType reportPort = applicationContext.getBean("reportPort", ReportPortType.class);
		
//		ReportPortType reportPort = createReportPort(PrismTestUtil.getPrismContext());
		
		QueryType f = (QueryType) t.getParsedQuery(PrismTestUtil.getPrismContext(), reportPort);
		System.out.println("returned filter: " + f.debugDump());
		} catch (SchemaException | SAXException | IOException | ObjectNotFoundException | ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static ReportPortType createReportPort(PrismContext prismContext) {
		ClientPasswordHandler.setPassword("5ecr3t");

        // uncomment this if you want to use Fiddler or any other proxy
        //ProxySelector.setDefault(new MyProxySelector("127.0.0.1", 8888));
		
		ReportService reportService = new ReportService();
		ReportPortType reportPort = reportService.getReportPort();
		BindingProvider bp = (BindingProvider)reportPort;
		Map<String, Object> requestContext = bp.getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://localhost:8080/midpoint/ws/report-3");
		
		org.apache.cxf.endpoint.Client client = ClientProxy.getClient(reportPort);
		org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();
		
		Map<String,Object> outProps = new HashMap<String,Object>();
		
		outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
		outProps.put(WSHandlerConstants.USER, "administrator");
		outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
		outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());
		
		WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
		BareOutInterceptor bareOutInterceptor = null;
		for (Interceptor in : cxfEndpoint.getOutInterceptors()){
			if (in instanceof BareOutInterceptor){
				bareOutInterceptor = (BareOutInterceptor) in;
			}
		}
		cxfEndpoint.getOutInterceptors().remove(bareOutInterceptor);
		cxfEndpoint.getOutInterceptors().add(wssOut);
		cxfEndpoint.getOutInterceptors().add(new CustomWrappedOutInterceptor(prismContext));

		// enable the following to get client-side logging of outgoing requests and incoming responses
        cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor());

		return reportPort;
	}
	
//	protected Collection<PrismObject<? extends ObjectType>> searchObjects(Object query,
//			Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException,
//			ObjectNotFoundException, SecurityViolationException, CommunicationException,
//			ConfigurationException {
//		// TODO Auto-generated method stub
//		SelectorQualifiedGetOptionsType optionsType = MiscSchemaUtil.optionsToOptionsType(options);
//	
//		ObjectListType results = reportPort.searchObjects((String) query, optionsType);
//		
//		return toPrismList(results);
//	}

}
