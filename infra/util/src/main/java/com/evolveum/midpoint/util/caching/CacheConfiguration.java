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
	private Boolean traceMiss;
	private Boolean tracePass;
	private StatisticsLevel statisticsLevel;
	private final Map<Class<?>, CacheObjectTypeConfiguration> objectTypes = new HashMap<>();

	public enum StatisticsLevel {
		SKIP, PER_CACHE, PER_OBJECT_TYPE
	}

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

	public class CacheObjectTypeConfiguration {
		private Integer timeToLive;
		private Integer timeToVersionCheck;
		private Boolean traceMiss;
		private Boolean tracePass;
		private StatisticsLevel statisticsLevel;

		public Integer getEffectiveTimeToLive() {
			return timeToLive != null ? timeToLive : CacheConfiguration.this.timeToLive;
		}

		public void setTimeToLive(Integer timeToLive) {
			this.timeToLive = timeToLive;
		}

		public Integer getEffectiveTimeToVersionCheck() {
			return timeToVersionCheck;
		}

		public void setTimeToVersionCheck(Integer timeToVersionCheck) {
			this.timeToVersionCheck = timeToVersionCheck;
		}

		public boolean getEffectiveTraceMiss() {
			return traceMiss != null ? traceMiss : Boolean.TRUE.equals(CacheConfiguration.this.traceMiss);
		}

		public void setTraceMiss(Boolean traceMiss) {
			this.traceMiss = traceMiss;
		}

		public boolean getEffectiveTracePass() {
			return tracePass != null ? tracePass : Boolean.TRUE.equals(CacheConfiguration.this.tracePass);
		}

		public void setTracePass(Boolean tracePass) {
			this.tracePass = tracePass;
		}

		public StatisticsLevel getEffectiveStatisticsLevel() {
			return statisticsLevel != null ? statisticsLevel : CacheConfiguration.this.statisticsLevel;
		}

		public void setStatisticsLevel(StatisticsLevel statisticsLevel) {
			this.statisticsLevel = statisticsLevel;
		}

		public boolean supportsCaching() {
			return timeToLive == null || timeToLive > 0;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			append(sb, "timeToLive", timeToLive);
			append(sb, "timeToVersionCheck", timeToVersionCheck);
			append(sb, "traceMiss", traceMiss);
			append(sb, "tracePass", tracePass);
			append(sb, "statisticsLevel", statisticsLevel);
			if (sb.length() == 0) {
				sb.append("(default)");
			}
			return sb.toString();
		}

		private void append(StringBuilder sb, String label, Object value) {
			if (value != null) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(label).append("=").append(value);
			}
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

	public Boolean getTraceMiss() {
		return traceMiss;
	}

	public void setTraceMiss(Boolean traceMiss) {
		this.traceMiss = traceMiss;
	}

	public Boolean getTracePass() {
		return tracePass;
	}

	public void setTracePass(Boolean tracePass) {
		this.tracePass = tracePass;
	}

	public StatisticsLevel getStatisticsLevel() {
		return statisticsLevel;
	}

	public void setStatisticsLevel(StatisticsLevel statisticsLevel) {
		this.statisticsLevel = statisticsLevel;
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
		if (traceMiss != null) {
			DebugUtil.debugDumpWithLabelLn(sb, "traceMiss", traceMiss, indent);
		}
		if (tracePass != null) {
			DebugUtil.debugDumpWithLabelLn(sb, "tracePass", tracePass, indent);
		}
		if (statisticsLevel != null) {
			DebugUtil.debugDumpWithLabelLn(sb, "statisticsLevel", String.valueOf(statisticsLevel), indent);
		}
		DebugUtil.debugDumpLabelLn(sb, "object types", indent);
		for (Map.Entry<Class<?>, CacheObjectTypeConfiguration> entry : objectTypes.entrySet()) {
			DebugUtil.debugDumpWithLabelLn(sb, entry.getKey().getSimpleName(), entry.getValue().toString(), indent+1);
		}
		return sb.toString();
	}

	public static boolean getTraceMiss(CacheObjectTypeConfiguration typeConfig, CacheConfiguration cacheConfig) {
		if (typeConfig != null) {
			return typeConfig.getEffectiveTraceMiss();
		} else {
			return cacheConfig != null && Boolean.TRUE.equals(cacheConfig.getTraceMiss());
		}
	}

	public static boolean getTracePass(CacheObjectTypeConfiguration typeConfig, CacheConfiguration cacheConfig) {
		if (typeConfig != null) {
			return typeConfig.getEffectiveTracePass();
		} else {
			return cacheConfig != null && Boolean.TRUE.equals(cacheConfig.getTracePass());
		}
	}

	public static StatisticsLevel getStatisticsLevel(CacheObjectTypeConfiguration typeConfig,
			CacheConfiguration config) {
		if (typeConfig != null) {
			return typeConfig.getEffectiveStatisticsLevel();
		} else if (config != null) {
			return config.getStatisticsLevel();
		} else {
			return null;
		}
	}



}
