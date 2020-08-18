/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.util.ProvenanceMetadataUtil.hasOrigin;

import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceYieldType;

public class ProvenanceMetadataAsserter<RA extends AbstractAsserter> extends PrismContainerValueAsserter<ProvenanceMetadataType, RA> {

    ProvenanceMetadataAsserter(ProvenanceMetadataType metadata, RA returnAsserter, String detail) {
        //noinspection unchecked
        super(metadata.asPrismContainerValue(), returnAsserter, detail);
    }

    public YieldMetadataAsserter<ProvenanceMetadataAsserter<RA>> singleYield() {
        return new YieldMetadataAsserter<>(getSingleYield(), this, "yield in " + getDetails());
    }

    public YieldMetadataAsserter<ProvenanceMetadataAsserter<RA>> singleYield(String originOid) {
        return new YieldMetadataAsserter<>(getSingleYield(originOid), this, "yield in " + getDetails());
    }

    private ProvenanceYieldType getSingleYield() {
        List<ProvenanceYieldType> yields = getPrismValue().asContainerable().getYield();
        assertThat(yields.size()).as("# of yields in " + getDetails())
                .isEqualTo(1);
        return yields.get(0);
    }

    private ProvenanceYieldType getSingleYield(String originOid) {
        List<ProvenanceYieldType> yields = getPrismValue().asContainerable().getYield().stream()
                .filter(yield -> hasOrigin(yield, originOid))
                .collect(Collectors.toList());
        assertThat(yields.size()).as("# of yields with origin " + originOid + " in " + getDetails())
                .isEqualTo(1);
        return yields.get(0);
    }
}
