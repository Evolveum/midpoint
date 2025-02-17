/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.ComponentsPerformanceInformationPrinter;
import com.evolveum.midpoint.schema.statistics.ComponentsPerformanceInformationUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ComponentsPerformanceInformationType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.Format.TEXT;
import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.SortBy.TIME;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserter that checks {@link ComponentsPerformanceInformationType} objects.
 */
@SuppressWarnings("UnusedReturnValue")
public class ComponentsPerformanceInformationAsserter<RA> extends AbstractAsserter<RA> {

    @NotNull private final ComponentsPerformanceInformationType information;

    public ComponentsPerformanceInformationAsserter(
            @NotNull ComponentsPerformanceInformationType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = Objects.requireNonNull(information);
    }

    public ComponentsPerformanceInformationAsserter<RA> display() {
        var printer = new ComponentsPerformanceInformationPrinter(
                information,
                new AbstractStatisticsPrinter.Options(TEXT, TIME),
                null);

        IntegrationTestTools.display(desc(), printer.print());
        return this;
    }

    public @NotNull ComponentsPerformanceInformationType get() {
        return information;
    }

    @Override
    protected String desc() {
        return descWithDetails("components performance information");
    }

    public ComponentsPerformanceInformationAsserter<RA> assertTotalTimeBetween(String name, long low, long high) {
        assertThat(ComponentsPerformanceInformationUtil.getTotalTime(information, name))
                .as("total time for " + name + " in " + desc())
                .isBetween(low, high);
        return this;
    }

    public ComponentsPerformanceInformationAsserter<RA> assertTotalTime(String name, Long expected) {
        assertThat(ComponentsPerformanceInformationUtil.getTotalTime(information, name))
                .as("total time for " + name + " in " + desc())
                .isEqualTo(expected);
        return this;
    }
}
