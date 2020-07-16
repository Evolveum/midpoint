/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.stream.Collectors;

public class VirtualContainersSpecificationFinder<RA> {

    private VirtualContainersSpecificationAsserter<RA> virtualContainersAsserter;
    private String identifier;

    public VirtualContainersSpecificationFinder(VirtualContainersSpecificationAsserter<RA> virtualContainersAsserter) {
        this.virtualContainersAsserter = virtualContainersAsserter;
    }

    public VirtualContainersSpecificationFinder<RA> identifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public VirtualContainerSpecificationAsserter<VirtualContainersSpecificationAsserter<RA>> find() {
        List<VirtualContainersSpecificationType> foundVirtualContainers = virtualContainersAsserter.getVirtualContainers()
                .stream()
                .filter(vc -> identifier != null && identifier.equals(vc.getIdentifier()))
                .collect(Collectors.toList());
        Assertions.assertThat(foundVirtualContainers).hasSize(1);
        return new VirtualContainerSpecificationAsserter<>(foundVirtualContainers.iterator().next(), virtualContainersAsserter, "from list of virtual containers " + virtualContainersAsserter.getVirtualContainers());
    }
}
