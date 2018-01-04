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

import java.util.Collection;
import java.util.Random;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnector;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnectorConfiguration;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManualConnectorInstance;
import com.evolveum.midpoint.repo.api.RepositoryAware;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 *
 */
@ManagedConnector(type="ManualConnector", version="1.0.0")
public class ManualConnectorInstance extends AbstractManualConnectorInstance implements RepositoryAware {

	public static final String OPERATION_QUERY_CASE = ".queryCase";

	private static final Trace LOGGER = TraceManager.getTrace(ManualConnectorInstance.class);

	private ManualConnectorConfiguration configuration;

	private RepositoryService repositoryService;

	private boolean connected = false;

	private static int randomDelayRange = 0;

	protected static final Random RND = new Random();

	@ManagedConnectorConfiguration
	public ManualConnectorConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(ManualConnectorConfiguration configuration) {
		this.configuration = configuration;
	}

	public boolean isConnected() {
		return connected;
	}

	@Override
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	@Override
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}


	@Override
	protected String createTicketAdd(PrismObject<? extends ShadowType> object,
			Collection<Operation> additionalOperations, OperationResult result) throws CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		LOGGER.debug("Creating case to add account\n{}", object.debugDump(1));
		String description = "Please create account "+object;
		PrismObject<CaseType> acase = addCase(description, result);
		return acase.getOid();
	}

	@Override
	protected String createTicketModify(ObjectClassComplexTypeDefinition objectClass,
			Collection<? extends ResourceAttribute<?>> identifiers, Collection<Operation> changes,
			OperationResult result) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		LOGGER.debug("Creating case to modify account {}:\n{}", identifiers, DebugUtil.debugDump(changes, 1));
		if (InternalsConfig.isSanityChecks()) {
			if (MiscUtil.hasDuplicates(changes)) {
				throw new SchemaException("Duplicated changes: "+changes);
			}
		}
		String description = "Please modify account "+identifiers+": "+changes;
		PrismObject<CaseType> acase = addCase(description, result);
		return acase.getOid();
	}

	@Override
	protected String createTicketDelete(ObjectClassComplexTypeDefinition objectClass,
			Collection<? extends ResourceAttribute<?>> identifiers, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
			SchemaException, ConfigurationException {
		LOGGER.debug("Creating case to delete account {}", identifiers);
		String description = "Please delete account "+identifiers;
		PrismObject<CaseType> acase;
		try {
			acase = addCase(description, result);
		} catch (ObjectAlreadyExistsException e) {
			// should not happen
			throw new SystemException(e.getMessage(), e);
		}
		return acase.getOid();
	}

	private PrismObject<CaseType> addCase(String description, OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
		PrismObject<CaseType> acase = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(CaseType.class).instantiate();
		CaseType caseType = acase.asObjectable();

		if (randomDelayRange != 0) {
			int waitMillis = RND.nextInt(randomDelayRange);
			LOGGER.info("Manual connector waiting {} ms before creating the case", waitMillis);
			try {
				Thread.sleep(waitMillis);
			} catch (InterruptedException e) {
				LOGGER.error("Manual connector wait is interrupted");
			}
			LOGGER.info("Manual connector wait is over");
		}

		String caseOid = OidUtil.generateOid();

		caseType.setOid(caseOid);
		// TODO: human-readable case ID
		caseType.setName(new PolyStringType(caseOid));

		caseType.setDescription(description);

		// subtype
		caseType.setState(SchemaConstants.CASE_STATE_OPEN);

		// TODO: case payload
		// TODO: a lot of other things

		// TODO: move to case-manager

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("CREATING CASE:\n{}", acase.debugDump(1));
		}

		repositoryService.addObject(acase, null, result);
		return acase;
	}

	@Override
	public OperationResultStatus queryOperationStatus(String asyncronousOperationReference, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createMinorSubresult(OPERATION_QUERY_CASE);
		
		InternalMonitor.recordConnectorOperation("queryOperationStatus");

		PrismObject<CaseType> acase;
		try {
			acase = repositoryService.getObject(CaseType.class, asyncronousOperationReference, null, result);
		} catch (ObjectNotFoundException | SchemaException e) {
			result.recordFatalError(e);
			throw e;
		}

		CaseType caseType = acase.asObjectable();
		String state = caseType.getState();

		if (QNameUtil.matchWithUri(SchemaConstants.CASE_STATE_OPEN_QNAME, state)) {
			result.recordSuccess();
			return OperationResultStatus.IN_PROGRESS;

		} else if (QNameUtil.matchWithUri(SchemaConstants.CASE_STATE_CLOSED_QNAME, state)) {

			String outcome = caseType.getOutcome();
			OperationResultStatus status = translateOutcome(outcome);
			result.recordSuccess();
			return status;

		} else {
			SchemaException e = new SchemaException("Unknown case state "+state);
			result.recordFatalError(e);
			throw e;
		}

	}

	private OperationResultStatus translateOutcome(String outcome) {

		// TODO: better algorithm
		if (outcome == null) {
			return null;
		} else if (outcome.equals(OperationResultStatusType.SUCCESS.value())) {
			return OperationResultStatus.SUCCESS;
		} else {
			return OperationResultStatus.UNKNOWN;
		}
	}

	@Override
	protected void connect(OperationResult result) {
		if (connected && InternalsConfig.isSanityChecks()) {
			throw new IllegalStateException("Double connect in "+this);
		}
		connected = true;
		// Nothing else to do
	}

	@Override
	public void test(OperationResult parentResult) {
		OperationResult connectionResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
		connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ManualConnectorInstance.class);
		connectionResult.addContext("connector", getConnectorObject().toString());

		if (repositoryService == null) {
			connectionResult.recordFatalError("No repository service");
			return;
		}

		if (!connected && InternalsConfig.isSanityChecks()) {
			throw new IllegalStateException("Attempt to test non-connected connector instance "+this);
		}

		connectionResult.recordSuccess();
	}

	@Override
	public void dispose() {
		// Nothing to dispose
		connected = false;
	}

	public static int getRandomDelayRange() {
		return randomDelayRange;
	}

	public static void setRandomDelayRange(int randomDelayRange) {
		ManualConnectorInstance.randomDelayRange = randomDelayRange;
	}

}
