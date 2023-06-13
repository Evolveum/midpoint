/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static com.evolveum.midpoint.prism.Referencable.getOid;

import org.jetbrains.annotations.NotNull;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;

import java.util.function.Consumer;

public class CaseWorkItemFinder<RA, WI extends AbstractWorkItemType> {

    private final CaseWorkItemsAsserter<RA, WI> workItemsAsserter;
    private Integer stageNumber;
    private Long workItemId;
    private String originalAssigneeOid;

    public CaseWorkItemFinder(CaseWorkItemsAsserter<RA, WI> workItemsAsserter) {
        this.workItemsAsserter = workItemsAsserter;
    }

    public CaseWorkItemFinder<RA, WI> stageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
        return this;
    }

    public CaseWorkItemFinder<RA, WI> workItemId(Long workItemId) {
        this.workItemId = workItemId;
        return this;
    }

    public CaseWorkItemFinder<RA, WI> originalAssignee(String oid) {
        this.originalAssigneeOid = oid;
        return this;
    }

    public CaseWorkItemsAsserter<RA, WI> find(Consumer<CaseWorkItemAsserter<?, WI>> consumer) {
        consumer.accept(find());
        return workItemsAsserter;
    }

    public CaseWorkItemAsserter<CaseWorkItemsAsserter<RA, WI>, WI> find() {
        return workItemsAsserter.forWorkItem(findInternal());
    }

    private @NotNull WI findInternal() {
        WI found = null;
        for (WI workItem: workItemsAsserter.getWorkItems()) {
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
            return found;
        }
    }

    public CaseWorkItemsAsserter<RA, WI> assertNone() {
        for (WI workItem: workItemsAsserter.getWorkItems()) {
            if (matches(workItem)) {
                fail("Found workItem while not expecting it: " + workItem);
            }
        }
        return workItemsAsserter;
    }

    public CaseWorkItemsAsserter<RA, WI> assertAll() {
        for (WI workItem: workItemsAsserter.getWorkItems()) {
            if (!matches(workItem)) {
                fail("Found work item that does not match search criteria: "+workItem);
            }
        }
        return workItemsAsserter;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean matches(WI workItem) {
        if (stageNumber != null && !stageNumber.equals(workItem.getStageNumber())) {
            return false;
        }
        if (workItemId != null && !workItemId.equals(workItem.getId())) {
            return false;
        }
        if (originalAssigneeOid != null
                && !originalAssigneeOid.equals(getOid(workItem.getOriginalAssigneeRef()))) {
            return false;
        }
        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }
}
