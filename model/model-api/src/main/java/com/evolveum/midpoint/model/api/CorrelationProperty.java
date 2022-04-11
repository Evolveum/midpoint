/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.PRIMARY_CORRELATION_ITEM_TARGET;

/**
 * Contains information about a correlation property that is to be (e.g.) displayed in the correlation case view.
 *
 * TEMPORARY
 */
@Experimental
public class CorrelationProperty implements Serializable, DebugDumpable {

    public static final String F_DISPLAY_NAME = "displayName";

    /** The "technical" name. */
    @NotNull private final String name;

    @NotNull private final Collection<?> sourceRealValues;
    @NotNull private final Map<String, ItemRoute> targetRouteMap;

    @Nullable private final ItemDefinition<?> definition;

    private CorrelationProperty(
            @NotNull String name,
            @NotNull Collection<?> sourceRealValues,
            @NotNull Map<String, ItemRoute> targetRouteMap,
            @Nullable ItemDefinition<?> definition) {
        this.name = name;
        this.sourceRealValues = sourceRealValues;
        this.targetRouteMap = targetRouteMap;
        this.definition = definition;
    }

    public static CorrelationProperty create(
            @NotNull String name,
            @NotNull Collection<?> sourceRealValues,
            @NotNull Map<String, ItemRoute> targetRouteMap,
            @Nullable ItemDefinition<?> definition) {
        return new CorrelationProperty(name, sourceRealValues, targetRouteMap, definition);
    }

    public static CorrelationProperty createSimple(
            @NotNull Collection<?> sourceRealValues,
            @NotNull ItemPath path,
            @Nullable PrismPropertyDefinition<?> definition) {
        ItemName lastName =
                MiscUtil.requireNonNull(path.lastName(), () -> new IllegalArgumentException("Path has no last name: " + path));
        return new CorrelationProperty(
                lastName.getLocalPart(),
                sourceRealValues,
                Map.of(PRIMARY_CORRELATION_ITEM_TARGET, ItemRoute.fromPath(path)),
                definition);
    }

    public @NotNull Set<String> getSourceRealStringValues() {
        return sourceRealValues.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toSet());
    }

    public @NotNull ItemRoute getPrimaryTargetRoute() {
        return MiscUtil.requireNonNull(
                targetRouteMap.get(ModelPublicConstants.PRIMARY_CORRELATION_ITEM_TARGET),
                () -> new IllegalStateException("No primary target defined"));
    }

    public @NotNull List<ItemRoute> getSecondaryTargetRoutes() {
        return targetRouteMap.entrySet().stream()
                .filter(e -> !ModelPublicConstants.PRIMARY_CORRELATION_ITEM_TARGET.equals(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public @Nullable ItemDefinition<?> getDefinition() {
        return definition;
    }

    public @NotNull String getDisplayName() {
        if (definition != null) {
            if (definition.getDisplayName() != null) {
                return definition.getDisplayName();
            } else {
                return definition.getItemName().getLocalPart();
            }
        } else {
            return name;
        }
    }

    public @NotNull String getName() {
        return name;
    }

    /** Merges this definition with new data. */
    public CorrelationProperty merge(
            @NotNull Collection<?> newRealValues,
            @Nullable PrismPropertyDefinition<?> newDefinition) {
        Collection<Object> mergedValue = new ArrayList<>(sourceRealValues);
        mergedValue.addAll(newRealValues);
        return new CorrelationProperty(
                name,
                mergedValue,
                targetRouteMap,
                definition != null ? definition : newDefinition);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", name, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "sourceRealValues", sourceRealValues, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "targetRouteMap", targetRouteMap, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "definition", String.valueOf(definition), indent + 1);
        return sb.toString();
    }
}
