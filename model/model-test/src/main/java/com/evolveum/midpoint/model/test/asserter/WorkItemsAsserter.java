/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.CaseWorkItemAsserter;
import com.evolveum.midpoint.test.asserter.CaseWorkItemsAsserter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Asserts on the collections of {@link CaseWorkItemType} instances.
 *
 * TODO merge with {@link CaseWorkItemsAsserter}? That one is bound to {@link CaseType} object for now
 */
@SuppressWarnings("WeakerAccess")
public class WorkItemsAsserter<RA> extends AbstractAsserter<RA> {

    @NotNull private final Collection<CaseWorkItemType> workItems;

    WorkItemsAsserter(Collection<CaseWorkItemType> workItems, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.workItems = emptyIfNull(workItems);
    }

    public static WorkItemsAsserter<Void> forWorkItems(
            Collection<CaseWorkItemType> workItems, String details) {
        return new WorkItemsAsserter<>(workItems, null, details);
    }

    public @NotNull Collection<CaseWorkItemType> getWorkItems() {
        return workItems;
    }

    public WorkItemsAsserter<RA> assertSize(int expected) {
        assertThat(workItems).as("work items").hasSize(expected);
        return this;
    }

    public WorkItemsAsserter<RA> assertSizeBetween(int min, int max) {
        assertThat(workItems).as("work items")
                .hasSizeGreaterThanOrEqualTo(min)
                .hasSizeLessThanOrEqualTo(max);
        return this;
    }

    public <O extends ObjectType> CaseWorkItemAsserter<WorkItemsAsserter<RA>> single() {
        assertSize(1);
        CaseWorkItemAsserter<WorkItemsAsserter<RA>> asserter =
                new CaseWorkItemAsserter<>(
                        workItems.iterator().next(),
                        this,
                        "single work item in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public WorkItemsAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(workItems));
        return this;
    }
}
