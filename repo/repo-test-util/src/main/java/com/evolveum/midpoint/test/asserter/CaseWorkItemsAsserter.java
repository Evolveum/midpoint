/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * Asserts over a set of work items.
 */
public class CaseWorkItemsAsserter<RA> extends AbstractAsserter<CaseAsserter<RA>> {

    @NotNull private final CaseAsserter<RA> caseAsserter;
    @NotNull private final List<CaseWorkItemType> workItems;

    public CaseWorkItemsAsserter(@NotNull CaseAsserter<RA> caseAsserter, @NotNull List<CaseWorkItemType> workItems, String details) {
        super(caseAsserter, details);
        this.caseAsserter = caseAsserter;
        this.workItems = workItems;
    }

    PrismObject<CaseType> getCase() {
        return caseAsserter.getObject();
    }

    public @NotNull List<CaseWorkItemType> getWorkItems() {
        return workItems;
    }

    public CaseWorkItemsAsserter<RA> assertWorkItems(int expected) {
        assertEquals("Wrong number of work items in " + desc(), expected, getWorkItems().size());
        return this;
    }

    public CaseWorkItemsAsserter<RA> assertNone() {
        assertWorkItems(0);
        return this;
    }

    CaseWorkItemAsserter<CaseWorkItemsAsserter<RA>> forWorkItem(CaseWorkItemType workItem) {
        CaseWorkItemAsserter<CaseWorkItemsAsserter<RA>> asserter = new CaseWorkItemAsserter<>(workItem, this, "work item in "+ getCase());
        copySetupTo(asserter);
        return asserter;
    }

    public CaseWorkItemAsserter<CaseWorkItemsAsserter<RA>> single() {
        assertWorkItems(1);
        return forWorkItem(getWorkItems().get(0));
    }

    @Override
    protected String desc() {
        return descWithDetails("work items in "+ getCase());
    }

    public CaseWorkItemFinder<RA> by() {
        return new CaseWorkItemFinder<>(this);
    }

    public CaseWorkItemAsserter<CaseWorkItemsAsserter<RA>> forStageNumber(Integer stageNumber) {
        return by()
                .stageNumber(stageNumber)
                .find();
    }

    public CaseWorkItemAsserter<CaseWorkItemsAsserter<RA>> forWorkItemId(Long workItemId) {
        return by()
                .workItemId(workItemId)
                .find();
    }
}
