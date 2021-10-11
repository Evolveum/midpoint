/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public class SelectorOptions<T> implements Serializable, DebugDumpable, ShortDumpable {
    private static final long serialVersionUID = 1L;

    private ObjectSelector selector;
    private T options;

    //region Construction
    public SelectorOptions(ObjectSelector selector, T options) {
        super();
        this.selector = selector;
        this.options = options;
    }

    public SelectorOptions(T options) {
        super();
        this.selector = null;
        this.options = options;
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
    private UniformItemPath getItemPathOrNull() {
        return selector != null && selector.getPath() != null ? selector.getPath() : null;
    }

    @NotNull
    public UniformItemPath getItemPath(UniformItemPath emptyPath) {
        return ObjectUtils.defaultIfNull(getItemPathOrNull(), emptyPath);
    }

    /**
     * Returns options that apply to the "root" object. I.e. options that have null selector, null path, empty path, ...
     * Must return 'live object' that could be modified.
     */
    public static <T> T findRootOptions(Collection<SelectorOptions<T>> options) {
        if (options == null) {
            return null;
        }
        for (SelectorOptions<T> oooption : options) {
            if (oooption.isRoot()) {
                return oooption.getOptions();
            }
        }
        return null;
    }

    public static <T> Collection<SelectorOptions<T>> updateRootOptions(Collection<SelectorOptions<T>> options, Consumer<T> updater, Supplier<T> newValueSupplier) {
        if (options == null) {
            options = new ArrayList<>();
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
     * Finds all the options for given path. TODO could there be more than one?
     * Returns live objects that could be modified by client.
     */
    @NotNull
    public static <T> Collection<T> findOptionsForPath(Collection<SelectorOptions<T>> options, @NotNull UniformItemPath path) {
        Collection<T> rv = new ArrayList<>();
        for (SelectorOptions<T> oooption : CollectionUtils.emptyIfNull(options)) {
            if (path.equivalent(oooption.getItemPathOrNull())) {
                rv.add(oooption.getOptions());
            }
        }
        return rv;
    }

    public boolean isRoot() {
        UniformItemPath itemPathOrNull = getItemPathOrNull();
        return itemPathOrNull == null || itemPathOrNull.isEmpty();
    }

    // TODO find a better way to specify this
    private static final Set<ItemPath> PATHS_NOT_RETURNED_BY_DEFAULT = new HashSet<>(Arrays.asList(
            ItemPath.create(UserType.F_JPEG_PHOTO),
            ItemPath.create(TaskType.F_RESULT),
            ItemPath.create(TaskType.F_SUBTASK_REF),
            ItemPath.create(TaskType.F_NODE_AS_OBSERVED),
            ItemPath.create(TaskType.F_NEXT_RUN_START_TIMESTAMP),
            ItemPath.create(TaskType.F_NEXT_RETRY_TIMESTAMP),
            ItemPath.create(LookupTableType.F_ROW),
            ItemPath.create(AccessCertificationCampaignType.F_CASE)));

    private static final Set<Class<?>> OBJECTS_NOT_RETURNED_FULLY_BY_DEFAULT = new HashSet<>(Arrays.asList(
            UserType.class, RoleType.class, OrgType.class, ServiceType.class, AbstractRoleType.class,
            FocusType.class, AssignmentHolderType.class, ObjectType.class,
            TaskType.class, LookupTableType.class, AccessCertificationCampaignType.class,
            ShadowType.class            // because of index-only attributes
    ));

    public static boolean isRetrievedFullyByDefault(Class<?> objectType) {
        return !OBJECTS_NOT_RETURNED_FULLY_BY_DEFAULT.contains(objectType);
    }

    public static boolean hasToLoadPath(@NotNull ItemPath path, Collection<SelectorOptions<GetOperationOptions>> options) {
        return hasToLoadPath(path, options, !ItemPathCollectionsUtil.containsEquivalent(PATHS_NOT_RETURNED_BY_DEFAULT, path));
    }

    public static boolean hasToLoadPath(@NotNull ItemPath path, Collection<SelectorOptions<GetOperationOptions>> options,
            boolean defaultValue) {
        for (SelectorOptions<GetOperationOptions> option : emptyIfNull(options)) {
            // TODO consider ordering of the options from most specific to least specific
            RetrieveOption retrievalCommand = option != null && option.getOptions() != null ? option.getOptions().getRetrieve() : null;
            if (retrievalCommand != null) {
                ObjectSelector selector = option.getSelector();
                if (selector == null || selector.getPath() == null || selector.getPath().isSubPathOrEquivalent(path)) {
                    switch (retrievalCommand) {
                        case EXCLUDE:
                            return false;
                        case DEFAULT:
                            return defaultValue;
                        case INCLUDE:
                            return true;
                        default:
                            throw new AssertionError("Wrong retrieve option: " + retrievalCommand);
                    }
                }
            }
        }
        return defaultValue;
    }

    public static List<SelectorOptions<GetOperationOptions>> filterRetrieveOptions(
            Collection<SelectorOptions<GetOperationOptions>> options) {
        return MiscUtil.streamOf(options)
                .filter(option -> option.getOptions() != null && option.getOptions().getRetrieve() != null)
                .collect(Collectors.toList());
    }

    public static <T> Map<T, Collection<UniformItemPath>> extractOptionValues(Collection<SelectorOptions<GetOperationOptions>> options,
            Function<GetOperationOptions, T> supplier, PrismContext prismContext) {
        Map<T, Collection<UniformItemPath>> rv = new HashMap<>();
        final UniformItemPath emptyPath = prismContext.emptyPath();
        for (SelectorOptions<GetOperationOptions> selectorOption : CollectionUtils.emptyIfNull(options)) {
            T value = supplier.apply(selectorOption.getOptions());
            if (value != null) {
                Collection<UniformItemPath> itemPaths = rv.computeIfAbsent(value, t -> new HashSet<>());
                itemPaths.add(selectorOption.getItemPath(emptyPath));
            }
        }
        return rv;
    }

    //endregion

    //region hashCode, equals, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
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
}
