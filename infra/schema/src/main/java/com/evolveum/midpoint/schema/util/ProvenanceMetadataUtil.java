/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.metadata.MidpointProvenanceEquivalenceStrategy;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Experimental
public class ProvenanceMetadataUtil {

    public static boolean hasOrigin(ValueMetadataType metadata, String originOid) {
        return metadata != null && hasOrigin(metadata.getProvenance(), originOid);
    }

    public static boolean hasOrigin(ProvenanceMetadataType provenance, String originOid) {
        return provenance.getAcquisition().stream()
                .anyMatch(acquisition -> hasOrigin(acquisition, originOid));
    }

    public static boolean hasOrigin(ProvenanceAcquisitionType acquisition, String originOid) {
        return acquisition.getOriginRef() != null && originOid.equals(acquisition.getOriginRef().getOid());
    }

    public static boolean hasMappingSpec(ValueMetadataType metadata, MappingSpecificationType mappingSpecification) {
        return metadata != null && hasMappingSpec(metadata.getProvenance(), mappingSpecification);
    }

    public static boolean hasMappingSpec(ProvenanceMetadataType provenance, MappingSpecificationType mappingSpecification) {
        return MidpointProvenanceEquivalenceStrategy.INSTANCE.equals(provenance.getMappingSpec(), mappingSpecification);
    }
}
