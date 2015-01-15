package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataset;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRValueParameter;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.base.JRBaseParameter;
import net.sf.jasperreports.engine.data.JRBeanArrayDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.fill.JRFillParameter;
import net.sf.jasperreports.engine.query.JRAbstractQueryExecuter;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class MidPointQueryExecutor extends JRAbstractQueryExecuter{
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointQueryExecutor.class);
	PrismContext prismContext;
	ModelService model;
	TaskManager taskManager;
	ExpressionFactory expressionFactory;
	ObjectResolver objectResolver;
	ObjectQuery query;
	String script;
	Class type;
	ExpressionVariables variables;
	MidpointFunctions midpointFunctions;
	AuditService auditService;
	ReportFunctions reportFunctions;
	
	@Override
	protected void parseQuery() {
		// TODO Auto-generated method stub
		String s = dataset.getQuery().getText();
		JRBaseParameter p = (JRBaseParameter) dataset.getParameters()[0];
//		LOGGER.info("dataset param: {}", p.);
		
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
		
		variables = new ExpressionVariables();
		variables.addVariableDefinitions(expressionParameters);

		LOGGER.info("query: " + s);
		ObjectQuery q;
		if (StringUtils.isEmpty(s)){
			q = null;
		} else {
			
		try {
			if (s.startsWith("<filter")){
				SearchFilterType filter = (SearchFilterType) prismContext.parseAtomicValue(s, SearchFilterType.COMPLEX_TYPE);
				LOGGER.info("filter {}", filter);
				ObjectFilter f = QueryConvertor.parseFilter(filter, UserType.class, prismContext);
				LOGGER.info("f {}", f.debugDump());
				if (!(f instanceof TypeFilter)){
					throw new IllegalArgumentException("Defined query must contain type. Use 'type filter' in your report query.");
				}
				
				type = prismContext.getSchemaRegistry().findObjectDefinitionByType(((TypeFilter) f).getType()).getCompileTimeClass();
				
				ObjectFilter subFilter = ((TypeFilter) f).getFilter();
				if (subFilter instanceof PropertyValueFilter || subFilter instanceof InOidFilter){
					if (containsExpression(subFilter)){
						q = ObjectQuery.createObjectQuery(subFilter);
						Task task = taskManager.createTaskInstance();
						query = ExpressionUtil.evaluateQueryExpressions(q, variables, expressionFactory, prismContext, "parsing expression values for report", task, task.getResult());
					} 
				} 
				
				if (query == null) {
					query = ObjectQuery.createObjectQuery(subFilter);
				}
				
				LOGGER.info("query dump {}", query.debugDump());
			} else if (s.startsWith("<code")){
				String normalized = s.replace("<code>", "");
				script = normalized.replace("</code>", "");
				
			}
			
		} catch (SchemaException e) {
			throw new RuntimeException(e);
			
		} catch (ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		
	}
	
	private boolean containsExpression(ObjectFilter subFilter){
		if (subFilter instanceof PropertyValueFilter){
			return ((PropertyValueFilter) subFilter).getExpression() != null;
		} else if (subFilter instanceof InOidFilter){
			return ((InOidFilter) subFilter).getExpression() != null;
		}
		
		return false;
	}
	
	protected MidPointQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parametersMap) {
		super(jasperReportsContext, dataset, parametersMap);
		
		JRFillParameter fillparam = (JRFillParameter) parametersMap.get(JRParameter.REPORT_PARAMETERS_MAP);
		Map reportParams = (Map) fillparam.getValue();
		prismContext = (PrismContext) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT);
		taskManager = (TaskManager) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_TASK_MANAGER);
		expressionFactory = (ExpressionFactory) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_EXPRESSION_FACTORY);
		objectResolver = (ObjectResolver) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_OBJECT_RESOLVER);
		midpointFunctions = (MidpointFunctions) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_FUNCTION);
		auditService = (AuditService) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_AUDIT_SERVICE);
		reportFunctions = (ReportFunctions) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_REPORT_FUNCTIONS);
		model = (ModelService) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_CONNECTION).getValue();
		
		if (prismContext == null){
			prismContext = (PrismContext) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT).getValue();
			taskManager = (TaskManager) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_TASK_MANAGER).getValue();
			objectResolver = (ObjectResolver) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_OBJECT_RESOLVER).getValue();
			expressionFactory = (ExpressionFactory) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_EXPRESSION_FACTORY).getValue();
			midpointFunctions = (MidpointFunctions) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_FUNCTION).getValue();
			
		}
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
		
		
		Task task = taskManager.createTaskInstance();
		
		OperationResult parentResult = task.getResult();
		List<PrismObject<? extends ObjectType>> results = results = new ArrayList<>();
//		Class<? extends ObjectType> clazz = UserType.class;
	
		
		try {
			if (query == null && script == null){
				throw new JRException("Neither query, nor script defined in the report.");
			}
			if (script != null){
				FunctionLibrary functionLib = ExpressionUtil.createBasicFunctionLibrary(prismContext, prismContext.getDefaultProtector());
				FunctionLibrary midPointLib = new FunctionLibrary();
				midPointLib.setVariableName("report");
				midPointLib.setNamespace("http://midpoint.evolveum.com/xml/ns/public/function/report-3");
//				ReportFunctions reportFunctions = new ReportFunctions(prismContext, model, taskManager, auditService);
				midPointLib.setGenericFunctions(reportFunctions);
				
				Collection<FunctionLibrary> functions = new ArrayList<>();
				functions.add(functionLib);
				
				
				functions.add(midPointLib);
				Jsr223ScriptEvaluator scripts = new Jsr223ScriptEvaluator("Groovy", prismContext, prismContext.getDefaultProtector());
				Object o = scripts.evaluateReportScript(script, variables, objectResolver, functions, "desc", task.getResult());
				if (o != null){

					if (Collection.class.isAssignableFrom(o.getClass())) {
						Collection resultSet = (Collection) o;
						if (resultSet != null && !resultSet.isEmpty()){
							if (resultSet.iterator().next() instanceof PrismObject){
								results.addAll((Collection<? extends PrismObject<? extends ObjectType>>) o);
							} else {
								return new JRBeanCollectionDataSource(resultSet);
							}
						}
						
					} else {
						results.add((PrismObject) o);
					}
				}
			}else{
				
				GetOperationOptions options = GetOperationOptions.createRaw();
				options.setResolveNames(true);
			results = model.searchObjects(type, query, SelectorOptions.createCollection(options), task, parentResult);;
		
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
