/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPathType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Object identifying an activity in the (logical) activity tree.
 *
 * For example, if we have a composite activity of two reconciliations (`recon-A` and `recon-B`), there are the following
 * activity paths present:
 *
 * - `reconA/operationCompletion`
 * - `reconA/resourceObjects`
 * - `reconA/remainingShadows`
 * - `reconB/operationCompletion`
 * - `reconB/resourceObjects`
 * - `reconB/remainingShadows`
 *
 * These paths denote the activities _regardless_ of whether they are distributed (into worker tasks), delegated (into subtasks),
 * or both.
 *
 * Empty path denotes the root activity.
 *
 * Externalized form is {@link ActivityPathType}.
 */
public class ActivityPath implements Serializable {

    /** Unmodifiable list of activity identifiers. */
    @NotNull private final List<String> identifiers;

    private ActivityPath() {
        this.identifiers = List.of();
    }

    private ActivityPath(@NotNull List<String> identifiers) {
        this.identifiers = List.copyOf(identifiers);
    }

    public static @NotNull ActivityPath fromBean(@Nullable ActivityPathType bean) {
        if (bean != null) {
            return new ActivityPath(bean.getIdentifier());
        } else {
            return new ActivityPath();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static ActivityPath fromList(List<String> identifiers) {
        return new ActivityPath(identifiers);
    }

    public static ActivityPath empty() {
        return new ActivityPath();
    }

    public static ActivityPath fromId(String... identifiers) {
        return fromList(Arrays.asList(identifiers));
    }

    @NotNull
    public List<String> getIdentifiers() {
        return identifiers;
    }

    public boolean isEmpty() {
        return identifiers.isEmpty();
    }

    @Override
    public String toString() {
        return String.join("/", identifiers);
    }

    public boolean startsWith(ActivityPath otherPath) {
        return MiscUtil.startsWith(identifiers, otherPath.getIdentifiers());
    }

    public int size() {
        return identifiers.size();
    }

    public String first() {
        checkNotEmpty();
        return identifiers.get(0);
    }

    private void checkNotEmpty() {
        stateCheck(!isEmpty(), "Path is empty");
    }

    public ActivityPath rest() {
        checkNotEmpty();
        return ActivityPath.fromList(identifiers.subList(1, identifiers.size()));
    }

    public String last() {
        checkNotEmpty();
        return identifiers.get(identifiers.size() - 1);
    }

    public @NotNull ActivityPath allExceptLast() {
        checkNotEmpty();
        return new ActivityPath(identifiers.subList(0, identifiers.size() - 1));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActivityPath that = (ActivityPath) o;
        return identifiers.equals(that.identifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifiers);
    }

    public ActivityPath append(String identifier) {
        List<String> union = new ArrayList<>(identifiers);
        union.add(identifier);
        return ActivityPath.fromList(union);
    }

    public ActivityPathType toBean() {
        ActivityPathType bean = new ActivityPathType();
        bean.getIdentifier().addAll(identifiers);
        return bean;
    }

    public boolean equalsBean(ActivityPathType bean) {
        return bean != null && identifiers.equals(bean.getIdentifier());
    }

    public String toDebugName() {
        return isEmpty() ? "root" : "'" + this + "'";
    }
}
