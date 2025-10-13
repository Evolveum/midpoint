/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

    public VirtualContainerItemsSpecificationAsserter<RA> assertItem(ItemPath path) {
        List<VirtualContainerItemSpecificationType> foundItems = filterItems(path);
        Assertions.assertThat(foundItems).hasSize(1);
        return this;
        //TODO later
//        return new VirtualContainerItemSpecificationAsserter(foundItems.iterator().next(), this, "from item list " + containerItems);
    }

    public VirtualContainerItemsSpecificationAsserter<RA> assertNoItem(ItemPath path) {
        List<VirtualContainerItemSpecificationType> foundItems = filterItems(path);
        Assertions.assertThat(foundItems).isEmpty();
        return this;
    }

    private List<VirtualContainerItemSpecificationType> filterItems(ItemPath path) {
        return containerItems.stream()
                .filter(item -> item.getPath() != null && item.getPath().getItemPath().equivalent(path))
                .collect(Collectors.toList());

    }

    @Override
    protected String desc() {
        return "virtual container item";
    }
}
