/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;
import org.assertj.core.api.Assertions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemConstraintType;

public class ItemConstraintAsserter<RA> extends AbstractAsserter<RA> {

    private ItemConstraintType itemConstraint;

    public ItemConstraintAsserter(ItemConstraintType itemConstraint, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.itemConstraint = itemConstraint;
    }

    @Override
    protected String desc() {
        return descWithDetails("item constraint");
    }

    public ItemConstraintAsserter<RA> assertVisibility(UserInterfaceElementVisibilityType visibility) {
        Assertions.assertThat(itemConstraint.getVisibility()).isEqualTo(visibility);
        return this;
    }




}
