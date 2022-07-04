/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Sets the origin information in prism values being merged.
 *
 * In order to track the origin of individual prism item values in the merged object, the mergers are obliged to fill-in
 * this information on any values being processed. Specifically, when a prism value `V` is inherited from the source
 * (super-)object `S`, this fact is recorded in the {@link ValueMetadataType} of `V` by setting `provenance/acquisition/originRef`
 * to the reference to `S`.
 *
 * Notes:
 *
 * . When a composite value ({@link PrismContainerValue}) is inherited, only the root value gets the metadata. It's not necessary
 * (and therefore it's avoided) to set the metadata on each of the contained ("inner") values.
 * . When a value is passed through multiple layers of inheritance (e.g. inherited from `O1` to `O2` and finally to `O3`),
 * only the "real" origin (i.e. `O1`) is recorded.
 * . Origin for values not passing through the inheritance relation (i.e. values at the bottom of the inheritance hierarchy)
 * are not marked - not even if the containing resource has an OID.
 *
 * For some examples please see `TestResourceTemplateMerge`.
 *
 * Important assumption:
 *
 * - The current implementation assumes that there is no value metadata stored in the (original) objects being merged; i.e.
 * that all metadata come from this {@link OriginMarker}.
 */
public interface OriginMarker {

    /** Imprints the origin into the value. */
    void mark(PrismValue value) throws SchemaException;

    /** Marks values with oid/type pair. */
    static @Nullable OriginMarker forOid(@Nullable String oid, QName typeName) {
        if (oid != null) {
            return forRef(
                    new ObjectReferenceType()
                            .oid(oid)
                            .type(typeName));
        } else {
            return null;
        }
    }

    /** Marks values with given reference. */
    static @Nullable OriginMarker forRef(@Nullable ObjectReferenceType ref) {
        if (ref != null) {
            return value -> {
                // We assume that the only metadata used are our own ones.
                // So if a value is already marked, we do not add our mark - we know that it comes from an ancestor.
                if (!value.hasValueMetadata()) {
                    ValueMetadataType metadata = new ValueMetadataType()
                            .provenance(new ProvenanceMetadataType()
                                    .acquisition(new ProvenanceAcquisitionType()
                                            .originRef(ref)));
                    value.getValueMetadata().addMetadataValue(
                            metadata.asPrismContainerValue());
                }
            };
        } else {
            return null;
        }
    }
}
