/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.schema.merger.threeway.MergeResult;

public class AssertMergeFragment {

    private MergeResult result;

    public AssertMergeFragment(MergeResult result) {
        AssertJUnit.assertNotNull(result);

        this.result = result;
    }

    public AssertMergeFragment assertHasConflict() {
        AssertJUnit.assertTrue("Result should have at least one conflict fragment", result.hasConflict());
        return this;
    }

    public AssertMergeFragment assertNoConflict() {
        AssertJUnit.assertFalse("Result should not have conflict fragment", !result.hasConflict());
        return this;
    }

    public AssertMergeFragment assertSize(int expected) {
        AssertJUnit.assertEquals("Incorrect result size", expected, result.fragments().size());
        return this;
    }

}
