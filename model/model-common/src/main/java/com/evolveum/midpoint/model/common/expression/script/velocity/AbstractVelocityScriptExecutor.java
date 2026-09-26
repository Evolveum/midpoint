/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.velocity;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import com.evolveum.midpoint.repo.common.SystemObjectCache.ExpressionsConfigurationView;
import com.evolveum.midpoint.schema.expression.CustomVelocityExtension;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.apache.velocity.runtime.RuntimeConstants;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.model.common.expression.script.AbstractScriptExecutor;
import com.evolveum.midpoint.model.common.expression.script.ScriptExecutionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.binding.TypeSafeEnum;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Base class for safe/full evaluators based on Apache Velocity engine.
 */
abstract class AbstractVelocityScriptExecutor extends AbstractScriptExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractVelocityScriptExecutor.class);

    private static final ThreadLocal<ExecutionMode> EXECUTION_MODE_THREAD_LOCAL = new ThreadLocal<>();

    private static boolean velocityInitialized;

    @Experimental
    @Nullable private static CustomVelocityExtension customVelocityExtension;

    @NotNull private final ExpressionsConfigurationView expressionsConfigurationView;

    AbstractVelocityScriptExecutor(
            PrismContext prismContext,
            Protector protector,
            LocalizationService localizationService,
            ExpressionsConfigurationSection configuration,
            @NotNull ExpressionsConfigurationView expressionsConfigurationView) {
        super(prismContext, protector, localizationService, configuration);
        this.expressionsConfigurationView = expressionsConfigurationView;
        synchronized (AbstractVelocityScriptExecutor.class) {
            if (!velocityInitialized) {
                Velocity.init(createVelocityEngineProperties());
                LOGGER.info("Velocity initialized (legacy mode: {})", configuration.legacyVelocityEngine());
                // We instantiate even if experimental code is disabled, as we don't have OperationResult here to get the config
                instantiateCustomVelocityExtension(configuration.customVelocityExtensionClassName());
                velocityInitialized = true;
            }
        }
    }

    private void instantiateCustomVelocityExtension(@Nullable String className) {
        if (className != null) {
            try {
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (!(instance instanceof CustomVelocityExtension velocityExtension)) {
                    throw new IllegalArgumentException("Class " + className + " does not implement CustomVelocityExtension");
                }
                LOGGER.info("Loaded custom Velocity extension class {} (visible in templates as '{}')",
                        className, velocityExtension.getVariableName());
                customVelocityExtension = velocityExtension;
            } catch (ClassNotFoundException e) {
                // minor error, just log it and continue (we need this for tests, but also in production it is not a fatal error)
                LoggingUtils.logException(LOGGER, "Unable to load CustomVelocityExtension class {}", e, className);
            } catch (Throwable e) {
                // this is something strange that should not happen, so we throw an exception
                throw SystemException.unexpected(e, "when loading CustomVelocityExtension class " + className);
            }
        }
    }

    // Note that these properties are shared for safe and regular Velocity engine.
    // It is a limitation of the Velocity engine that its properties are static and global.
    private Properties createVelocityEngineProperties() {
        Properties properties = new Properties();
        if (!configuration.legacyVelocityEngine()) {
            properties.put(RuntimeConstants.UBERSPECT_CLASSNAME, SafeUberspectorImpl.class.getName()); // restricts method calls
            properties.put(RuntimeConstants.EVENTHANDLER_INCLUDE, RejectIncludes.class.getName()); // forbids #parse and #include
            properties.put(RuntimeConstants.VM_PERM_ALLOW_INLINE, false); // forbids defining new macros
            properties.put(RuntimeConstants.VM_LIBRARY_AUTORELOAD, false); // perhaps not strictly necessary, but just to be safe
            properties.put(RuntimeConstants.CONTEXT_SCOPE_CONTROL + "evaluate", false); // this seems to be the default
            properties.put(RuntimeConstants.CONTEXT_SCOPE_CONTROL + "define", false); // this seems to be the default
        }
        return properties;
    }

    @Override
    public @NotNull Object executeInternal(
            @NotNull String codeString,
            @NotNull ScriptExecutionContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, SubscriptionComplianceException {

        ExecutionMode previousExecutionMode = EXECUTION_MODE_THREAD_LOCAL.get();
        try {
            EXECUTION_MODE_THREAD_LOCAL.set(getExecutionModeFromExecutor());

            VelocityContext velocityCtx = createVelocityContext(context);

            StringWriter resultWriter = new StringWriter();

            InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
            Velocity.evaluate(velocityCtx, resultWriter, "", codeString);

            return resultWriter.toString();

        } finally {
            if (previousExecutionMode != null) {
                EXECUTION_MODE_THREAD_LOCAL.set(previousExecutionMode);
            } else {
                EXECUTION_MODE_THREAD_LOCAL.remove();
            }
        }
    }

    static boolean isFullExecutionMode() {
        return getExecutionMode() == ExecutionMode.FULL;
    }

    private static @NotNull ExecutionMode getExecutionMode() {
        return stateNonNull(EXECUTION_MODE_THREAD_LOCAL.get(), "Execution mode is not set in the current thread");
    }

    abstract ExecutionMode getExecutionModeFromExecutor();

    private VelocityContext createVelocityContext(ScriptExecutionContext context)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, SubscriptionComplianceException {
        VelocityContext velocityCtx = new VelocityContext();

        // Render midPoint schema enums using their lexical values
        EventCartridge eventCartridge = new EventCartridge();
        eventCartridge.addEventHandler((ReferenceInsertionEventHandler)
                        (velocityContext, reference, value) ->
                                value instanceof TypeSafeEnum typeSafeEnum ? typeSafeEnum.value() : value);
        eventCartridge.attachToContext(velocityCtx);

        Map<String, Object> scriptVariables = prepareUnifiedScriptVariablesValueMap(context);
        for (Map.Entry<String, Object> scriptVariable : scriptVariables.entrySet()) {
            velocityCtx.put(scriptVariable.getKey(), scriptVariable.getValue());
        }
        if (customVelocityExtension != null) {
            if (expressionsConfigurationView.isExperimentalCodeEnabled(context.getResult())) {
                velocityCtx.put(customVelocityExtension.getVariableName(), customVelocityExtension);
            } else {
                LOGGER.warn("Ignoring custom velocity extension, as experimental code is not enabled");
            }
        }
        checkVelocityContextBeforeExecution(velocityCtx);
        return velocityCtx;
    }

    void checkVelocityContextBeforeExecution(VelocityContext velocityCtx) {
    }

    enum ExecutionMode {

        /** Restricted mode implemented by {@link SafeVelocityScriptExecutor}. */
        SAFE,

        /** Full (legacy) mode implemented by {@link VelocityScriptExecutor}. */
        FULL
    }
}
