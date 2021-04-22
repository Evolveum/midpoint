/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation;

import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates configured bucket content factories.
 */
@Component
public class BucketContentFactoryCreator {

    private final Map<Class<? extends AbstractWorkSegmentationType>, Class<? extends BucketContentFactory>> classMap
            = new HashMap<>();

    {
        registerFactoryClass(NumericWorkSegmentationType.class, NumericBucketContentFactory.class);
        registerFactoryClass(StringWorkSegmentationType.class, StringBucketContentFactory.class);
        registerFactoryClass(OidWorkSegmentationType.class, StringBucketContentFactory.class);
        registerFactoryClass(ExplicitWorkSegmentationType.class, ExplicitBucketContentFactory.class);
    }

    @NotNull
    public BucketContentFactory createContentFactory(TaskPartDefinitionType partDef) {
        TaskWorkManagementType workManagement = partDef != null ? partDef.getWorkManagement() : null;
        WorkBucketsManagementType buckets = workManagement != null ? workManagement.getBuckets() : null;
        return createContentFactory(buckets);
    }

    /**
     * Creates work state management strategy based on provided configuration.
     */
    @NotNull BucketContentFactory createContentFactory(@Nullable WorkBucketsManagementType bucketingConfig) {

        AbstractWorkSegmentationType segmentationConfig = TaskWorkStateUtil.getWorkSegmentationConfiguration(bucketingConfig);

        if (segmentationConfig == null) {
            return new NullBucketContentFactory();
        }

        Class<? extends BucketContentFactory> factoryClass = classMap.get(segmentationConfig.getClass());
        if (factoryClass == null) {
            throw new IllegalStateException("Unknown or unsupported work state management configuration: " + bucketingConfig);
        }
        try {
            Constructor<? extends BucketContentFactory> constructor = factoryClass.getConstructor(segmentationConfig.getClass());
            return constructor.newInstance(segmentationConfig);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new SystemException("Couldn't instantiate work bucket segmentation strategy " + factoryClass +
                    " for " + bucketingConfig, e);
        }
    }

    private void registerFactoryClass(Class<? extends AbstractWorkSegmentationType> configurationClass,
            Class<? extends BucketContentFactory> strategyClass) {
        classMap.put(configurationClass, strategyClass);
    }
}
