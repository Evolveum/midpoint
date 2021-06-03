/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPathType;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

public class ActivityPath {

    /** Unmodifiable list of activity identifiers. */
    @NotNull private final List<String> identifiers;

    private ActivityPath() {
        this.identifiers = List.of();
    }

    private ActivityPath(@NotNull List<String> identifiers) {
        this.identifiers = List.copyOf(identifiers);
    }

    public static ActivityPath fromBean(ActivityPathType bean) {
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
        stateCheck(!isEmpty(), "Path is empty");
        return identifiers.get(0);
    }

    public ActivityPath rest() {
        stateCheck(!isEmpty(), "Path is empty");
        return ActivityPath.fromList(identifiers.subList(1, identifiers.size()));
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
}
