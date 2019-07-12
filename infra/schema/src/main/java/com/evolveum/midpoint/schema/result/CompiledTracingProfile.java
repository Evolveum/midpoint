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

package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 *
 */
public class CompiledTracingProfile implements Serializable {

	private static final Trace LOGGER = TraceManager.getTrace(CompiledTracingProfile.class);

	@NotNull private final TracingProfileType definition;
	private final Map<Class<? extends TraceType>, TracingLevelType> levelMap = new HashMap<>();

	private CompiledTracingProfile(@NotNull TracingProfileType definition) {
		this.definition = definition;
	}

	public static CompiledTracingProfile create(TracingProfileType resolvedProfile, PrismContext prismContext) {
		CompiledTracingProfile compiledProfile = new CompiledTracingProfile(resolvedProfile);
		PrismSchema commonSchema = prismContext.getSchemaRegistry().findSchemaByNamespace(SchemaConstants.NS_C);
		for (ComplexTypeDefinition complexTypeDefinition : commonSchema.getComplexTypeDefinitions()) {
			Class<?> clazz = complexTypeDefinition.getCompileTimeClass();
			if (clazz != null && TraceType.class.isAssignableFrom(clazz)) {
				//noinspection unchecked
				Class<? extends TraceType> traceClass = (Class<? extends TraceType>) clazz;
				compiledProfile.levelMap.put(traceClass, getLevel(resolvedProfile, traceClass, prismContext));
			}
		}
		return compiledProfile;
	}

	@NotNull
	public TracingLevelType getLevel(@NotNull Class<? extends TraceType> traceClass) {
		return ObjectUtils.defaultIfNull(levelMap.get(traceClass), TracingLevelType.MINIMAL);
	}

	@NotNull
	public TracingProfileType getDefinition() {
		return definition;
	}

	@NotNull
	private static TracingLevelType getLevel(TracingProfileType resolvedProfile, Class<? extends TraceType> traceClass, PrismContext prismContext) {
		if (!resolvedProfile.getRef().isEmpty()) {
			throw new IllegalArgumentException("Profile is not resolved: " + resolvedProfile);
		}
		List<QName> ancestors = getAncestors(traceClass, prismContext);
		LOGGER.trace("Ancestors for {}: {}", traceClass, ancestors);
		for (QName ancestor : ancestors) {
			TracingLevelType level = getLevel(resolvedProfile, ancestor);
			if (level != null) {
				return level;
			}
		}
		return TracingLevelType.MINIMAL;
	}

	private static List<QName> getAncestors(Class<? extends TraceType> traceClass, PrismContext prismContext) {
		List<QName> rv = new ArrayList<>();
		for (;;) {
			if (!TraceType.class.isAssignableFrom(traceClass)) {
				throw new IllegalStateException("Wrong trace class: " + traceClass);
			}
			rv.add(prismContext.getSchemaRegistry().findTypeDefinitionByCompileTimeClass(traceClass, ComplexTypeDefinition.class).getTypeName());
			if (traceClass.equals(TraceType.class)) {
				return rv;
			}
			//noinspection unchecked
			traceClass = (Class<? extends TraceType>) traceClass.getSuperclass();
		}
	}

	private static TracingLevelType getLevel(@NotNull TracingProfileType profile, @NotNull QName traceClassName) {
		boolean isRoot = TraceType.COMPLEX_TYPE.equals(traceClassName);
		Set<TracingLevelType> levels = profile.getTracingTypeProfile().stream()
				.filter(p -> isRoot && p.getOperationType().isEmpty() || QNameUtil.matchAny(traceClassName, p.getOperationType()))
				.map(p -> defaultIfNull(p.getLevel(), TracingLevelType.MINIMAL))
				.collect(Collectors.toSet());
		LOGGER.trace("Levels for {}: {}", traceClassName, levels);
		if (!levels.isEmpty()) {
			TracingLevelType level = levels.stream().max(Comparator.comparing(Enum::ordinal)).orElse(null);
			LOGGER.trace("Max level for {}: {}", traceClassName, level);
			return level;
		} else {
			return null;
		}
	}

	public boolean isLevel(@NotNull Class<? extends TraceType> traceClass, @NotNull TracingLevelType level) {
		return getLevel(traceClass).ordinal() >= level.ordinal();
	}
}
