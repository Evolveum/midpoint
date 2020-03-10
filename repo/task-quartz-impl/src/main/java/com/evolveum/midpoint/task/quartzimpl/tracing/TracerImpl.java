/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tracing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.CompiledTracingProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    public static boolean checkHashCodeEqualsRelation = false;

    @Autowired private PrismContext prismContext;
    @Autowired private TaskManager taskManager;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService repositoryService;

    private SystemConfigurationType systemConfiguration; // can be null during some tests

    private static final String OP_STORE_TRACE = TracerImpl.class.getName() + ".storeTrace";

    private static final String TRACE_DIR_NAME = "trace";
    private static final String ZIP_ENTRY_NAME = "trace.xml";

    private static final String DEFAULT_FILE_NAME_PATTERN = "trace-%{timestamp}";

    public static final Consumer<Map<String, String>> DEFAULT_TEMPLATE_PARAMETERS_CUSTOMIZER = params -> {
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
            result.clearTracingProfile();

            if (!Boolean.FALSE.equals(tracingProfile.isCreateTraceFile())) {
                boolean zip = !Boolean.FALSE.equals(tracingProfile.isCompressOutput());
                Map<String, String> templateParameters = createTemplateParameters(result); // todo evaluate lazily if needed
                File file = createFileName(zip, tracingProfile, templateParameters);
                try {
                    long start = System.currentTimeMillis();
                    TracingOutputType tracingOutput = createTracingOutput(task, result, tracingProfile);
                    String xml = prismContext.xmlSerializer().serializeRealValue(tracingOutput);
                    if (zip) {
                        MiscUtil.writeZipFile(file, ZIP_ENTRY_NAME, xml, StandardCharsets.UTF_8);
                        LOGGER.info("Trace was written to {} ({} chars uncompressed) in {} milliseconds", file, xml.length(),
                                System.currentTimeMillis() - start);
                    } else {
                        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                            pw.write(xml);
                            LOGGER.info("Trace was written to {} ({} chars) in {} milliseconds", file, xml.length(),
                                    System.currentTimeMillis() - start);
                        }
                    }
                    if (!Boolean.FALSE.equals(tracingProfile.isCreateRepoObject())) {
                        ReportOutputType reportOutputObject = new ReportOutputType(prismContext)
                                .name(createObjectName(tracingProfile, templateParameters))
                                .archetypeRef(SystemObjectsType.ARCHETYPE_TRACE.value(), ArchetypeType.COMPLEX_TYPE)
                                .filePath(file.getAbsolutePath())
                                .nodeRef(ObjectTypeUtil.createObjectRef(taskManager.getLocalNode(), prismContext));
                        repositoryService.addObject(reportOutputObject.asPrismObject(), null, thisOpResult);
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

    private TracingOutputType createTracingOutput(Task task, OperationResult result, TracingProfileType tracingProfile) {
        TracingOutputType output = new TracingOutputType(prismContext);
        output.beginMetadata()
                .createTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()))
                .profile(tracingProfile);
        output.setEnvironment(createTracingEnvironmentDescription(task, tracingProfile));

        OperationResultType resultBean = result.createOperationResultType();

        List<TraceDictionaryType> embeddedDictionaries = extractDictionaries(result);
        TraceDictionaryType dictionary = extractDictionary(embeddedDictionaries, resultBean);
        output.setDictionary(dictionary);
        result.setExtractedDictionary(dictionary);
        output.setResult(resultBean);
        return output;
    }

    private List<TraceDictionaryType> extractDictionaries(OperationResult result) {
        return result.getResultStream()
                .filter(r -> r.getExtractedDictionary() != null)
                .map(OperationResult::getExtractedDictionary)
                .collect(Collectors.toList());
    }

    @NotNull
    private TracingEnvironmentType createTracingEnvironmentDescription(Task task, TracingProfileType tracingProfile) {
        TracingEnvironmentType environment = new TracingEnvironmentType(prismContext);
        if (!Boolean.TRUE.equals(tracingProfile.isHideDeploymentInformation())) {
            DeploymentInformationType deployment = systemConfiguration != null ? systemConfiguration.getDeploymentInformation() : null;
            if (deployment != null) {
                DeploymentInformationType deploymentClone = deployment.clone();
                deploymentClone.setSubscriptionIdentifier(null);
                environment.setDeployment(deploymentClone);
            }
        }
        NodeType localNode = taskManager.getLocalNode();
        if (localNode != null) {
            NodeType selectedNodeInformation = new NodeType(prismContext);
            selectedNodeInformation.setName(localNode.getName());
            selectedNodeInformation.setNodeIdentifier(localNode.getNodeIdentifier());
            selectedNodeInformation.setBuild(CloneUtil.clone(localNode.getBuild()));
            selectedNodeInformation.setClustered(localNode.isClustered());
            environment.setNodeRef(ObjectTypeUtil.createObjectRefWithFullObject(selectedNodeInformation, prismContext));
        }
        TaskType taskClone = task.getClonedTaskObject().asObjectable();
        if (taskClone.getResult() != null) {
            taskClone.getResult().getPartialResults().clear();
        }
        environment.setTaskRef(ObjectTypeUtil.createObjectRefWithFullObject(taskClone, prismContext));
        return environment;
    }

    // Extracting from JAXB objects currently does not work because RawType.getValue fails for
    // raw versions of ObjectReferenceType holding full object
    private static class ExtractingVisitor implements Visitor {

        private final long started = System.currentTimeMillis();
        private final TraceDictionaryType dictionary;
        private final int dictionaryId;
        private final PrismContext prismContext;
        private int comparisons = 0;
        private int objectsChecked = 0;
        private int objectsAdded = 0;

        private ExtractingVisitor(TraceDictionaryType dictionary, int dictionaryId, PrismContext prismContext) {
            this.dictionary = dictionary;
            this.dictionaryId = dictionaryId;
            this.prismContext = prismContext;
        }

//        @Override
//        public void visit(JaxbVisitable visitable) {
//            JaxbVisitable.visitPrismStructure(visitable, this);
//        }

        @Override
        public void visit(Visitable visitable) {
//            if (visitable instanceof PrismPropertyValue) {
//                PrismPropertyValue<?> pval = ((PrismPropertyValue) visitable);
//                Object realValue = pval.getRealValue();
//                if (realValue instanceof JaxbVisitable) {
//                    ((JaxbVisitable) realValue).accept(this);
//                }
//            } else
            if (visitable instanceof PrismReferenceValue) {
                PrismReferenceValue refVal = (PrismReferenceValue) visitable;
                //noinspection unchecked
                PrismObject<? extends ObjectType> object = refVal.getObject();
                if (object != null && !object.isEmpty()) {
                    String qualifiedEntryId = findOrCreateEntry(object);
                    refVal.setObject(null);
                    refVal.setOid(SchemaConstants.TRACE_DICTIONARY_PREFIX + qualifiedEntryId);
                    if (object.getDefinition() != null) {
                        refVal.setTargetType(object.getDefinition().getTypeName());
                    }
                }
            }
        }

        private String findOrCreateEntry(PrismObject<? extends ObjectType> object) {
            long started = System.currentTimeMillis();
            int maxEntryId = 0;

            objectsChecked++;
            PrismObject<? extends ObjectType> objectToStore = stripFetchResult(object);

            for (TraceDictionaryEntryType entry : dictionary.getEntry()) {
                PrismObject dictionaryObject = entry.getObject().asReferenceValue().getObject();
                if (Objects.equals(objectToStore.getOid(), dictionaryObject.getOid()) &&
                        Objects.equals(objectToStore.getVersion(), dictionaryObject.getVersion())) {
                    comparisons++;
                    boolean equals = objectToStore.equals(dictionaryObject);
                    if (equals) {
                        if (checkHashCodeEqualsRelation) {
                            checkHashCodes(objectToStore, dictionaryObject);
                        }
                        LOGGER.trace("Found existing entry #{}:{}: {} [in {} ms]", entry.getOriginDictionaryId(),
                                entry.getIdentifier(), objectToStore, System.currentTimeMillis() - started);
                        return entry.getOriginDictionaryId() + ":" + entry.getIdentifier();
                    } else {
                        LOGGER.trace("Found object with the same OID {} and same version '{}' but with different content",
                                objectToStore.getOid(), objectToStore.getVersion());
                    }
                }
                if (entry.getIdentifier() > maxEntryId) {
                    // We intentionally ignore context for entries.
                    maxEntryId = entry.getIdentifier();
                }
            }
            int newEntryId = maxEntryId + 1;
            LOGGER.trace("Inserting object as entry #{}:{}: {} [in {} ms]", dictionaryId, newEntryId, objectToStore,
                    System.currentTimeMillis() - started);

            dictionary.beginEntry()
                    .identifier(newEntryId)
                    .originDictionaryId(dictionaryId)
                    .object(ObjectTypeUtil.createObjectRefWithFullObject(objectToStore, prismContext));
            objectsAdded++;
            return dictionaryId + ":" + newEntryId;
        }

        // These objects are equal. They hashcodes should be also. This is a helping hand given to tests. See MID-5851.
        private void checkHashCodes(PrismObject object1, PrismObject object2) {
            int hash1 = object1.hashCode();
            int hash2 = object2.hashCode();
            if (hash1 != hash2) {
                System.out.println("Hash 1 = " + hash1);
                System.out.println("Hash 2 = " + hash2);
                System.out.println("Object 1:\n" + object1.debugDump());
                System.out.println("Object 2:\n" + object2.debugDump());
                //noinspection unchecked
                System.out.println("Diff:\n" + DebugUtil.debugDump(object1.diff(object2)));
                throw new AssertionError("Objects " + object1 + " and " + object2 + " are equal but their hashcodes are different");
            }
        }

        @NotNull
        private PrismObject<? extends ObjectType> stripFetchResult(PrismObject<? extends ObjectType> object) {
            PrismObject<? extends ObjectType> objectToStore;
            if (object.asObjectable().getFetchResult() != null) {
                objectToStore = object.clone();
                objectToStore.asObjectable().setFetchResult(null);
            } else {
                objectToStore = object;
            }
            return objectToStore;
        }

        private void logDiagnosticInformation() {
            LOGGER.trace("Extracted dictionary: {} objects added in {} ms ({} total), using {} object comparisons while checking {} objects",
                    objectsAdded, System.currentTimeMillis() - started, dictionary.getEntry().size(), comparisons, objectsChecked);
        }
    }

    private TraceDictionaryType extractDictionary(List<TraceDictionaryType> embeddedDictionaries, OperationResultType resultBean) {
        TraceDictionaryType dictionary = new TraceDictionaryType(prismContext);

        embeddedDictionaries.forEach(embeddedDictionary ->
                dictionary.getEntry().addAll(CloneUtil.cloneCollectionMembers(embeddedDictionary.getEntry())));

        int newDictionaryId = generateDictionaryId(embeddedDictionaries);
        dictionary.setIdentifier(newDictionaryId);

        ExtractingVisitor extractingVisitor = new ExtractingVisitor(dictionary, newDictionaryId, prismContext);
        extractDictionary(resultBean, extractingVisitor);
        extractingVisitor.logDiagnosticInformation();

        return dictionary;
    }

    private int generateDictionaryId(List<TraceDictionaryType> embedded) {
        int max = embedded.stream()
                .map(TraceDictionaryType::getIdentifier)
                .max(Integer::compareTo)
                .orElse(0);
        return max + 1;
    }

    private void extractDictionary(OperationResultType resultBean, ExtractingVisitor extractingVisitor) {
        resultBean.getTrace().forEach(trace -> trace.asPrismContainerValue().accept(extractingVisitor));
        resultBean.getPartialResults().forEach(partialResult -> extractDictionary(partialResult, extractingVisitor));
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
        if (result.getMicroseconds() != null) {
            return String.valueOf(result.getMicroseconds() / 1000);
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
            TracingProfileType rv = new TracingProfileType();
            for (String ref : tracingProfile.getRef()) {
                merge(rv, getResolvedProfile(ref, tracingConfiguration, resolutionPath));
            }
            merge(rv, tracingProfile);
            rv.getRef().clear();
            return rv;
        }
    }

    private TracingProfileType getResolvedProfile(String ref, TracingConfigurationType tracingConfiguration,
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

    private TracingProfileType findProfile(String name, TracingConfigurationType tracingConfiguration) throws SchemaException {
        List<TracingProfileType> matching = tracingConfiguration.getProfile().stream()
                .filter(p -> name.equals(p.getName()))
                .collect(Collectors.toList());
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
    public boolean update(@Nullable SystemConfigurationType value) {
        systemConfiguration = value;
        return true;
    }

    @Override
    public TracingProfileType getDefaultProfile() {
        TracingConfigurationType tracingConfiguration = getTracingConfiguration();
        if (tracingConfiguration == null || tracingConfiguration.getProfile().isEmpty()) {
            return new TracingProfileType(prismContext);
        } else {
            List<TracingProfileType> defaultProfiles = tracingConfiguration.getProfile().stream()
                    .filter(p -> Boolean.TRUE.equals(p.isDefault()))
                    .collect(Collectors.toList());
            if (defaultProfiles.isEmpty()) {
                return tracingConfiguration.getProfile().get(0);
            } else if (defaultProfiles.size() == 1) {
                return defaultProfiles.get(0);
            } else {
                LOGGER.warn("More than one default tracing profile configured; selecting the first one");
                return defaultProfiles.get(0);
            }
        }
    }

    @Override
    public CompiledTracingProfile compileProfile(TracingProfileType profile, OperationResult result) throws SchemaException {
        TracingProfileType resolvedProfile = resolve(profile, result);
        return CompiledTracingProfile.create(resolvedProfile, prismContext);
    }

    @Nullable
    private TracingConfigurationType getTracingConfiguration() {
        return systemConfiguration != null && systemConfiguration.getInternals() != null ?
                systemConfiguration.getInternals().getTracing() :
                null;
    }
}
