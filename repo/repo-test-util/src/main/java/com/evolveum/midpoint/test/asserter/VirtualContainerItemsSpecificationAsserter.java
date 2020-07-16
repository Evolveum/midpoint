/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;

import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.stream.Collectors;

public class VirtualContainerItemsSpecificationAsserter<RA> extends AbstractAsserter<RA> {

    private List<VirtualContainerItemSpecificationType> containerItems;

    public VirtualContainerItemsSpecificationAsserter(List<VirtualContainerItemSpecificationType> containerItems, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.containerItems = containerItems;
    }

    public VirtualContainerItemsSpecificationAsserter<RA> item(ItemPath path) {
        List<VirtualContainerItemSpecificationType> foundItems = containerItems.stream()
                .filter(item -> item.getPath() != null && item.getPath().getItemPath().equivalent(path))
                .collect(Collectors.toList());
        Assertions.assertThat(foundItems).hasSize(1);
        return this;
        //TODO later
//        return new VirtualContainerItemSpecificationAsserter(foundItems.iterator().next(), this, "from item list " + containerItems);
    }

    @Override
    protected String desc() {
        return "virtual container item";
    }
}
