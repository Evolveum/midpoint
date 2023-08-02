/*
 * Copyright (C) 2013-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.DirectoryFileObjectResolver;

import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationTestUtil;
import com.evolveum.midpoint.model.common.ConstantsManager;
import com.evolveum.midpoint.model.common.expression.evaluator.ConstExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.evaluator.path.PathExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
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

import org.xml.sax.SAXException;

/**
 * @author Radovan Semancik
 */
public class ExpressionTestUtil {

    private static final String CONST_FOO_NAME = "foo";
    public static final String CONST_FOO_VALUE = "foobar";

    private static Protector createInitializedProtector(PrismContext prismContext) {
        return KeyStoreBasedProtectorBuilder.create(prismContext)
                .keyStorePath(MidPointTestConstants.KEYSTORE_PATH)
                .keyStorePassword(MidPointTestConstants.KEYSTORE_PASSWORD)
                .initialize();
    }

    private static ExpressionFactory createInitializedExpressionFactory(
            ObjectResolver resolver, Protector protector, PrismContext prismContext, Clock clock) {

        ExpressionFactory expressionFactory =
                new ExpressionFactory(LocalizationTestUtil.getLocalizationService());
        expressionFactory.setObjectResolver(resolver);

        // NOTE: we need to register the evaluator factories to expressionFactory manually here
        // this is not spring-wired test. PostConstruct methods are not invoked here

        // asIs
        AsIsExpressionEvaluatorFactory asIsFactory = new AsIsExpressionEvaluatorFactory(protector);
        expressionFactory.registerEvaluatorFactory(asIsFactory);
        expressionFactory.setDefaultEvaluatorFactory(asIsFactory);

        // value
        expressionFactory.registerEvaluatorFactory(
                new LiteralExpressionEvaluatorFactory(protector));

        // const
        ConstantsManager constManager = new ConstantsManager(createConfiguration());
        expressionFactory.registerEvaluatorFactory(
                new ConstExpressionEvaluatorFactory(protector, constManager));

        // path
        var pathFactory = new PathExpressionEvaluatorFactory(expressionFactory, protector);
        pathFactory.setObjectResolver(resolver);
        expressionFactory.registerEvaluatorFactory(pathFactory);

        // generate
        ValuePolicyProcessor valuePolicyProcessor = new ValuePolicyProcessor(expressionFactory);
        GenerateExpressionEvaluatorFactory generateFactory =
                new GenerateExpressionEvaluatorFactory(expressionFactory, protector, valuePolicyProcessor);
        generateFactory.setObjectResolver(resolver);
        expressionFactory.registerEvaluatorFactory(generateFactory);

        // script
        Collection<FunctionLibraryBinding> functions = new ArrayList<>();
        functions.add(FunctionLibraryUtil.createBasicFunctionLibraryBinding(prismContext, protector, clock));
        functions.add(FunctionLibraryUtil.createLogFunctionLibraryBinding(prismContext));
        ScriptExpressionFactory scriptExpressionFactory = new ScriptExpressionFactory(functions, resolver);

        scriptExpressionFactory.registerEvaluator(
                new GroovyScriptEvaluator(
                        prismContext, protector, LocalizationTestUtil.getLocalizationService()));

        Jsr223ScriptEvaluator jsEvaluator = new Jsr223ScriptEvaluator(
                "ECMAScript", prismContext, protector, LocalizationTestUtil.getLocalizationService());
        if (jsEvaluator.isInitialized()) {
            scriptExpressionFactory.registerEvaluator(jsEvaluator);
        }

        expressionFactory.registerEvaluatorFactory(
                new ScriptExpressionEvaluatorFactory(scriptExpressionFactory));

        return expressionFactory;
    }

    private static Configuration createConfiguration() {
        BaseConfiguration config = new BaseConfiguration();
        config.addProperty(CONST_FOO_NAME, CONST_FOO_VALUE);
        return config;
    }

    public static ModelCommonBeans initializeModelCommonBeans() throws SchemaException, IOException, SAXException {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();

        ObjectResolver resolver = new DirectoryFileObjectResolver(MidPointTestConstants.OBJECTS_DIR);
        Protector protector = ExpressionTestUtil.createInitializedProtector(prismContext);
        Clock clock = new Clock();

        ((PrismContextImpl) prismContext).setDefaultProtector(protector);
        ExpressionFactory expressionFactory =
                ExpressionTestUtil.createInitializedExpressionFactory(resolver, protector, prismContext, clock);

        ModelCommonBeans modelCommonBeans = new ModelCommonBeans();
        modelCommonBeans.expressionFactory = expressionFactory;
        modelCommonBeans.objectResolver = resolver;
        modelCommonBeans.protector = protector;
        modelCommonBeans.prismContext = prismContext;
        modelCommonBeans.init();

        return modelCommonBeans;
    }
}
