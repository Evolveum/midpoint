/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import java.util.Objects;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventType;

/**
 *
 */
public class CaseEventFinder<RA> {

    private final CaseEventsAsserter<RA> eventsAsserter;
    private Integer stageNumber;
    private Long workItemId;
    private Class<? extends CaseEventType> type;

    public CaseEventFinder(CaseEventsAsserter<RA> eventsAsserter) {
        this.eventsAsserter = eventsAsserter;
    }

    public CaseEventFinder<RA> stageNumber(Integer stageNumber) {
        this.stageNumber = stageNumber;
        return this;
    }

    public CaseEventFinder<RA> workItemId(Long workItemId) {
        this.workItemId = workItemId;
        return this;
    }

    public CaseEventFinder<RA> type(Class<? extends CaseEventType> type) {
        this.type = type;
        return this;
    }

    public CaseEventAsserter<CaseEventsAsserter<RA>> find() {
        CaseEventType found = null;
        for (CaseEventType event: eventsAsserter.getEvents()) {
            if (matches(event)) {
                if (found == null) {
                    found = event;
                } else {
                    fail("Found more than one event that matches search criteria");
                }
            }
        }
        if (found == null) {
            throw new AssertionError("Found no event that matches search criteria");
        } else {
            return eventsAsserter.forEvent(found);
        }
    }

    public CaseEventsAsserter<RA> assertNone() {
        for (CaseEventType event: eventsAsserter.getEvents()) {
            if (matches(event)) {
                fail("Found event while not expecting it: " + event);
            }
        }
        return eventsAsserter;
    }

    public CaseEventsAsserter<RA> assertAll() {
        for (CaseEventType event: eventsAsserter.getEvents()) {
            if (!matches(event)) {
                fail("Found event that does not match search criteria: "+event);
            }
        }
        return eventsAsserter;
    }

    private boolean matches(CaseEventType event) {
        if (stageNumber != null) {
            if (!Objects.equals(stageNumber, event.getStageNumber())) {
                return false;
            }
        }
        if (workItemId != null) {
            if (!(event instanceof WorkItemEventType) ||
                    !Objects.equals(workItemId, ((WorkItemEventType) event).getWorkItemId())) {
                return false;
            }
        }
        if (type != null) {
            if (!type.isAssignableFrom(event.getClass())) {
                return false;
            }
        }
        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

}
