/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
 *
 * @param <I> script interpreter/compiler
 * @param <C> compiled code
 * @author Radovan Semancik
 */
public abstract class AbstractCachingScriptEvaluator<I, C> extends AbstractScriptEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCachingScriptEvaluator.class);

    private final ScriptCache<I, C> scriptCache;

    public AbstractCachingScriptEvaluator(PrismContext prismContext, Protector protector, LocalizationService localizationService) {
        super(prismContext, protector, localizationService);
        this.scriptCache = new ScriptCache<>();
    }

    protected ScriptCache<I, C> getScriptCache() {
        return scriptCache;
    }

    @NotNull
    @Override
    public <T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluationContext context) throws ExpressionEvaluationException,
            ObjectNotFoundException, ExpressionSyntaxException, CommunicationException, ConfigurationException, SecurityViolationException {
        checkRestrictions(context);

        String codeString = context.getExpressionType().getCode();
        if (codeString == null) {
            throw new ExpressionEvaluationException("No script code in " + context.getContextDescription());
        }

        C compiledScript = getCompiledScript(codeString, context);

        Object evalRawResult;
        try {
            InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);

            evalRawResult = evaluateScript(compiledScript, context);

        } catch (ExpressionEvaluationException | ObjectNotFoundException | ExpressionSyntaxException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            // Exception already processed by the underlying code.
            throw e;
        } catch (Throwable e) {
            throw getLocalizationService().translate(
                    new ExpressionEvaluationException(e.getMessage() + " in " + context.getContextDescription(),
                            e, ExceptionUtil.getUserFriendlyMessage(e)));
        }

        if (context.getOutputDefinition() == null) {
            // No outputDefinition may mean "void" return type
            // or it can mean that we do not have definition, because this is something non-prism (e.g. report template).
            // Either way we can return immediately, without any value conversion. Just wrap the value in fake PrismPropertyValue.
            // For no value/null we return empty list.
            List<V> evalPrismValues = new ArrayList<>();
            if (evalRawResult != null) {
                if (evalRawResult instanceof Collection) {
                    //noinspection unchecked,rawtypes
                    ((Collection)evalRawResult).forEach(rawResultValue -> {
                        //noinspection unchecked
                        evalPrismValues.add(
                                (V) getPrismContext().itemFactory().createPropertyValue(rawResultValue));
                    });
                } else {
                    //noinspection unchecked
                    evalPrismValues.add(
                            (V) getPrismContext().itemFactory().createPropertyValue(evalRawResult));
                }
            }
            return evalPrismValues;
        }

        Class<T> javaReturnType = determineJavaReturnType(context);

        List<V> values = new ArrayList<>();

        // TODO: what about PrismContainer and PrismReference? Shouldn't they be processed in the same way as PrismProperty?
        if (evalRawResult instanceof Collection) {
            //noinspection rawtypes
            for (Object evalRawResultElement : (Collection) evalRawResult) {
                T evalResult = convertScalarResult(javaReturnType, evalRawResultElement, context);
                values.add(
                        ExpressionUtil.convertToPrismValue(
                                evalResult, context.getOutputDefinition(), context.getContextDescription()));
            }
        } else if (evalRawResult instanceof PrismProperty<?>) {
            //noinspection unchecked
            values.addAll(
                    (Collection<? extends V>) PrismValueCollectionsUtil.cloneCollection(
                            ((PrismProperty<T>) evalRawResult).getValues()));
        } else {
            T evalResult = convertScalarResult(javaReturnType, evalRawResult, context);
            values.add(
                    ExpressionUtil.convertToPrismValue(
                            evalResult, context.getOutputDefinition(), context.getContextDescription()));
        }

        return values;
    }

    @NotNull
    private <T> Class<T> determineJavaReturnType(ScriptExpressionEvaluationContext context) {
        QName xsdReturnType = context.getOutputDefinition().getTypeName();

        // Ugly hack. Indented to allow xsd:anyType return type, see MID-6775.
        if (QNameUtil.match(xsdReturnType, DOMUtil.XSD_ANYTYPE)) {
            //noinspection unchecked
            return (Class<T>) Object.class;
        }

        Class<T> javaReturnType = XsdTypeMapper.toJavaType(xsdReturnType);
        if (javaReturnType == null) {
            javaReturnType = getPrismContext().getSchemaRegistry().getCompileTimeClass(xsdReturnType);
        }

        if (javaReturnType == null && (context.getOutputDefinition() instanceof PrismContainerDefinition<?>)) {
            // This is the case when we need a container, but we do not have compile-time class for that
            // E.g. this may be container in object extension (MID-5080)
            //noinspection unchecked
            javaReturnType = (Class<T>) PrismContainerValue.class;
        }

        if (javaReturnType == null) {
            // TODO quick and dirty hack - because this could be because of enums defined in schema extension (MID-2399)
            //  ...and enums (xsd:simpleType) are not parsed into ComplexTypeDefinitions
            //noinspection unchecked
            javaReturnType = (Class<T>) String.class;
        }
        LOGGER.trace("expected return type: XSD={}, Java={}", xsdReturnType, javaReturnType);
        return javaReturnType;
    }

    private C getCompiledScript(String codeString, ScriptExpressionEvaluationContext context) throws ExpressionEvaluationException, SecurityViolationException {
        C compiledScript = scriptCache.getCode(context.getExpressionProfile(), codeString);
        if (compiledScript != null) {
            return compiledScript;
        }
        InternalMonitor.recordCount(InternalCounters.SCRIPT_COMPILE_COUNT);
        try {
            compiledScript = compileScript(codeString, context);
        } catch (ExpressionEvaluationException | SecurityViolationException e) {
            throw e;
        } catch (Exception e) {
            throw new ExpressionEvaluationException(e.getMessage() + " while compiling " + context.getContextDescription(), e);
        }
        scriptCache.putCode(context.getExpressionProfile(), codeString, compiledScript);
        return compiledScript;
    }

    protected abstract C compileScript(String codeString, ScriptExpressionEvaluationContext context) throws Exception;

    protected abstract Object evaluateScript(C compiledScript, ScriptExpressionEvaluationContext context)
            throws Exception;

    private <T> T convertScalarResult(Class<T> expectedType, Object rawValue, ScriptExpressionEvaluationContext context) throws ExpressionEvaluationException {
        try {
            return ExpressionUtil.convertValue(expectedType, context.getAdditionalConvertor(), rawValue, getProtector());
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException(e.getMessage() + " in " + context.getContextDescription(), e);
        }
    }

}
