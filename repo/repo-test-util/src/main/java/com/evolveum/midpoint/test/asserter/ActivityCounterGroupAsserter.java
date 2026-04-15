/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCounterGroupType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCounterType;

import org.assertj.core.api.Assertions;

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
        List<Integer> values = information.getCounter().stream()
                .filter(c -> Objects.equals(c.getIdentifier(), identifier))
                .map(ActivityCounterType::getValue)
                .filter(Objects::nonNull)
                .toList();

        Assertions.assertThat(values)
                .withFailMessage("Counter '%s' not found or has no value", identifier)
                .isNotEmpty();

        Assertions.assertThat(values)
                .withFailMessage("Counter '%s' found multiple times in %s", identifier, values.size())
                .hasSize(1);

        return values.stream()
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
