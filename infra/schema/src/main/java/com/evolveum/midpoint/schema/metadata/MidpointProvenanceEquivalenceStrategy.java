/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Used to compare metadata from the provenance point of view.
 * I.e. two metadata PC (or PCV) are equal iff they have the same acquisition. (TODO and mapping?)
 *
 * So, this strategy cannot be applied to non-metadata PCs/PCVs.
 *
 * Temporary implementation.
 */
@Experimental
public class MidpointProvenanceEquivalenceStrategy implements EquivalenceStrategy {

    public static final MidpointProvenanceEquivalenceStrategy INSTANCE = new MidpointProvenanceEquivalenceStrategy();

    private MidpointProvenanceEquivalenceStrategy() {
    }

    @Override
    public boolean equals(Item<?, ?> first, Item<?, ?> second) {
        return MiscUtil.unorderedCollectionEquals(first.getValues(), second.getValues(), this::equals);
    }

    @Override
    public boolean equals(PrismValue first, PrismValue second) {
        ValueMetadataType metadata1 = (ValueMetadataType) ((PrismContainerValue<?>) first).asContainerable();
        ValueMetadataType metadata2 = (ValueMetadataType) ((PrismContainerValue<?>) second).asContainerable();
        return equals(metadata1, metadata2);
    }

    public boolean equals(ValueMetadataType metadata1, ValueMetadataType metadata2) {
        ProvenanceMetadataType provenance1 = metadata1.getProvenance();
        ProvenanceMetadataType provenance2 = metadata2.getProvenance();
        MappingSpecificationType mappingSpec1 = provenance1 != null ? provenance1.getMappingSpecification() : null;
        MappingSpecificationType mappingSpec2 = provenance2 != null ? provenance2.getMappingSpecification() : null;
        if (!equals(mappingSpec1, mappingSpec2)) {
            return false;
        }

        List<ProvenanceAcquisitionType> acquisitions1 = provenance1 != null ? provenance1.getAcquisition() : Collections.emptyList();
        List<ProvenanceAcquisitionType> acquisitions2 = provenance2 != null ? provenance2.getAcquisition() : Collections.emptyList();
        return MiscUtil.unorderedCollectionEquals(acquisitions1, acquisitions2, this::equals);
    }

    public boolean equals(MappingSpecificationType mappingSpec1, MappingSpecificationType mappingSpec2) {
        if (mappingSpec1 == null && mappingSpec2 == null) {
            return true;
        } else if (mappingSpec1 == null || mappingSpec2 == null) {
            return false;
        } else {
            return Objects.equals(mappingSpec1.getMappingName(), mappingSpec2.getMappingName())
                    && refsEqual(mappingSpec1.getDefinitionObjectRef(), mappingSpec2.getDefinitionObjectRef())
                    && Objects.equals(mappingSpec1.getObjectType(), mappingSpec2.getObjectType())
                    && QNameUtil.match(mappingSpec1.getAssociationType(), mappingSpec2.getAssociationType())
                    && Objects.equals(mappingSpec1.getTag(), mappingSpec2.getTag());
//            && Objects.equals(mappingSpec1.getAssignmentId(), mappingSpec2.getAssignmentId());
        }
    }

    public boolean equals(ProvenanceAcquisitionType acq1, ProvenanceAcquisitionType acq2) {
        return // Objects.equals(acq1.getChannel(), acq2.getChannel()) &&
                refsEqual(acq1.getOriginRef(), acq2.getOriginRef())
                        && refsEqual(acq1.getResourceRef(), acq2.getResourceRef());
    }

    private boolean refsEqual(ObjectReferenceType ref1, ObjectReferenceType ref2) {
        if (ref1 == null && ref2 == null) {
            return true;
        } else if (ref1 == null || ref2 == null) {
            return false;
        } else {
            return ref1.asReferenceValue().equals(ref2.asReferenceValue(), EquivalenceStrategy.REAL_VALUE);
        }
    }

    @Override
    public int hashCode(Item<?, ?> item) {
        return 0; // TODO
    }

    @Override
    public int hashCode(PrismValue value) {
        return 0; // TODO
    }
}
