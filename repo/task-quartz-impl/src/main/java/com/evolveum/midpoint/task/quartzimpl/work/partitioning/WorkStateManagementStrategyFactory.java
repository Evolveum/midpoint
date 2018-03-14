/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.work.partitioning;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.quartzimpl.work.WorkBucketUtil;
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
 * Responsible for creation of configured work state related strategies.
 *
 * @author mederly
 */
@Component
public class WorkStateManagementStrategyFactory {

	@Autowired private PrismContext prismContext;

	private final Map<Class<? extends AbstractTaskWorkBucketsConfigurationType>, Class<? extends WorkBucketPartitioningStrategy>> strategyClassMap = new HashMap<>();

	{
		registerStrategyClass(NumericIntervalWorkBucketsConfigurationType.class, NumericIntervalWorkBucketPartitioningStrategy.class);
		registerStrategyClass(StringWorkBucketsConfigurationType.class, StringWorkBucketPartitioningStrategy.class);
		registerStrategyClass(EnumeratedWorkBucketsConfigurationType.class, EnumeratedWorkBucketPartitioningStrategy.class);
	}

	/**
	 * Creates work state management strategy based on provided configuration.
	 */
	@NotNull
	public WorkBucketPartitioningStrategy createStrategy(TaskWorkStateConfigurationType configuration) {

		AbstractTaskWorkBucketsConfigurationType cfg = WorkBucketUtil.getWorkBucketsConfiguration(configuration);

		if (cfg == null) {
			return new SingleNullWorkBucketPartitioningStrategy(configuration, prismContext);
		}

		Class<? extends WorkBucketPartitioningStrategy> strategyClass = strategyClassMap.get(cfg.getClass());
		if (strategyClass == null) {
			throw new IllegalStateException("Unknown or unsupported work state management configuration: " + configuration);
		}
		try {
			Constructor<? extends WorkBucketPartitioningStrategy> constructor = strategyClass.getConstructor(configuration.getClass(),
					PrismContext.class);
			return constructor.newInstance(configuration, prismContext);
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new SystemException("Couldn't instantiate work bucket partitioning strategy " + strategyClass + " for " + configuration);
		}
	}

	public void registerStrategyClass(Class<? extends AbstractTaskWorkBucketsConfigurationType> configurationClass,
			Class<? extends WorkBucketPartitioningStrategy> strategyClass) {
		strategyClassMap.put(configurationClass, strategyClass);
	}
}
