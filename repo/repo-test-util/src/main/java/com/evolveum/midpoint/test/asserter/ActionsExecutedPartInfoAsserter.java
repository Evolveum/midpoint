/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import static com.evolveum.midpoint.schema.statistics.ActionsExecutedInformationUtil.Part;
import static com.evolveum.midpoint.schema.statistics.ActionsExecutedInformationUtil.format;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityActionsExecutedType;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectActionsExecutedEntryType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Asserter that checks a single part of task "actions executed" information ("all" / "resulting").
 */
public class ActionsExecutedPartInfoAsserter<RA> extends AbstractAsserter<RA> {

    @NotNull private final ActivityActionsExecutedType information;
    @NotNull private final Part part;

    ActionsExecutedPartInfoAsserter(@NotNull ActivityActionsExecutedType information, RA returnAsserter, @NotNull Part part,
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

    public ActionsExecutedPartInfoAsserter<RA> assertSuccessCount(ChangeTypeType operation, QName objectType, int from, int to) {
        Counts counts = getCounts(e -> e.getOperation() == operation && QNameUtil.match(objectType, e.getObjectType()));
        assertMinMax("Wrong # of successes for " + operation + ":" + objectType, from, to, counts.success);
        return this;
    }

    public ActionsExecutedPartInfoAsserter<RA> assertFailureCount(ChangeTypeType operation, QName objectType, int from, int to) {
        Counts counts = getCounts(e -> e.getOperation() == operation && QNameUtil.match(objectType, e.getObjectType()));
        assertMinMax("Wrong # of failures for " + operation + ":" + objectType, from, to, counts.failure);
        return this;
    }

    public ActionsExecutedPartInfoAsserter<RA> assertCount(ChangeTypeType operation, QName objectType, int success, int failure) {
        Counts counts = getCounts(e -> e.getOperation() == operation && QNameUtil.match(objectType, e.getObjectType()));
        assertEquals("Wrong # of successes for " + operation + ":" + objectType, success, counts.success);
        assertEquals("Wrong # of failures for " + operation + ":" + objectType, failure, counts.failure);
        return this;
    }

    public ActionsExecutedPartInfoAsserter<RA> assertLastSuccessName(ChangeTypeType operation, QName objectType,
            String... expected) {
        Collection<String> successes = getLastSuccessName(
                e -> e.getOperation() == operation &&
                        QNameUtil.match(objectType, e.getObjectType()));
        assertThat(successes)
                .as("last successfully executed action targets for action %s, type=%s", operation, objectType)
                .containsExactlyInAnyOrder(expected);
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

    private Collection<String> getLastSuccessName(Predicate<ObjectActionsExecutedEntryType> predicate) {
        return getEntries().stream()
                .filter(predicate)
                .map(ObjectActionsExecutedEntryType::getLastSuccessObjectName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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

    public ActionsExecutedPartInfoAsserter<RA> assertChannels(String... uris) {
        assertThat(getAllChannels()).as("all channels").containsExactlyInAnyOrder(uris);
        return this;
    }

    public Collection<String> getAllChannels() {
        return getEntries().stream()
                .map(ObjectActionsExecutedEntryType::getChannel)
                .collect(Collectors.toSet());
    }
}
