/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
