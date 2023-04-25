/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.path.PathSet;

/**
 * Internal (parsed) representation of a collection of selector-qualified {@link GetOperationOptions}.
 * Used to avoid passing along a collection of options along with root options. (Or re-analyzing the options each time.)
 *
 * In the future we could add parsing to a path-keyed map.
 *
 * Also, we could consider passing this object via various APIs, instead of today's
 * `Collection<SelectorOptions<GetOperationOptions>>`.
 */
@Experimental
public class ParsedGetOperationOptions {

    @NotNull private final Collection<SelectorOptions<GetOperationOptions>> originalCollection;
    @NotNull private final Lazy<GetOperationOptions> rootOptions = Lazy.from(rootOptionsSupplier());

    private static final ParsedGetOperationOptions EMPTY = new ParsedGetOperationOptions();

    private ParsedGetOperationOptions() {
        this.originalCollection = List.of();
        this.rootOptions.get(); // to resolve it
    }

    private ParsedGetOperationOptions(@Nullable Collection<SelectorOptions<GetOperationOptions>> originalCollection) {
        this.originalCollection = emptyIfNull(originalCollection);
    }

    private ParsedGetOperationOptions(@Nullable GetOperationOptions rootOptions) {
        originalCollection = rootOptions != null ?
                SelectorOptions.createCollection(rootOptions) :
                List.of();
    }

    public static @NotNull ParsedGetOperationOptions empty() {
        return EMPTY;
    }

    private @NotNull Lazy.Supplier<GetOperationOptions> rootOptionsSupplier() {
        return () -> SelectorOptions.findRootOptions(originalCollection);
    }

    public static ParsedGetOperationOptions of(
            @Nullable Collection<SelectorOptions<GetOperationOptions>> originalCollection) {
        return new ParsedGetOperationOptions(originalCollection);
    }

    public static ParsedGetOperationOptions of(@Nullable GetOperationOptions rootOptions) {
        return new ParsedGetOperationOptions(rootOptions);
    }

    public @NotNull Collection<SelectorOptions<GetOperationOptions>> getCollection() {
        return originalCollection;
    }

    public @Nullable GetOperationOptions getRootOptions() {
        return rootOptions.get();
    }

    public boolean isEmpty() {
        return originalCollection.isEmpty();
    }

    public Map<DefinitionProcessingOption, PathSet> getDefinitionProcessingMap() {
        return SelectorOptions.extractOptionValues(originalCollection, (o) -> o.getDefinitionProcessing());
    }

    public @NotNull DefinitionUpdateOption getDefinitionUpdate() {
        return Objects.requireNonNullElse(
                GetOperationOptions.getDefinitionUpdate(getRootOptions()),
                DefinitionUpdateOption.NONE); // temporary default (for experiments)
    }
}
