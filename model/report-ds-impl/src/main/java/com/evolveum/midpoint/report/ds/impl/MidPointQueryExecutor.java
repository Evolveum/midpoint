package com.evolveum.midpoint.report.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.base.JRBaseParameter;
import net.sf.jasperreports.engine.fill.JRFillParameter;
import net.sf.jasperreports.engine.query.JRAbstractQueryExecuter;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class MidPointQueryExecutor extends JRAbstractQueryExecuter{
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointQueryExecutor.class);
//	private PrismContext prismContext;
//	private ModelService model;
//	private TaskManager taskManager;
//	private ExpressionFactory expressionFactory;
//	private ObjectResolver objectResolver;
	private ObjectQuery query;
	private String script;
	private Class type;
	private ReportService reportService;
//	private ExpressionVariables variables;
//	private MidpointFunctions midpointFunctions;
//	private AuditService auditService;
//	private ReportFunctions reportFunctions;
	
	public String getScript() {
		return script;
	}
	public ObjectQuery getQuery() {
		return query;
	}
	public Class getType() {
		return type;
	}
	
//	public ExpressionVariables getVariables() {
//		return variables;
//	}
	
//	public MidPointQueryExecutor(PrismContext prismContext, TaskManager taskManager, JRDataset dataset) {
//		super(null, dataset, new HashMap());
//		this.prismContext = prismContext;
//		this.taskManager = taskManager;
////		this.dataset = dataset;
//		
//		parseQuery();
//		
//	}
	
	private Map<QName, Object> getParameters(){
		JRParameter[] params = dataset.getParameters();
		Map<QName, Object> expressionParameters = new HashMap<QName, Object>();
		for (JRParameter param : params){
			LOGGER.info(((JRBaseParameter)param).getName());
			Object v = getParameterValue(param.getName());
			try{ 
			expressionParameters.put(new QName(param.getName()), new PrismPropertyValue(v));
			} catch (Exception e){
				//just skip properties that are not important for midpoint
			}
			
			LOGGER.info("p.val: {}", v);
		}
		return expressionParameters;
	}
	
	@Override
	protected void parseQuery() {
		// TODO Auto-generated method stub
		String s = dataset.getQuery().getText();
		JRBaseParameter p = (JRBaseParameter) dataset.getParameters()[0];
//		LOGGER.info("dataset param: {}", p.);
		
		
		Map<QName, Object> expressionParameters = getParameters();
		LOGGER.info("query: " + s);
//		ObjectQuery q;
		if (StringUtils.isEmpty(s)){
			query = null;
		} else {
			try {
			if (s.startsWith("<filter")){
			
				query = reportService.parseQuery(s, expressionParameters);
			
			} else if (s.startsWith("<code")){
				String normalized = s.replace("<code>", "");
				script = normalized.replace("</code>", "");
				
			}
			} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//		
//			if (s.startsWith("<filter")){
//				SearchFilterType filter = (SearchFilterType) prismContext.parseAtomicValue(s, SearchFilterType.COMPLEX_TYPE);
//				LOGGER.info("filter {}", filter);
//				ObjectFilter f = QueryConvertor.parseFilter(filter, UserType.class, prismContext);
//				LOGGER.info("f {}", f.debugDump());
//				if (!(f instanceof TypeFilter)){
//					throw new IllegalArgumentException("Defined query must contain type. Use 'type filter' in your report query.");
//				}
//				
//				type = prismContext.getSchemaRegistry().findObjectDefinitionByType(((TypeFilter) f).getType()).getCompileTimeClass();
//				
//				ObjectFilter subFilter = ((TypeFilter) f).getFilter();
//				if (subFilter instanceof PropertyValueFilter || subFilter instanceof InOidFilter){
//					if (containsExpression(subFilter)){
//						q = ObjectQuery.createObjectQuery(subFilter);
//						Task task = taskManager.createTaskInstance();
//						query = ExpressionUtil.evaluateQueryExpressions(q, variables, expressionFactory, prismContext, "parsing expression values for report", task, task.getResult());
//					} 
//				} 
//				
//				if (query == null) {
//					query = ObjectQuery.createObjectQuery(subFilter);
//				}
//				
//				LOGGER.info("query dump {}", query.debugDump());
//			} else if (s.startsWith("<code")){
//				String normalized = s.replace("<code>", "");
//				script = normalized.replace("</code>", "");
//				
//			}
//			
//		} catch (SchemaException e) {
//			throw new RuntimeException(e);
//			
//		} catch (ObjectNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ExpressionEvaluationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		}
		
	}
	
//	private boolean containsExpression(ObjectFilter subFilter){
//		if (subFilter instanceof PropertyValueFilter){
//			return ((PropertyValueFilter) subFilter).getExpression() != null;
//		} else if (subFilter instanceof InOidFilter){
//			return ((InOidFilter) subFilter).getExpression() != null;
//		}
//		
//		return false;
//	}
	
	public MidPointQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parametersMap, ReportService reportService){
		super(jasperReportsContext, dataset, parametersMap);
		
		
		
	}
	
	protected MidPointQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parametersMap) {
		super(jasperReportsContext, dataset, parametersMap);
		
		JRFillParameter fillparam = (JRFillParameter) parametersMap.get(JRParameter.REPORT_PARAMETERS_MAP);
		Map reportParams = (Map) fillparam.getValue();
//		prismContext = (PrismContext) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT);
//		taskManager = (TaskManager) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_TASK_MANAGER);
//		expressionFactory = (ExpressionFactory) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_EXPRESSION_FACTORY);
//		objectResolver = (ObjectResolver) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_OBJECT_RESOLVER);
//		midpointFunctions = (MidpointFunctions) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_FUNCTION);
//		auditService = (AuditService) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_AUDIT_SERVICE);
//		reportFunctions = (ReportFunctions) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_REPORT_FUNCTIONS);
		reportService = (ReportService) parametersMap.get(ReportService.PARAMETER_REPORT_SERVICE).getValue();
		
//		if (prismContext == null){
//			prismContext = (PrismContext) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT).getValue();
//			taskManager = (TaskManager) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_TASK_MANAGER).getValue();
//			objectResolver = (ObjectResolver) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_OBJECT_RESOLVER).getValue();
//			expressionFactory = (ExpressionFactory) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_EXPRESSION_FACTORY).getValue();
//			midpointFunctions = (MidpointFunctions) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_FUNCTION).getValue();
//			
//		}
//		if (SecurityContextHolder.getContext().getAuthentication() == null){
//			Authentication principal = (Authentication) reportParams.get("principal");
//			if (principal != null){
//				SecurityContextHolder.getContext().setAuthentication(principal);
//			}
//		}
		parseQuery();
//		 TODO Auto-generated constructor stub
	}

	@Override
	public JRDataSource createDatasource() throws JRException {
		// TODO Auto-generated method stub
		
		
//		Task task = taskManager.createTaskInstance();
		
//		OperationResult parentResult = task.getResult();
		Collection<PrismObject<? extends ObjectType>> results = new ArrayList<>();
//		Class<? extends ObjectType> clazz = UserType.class;
	
		
		try {
		
			if (query == null && script == null){
				throw new JRException("Neither query, nor script defined in the report.");
			}
			
			if (query != null){
				results = reportService.searchObjects(query, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
//				results = ReportUtils.getReportData(model, type, query, task, parentResult);
			} else {
				results = reportService.evaluateScript(script, getParameters());
//				results = ReportUtils.getReportData(prismContext, task, reportFunctions, script, variables, objectResolver);
			}
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
				| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			throw new JRException(e);
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
	
	

}
