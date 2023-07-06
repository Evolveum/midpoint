/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.schema.validator.UpgradeValidationItem;

import org.assertj.core.api.Assertions;

public class UpgradeValidationItemAsserter {

    private final UpgradeValidationItem item;

    public UpgradeValidationItemAsserter(UpgradeValidationItem item) {
        this.item = item;
    }

    public UpgradeValidationItemAsserter assertPath(ItemPath path) {
        if (!path.equivalent(item.getItem().getItemPath())) {
            Assertions.fail("Expected path " + path + " but was " + item.getItem().getItemPath());
        }
        return this;
    }

    public UpgradeValidationItemAsserter assertType(UpgradeType expected) {
        if (expected != item.getType()) {
            Assertions.fail("Expected type " + expected + " but was " + item.getType());
        }
        return this;
    }
}
