/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.util.List;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPathType;

public class ActivityPath {

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
}
