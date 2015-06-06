package com.evolveum.midpoint.report.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRException;

import org.apache.cxf.interceptor.Fault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.report.api.ReportPort;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParameterType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParametersType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@Service
public class ReportWebService implements ReportPortType, ReportPort {

	private static transient Trace LOGGER = TraceManager.getTrace(ReportWebService.class);

	@Autowired(required = true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
	private ReportService reportService;

	@Override
	public ObjectListType processReport(ReportType report) {
//
//		Task task = taskManager.createTaskInstance("process report");
//		OperationResult parentResult = task.getResult();
//
//		JasperReport jasperReport;
//		try {
//			jasperReport = ReportUtils.loadJasperReport(report);
//
//			JRDataset dataset = jasperReport.getMainDataset();
//
//			MidPointQueryExecutor queryExecutor = new MidPointQueryExecutor(prismContext, taskManager,
//					dataset);
//			List results = new ArrayList<>();
//			if (queryExecutor.getQuery() != null) {
//				results = ReportUtils.getReportData(model, queryExecutor.getType(), queryExecutor.getQuery(),
//						task, parentResult);
//			} else {
//				ReportFunctions reportFunctions = new ReportFunctions(prismContext, model, taskManager,
//						auditService);
//				results = ReportUtils.getReportData(prismContext, task, reportFunctions,
//						queryExecutor.getScript(), queryExecutor.getVariables(), objectResolver);
//			}
//
//			ObjectListType listType = new ObjectListType();
//			for (Object o : results) {
//				if (o instanceof PrismObject) {
//					listType.getObject().add((ObjectType) ((PrismObject) o).asObjectable());
//				} else {
//					listType.getObject().add((ObjectType) o);
//				}
//			}
//			return listType;
//		} catch (SchemaException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ObjectNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SecurityViolationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (CommunicationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ConfigurationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ExpressionEvaluationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
		return null;

	}


	@Override
	public String parseQuery(String query, RemoteReportParametersType parametersType) {
		
//		Map<QName, Object> params = getParamsMap(parameters);
		
		
		try {
			Map<QName, Object> parametersMap = getParamsMap(parametersType);
			
			ObjectQuery q =  reportService.parseQuery(query, parametersMap);
			SearchFilterType filterType = QueryConvertor.createSearchFilterType(q.getFilter(), prismContext);
			return prismContext.serializeAtomicValue(filterType, SearchFilterType.COMPLEX_TYPE, PrismContext.LANG_XML);
		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			throw new Fault(e);
		}
		
	}


	@Override
	public ObjectListType searchObjects(String query, SelectorQualifiedGetOptionsType options) {
		
		try {
//			Map<QName, Object> params = getParamsMap(parameters);
//			ObjectQuery objectQuery =  reportService.parseQuery(query, null);
//			GetOperationOptions getOpts = GetOperationOptions.createRaw();
//			getOpts.setResolveNames(Boolean.TRUE);
			SearchFilterType filterType = prismContext.parseAtomicValue(query, SearchFilterType.COMPLEX_TYPE);
//			ObjectFilter filter = QueryConvertor.parseFilter(query, UserType.class, prismContext);
			ObjectQuery objectQuery = ObjectQuery.createObjectQuery(QueryJaxbConvertor.createObjectFilter(UserType.class, filterType, prismContext));
			
			Collection<PrismObject<? extends ObjectType>> resultList = reportService.searchObjects(objectQuery, MiscSchemaUtil.optionsTypeToOptions(options));
			
			return createObjectListType(resultList);
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
				| CommunicationException  | ConfigurationException e) {
			// TODO Auto-generated catch block
			throw new Fault(e);
		}
	}
	
	@Override
	public ObjectListType evaluateScript(String script, RemoteReportParametersType parameters){
		try {
			Map<QName, Object> params = getParamsMap(parameters);
			Collection<PrismObject<? extends ObjectType>> resultList = reportService.evaluateScript(script, params);
			return createObjectListType(resultList);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			throw new Fault(e);
		}
		
		
	}
	
	@Override
	public AuditEventRecordListType evaluateAuditScript(String script, RemoteReportParametersType parameters){
		
		try {
			Map<QName, Object> params = getParamsMap(parameters);
			Collection<AuditEventRecord> resultList = reportService.evaluateAuditScript(script, params);
			return createAuditEventRecordListType(resultList);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			throw new Fault(e);
		}
		
		
	}
	
	

	
	private Map<QName, Object> getParamsMap(RemoteReportParametersType parametersType) throws SchemaException{
		
//		prismContext.adopt(parametersType);
//		PrismContainerValue<ReportParameterType> parameter = parametersType.asPrismContainerValue();
		Map<QName, Object> parametersMap = new HashMap<>();
		if (parametersType != null){
			for (RemoteReportParameterType item : parametersType.getRemoteParameter()){
				parametersMap.put(new QName(item.getParameterName()), item.getAny());
			}
		}
		return parametersMap;
//		Map<QName, Object> params = null;
//		if (parameters != null) {
//			params = new HashMap<QName, Object>();
//			for (EntryType entry : parameters.getEntry()) {
//				Object obj = entry.getEntryValue();
//				Serializable value = null;
//				if (obj instanceof JAXBElement){
//					value = (Serializable) ((JAXBElement) obj).getValue();
//				} else {
//					value = (Serializable) entry.getEntryValue();
//				}
//				params.put(new QName(entry.getKey()), value);
//			}
//		}
//		return params;
		
	}
	
	private ObjectListType createObjectListType(Collection<PrismObject<? extends ObjectType>> resultList){
		if (resultList == null){
			return new ObjectListType();
		}
		
		ObjectListType results = new ObjectListType();
		for (PrismObject<? extends ObjectType> prismObject : resultList){
			results.getObject().add(prismObject.asObjectable());
		}
		
		return results;
	}
	
	private AuditEventRecordListType createAuditEventRecordListType(Collection<AuditEventRecord> resultList){
		if (resultList == null){
			return new AuditEventRecordListType();
		}
		
		AuditEventRecordListType results = new AuditEventRecordListType();
		for (AuditEventRecord auditRecord : resultList){
			results.getObject().add(auditRecord.createAuditEventRecordType());
		}
		
		return results;
	}


	@Override
	public RemoteReportParameterType getFieldValue(String parameterName, ObjectType object) {
		try {
			prismContext.adopt(object);
		} catch (SchemaException e) {
			throw new Fault(e);
		}
		
		PrismObject<? extends ObjectType> prismObject = object.asPrismObject();
		
		QName itemName = QNameUtil.uriToQName(parameterName);
		
		Item i = prismObject.findItem(itemName);
		if (i == null){
			return null;
//			throw new JRException("Object of type " + currentObject.getCompileTimeClass().getSimpleName() + " does not contain field " + fieldName +".");
		}
	
		RemoteReportParameterType param = new RemoteReportParameterType();
		
		if (i instanceof PrismProperty){
			if (i.isSingleValue()){
				param.getAny().add(((PrismProperty) i).getRealValue());
			} else {
				for (Object o : ((PrismProperty) i).getRealValues()){
					param.getAny().add(o);
				}
			}
		} else if (i instanceof PrismReference){
			if (i.isSingleValue()){
				param.getAny().add(((PrismReference) i).getValue().asReferencable());				
			} else {
				for (PrismReferenceValue refVal : ((PrismReference) i).getValues()){
					param.getAny().add(refVal.asReferencable());
				}
			}
		} else if (i instanceof PrismContainer){
			if (i.isSingleValue()){
				param.getAny().add(((PrismContainer) i).getValue().asContainerable());
			} else {
				for (Object pcv : i.getValues()){
					if (pcv instanceof PrismContainerValue){
						param.getAny().add(((PrismContainerValue) pcv).asContainerable());
					}
				}
			}
		
		} else
			throw new Fault(new IllegalArgumentException("Could not get value of the field: " + itemName));
		
		return param;
//		return 
//		throw new UnsupportedOperationException("dataSource.getFiledValue() not supported");
	
	}


	
}
