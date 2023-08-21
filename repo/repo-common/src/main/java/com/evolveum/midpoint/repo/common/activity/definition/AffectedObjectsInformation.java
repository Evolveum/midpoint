/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Information about objects affected by an activity plus some extra data (type of activity, execution mode, and so on).
 *
 * Different for simple (single object set) and complex (potentially multiple object sets) situations;
 * currently this corresponds to custom-composite vs all other activities; but this may change in the future.
 */
public abstract class AffectedObjectsInformation {

    public static AffectedObjectsInformation simple(
            @NotNull QName activityTypeName,
            @NotNull ObjectSet objectSet,
            @NotNull ExecutionModeType executionMode,
            @Nullable PredefinedConfigurationType predefinedConfiguration) {
        return new Simple(activityTypeName, objectSet, executionMode, predefinedConfiguration);
    }

    public static AffectedObjectsInformation complex(
            @NotNull Collection<AffectedObjectsInformation> children) {
        return new Complex(children);
    }

    /**
     * Returns the "bean" representation. It should be freely usable, e.g. parent-less and modifiable by the caller.
     */
    public @NotNull TaskAffectedObjectsType toBean() {
        TaskAffectedObjectsType bean = new TaskAffectedObjectsType();

        List<Simple> simpleChildren = new ArrayList<>();
        collectSimpleChildren(simpleChildren);
        for (Simple simpleChild : simpleChildren) {
            var activityBean = simpleChild.toActivityBean();
            if (activityBean != null && !bean.getActivity().contains(activityBean)) {
                bean.getActivity().add(activityBean);
            }
        }

        return bean;
    }

    abstract void collectSimpleChildren(@NotNull List<Simple> targetList);

    /** For a single "object set"; currently this corresponds to any activity other than custom-composite one. */
    static class Simple extends AffectedObjectsInformation {
        @NotNull private final QName activityTypeName;
        @NotNull private final ObjectSet objectSet;
        private final ExecutionModeType executionMode;
        private final PredefinedConfigurationType predefinedConfiguration;

        Simple(
                @NotNull QName activityTypeName,
                @NotNull ObjectSet objectSet,
                @NotNull ExecutionModeType executionMode,
                @Nullable PredefinedConfigurationType predefinedConfiguration) {
            this.activityTypeName = activityTypeName;
            this.objectSet = objectSet;
            this.executionMode = executionMode;
            this.predefinedConfiguration = predefinedConfiguration;
        }

        @Override
        public void collectSimpleChildren(@NotNull List<Simple> targetList) {
            targetList.add(this);
        }

        /** Returns `null` if not applicable. */
        @Nullable ActivityAffectedObjectsType toActivityBean() {
            ActivityAffectedObjectsType bean = new ActivityAffectedObjectsType()
                    .activityType(activityTypeName)
                    .executionMode(executionMode)
                    .predefinedConfigurationToUse(predefinedConfiguration);
            return objectSet.setIntoBean(bean);
        }
    }

    static class Complex extends AffectedObjectsInformation {

        @NotNull private final Collection<AffectedObjectsInformation> children;

        Complex(@NotNull Collection<AffectedObjectsInformation> children) {
            this.children = children;
        }

        @Override
        public void collectSimpleChildren(@NotNull List<Simple> targetList) {
            for (AffectedObjectsInformation child : children) {
                child.collectSimpleChildren(targetList);
            }
        }
    }

    public abstract static class ObjectSet {

        public static ObjectSet notSupported() {
            return new NotSupportedObjectSet();
        }

        public static ObjectSet repository(@NotNull BasicObjectSetType objects) {
            return new RepositoryObjectSet(objects);
        }

        public static ObjectSet resource(@NotNull BasicResourceObjectSetType objects) {
            return new ResourceObjectSet(objects);
        }

        @Nullable abstract ActivityAffectedObjectsType setIntoBean(ActivityAffectedObjectsType bean);
    }

    static class RepositoryObjectSet extends ObjectSet {
        @NotNull private final BasicObjectSetType objects;

        RepositoryObjectSet(@NotNull BasicObjectSetType objects) {
            this.objects = objects;
        }

        @Override
        @Nullable ActivityAffectedObjectsType setIntoBean(ActivityAffectedObjectsType bean) {
            return bean.objects(objects);
        }
    }

    static class ResourceObjectSet extends ObjectSet {
        @NotNull private final BasicResourceObjectSetType resourceObjects;

        ResourceObjectSet(@NotNull BasicResourceObjectSetType resourceObjects) {
            this.resourceObjects = resourceObjects;
        }

        @Override
        @Nullable ActivityAffectedObjectsType setIntoBean(ActivityAffectedObjectsType bean) {
            return bean.resourceObjects(resourceObjects);
        }
    }

    private static class NotSupportedObjectSet extends ObjectSet {

        @Override
        @Nullable ActivityAffectedObjectsType setIntoBean(ActivityAffectedObjectsType bean) {
            // We intentionally won't provide semi-filled beans, i.e. having type/mode/etc set, but no objects defined.
            return null;
        }
    }
}
