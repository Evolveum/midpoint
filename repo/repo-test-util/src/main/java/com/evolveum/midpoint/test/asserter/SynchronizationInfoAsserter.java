/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType.*;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *  Asserter that checks task synchronization information.
 */
public class SynchronizationInfoAsserter<RA> extends AbstractAsserter<RA> {

    private final SynchronizationInformationType information;

    SynchronizationInfoAsserter(SynchronizationInformationType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public SynchronizationInfoAsserter<RA> assertProtected(int expectedBefore, int expectedAfter) {
        return assertCounter(F_COUNT_PROTECTED, expectedBefore, expectedAfter);
    }

    public SynchronizationInfoAsserter<RA> assertUnmatched(int expectedBefore, int expectedAfter) {
        return assertCounter(F_COUNT_UNMATCHED, expectedBefore, expectedAfter);
    }

    public SynchronizationInfoAsserter<RA> assertUnlinked(int expectedBefore, int expectedAfter) {
        return assertCounter(F_COUNT_UNLINKED, expectedBefore, expectedAfter);
    }

    public SynchronizationInfoAsserter<RA> assertLinked(int expectedBefore, int expectedAfter) {
        return assertCounter(F_COUNT_LINKED, expectedBefore, expectedAfter);
    }

    public SynchronizationInfoAsserter<RA> assertDeleted(int expectedBefore, int expectedAfter) {
        return assertCounter(F_COUNT_DELETED, expectedBefore, expectedAfter);
    }

    @SuppressWarnings("WeakerAccess")
    public SynchronizationInfoAsserter<RA> assertCounter(ItemName name, int expectedBefore, int expectedAfter) {
        assertCounterBefore(name, expectedBefore);
        assertCounterAfter(name, expectedAfter);
        return this;
    }

    private void assertCounterBefore(ItemName name, int expected) {
        assertEquals("Wrong # of 'before' value for " + name, expected, get(name, false));
    }

    private void assertCounterAfter(ItemName name, int expected) {
        assertEquals("Wrong # of 'after' value for " + name, expected, get(name, true));
    }

    private int get(ItemName counterName, boolean after) {
        String getterName = "get" + StringUtils.capitalize(counterName.getLocalPart()) + (after ? "After" : "");
        try {
            Method getter = information.getClass().getMethod(getterName);
            return (Integer) getter.invoke(information);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError("Couldn't find/invoke getter " + getterName + ": " + e.getMessage(), e);
        }
    }

    public SynchronizationInfoAsserter<RA> assertTotal(int expectedBefore, int expectedAfter) {
        assertEquals("Wrong sum of 'before' values", expectedBefore, getAllBefore());
        assertEquals("Wrong sum of 'after' values", expectedAfter, getAllAfter());
        return this;
    }

    private int getAllBefore() {
        return information.getCountProtected()
                + information.getCountNoSynchronizationPolicy()
                + information.getCountSynchronizationDisabled()
                + information.getCountNotApplicableForTask()
                + information.getCountDeleted()
                + information.getCountDisputed()
                + information.getCountLinked()
                + information.getCountUnlinked()
                + information.getCountUnmatched();
    }

    private int getAllAfter() {
        return information.getCountProtectedAfter()
                + information.getCountNoSynchronizationPolicyAfter()
                + information.getCountSynchronizationDisabledAfter()
                + information.getCountNotApplicableForTaskAfter()
                + information.getCountDeletedAfter()
                + information.getCountDisputedAfter()
                + information.getCountLinkedAfter()
                + information.getCountUnlinkedAfter()
                + information.getCountUnmatchedAfter();
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public SynchronizationInfoAsserter display() {
        IntegrationTestTools.display(desc(), SynchronizationInformation.format(information));
        return this;
    }
}
