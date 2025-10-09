/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;


public class VirtualContainerItemSpecificationAsserter<RA> extends AbstractAsserter<RA> {

    private VirtualContainerItemSpecificationType containerItem;

    public VirtualContainerItemSpecificationAsserter(VirtualContainerItemSpecificationType containerItem, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.containerItem = containerItem;
    }

    @Override
    protected String desc() {
        return "virtual container item";
    }
}
