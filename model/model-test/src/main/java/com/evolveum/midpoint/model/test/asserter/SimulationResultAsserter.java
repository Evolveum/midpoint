/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import com.evolveum.midpoint.schema.simulation.SimulationMetricReference;
import com.evolveum.midpoint.schema.util.SimulationResultTypeUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BuiltInSimulationMetricType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import java.math.BigDecimal;

/**
 * Asserts on the collections of {@link SimulationResultType} objects.
 */
@SuppressWarnings("WeakerAccess")
public class SimulationResultAsserter<RA> extends AbstractAsserter<RA> {

    @NotNull private final SimulationResultType simulationResult;

    SimulationResultAsserter(@NotNull SimulationResultType simulationResult, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.simulationResult = simulationResult;
    }

    public static SimulationResultAsserter<Void> forResult(SimulationResultType simulationResult, String details) {
        return new SimulationResultAsserter<>(simulationResult, null, details);
    }

    public SimulationResultAsserter<RA> assertStartTimestampBetween(long start, long end) {
        TestUtil.assertBetween("Start timestamp in " + desc(), start, end, getStartTimestamp());
        return this;
    }

    public SimulationResultAsserter<RA> assertEndTimestampBetween(long start, long end) {
        TestUtil.assertBetween("End timestamp in " + desc(), start, end, getEndTimestamp());
        return this;
    }

    private Long getStartTimestamp() {
        return XmlTypeConverter.toMillis(simulationResult.getStartTimestamp());
    }

    private Long getEndTimestamp() {
        return XmlTypeConverter.toMillis(simulationResult.getEndTimestamp());
    }

    public SimulationResultAsserter<RA> assertMetricValue(SimulationMetricReference ref, BigDecimal expected) {
        assertThat(SimulationResultTypeUtil.getSummarizedMetricValue(simulationResult, ref))
                .as("metric " + ref + " value")
                .isEqualTo(expected);
        return this;
    }

    public SimulationResultAsserter<RA> assertMetricValueByEventMark(String oid, BigDecimal expected) {
        return assertMetricValue(SimulationMetricReference.forMark(oid), expected);
    }

    public SimulationResultAsserter<RA> assertMetricValueForBuiltIn(BuiltInSimulationMetricType builtIn, BigDecimal expected) {
        return assertMetricValue(SimulationMetricReference.forBuiltIn(builtIn), expected);
    }

    public SimulationResultAsserter<RA> assertObjectsAdded(int expected) {
        assertThat(SimulationResultTypeUtil.getObjectsAdded(simulationResult))
                .as("objects added")
                .isEqualTo(expected);
        return this;
    }

    public SimulationResultAsserter<RA> assertObjectsModified(int expected) {
        assertThat(SimulationResultTypeUtil.getObjectsModified(simulationResult))
                .as("objects modified")
                .isEqualTo(expected);
        return this;
    }

    public SimulationResultAsserter<RA> assertObjectsDeleted(int expected) {
        assertThat(SimulationResultTypeUtil.getObjectsDeleted(simulationResult))
                .as("objects deleted")
                .isEqualTo(expected);
        return this;
    }

    public SimulationResultAsserter<RA> assertObjectsProcessed(int expected) {
        assertThat(SimulationResultTypeUtil.getObjectsProcessed(simulationResult))
                .as("objects deleted")
                .isEqualTo(expected);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public SimulationResultAsserter<RA> assertMetricValueEntryCount(int expected) {
        assertThat(simulationResult.getMetric())
                .as("metric value entry set")
                .hasSize(expected);
        return this;
    }

    public SimulationResultType getObjectable() {
        return simulationResult;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public SimulationResultAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(simulationResult));
        return this;
    }
}
