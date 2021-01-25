/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import java.io.Serializable;

import com.evolveum.midpoint.schema.AbstractOptions;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * Options for {@link RepositoryService#modifyObject} operation.
 */
public class RepoModifyOptions extends AbstractOptions
        implements Serializable, ShortDumpable, Cloneable {

    /**
     * Forces "reindex" during modify operation - that is refresh of all column values.
     * Reindex is typically used with empty list of changes.
     * If used with some modifications, reindex is executed after applying the modifications.
     */
    private boolean forceReindex;

    /**
     * Whether to allow inserting extension values without fetching them first.
     * This will spare some SELECT's done by Hibernate. The only risk is to get constraint violation,
     * either because we are adding duplicate values for index-only items, or because we are adding
     * duplicate values for indexed items that were (for strange reason) not filtered out by delta narrowing.
     * The resolution is simply to retry operation with this value set to false.
     * <p>
     * Value of null means it is up to repository service to decide.
     * The repository service can override any value e.g. if constraint violation occurs or if this feature is explicitly disabled.
     */
    private Boolean useNoFetchExtensionValuesInsertion;

    /**
     * Whether to allow deleting extension values without fetching all existing values first.
     * When true, values are deleted "manually" using HQL, one by one. When using false, the deletion is
     * done by Hibernate: fetching all values first, and then issuing batched DELETE against those that need it.
     * <p>
     * The "no fetch" approach can be applied any time (although currently supported only for ROExtString items), but in
     * some scenarios it could be slower than the regular approach: Namely, if there are many values to delete, but
     * not too many values overall. The overhead of repeated deletion can overweight single SELECT + batched deletion.
     * <p>
     * Value of null means it is up to repository service to decide.
     * The repository service can override any value e.g. if this feature is explicitly disabled.
     * <p>
     * Note although these two flags are named similarly their meaning/effect is not that similar:
     * <ol>
     * <li>If useNoFetchExtensionValuesInsertion is false, there is a SINGLE SELECT FOR EACH VALUE being inserted.</li>
     * <li>If useNoFetchExtensionValuesDeletion is false, there is a SINGLE (COMMON) SELECT FOR ALL EXTENSION VALUES of given
     * type, basically falling back to the original Hibernate-driven behavior.</li>
     * </ol>
     * The effect of useNoFetchExtensionValuesInsertion=false may change in the future. (But most probably it will not.)
     */
    private Boolean useNoFetchExtensionValuesDeletion;

    @SuppressWarnings("WeakerAccess")
    public boolean isForceReindex() {
        return forceReindex;
    }

    @SuppressWarnings("WeakerAccess")
    public void setForceReindex(boolean forceReindex) {
        this.forceReindex = forceReindex;
    }

    public static boolean isForceReindex(RepoModifyOptions options) {
        return options != null && options.isForceReindex();
    }

    public static RepoModifyOptions createForceReindex() {
        RepoModifyOptions opts = new RepoModifyOptions();
        opts.setForceReindex(true);
        return opts;
    }

    public Boolean getUseNoFetchExtensionValuesInsertion() {
        return useNoFetchExtensionValuesInsertion;
    }

    public void setUseNoFetchExtensionValuesInsertion(Boolean useNoFetchExtensionValuesInsertion) {
        this.useNoFetchExtensionValuesInsertion = useNoFetchExtensionValuesInsertion;
    }

    public static Boolean getUseNoFetchExtensionValuesInsertion(RepoModifyOptions options) {
        return options != null ? options.getUseNoFetchExtensionValuesInsertion() : null;
    }

    @SuppressWarnings("unused")
    public static RepoModifyOptions createUseNoFetchExtensionValuesInsertion() {
        RepoModifyOptions opts = new RepoModifyOptions();
        opts.setUseNoFetchExtensionValuesInsertion(true);
        return opts;
    }

    public Boolean getUseNoFetchExtensionValuesDeletion() {
        return useNoFetchExtensionValuesDeletion;
    }

    public void setUseNoFetchExtensionValuesDeletion(Boolean useNoFetchExtensionValuesDeletion) {
        this.useNoFetchExtensionValuesDeletion = useNoFetchExtensionValuesDeletion;
    }

    public static Boolean getUseNoFetchExtensionValuesDeletion(RepoModifyOptions options) {
        return options != null ? options.getUseNoFetchExtensionValuesDeletion() : null;
    }

    @SuppressWarnings("unused")
    public static RepoModifyOptions createUseNoFetchExtensionValuesDeletion() {
        RepoModifyOptions opts = new RepoModifyOptions();
        opts.setUseNoFetchExtensionValuesDeletion(true);
        return opts;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RepoModifyOptions(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        appendFlag(sb, "forceReindex", forceReindex);
        appendFlag(sb, "useNoFetchExtensionValuesInsertion", useNoFetchExtensionValuesInsertion);
        appendFlag(sb, "useNoFetchExtensionValuesDeletion", useNoFetchExtensionValuesDeletion);
        removeLastComma(sb);
    }

    public RepoModifyOptions clone() {
        try {
            return (RepoModifyOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
