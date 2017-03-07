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

import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.evaluator.AsIsExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.PathExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.xpath.XPathScriptEvaluator;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyGenerator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.ProtectorImpl;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
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
        //protector.setPrismContext(prismContext);
        protector.init();
        return protector;
	}
	
	public static ExpressionFactory createInitializedExpressionFactory(ObjectResolver resolver, ProtectorImpl protector, 
			PrismContext prismContext, SecurityEnforcer securityEnforcer) {
    	ExpressionFactory expressionFactory = new ExpressionFactory(resolver, prismContext);
    	
    	// asIs
    	AsIsExpressionEvaluatorFactory asIsFactory = new AsIsExpressionEvaluatorFactory(prismContext, protector);
    	expressionFactory.addEvaluatorFactory(asIsFactory);
    	expressionFactory.setDefaultEvaluatorFactory(asIsFactory);

    	// value
    	LiteralExpressionEvaluatorFactory valueFactory = new LiteralExpressionEvaluatorFactory(prismContext);
    	expressionFactory.addEvaluatorFactory(valueFactory);
    	
    	// path
    	PathExpressionEvaluatorFactory pathFactory = new PathExpressionEvaluatorFactory(prismContext, resolver, protector);
    	expressionFactory.addEvaluatorFactory(pathFactory);
    	
    	// generate
    	ValuePolicyGenerator valuePolicyGenerator = new ValuePolicyGenerator();
    	valuePolicyGenerator.setExpressionFactory(expressionFactory);
    	GenerateExpressionEvaluatorFactory generateFactory = new GenerateExpressionEvaluatorFactory(protector, resolver, valuePolicyGenerator, prismContext);
    	expressionFactory.addEvaluatorFactory(generateFactory);

    	// script
    	Collection<FunctionLibrary> functions = new ArrayList<FunctionLibrary>();
        functions.add(ExpressionUtil.createBasicFunctionLibrary(prismContext, protector));
        functions.add(ExpressionUtil.createLogFunctionLibrary(prismContext));
        ScriptExpressionFactory scriptExpressionFactory = new ScriptExpressionFactory(resolver, prismContext, protector);
        scriptExpressionFactory.setFunctions(functions);
        XPathScriptEvaluator xpathEvaluator = new XPathScriptEvaluator(prismContext);
        scriptExpressionFactory.registerEvaluator(XPathScriptEvaluator.XPATH_LANGUAGE_URL, xpathEvaluator);
        Jsr223ScriptEvaluator groovyEvaluator = new Jsr223ScriptEvaluator("Groovy", prismContext, protector);
        scriptExpressionFactory.registerEvaluator(groovyEvaluator.getLanguageUrl(), groovyEvaluator);
        ScriptExpressionEvaluatorFactory scriptExpressionEvaluatorFactory = new ScriptExpressionEvaluatorFactory(scriptExpressionFactory, securityEnforcer);
        expressionFactory.addEvaluatorFactory(scriptExpressionEvaluatorFactory);
        
        return expressionFactory;
	}

}
