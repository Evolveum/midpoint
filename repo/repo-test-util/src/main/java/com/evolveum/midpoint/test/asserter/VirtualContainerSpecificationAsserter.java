/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import org.assertj.core.api.Assertions;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

public class VirtualContainerSpecificationAsserter<RA> extends AbstractAsserter<RA> {

    private final VirtualContainersSpecificationType virtualContainer;

    public VirtualContainerSpecificationAsserter(VirtualContainersSpecificationType virtualContainer, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.virtualContainer = virtualContainer;
    }

    public VirtualContainerSpecificationAsserter<RA> identifier(String identifier) {
        Assertions.assertThat(virtualContainer.getIdentifier()).isEqualTo(identifier);
        return this;
    }

    public VirtualContainerSpecificationAsserter<RA> visibility(UserInterfaceElementVisibilityType visibility) {
        Assertions.assertThat(virtualContainer.getVisibility()).isEqualTo(visibility);
        return this;
    }

    public DisplayTypeAsserter<VirtualContainerSpecificationAsserter<RA>> displayType() {
        return new DisplayTypeAsserter<>(virtualContainer.getDisplay(), this, "from virtual container " + virtualContainer);
    }

    public VirtualContainerSpecificationAsserter<RA> assertDisplayOrder(int order) {
        Assertions.assertThat(virtualContainer.getDisplayOrder()).isEqualTo(order);
        return this;
    }

    public VirtualContainerItemsSpecificationAsserter<VirtualContainerSpecificationAsserter<RA>> items() {
        return new VirtualContainerItemsSpecificationAsserter<>(
                virtualContainer.getItem(), this, "from virtual container " + virtualContainer);
    }

    public VirtualContainerSpecificationAsserter<RA> assertItems(int expectedSize) {
        Assertions.assertThat(virtualContainer.getItem()).hasSize(expectedSize);
        return this;
    }

    @Override
    protected String desc() {
        return "virtual containers";
    }
}
