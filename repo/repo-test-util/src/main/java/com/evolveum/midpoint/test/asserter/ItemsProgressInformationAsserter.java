/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import com.evolveum.midpoint.schema.util.task.ItemsProgressInformation;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;

/**
 *  Asserter that checks {@link ItemsProgressInformation} objects.
 */
@SuppressWarnings("unused")
public class ItemsProgressInformationAsserter<RA> extends AbstractAsserter<RA> {

    private final ItemsProgressInformation information;

    ItemsProgressInformationAsserter(ItemsProgressInformation information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public ItemsProgressInformationAsserter<RA> assertPresent() {
        assertThat(information).isNotNull();
        return this;
    }

    public ItemsProgressInformationAsserter<RA> assertProgressGreaterThanZero() {
        assertThat(information.getProgress()).as("progress").isGreaterThan(0);
        return this;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ItemsProgressInformationAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(information));
        return this;
    }

    public ItemsProgressInformation get() {
        return information;
    }

    public ItemsProgressInformationAsserter<RA> assertNoExpectedTotal() {
        assertThat(information.getExpectedTotal()).isNull();
        return this;
    }
}
