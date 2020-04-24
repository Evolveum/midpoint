/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import java.util.Objects;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 *
 */
public class CaseWorkItemFinder<RA> {

    private final CaseWorkItemsAsserter<RA> workItemsAsserter;
    private Integer stageNumber;
    private Long workItemId;

    public CaseWorkItemFinder(CaseWorkItemsAsserter<RA> workItemsAsserter) {
        this.workItemsAsserter = workItemsAsserter;
    }

    public CaseWorkItemFinder<RA> stageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
        return this;
    }

    public CaseWorkItemFinder<RA> workItemId(Long workItemId) {
        this.workItemId = workItemId;
        return this;
    }

    public CaseWorkItemAsserter<CaseWorkItemsAsserter<RA>> find() {
        CaseWorkItemType found = null;
        for (CaseWorkItemType workItem: workItemsAsserter.getWorkItems()) {
            if (matches(workItem)) {
                if (found == null) {
                    found = workItem;
                } else {
                    fail("Found more than one workItem that matches search criteria");
                }
            }
        }
        if (found == null) {
            throw new AssertionError("Found no work item that matches search criteria");
        } else {
            return workItemsAsserter.forWorkItem(found);
        }
    }

    public CaseWorkItemsAsserter<RA> assertNone() {
        for (CaseWorkItemType workItem: workItemsAsserter.getWorkItems()) {
            if (matches(workItem)) {
                fail("Found workItem while not expecting it: " + workItem);
            }
        }
        return workItemsAsserter;
    }

    public CaseWorkItemsAsserter<RA> assertAll() {
        for (CaseWorkItemType workItem: workItemsAsserter.getWorkItems()) {
            if (!matches(workItem)) {
                fail("Found work item that does not match search criteria: "+workItem);
            }
        }
        return workItemsAsserter;
    }

    private boolean matches(CaseWorkItemType workItem) {
        if (stageNumber != null) {
            if (!Objects.equals(stageNumber, workItem.getStageNumber())) {
                return false;
            }
        }
        if (workItemId != null) {
            if (!Objects.equals(workItemId, workItem.getId())) {
                return false;
            }
        }
        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }
}
