/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation;

import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Creates configured bucket content factories.
 */
@Component
public class BucketContentFactoryGenerator {

    /**
     * Suppliers for individual configuration bean classes. For each entry it holds that the config class (the key)
     * is the same as the first type parameter of supplier (the value).
     */
    private final Map<Class<? extends AbstractWorkSegmentationType>, BucketContentFactorySupplier<?, ?>> supplierMap
            = new HashMap<>();

    {
        registerSupplier(NumericWorkSegmentationType.class, (cfg, ctx) -> new NumericBucketContentFactory(cfg));
        registerSupplier(StringWorkSegmentationType.class, (cfg, ctx) -> new StringBucketContentFactory(cfg));
        registerSupplier(OidWorkSegmentationType.class, (cfg, ctx) -> new StringBucketContentFactory(cfg));
        registerSupplier(ExplicitWorkSegmentationType.class, (cfg, ctx) -> new ExplicitBucketContentFactory(cfg));
        registerSupplier(ImplicitWorkSegmentationType.class, this::createContentFactoryInImplicitCase);
    }

    /**
     * Creates work state management strategy based on provided configuration.
     */
    @NotNull
    public BucketContentFactory createContentFactory(@Nullable BucketsDefinitionType bucketing,
            @Nullable ImplicitSegmentationResolver implicitSegmentationResolver) {
        return createContentFactory(
                BucketingUtil.getWorkSegmentationConfiguration(bucketing),
                implicitSegmentationResolver);
    }

    @NotNull
    private BucketContentFactory createContentFactory(@Nullable AbstractWorkSegmentationType segmentationConfig,
            @Nullable ImplicitSegmentationResolver implicitSegmentationResolver) {
        if (segmentationConfig == null) {
            return new NullBucketContentFactory();
        }

        // We can safely do the casting because of the condition that holds for supplier map entries (described above).
        //noinspection unchecked
        BucketContentFactorySupplier<AbstractWorkSegmentationType, ?> supplier =
                (BucketContentFactorySupplier<AbstractWorkSegmentationType, ?>)
                        supplierMap.get(segmentationConfig.getClass());
        if (supplier == null) {
            throw new IllegalStateException("Unknown or unsupported work state management configuration: " + segmentationConfig);
        } else {
            return supplier.supply(segmentationConfig, implicitSegmentationResolver);
        }
    }

    private <T extends AbstractWorkSegmentationType> void registerSupplier(Class<T> configurationClass,
            BucketContentFactorySupplier<T, ?> supplier) {
        supplierMap.put(configurationClass, supplier);
    }

    private @NotNull BucketContentFactory createContentFactoryInImplicitCase(
            @NotNull ImplicitWorkSegmentationType configuration,
            ImplicitSegmentationResolver resolver) {
        argCheck(resolver != null, "No bucketing context for implicit work segmentation");
        return createContentFactory(
                resolver.resolveImplicitSegmentation(configuration),
                null // We no longer need the resolver (this also avoids endless loops)
        );
    }
}
