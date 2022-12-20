/*
 * Copyright (C) 2018-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.util.ProvenanceMetadataUtil.hasOrigin;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;

public class ProvenanceMetadataAsserter<RA extends AbstractAsserter<?>>
        extends PrismContainerValueAsserter<ProvenanceMetadataType, RA> {

    ProvenanceMetadataAsserter(ProvenanceMetadataType metadata, RA returnAsserter, String detail) {
        //noinspection unchecked
        super(metadata.asPrismContainerValue(), returnAsserter, detail);
    }

    public AcquisitionMetadataAsserter<ProvenanceMetadataAsserter<RA>> singleAcquisition() {
        return new AcquisitionMetadataAsserter<>(getSingleAcquisition(), this, "acquisition in " + getDetails());
    }

    public AcquisitionMetadataAsserter<ProvenanceMetadataAsserter<RA>> singleAcquisition(String originOid) {
        return new AcquisitionMetadataAsserter<>(getSingleAcquisition(originOid), this, "acquisition in " + getDetails());
    }

    private ProvenanceAcquisitionType getSingleAcquisition() {
        List<ProvenanceAcquisitionType> acquisitions = getProvenance().getAcquisition();
        assertThat(acquisitions.size()).as("# of acquisitions in " + getDetails())
                .isEqualTo(1);
        return acquisitions.get(0);
    }

    @NotNull
    private ProvenanceMetadataType getProvenance() {
        return getPrismValue().asContainerable();
    }

    private ProvenanceAcquisitionType getSingleAcquisition(String originOid) {
        List<ProvenanceAcquisitionType> acquisitions = getProvenance().getAcquisition().stream()
                .filter(acquisition -> hasOrigin(acquisition, originOid))
                .collect(Collectors.toList());
        assertThat(acquisitions.size()).as("# of acquisitions with origin " + originOid + " in " + getDetails())
                .isEqualTo(1);
        return acquisitions.get(0);
    }

    public ProvenanceMetadataAsserter<RA> assertAcquisitions(int number) {
        assertThat(getProvenance().getAcquisition().size()).as("# of acquisitions").isEqualTo(number);
        return this;
    }

    public ProvenanceMetadataAsserter<RA> assertNoMappingSpec() {
        assertThat(getProvenance().getMappingSpecification()).as("mapping spec").isNull();
        return this;
    }

    public ProvenanceMetadataAsserter<RA> assertMappingSpec(String definitionObjectOid) {
        MappingSpecificationType mappingSpec = getMappingSpecification();
        assertThat(mappingSpec).as("mapping spec").isNotNull();
        assertThat(mappingSpec.getDefinitionObjectRef()).as("mapping spec definition object ref").isNotNull();
        assertThat(mappingSpec.getDefinitionObjectRef().getOid()).as("mapping spec definition object ref OID").isEqualTo(definitionObjectOid);
        return this;
    }

    private MappingSpecificationType getMappingSpecification() {
        return getProvenance().getMappingSpecification();
    }
}
