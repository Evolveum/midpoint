package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
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
import net.sf.jasperreports.engine.fill.JRFillParameter;
import net.sf.jasperreports.engine.query.JRAbstractQueryExecuter;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class MidPointQueryExecutor extends JRAbstractQueryExecuter{
	
	private static final Trace LOGGER = TraceManager.getTrace(MidPointQueryExecutor.class);
	PrismContext prismContext;
	ModelService model;
	TaskManager taskManager;
	ExpressionFactory expressionFactory;
	ObjectQuery query;
	Class type;
	
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
		
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinitions(expressionParameters);
		
		LOGGER.info("query: " + s);
		ObjectQuery q;
		if (StringUtils.isEmpty(s)){
			q = null;
		} else {
			
		try {
			SearchFilterType filter = (SearchFilterType) prismContext.parseAtomicValue(s, SearchFilterType.COMPLEX_TYPE);
			LOGGER.info("filter {}", filter);
			ObjectFilter f = QueryConvertor.parseFilter(filter, UserType.class, prismContext);
			LOGGER.info("f {}", f.debugDump());
			if (!(f instanceof TypeFilter)){
				throw new IllegalArgumentException("Defined query must contain type. Use 'type filter' in your report query.");
			}
			
			type = prismContext.getSchemaRegistry().findObjectDefinitionByType(((TypeFilter) f).getType()).getCompileTimeClass();
			
			ObjectFilter subFilter = ((TypeFilter) f).getFilter();
			if (subFilter instanceof PropertyValueFilter){
				if (((PropertyValueFilter) subFilter).getExpression() != null){
					q = ObjectQuery.createObjectQuery(subFilter);
					Task task = taskManager.createTaskInstance();
					query = ExpressionUtil.evaluateQueryExpressions(q, variables, expressionFactory, prismContext, "parsing expression values for report", task, task.getResult());
				} 
			} 
			
			if (query == null) {
				query = ObjectQuery.createObjectQuery(subFilter);
			}
			
			LOGGER.info("query dump {}", query.debugDump());
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
	
	protected MidPointQueryExecutor(JasperReportsContext jasperReportsContext, JRDataset dataset,
			Map<String, ? extends JRValueParameter> parametersMap) {
		super(jasperReportsContext, dataset, parametersMap);
		JRFillParameter fillparam = (JRFillParameter) parametersMap.get(JRParameter.REPORT_PARAMETERS_MAP);
		Map reportParams = (Map) fillparam.getValue();
		prismContext = (PrismContext) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT);
		taskManager = (TaskManager) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_TASK_MANAGER);
		expressionFactory = (ExpressionFactory) reportParams.get(MidPointQueryExecutorFactory.PARAMETER_EXPRESSION_FACTORY);
		model = (ModelService) parametersMap.get(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_CONNECTION).getValue();
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
		if (isSearchByOid(query.getFilter())){
			String oid = getOid(query.getFilter());
			PrismObject result = model.getObject(type, oid, null, task, parentResult);
			results.add(result);
			
		} else{
		
		
			results = model.searchObjects(type, query, null, task, parentResult);;
		
		}
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
				| CommunicationException | ConfigurationException e) {
			// TODO Auto-generated catch block
			throw new JRException(e);
		}
		
		MidPointDataSource mds = new MidPointDataSource(results);
		
		return mds;
	}
	
	private boolean isSearchByOid(ObjectFilter filter){
//		ObjectFilter filter = query.getFilter();
		if (filter instanceof TypeFilter){
			return isSearchByOid(((TypeFilter) filter).getFilter());
		} else if (filter instanceof LogicalFilter){
			for (ObjectFilter f : ((LogicalFilter) filter).getConditions()){
				boolean isSearchByOid = isSearchByOid(f);
				if (isSearchByOid){
					return true;
				}
			}
		}
		if (filter instanceof PropertyValueFilter){
			if (QNameUtil.match(((PropertyValueFilter) filter).getPath().lastNamed().getName(), new QName("oid"))){
				return true;
			}
		}
		
		return false;
	}
	
	private String getOid(ObjectFilter filter){
		if (filter instanceof TypeFilter){
			return getOid(((TypeFilter) filter).getFilter());
		} else if (filter instanceof LogicalFilter){
			for (ObjectFilter f : ((LogicalFilter) filter).getConditions()){
				String oid = getOid(f);
				if (oid != null){
					return oid;
				}
			}
		}
		if (filter instanceof PropertyValueFilter){
			if (QNameUtil.match(((PropertyValueFilter) filter).getPath().lastNamed().getName(), new QName("oid"))){
				return (String) ((PrismPropertyValue)((PropertyValueFilter) filter).getValues().iterator().next()).getValue();
			}
		}
		
		return null;
	}

	@Override
	public void close() {
//		throw new UnsupportedOperationException("QueryExecutor.close() not supported");
		//nothing to DO
		System.out.println("query executer close()");
		
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
