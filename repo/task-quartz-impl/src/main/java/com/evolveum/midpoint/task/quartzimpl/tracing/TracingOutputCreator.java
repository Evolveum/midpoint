/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tracing;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismObjectImpl;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Creates serializable tracing output.
 */
@Component
public class TracingOutputCreator {

    private static final Trace LOGGER = TraceManager.getTrace(TracerImpl.class);

    @Autowired private PrismContext prismContext;
    @Autowired private TracerImpl tracer;
    @Autowired private TaskManager taskManager;

    TracingOutputType createTracingOutput(Task task, OperationResult result, TracingProfileType tracingProfile) {
        TracingOutputType output = new TracingOutputType();
        output.beginMetadata()
                .createTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()))
                .profile(tracingProfile);
        output.setEnvironment(createTracingEnvironmentDescription(task, tracingProfile));

        result.checkLogRecorderFlushed();

        OperationResultType resultBean = result.createOperationResultType();
        new LogCompressor().compressResult(resultBean);

        List<TraceDictionaryType> embeddedDictionaries = extractDictionaries(result);
        TraceDictionaryType dictionary = extractDictionary(embeddedDictionaries, resultBean);
        output.setDictionary(dictionary);
        result.setExtractedDictionary(dictionary);
        output.setResult(resultBean);
        return output;
    }

    private List<TraceDictionaryType> extractDictionaries(OperationResult result) {
        return result.getResultStream()
                .map(OperationResult::getExtractedDictionary)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @NotNull
    private TracingEnvironmentType createTracingEnvironmentDescription(Task task, TracingProfileType tracingProfile) {
        TracingEnvironmentType environment = new TracingEnvironmentType();
        if (!Boolean.TRUE.equals(tracingProfile.isHideDeploymentInformation())) {
            SystemConfigurationType systemConfiguration = tracer.getSystemConfiguration();
            DeploymentInformationType deployment = systemConfiguration != null ? systemConfiguration.getDeploymentInformation() : null;
            if (deployment != null) {
                DeploymentInformationType deploymentClone = deployment.clone();
                deploymentClone.setSubscriptionIdentifier(null);
                environment.setDeployment(deploymentClone);
            }
        }
        NodeType localNode = taskManager.getLocalNode();
        NodeType selectedNodeInformation = new NodeType();
        selectedNodeInformation.setName(localNode.getName());
        selectedNodeInformation.setNodeIdentifier(localNode.getNodeIdentifier());
        selectedNodeInformation.setBuild(CloneUtil.clone(localNode.getBuild()));
        selectedNodeInformation.setClustered(localNode.isClustered());
        environment.setNodeRef(ObjectTypeUtil.createObjectRefWithFullObject(selectedNodeInformation));
        TaskType taskClone = task.getRawTaskObjectClone().asObjectable();
        taskClone.asPrismObject().removeProperty(TaskType.F_RESULT); // There are some issues with incomplete="true" flag
        environment.setTaskRef(ObjectTypeUtil.createObjectRefWithFullObject(taskClone));
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
                PrismObject<?> dictionaryObject = entry.getObject().asReferenceValue().getObject();
                if (java.util.Objects.equals(objectToStore.getOid(), dictionaryObject.getOid()) &&
                        Objects.equals(objectToStore.getVersion(), dictionaryObject.getVersion())) {
                    comparisons++;
                    boolean equals = objectToStore.equals(dictionaryObject);
                    if (equals) {
                        if (TracerImpl.checkHashCodeEqualsRelation) {
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
                    .object(ObjectTypeUtil.createObjectRefWithFullObject(objectToStore));
            objectsAdded++;
            return dictionaryId + ":" + newEntryId;
        }

        // These objects are equal. They hashcodes should be also. This is a helping hand given to tests. See MID-5851.
        @SuppressWarnings("rawtypes")
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
                objectToStore = cloneExceptForFetchResult(object);
            } else {
                objectToStore = object;
            }
            return objectToStore;
        }

        <T extends ObjectType> PrismObject<T> cloneExceptForFetchResult(PrismObject<T> source) {
            PrismObject<T> clone = new PrismObjectImpl<>(
                    source.getElementName(),
                    source.getDefinition(),
                    PrismContext.get());
            for (Item<?, ?> sourceItem : source.getValue().getItems()) {
                if (ObjectType.F_FETCH_RESULT.equals(sourceItem.getElementName())) {
                    continue;
                }
                try {
                    //noinspection unchecked
                    clone.getValue().add(
                            sourceItem.clone());
                } catch (SchemaException e) {
                    throw SystemException.unexpected(e, "when cloning");
                }
            }
            return clone;
        }

        private void logDiagnosticInformation() {
            LOGGER.trace("Extracted dictionary: {} objects added in {} ms ({} total), using {} object comparisons while checking {} objects",
                    objectsAdded, System.currentTimeMillis() - started, dictionary.getEntry().size(), comparisons, objectsChecked);
        }
    }

    private TraceDictionaryType extractDictionary(List<TraceDictionaryType> embeddedDictionaries, OperationResultType resultBean) {
        TraceDictionaryType dictionary = new TraceDictionaryType();

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

}
