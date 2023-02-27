/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.simulation;

import java.math.BigDecimal;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricAggregationFunctionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricPartitionType;

import static com.evolveum.midpoint.util.MiscUtil.or0;

/**
 * Parsed form of {@link SimulationMetricPartitionType}.
 *
 * Created for fast, simple, and thread-safe aggregation.
 */
public class SimulationMetricPartition {

    private int selectionSize;
    private BigDecimal selectionTotalValue = BigDecimal.ZERO;
    private int domainSize;
    private BigDecimal domainTotalValue = BigDecimal.ZERO;

    private BigDecimal selectionMinValue;
    private BigDecimal selectionMaxValue;
    private BigDecimal domainMinValue;
    private BigDecimal domainMaxValue;

    @SuppressWarnings("DuplicatedCode")
    public synchronized void addObject(BigDecimal sourceMetricValue, boolean inSelection) {
        if (domainMinValue == null || sourceMetricValue.compareTo(domainMinValue) < 0) {
            domainMinValue = sourceMetricValue;
        }
        if (domainMaxValue == null || sourceMetricValue.compareTo(domainMaxValue) > 0) {
            domainMaxValue = sourceMetricValue;
        }
        domainSize++;
        domainTotalValue = domainTotalValue.add(sourceMetricValue);

        if (inSelection) {
            if (selectionMinValue == null || sourceMetricValue.compareTo(selectionMinValue) < 0) {
                selectionMinValue = sourceMetricValue;
            }
            if (selectionMaxValue == null || sourceMetricValue.compareTo(selectionMaxValue) > 0) {
                selectionMaxValue = sourceMetricValue;
            }
            selectionSize++;
            selectionTotalValue = selectionTotalValue.add(sourceMetricValue);
        }
    }

    synchronized void addOtherPartition(SimulationMetricPartitionType other) {
        BigDecimal otherDomainMinValue = other.getDomainMinValue();
        if (domainMinValue == null || otherDomainMinValue != null && otherDomainMinValue.compareTo(domainMinValue) < 0) {
            domainMinValue = otherDomainMinValue;
        }
        BigDecimal otherDomainMaxValue = other.getDomainMaxValue();
        if (domainMaxValue == null || otherDomainMaxValue != null && otherDomainMaxValue.compareTo(domainMaxValue) > 0) {
            domainMaxValue = otherDomainMaxValue;
        }
        domainSize += or0(other.getDomainSize());
        domainTotalValue = domainTotalValue.add(or0(other.getDomainTotalValue()));

        BigDecimal otherSelectionMinValue = other.getSelectionMinValue();
        if (selectionMinValue == null || otherSelectionMinValue != null && otherSelectionMinValue.compareTo(selectionMinValue) < 0) {
            selectionMinValue = otherSelectionMinValue;
        }
        BigDecimal otherSelectionMaxValue = other.getSelectionMaxValue();
        if (selectionMaxValue == null || otherSelectionMaxValue != null && otherSelectionMaxValue.compareTo(selectionMaxValue) > 0) {
            selectionMaxValue = otherSelectionMaxValue;
        }
        selectionSize += or0(other.getSelectionSize());
        selectionTotalValue = selectionTotalValue.add(or0(other.getSelectionTotalValue()));
    }

    public synchronized SimulationMetricPartitionType toBean(
            PartitionScope key, SimulationMetricAggregationFunctionType function) {
        var bean = toBean(key);
        bean.setValue(
                SimulationMetricComputer.computeValue(bean, function, null));
        return bean;
    }

    private SimulationMetricPartitionType toBean(PartitionScope scope) {
        return new SimulationMetricPartitionType()
                .scope(scope.toBean())
                .selectionSize(selectionSize)
                .selectionTotalValue(selectionTotalValue)
                .domainSize(domainSize)
                .domainTotalValue(domainTotalValue)
                .selectionMinValue(selectionMinValue)
                .selectionMaxValue(selectionMaxValue)
                .domainMinValue(domainMinValue)
                .domainMaxValue(domainMaxValue);
    }
}
