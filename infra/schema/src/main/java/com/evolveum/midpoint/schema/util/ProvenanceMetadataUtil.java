/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.ValueSelector;
import com.evolveum.midpoint.schema.metadata.MidpointProvenanceEquivalenceStrategy;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

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

    public static boolean hasMappingSpecification(ValueMetadataType metadata, MappingSpecificationType mappingSpecification) {
        return metadata != null && hasMappingSpecification(metadata.getProvenance(), mappingSpecification);
    }

    public static boolean hasMappingSpecification(ProvenanceMetadataType provenance, MappingSpecificationType mappingSpecification) {
        return provenance != null && MidpointProvenanceEquivalenceStrategy.INSTANCE.equals(provenance.getMappingSpecification(), mappingSpecification);
    }

    public static ValueSelector<PrismContainerValue<ValueMetadataType>> originSelector(String oid) {
        return pcv -> hasOrigin(pcv.asContainerable(), oid);
    }

    public static boolean valueHasMappingSpec(PrismValue value, MappingSpecificationType mappingSpecification) {
        return value.<ValueMetadataType>getValueMetadataAsContainer().valuesStream()
                .anyMatch(md -> hasMappingSpecification(md.asContainerable(), mappingSpecification));
    }
}
