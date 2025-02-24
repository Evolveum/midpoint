/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ComponentsPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationsPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleComponentPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleOperationPerformanceInformationType;

/** Computes {@link ComponentsPerformanceInformationType} from {@link OperationsPerformanceInformationType}. */
public class ComponentsPerformanceComputer {

    @NotNull private final List<ComponentDescription> componentDescriptions;

    public ComponentsPerformanceComputer(@NotNull List<ComponentDescription> componentDescriptions) {
        this.componentDescriptions = componentDescriptions;
    }

    public ComponentsPerformanceInformationType compute(@NotNull OperationsPerformanceInformationType operationsInfo) {
        ComponentsPerformanceInformationType componentsInfo = new ComponentsPerformanceInformationType();
        operationsInfo.getOperation().forEach(operationInfo -> {
            var componentName = getComponentName(operationInfo.getName());
            addTo(componentsInfo, componentName, operationInfo);
        });
        return componentsInfo;
    }

    private @NotNull String getComponentName(String operationName) {
        return componentDescriptions.stream()
                .filter(description -> description.matches(operationName))
                .findFirst()
                .map(ComponentDescription::getName)
                .orElseGet(() -> "Other: " + operationName);
    }

    private static void addTo(
            @NotNull ComponentsPerformanceInformationType componentsInfo,
            @NotNull String componentName,
            @NotNull SingleOperationPerformanceInformationType operationInfo) {
        SingleComponentPerformanceInformationType matchingComponentInfo = null;
        for (var componentInfo : componentsInfo.getComponent()) {
            if (Objects.equals(componentInfo.getName(), componentName)) {
                matchingComponentInfo = componentInfo;
                break;
            }
        }
        if (matchingComponentInfo == null) {
            matchingComponentInfo =
                    new SingleComponentPerformanceInformationType()
                            .name(componentName);
            componentsInfo.getComponent().add(matchingComponentInfo);
        }
        matchingComponentInfo.setInvocationCount(
                or0(matchingComponentInfo.getInvocationCount()) + or0(operationInfo.getInvocationCount()));
        matchingComponentInfo.setTotalTime(
                or0(matchingComponentInfo.getTotalTime()) + or0(operationInfo.getOwnTime()));
    }

    /** Describes a component: its name and the operation(s) it provides. Used for grouping operations into components. */
    public interface ComponentDescription {

        /** Provides the component name. This name is to be put into the components performance information. */
        @NotNull String getName();

        /** Returns `true` if the given operation belongs to this component. */
        boolean matches(@NotNull String operationName);
    }

    public static class RegexBasedComponentDescription implements ComponentDescription {
        @NotNull private final String name;
        @NotNull private final Collection<Pattern> included;
        @NotNull private final Collection<Pattern> excluded;

        RegexBasedComponentDescription(
                @NotNull String name,
                @NotNull Collection<String> included,
                @NotNull Collection<String> excluded) {
            this.name = name;
            this.included = compile(included);
            this.excluded = compile(excluded);
        }

        private static @NotNull Collection<Pattern> compile(@NotNull Collection<String> patterns) {
            return patterns.stream()
                    .map(Pattern::compile)
                    .toList();
        }

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public boolean matches(@NotNull String operationName) {
            return matchesAnyIn(operationName, included) && !matchesAnyIn(operationName, excluded);
        }

        private boolean matchesAnyIn(
                @NotNull String operationName,
                @NotNull Collection<Pattern> patterns) {
            return patterns.stream()
                    .anyMatch(pattern -> pattern.matcher(operationName).matches());
        }
    }
}
