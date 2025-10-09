/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.assertj.core.api.Assertions;

import java.util.List;

public class VirtualContainersSpecificationAsserter<RA> extends AbstractAsserter<RA> {

    private List<VirtualContainersSpecificationType> virtualContainers;

    public VirtualContainersSpecificationAsserter(List<VirtualContainersSpecificationType> virtualContainers, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.virtualContainers = virtualContainers;
    }

    public VirtualContainersSpecificationAsserter<RA> assertSize(int expectedContainers) {
        Assertions.assertThat(virtualContainers).hasSize(expectedContainers);
        return this;
    }

    List<VirtualContainersSpecificationType> getVirtualContainers() {
        Assertions.assertThat(virtualContainers).isNotEmpty();
        return virtualContainers;
    }

    public VirtualContainersSpecificationFinder by() {
        return new VirtualContainersSpecificationFinder(this);
    }

    public VirtualContainerSpecificationAsserter<VirtualContainersSpecificationAsserter<RA>> byIdentifier(String identifier) {
        return by().identifier(identifier).find();
    }

    public VirtualContainerSpecificationAsserter<VirtualContainersSpecificationAsserter<RA>> byDisplayName(String displayName) {
        return by().displayName(displayName).find();
    }

    @Override
    protected String desc() {
        return "virtual containers";
    }
}
