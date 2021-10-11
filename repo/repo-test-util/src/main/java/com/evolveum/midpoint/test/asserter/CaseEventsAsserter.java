/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Asserts over a set of events.
 */
public class CaseEventsAsserter<RA> extends AbstractAsserter<CaseAsserter<RA>> {

    @NotNull private final CaseAsserter<RA> caseAsserter;
    @NotNull private final List<CaseEventType> events;

    public CaseEventsAsserter(@NotNull CaseAsserter<RA> caseAsserter, @NotNull List<CaseEventType> events, String details) {
        super(caseAsserter, details);
        this.caseAsserter = caseAsserter;
        this.events = events;
    }

    PrismObject<CaseType> getCase() {
        return caseAsserter.getObject();
    }

    public @NotNull List<CaseEventType> getEvents() {
        return events;
    }

    public CaseEventsAsserter<RA> assertEvents(int expected) {
        assertEquals("Wrong number of events in " + desc(), expected, getEvents().size());
        return this;
    }

    public CaseEventsAsserter<RA> assertNone() {
        assertEvents(0);
        return this;
    }

    CaseEventAsserter<CaseEventsAsserter<RA>> forEvent(CaseEventType event) {
        CaseEventAsserter<CaseEventsAsserter<RA>> asserter = new CaseEventAsserter<>(event, this, "event in "+ getCase());
        copySetupTo(asserter);
        return asserter;
    }

    public CaseEventAsserter<CaseEventsAsserter<RA>> single() {
        assertEvents(1);
        return forEvent(getEvents().get(0));
    }

    @Override
    protected String desc() {
        return descWithDetails("events in "+ getCase());
    }

    public CaseEventFinder<RA> by() {
        return new CaseEventFinder<>(this);
    }

    public CaseEventAsserter<CaseEventsAsserter<RA>> forStageNumber(Integer stageNumber) {
        return by()
                .stageNumber(stageNumber)
                .find();
    }

    public CaseEventAsserter<CaseEventsAsserter<RA>> forWorkItemId(Long workItemId) {
        return by()
                .workItemId(workItemId)
                .find();
    }

    public CaseEventAsserter<CaseEventsAsserter<RA>> ofType(Class<? extends CaseEventType> type) {
        return by()
                .type(type)
                .find();
    }

}
