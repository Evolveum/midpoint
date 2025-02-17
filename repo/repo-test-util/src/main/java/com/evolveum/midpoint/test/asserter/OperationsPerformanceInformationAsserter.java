/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.Format.TEXT;
import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.SortBy.TIME;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationPrinter;
import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceInformation;

import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;

import org.jetbrains.annotations.NotNull;

/**
 * Asserter that checks {@link OperationsPerformanceInformation} objects.
 */
public class OperationsPerformanceInformationAsserter<RA> extends AbstractAsserter<RA> {

    @NotNull private final OperationsPerformanceInformation information;

    public OperationsPerformanceInformationAsserter(
            @NotNull OperationsPerformanceInformation information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = Objects.requireNonNull(information);
    }

    public OperationsPerformanceInformationAsserter<RA> display() {
        var performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());

        var printer = new OperationsPerformanceInformationPrinter(
                performanceInformation,
                new AbstractStatisticsPrinter.Options(TEXT, TIME),
                null, null, false);

        IntegrationTestTools.display(desc(), printer.print());
        return this;
    }

    public OperationsPerformanceInformation get() {
        return information;
    }

    @Override
    protected String desc() {
        return descWithDetails("operations performance information");
    }

    public OperationsPerformanceInformationAsserter<RA> assertInvocationCount(String name, int expected) {
        assertThat(information.getInvocationCount(name))
                .as("invocation count for " + name + " in " + desc())
                .isEqualTo(expected);
        return this;
    }

    public OperationsPerformanceInformationAsserter<RA> assertTotalTimeBetween(String name, long low, long high) {
        assertThat(information.getTotalTime(name))
                .as("total time for " + name + " in " + desc())
                .isBetween(low, high);
        return this;
    }

    public OperationsPerformanceInformationAsserter<RA> assertOwnTimeBetween(String name, long low, long high) {
        assertThat(information.getOwnTime(name))
                .as("own time for " + name + " in " + desc())
                .isBetween(low, high);
        return this;
    }
}
