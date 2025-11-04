/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util.task.work;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.schema.util.task.ActivityPath;

public class ActivityDefinitionUtil {

    public static @NotNull ActivityDistributionDefinitionType findOrCreateDistribution(ActivityDefinitionType activity) {
        if (activity.getDistribution() != null) {
            return activity.getDistribution();
        } else {
            return activity.beginDistribution();
        }
    }

    public static void visitActivityDefinitions(
            @NotNull TaskType task, @NotNull BiFunction<ActivityDefinitionType, ActivityPath, Boolean> visitor) {

        ActivityDefinitionType def = task.getActivity();
        if (def == null) {
            return;
        }

        visitActivityDefinitions(def, ActivityPath.empty(), visitor);
    }

    public static void visitActivityDefinitions(
            @NotNull ActivityDefinitionType def,
            @NotNull ActivityPath path,
            @NotNull BiFunction<ActivityDefinitionType, ActivityPath, Boolean> visitor) {

        visitor.apply(def, path);

        ActivityCompositionType composition = def.getComposition();
        if (composition != null) {
            for (ActivityDefinitionType child : composition.getActivity()) {
                visitActivityDefinitions(child, path.append(child.getIdentifier()), visitor);
            }
        }

        // noinspection unchecked
        Collection<Item<?, ?>> items = def.asPrismContainerValue().getItems();
        items.stream()
                .filter(i -> i.getDefinition().getTypeClass().isAssignableFrom(ActivityDefinitionType.class))
                .map(i -> i.getRealValues())
                .flatMap(Collection::stream)
                .map(d -> (ActivityDefinitionType) d)
                .forEach(d -> visitActivityDefinitions(d, path.append(d.getIdentifier()), visitor));
    }

    public static ActivityDefinitionType findActivityDefinition(ActivityDefinitionType def, ActivityPath path) {
        if (path.isEmpty()) {
            return def;
        }

        if (def == null) {
            return null;
        }

        String first = path.first();
        ActivityPath remainder = path.rest();

        ActivityCompositionType composition = def.getComposition();
        if (composition != null) {
            ActivityDefinitionType child = composition.getActivity().stream()
                    .filter(a -> Objects.equals(first, a.getIdentifier()))
                    .findFirst()
                    .orElse(null);

            if (child != null) {
                return findActivityDefinition(child, remainder);
            }
        }

        if (!remainder.isEmpty()) {
            return null; // no more to search
        }

        // noinspection unchecked
        Collection<Item<?, ?>> items = def.asPrismContainerValue().getItems();
        return items.stream()
                .filter(i -> i.getDefinition().getTypeClass().isAssignableFrom(ActivityDefinitionType.class))
                .map(i -> i.getRealValues())
                .flatMap(Collection::stream)
                .map(d -> (ActivityDefinitionType) d)
                .filter(d -> Objects.equals(first, d.getIdentifier()))
                .findFirst()
                .orElse(null);
    }
}
