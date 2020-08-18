/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
