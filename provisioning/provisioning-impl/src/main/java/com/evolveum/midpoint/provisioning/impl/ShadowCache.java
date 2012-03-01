/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.consistency.impl.ErrorHandlerFactory;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType.EnableDisable;
import org.apache.commons.lang.Validate;
import org.apache.cxf.bus.spring.OldSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 * This class manages the "cache" of ResourceObjectShadows in the repository.
 * <p/>
 * In short, this class takes care of aligning the shadow objects in repository
 * with the real state of the resource.
 * <p/>
 * The repository content is considered a "cache" when it comes to Shadow
 * objects. That's why they are called "shadow" objects after all. When a new
 * state (values) of the resource object is detected, the shadow in the
 * repository should be updated. No matter if that was detected by
 * synchronization, reconciliation or an ordinary get from resource. This class
 * is supposed to do that.
 * <p/>
 * Therefore all operations that deal with "shadows" should pass through this
 * class. It forms yet another layer of the provisioning subsystem.
 * <p/>
 * Current implementation assumes we are only storing primary identifier in the
 * repository. That should be made configurable later. It also only support
 * Account objects now.
 * 
 * @author Radovan Semancik
 */
@Component
public class ShadowCache {

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	@Autowired
	private ShadowConverter shadowConverter;
	@Autowired
	private ErrorHandlerFactory errorHandlerFactory;

	private static final Trace LOGGER = TraceManager.getTrace(ShadowCache.class);

	public ShadowCache() {
		repositoryService = null;
	}

	/**
	 * Get the value of repositoryService.
	 * 
	 * @return the value of repositoryService
	 */
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	/**
	 * Set the value of repositoryService
	 * <p/>
	 * Expected to be injected.
	 * 
	 * @param repositoryService
	 *            new value of repositoryService
	 */
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	/**
	 * Gets the shadow with specified OID
	 * <p/>
	 * The shadow will be read from the repository and missing information will
	 * be fetched from the resource.
	 * <p/>
	 * If no repositoryShadow is specified, the shadow will be retrieved from
	 * the repository. This is just an optimization if the object was already
	 * fetched (which is a usual case).
	 * <p/>
	 * This method is using identification by OID. This is intended for normal
	 * usage. Method that uses native identification will be provided later.
	 * 
	 * @param oid
	 *            OID of shadow to get.
	 * @param repositoryShadow
	 *            shadow that was read from the repository
	 * @return retrieved shadow (merged attributes from repository and resource)
	 * @throws ObjectNotFoundException
	 *             shadow was not found or object was not found on the resource
	 * @throws CommunicationException
	 *             problem communicating with the resource
	 * @throws SchemaException
	 *             problem processing schema or schema violation
	 */
	public <T extends ResourceObjectShadowType> T getShadow(Class<T> type, String oid, T repositoryShadow,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			SchemaException {

		Validate.notNull(oid, "Object id must not be null.");

		LOGGER.trace("Start getting object with oid {}", oid);

		// We are using parent result directly, not creating subresult.
		// We want to hide the existence of shadow cache from the user.

		// Get the shadow from repository. There are identifiers that we need
		// for accessing the object by UCF.
		// Later, the repository object may have a fully cached object from.
		if (repositoryShadow == null) {
			 PrismObject<T> repositoryPrism = getRepositoryService().getObject(type, oid, null,
					parentResult);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Found shadow object:\n{}", repositoryPrism.dump());
			}
			repositoryShadow = repositoryPrism.asObjectable();
		}

		// Sanity check
		if (!oid.equals(repositoryShadow.getOid())) {
			parentResult.recordFatalError("Provided OID is not equal to OID of repository shadow");
			throw new IllegalArgumentException("Provided OID is not equal to OID of repository shadow");
		}

		ResourceType resource = getResource(ResourceObjectShadowUtil.getResourceOid(repositoryShadow),
				parentResult);

		LOGGER.trace("Getting fresh object from ucf.");

		T resultShadow = null;
		try {
			resultShadow = shadowConverter.getShadow(type, resource, repositoryShadow, parentResult);
		} catch (ObjectNotFoundException ex) {
			// TODO: Discovery
			parentResult.recordFatalError("Object " + ObjectTypeUtil.toShortString(repositoryShadow)
					+ "not found on the " + ObjectTypeUtil.toShortString(resource), ex);
			// throw new ObjectNotFoundException("Object " +
			// ObjectTypeUtil.toShortString(repositoryShadow) +
			// " not found on the Resource "
			// + ObjectTypeUtil.toShortString(resource), ex);
			throw ex;
		} catch (CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector. Reason: " + ex.getMessage(), ex);
			// throw new
			// CommunicationException("Error communicating with the connector",
			// ex);
			throw ex;
		}
		parentResult.recordSuccess();
		return resultShadow;

	}

	public String addShadow(ResourceObjectShadowType shadow, ScriptsType scripts, ResourceType resource,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {

		Validate.notNull(shadow, "Object to add must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start adding shadow object:\n{}", shadow.asPrismObject().dump());
			LOGGER.trace("Scripts: {}", SchemaDebugUtil.dumpJaxbObject(scripts, "scripts", shadow.asPrismObject().getPrismContext()));
		}

		if (resource == null) {
			resource = getResource(ResourceObjectShadowUtil.getResourceOid(shadow), parentResult);
		}

		Set<Operation> additionalOperations = new HashSet<Operation>();

		// Check for password
		if (shadow instanceof AccountShadowType) {
			AccountShadowType account = (AccountShadowType) shadow;
			if (account.getCredentials() != null && account.getCredentials().getPassword() != null) {
				PasswordType password = account.getCredentials().getPassword();
				ProtectedStringType protectedString = password.getProtectedString();
				if (protectedString != null) {
					PasswordChangeOperation passOp = new PasswordChangeOperation(protectedString);
					additionalOperations.add(passOp);
				}
			}

		}
		addExecuteScriptOperation(additionalOperations, OperationTypeType.ADD, scripts, parentResult);

		OperationResult shadowConverterResult = parentResult.createSubresult(ShadowConverter.class.getName() +".addShadow");
		
		try {
			shadow = shadowConverter.addShadow(resource, shadow, additionalOperations, shadowConverterResult);
		} catch (Exception ex) {
			
			ErrorHandler handler = errorHandlerFactory.createErrorHandler(ex);
			shadow.setFailedOperationType(FailedOperationTypeType.ADD);
			shadow.setResult(shadowConverterResult.createOperationResultType());
			shadow.setResource(resource);
			handler.handleError(shadow, ex);
			
		}
		// } catch (CommunicationException ex) {
		// parentResult.recordFatalError("Error communicating with connector. Reason: "
		// + ex.getMessage(),
		// ex);
		// throw ex;
		// } catch (SchemaException ex) {
		// parentResult.recordFatalError(ex.getMessage(), ex);
		// throw ex;
		// } catch (ObjectAlreadyExistsException ex) {
		// parentResult.recordFatalError(
		// "Object " + ObjectTypeUtil.toShortString(shadow) + "already exist.",
		// ex);
		// throw ex;
		// }

		if (shadow == null) {
			parentResult
					.recordFatalError("Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
			throw new IllegalStateException(
					"Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
		}

		LOGGER.trace("Adding object with identifiers to the repository.");

		addShadowToRepository(shadow, parentResult);

		LOGGER.trace("Object added to the repository successfully.");

		parentResult.recordSuccess();
		return shadow.getOid();

	}

	public void deleteShadow(ObjectType objectType, ScriptsType scripts, ResourceType resource,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			ObjectNotFoundException, SchemaException {

		Validate.notNull(objectType, "Object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		if (objectType instanceof AccountShadowType) {

			AccountShadowType accountShadow = (AccountShadowType) objectType;

			if (resource == null) {
				resource = getResource(ResourceObjectShadowUtil.getResourceOid(accountShadow), parentResult);
			}

			LOGGER.trace("Deleting obeject {} from the resource {}.",
					ObjectTypeUtil.toShortString(objectType), ObjectTypeUtil.toShortString(resource));

			Set<Operation> additionalOperations = new HashSet<Operation>();

			addExecuteScriptOperation(additionalOperations, OperationTypeType.DELETE, scripts, parentResult);

			try {
				shadowConverter.deleteShadow(resource, accountShadow, additionalOperations, parentResult);
			} catch (Exception ex) {
				ErrorHandler handler = errorHandlerFactory.createErrorHandler(ex);
				accountShadow.setFailedOperationType(FailedOperationTypeType.DELETE);
				accountShadow.setResult(parentResult.createOperationResultType());
				accountShadow.setResource(resource);
				try {
					handler.handleError(accountShadow, ex);
				} catch (ObjectAlreadyExistsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
			// } catch (CommunicationException ex) {
			// parentResult.recordFatalError(
			// "Error communicating with connector. Reason: " + ex.getMessage(),
			// ex);
			// throw ex;
			// } catch (SchemaException ex) {
			// parentResult.recordFatalError(ex.getMessage(), ex);
			// throw ex;
			// } catch (ObjectNotFoundException ex) {
			// parentResult.recordFatalError(
			// "Object with identifiers " +
			// ObjectTypeUtil.toShortString(accountShadow)
			// + " can't be deleted. Reason: " + ex.getMessage(), ex);
			// throw ex;
			// }

			LOGGER.trace("Detele object with oid {} form repository.", accountShadow.getOid());
			try {
				getRepositoryService().deleteObject(AccountShadowType.class, accountShadow.getOid(),
						parentResult);
			} catch (ObjectNotFoundException ex) {
				parentResult.recordFatalError(
						"Can't delete object " + ObjectTypeUtil.toShortString(accountShadow) + ". Reason: "
								+ ex.getMessage(), ex);
				throw new ObjectNotFoundException("An error occured while deleting resource object "
						+ accountShadow + "whith identifiers " + ObjectTypeUtil.toShortString(accountShadow)
						+ ": " + ex.getMessage(), ex);
			}
			LOGGER.trace("Object deleted from repository successfully.");
			parentResult.recordSuccess();
		}
	}

	public void modifyShadow(ObjectType objectType, ResourceType resource, String oid,
			Collection<? extends ItemDelta> modifications, ScriptsType scripts, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
			SchemaException {

		Validate.notNull(objectType, "Object to modify must not be null.");
		Validate.notNull(oid, "OID must not be null.");
		Validate.notNull(modifications, "Object modification must not be null.");

		if (objectType instanceof ResourceObjectShadowType) {
			ResourceObjectShadowType shadow = (ResourceObjectShadowType) objectType;
			if (resource == null) {
				resource = getResource(ResourceObjectShadowUtil.getResourceOid(shadow), parentResult);

			}
			
			ResourceAttributeContainerDefinition objectClassDefinition = ResourceObjectShadowUtil.getObjectClassDefinition(shadow);;

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Modifying resource with oid {}, object:\n{}", resource.getOid(), 
						shadow.asPrismObject().dump());
			}

			Set<Operation> changes = new HashSet<Operation>();
			if (shadow instanceof AccountShadowType) {

				// Look for password change
				PasswordChangeOperation passwordChangeOp = determinePasswordChange(modifications, shadow);
				if (passwordChangeOp != null) {
					changes.add(passwordChangeOp);
				}
				
				// look for activation change
				Operation activationOperation = determineActivationChange(modifications, resource, objectClassDefinition);
				if (activationOperation != null) {
					changes.add(activationOperation);
				}
			}

			addExecuteScriptOperation(changes, OperationTypeType.MODIFY, scripts, parentResult);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Applying change: {}", DebugUtil.debugDump(modifications));
			}

			Set<PropertyModificationOperation> sideEffectChanges = null;
			try {
				sideEffectChanges = shadowConverter.modifyShadow(resource, shadow, changes, oid, modifications,
						parentResult);
			} catch (Exception ex) {
				ErrorHandler handler = errorHandlerFactory.createErrorHandler(ex);
				shadow.setFailedOperationType(FailedOperationTypeType.MODIFY);
				shadow.setResult(parentResult.createOperationResultType());
				shadow.setResource(resource);
				ObjectChangeModificationType objectChangeType = new ObjectChangeModificationType();
				ObjectModificationType omt = new ObjectModificationType();
				omt.setOid(shadow.getOid());
				for (ItemDelta itemDelta : modifications){
					omt.getPropertyModification().addAll(DeltaConvertor.toPropertyModificationTypes(itemDelta));
				}
				objectChangeType.setObjectModification(omt);	
				shadow.setObjectChange(objectChangeType);
				try {
					handler.handleError(shadow, ex);
				} catch (ObjectAlreadyExistsException e) {
				}
				return;
			}
			// } catch (ObjectNotFoundException ex) {
			// parentResult.recordFatalError(
			// "Object with identifiers " + ObjectTypeUtil.toShortString(shadow)
			// + " can't be modified. Reason: " + ex.getMessage(), ex);
			// throw ex;
			// }
			if (!sideEffectChanges.isEmpty()) {
				// TODO: implement
				throw new UnsupportedOperationException(
						"Handling of side-effect changes is not yet supported");
			}

			parentResult.recordSuccess();
		}
	}

	public PrismProperty fetchCurrentToken(ResourceType resourceType, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.trace("Getting last token");
		PrismProperty lastToken = null;
		try {
			lastToken = shadowConverter.fetchCurrentToken(resourceType, parentResult);
		} catch (CommunicationException e) {
			parentResult.recordFatalError(e.getMessage(), e);
			throw e;

		}

		LOGGER.trace("Got last token: {}", SchemaDebugUtil.prettyPrint(lastToken));
		parentResult.recordSuccess();
		return lastToken;
	}

	public List<Change> fetchChanges(ResourceType resourceType, PrismProperty lastToken,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException, ConfigurationException {

		List<Change> changes = null;
		try {
			// changes = connector.fetchChanges(objectClass, lastToken,
			// parentResult);

			changes = shadowConverter.fetchChanges(resourceType, lastToken, parentResult);

			for (Iterator<Change> i = changes.iterator(); i.hasNext();) {
				// search objects in repository
				Change change = i.next();
				// try {

				ResourceObjectShadowType newShadow = findOrCreateShadowFromChange(resourceType, change,
						parentResult);

				// if (change.getObjectDelta() != null &&
				// change.getObjectDelta().getChangeType()==ChangeType.DELETE &&
				// newShadow == null){
				//

				change.setOldShadow(newShadow.asPrismObject());

				// skip setting other attribute when shadow is null
				if (newShadow == null) {
					continue;
				}

				// FIXME: hack. make sure that the current shadow has OID
				// and resource ref, also the account type should be set
				ResourceObjectShadowType currentShadowType = change.getCurrentShadow().asObjectable();
				if (currentShadowType != null) {
					currentShadowType.setOid(newShadow.getOid());
					currentShadowType.setResourceRef(newShadow.getResourceRef());
					if (currentShadowType instanceof AccountShadowType
							&& newShadow instanceof AccountShadowType) {
						((AccountShadowType) currentShadowType)
								.setAccountType(((AccountShadowType) newShadow).getAccountType());
					}
				}
				// FIXME: hack. the object delta must have oid specified.
				if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
					if (newShadow instanceof AccountShadowType) {
						ObjectDelta<AccountShadowType> objDelta = new ObjectDelta<AccountShadowType>(
								AccountShadowType.class, ChangeType.DELETE);
						change.setObjectDelta(objDelta);
					}
					change.getObjectDelta().setOid(newShadow.getOid());
				}
				// } catch (ObjectNotFoundException ex) {
				// parentResult
				// .recordPartialError("Couldn't find object defined in change. Skipping processing this change.");
				// i.remove();
				// }

			}

		} catch (SchemaException ex) {
			parentResult.recordFatalError("Schema error: " + ex.getMessage(), ex);
			throw ex;
		} catch (CommunicationException ex) {
			parentResult.recordFatalError("Communication error: " + ex.getMessage(), ex);
			throw ex;
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error: " + ex.getMessage(), ex);
			throw ex;
		} catch (ConfigurationException ex) {
			parentResult.recordFatalError("Configuration error: " + ex.getMessage(), ex);
			throw ex;
		}
		parentResult.recordSuccess();
		return changes;
	}

	private void addExecuteScriptOperation(Set<Operation> operations, OperationTypeType type,
			ScriptsType scripts, OperationResult result) throws SchemaException {
		if (scripts == null) {
			// No warning needed, this is quite normal
			// result.recordWarning("Skiping creating script operation to execute. Scripts was not defined.");
			LOGGER.trace("Skiping creating script operation to execute. Scripts was not defined.");
			return;
		}

		for (ScriptType script : scripts.getScript()) {
			for (OperationTypeType operationType : script.getOperation()) {
				if (type.equals(operationType)) {
					ExecuteScriptOperation scriptOperation = new ExecuteScriptOperation();

					for (ScriptArgumentType argument : script.getArgument()) {
						ExecuteScriptArgument arg = new ExecuteScriptArgument(argument.getName(),
								ValueConstruction.getStaticValueList(argument));
						scriptOperation.getArgument().add(arg);
					}

					scriptOperation.setLanguage(script.getLanguage());
					scriptOperation.setTextCode(script.getCode());

					scriptOperation.setScriptOrder(script.getOrder());

					if (script.getHost().equals(ScriptHostType.CONNECTOR)) {
						scriptOperation.setConnectorHost(true);
						scriptOperation.setResourceHost(false);
					}
					if (script.getHost().equals(ScriptHostType.RESOURCE)) {
						scriptOperation.setConnectorHost(false);
						scriptOperation.setResourceHost(true);
					}
					LOGGER.trace("Created script operation: {}", SchemaDebugUtil.prettyPrint(scriptOperation));
					operations.add(scriptOperation);
				}
			}
		}
	}

	private PropertyModificationOperation convertToActivationAttribute(ResourceType resource, Boolean enabled, 
			ResourceAttributeContainerDefinition objectClassDefinition) throws SchemaException {
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		if (activationCapability == null) {
			throw new SchemaException("Resource " + ObjectTypeUtil.toShortString(resource)
					+ " does not have native or simulated activation capability");
		}
		
		EnableDisable enableDisable = activationCapability.getEnableDisable();
		if (enableDisable == null) {
			throw new SchemaException("Resource " + ObjectTypeUtil.toShortString(resource)
					+ " does not have native or simulated activation/enableDisable capability");
		}

		QName enableAttributeName = enableDisable.getAttribute();
		if (enableAttributeName == null) {
			throw new SchemaException(
					"Resource " + ObjectTypeUtil.toShortString(resource)
							+ " does not have attribute specification for simulated activation/enableDisable capability");
		}
		
		ResourceAttributeDefinition enableAttributeDefinition = objectClassDefinition.findAttributeDefinition(enableAttributeName);
		if (enableAttributeDefinition == null) {
			throw new SchemaException(
					"Resource " + ObjectTypeUtil.toShortString(resource)
							+ "  attribute for simulated activation/enableDisable capability" + enableAttributeName +
							" in not present in the schema for objeclass " + objectClassDefinition);
		}
		
		PropertyDelta enableAttributeDelta 
			= new PropertyDelta(new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES, enableAttributeName),
					enableAttributeDefinition);
		
		List<String> enableValues = enableDisable.getEnableValue();

		Iterator<String> i = enableValues.iterator();
		String enableValue = i.next();
		if ("".equals(enableValue)) {
			if (enableValues.size() < 2) {
				enableValue = "false";
			} else {
				enableValue = i.next();
			}
		}
		String disableValue = enableDisable.getDisableValue().iterator().next();
		if (enabled) {
			enableAttributeDelta.setValueToReplace(new PrismPropertyValue(enableValue));
		} else {
			enableAttributeDelta.setValueToReplace(new PrismPropertyValue(disableValue));
		}
		
		PropertyModificationOperation attributeChange = new PropertyModificationOperation(enableAttributeDelta);
		return attributeChange;
	}

	private Operation determineActivationChange(Collection<? extends ItemDelta> objectChange, ResourceType resource,
			ResourceAttributeContainerDefinition objectClassDefinition) throws SchemaException {
		
		PropertyDelta<Boolean> enabledPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				new PropertyPath(ResourceObjectShadowType.F_ACTIVATION, ActivationType.F_ENABLED));
		if (enabledPropertyDelta == null) {
			return null;
		}
		Boolean enabled = enabledPropertyDelta.getPropertyNew().getRealValue();
		LOGGER.trace("Find activation change to: {}", enabled);

		if (enabled != null) {

			LOGGER.trace("enabled not null.");
			if (!ResourceTypeUtil.hasResourceNativeActivationCapability(resource)) {
				// if resource cannot do activation, resource should
				// have specified policies to do that
				PropertyModificationOperation activationAttribute = convertToActivationAttribute(resource,
						enabled, objectClassDefinition);
				// changes.add(activationAttribute);
				return activationAttribute;
			} else {
				// Navive activation, nothing special to do
				return new PropertyModificationOperation(enabledPropertyDelta);
			}

		}
		return null;
	}

	private PasswordChangeOperation determinePasswordChange(Collection<? extends ItemDelta> objectChange,
			ResourceObjectShadowType objectType) throws SchemaException {
		// Look for password change
		
		PropertyDelta<PasswordType> passwordPropertyDelta = PropertyDelta.findPropertyDelta(objectChange,
				new PropertyPath(AccountShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD));
		if (passwordPropertyDelta == null) {
			return null;
		}
		PasswordType newPasswordStructure = passwordPropertyDelta.getPropertyNew().getRealValue();
		
		PasswordChangeOperation passwordChangeOp = null;
		if (newPasswordStructure != null) {
			ProtectedStringType newPasswordPS = newPasswordStructure.getProtectedString();
			if (MiscSchemaUtil.isNullOrEmpty(newPasswordPS)) {
				throw new IllegalArgumentException(
						"ProtectedString is empty in an attempt to change password of "
								+ ObjectTypeUtil.toShortString(objectType));
			}
			passwordChangeOp = new PasswordChangeOperation(newPasswordPS);
			// TODO: other things from the structure
			// changes.add(passwordChangeOp);
		}
		return passwordChangeOp;
	}

	private ResourceObjectShadowType findOrCreateShadowFromChange(ResourceType resource, Change change,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
			CommunicationException, GenericFrameworkException {

		// Try to locate existing shadow in the repository
		List<PrismObject<AccountShadowType>> accountList = searchAccountByIdenifiers(change, parentResult);

		if (accountList.size() > 1) {
			String message = "Found more than one account with the identifier " + change.getIdentifiers()
					+ ".";
			LOGGER.error(message);
			parentResult.recordFatalError(message);
			throw new IllegalArgumentException(message);
		}

		ResourceObjectShadowType newShadow = null;

		if (accountList.isEmpty()) {
			// account was not found in the repository, create it now

			if (change.getObjectDelta() == null
					|| !(change.getObjectDelta().getChangeType() == ChangeType.DELETE)) {
				try {
					newShadow = shadowConverter.createNewAccountFromChange(change, resource, parentResult);
				} catch (ObjectNotFoundException ex) {
					throw ex;
				}

				try {
					addShadowToRepository(newShadow, parentResult);
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError("Can't add account " + SchemaDebugUtil.prettyPrint(newShadow)
							+ " to the repository. Reason: " + e.getMessage(), e);
					throw new IllegalStateException(e.getMessage(), e);
				}
				LOGGER.trace("Created account shadow object: {}", ObjectTypeUtil.toShortString(newShadow));
			}

		} else {
			// Account was found in repository

			newShadow = accountList.get(0).asObjectable();
			// if the fetched change was one of the deletion type, delete
			// corresponding account from repo now
			// if (change.getObjectDelta() != null
			// && change.getObjectDelta().getChangeType() == ChangeType.DELETE)
			// {
			// try {
			// getRepositoryService().deleteObject(AccountShadowType.class,
			// newShadow.getOid(),
			// parentResult);
			// } catch (ObjectNotFoundException ex) {
			// parentResult.recordFatalError("Object with oid " +
			// newShadow.getOid()
			// + " not found in repo. Reason: " + ex.getMessage(), ex);
			// throw new ObjectNotFoundException("Object with oid " +
			// newShadow.getOid()
			// + " not found in repo. Reason: " + ex.getMessage(), ex);
			// }
			// }
		}

		return newShadow;
	}

	private List<PrismObject<AccountShadowType>> searchAccountByIdenifiers(Change change, OperationResult parentResult)
			throws SchemaException {

		QueryType query = ShadowCacheUtil.createSearchShadowQuery(change.getIdentifiers(), parentResult);

		List<PrismObject<AccountShadowType>> accountList = null;
		try {
			accountList = getRepositoryService().searchObjects(AccountShadowType.class, query,
					new PagingType(), parentResult);
		} catch (SchemaException ex) {
			parentResult.recordFatalError(
					"Failed to search account according to the identifiers: " + change.getIdentifiers()
							+ ". Reason: " + ex.getMessage(), ex);
			throw new SchemaException("Failed to search account according to the identifiers: "
					+ change.getIdentifiers() + ". Reason: " + ex.getMessage(), ex);
		}
		return accountList;
	}

	private ResourceType getResource(String oid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		// TODO: add some caching
		PrismObject<ResourceType> resource = getRepositoryService().getObject(ResourceType.class, oid, null, parentResult);
		// return resource;
		return shadowConverter.completeResource(resource.asObjectable(), parentResult);
	}

	private void addShadowToRepository(ResourceObjectShadowType shadow, OperationResult parentResult)
			throws SchemaException, ObjectAlreadyExistsException {

		// Store shadow in the repository
		String oid = null;
		try {

			oid = getRepositoryService().addObject(shadow.asPrismObject(), parentResult);

		} catch (ObjectAlreadyExistsException ex) {
			// This should not happen. The OID is not supplied and it is
			// generated by the repo
			// If it happens, it must be a repo bug. Therefore it is safe to
			// convert to runtime exception
			parentResult.recordFatalError(
					"Can't add shadow object to the repository. Shadow object already exist. Reason: "
							+ ex.getMessage(), ex);
			throw new ObjectAlreadyExistsException(
					"Can't add shadow object to the repository. Shadow object already exist. Reason: "
							+ ex.getMessage(), ex);
		}
		shadow.setOid(oid);
	}

}
