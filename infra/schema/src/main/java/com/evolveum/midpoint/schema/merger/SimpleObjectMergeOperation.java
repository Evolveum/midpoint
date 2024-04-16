/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.prism.OriginMarker;
import com.evolveum.midpoint.prism.impl.GenericItemMerger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author Viliam Repan
 */
@Deprecated
public class SimpleObjectMergeOperation extends BaseMergeOperation<ObjectType> {

    private static final Set<Class<? extends ObjectType>> SUPPORTED_TYPES = Set.of(
            LookupTableType.class,
            SecurityPolicyType.class,
            SystemConfigurationType.class,
            RoleType.class,
            TaskType.class,
            ReportType.class,
            ObjectCollectionType.class,
            DashboardType.class,
            UserType.class,
            ArchetypeType.class,
            MarkType.class,
            ObjectTemplateType.class,
            ServiceType.class,
            ValuePolicyType.class
    );

    public SimpleObjectMergeOperation(@Nullable OriginMarker originMarker, @NotNull ObjectType target, @NotNull ObjectType source) {
        super(
                target,
                source,
                new GenericItemMerger(
                        originMarker,
                        createPathMap(Map.of())));
    }

    /**
     * Returns true if the merge is supported (and was reviewed, tested) for the given object type.
     * Note:
     * </p>
     * Naive/simple merge will work for all object types, however merge might create more duplicates
     * for values (properties, containers) where natural key wasn't properly defined.
     *
     * @param target
     * @param <O>
     * @return
     */
    public static <O extends ObjectType> boolean isMergeSupported(@NotNull PrismObject<O> target) {
        Class<?> type = target.getCompileTimeClass();

        return SUPPORTED_TYPES.contains(type);
    }

    public static <O extends ObjectType> void merge(@NotNull PrismObject<O> target, @NotNull PrismObject<O> source)
            throws ConfigurationException, SchemaException {

        new SimpleObjectMergeOperation(null, target.asObjectable(), source.asObjectable()).execute();
    }
}
