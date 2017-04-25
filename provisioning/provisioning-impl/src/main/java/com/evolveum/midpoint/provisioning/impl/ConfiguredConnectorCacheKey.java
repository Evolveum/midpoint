/*
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

/**
 * @author semancik
 *
 */
public class ConfiguredConnectorCacheKey {
	
	private String resourceOid;
	private String connectorName;
	
	public ConfiguredConnectorCacheKey(String resourceOid, String connectorName) {
		super();
		this.resourceOid = resourceOid;
		this.connectorName = connectorName;
	}

	public String getResourceOid() {
		return resourceOid;
	}

	public String getConnectorName() {
		return connectorName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((connectorName == null) ? 0 : connectorName.hashCode());
		result = prime * result + ((resourceOid == null) ? 0 : resourceOid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConfiguredConnectorCacheKey other = (ConfiguredConnectorCacheKey) obj;
		if (connectorName == null) {
			if (other.connectorName != null)
				return false;
		} else if (!connectorName.equals(other.connectorName))
			return false;
		if (resourceOid == null) {
			if (other.resourceOid != null)
				return false;
		} else if (!resourceOid.equals(other.resourceOid))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ConfiguredConnectorCacheKey(" + resourceOid + ":" + connectorName + ")";
	}
	
	
}
