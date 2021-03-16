/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for creation of configured work segmentation strategies.
 *
 * @author mederly
 */
@Component
public class WorkSegmentationStrategyFactory {

    @Autowired private PrismContext prismContext;

    private final Map<Class<? extends AbstractWorkSegmentationType>, Class<? extends WorkSegmentationStrategy>> strategyClassMap = new HashMap<>();

    {
        registerStrategyClass(NumericWorkSegmentationType.class, NumericWorkSegmentationStrategy.class);
        registerStrategyClass(StringWorkSegmentationType.class, StringWorkSegmentationStrategy.class);
        registerStrategyClass(OidWorkSegmentationType.class, StringWorkSegmentationStrategy.class);
        registerStrategyClass(ExplicitWorkSegmentationType.class, ExplicitWorkSegmentationStrategy.class);
    }

    /**
     * Creates work state management strategy based on provided configuration.
     */
    @NotNull
    public WorkSegmentationStrategy createStrategy(TaskWorkManagementType configuration) {

        AbstractWorkSegmentationType cfg = TaskWorkStateUtil.getWorkSegmentationConfiguration(configuration);

        if (cfg == null) {
            return new SingleNullWorkSegmentationStrategy(configuration, prismContext);
        }

        Class<? extends WorkSegmentationStrategy> strategyClass = strategyClassMap.get(cfg.getClass());
        if (strategyClass == null) {
            throw new IllegalStateException("Unknown or unsupported work state management configuration: " + configuration);
        }
        try {
            Constructor<? extends WorkSegmentationStrategy> constructor = strategyClass.getConstructor(configuration.getClass(),
                    PrismContext.class);
            return constructor.newInstance(configuration, prismContext);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new SystemException("Couldn't instantiate work bucket segmentation strategy " + strategyClass + " for " + configuration, e);
        }
    }

    public void registerStrategyClass(Class<? extends AbstractWorkSegmentationType> configurationClass,
            Class<? extends WorkSegmentationStrategy> strategyClass) {
        strategyClassMap.put(configurationClass, strategyClass);
    }
}
