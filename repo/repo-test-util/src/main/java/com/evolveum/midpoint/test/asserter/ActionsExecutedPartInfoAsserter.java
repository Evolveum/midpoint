/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import static com.evolveum.midpoint.schema.statistics.ActionsExecutedInformation.Part;
import static com.evolveum.midpoint.schema.statistics.ActionsExecutedInformation.format;

import java.util.List;
import java.util.function.Predicate;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActionsExecutedInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectActionsExecutedEntryType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Asserter that checks a single part of task "actions executed" information ("all" / "resulting").
 */
public class ActionsExecutedPartInfoAsserter<RA> extends AbstractAsserter<RA> {

    @NotNull private final ActionsExecutedInformationType information;
    @NotNull private final Part part;

    ActionsExecutedPartInfoAsserter(@NotNull ActionsExecutedInformationType information, RA returnAsserter, @NotNull Part part,
            String details) {
        super(returnAsserter, details);
        this.information = information;
        this.part = part;
    }

    private static class Counts {
        private int success;
        private int failure;

        public void add(int successIncrement, int failureIncrement) {
            success += successIncrement;
            failure += failureIncrement;
        }
    }

    public ActionsExecutedPartInfoAsserter<RA> assertCount(ChangeTypeType operation, QName objectType, int success, int failure) {
        Counts counts = getCounts(e -> e.getOperation() == operation && QNameUtil.match(objectType, e.getObjectType()));
        assertEquals("Wrong # of successes for " + operation + ":" + objectType, success, counts.success);
        assertEquals("Wrong # of failures for " + operation + ":" + objectType, failure, counts.failure);
        return this;
    }

    public ActionsExecutedPartInfoAsserter<RA> assertCount(QName objectType, int success, int failure) {
        Counts counts = getCounts(e -> QNameUtil.match(objectType, e.getObjectType()));
        assertEquals("Wrong # of successes for " + objectType, success, counts.success);
        assertEquals("Wrong # of failures for " + objectType, failure, counts.failure);
        return this;
    }

    public ActionsExecutedPartInfoAsserter<RA> assertCount(int success, int failure) {
        Counts counts = getCounts(e -> true);
        assertEquals("Wrong # of successes", success, counts.success);
        assertEquals("Wrong # of failures", failure, counts.failure);
        return this;
    }

    private Counts getCounts(Predicate<ObjectActionsExecutedEntryType> predicate) {
        Counts counts = new Counts();
        getEntries().stream()
                .filter(predicate)
                .forEach(e -> counts.add(e.getTotalSuccessCount(), e.getTotalFailureCount()));
        return counts;
    }

    private List<ObjectActionsExecutedEntryType> getEntries() {
        switch (part) {
            case ALL:
                return information.getObjectActionsEntry();
            case RESULTING:
                return information.getResultingObjectActionsEntry();
            default:
                throw new AssertionError(part);
        }
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActionsExecutedPartInfoAsserter<RA> display() {
        IntegrationTestTools.display(desc(), format(information, part));
        return this;
    }

    public ActionsExecutedPartInfoAsserter<RA> assertEmpty() {
        assertTrue("all object actions list is not empty", information.getObjectActionsEntry().isEmpty());
        assertTrue("resulting object actions list is not empty", information.getResultingObjectActionsEntry().isEmpty());
        return this;
    }


}
