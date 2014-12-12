package com.evolveum.midpoint.report.impl;



public class MidPointDataSourceProvider {
//	implements JRDataSourceProvider{
//}
//
//	
//	public static final String ADM_USERNAME = "administrator";
//	public static final String ADM_PASSWORD = "5ecr3t";
//	private static final String DEFAULT_ENDPOINT_URL = "http://localhost.:8080/midpoint/model/model-3";
//	
//	@Override
//	public boolean supportsGetFieldsOperation() {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("dataSourceProvider.supportsGetFieldsOperation() not supported");
//	}
//
//	@Override
//	public JRField[] getFields(JasperReport report) throws JRException, UnsupportedOperationException {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("dataSourceProvider.getFields() not supported");
//	}
//
//	@Override
//	public JRDataSource create(JasperReport report) throws JRException {
//		
//			String endpointUrl = DEFAULT_ENDPOINT_URL;
//			
//			System.out.println("Endpoint URL: "+endpointUrl);
//
//	        // uncomment this if you want to use Fiddler or any other proxy
//	        //ProxySelector.setDefault(new MyProxySelector("127.0.0.1", 8888));
//			
//			ModelService modelService = new ModelService();
//			ModelPortType modelPort = modelService.getModelPort();
//			BindingProvider bp = (BindingProvider)modelPort;
//			Map<String, Object> requestContext = bp.getRequestContext();
//			requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
//			
//			org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
//			org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();
//			
//			Map<String,Object> outProps = new HashMap<String,Object>();
//			
//			outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
//			outProps.put(WSHandlerConstants.USER, ADM_USERNAME);
//			outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
//			outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());
//			
//			WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
//			cxfEndpoint.getOutInterceptors().add(wssOut);
//	        // enable the following to get client-side logging of outgoing requests and incoming responses
//	        //cxfEndpoint.getOutInterceptors().add(new LoggingOutInterceptor());
//	        //cxfEndpoint.getInInterceptors().add(new LoggingInInterceptor());
//
//		String s = report.getQuery().getText();
//		QueryType q;
//		if (StringUtils.isEmpty(s)){
//			q = null;
//		} else {
//		
//		try {
//			SearchFilterType fitler = ModelClientUtil.parseSearchFilterType(s);
//			
//			q = new QueryType();
//			q.setFilter(fitler);
//		} catch (IOException | SAXException | JAXBException e) {
//			throw new JRException(e);
//		}
//		}
//		List<PrismObject<? extends ObjectType>> results = new ArrayList<>();
//		try {
//			Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
//			Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
//			modelPort.searchObjects(UserType.COMPLEX_TYPE, q, null, objectListHolder, resultHolder);
//			
//			List<PrismObject<UserType>> users = new ArrayList<>();
//			
//			ObjectListType objectList = objectListHolder.value;
//			List<ObjectType> objects = objectList.getObject();
//			
//			for (ObjectType obj : objects){
//				users.add(obj.asPrismObject());
//			}
//			
//			results.addAll(users);
//		
//		} catch (FaultMessage e) {
//			// TODO Auto-generated catch block
//			throw new JRException(e);
//		}		
//		
////		
//		MidPointDataSource mds = new MidPointDataSource(results);
//		return mds;
//	}
//
//	@Override
//	public void dispose(JRDataSource dataSource) throws JRException {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("dataSourceProvider.dispose() not supported");
//		
//	}

}
