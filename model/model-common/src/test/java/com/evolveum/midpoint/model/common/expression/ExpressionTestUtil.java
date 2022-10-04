/*
 * Copyright (C) 2013-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationTestUtil;
import com.evolveum.midpoint.model.common.ConstantsManager;
import com.evolveum.midpoint.model.common.expression.evaluator.ConstExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.path.PathExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryUtil;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.common.expression.script.groovy.GroovyScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.jsr223.Jsr223ScriptEvaluator;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.KeyStoreBasedProtectorBuilder;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.evaluator.AsIsExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.test.util.MidPointTestConstants;

/**
 * @author Radovan Semancik
 */
public class ExpressionTestUtil {

    private static final String CONST_FOO_NAME = "foo";
    public static final String CONST_FOO_VALUE = "foobar";

    public static Protector createInitializedProtector(PrismContext prismContext) {
        return KeyStoreBasedProtectorBuilder.create(prismContext)
                .keyStorePath(MidPointTestConstants.KEYSTORE_PATH)
                .keyStorePassword(MidPointTestConstants.KEYSTORE_PASSWORD)
                .initialize();
    }

    public static ExpressionFactory createInitializedExpressionFactory(
            ObjectResolver resolver, Protector protector, PrismContext prismContext, Clock clock) {
        ExpressionFactory expressionFactory = new ExpressionFactory(
                null, prismContext, LocalizationTestUtil.getLocalizationService());
        expressionFactory.setObjectResolver(resolver);

        // NOTE: we need to register the evaluator factories to expressionFactory manually here
        // this is not spring-wired test. PostConstruct methods are not invoked here

        // asIs
        AsIsExpressionEvaluatorFactory asIsFactory =
                new AsIsExpressionEvaluatorFactory(prismContext, protector);
        expressionFactory.registerEvaluatorFactory(asIsFactory);
        expressionFactory.setDefaultEvaluatorFactory(asIsFactory);

        // value
        LiteralExpressionEvaluatorFactory valueFactory =
                new LiteralExpressionEvaluatorFactory(prismContext);
        expressionFactory.registerEvaluatorFactory(valueFactory);

        // const
        ConstantsManager constManager = new ConstantsManager(createConfiguration());
        ConstExpressionEvaluatorFactory constFactory =
                new ConstExpressionEvaluatorFactory(protector, constManager, prismContext);
        expressionFactory.registerEvaluatorFactory(constFactory);

        // path
        PathExpressionEvaluatorFactory pathFactory = new PathExpressionEvaluatorFactory(
                expressionFactory, prismContext, protector, null);
        pathFactory.setObjectResolver(resolver);
        expressionFactory.registerEvaluatorFactory(pathFactory);

        // generate
        ValuePolicyProcessor valuePolicyGenerator = new ValuePolicyProcessor();
        valuePolicyGenerator.setExpressionFactory(expressionFactory);
        GenerateExpressionEvaluatorFactory generateFactory =
                new GenerateExpressionEvaluatorFactory(
                        expressionFactory, protector, valuePolicyGenerator, prismContext, null);
        generateFactory.setObjectResolver(resolver);
        expressionFactory.registerEvaluatorFactory(generateFactory);

        // script
        Collection<FunctionLibrary> functions = new ArrayList<>();
        functions.add(FunctionLibraryUtil.createBasicFunctionLibrary(prismContext, protector, clock));
        functions.add(FunctionLibraryUtil.createLogFunctionLibrary(prismContext));
        ScriptExpressionFactory scriptExpressionFactory = new ScriptExpressionFactory(functions, resolver);

        GroovyScriptEvaluator groovyEvaluator = new GroovyScriptEvaluator(
                prismContext, protector, LocalizationTestUtil.getLocalizationService());
        scriptExpressionFactory.registerEvaluator(groovyEvaluator);

        Jsr223ScriptEvaluator jsEvaluator = new Jsr223ScriptEvaluator(
                "ECMAScript", prismContext, protector, LocalizationTestUtil.getLocalizationService());
        if (jsEvaluator.isInitialized()) {
            scriptExpressionFactory.registerEvaluator(jsEvaluator);
        }

        ScriptExpressionEvaluatorFactory scriptExpressionEvaluatorFactory =
                new ScriptExpressionEvaluatorFactory(
                        scriptExpressionFactory, null, prismContext);
        expressionFactory.registerEvaluatorFactory(scriptExpressionEvaluatorFactory);

        return expressionFactory;
    }

    private static Configuration createConfiguration() {
        BaseConfiguration config = new BaseConfiguration();
        config.addProperty(CONST_FOO_NAME, CONST_FOO_VALUE);
        return config;
    }
}
