/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collection;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;

/**
 * Asserts over a set of work items.
 */
public class CaseWorkItemsAsserter<RA, WI extends AbstractWorkItemType> extends AbstractAsserter<RA> {

    @NotNull private final Collection<WI> workItems;

    CaseWorkItemsAsserter(RA parentAsserter, @NotNull Collection<WI> workItems, String details) {
        super(parentAsserter, details);
        this.workItems = workItems;
    }

    public static <WI extends AbstractWorkItemType> CaseWorkItemsAsserter<Void, WI> forWorkItems(
            Collection<WI> workItems, String details) {
        return new CaseWorkItemsAsserter<>(null, workItems, details);
    }

    public @NotNull Collection<WI> getWorkItems() {
        return workItems;
    }

    public CaseWorkItemsAsserter<RA, WI> assertWorkItems(int expected) {
        assertEquals("Wrong number of work items in " + desc(), expected, getWorkItems().size());
        return this;
    }

    public CaseWorkItemsAsserter<RA, WI> assertNone() {
        assertWorkItems(0);
        return this;
    }

    CaseWorkItemAsserter<CaseWorkItemsAsserter<RA, WI>, WI> forWorkItem(WI workItem) {
        var asserter = new CaseWorkItemAsserter<>(workItem, this, "work item");
        copySetupTo(asserter);
        return asserter;
    }

    public CaseWorkItemAsserter<CaseWorkItemsAsserter<RA, WI>, WI> single() {
        assertWorkItems(1);
        return forWorkItem(getWorkItems().iterator().next());
    }

    public CaseWorkItemsAsserter<RA, WI> single(Consumer<CaseWorkItemAsserter<?, WI>> consumer) {
        consumer.accept(single());
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("work items");
    }

    public CaseWorkItemFinder<RA, WI> by() {
        return new CaseWorkItemFinder<>(this);
    }

    public CaseWorkItemAsserter<CaseWorkItemsAsserter<RA, WI>, WI> forStageNumber(Integer stageNumber) {
        return by()
                .stageNumber(stageNumber)
                .find();
    }

    public CaseWorkItemAsserter<CaseWorkItemsAsserter<RA, WI>, WI> forWorkItemId(Long workItemId) {
        return by()
                .workItemId(workItemId)
                .find();
    }

    public CaseWorkItemAsserter<CaseWorkItemsAsserter<RA, WI>, WI> forOriginalAssignee(String oid) {
        return by()
                .originalAssignee(oid)
                .find();
    }
}
