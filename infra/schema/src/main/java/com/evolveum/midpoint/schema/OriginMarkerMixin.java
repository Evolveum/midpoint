/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.OriginMarker;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public interface OriginMarkerMixin {

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
