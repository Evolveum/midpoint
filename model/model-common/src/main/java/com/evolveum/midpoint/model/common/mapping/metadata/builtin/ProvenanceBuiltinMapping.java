/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.metadata.ConsolidationMetadataComputation;
import com.evolveum.midpoint.model.common.mapping.metadata.TransformationalMetadataComputation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.metadata.MidpointProvenanceEquivalenceStrategy;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

/**
 * Mapping that manages provenance metadata.
 */
@Component
public class ProvenanceBuiltinMapping extends BaseBuiltinMetadataMapping {

    private static final Trace LOGGER = TraceManager.getTrace(ProvenanceBuiltinMapping.class);

    private static final ItemPath PROVENANCE_PATH = ItemPath.create(ValueMetadataType.F_PROVENANCE);

    @SuppressWarnings("unused")
    ProvenanceBuiltinMapping() {
        super(PROVENANCE_PATH);
    }

    @VisibleForTesting
    public ProvenanceBuiltinMapping(PrismContext prismContext, BuiltinMetadataMappingsRegistry registry) {
        super(PROVENANCE_PATH);
        this.prismContext = prismContext;
        this.registry = registry;
    }

    /**
     * Transformation merges the acquisitions into single yield.
     */
    @Override
    public void applyForTransformation(@NotNull TransformationalMetadataComputation computation) {
        List<PrismValue> input = computation.getInputValues();

        LOGGER.trace("Computing provenance during value transformation. Input values:\n{}", lazy(() -> dumpInput(input)));

        ProvenanceMetadataType provenance = new ProvenanceMetadataType()
                .mappingSpecification(computation.getMappingSpecification());
        provenance.getAcquisition().addAll(collectAcquisitions(input));

        LOGGER.trace("Output: provenance:\n{}", lazy(() -> provenance.asPrismContainerValue().debugDump()));

        computation.getOutputMetadataValueBean().setProvenance(provenance);
    }

    private List<ProvenanceAcquisitionType> collectAcquisitions(List<PrismValue> input) {
        List<ProvenanceAcquisitionType> acquisitions = new ArrayList<>();
        input.stream()
                .filter(Objects::nonNull)
                .flatMap(prismValue -> prismValue.<ValueMetadataType>getValueMetadataAsContainer().getRealValues().stream())
                .map(ValueMetadataType::getProvenance)
                .filter(Objects::nonNull)
                .flatMap(provenance -> provenance.getAcquisition().stream())
                .forEach(acquisition -> addAcquisitionIfNotPresent(acquisition, acquisitions));
        return acquisitions;
    }

    private void addAcquisitionIfNotPresent(ProvenanceAcquisitionType acquisition, List<ProvenanceAcquisitionType> acquisitions) {
        if (isNotPresent(acquisition, acquisitions)) {
            acquisitions.add(acquisition.clone());
        }
    }

    private boolean isNotPresent(ProvenanceAcquisitionType acquisition, List<ProvenanceAcquisitionType> acquisitions) {
        return acquisitions.stream()
                .noneMatch(a -> MidpointProvenanceEquivalenceStrategy.INSTANCE.equals(acquisition, a));
    }

    /**
     * Consolidation merges the information for one specific yield.
     * All inputs have the same basic characteristics:
     * - equivalent mapping spec
     * - equivalent set of acquisitions
     *
     * We have to construct a new set of acquisitions, selecting the oldest ones of each kind.
     */
    @Override
    public void applyForConsolidation(@NotNull ConsolidationMetadataComputation computation) {
        ProvenanceMetadataType provenance = new ProvenanceComputation(computation).compute();
        if (provenance != null) {
            computation.getOutputMetadataValueBean().setProvenance(provenance);
        }
    }

    private static class ProvenanceComputation {

        @NotNull private final ConsolidationMetadataComputation computation;
        @NotNull private final List<ValueMetadataType> allValues;
        private final ProvenanceMetadataType representativeProvenance;

        private ProvenanceComputation(@NotNull ConsolidationMetadataComputation computation) {
            this.computation = computation;

            allValues = ListUtils.union(this.computation.getNonNegativeValues(), this.computation.getExistingValues());
            if (allValues.isEmpty()) {
                throw new IllegalArgumentException("No input values in " + this.computation.getContextDescription());
            }

            representativeProvenance = getRepresentativeProvenance();
        }

        @Nullable
        private ProvenanceMetadataType getRepresentativeProvenance() {
            return allValues.get(0).getProvenance();
        }

        private ProvenanceMetadataType compute() {
            if (representativeProvenance == null) {
                return null;
            }

            ProvenanceMetadataType resultingProvenance = new ProvenanceMetadataType();
            resultingProvenance.setMappingSpecification(CloneUtil.clone(representativeProvenance.getMappingSpecification()));

            for (ProvenanceAcquisitionType representativeAcquisition : representativeProvenance.getAcquisition()) {
                List<ProvenanceAcquisitionType> compatibleAcquisitions = getCompatibleAcquisitions(representativeAcquisition);
                ProvenanceAcquisitionType earliest = compatibleAcquisitions.stream()
                        .min(Comparator.comparing(
                                acquisition -> XmlTypeConverter.toMillisNullable(acquisition.getTimestamp()),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .orElseThrow(() -> new IllegalStateException("No earliest acquisition"));
                resultingProvenance.getAcquisition().add(earliest.clone());
            }

            return resultingProvenance;
        }

        private @NotNull List<ProvenanceAcquisitionType> getCompatibleAcquisitions(ProvenanceAcquisitionType representativeAcquisition) {
            List<ProvenanceAcquisitionType> acquisitions = new ArrayList<>();
            for (ValueMetadataType metadata : allValues) {
                ProvenanceMetadataType provenance = metadata.getProvenance();
                List<ProvenanceAcquisitionType> compatibleAcq = provenance.getAcquisition().stream()
                        .filter(acq -> MidpointProvenanceEquivalenceStrategy.INSTANCE.equals(acq, representativeAcquisition))
                        .collect(Collectors.toList());
                if (compatibleAcq.size() != 1) {
                    LOGGER.error("Something went wrong. We expected to find single acquisition in each yield. In: {}\n"
                                    + "allValues:\n{}\nrepresentativeProvenance:\n{}\nrepresentativeAcquisition:\n{}\n"
                                    + "this metadata:\n{}\ncompatible acquisitions found:\n{}",
                            computation.getContextDescription(),
                            DebugUtil.debugDump(allValues, 1),
                            DebugUtil.debugDump(representativeProvenance, 1),
                            DebugUtil.debugDump(representativeAcquisition, 1),
                            DebugUtil.debugDump(metadata, 1),
                            DebugUtil.debugDump(compatibleAcq, 1));
                    throw new IllegalStateException("Something went wrong. We expected to find single acquisition in each value. "
                            + "See the log. In: " + computation.getContextDescription());
                }
                acquisitions.add(compatibleAcq.get(0));
            }
            return acquisitions;
        }
    }

//    private String dumpInputMetadata(List<ValueMetadataType> values) {
//        if (values.isEmpty()) {
//            return "  (no values)";
//        } else {
//            StringBuilder sb = new StringBuilder();
//            for (ValueMetadataType value : values) {
//                sb.append(" -\n");
//                sb.append(value.asPrismContainerValue().debugDump(2)).append("\n");
//            }
//            return sb.toString();
//        }
//    }
}
