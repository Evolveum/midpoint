/**
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.schema.statistics;

import java.io.Serializable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class ConnectorOperationalStatus implements DebugDumpable, Serializable {
	private static final long serialVersionUID = 5644725183169800491L;

	public static final String F_CONNECTOR_NAME = "connectorName";
	public static final String F_CONNECTOR_CLASS_NAME = "connectorClassName";
	public static final String F_POOL_CONFIG_MIN_SIZE = "poolConfigMinSize";
	public static final String F_POOL_CONFIG_MAX_SIZE = "poolConfigMaxSize";
	public static final String F_POOL_CONFIG_MIN_IDLE = "poolConfigMinIdle";
	public static final String F_POOL_CONFIG_MAX_IDLE = "poolConfigMaxIdle";
	public static final String F_POOL_CONFIG_WAIT_TIMEOUT = "poolConfigWaitTimeout";
	public static final String F_POOL_CONFIG_MIN_EVICTABLE_IDLE_TIME = "poolConfigMinEvictableIdleTime";
	public static final String F_POOL_STATUS_NUM_IDLE = "poolStatusNumIdle";
	public static final String F_POOL_STATUS_NUM_ACTIVE = "poolStatusNumActive";

	private String connectorName;
	private String connectorClassName;

	// pool configuration
	private Integer poolConfigMinSize;
	private Integer poolConfigMaxSize;
	private Integer poolConfigMinIdle;
	private Integer poolConfigMaxIdle;
	private Long poolConfigWaitTimeout;
	private Long poolConfigMinEvictableIdleTime;

	// pool status
	private Integer poolStatusNumIdle;
	private Integer poolStatusNumActive;

	public String getConnectorName() {
		return connectorName;
	}

	public void setConnectorName(String connectorName) {
		this.connectorName = connectorName;
	}

	public String getConnectorClassName() {
		return connectorClassName;
	}

	public void setConnectorClassName(String connectorClassName) {
		this.connectorClassName = connectorClassName;
	}

	public Integer getPoolConfigMaxSize() {
		return poolConfigMaxSize;
	}

	public void setPoolConfigMaxSize(Integer poolConfigMaxSize) {
		this.poolConfigMaxSize = poolConfigMaxSize;
	}

	public Integer getPoolConfigMinSize() {
		return poolConfigMinSize;
	}

	public void setPoolConfigMinSize(Integer poolConfigMinSize) {
		this.poolConfigMinSize = poolConfigMinSize;
	}

	public Integer getPoolConfigMinIdle() {
		return poolConfigMinIdle;
	}

	public void setPoolConfigMinIdle(Integer poolConfigMinIdle) {
		this.poolConfigMinIdle = poolConfigMinIdle;
	}

	public Integer getPoolConfigMaxIdle() {
		return poolConfigMaxIdle;
	}

	public void setPoolConfigMaxIdle(Integer poolConfigMaxIdle) {
		this.poolConfigMaxIdle = poolConfigMaxIdle;
	}

	public Long getPoolConfigWaitTimeout() {
		return poolConfigWaitTimeout;
	}

	public void setPoolConfigWaitTimeout(Long poolConfigWaitTimeout) {
		this.poolConfigWaitTimeout = poolConfigWaitTimeout;
	}

	public Long getPoolConfigMinEvictableIdleTime() {
		return poolConfigMinEvictableIdleTime;
	}

	public void setPoolConfigMinEvictableIdleTime(Long poolConfigMinEvictableIdleTime) {
		this.poolConfigMinEvictableIdleTime = poolConfigMinEvictableIdleTime;
	}

	public Integer getPoolStatusNumIdle() {
		return poolStatusNumIdle;
	}

	public void setPoolStatusNumIdle(Integer poolStatusNumIdle) {
		this.poolStatusNumIdle = poolStatusNumIdle;
	}

	public Integer getPoolStatusNumActive() {
		return poolStatusNumActive;
	}

	public void setPoolStatusNumActive(Integer poolStatusNumActive) {
		this.poolStatusNumActive = poolStatusNumActive;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((connectorClassName == null) ? 0 : connectorClassName.hashCode());
		result = prime * result + ((poolConfigMaxIdle == null) ? 0 : poolConfigMaxIdle.hashCode());
		result = prime * result + ((poolConfigMaxSize == null) ? 0 : poolConfigMaxSize.hashCode());
		result = prime * result
				+ ((poolConfigMinEvictableIdleTime == null) ? 0 : poolConfigMinEvictableIdleTime.hashCode());
		result = prime * result + ((poolConfigMinIdle == null) ? 0 : poolConfigMinIdle.hashCode());
		result = prime * result + ((poolConfigMinSize == null) ? 0 : poolConfigMinSize.hashCode());
		result = prime * result + ((poolConfigWaitTimeout == null) ? 0 : poolConfigWaitTimeout.hashCode());
		result = prime * result + ((poolStatusNumActive == null) ? 0 : poolStatusNumActive.hashCode());
		result = prime * result + ((poolStatusNumIdle == null) ? 0 : poolStatusNumIdle.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ConnectorOperationalStatus other = (ConnectorOperationalStatus) obj;
		if (connectorClassName == null) {
			if (other.connectorClassName != null) {
				return false;
			}
		} else if (!connectorClassName.equals(other.connectorClassName)) {
			return false;
		}
		if (poolConfigMaxIdle == null) {
			if (other.poolConfigMaxIdle != null) {
				return false;
			}
		} else if (!poolConfigMaxIdle.equals(other.poolConfigMaxIdle)) {
			return false;
		}
		if (poolConfigMaxSize == null) {
			if (other.poolConfigMaxSize != null) {
				return false;
			}
		} else if (!poolConfigMaxSize.equals(other.poolConfigMaxSize)) {
			return false;
		}
		if (poolConfigMinEvictableIdleTime == null) {
			if (other.poolConfigMinEvictableIdleTime != null) {
				return false;
			}
		} else if (!poolConfigMinEvictableIdleTime.equals(other.poolConfigMinEvictableIdleTime)) {
			return false;
		}
		if (poolConfigMinIdle == null) {
			if (other.poolConfigMinIdle != null) {
				return false;
			}
		} else if (!poolConfigMinIdle.equals(other.poolConfigMinIdle)) {
			return false;
		}
		if (poolConfigMinSize == null) {
			if (other.poolConfigMinSize != null) {
				return false;
			}
		} else if (!poolConfigMinSize.equals(other.poolConfigMinSize)) {
			return false;
		}
		if (poolConfigWaitTimeout == null) {
			if (other.poolConfigWaitTimeout != null) {
				return false;
			}
		} else if (!poolConfigWaitTimeout.equals(other.poolConfigWaitTimeout)) {
			return false;
		}
		if (poolStatusNumActive == null) {
			if (other.poolStatusNumActive != null) {
				return false;
			}
		} else if (!poolStatusNumActive.equals(other.poolStatusNumActive)) {
			return false;
		}
		if (poolStatusNumIdle == null) {
			if (other.poolStatusNumIdle != null) {
				return false;
			}
		} else if (!poolStatusNumIdle.equals(other.poolStatusNumIdle)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ConnectorOperationalStatus(" + connectorName + ": " + connectorClassName + ": " + poolStatusNumActive + "/" + poolConfigMaxSize+")";
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ConnectorOperationalStatus\n");
		DebugUtil.debugDumpWithLabelLn(sb, "connectorName", connectorName, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "connectorClassName", connectorClassName, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "poolConfigMinSize", poolConfigMinSize, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "poolConfigMaxSize", poolConfigMaxSize, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "poolConfigMinIdle", poolConfigMinIdle, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "poolConfigMaxIdle", poolConfigMaxIdle, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "poolConfigWaitTimeout", poolConfigWaitTimeout, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "poolConfigMinEvictableIdleTime", poolConfigMinEvictableIdleTime, indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "poolStatusNumIdle", poolStatusNumIdle, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "poolStatusNumActive", poolStatusNumActive, indent + 1);
		return sb.toString();

	}




}
