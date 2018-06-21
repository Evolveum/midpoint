/*
 * Copyright (c) 2013-2017 Evolveum
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
package com.evolveum.midpoint.model.common.expression;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.common.LocalizationTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.evaluator.AsIsExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.ConstantsManager;
import com.evolveum.midpoint.model.common.expression.evaluator.ConstExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.PathExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryUtil;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.xpath.XPathScriptEvaluator;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.ProtectorImpl;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.test.util.MidPointTestConstants;

/**
 * @author Radovan Semancik
 *
 */
public class ExpressionTestUtil {

	public static ProtectorImpl createInitializedProtector(PrismContext prismContext) {
		ProtectorImpl protector = new ProtectorImpl();
        protector.setKeyStorePath(MidPointTestConstants.KEYSTORE_PATH);
        protector.setKeyStorePassword(MidPointTestConstants.KEYSTORE_PASSWORD);
        protector.init();
        return protector;
	}

	public static ExpressionFactory createInitializedExpressionFactory(ObjectResolver resolver, ProtectorImpl protector,
			PrismContext prismContext, SecurityContextManager securityContextManager, RepositoryService repositoryService) {
    	ExpressionFactory expressionFactory = new ExpressionFactory(securityContextManager, prismContext, LocalizationTestUtil.getLocalizationService());
    	expressionFactory.setObjectResolver(resolver);

    	// NOTE: we need to register the evaluator factories to expressionFactory manually here
    	// this is not spring-wired test. PostConstruct methods are not invoked here
    	
    	// asIs
    	AsIsExpressionEvaluatorFactory asIsFactory = new AsIsExpressionEvaluatorFactory(prismContext, protector);
    	expressionFactory.registerEvaluatorFactory(asIsFactory);
    	expressionFactory.setDefaultEvaluatorFactory(asIsFactory);

    	// value
    	LiteralExpressionEvaluatorFactory valueFactory = new LiteralExpressionEvaluatorFactory(prismContext);
    	expressionFactory.registerEvaluatorFactory(valueFactory);

		// const
    	ConstantsManager constManager = new ConstantsManager(createConfiguration());
    	ConstExpressionEvaluatorFactory constFactory = new ConstExpressionEvaluatorFactory(protector, constManager, prismContext);
    	expressionFactory.registerEvaluatorFactory(constFactory);

    	// path
    	PathExpressionEvaluatorFactory pathFactory = new PathExpressionEvaluatorFactory(expressionFactory, prismContext, protector);
    	pathFactory.setObjectResolver(resolver);
    	expressionFactory.registerEvaluatorFactory(pathFactory);

    	// generate
    	ValuePolicyProcessor valuePolicyGenerator = new ValuePolicyProcessor();
    	valuePolicyGenerator.setExpressionFactory(expressionFactory);
    	GenerateExpressionEvaluatorFactory generateFactory = new GenerateExpressionEvaluatorFactory(expressionFactory, protector, valuePolicyGenerator, prismContext);
    	generateFactory.setObjectResolver(resolver);
    	expressionFactory.registerEvaluatorFactory(generateFactory);

    	// script
    	Collection<FunctionLibrary> functions = new ArrayList<>();
        functions.add(FunctionLibraryUtil.createBasicFunctionLibrary(prismContext, protector));
        functions.add(FunctionLibraryUtil.createLogFunctionLibrary(prismContext));
        ScriptExpressionFactory scriptExpressionFactory = new ScriptExpressionFactory(prismContext, protector, repositoryService);
        scriptExpressionFactory.setObjectResolver(resolver);
        scriptExpressionFactory.setFunctions(functions);
        XPathScriptEvaluator xpathEvaluator = new XPathScriptEvaluator(prismContext);
        scriptExpressionFactory.registerEvaluator(XPathScriptEvaluator.XPATH_LANGUAGE_URL, xpathEvaluator);
        Jsr223ScriptEvaluator groovyEvaluator = new Jsr223ScriptEvaluator("Groovy", prismContext, protector, LocalizationTestUtil.getLocalizationService());
        scriptExpressionFactory.registerEvaluator(groovyEvaluator.getLanguageUrl(), groovyEvaluator);
        ScriptExpressionEvaluatorFactory scriptExpressionEvaluatorFactory = new ScriptExpressionEvaluatorFactory(scriptExpressionFactory, securityContextManager);
        expressionFactory.registerEvaluatorFactory(scriptExpressionEvaluatorFactory);

        return expressionFactory;
	}

	private static Configuration createConfiguration() {
    	BaseConfiguration config = new BaseConfiguration();
    	config.addProperty("foo", "foobar");
		return config;
	}

}
