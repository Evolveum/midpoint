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

package com.evolveum.midpoint.util.caching;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import java.util.HashMap;
import java.util.Map;

/**
 *  This is a "compiled" configuration for a cache.
 *
 *  It is usually created by composing cache profiles defined using common-3 schema.
 *  (Even if the schema itself is not available in this module.)
 */
public class CacheConfiguration implements DebugDumpable {

	private Integer maxSize;
	private Integer timeToLive;
	private final Map<Class<?>, CacheObjectTypeConfiguration> objectTypes = new HashMap<>();

	public boolean supportsObjectType(Class<?> type) {
		if (!isAvailable()) {
			return false;
		} else {
			CacheObjectTypeConfiguration config = objectTypes.get(type);
			return config != null && config.supportsCaching();
		}
	}

	public CacheObjectTypeConfiguration getForObjectType(Class<?> type) {
		return objectTypes.get(type);
	}

	public boolean isAvailable() {
		return (maxSize == null || maxSize > 0) && (timeToLive == null || timeToLive > 0) && !objectTypes.isEmpty();
	}

	public static class CacheObjectTypeConfiguration {
		private Integer timeToLive;
		private Integer timeToVersionCheck;

		public Integer getTimeToLive() {
			return timeToLive;
		}

		public void setTimeToLive(Integer timeToLive) {
			this.timeToLive = timeToLive;
		}

		public Integer getTimeToVersionCheck() {
			return timeToVersionCheck;
		}

		public void setTimeToVersionCheck(Integer timeToVersionCheck) {
			this.timeToVersionCheck = timeToVersionCheck;
		}

		public boolean supportsCaching() {
			return timeToLive == null || timeToLive > 0;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (timeToLive != null) {
				sb.append("timeToLive=").append(timeToLive);
			}
			if (timeToVersionCheck != null) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append("timeToVersionCheck=").append(timeToVersionCheck);
			}
			if (sb.length() == 0) {
				sb.append("(default)");
			}
			return sb.toString();
		}
	}

	public Integer getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(Integer maxSize) {
		this.maxSize = maxSize;
	}

	public Integer getTimeToLive() {
		return timeToLive;
	}

	public void setTimeToLive(Integer timeToLive) {
		this.timeToLive = timeToLive;
	}

	public Map<Class<?>, CacheObjectTypeConfiguration> getObjectTypes() {
		return objectTypes;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		if (maxSize != null) {
			DebugUtil.debugDumpWithLabelLn(sb, "maxSize", maxSize, indent);
		}
		if (timeToLive != null) {
			DebugUtil.debugDumpWithLabelLn(sb, "timeToLive", timeToLive, indent);
		}
		DebugUtil.debugDumpLabelLn(sb, "object types", indent);
		for (Map.Entry<Class<?>, CacheObjectTypeConfiguration> entry : objectTypes.entrySet()) {
			DebugUtil.debugDumpWithLabelLn(sb, entry.getKey().getSimpleName(), entry.getValue().toString(), indent+1);
		}
		return sb.toString();
	}


}
