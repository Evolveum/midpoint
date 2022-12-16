/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.CompiledTracingProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;

public interface Tracer {

    /**
     * Stores trace to persistent storage (usually a file in "trace" directory).
     *
     * @param task Task containing the context information necessary e.g. to derive name of the trace file.
     * @param result Result that is to be serialized and stored.
     * @param parentResult Parent result where this operation should be recorded (if any).
     */
    void storeTrace(Task task, OperationResult result, @Nullable OperationResult parentResult);

    /**
     * Resolves a tracing profile - i.e. replaces references to other (named) profiles with their content.
     *
     * @throws SchemaException If the profile name cannot be resolved e.g. if the referenced
     * profile does not exist or the name in ambiguous.
     */
    TracingProfileType resolve(TracingProfileType tracingProfile, OperationResult result) throws SchemaException;

    /** TODO */
    @NotNull TracingProfileType getDefaultProfile();

    /**
     * Compiles the tracing profile; or a default one, if no profile is provided.
     */
    CompiledTracingProfile compileProfile(@Nullable TracingProfileType profile, @NotNull OperationResult result)
            throws SchemaException;

    /**
     * Sets customizer of tracer template parameters, replacing any previous one.
     * This allows to inject custom parameters, for instance during test runs.
     */
    void setTemplateParametersCustomizer(@NotNull Consumer<Map<String, String>> customizer);

    //TracingLevelType getLevel(@NotNull TracingProfileType resolvedProfile, @NotNull Class<TraceType> traceClass);
}
