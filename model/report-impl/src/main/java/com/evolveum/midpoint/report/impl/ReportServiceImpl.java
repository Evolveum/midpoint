/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
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

@Component
public class ReportServiceImpl implements ReportService {

	private static final transient Trace LOGGER = TraceManager.getTrace(ReportServiceImpl.class);

	@Autowired(required = true)
	private ModelService model;

	@Autowired(required = true)
	private TaskManager taskManager;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

	@Autowired(required = true)
	private ObjectResolver objectResolver;

	@Autowired(required = true)
	private AuditService auditService;
	
	@Autowired(required = true)
	private FunctionLibrary logFunctionLibrary;
	
	@Autowired(required = true)
	private FunctionLibrary basicFunctionLibrary;
	
	@Autowired(required = true)
	private FunctionLibrary midpointFunctionLibrary;

	@Override
	public ObjectQuery parseQuery(String query, Map<QName, Object> parameters) throws SchemaException,
			ObjectNotFoundException, ExpressionEvaluationException {
		if (StringUtils.isBlank(query)) {
			return null;
		}

		ObjectQuery parsedQuery = null;
		try {
			Task task = taskManager.createTaskInstance();
			ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, task.getResult()));
			SearchFilterType filter = prismContext.parserFor(query).parseRealValue(SearchFilterType.class);
			LOGGER.trace("filter {}", filter);
			ObjectFilter f = QueryConvertor.parseFilter(filter, UserType.class, prismContext);
			LOGGER.trace("f {}", f.debugDump());
			if (!(f instanceof TypeFilter)) {
				throw new IllegalArgumentException(
						"Defined query must contain type. Use 'type filter' in your report query.");
			}

			ObjectFilter subFilter = ((TypeFilter) f).getFilter();
			ObjectQuery q = ObjectQuery.createObjectQuery(subFilter);
			
			ExpressionVariables variables = new ExpressionVariables();
			variables.addVariableDefinitions(parameters);
			
			q = ExpressionUtil.evaluateQueryExpressions(q, variables, expressionFactory, prismContext,
					"parsing expression values for report", task, task.getResult());
			((TypeFilter) f).setFilter(q.getFilter());
			parsedQuery = ObjectQuery.createObjectQuery(f);

			LOGGER.trace("query dump {}", parsedQuery.debugDump());
		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
		return parsedQuery;

	}

	@Override
	public Collection<PrismObject<? extends ObjectType>> searchObjects(ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException,
			ObjectNotFoundException, SecurityViolationException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException {
		// List<PrismObject<? extends ObjectType>> results = new ArrayList<>();

		// GetOperationOptions options = GetOperationOptions.createRaw();

		if (!(query.getFilter() instanceof TypeFilter)) {
			throw new IllegalArgumentException("Query must contain type filter.");
		}

		TypeFilter typeFilter = (TypeFilter) query.getFilter();
		QName type = typeFilter.getType();
		Class clazz = prismContext.getSchemaRegistry().determineCompileTimeClass(type);
		if (clazz == null) {
			clazz = prismContext.getSchemaRegistry().findObjectDefinitionByType(type).getCompileTimeClass();
		}

		ObjectQuery queryForSearch = ObjectQuery.createObjectQuery(typeFilter.getFilter());

		Task task = taskManager.createTaskInstance(ReportService.class.getName() + ".searchObjects()");
		OperationResult parentResult = task.getResult();

		// options.add(new
		// SelectorOptions(GetOperationOptions.createResolveNames()));
		GetOperationOptions getOptions = GetOperationOptions.createResolveNames();
		getOptions.setRaw(Boolean.TRUE);
		options = SelectorOptions.createCollection(getOptions);
		List<PrismObject<? extends ObjectType>> results;
		try {
			results = model.searchObjects(clazz, queryForSearch, options, task, parentResult);
			return results;
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
				| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			throw e;
		}

	}

	public Collection<PrismContainerValue<? extends Containerable>> evaluateScript(String script,
			Map<QName, Object> parameters) throws SchemaException, ExpressionEvaluationException,
			ObjectNotFoundException {
		List<PrismContainerValue<? extends Containerable>> results = new ArrayList<>();

		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinitions(parameters);

		// special variable for audit report
		variables.addVariableDefinition(new QName("auditParams"), getConvertedParams(parameters));

		Task task = taskManager.createTaskInstance(ReportService.class.getName() + ".evaluateScript");
		OperationResult parentResult = task.getResult();

		Collection<FunctionLibrary> functions = createFunctionLibraries();

		Jsr223ScriptEvaluator scripts = new Jsr223ScriptEvaluator("Groovy", prismContext,
				prismContext.getDefaultProtector());
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, task.getResult()));
		Object o = null;
		try{
			o = scripts.evaluateReportScript(script, variables, objectResolver, functions, "desc",
				parentResult);
		} finally{
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
		if (o != null) {

			if (Collection.class.isAssignableFrom(o.getClass())) {
				Collection resultSet = (Collection) o;
				if (resultSet != null && !resultSet.isEmpty()) {
					for (Object obj : resultSet) {
						results.add(convertResultingObject(obj));
					}
				}

			} else {
				results.add(convertResultingObject(o));
			}
		}

		return results;
	}

	protected PrismContainerValue convertResultingObject(Object obj) {
		if (obj instanceof PrismObject) {
            return ((PrismObject) obj).asObjectable().asPrismContainerValue();
        } else if (obj instanceof Objectable) {
            return ((Objectable) obj).asPrismContainerValue();
        } else if (obj instanceof PrismContainerValue) {
            return (PrismContainerValue) obj;
        } else if (obj instanceof Containerable) {
            return ((Containerable) obj).asPrismContainerValue();
        } else {
            throw new IllegalStateException("Reporting script should return something compatible with PrismContainerValue, not a " + obj.getClass());
        }
	}

	public Collection<AuditEventRecord> evaluateAuditScript(String script, Map<QName, Object> parameters)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		Collection<AuditEventRecord> results = new ArrayList<AuditEventRecord>();

		ExpressionVariables variables = new ExpressionVariables();
			variables.addVariableDefinition(new QName("auditParams"), getConvertedParams(parameters));

		Task task = taskManager.createTaskInstance(ReportService.class.getName() + ".searchObjects()");
		OperationResult parentResult = task.getResult();

		Collection<FunctionLibrary> functions = createFunctionLibraries();

		Jsr223ScriptEvaluator scripts = new Jsr223ScriptEvaluator("Groovy", prismContext,
				prismContext.getDefaultProtector());
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, task.getResult()));
		Object o = null;
		try{
			o = scripts.evaluateReportScript(script, variables, objectResolver, functions, "desc",
				parentResult);
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
		if (o != null) {

			if (Collection.class.isAssignableFrom(o.getClass())) {
				Collection resultSet = (Collection) o;
				if (resultSet != null && !resultSet.isEmpty()) {
					for (Object obj : resultSet) {
						if (!(obj instanceof AuditEventRecord)) {
							LOGGER.warn("Skipping result, not an audit event record " + obj);
							continue;
						}
						results.add((AuditEventRecord) obj);
					}

				}

			} else {
				results.add((AuditEventRecord) o);
			}
		}

		return results;
	}

	private Map<String, Object> getConvertedParams(Map<QName, Object> parameters) {
		if (parameters == null) {
			return null;
		}

		Map<String, Object> resultParams = new HashMap<String, Object>();
		Set<Entry<QName, Object>> paramEntries = parameters.entrySet();
		for (Entry<QName, Object> e : paramEntries) {
			if (e.getValue() instanceof PrismPropertyValue) {
				resultParams.put(e.getKey().getLocalPart(), ((PrismPropertyValue) e.getValue()).getValue());
			} else {
				resultParams.put(e.getKey().getLocalPart(), e.getValue());
			}
		}

		return resultParams;
	}

	private Collection<FunctionLibrary> createFunctionLibraries() {
//		FunctionLibrary functionLib = ExpressionUtil.createBasicFunctionLibrary(prismContext,
//				prismContext.getDefaultProtector());
		FunctionLibrary midPointLib = new FunctionLibrary();
		midPointLib.setVariableName("report");
		midPointLib.setNamespace("http://midpoint.evolveum.com/xml/ns/public/function/report-3");
		ReportFunctions reportFunctions = new ReportFunctions(prismContext, model, taskManager, auditService);
		midPointLib.setGenericFunctions(reportFunctions);
//		
//		MidpointFunctionsImpl mp = new MidpointFunctionsImpl();
//		mp.

		Collection<FunctionLibrary> functions = new ArrayList<>();
		functions.add(basicFunctionLibrary);
		functions.add(logFunctionLibrary);
		functions.add(midpointFunctionLibrary);
		functions.add(midPointLib);
		return functions;
	}
}
