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

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

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

    /** Path within the focus object. */
    @NotNull private final ItemPath itemPath;

    /** Definition in the focus object. */
    @Nullable private final ItemDefinition<?> definition;

    /** Value(s) in the object to be correlated. */
    @NotNull private final Collection<?> sourceRealValues;

    private CorrelationProperty(
            @NotNull String name,
            @NotNull ItemPath itemPath,
            @Nullable ItemDefinition<?> definition,
            @NotNull Collection<?> sourceRealValues) {
        this.name = name;
        this.itemPath = itemPath;
        this.definition = definition;
        this.sourceRealValues = sourceRealValues;
    }

    public static CorrelationProperty create(
            @NotNull String name,
            @NotNull ItemPath itemPath,
            @NotNull Collection<?> sourceRealValues,
            @Nullable ItemDefinition<?> definition) {
        return new CorrelationProperty(name, itemPath, definition, sourceRealValues);
    }

    public static CorrelationProperty createSimple(
            @NotNull Collection<?> sourceRealValues,
            @NotNull ItemPath path,
            @Nullable PrismPropertyDefinition<?> definition) {
        ItemName lastName =
                MiscUtil.requireNonNull(path.lastName(), () -> new IllegalArgumentException("Path has no last name: " + path));
        return new CorrelationProperty(lastName.getLocalPart(), path, definition, sourceRealValues);
    }

    public @NotNull Set<String> getSourceRealStringValues() {
        return sourceRealValues.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toSet());
    }

    public @NotNull ItemPath getPrimaryTargetPath() {
        return itemPath;
    }

    public @NotNull ItemPath getSecondaryTargetPath() {
        return SchemaConstants.PATH_IDENTITY.append(FocusIdentityType.F_DATA, itemPath);
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
                itemPath,
                definition != null ? definition : newDefinition,
                mergedValue);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", name, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "itemPath", String.valueOf(itemPath), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "definition", String.valueOf(definition), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "sourceRealValues", sourceRealValues, indent + 1);
        return sb.toString();
    }
}
