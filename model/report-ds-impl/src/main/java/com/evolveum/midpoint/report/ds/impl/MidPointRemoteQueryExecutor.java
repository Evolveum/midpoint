package com.evolveum.midpoint.report.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordListType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParameterType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParametersType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

public class MidPointRemoteQueryExecutor extends MidPointQueryExecutor{
	
	private String query;
	private ReportPortType reportPort;
	private PrismContext prismContext;
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointRemoteQueryExecutor.class);
	private ClassPathXmlApplicationContext applicationContext;
	
	
	public String getQuery() {
		return query;
	}
	
	
	@Override
	protected Object getParsedQuery(String query, Map<QName, Object> expressionParameters) throws  SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
	
		RemoteReportParametersType reportParamters = converToReportParameterType(expressionParameters);
		
		return reportPort.parseQuery(query, reportParamters);
//		return getStringQuery();
	}
	
	private RemoteReportParametersType converToReportParameterType(Map<QName, Object> expressionParameters) throws SchemaException {
		Set<Entry<QName, Object>> paramSet = expressionParameters.entrySet();
		if (paramSet == null || paramSet.isEmpty()){
			return null;
		}
		RemoteReportParametersType reportParams = new RemoteReportParametersType();
		for (Entry<QName, Object> param : paramSet){
			RemoteReportParameterType remoteParam = new RemoteReportParameterType();
			remoteParam.setParameterName(param.getKey().getLocalPart());
//			DOM3SerializerImpl domser = new DOM3SerializerImpl(null);
			Object value = ((PrismPropertyValue)param.getValue()).getValue();
//			if (value!= null && List.class.isAssignableFrom(value.getClass())){
//				remoteParam.getAny().addAll((List<Object>) value);
//			} else {
//				remoteParam.getAny().add(value);
//			}
			reportParams.getRemoteParameter().add(remoteParam);
		}
		return reportParams;
	}


	@Override
	protected Collection searchObjects(Object query,
			Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException,
			ObjectNotFoundException, SecurityViolationException, CommunicationException,
			ConfigurationException {
		// TODO Auto-generated method stub
		SelectorQualifiedGetOptionsType optionsType = MiscSchemaUtil.optionsToOptionsType(options);
	
		ObjectListType results = reportPort.searchObjects((QueryType)query, optionsType);
		if (results == null){
			return new ArrayList<>();
		}
		return results.getObject();
	}
	
//	private Collection<PrismObject<? extends ObjectType>> toPrismList(ObjectListType results) throws SchemaException{
//			Collection<PrismObject<? extends ObjectType>> resultPrismList = new ArrayList<>();
//			for (ObjectType objType : results.getObject()){
//				PrismObject prism = ((Objectable)objType).asPrismObject();
//				prism.revive(prismContext);
//			
//				resultPrismList.add(prism);
//			}
//			return resultPrismList;
//		
//	}
	
	private Collection<AuditEventRecord> toAuditRecordList(AuditEventRecordListType results) throws SchemaException{
			Collection<AuditEventRecord> resultPrismList = new ArrayList<>();
			for (AuditEventRecordType objType : results.getObject()){
				AuditEventRecord auditRecord = AuditEventRecord.createAuditEventRecord(objType);
				resultPrismList.add(auditRecord);
			}
			return resultPrismList;
		
	}
	
	@Override
	protected JRDataSource createDataSource(Collection results) {
		return new MidPointRemoteDataSource(results, reportPort);
	}
	
	
	protected MidPointRemoteQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parametersMap) {
		super(jasperReportsContext, dataset, parametersMap);
		MidPointPrismContextFactory factory = new MidPointPrismContextFactory();
//		try {
//			if (prismContext == null) {
//				prismContext = factory.createInitializedPrismContext();
//			}
//		} catch (SchemaException | SAXException | IOException e) {
//			throw new SystemException(e.getMessage(), e);
//		}
		if (applicationContext == null) {
			applicationContext = new ClassPathXmlApplicationContext("ctx-report-ds-context.xml");
		}
		MidPointClientConfiguration clientConfig = applicationContext.getBean("clientConfig",
				MidPointClientConfiguration.class);
		if (reportPort == null) {
			reportPort = applicationContext.getBean("reportPort", ReportPortType.class);
		}
//		if (reportPort == null) {
//			reportPort = clientConfig.createReportPort(prismContext);
//		}
		parseQuery();
	}
		
		private String getStringQuery(){
			if (dataset.getQuery() == null){
//				query = null;
				return null;
			}
			return dataset.getQuery().getText();
		}


		@Override
		protected Collection evaluateScript(String script,
				Map<QName, Object> parameters) throws SchemaException, ObjectNotFoundException,
				SecurityViolationException, CommunicationException, ConfigurationException,
				ExpressionEvaluationException {
			// TODO Auto-generated method stu
			LOGGER.debug("evaluating script: {} with parameters: {}", script, parameters);
		
			RemoteReportParametersType reportParamters = converToReportParameterType(parameters);
			if (reportParamters == null){
				return new ArrayList<>();
			}
			LOGGER.debug("coverted to report parameters: {}", reportParamters);
			ObjectListType results =  reportPort.evaluateScript(script, reportParamters);
			if (results == null){
				return new ArrayList<>();
			}
			return results.getObject();
		}


		@Override
		protected Collection<AuditEventRecord> searchAuditRecords(String script, Map<QName, Object> parameters)
				throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
			// TODO Auto-generated method stub
			RemoteReportParametersType reportParamters = converToReportParameterType(parameters);
			
			if (reportParamters == null){
				return new ArrayList<>();
			}
			AuditEventRecordListType results = reportPort.evaluateAuditScript(script, reportParamters);
			return toAuditRecordList(results);
		}

		@Override
		public void close() {
			applicationContext.destroy();
//			throw new UnsupportedOperationException("QueryExecutor.close() not supported");
			//nothing to DO
		}


}
