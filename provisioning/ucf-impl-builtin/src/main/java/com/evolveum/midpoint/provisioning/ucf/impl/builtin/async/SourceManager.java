/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async;

import com.evolveum.midpoint.provisioning.ucf.api.AsyncUpdateSource;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.Amqp091SourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourceType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 *  Creates AsyncUpdateSource objects based on their configurations (AsyncUpdateSourceType objects).
 */
class SourceManager {

	private static final Trace LOGGER = TraceManager.getTrace(SourceManager.class);

	@NotNull private final AsyncUpdateConnectorInstance connectorInstance;

	SourceManager(@NotNull AsyncUpdateConnectorInstance connectorInstance) {
		this.connectorInstance = connectorInstance;
	}

	@NotNull
	Collection<AsyncUpdateSource> createSources(Collection<AsyncUpdateSourceType> sourceConfigurations) {
		if (sourceConfigurations.isEmpty()) {
			throw new IllegalStateException("No asynchronous update sources are configured");
		}
		return sourceConfigurations.stream()
				.map(this::createSource)
				.collect(Collectors.toList());
	}

	@NotNull
	private AsyncUpdateSource createSource(AsyncUpdateSourceType sourceConfiguration) {
		LOGGER.trace("Creating source from configuration: {}", sourceConfiguration);
		Class<? extends AsyncUpdateSource> sourceClass = determineSourceClass(sourceConfiguration);
		try {
			Method createMethod = sourceClass.getMethod("create", AsyncUpdateSourceType.class, AsyncUpdateConnectorInstance.class);
			AsyncUpdateSource source = (AsyncUpdateSource) createMethod.invoke(null, sourceConfiguration, connectorInstance);
			if (source == null) {
				throw new SystemException("Asynchronous update source was not created for " + sourceClass);
			}
			return source;
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
			throw new SystemException("Couldn't instantiate asynchronous update source class " + sourceClass + ": " + e.getMessage(), e);
		}
	}

	private Class<? extends AsyncUpdateSource> determineSourceClass(AsyncUpdateSourceType cfg) {
		if (cfg.getClassName() != null) {
			try {
				//noinspection unchecked
				return ((Class<? extends AsyncUpdateSource>) Class.forName(cfg.getClassName()));
			} catch (ClassNotFoundException e) {
				throw new SystemException("Couldn't find async source implementation class: " + cfg.getClassName());
			}
		} else if (cfg instanceof Amqp091SourceType) {
			return Amqp091AsyncUpdateSource.class;
		} else {
			throw new SystemException("Couldn't find async update source class for configuration: " + cfg.getClass());
		}
	}
}
