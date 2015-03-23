package com.evolveum.midpoint.report.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.report.api.ReportPort;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.GetOperationOptions;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.EntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParamsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

@Service
public class ReportWebService implements ReportPortType, ReportPort {

	private static transient Trace LOGGER = TraceManager.getTrace(ReportWebService.class);

	@Autowired(required = true)
	private PrismContext prismContext;
//
//	@Autowired(required = true)
//	private TaskManager taskManager;
//
//	@Autowired(required = true)
//	private ModelService model;
//
//	@Autowired(required = true)
//	private ObjectResolver objectResolver;
//
//	@Autowired(required = true)
//	private AuditService auditService;
	
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


//	@Override
//	private QueryType parseQuery(String query, ParamsType parameters) {
//		
//		Map<QName, Object> params = getParamsMap(parameters);
//		
//		try {
//			ObjectQuery q =  reportService.parseQuery(query, params);
//			return QueryJaxbConvertor.createQueryType(q, prismContext);
//		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
//		
//	}


	@Override
	public ObjectListType searchObjects(String query, ParamsType parameters, SelectorQualifiedGetOptionsType options) {
		
		try {
			Map<QName, Object> params = getParamsMap(parameters);
			ObjectQuery objectQuery =  reportService.parseQuery(query, params);
			GetOperationOptions getOpts = GetOperationOptions.createRaw();
			getOpts.setResolveNames(Boolean.TRUE);
			
			Collection<PrismObject<? extends ObjectType>> resultList = reportService.searchObjects(objectQuery, SelectorOptions.createCollection(getOpts));
			
			return createObjectListType(resultList);
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
				| CommunicationException | ExpressionEvaluationException | ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
//	@Override
	public ObjectListType evaluateScript(String script, ParamsType parameters){
		Map<QName, Object> params = getParamsMap(parameters);
		
		try {
			Collection<PrismObject<? extends ObjectType>> resultList = reportService.evaluateScript(script, params);
			return createObjectListType(resultList);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		
	}

	
	private Map<QName, Object> getParamsMap(ParamsType parameters){
		
		Map<QName, Object> params = null;
		if (parameters != null) {
			params = new HashMap<QName, Object>();
			for (EntryType entry : parameters.getEntry()) {
				params.put(new QName(entry.getKey()), (Serializable) entry.getEntryValue());
			}
		}
		return params;
		
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
}
