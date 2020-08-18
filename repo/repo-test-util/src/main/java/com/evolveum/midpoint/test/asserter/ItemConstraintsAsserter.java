/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemConstraintType;

import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.stream.Collectors;

public class ItemConstraintsAsserter<RA> extends AbstractAsserter<RA> {

    private List<ItemConstraintType> itemConstraintTypeList;

    public ItemConstraintsAsserter(List<ItemConstraintType> itemConstraintTypeList, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.itemConstraintTypeList = itemConstraintTypeList;
    }

    @Override
    protected String desc() {
        return descWithDetails("item constraint");
    }

    private List<ItemConstraintType> getItemConstraintList() {
        Assertions.assertThat(itemConstraintTypeList).isNotEmpty();
        return itemConstraintTypeList;
    }

    public ItemConstraintAsserter<ItemConstraintsAsserter<RA>> itemConstraint(ItemPath itemPath) {
        List<ItemConstraintType> foundItems = filterItems(itemPath);
        Assertions.assertThat(foundItems).isNotEmpty().hasSize(1);
        return new ItemConstraintAsserter<>(foundItems.iterator().next(), this, "for " + itemConstraintTypeList);
    }

    public ItemConstraintsAsserter<RA> assertNoItemConstraint(ItemPath itemPath) {
        List<ItemConstraintType> foundItems =filterItems(itemPath);
        Assertions.assertThat(foundItems).isEmpty();
        return this;
    }

    private List<ItemConstraintType> filterItems(ItemPath itemPath) {
        return getItemConstraintList().stream()
                .filter(i -> i.getPath() != null && i.getPath().getItemPath().equivalent(itemPath))
                .collect(Collectors.toList());
    }


}
