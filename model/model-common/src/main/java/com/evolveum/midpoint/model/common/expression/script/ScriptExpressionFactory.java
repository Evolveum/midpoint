/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.model.common.expression.functions.CustomFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.CacheRegistry;
import com.evolveum.midpoint.repo.api.Cacheable;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Radovan Semancik
 *
 */
public class ScriptExpressionFactory implements Cacheable {

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExpressionFactory.class);

    public static final String DEFAULT_LANGUAGE = "http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy";

    private Map<String,ScriptEvaluator> evaluatorMap = new HashMap<>();
    private ObjectResolver objectResolver;
    private final PrismContext prismContext;
    private Collection<FunctionLibrary> functions;
    private final Protector protector;
    private final RepositoryService repositoryService;          // might be null during low-level testing

    private Map<String, FunctionLibrary> customFunctionLibraryCache;

    private CacheRegistry cacheRegistry;

    @PostConstruct
    public void register() {
        cacheRegistry.registerCacheableService(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCacheableService(this);
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

    public CacheRegistry geCacheRegistry() {
        return cacheRegistry;
    }

    public void setCacheRegistry(CacheRegistry registry) {
        this.cacheRegistry = registry;
    }

    public ScriptExpression createScriptExpression(ScriptExpressionEvaluatorType expressionType, ItemDefinition outputDefinition,
            ExpressionProfile expressionProfile, ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult result)
                    throws ExpressionSyntaxException, SecurityViolationException {

        initializeCustomFunctionsLibraryCache(expressionFactory, result);
        //cache cleanup method

        String language = getLanguage(expressionType);
        ScriptExpression expression = new ScriptExpression(getEvaluator(language, shortDesc), expressionType);
        expression.setPrismContext(prismContext);
        expression.setOutputDefinition(outputDefinition);
        expression.setObjectResolver(objectResolver);
        Collection<FunctionLibrary> functionsToUse = new ArrayList<>(functions);
        functionsToUse.addAll(customFunctionLibraryCache.values());
        expression.setFunctions(functionsToUse);

        // It is not very elegant to process expression profile and script expression profile here.
        // It is somehow redundant, as it was already pre-processed in the expression evaluator/factory
        // We are throwing that out and we are processing it again. But maybe this is consequence of having
        // the duality of Expression and ScriptExpression ... maybe the ScriptExpression is unnecessary abstraction
        // and it should be removed.
        expression.setExpressionProfile(expressionProfile);
        expression.setScriptExpressionProfile(processScriptExpressionProfile(expressionProfile, language, shortDesc));

        return expression;
    }

    private ScriptExpressionProfile processScriptExpressionProfile(ExpressionProfile expressionProfile, String language, String shortDesc) throws SecurityViolationException {
        if (expressionProfile == null) {
            return null;
        }
        ExpressionEvaluatorProfile evaluatorProfile = expressionProfile.getEvaluatorProfile(ScriptExpressionEvaluatorFactory.ELEMENT_NAME);
        if (evaluatorProfile == null) {
            if (expressionProfile.getDecision() == AccessDecision.ALLOW) {
                return null;
            } else {
                throw new SecurityViolationException("Access to script expression evaluator " +
                        " not allowed (expression profile: "+expressionProfile.getIdentifier()+") in "+shortDesc);
            }
        }
        ScriptExpressionProfile scriptProfile = evaluatorProfile.getScriptExpressionProfile(language);
        if (scriptProfile == null) {
            if (evaluatorProfile.getDecision() == AccessDecision.ALLOW) {
                return null;
            } else {
                throw new SecurityViolationException("Access to script language " + language +
                        " not allowed (expression profile: "+expressionProfile.getIdentifier()+") in "+shortDesc);
            }
        }
        return scriptProfile;
    }

    // if performance becomes an issue, replace 'synchronized' with something more elaborate
    private synchronized void initializeCustomFunctionsLibraryCache(ExpressionFactory expressionFactory,
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
                .createMinorSubresult(ScriptExpressionFactory.class.getName() + ".searchCustomFunctions");
        ResultHandler<FunctionLibraryType> functionLibraryHandler = (object, parentResult) -> {
            // TODO: determine profile from function library archetype
            ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
            FunctionLibrary customLibrary = new FunctionLibrary();
            customLibrary.setVariableName(object.getName().getOrig());
            customLibrary.setGenericFunctions(
                    new CustomFunctions(object.asObjectable(), expressionFactory, expressionProfile));
            customLibrary.setNamespace(MidPointConstants.NS_FUNC_CUSTOM);
            customFunctionLibraryCache.put(object.getName().getOrig(), customLibrary);
            return true;
        };
        try {
            repositoryService.searchObjectsIterative(FunctionLibraryType.class, null, functionLibraryHandler,
                    SelectorOptions.createCollection(GetOperationOptions.createReadOnly()), true, subResult);
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
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null || type.isAssignableFrom(FunctionLibraryType.class)) {
            // Currently we don't try to select entries to be cleared based on OID
            customFunctionLibraryCache = null;
        }
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType(prismContext)
                .name(ScriptExpressionFactory.class.getName())
                .size(customFunctionLibraryCache != null ? customFunctionLibraryCache.size() : 0));
    }
}

