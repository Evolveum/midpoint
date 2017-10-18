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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManualConnectorInstance;
import com.evolveum.midpoint.repo.api.RepositoryAware;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.*;

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

	private static final String DEFAULT_OPERATOR_OID = "00000000-0000-0000-0000-000000000002";  // administrator
	
	protected static final Random RND = new Random();

	private Clock clock = new Clock();
	
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
		ObjectDelta<? extends ShadowType> objectDelta = ObjectDelta.createAddDelta(object);
		ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);
		String shadowName = getShadowIdentifier(ShadowUtil.getPrimaryIdentifiers(object));
		String description = "Please create resource account: "+shadowName;
		PrismObject<CaseType> acase = addCase(description, ShadowUtil.getResourceOid(object.asObjectable()), shadowName, objectDeltaType, result);
		return acase.getOid();
	}

	@Override
	protected String createTicketModify(ObjectClassComplexTypeDefinition objectClass,
			PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, String resourceOid, Collection<Operation> changes,
			OperationResult result) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		LOGGER.debug("Creating case to modify account {}:\n{}", identifiers, DebugUtil.debugDump(changes, 1));
		if (InternalsConfig.isSanityChecks()) {
			if (MiscUtil.hasDuplicates(changes)) {
				throw new SchemaException("Duplicated changes: "+changes);
			}
		}
		Collection<ItemDelta> changeDeltas = changes.stream()
				.filter(change -> change != null)
				.map(change -> ((PropertyModificationOperation)change).getPropertyDelta())
				.collect(Collectors.toList());
		ObjectDelta<? extends ShadowType> objectDelta = ObjectDelta.createModifyDelta("", changeDeltas, ShadowType.class, getPrismContext());
		ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);
		objectDeltaType.setOid(shadow.getOid());
		String shadowName = getShadowIdentifier(identifiers);
		String description = "Please modify resource account: "+shadowName;
		PrismObject<CaseType> acase = addCase(description, resourceOid, shadow.getOid(), objectDeltaType, result);
		return acase.getOid();
	}

	@Override
	protected String createTicketDelete(ObjectClassComplexTypeDefinition objectClass,
			PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, String resourceOid, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
			SchemaException, ConfigurationException {
		LOGGER.debug("Creating case to delete account {}", identifiers);
		String shadowName = getShadowIdentifier(identifiers);
		String description = "Please delete resource account: "+shadowName;
		ObjectDeltaType objectDeltaType = new ObjectDeltaType();
		objectDeltaType.setChangeType(ChangeTypeType.DELETE);
		objectDeltaType.setObjectType(ShadowType.COMPLEX_TYPE);
		ItemDeltaType itemDeltaType = new ItemDeltaType();
		itemDeltaType.setPath(new ItemPathType("kind"));
		itemDeltaType.setModificationType(ModificationTypeType.DELETE);
		objectDeltaType.setOid(shadow.getOid());

		objectDeltaType.getItemDelta().add(itemDeltaType);
		PrismObject<CaseType> acase;
		try {
			acase = addCase(description, resourceOid, shadow.getOid(), objectDeltaType, result);
		} catch (ObjectAlreadyExistsException e) {
			// should not happen
			throw new SystemException(e.getMessage(), e);
		}
		return acase.getOid();
	}
	
	private PrismObject<CaseType> addCase(String description, String resourceOid, String shadowOid, ObjectDeltaType objectDelta, OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
		PrismObject<CaseType> acase = getPrismContext().createObject(CaseType.class);
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

		PrismObject<ResourceType> resource;
		try {
			resource = repositoryService.getObject(ResourceType.class, resourceOid, null, result);
		} catch (ObjectNotFoundException e) {
			// We do not signal this as ObjectNotFoundException as it could be misinterpreted as "shadow
			// object not found" with subsequent handling as such.
			throw new SystemException("Resource " + resourceOid + " couldn't be found", e);
		}
		ResourceBusinessConfigurationType businessConfiguration = resource.asObjectable().getBusiness();
		List<ObjectReferenceType> operators = new ArrayList<>();
		if (businessConfiguration != null) {
			operators.addAll(businessConfiguration.getOperatorRef());
		}
		if (operators.isEmpty()) {
			operators.add(new ObjectReferenceType().oid(DEFAULT_OPERATOR_OID).type(UserType.COMPLEX_TYPE));
		}

		String caseOid = OidUtil.generateOid();
		
		caseType.setOid(caseOid);
		// TODO: human-readable case ID
		caseType.setName(new PolyStringType(caseOid));

		caseType.setDescription(description);
		
		// subtype
		caseType.setState(SchemaConstants.CASE_STATE_OPEN);

		caseType.setObjectRef(new ObjectReferenceType().oid(resourceOid).type(ResourceType.COMPLEX_TYPE));
		caseType.setTargetRef(new ObjectReferenceType().oid(shadowOid).type(ShadowType.COMPLEX_TYPE));

		if (objectDelta != null) {
			caseType.setObjectChange(objectDelta);
		}

		for (ObjectReferenceType operator : operators) {
			CaseWorkItemType workItem = new CaseWorkItemType(getPrismContext())
					.originalAssigneeRef(operator.clone())
					.assigneeRef(operator.clone())
					.name(caseType.getName().getOrig());
			caseType.getWorkItem().add(workItem);
			// TODO deadline and maybe other fields
		}

		caseType.beginMetadata().setCreateTimestamp(clock.currentTimeXMLGregorianCalendar());

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
	public OperationResultStatus queryOperationStatus(String asynchronousOperationReference, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createMinorSubresult(OPERATION_QUERY_CASE);
		
		PrismObject<CaseType> acase;
		try {
			acase = repositoryService.getObject(CaseType.class, asynchronousOperationReference, null, result);
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

	private String getShadowIdentifier(Collection<? extends ResourceAttribute<?>> identifiers){
		try {
			Object[] shadowIdentifiers = identifiers.toArray();
			return ((ResourceAttribute)shadowIdentifiers[0]).getValue().getValue().toString();
		} catch (NullPointerException e){
			return "";
		}
	}

	@Override
	public void test(OperationResult parentResult) {
		OperationResult connectionResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
		connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ManualConnectorInstance.class);
		connectionResult.addContext("connector", getConnectorObject());
		
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
