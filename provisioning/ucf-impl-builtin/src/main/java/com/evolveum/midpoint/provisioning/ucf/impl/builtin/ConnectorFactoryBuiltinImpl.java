/**
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
package com.evolveum.midpoint.provisioning.ucf.impl.builtin;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

/**
 * Connector factory for the connectors built-in to midPoint, such as
 * the "manual connector".
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ConnectorFactoryBuiltinImpl implements ConnectorFactory {
	
	@Override
	public ConnectorInstance createConnectorInstance(ConnectorType connectorType, String namespace,
			String desc) throws ObjectNotFoundException, SchemaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ConnectorType> listConnectors(ConnectorHostType host, OperationResult parentRestul)
			throws CommunicationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void selfTest(OperationResult parentTestResult) {
		// Nothing to do
	}

	@Override
	public boolean supportsFramework(String frameworkIdentifier) {
		return SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN.equals(frameworkIdentifier);
	}
	
	@Override
	public String getFrameworkVersion() {
		return "1.0.0";
	}

	@Override
	public void shutdown() {
		// Nothing to do
	}

}
