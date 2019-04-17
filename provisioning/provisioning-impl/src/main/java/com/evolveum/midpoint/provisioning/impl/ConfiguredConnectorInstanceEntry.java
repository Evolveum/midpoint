/**
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
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;

/**
 * @author semancik
 *
 */
public class ConfiguredConnectorInstanceEntry {
	
	private String connectorOid;
	private PrismContainer<ConnectorConfigurationType> configuration;
	private ConnectorInstance connectorInstance;
	
	public String getConnectorOid() {
		return connectorOid;
	}

	public void setConnectorOid(String connectorOid) {
		this.connectorOid = connectorOid;
	}

	public PrismContainer<ConnectorConfigurationType> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(PrismContainer<ConnectorConfigurationType> configuration) {
		this.configuration = configuration;
	}
	
	public boolean isConfigured() {
		return configuration != null;
	}

	public ConnectorInstance getConnectorInstance() {
		return connectorInstance;
	}

	public void setConnectorInstance(ConnectorInstance connectorInstance) {
		this.connectorInstance = connectorInstance;
	}

}
