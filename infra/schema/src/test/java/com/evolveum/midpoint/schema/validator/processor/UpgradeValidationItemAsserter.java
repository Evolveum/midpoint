/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import org.assertj.core.api.Assertions;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.schema.validator.UpgradeValidationItem;

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

    public UpgradeValidationItemAsserter assertPhase(UpgradePhase expected) {
        if (expected != item.getPhase()) {
            Assertions.fail("Expected phase " + expected + " but was " + item.getPhase());
        }
        return this;
    }

    public UpgradeValidationItemAsserter assertChanged() {
        if (!item.isChanged()) {
            Assertions.fail("Expected changed but was not");
        }
        return this;
    }

    public UpgradeValidationItemAsserter assertUnchanged() {
        if (item.isChanged()) {
            Assertions.fail("Expected changed but was not");
        }
        return this;
    }

    // todo assert deltas using class repo-test-util - ObjectDeltaAsserter. move it somewhere lower
}
