/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.common.mapping.metadata.ConsolidationMetadataComputation;
import com.evolveum.midpoint.model.common.mapping.metadata.TransformationalMetadataComputation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.metadata.MidpointProvenanceEquivalenceStrategy;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

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

        ProvenanceYieldType yield = new ProvenanceYieldType(prismContext)
                .mappingSpec(computation.getMappingSpecification());
        yield.getAcquisition().addAll(collectAcquisitions(input));

        ProvenanceMetadataType provenance = new ProvenanceMetadataType(prismContext)
                .yield(yield);

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
                .flatMap(provenance -> provenance.getYield().stream())
                .flatMap(yield -> yield.getAcquisition().stream())
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
     *  - equivalent mapping spec
     *  - equivalent set of acquisitions
     *
     * We have to construct a new set of acquisitions, selecting the oldest ones of each kind.
     */
    @Override
    public void applyForConsolidation(@NotNull ConsolidationMetadataComputation computation) {
        ProvenanceYieldType yield = new YieldComputation(computation).compute();
        if (yield != null) {
            computation.getOutputMetadataValueBean().setProvenance(
                    new ProvenanceMetadataType(prismContext)
                            .yield(yield));
        }
    }

    private class YieldComputation {

        @NotNull private final ConsolidationMetadataComputation computation;
        @NotNull private final List<ValueMetadataType> allValues;
        private final ProvenanceYieldType representativeYield;

        private YieldComputation(@NotNull ConsolidationMetadataComputation computation) {
            this.computation = computation;

            allValues = ListUtils.union(this.computation.getNonNegativeValues(), this.computation.getExistingValues());
            if (allValues.isEmpty()) {
                throw new IllegalArgumentException("No input values in " + this.computation.getContextDescription());
            }

            representativeYield = getRepresentativeYield();
        }

        @Nullable
        private ProvenanceYieldType getRepresentativeYield() {
            ProvenanceMetadataType representative = allValues.get(0).getProvenance();
            if (representative == null || representative.getYield().isEmpty()) {
                return null;
            } else if (representative.getYield().size() > 1) {
                throw new IllegalStateException("More than one yield in " + representative);
            } else {
                return representative.getYield().get(0);
            }
        }

        private ProvenanceYieldType compute() {
            if (representativeYield == null) {
                return null;
            }

            ProvenanceYieldType resultingYield = new ProvenanceYieldType(prismContext);
            resultingYield.setMappingSpec(CloneUtil.clone(representativeYield.getMappingSpec()));

            for (ProvenanceAcquisitionType representativeAcquisition : representativeYield.getAcquisition()) {
                List<ProvenanceAcquisitionType> compatibleAcquisitions = getCompatibleAcquisitions(representativeAcquisition);
                ProvenanceAcquisitionType earliest = compatibleAcquisitions.stream()
                        .min(Comparator.nullsLast(Comparator.comparing(acquisition -> XmlTypeConverter.toMillisNullable(acquisition.getTimestamp()))))
                        .orElseThrow(() -> new IllegalStateException("No earliest acquisition"));
                resultingYield.getAcquisition().add(earliest.clone());
            }

            return resultingYield;
        }

        private @NotNull List<ProvenanceAcquisitionType> getCompatibleAcquisitions(ProvenanceAcquisitionType representativeAcquisition) {
            List<ProvenanceAcquisitionType> acquisitions = new ArrayList<>();
            for (ValueMetadataType metadata : allValues) {
                assert metadata.getProvenance() != null && metadata.getProvenance().getYield().size() == 1;
                ProvenanceYieldType yield = metadata.getProvenance().getYield().get(0);
                List<ProvenanceAcquisitionType> compatibleAcq = yield.getAcquisition().stream()
                        .filter(acq -> MidpointProvenanceEquivalenceStrategy.INSTANCE.equals(acq, representativeAcquisition))
                        .collect(Collectors.toList());
                if (compatibleAcq.size() != 1) {
                    LOGGER.error("Something went wrong. We expected to find single acquisition in each yield. In: {}\n"
                                    + "allValues:\n{}\nrepresentativeYield:\n{}\nrepresentativeAcquisition:\n{}\n"
                                    + "this metadata:\n{}\ncompatible acquisitions found:\n{}",
                            computation.getContextDescription(),
                            DebugUtil.debugDump(allValues, 1),
                            DebugUtil.debugDump(representativeYield, 1),
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

    private String dumpInput(List<PrismValue> inputValues) {
        if (inputValues.isEmpty()) {
            return "  (no values)";
        } else {
            StringBuilder sb = new StringBuilder();
            for (PrismValue inputValue : inputValues) {
                if (inputValue != null) {
                    sb.append("  - ").append(inputValue.toString()).append(" with metadata:\n");
                    sb.append(inputValue.getValueMetadata().debugDump(2)).append("\n");
                }
            }
            return sb.toString();
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
