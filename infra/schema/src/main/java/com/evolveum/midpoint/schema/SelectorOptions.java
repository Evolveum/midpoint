/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.PathSet;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathComparatorUtil;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public class SelectorOptions<T> implements Serializable, DebugDumpable, ShortDumpable, Cloneable {
    private static final long serialVersionUID = 1L;

    private final ObjectSelector selector;
    private final T options;

    //region Construction
    public SelectorOptions(ObjectSelector selector, T options) {
        this.selector = selector;
        this.options = options;
    }

    public SelectorOptions(T options) {
        this.selector = null;
        this.options = options;
    }

    public SelectorOptions(@NotNull SelectorOptions<T> prototype) {
        this.selector = prototype.selector != null ? new ObjectSelector(prototype.selector) : null;
        this.options = CloneUtil.clone(prototype.options);
    }

    public static <T> SelectorOptions<T> create(UniformItemPath path, T options) {
        return new SelectorOptions<>(new ObjectSelector(path), options);
    }

    public static <T> SelectorOptions<T> create(T options) {
        return new SelectorOptions<>(options);
    }

    public static <T> Collection<SelectorOptions<T>> createCollection(UniformItemPath path, T options) {
        Collection<SelectorOptions<T>> optionsCollection = new ArrayList<>(1);
        optionsCollection.add(create(path, options));
        return optionsCollection;
    }

    public static <T> Collection<SelectorOptions<T>> createCollection(T options) {
        Collection<SelectorOptions<T>> optionsCollection = new ArrayList<>(1);
        optionsCollection.add(new SelectorOptions<>(options));
        return optionsCollection;
    }

    public static <T> Collection<SelectorOptions<T>> createCollection(T options, UniformItemPath... paths) {
        Collection<SelectorOptions<T>> optionsCollection = new ArrayList<>(paths.length);
        for (UniformItemPath path : paths) {
            optionsCollection.add(create(path, options));
        }
        return optionsCollection;
    }
    //endregion

    //region Simple getters
    public ObjectSelector getSelector() {
        return selector;
    }

    public T getOptions() {
        return options;
    }
    //endregion

    //region Methods for accessing content (findRoot, hasToLoadPath, ...)
    @Nullable
    private UniformItemPath getUniformItemPathOrNull() {
        return selector != null ? selector.getPath() : null;
    }

    @NotNull
    public UniformItemPath getItemPath(UniformItemPath emptyPath) {
        return ObjectUtils.defaultIfNull(getUniformItemPathOrNull(), emptyPath);
    }

    public @NotNull ItemPath getItemPath() {
        if (selector != null) {
            return Objects.requireNonNullElse(selector.getPath(), ItemPath.EMPTY_PATH);
        } else {
            return ItemPath.EMPTY_PATH;
        }
    }

    /**
     * Returns options that apply to the "root" object. I.e. options that have null selector, null path, empty path, ...
     * Must return 'live object' that could be modified.
     */
    public static <T> T findRootOptions(Collection<SelectorOptions<T>> options) {
        if (options == null) {
            return null;
        }
        for (SelectorOptions<T> option : options) {
            if (option.isRoot()) {
                return option.getOptions();
            }
        }
        return null;
    }

    // newValueSupplier should be not null as well (unless the caller is 100% sure it won't be needed)
    public static <T> @NotNull Collection<SelectorOptions<T>> updateRootOptions(
            @Nullable Collection<SelectorOptions<T>> options,
            @NotNull Consumer<T> updater, Supplier<T> newValueSupplier) {
        if (options == null) {
            options = new ArrayList<>();
        } else if (!(options instanceof ArrayList)) {
            options = new ArrayList<>(options); // ugly hack, but the whole concept of updating the options is ugly
        }
        T rootOptions = findRootOptions(options);
        if (rootOptions == null) {
            rootOptions = newValueSupplier.get();
            options.add(new SelectorOptions<>(rootOptions));
        }
        updater.accept(rootOptions);
        return options;
    }

    /**
     * As {@link #updateRootOptions(Collection, Consumer, Supplier)} but does not modify the original options
     * (and checks whether the update is needed at all).
     */
    @Experimental
    static <T> @Nullable Collection<SelectorOptions<T>> updateRootOptionsSafe(
            @Nullable Collection<SelectorOptions<T>> originalOptions,
            @NotNull Predicate<T> predicate,
            @NotNull Consumer<T> updater,
            Supplier<T> newValueSupplier) {
        T originalRootOptions = findRootOptions(originalOptions);
        if (!predicate.test(originalRootOptions)) {
            return originalOptions;
        }

        Collection<SelectorOptions<T>> newOptions = new ArrayList<>();

        T newRootOptions;
        if (originalRootOptions == null) {
            newRootOptions = newValueSupplier.get();
        } else {
            newRootOptions = CloneUtil.clone(originalRootOptions);
        }
        updater.accept(newRootOptions);
        newOptions.add(new SelectorOptions<>(newRootOptions));

        for (SelectorOptions<T> originalOption : emptyIfNull(originalOptions)) {
            if (!originalOption.isRoot()) {
                newOptions.add(originalOption);
            }
        }

        return newOptions;
    }

    /**
     * Finds all the options for given path. TODO could there be more than one?
     * Returns live objects that could be modified by client.
     */
    @NotNull
    public static <T> Collection<T> findOptionsForPath(Collection<SelectorOptions<T>> options, @NotNull UniformItemPath path) {
        Collection<T> rv = new ArrayList<>();
        for (SelectorOptions<T> option : CollectionUtils.emptyIfNull(options)) {
            if (path.isSuperPathOrEquivalent(option.getUniformItemPathOrNull())) {
                rv.add(option.getOptions());
            }
        }
        return rv;
    }

    public boolean isRoot() {
        UniformItemPath itemPathOrNull = getUniformItemPathOrNull();
        return itemPathOrNull == null || itemPathOrNull.isEmpty();
    }

    private static final Set<Class<?>> OBJECTS_NOT_RETURNED_FULLY_BY_DEFAULT = new HashSet<>(Arrays.asList(
            UserType.class, RoleType.class, OrgType.class, ServiceType.class, AbstractRoleType.class,
            FocusType.class, AssignmentHolderType.class, ObjectType.class,
            TaskType.class, LookupTableType.class, AccessCertificationCampaignType.class,
            ShadowType.class // because of index-only attributes
    ));

    public static boolean isRetrievedFullyByDefault(Class<?> objectType) {
        return !OBJECTS_NOT_RETURNED_FULLY_BY_DEFAULT.contains(objectType);
    }

    /** Very primitive method that checks whether there is an instruction to exclude any item from retrieval. */
    public static boolean excludesSomethingFromRetrieval(@Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        return emptyIfNull(options).stream()
                .anyMatch(
                        option -> option != null
                                && option.getOptions() != null
                                && option.getOptions().getRetrieve() == RetrieveOption.EXCLUDE);
    }

    /**
     * Returns true if the asked path must be included in the object based on the provided get options.
     * See {@link GetOperationOptions#retrieve} javadoc for more details.
     * Retrieve option with null or empty path asks to load everything.
     *
     * This is not used only in repository but also for retrieving information during provisioning.
     * Default value must be explicitly provided as it depends on the context of the question.
     *
     * Simplified example:
     * When we ask for path "x/y", the path itself and anything under it is to be included.
     * User wants "x/y", as well as "x/y/ya", etc.
     * User does not say anything about path "x" (prefix) or "z" (different path altogether).
     * If no option defines what to do with the asked path, defaultValue is returned.
     */
    public static boolean hasToIncludePath(
            @NotNull ItemPath path,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            boolean defaultValue) {
        if (options == null) {
            return defaultValue;
        }

        for (SelectorOptions<GetOperationOptions> option : emptyIfNull(options)) {
            // TODO consider ordering of the options from most specific to least specific
            //  (currently not mandated by GetOperationOptions.retrieve specification).
            RetrieveOption retrievalCommand = option != null
                    && option.getOptions() != null ? option.getOptions().getRetrieve() : null;
            if (retrievalCommand != null) {
                ObjectSelector selector = option.getSelector();
                if (selector == null || selector.getPath() == null || selector.getPath().isSubPathOrEquivalent(path)) {
                    return switch (retrievalCommand) {
                        case EXCLUDE -> false;
                        case DEFAULT -> defaultValue;
                        case INCLUDE -> true;
                    };
                }
            }
        }
        return defaultValue;
    }

    /**
     * Returns true if the asked path must be considered for fetching based on the provided get options.
     * This is different from {@link #hasToIncludePath(ItemPath, Collection, boolean)} as it considers
     * path relation in any direction (subpath or prefix or equal).
     * This is typical for repository usage that asks questions like:
     * "Should I load container x *that I don't fetch by default*?"
     * When the user specifies options with retrieve of the path inside that container, e.g. "x/a"),
     * he does not state that he wants to fetch "x", but the repo can't just ignore processing of "x"
     * and skip it altogether if it is stored separately.
     * At the same time, original semantics of {@link #hasToIncludePath(ItemPath, Collection, boolean)}
     * has to hold as well - if user asks to load "a", repo needs to fetch "a/b" as well (everything under "a").
     *
     * Simplified example:
     * When we ask for path "x/y", the path itself, any subpath and any prefix must be considered for fetching.
     * User wants "x/y", as well as "x/y/ya", etc.
     * User does not say anything about path "x" (prefix) or "z" (different path altogether),
     * but while we can ignore "z", we must consider "x" if the repo does not provide it by default.
     * In that case it needs to fetch it (fully or partially, whatever is practical and/or efficient)
     * to provide the requested "x/y" content.
     */
    public static boolean hasToFetchPathNotRetrievedByDefault(
            @NotNull ItemPath path,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        if (options == null) {
            return false;
        }

        for (SelectorOptions<GetOperationOptions> option : emptyIfNull(options)) {
            RetrieveOption retrievalCommand = option != null
                    && option.getOptions() != null ? option.getOptions().getRetrieve() : null;
            if (retrievalCommand != null) {
                ObjectSelector selector = option.getSelector();
                if (selector == null || selector.getPath() == null
                        // shortcut for "is subpath or prefix or equal"
                        || ItemPathComparatorUtil.compareComplex(selector.getPath(), path) != ItemPath.CompareResult.NO_RELATION) {
                    return switch (retrievalCommand) {
                        case EXCLUDE, DEFAULT -> false;
                        case INCLUDE -> true;
                    };
                }
            }
        }
        return false;
    }

    public static List<SelectorOptions<GetOperationOptions>> filterRetrieveOptions(
            Collection<SelectorOptions<GetOperationOptions>> options) {
        return MiscUtil.streamOf(options)
                .filter(option -> option.getOptions() != null && option.getOptions().getRetrieve() != null)
                .collect(Collectors.toList());
    }

    public static <T> Map<T, PathSet> extractOptionValues(
            Collection<SelectorOptions<GetOperationOptions>> options, Function<GetOperationOptions, T> supplier) {
        Map<T, PathSet> rv = new HashMap<>();
        for (SelectorOptions<GetOperationOptions> selectorOption : CollectionUtils.emptyIfNull(options)) {
            T value = supplier.apply(selectorOption.getOptions());
            if (value != null) {
                rv.computeIfAbsent(value, t -> new PathSet())
                        .add(selectorOption.getItemPath());
            }
        }
        return rv;
    }

    //endregion

    //region hashCode, equals, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        SelectorOptions<?> that = (SelectorOptions<?>) o;
        return Objects.equals(selector, that.selector) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, options);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ObjectOperationOptions(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        return toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (selector == null) {
            sb.append("/");
        } else {
            selector.shortDump(sb);
        }
        sb.append(":");
        if (options == null) {
            sb.append("null");
        } else if (options instanceof ShortDumpable) {
            ((ShortDumpable) options).shortDump(sb);
        } else {
            sb.append(options);
        }
    }
    //endregion

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public SelectorOptions<T> clone() {
        return new SelectorOptions<>(this);
    }
}
