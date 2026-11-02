/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.mapping.MappingTestEvaluator;
import com.evolveum.midpoint.model.common.mapping.metadata.TransformationalMetadataComputation;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingTransformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeIdentificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

/**
 * Verifies that mapping specifications are cloned before being attached to
 * provenance and transformation metadata, preventing Prism parent ownership
 * conflicts when the same specification is reused concurrently.
 *
 * See MID-11746
 */
public class TestBuiltinMetadataMappingOwnership {

    private static final int THREAD_COUNT = 4;
    private static final QName ASSOCIATION_TYPE = new QName("https://example.com/mid-11746", "groupMembership");

    private MappingTestEvaluator evaluator;
    private ProvenanceBuiltinMapping provenanceMapping;
    private TransformationBuiltinMapping transformationMapping;

    @BeforeClass
    public void setUp() throws Exception {
        evaluator = new MappingTestEvaluator();
        evaluator.init();

        var registry = new BuiltinMetadataMappingsRegistry();
        provenanceMapping = new ProvenanceBuiltinMapping(evaluator.getPrismContext(), registry);
        transformationMapping = new TransformationBuiltinMapping();
    }

    @Test
    public void testProvenanceMappingSpecificationIsCloned() throws Exception {
        MappingSpecificationType original = createMappingSpecification();

        MappingSpecificationType first = applyProvenance(original).getMappingSpecification();
        MappingSpecificationType second = applyProvenance(original).getMappingSpecification();

        assertIndependentCopies(original, List.of(first, second));
    }

    @Test
    public void testTransformationMappingSpecificationIsCloned() throws Exception {
        MappingSpecificationType original = createMappingSpecification();

        MappingSpecificationType first = applyTransformation(original).getMappingSpecification();
        MappingSpecificationType second = applyTransformation(original).getMappingSpecification();

        assertIndependentCopies(original, List.of(first, second));
    }

    @Test
    public void testMappingSpecificationIsClonedConcurrently() throws Exception {
        MappingSpecificationType original = createMappingSpecification();
        List<TransformationalMetadataComputation> computations = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            computations.add(evaluator.createMetadataComputation(original));
        }

        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        Queue<ValueMetadataType> results = new ConcurrentLinkedQueue<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            try {
                for (TransformationalMetadataComputation computation : computations) {
                    executor.submit(() -> {
                        ready.countDown();
                        try {
                            start.await();
                            provenanceMapping.applyForTransformation(computation);
                            transformationMapping.applyForTransformation(computation);
                            results.add(computation.getOutputMetadataValueBean());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            failures.add(e);
                        } catch (Throwable t) {
                            failures.add(t);
                        }
                    });
                }

                boolean allWorkersReady = ready.await(10, SECONDS);
                start.countDown();
                assertThat(allWorkersReady).as("workers ready").isTrue();
            } finally {
                start.countDown();
                shutdownExecutor(executor);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }

        assertThat(failures).as("worker failures").isEmpty();
        assertThat(results).hasSize(THREAD_COUNT);

        List<MappingSpecificationType> resultingSpecifications = new ArrayList<>();
        for (ValueMetadataType result : results) {
            resultingSpecifications.add(result.getProvenance().getMappingSpecification());
            resultingSpecifications.add(result.getTransformation().getMappingTransformation().getMappingSpecification());
        }
        assertIndependentCopies(original, resultingSpecifications);
    }

    private void shutdownExecutor(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, SECONDS)) {
                executor.shutdownNow();
                assertThat(executor.awaitTermination(10, SECONDS)).as("workers finished").isTrue();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private ProvenanceMetadataType applyProvenance(MappingSpecificationType mappingSpecification) throws Exception {
        TransformationalMetadataComputation computation = evaluator.createMetadataComputation(mappingSpecification);
        provenanceMapping.applyForTransformation(computation);
        return computation.getOutputMetadataValueBean().getProvenance();
    }

    private MappingTransformationType applyTransformation(MappingSpecificationType mappingSpecification) throws Exception {
        TransformationalMetadataComputation computation = evaluator.createMetadataComputation(mappingSpecification);
        transformationMapping.applyForTransformation(computation);
        return computation.getOutputMetadataValueBean().getTransformation().getMappingTransformation();
    }

    private MappingSpecificationType createMappingSpecification() {
        return new MappingSpecificationType()
                .mappingName("ACCOUNT-employee-ref-ENTITLEMENT-ad-group-inbound")
                .definitionObjectRef("00000000-0000-0000-0000-000000011746", ResourceType.COMPLEX_TYPE)
                .objectType(new ResourceObjectTypeIdentificationType()
                        .kind(ShadowKindType.ACCOUNT)
                        .intent("employee"))
                .associationType(ASSOCIATION_TYPE)
                .tag("mid-11746");
    }

    private void assertIndependentCopies(MappingSpecificationType original, List<MappingSpecificationType> copies) {
        PrismContainerValue<?> originalValue = original.asPrismContainerValue();
        assertThat(originalValue.getParent()).as("original parent").isNull();

        Set<PrismContainerValue<?>> valuesByIdentity = Collections.newSetFromMap(new IdentityHashMap<>());
        for (MappingSpecificationType copy : copies) {
            assertExpectedContent(copy);
            PrismContainerValue<?> copyValue = copy.asPrismContainerValue();
            assertThat(copyValue).as("copy PCV").isNotSameAs(originalValue);
            assertThat(copyValue.getParent()).as("copy parent").isNotNull();
            assertThat(valuesByIdentity.add(copyValue)).as("unique copy PCV").isTrue();
        }

        assertThat(originalValue.getParent()).as("original parent after all attachments").isNull();
    }

    private void assertExpectedContent(MappingSpecificationType actual) {
        assertThat(actual).isNotNull();
        assertThat(actual.getMappingName()).isEqualTo("ACCOUNT-employee-ref-ENTITLEMENT-ad-group-inbound");
        assertThat(actual.getDefinitionObjectRef().getOid()).isEqualTo("00000000-0000-0000-0000-000000011746");
        assertThat(actual.getDefinitionObjectRef().getType()).isEqualTo(ResourceType.COMPLEX_TYPE);
        assertThat(actual.getObjectType().getKind()).isEqualTo(ShadowKindType.ACCOUNT);
        assertThat(actual.getObjectType().getIntent()).isEqualTo("employee");
        assertThat(actual.getAssociationType()).isEqualTo(ASSOCIATION_TYPE);
        assertThat(actual.getTag()).isEqualTo("mid-11746");
    }
}
