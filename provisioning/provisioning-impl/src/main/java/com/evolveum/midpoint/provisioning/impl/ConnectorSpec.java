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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
public class ConnectorSpec {
	
	private PrismObject<ResourceType> resource;
	private String connectorName;
	private String connectorOid;
	private PrismContainer<ConnectorConfigurationType> connectorConfiguration;
	
	public ConnectorSpec(PrismObject<ResourceType> resource, String connectorName, String connectorOid,
			PrismContainer<ConnectorConfigurationType> connectorConfiguration) {
		super();
		this.resource = resource;
		this.connectorName = connectorName;
		this.connectorOid = connectorOid;
		this.connectorConfiguration = connectorConfiguration;
	}

	public PrismObject<ResourceType> getResource() {
		return resource;
	}

	public String getConnectorName() {
		return connectorName;
	}

	public String getConnectorOid() {
		return connectorOid;
	}

	public PrismContainer<ConnectorConfigurationType> getConnectorConfiguration() {
		return connectorConfiguration;
	}

	public ConfiguredConnectorCacheKey getCacheKey() {
		return new ConfiguredConnectorCacheKey(resource.getOid(), connectorName);
	}
	
	@Override
	public String toString() {
		return "ConnectorSpec(" + resource + ", name=" + connectorName + ", oid="
				+ connectorOid + ")";
	}

}
