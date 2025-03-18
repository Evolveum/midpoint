/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tracing;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.schema.traces.TraceWriter;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.result.CompiledTracingProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Manages tracing requests.
 */
@Component
public class TracerImpl implements Tracer, SystemConfigurationChangeListener {

    private static final Trace LOGGER = TraceManager.getTrace(TracerImpl.class);

    private static final String MACRO_TIMESTAMP = "timestamp";
    private static final String MACRO_OPERATION_NAME = "operationName";
    private static final String MACRO_OPERATION_NAME_SHORT = "operationNameShort";
    private static final String MACRO_FOCUS_NAME = "focusName";
    private static final String MACRO_MILLISECONDS = "milliseconds";
    private static final String MACRO_RANDOM = "random";

    // To be used during tests to check for MID-5851
    @VisibleForTesting
    static boolean checkHashCodeEqualsRelation = false;

    @Autowired private PrismContext prismContext;
    @Autowired private TaskManager taskManager;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private TracingOutputCreator tracingOutputCreator;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    private SystemConfigurationType systemConfiguration; // can be null during some tests

    private static final String OP_STORE_TRACE = TracerImpl.class.getName() + ".storeTrace";

    private static final String TRACE_DIR_NAME = "trace";

    private static final String DEFAULT_FILE_NAME_PATTERN = "trace-%{timestamp}";

    private static final Consumer<Map<String, String>> DEFAULT_TEMPLATE_PARAMETERS_CUSTOMIZER = params -> {
    };

    @NotNull private Consumer<Map<String, String>> templateParametersCustomizer = DEFAULT_TEMPLATE_PARAMETERS_CUSTOMIZER;

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
    }

    @PreDestroy
    public void shutdown() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
    }

    @Override
    public void storeTrace(Task task, OperationResult result, @Nullable OperationResult parentResult) {
        OperationResult thisOpResult;
        if (parentResult != null) {
            thisOpResult = parentResult.createMinorSubresult(OP_STORE_TRACE);
        } else {
            thisOpResult = new OperationResult(OP_STORE_TRACE);
        }

        try {
            CompiledTracingProfile compiledTracingProfile = result.getTracingProfile();
            TracingProfileType tracingProfile = compiledTracingProfile.getDefinition();

            if (!Boolean.FALSE.equals(tracingProfile.isCreateTraceFile())) {
                boolean zip = !Boolean.FALSE.equals(tracingProfile.isCompressOutput());
                Map<String, String> templateParameters = createTemplateParameters(result); // todo evaluate lazily if needed
                File file = createFileName(zip, tracingProfile, templateParameters);
                try {
                    long start = System.currentTimeMillis();
                    TracingOutputType tracingOutput = tracingOutputCreator.createTracingOutput(task, result, tracingProfile);
                    String xml = new TraceWriter(prismContext)
                            .writeTrace(tracingOutput, file, zip);

                    if (zip) {
                        LOGGER.info("Trace was written to {} ({} chars uncompressed) in {} milliseconds", file, xml.length(),
                                System.currentTimeMillis() - start);
                    } else {
                        LOGGER.info("Trace was written to {} ({} chars) in {} milliseconds", file, xml.length(),
                                System.currentTimeMillis() - start);
                    }

                    if (!Boolean.FALSE.equals(tracingProfile.isCreateRepoObject())) {
                        ReportDataType reportDataObject = new ReportDataType()
                                .name(createObjectName(tracingProfile, templateParameters))
                                .archetypeRef(SystemObjectsType.ARCHETYPE_TRACE.value(), ArchetypeType.COMPLEX_TYPE)
                                .filePath(file.getAbsolutePath())
                                .nodeRef(ObjectTypeUtil.createObjectRef(taskManager.getLocalNode()));
                        repositoryService.addObject(reportDataObject.asPrismObject(), null, thisOpResult);
                    }
                } catch (IOException | SchemaException | ObjectAlreadyExistsException | RuntimeException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't write trace ({})", e, file);
                    throw new SystemException(e);
                }
            }
        } catch (Throwable t) {
            thisOpResult.recordFatalError(t);
            throw t;
        } finally {
            thisOpResult.computeStatusIfUnknown();
        }
    }

    private Map<String, String> createTemplateParameters(OperationResult result) {
        Map<String, String> rv = new HashMap<>();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
        rv.put(MACRO_TIMESTAMP, df.format(new Date()));
        String operationName = result.getOperation();
        rv.put(MACRO_OPERATION_NAME, operationName);
        rv.put(MACRO_OPERATION_NAME_SHORT, shorten(operationName));
        rv.put(MACRO_FOCUS_NAME, getFocusName(result));
        rv.put(MACRO_MILLISECONDS, getMilliseconds(result));
        rv.put(MACRO_RANDOM, String.valueOf((long) (Math.random() * 1_000_000_000_000_000L)));
        templateParametersCustomizer.accept(rv);
        return rv;
    }

    @Override
    public void setTemplateParametersCustomizer(
            @NotNull Consumer<Map<String, String>> customizer) {
        templateParametersCustomizer = Objects.requireNonNull(customizer, "templateParametersCustomizer");
    }

    private String shorten(String qualifiedName) {
        if (qualifiedName != null) {
            int secondLastDotIndex = StringUtils.lastOrdinalIndexOf(qualifiedName, ".", 2);
            return secondLastDotIndex >= 0 ? qualifiedName.substring(secondLastDotIndex + 1) : qualifiedName;
        } else {
            return null;
        }
    }

    private String getFocusName(OperationResult result) {
        ClockworkRunTraceType trace = result.getFirstTrace(ClockworkRunTraceType.class);
        if (trace != null && trace.getFocusName() != null) {
            return trace.getFocusName();
        } else {
            return "unknown";
        }
    }

    private String getMilliseconds(OperationResult result) {
        if (result.getMicrosecondsNullable() != null) {
            return String.valueOf(result.getMicrosecondsNullable() / 1000);
        } else if (result.getStart() != null && result.getEnd() != null) {
            return String.valueOf(result.getEnd() - result.getStart());
        } else {
            return "unknown";
        }
    }

    private String createObjectName(TracingProfileType profile, Map<String, String> parameters) {
        String pattern;
        if (profile.getObjectNamePattern() != null) {
            pattern = profile.getObjectNamePattern();
        } else if (profile.getFileNamePattern() != null) {
            pattern = profile.getFileNamePattern();
        } else {
            pattern = DEFAULT_FILE_NAME_PATTERN;
        }
        return expandMacros(pattern, parameters);
    }

    @NotNull
    private File createFileName(boolean zip, TracingProfileType profile, Map<String, String> parameters) {
        File traceDir = new File(System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY), TRACE_DIR_NAME);
        if (!traceDir.exists() || !traceDir.isDirectory()) {
            if (!traceDir.mkdir()) {
                LOGGER.warn("Attempted to create trace directory but failed: {}", traceDir);
            }
        }
        String pattern = profile.getFileNamePattern() != null ? profile.getFileNamePattern() : DEFAULT_FILE_NAME_PATTERN;
        return new File(traceDir, normalizeFileName(expandMacros(pattern, parameters)) + (zip ? ".zip" : ".xml"));
    }

    private String normalizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9 .-]", "_");
    }

    private String expandMacros(String pattern, Map<String, String> parameters) {
        return new StringSubstitutor(parameters, "%{", "}").replace(pattern);
    }

    @Override
    public TracingProfileType resolve(TracingProfileType tracingProfile, OperationResult result) throws SchemaException {
        if (tracingProfile == null) {
            return null;
        } else if (tracingProfile.getRef().isEmpty()) {
            return tracingProfile;
        } else {
            List<String> resolutionPath = new ArrayList<>();
            return resolveProfile(tracingProfile, getTracingConfiguration(), resolutionPath);
        }
    }

    @NotNull
    private TracingProfileType resolveProfile(TracingProfileType tracingProfile, TracingConfigurationType tracingConfiguration,
            List<String> resolutionPath) throws SchemaException {
        if (tracingProfile.getRef().isEmpty()) {
            return tracingProfile;
        } else {
            stateCheck(tracingConfiguration != null,
                    "No tracing configuration while resolving reference(s): %s", tracingProfile.getRef());

            TracingProfileType rv = new TracingProfileType();
            for (String ref : tracingProfile.getRef()) {
                merge(rv, getResolvedProfile(ref, tracingConfiguration, resolutionPath));
            }
            merge(rv, tracingProfile);
            rv.getRef().clear();
            return rv;
        }
    }

    private TracingProfileType getResolvedProfile(@NotNull String ref, @NotNull TracingConfigurationType tracingConfiguration,
            List<String> resolutionPath) throws SchemaException {
        if (resolutionPath.contains(ref)) {
            throw new IllegalStateException("A cycle in tracing profile resolution path: " + resolutionPath + " -> " + ref);
        }
        resolutionPath.add(ref);
        TracingProfileType profile = findProfile(ref, tracingConfiguration);
        resolutionPath.remove(resolutionPath.size() - 1);
        TracingProfileType rv = resolveProfile(profile, tracingConfiguration, resolutionPath);
        LOGGER.info("Resolved '{}' into:\n{}", ref, rv.asPrismContainerValue().debugDump());
        return rv;
    }

    private TracingProfileType findProfile(@NotNull String name, @NotNull TracingConfigurationType tracingConfiguration)
            throws SchemaException {
        List<TracingProfileType> matching = tracingConfiguration.getProfile().stream()
                .filter(p -> name.equals(p.getName()))
                .toList();
        if (matching.isEmpty()) {
            throw new SchemaException("Tracing profile '" + name + "' is referenced but not defined. Known names: "
                    + tracingConfiguration.getProfile().stream().map(TracingProfileType::getName).collect(Collectors.joining(", ")));
        } else if (matching.size() == 1) {
            return matching.get(0);
        } else {
            throw new SchemaException("More than one profile with the name '" + name + "' exist.");
        }
    }

    // fixme experimental ... not thought out very much - this method will probably work only for non-overlapping profiles
    private void merge(TracingProfileType summary, TracingProfileType delta) throws SchemaException {
        //noinspection unchecked
        summary.asPrismContainerValue().mergeContent(delta.asPrismContainerValue(), Collections.emptyList());
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        systemConfiguration = value;
    }

    @Override
    public @NotNull TracingProfileType getDefaultProfile() {
        TracingConfigurationType configuration = getTracingConfiguration();
        List<TracingProfileType> profiles = configuration != null ? configuration.getProfile() : List.of();
        if (profiles.isEmpty()) {
            return new TracingProfileType();
        } else {
            List<TracingProfileType> defaultProfiles = profiles.stream()
                    .filter(p -> Boolean.TRUE.equals(p.isDefault()))
                    .toList();
            if (defaultProfiles.isEmpty()) {
                return profiles.get(0);
            } else if (defaultProfiles.size() == 1) {
                return defaultProfiles.get(0);
            } else {
                LOGGER.warn("More than one default tracing profile configured; selecting the first one");
                return defaultProfiles.get(0);
            }
        }
    }

    @Override
    public CompiledTracingProfile compileProfile(@Nullable TracingProfileType profile, @NotNull OperationResult result)
            throws SchemaException {
        TracingProfileType resolvedProfile =
                resolve(profile != null ? profile : getDefaultProfile(), result);
        return CompiledTracingProfile.create(resolvedProfile);
    }

    @Nullable
    private TracingConfigurationType getTracingConfiguration() {
        return systemConfiguration != null && systemConfiguration.getInternals() != null ?
                systemConfiguration.getInternals().getTracing() :
                null;
    }

    public SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }
}
