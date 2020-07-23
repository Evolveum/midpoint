/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.metadata.ValueMetadataComputation;
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

    ProvenanceBuiltinMapping() {
        super(PROVENANCE_PATH);
    }

    @Override
    public void apply(@NotNull ValueMetadataComputation computation) {

        List<PrismValue> input = computation.getInputValues();

        LOGGER.trace("Computing provenance during {}. Input values:\n{}",
                computation.getScope(), lazy(() -> dumpInput(input)));

        ProvenanceMetadataType provenance;
        if (computation.getScope() == MetadataMappingScopeType.TRANSFORMATION) {
            provenance = applyDuringTransformation(input);
        } else if (computation.getScope() == MetadataMappingScopeType.CONSOLIDATION) {
            provenance = applyDuringConsolidation(input);
        } else {
            throw new AssertionError(computation.getScope());
        }

        LOGGER.trace("Output: provenance:\n{}", provenance != null ?
                lazy(() -> provenance.asPrismContainerValue().debugDump()) : "(none)");

        computation.getOutputMetadataBean().setProvenance(provenance);
    }

    /**
     * Transformation merges the acquisitions into single yield.
     */
    private ProvenanceMetadataType applyDuringTransformation(List<PrismValue> input) {
        List<ProvenanceAcquisitionType> acquisitions = collectAcquisitions(input);
        if (!acquisitions.isEmpty()) {
            ProvenanceYieldType yield = new ProvenanceYieldType(prismContext);
            yield.getAcquisition().addAll(acquisitions);
            return new ProvenanceMetadataType(prismContext).yield(yield);
        } else {
            return null;
        }
    }

    private List<ProvenanceAcquisitionType> collectAcquisitions(List<PrismValue> input) {
        List<ProvenanceAcquisitionType> acquisitions = new ArrayList<>();
        input.stream()
                .map(v -> ((ValueMetadataType) v.getValueMetadata().asContainerable()).getProvenance())
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
                .noneMatch(a -> areEquivalent(acquisition, a));
    }

    private boolean areEquivalent(ProvenanceAcquisitionType a1, ProvenanceAcquisitionType a2) {
        if (a1 == null) {
            return a2 == null;
        } else if (a2 == null) {
            return false;
        } else {
            return referencesEquivalent(a1.getOriginRef(), a2.getOriginRef()) &&
                    referencesEquivalent(a1.getActorRef(), a2.getActorRef()) &&
                    referencesEquivalent(a1.getResourceRef(), a2.getResourceRef()) &&
                    Objects.equals(a1.getChannel(), a2.getChannel());
        }
    }

    private boolean referencesEquivalent(ObjectReferenceType ref1, ObjectReferenceType ref2) {
        if (ref1 == null) {
            return ref2 == null;
        } else if (ref2 == null) {
            return false;
        } else {
            return Objects.equals(ref1.getOid(), ref2.getOid());
        }
    }

    /**
     * Consolidation merges the yields.
     */
    private ProvenanceMetadataType applyDuringConsolidation(List<PrismValue> input) {
        Collection<ProvenanceYieldType> yields = collectYields(input);
        if (!yields.isEmpty()) {
            ProvenanceMetadataType provenance = new ProvenanceMetadataType(prismContext);
            provenance.getYield().addAll(yields);
            return provenance;
        } else {
            return null;
        }
    }

    private Collection<ProvenanceYieldType> collectYields(List<PrismValue> input) {
        List<ProvenanceYieldType> yields = new ArrayList<>();
        input.stream()
                .map(v -> ((ValueMetadataType) v.getValueMetadata().asContainerable()).getProvenance())
                .filter(Objects::nonNull)
                .flatMap(provenance -> provenance.getYield().stream())
                .forEach(yield -> addYieldIfNotPresent(yield, yields));
        return yields;
    }

    private void addYieldIfNotPresent(ProvenanceYieldType yield, List<ProvenanceYieldType> yields) {
        if (isNotPresent(yield, yields)) {
            yields.add(yield.clone());
        }
    }

    private boolean isNotPresent(ProvenanceYieldType yield, List<ProvenanceYieldType> yields) {
        return yields.stream()
                .noneMatch(a -> areEquivalent(yield, a));
    }

    private boolean areEquivalent(ProvenanceYieldType y1, ProvenanceYieldType y2) {
        if (y1 == null) {
            return y2 == null;
        } else if (y2 == null) {
            return false;
        } else {
            return MiscUtil.unorderedCollectionCompare(y1.getAcquisition(), y2.getAcquisition(),
                    (a1, a2) -> areEquivalent(a1, a2) ? 0 : 1);
        }
    }

    private String dumpInput(List<PrismValue> inputValues) {
        if (inputValues.isEmpty()) {
            return "  (no values)";
        } else {
            StringBuilder sb = new StringBuilder();
            for (PrismValue inputValue : inputValues) {
                sb.append("  - ").append(inputValue.toString()).append(" with metadata:\n");
                sb.append(inputValue.getValueMetadata().debugDump(2)).append("\n");
            }
            return sb.toString();
        }
    }
}
