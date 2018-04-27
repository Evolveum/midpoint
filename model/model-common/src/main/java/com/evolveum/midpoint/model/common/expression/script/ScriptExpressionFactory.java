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
package com.evolveum.midpoint.model.common.expression.script;

import java.util.*;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.model.common.expression.functions.CustomFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.CacheRegistry;
import com.evolveum.midpoint.repo.common.Cacheable;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 *
 * @author Radovan Semancik
 *
 */
public class ScriptExpressionFactory implements Cacheable{

	private static final Trace LOGGER = TraceManager.getTrace(ScriptExpressionFactory.class);

	public static String DEFAULT_LANGUAGE = "http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy";

	private Map<String,ScriptEvaluator> evaluatorMap = new HashMap<>();
	private ObjectResolver objectResolver;
	private final PrismContext prismContext;
	private Collection<FunctionLibrary> functions;
	private final Protector protector;
	private final RepositoryService repositoryService;          // might be null during low-level testing

	private Map<String, FunctionLibrary> customFunctionLibraryCache;
	
	private CacheRegistry cahceRegistry;
	
	@PostConstruct
	public void register() {
		cahceRegistry.registerCacheableService(this);
	}
	
	public ScriptExpressionFactory(PrismContext prismContext, Protector protector, RepositoryService repositoryService) {
		this.prismContext = prismContext;
		this.protector = protector;
		this.repositoryService = repositoryService;
	}
	
	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public void setEvaluators(Collection<ScriptEvaluator> evaluators) {
		for (ScriptEvaluator evaluator: evaluators) {
			registerEvaluator(evaluator.getLanguageUrl(), evaluator);
		}
	}

	public Collection<FunctionLibrary> getFunctions() {
		return Collections.unmodifiableCollection(functions);       // MID-4396
	}

	public void setFunctions(Collection<FunctionLibrary> functions) {
		this.functions = functions;
	}

	public Map<String, ScriptEvaluator> getEvaluators() {
		return evaluatorMap;
	}
	
	public CacheRegistry geCachetRegistry() {
		return cahceRegistry;
	}
	
	public void setCacheRegistry(CacheRegistry registry) {
		this.cahceRegistry = registry;
	}

	public ScriptExpression createScriptExpression(ScriptExpressionEvaluatorType expressionType, ItemDefinition outputDefinition,
			ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult result) throws ExpressionSyntaxException {

		initializeCustomFunctionsLibraryCache(expressionFactory, task, result);
		//cache cleanup method

		ScriptExpression expression = new ScriptExpression(getEvaluator(getLanguage(expressionType), shortDesc), expressionType);
		expression.setOutputDefinition(outputDefinition);
		expression.setObjectResolver(objectResolver);
		Collection<FunctionLibrary> functionsToUse = new ArrayList<>(functions);
		functionsToUse.addAll(customFunctionLibraryCache.values());
		expression.setFunctions(functionsToUse);
		return expression;
	}

	// if performance becomes an issue, replace 'synchronized' with something more elaborate
	private synchronized void initializeCustomFunctionsLibraryCache(ExpressionFactory expressionFactory, Task task,
			OperationResult result) throws ExpressionSyntaxException {
		if (customFunctionLibraryCache != null) {
			return;
		}
		customFunctionLibraryCache = new HashMap<>();
		if (repositoryService == null) {
			LOGGER.warn("No repository service set for ScriptExpressionFactory; custom functions will not be loaded. This"
					+ " can occur during low-level testing; never during standard system execution.");
			return;
		}
		OperationResult subResult = result
				.createMinorSubresult(ScriptExpressionUtil.class.getName() + ".searchCustomFunctions");
		ResultHandler<FunctionLibraryType> functionLibraryHandler = (object, parentResult) -> {
			FunctionLibrary customLibrary = new FunctionLibrary();
			customLibrary.setVariableName(object.getName().getOrig());
			customLibrary
					.setGenericFunctions(new CustomFunctions(object.asObjectable(), expressionFactory, result, task));
			customLibrary.setNamespace(MidPointConstants.NS_FUNC_CUSTOM);
			customFunctionLibraryCache.put(object.getName().getOrig(), customLibrary);
			return true;
		};
		try {
			repositoryService.searchObjectsIterative(FunctionLibraryType.class, null, functionLibraryHandler,
					SelectorOptions.createCollection(GetOperationOptions.createReadOnly()), false, subResult);
			subResult.recordSuccessIfUnknown();
		} catch (SchemaException | RuntimeException e) {
			subResult.recordFatalError("Failed to initialize custom functions", e);
			throw new ExpressionSyntaxException(
					"An error occurred during custom libraries initialization. " + e.getMessage(), e);
		}
	}

	public void registerEvaluator(String language, ScriptEvaluator evaluator) {
		if (evaluatorMap.containsKey(language)) {
			throw new IllegalArgumentException("Evaluator for language "+language+" already registered");
		}
		evaluatorMap.put(language,evaluator);
	}

	private ScriptEvaluator getEvaluator(String language, String shortDesc) throws ExpressionSyntaxException {
		ScriptEvaluator evaluator = evaluatorMap.get(language);
		if (evaluator == null) {
			throw new ExpressionSyntaxException("Unsupported language "+language+" used in script in "+shortDesc);
		}
		return evaluator;
	}

	private String getLanguage(ScriptExpressionEvaluatorType expressionType) {
		if (expressionType.getLanguage() != null) {
			return expressionType.getLanguage();
		}
		return DEFAULT_LANGUAGE;
	}

	@Override
	public void clearCache() {
		customFunctionLibraryCache = null;		
	}
}

