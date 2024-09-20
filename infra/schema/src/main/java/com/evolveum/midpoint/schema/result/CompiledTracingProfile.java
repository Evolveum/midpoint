/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.LoggingLevelOverrideConfiguration;
import com.evolveum.midpoint.util.logging.LoggingLevelOverrideConfiguration.Entry;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.LoggingSchemaUtil.toLevel;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *
 */
public final class CompiledTracingProfile implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(CompiledTracingProfile.class);

    @NotNull private final TracingProfileType definition;

    @Nullable private final LoggingLevelOverrideConfiguration loggingLevelOverrideConfiguration;

    @NotNull private final OperationMonitoringConfiguration operationMonitoringConfiguration;

    private final Map<Class<? extends TraceType>, TracingLevelType> levelMap = new HashMap<>();

    private CompiledTracingProfile(@NotNull TracingProfileType definition) {
        this.definition = definition;
        this.loggingLevelOverrideConfiguration = compileLevelOverrideConfiguration(definition.getLoggingOverride());
        this.operationMonitoringConfiguration = OperationMonitoringConfiguration.create(definition.getOperationMonitoring());
    }

    public static CompiledTracingProfile create(TracingProfileType resolvedProfile) {
        CompiledTracingProfile compiledProfile = new CompiledTracingProfile(resolvedProfile);
        PrismSchema commonSchema = PrismContext.get().getSchemaRegistry().findSchemaByNamespace(SchemaConstants.NS_C);
        for (ComplexTypeDefinition complexTypeDefinition : commonSchema.getComplexTypeDefinitions()) {
            Class<?> clazz = complexTypeDefinition.getCompileTimeClass();
            if (clazz != null && TraceType.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                Class<? extends TraceType> traceClass = (Class<? extends TraceType>) clazz;
                compiledProfile.levelMap.put(traceClass, getLevel(resolvedProfile, traceClass));
            }
        }
        return compiledProfile;
    }

    @NotNull
    public TracingLevelType getLevel(@NotNull Class<? extends TraceType> traceClass) {
        return ObjectUtils.defaultIfNull(levelMap.get(traceClass), TracingLevelType.MINIMAL);
    }

    @NotNull
    public TracingProfileType getDefinition() {
        return definition;
    }

    @NotNull
    private static TracingLevelType getLevel(TracingProfileType resolvedProfile, Class<? extends TraceType> traceClass) {
        if (!resolvedProfile.getRef().isEmpty()) {
            throw new IllegalArgumentException("Profile is not resolved: " + resolvedProfile);
        }
        List<QName> ancestors = getAncestors(traceClass);
        LOGGER.trace("Ancestors for {}: {}", traceClass, ancestors);
        for (QName ancestor : ancestors) {
            TracingLevelType level = getLevel(resolvedProfile, ancestor);
            if (level != null) {
                return level;
            }
        }
        return TracingLevelType.MINIMAL;
    }

    private static List<QName> getAncestors(Class<? extends TraceType> traceClass) {
        List<QName> rv = new ArrayList<>();
        for (;;) {
            if (!TraceType.class.isAssignableFrom(traceClass)) {
                throw new IllegalStateException("Wrong trace class: " + traceClass);
            }
            rv.add(PrismContext.get().getSchemaRegistry().findTypeDefinitionByCompileTimeClass(traceClass, ComplexTypeDefinition.class).getTypeName());
            if (traceClass.equals(TraceType.class)) {
                return rv;
            }
            //noinspection unchecked
            traceClass = (Class<? extends TraceType>) traceClass.getSuperclass();
        }
    }

    private static TracingLevelType getLevel(@NotNull TracingProfileType profile, @NotNull QName traceClassName) {
        boolean isRoot = TraceType.COMPLEX_TYPE.equals(traceClassName);
        Set<TracingLevelType> levels = profile.getTracingTypeProfile().stream()
                .filter(p -> isRoot && p.getOperationType().isEmpty() || QNameUtil.matchAny(traceClassName, p.getOperationType()))
                .map(p -> defaultIfNull(p.getLevel(), TracingLevelType.MINIMAL))
                .collect(Collectors.toSet());
        LOGGER.trace("Levels for {}: {}", traceClassName, levels);
        if (!levels.isEmpty()) {
            TracingLevelType level = levels.stream().max(Comparator.comparing(Enum::ordinal)).orElse(null);
            LOGGER.trace("Max level for {}: {}", traceClassName, level);
            return level;
        } else {
            return null;
        }
    }

    boolean isCollectingLogEntries() {
        return Boolean.TRUE.equals(definition.isCollectLogEntries());
    }

    @Nullable LoggingLevelOverrideConfiguration getLoggingLevelOverrideConfiguration() {
        return loggingLevelOverrideConfiguration;
    }

    private LoggingLevelOverrideConfiguration compileLevelOverrideConfiguration(LoggingOverrideType override) {
        if (override == null) {
            return null;
        }
        LoggingLevelOverrideConfiguration rv = new LoggingLevelOverrideConfiguration();
        for (ClassLoggerLevelOverrideType levelOverride : override.getLevelOverride()) {
            rv.addEntry(new Entry(new HashSet<>(levelOverride.getLogger()), toLevel(levelOverride.getLevel())));
        }
        return rv;
    }

    boolean isMeasureCpuTime() {
        return !Boolean.FALSE.equals(definition.isMeasureCpuTime());
    }

    @NotNull OperationMonitoringConfiguration getOperationMonitoringConfiguration() {
        return operationMonitoringConfiguration;
    }
}
