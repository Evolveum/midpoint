/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VirtualContainersSpecificationFinder<RA> extends UserInterfaceFeatureFinder<VirtualContainerSpecificationAsserter<VirtualContainersSpecificationAsserter<RA>>, VirtualContainersSpecificationType> {

    private VirtualContainersSpecificationAsserter<RA> virtualContainersAsserter;

    public VirtualContainersSpecificationFinder(VirtualContainersSpecificationAsserter<RA> virtualContainersAsserter) {
        this.virtualContainersAsserter = virtualContainersAsserter;
    }

    @Override
    public VirtualContainersSpecificationFinder<RA> identifier(String identifier) {
        super.identifier(identifier);
        return this;
    }

    @Override
    public VirtualContainersSpecificationFinder<RA> displayName(String displayName) {
        super.displayName(displayName);
        return this;
    }

    @Override
    public VirtualContainerSpecificationAsserter<VirtualContainersSpecificationAsserter<RA>> find() {
        return super.find();
    }

    @Override
    protected VirtualContainerSpecificationAsserter<VirtualContainersSpecificationAsserter<RA>> find(Predicate<VirtualContainersSpecificationType> filter) {
        List<VirtualContainersSpecificationType> foundVirtualContainers = virtualContainersAsserter.getVirtualContainers()
                .stream()
                .filter(filter)
                .collect(Collectors.toList());
        Assertions.assertThat(foundVirtualContainers).hasSize(1);
        return new VirtualContainerSpecificationAsserter<>(foundVirtualContainers.iterator().next(), virtualContainersAsserter, "from list of virtual containers " + virtualContainersAsserter.getVirtualContainers());
    }

}
