/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.LEFT;
import static com.evolveum.midpoint.schema.statistics.Formatting.Alignment.RIGHT;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ComponentsPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleComponentPerformanceInformationType;

import org.jetbrains.annotations.NotNull;

/**
 * Prints the information about components "performance", i.e., time spent in them plus the number of invocations (which may
 * be misleading, but maybe useful in specific cases).
 */
public class ComponentsPerformanceInformationPrinter extends AbstractStatisticsPrinter<ComponentsPerformanceInformationType> {

    private List<SingleComponentPerformanceInformationType> components;

    public ComponentsPerformanceInformationPrinter(
            @NotNull ComponentsPerformanceInformationType information, Options options, Integer iterations) {
        super(information, options, iterations, null);
    }

    @Override
    public void prepare() {
        components = getSortedComponents();
        createData(components);
        createFormatting();
    }

    @NotNull
    private List<SingleComponentPerformanceInformationType> getSortedComponents() {
        return information.getComponent().stream()
                .sorted(getComparator())
                .collect(Collectors.toList());
    }

    private Comparator<SingleComponentPerformanceInformationType> getComparator() {
        return switch (options.sortBy) {
            case COUNT -> Comparator.comparing(SingleComponentPerformanceInformationType::getInvocationCount).reversed();
            case TIME, OWN_TIME -> Comparator.comparing(SingleComponentPerformanceInformationType::getTotalTime).reversed();
            default -> Comparator.comparing(SingleComponentPerformanceInformationType::getName);
        };
    }

    private void createData(List<SingleComponentPerformanceInformationType> components) {
        initData();
        for (var component : components) {
            createRecord(component);
        }
    }

    private void createRecord(SingleComponentPerformanceInformationType component) {
        String name = component.getName();
        int count = or0(component.getInvocationCount());

        Data.Record record = data.createRecord();
        record.add(name);
        record.add(count);
        if (iterations != null) {
            record.add(avg(count, iterations));
        }
        float totalTime = zeroIfNull(component.getTotalTime()) / 1000.0f;
        record.add(totalTime);
        if (iterations != null) {
            record.add(avg(totalTime, iterations));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void createFormatting() {
        initFormatting();
        addColumn("Component", LEFT, formatString());
        addColumn("Count", RIGHT, formatInt());
        if (iterations != null) {
            addColumn("Count/iter", RIGHT, formatFloat1());
        }
        addColumn("Total time (ms)", RIGHT, formatFloat1());
        if (iterations != null) {
            addColumn("Time/iter", RIGHT, formatFloat1());
        }
    }
}
