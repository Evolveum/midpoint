/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.groovy;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Inspired by work of Cédric Champeau (http://melix.github.io/blog/2015/03/sandboxing.html)
 *
 * @author Radovan Semancik
 */
public class SandboxTypeCheckingExtension extends AbstractTypeCheckingExtension {

    private static final Trace LOGGER = TraceManager.getTrace(SandboxTypeCheckingExtension.class);

    public SandboxTypeCheckingExtension(StaticTypeCheckingVisitor typeCheckingVisitor) {
        super(typeCheckingVisitor);
    }

    private @NotNull ScriptExpressionEvaluationContext getContext() {
        return ScriptExpressionEvaluationContext.getThreadLocalRequired();
    }

    @Override
    public void onMethodSelection(final Expression expression, final MethodNode target) {
        ClassNode targetDeclaringClass = target.getDeclaringClass();
        AccessDecision decision = decideClass(targetDeclaringClass.getName(), target.getName());

        if (decision != AccessDecision.ALLOW) {
            StringBuilder sb = new StringBuilder(GroovyScriptEvaluator.SANDBOX_ERROR_PREFIX);
            sb.append("Access to Groovy method ");
            sb.append(targetDeclaringClass.getName()).append("#").append(target.getName()).append(" ");
            if (decision == AccessDecision.DENY) {
                sb.append("denied");
            } else {
                sb.append("not allowed");
            }
            if (getContext().getExpressionProfile() != null) {
                sb.append(" (applied expression profile '").append(getContext().getExpressionProfile().getIdentifier()).append("')");
            }
            addStaticTypeError(sb.toString(), expression);
        }
    }

    private @NotNull AccessDecision decideClass(String className, String methodName) {
        AccessDecision builtinDecision = GroovyScriptEvaluator.decideGroovyBuiltin(className, methodName);
        LOGGER.trace("decideClass: builtin [{},{}] : {}", className, methodName, builtinDecision);
        if (builtinDecision != AccessDecision.DEFAULT) {
            return builtinDecision;
        }
        var scriptExpressionProfile = getContext().getScriptExpressionProfile();
        if (scriptExpressionProfile == null) {
            LOGGER.trace("decideClass: profile==null [{},{}] : ALLOW", className, methodName);
            return AccessDecision.ALLOW;
        }
        var methodDecision = scriptExpressionProfile.decideClassAccess(className, methodName);
        LOGGER.trace("decideClass: profile({}) [{},{}] : {}",
                getContext().getExpressionProfile().getIdentifier(), className, methodName, methodDecision);
        return methodDecision;
    }

    @Override
    public boolean handleUnresolvedVariableExpression(VariableExpression vExp) {
        String variableName = vExp.getName();
        ScriptExpressionEvaluationContext context = getContext();
        String contextDescription = context.getContextDescription();

        if (!isDynamic(vExp)) {
            LOGGER.error("Unresolved script variable {} because it is not dynamic, in {}", variableName, contextDescription);
            return false;
        }

        VariablesMap variables = context.getVariables();
        if (variables != null) {
            TypedValue<?> variableTypedValue = variables.get(variableName);
            if (variableTypedValue != null) {
                Class<?> variableClass;
                try {
                    variableClass = variableTypedValue.determineClass();
                } catch (SchemaException e) {
                    String msg = "Cannot determine type of variable '%s' (%s) in %s: %s".formatted(
                            variableName, variableTypedValue, contextDescription, e.getMessage());
                    LOGGER.error("{}", msg); // TODO Really? The logging should be done by the one who catches the exception.
                    throw new IllegalStateException(msg, e);
                }
                LOGGER.trace("Determine script variable {} as expression variable, class {} in {}",
                        variableName, variableClass, contextDescription);
                storeType(vExp, ClassHelper.make(variableClass));
                setHandled(true);
                return true;
            }
        }

        for (FunctionLibraryBinding function : emptyIfNull(context.getFunctionLibraryBindings())) {
            if (function.getVariableName().equals(variableName)) {
                Class<?> functionClass = function.getImplementation().getClass();
                LOGGER.trace("Determine script variable {} as function library, class {} in {}",
                        variableName, functionClass, contextDescription);
                storeType(vExp, ClassHelper.make(functionClass));
                setHandled(true);
                return true;
            }
        }

        LOGGER.error("Unresolved script variable {} because no declaration for it cannot be found in {}",
                variableName, contextDescription);
        return false;
    }
}
