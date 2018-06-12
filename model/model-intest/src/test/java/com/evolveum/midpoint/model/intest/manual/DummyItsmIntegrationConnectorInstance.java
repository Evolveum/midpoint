/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.model.intest.manual;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnector;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnectorConfiguration;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManualConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.ManualConnectorInstance;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
@ManagedConnector(type="DummyItsmIntegrationConnector", version="1.0.0")
public class DummyItsmIntegrationConnectorInstance extends AbstractManualConnectorInstance {
	
	private static final Trace LOGGER = TraceManager.getTrace(DummyItsmIntegrationConnectorInstance.class);
	
	private DummyItsmIntegrationConnectorConfiguration configuration;
	
	private boolean connected = false;
	
	@ManagedConnectorConfiguration
	public DummyItsmIntegrationConnectorConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(DummyItsmIntegrationConnectorConfiguration configuration) {
		this.configuration = configuration;
	}

	public boolean isConnected() {
		return connected;
	}
	
	@Override
	public OperationResultStatus queryOperationStatus(String asynchronousOperationReference,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		DummyItsm itsm = DummyItsm.getInstance();
		DummyItsmTicket ticket;
		try {
			ticket = itsm.findTicket(asynchronousOperationReference);
		} catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException e) {
			LOGGER.info("GET "+asynchronousOperationReference+" => "+e);
			throw e;
		} catch (Exception e) {
			LOGGER.info("GET "+asynchronousOperationReference+" => "+e);
			throw new RuntimeException(e);
		}
		LOGGER.info("GET "+asynchronousOperationReference+" => "+ticket);
		switch (ticket.getStatus()) {
			case OPEN:
				return OperationResultStatus.IN_PROGRESS;
			case CLOSED:
				return OperationResultStatus.SUCCESS;
			default:
				return null;
		}
	}
	

	@Override
	protected String createTicketAdd(PrismObject<? extends ShadowType> object,
			Collection<Operation> additionalOperations, OperationResult result) throws CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		DummyItsm itsm = DummyItsm.getInstance();
		String identifier;
		try {
			identifier = itsm.createTicket("ADD "+object);
		} catch (CommunicationException | SchemaException | ObjectAlreadyExistsException | ConfigurationException e) {
			LOGGER.info("ADD "+object+" => "+e);
			throw e;
		} catch (Exception e) {
			LOGGER.info("ADD "+object+" => "+e);
			throw new GenericFrameworkException(e);
		}
		LOGGER.info("ADD "+object+" => "+identifier);
		return identifier;
	}

	@Override
	protected String createTicketModify(ObjectClassComplexTypeDefinition objectClass,
			PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers,
			String resourceOid, Collection<Operation> changes, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
			SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		DummyItsm itsm = DummyItsm.getInstance();
		String identifier;
		try {
			identifier = itsm.createTicket("MODIFY "+identifiers+": "+changes);
		} catch (CommunicationException | SchemaException | ObjectAlreadyExistsException | ConfigurationException e) {
			LOGGER.info("MODIFY "+identifiers+": "+changes+" => "+e);
			throw e;
		} catch (Exception e) {
			LOGGER.info("MODIFY "+identifiers+": "+changes+" => "+e);
			throw new GenericFrameworkException(e);
		}
		LOGGER.info("MODIFY "+identifiers+": "+changes+" => "+identifier);
		return identifier;
	}

	@Override
	protected String createTicketDelete(ObjectClassComplexTypeDefinition objectClass,
			PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers,
			String resourceOid, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException {
		DummyItsm itsm = DummyItsm.getInstance();
		String identifier;
		try {
			identifier = itsm.createTicket("DELETE "+identifiers);
		} catch (CommunicationException | SchemaException | ObjectNotFoundException | ConfigurationException e) {
			LOGGER.info("DELETE "+identifiers+" => "+e);
			throw e;
		} catch (Exception e) {
			LOGGER.info("DELETE "+identifiers+" => "+e);
			throw new GenericFrameworkException(e);
		}
		LOGGER.info("DELETE "+identifiers+" => "+identifier);
		return identifier;
	}

	@Override
	protected void connect(OperationResult result) {
		if (connected && InternalsConfig.isSanityChecks()) {
			throw new IllegalStateException("Double connect in "+this);
		}
		connected = true;
		LOGGER.info("CONNECT");
	}
	
	@Override
	public void dispose() {
		connected = false;
		LOGGER.info("DISPOSE");
	}
	
	@Override
	public void test(OperationResult parentResult) {
		OperationResult connectionResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
		connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ManualConnectorInstance.class);
		connectionResult.addContext("connector", getConnectorObject().toString());
		try {
			DummyItsm itsm = DummyItsm.getInstance();
			itsm.test();
		} catch (Exception e) {
			LOGGER.info("TEST: "+e);
			connectionResult.recordFatalError(e);
		}
	
		if (!connected) {
			throw new IllegalStateException("Attempt to test non-connected connector instance "+this);
		}
		
		LOGGER.info("TEST: OK");
		
		connectionResult.recordSuccess();
	}


}
