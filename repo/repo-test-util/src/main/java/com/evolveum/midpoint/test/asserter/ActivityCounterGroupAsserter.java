/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCounterGroupType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCounterType;

/**
 * Asserter that checks activity counters (e.g. for thresholds implementation).
 */
@SuppressWarnings("WeakerAccess")
public class ActivityCounterGroupAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityCounterGroupType information;

    ActivityCounterGroupAsserter(ActivityCounterGroupType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public ActivityCounterGroupAsserter<RA> assertCounter(String identifier, int value) {
        assertThat(getValue(identifier)).as("value of " + identifier).isEqualTo(value);
        return this;
    }

    public ActivityCounterGroupAsserter<RA> assertCounterMinMax(String identifier, int min, int max) {
        assertMinMax("counter " + identifier, min, max, getValue(identifier));
        return this;
    }

    public ActivityCounterGroupAsserter<RA> assertTotal(int value) {
        assertThat(getTotal()).as("total").isEqualTo(value);
        return this;
    }


    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityCounterGroupAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(information));
        return this;
    }

    private int getValue(String identifier) {
        return information.getCounter().stream()
                .filter(c -> Objects.equals(c.getIdentifier(), identifier))
                .map(ActivityCounterType::getValue)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum(); // there should be at most one occurrence
    }

    private int getTotal() {
        return information.getCounter().stream()
                .map(ActivityCounterType::getValue)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
