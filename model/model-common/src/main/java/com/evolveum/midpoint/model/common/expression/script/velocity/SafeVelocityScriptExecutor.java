/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.velocity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import com.evolveum.midpoint.model.common.expression.script.ScriptExecutionContext;
import com.evolveum.midpoint.prism.Safe;

import com.evolveum.midpoint.util.exception.SecurityViolationException;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.springframework.core.annotation.AnnotationUtils;

import static com.evolveum.midpoint.model.common.expression.script.velocity.SafeIntrospectorImpl.ALLOWED_CLASSES;

/**
 * Expression evaluator that uses Apache Velocity engine in safe mode (with restricted access to Java classes and methods).
 */
public class SafeVelocityScriptExecutor extends AbstractVelocityScriptExecutor {

    public SafeVelocityScriptExecutor(
            PrismContext prismContext,
            Protector protector,
            LocalizationService localizationService,
            ExpressionsConfigurationSection configuration) {
        super(prismContext, protector, localizationService, configuration);
    }

    @Override
    ExecutionMode getExecutionModeFromExecutor() {
        return ExecutionMode.SAFE;
    }

    @Override
    protected void checkProfileAndSafetyRestrictions(ScriptExecutionContext context) throws SecurityViolationException {
        super.checkProfileAndSafetyRestrictions(context);
        if (configuration.legacyVelocityEngine()) {
            throw new UnsupportedOperationException(
                    "Safe Velocity mode is not supported when legacy Velocity engine is used. "
                            + "Please set 'legacyVelocityEngine' to 'false' in the configuration.");
        }
    }

    @Override
    protected boolean needsServiceVariables() {
        return false;
    }

    @Override
    protected boolean shouldProvideVariable(TypedValue<?> typedValue) {
        Class<?> clazz;
        if (typedValue.getValue() != null) {
            clazz = typedValue.getValue().getClass();
        } else if (typedValue.canDetermineType()) {
            // This is just to be nice - not including variables of incompatible types.
            // No harm would be done, as they don't have a value anyway.
            try {
                clazz = typedValue.determineClass();
            } catch (SchemaException e) {
                throw SystemException.unexpected(e);
            }
        } else {
            return true; // it's safe to include "null" values
        }
        return isAllowed(clazz);
    }

    private static boolean isAllowed(Class<?> clazz) {
        return ALLOWED_CLASSES.stream().anyMatch(allowedClass -> allowedClass.isAssignableFrom(clazz))
                || isAnnotatedAsSafe(clazz);
    }

    private static boolean isAnnotatedAsSafe(Class<?> clazz) {
        // This looks at all superclasses and intefaces of given class, so it is enough to annotate a base class or interface
        // to mark all subclasses safe.
        //
        // However, beware that it also looks at _annotations_ of the class, so if any of them is marked as @Safe,
        // the class will be considered safe as well. This is not a problem, but it is something to be aware of.
        // Do not mark any annotation as @Safe unless you really mean it.
        return AnnotationUtils.findAnnotation(clazz, Safe.class) != null;
    }

    /** Just a safety check to make sure no sneaky variable of an incompatible type got into the context. */
    @Override
    void checkVelocityContextBeforeExecution(VelocityContext velocityContext) {
        super.checkVelocityContextBeforeExecution(velocityContext);
        Arrays.stream(velocityContext.getKeys()).forEach(key -> {
            Object value = velocityContext.get(key);
            if (value != null && !isAllowed(value.getClass())) {
                throw new IllegalStateException(
                        "Not allowed variable type sneaked into safe velocity context: %s for key %s".formatted(
                                value.getClass(), key));
            }
        });
    }

    @Override
    public String getLanguageName() {
        return MidPointConstants.EXPRESSION_LANGUAGE_SAFE_VELOCITY_NAME;
    }

    @Override
    public @NotNull String getLanguageUrl() {
        return MidPointConstants.EXPRESSION_LANGUAGE_SAFE_VELOCITY_URL;
    }

    @Override
    protected boolean isConsideredSafe() {
        return true;
    }
}
