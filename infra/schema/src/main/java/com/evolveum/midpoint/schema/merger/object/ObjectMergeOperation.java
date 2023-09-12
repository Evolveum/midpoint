/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.object;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.Validate;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static java.util.Map.entry;

public class ObjectMergeOperation {

    public static final Map<Class<? extends ObjectType>, Class<? extends BaseMergeOperation>> MERGE_OPERATIONS = Map.ofEntries(
            entry(LookupTableType.class, LookupTableMergeOperation.class),
            entry(SecurityPolicyType.class, SecurityPolicyMergeOperation.class),
            entry(SystemConfigurationType.class, SystemConfigurationMergeOperation.class),
            entry(RoleType.class, RoleMergeOperation.class),
            entry(TaskType.class, TaskMergeOperation.class),
            entry(ReportType.class, ReportMergeOperation.class),
            entry(ObjectCollectionType.class, ObjectCollectionMergeOperation.class),
            entry(DashboardType.class, DashboardMergeOperation.class)
    );

    public static <O extends ObjectType> boolean hasMergeOperationFor(PrismObject<O> target) {
        Class<?> type = target.getCompileTimeClass();

        return MERGE_OPERATIONS.containsKey(type);
    }

    public static <O extends ObjectType> void merge(
            PrismObject<O> target, PrismObject<O> source) throws ConfigurationException, SchemaException {
        Class<?> type = target.getCompileTimeClass();

        Class<? extends BaseMergeOperation> clazz = MERGE_OPERATIONS.get(type);
        Validate.notNull(clazz, "Merge operation not available for type " + type.getName());

        try {
            BaseMergeOperation<O> operation = clazz.getConstructor(type, type)
                    .newInstance(target.asObjectable(), source.asObjectable());
            operation.execute();
        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new SystemException("Couldn't merge objects", ex);
        }
    }
}
