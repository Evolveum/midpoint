/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.schema.util.task.ActivityPath;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStatisticsType;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ActivityStatisticsUtil {

    public static ActivityStatisticsType findOrCreateMatchingInfo(
            @NotNull ActivityStatisticsType current, ActivityPath activityPath, boolean create) {
        if (activityPath.isEmpty()) {
            return current;
        }

        ActivityStatisticsType childInfo = findOrCreateMatchingInfo(current.getActivity(), activityPath.first(), create);
        return findOrCreateMatchingInfo(
                childInfo,
                activityPath.rest(),
                create);
    }

    private static ActivityStatisticsType findOrCreateMatchingInfo(List<ActivityStatisticsType> infos,
            String identifier, boolean create) {
        return findMatchingInfo(infos, identifier)
                .orElseGet(
                        () -> create ? add(infos, new ActivityStatisticsType().identifier(identifier)) : null);
    }

    private static Optional<ActivityStatisticsType> findMatchingInfo(
            @NotNull List<ActivityStatisticsType> list, String id) {
        return list.stream()
                .filter(item -> Objects.equals(item.getIdentifier(), id))
                .findFirst();
    }

    /** Like {@link List#add(Object)} but returns the value. */
    private static <T> T add(List<T> list, T value) {
        list.add(value);
        return value;
    }
}
