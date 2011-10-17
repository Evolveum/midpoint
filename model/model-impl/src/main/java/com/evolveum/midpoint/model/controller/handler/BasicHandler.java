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
package com.evolveum.midpoint.model.controller.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.controller.ModelControllerImpl;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.model.controller.SchemaHandler;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommonException;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.patch.PatchException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.PasswordCapabilityType;

/**
 * 
 * @author lazyman
 * 
 */
public class BasicHandler {

	private static final Trace LOGGER = TraceManager.getTrace(BasicHandler.class);
	private ModelController modelController;
	private ProvisioningService provisioning;
	private RepositoryService repository;
	private SchemaHandler schemaHandler;

	public BasicHandler(ModelController modelController, ProvisioningService provisioning,
			RepositoryService repository, SchemaHandler schemaHandler) {
		this.modelController = modelController;
		this.provisioning = provisioning;
		this.repository = repository;
		this.schemaHandler = schemaHandler;
	}

	protected ModelController getModelController() {
		return modelController;
	}

	protected ProvisioningService getProvisioning() {
		return provisioning;
	}

	protected RepositoryService getRepository() {
		return repository;
	}

	protected SchemaHandler getSchemaHandler() {
		return schemaHandler;
	}

	protected ResourceType resolveResource(ResourceObjectShadowType shadow, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notNull(shadow, "Resource object shadow must not be null.");

		ResourceType resource = shadow.getResource();
		if (resource == null && shadow.getResourceRef() != null) {
			resource = getObject(ResourceType.class, shadow.getResourceRef().getOid(),
					new PropertyReferenceListType(), result);
		}

		if (resource == null) {
			throw new SystemException("Couldn't get resource from shadow '" + shadow.getName()
					+ "', resource and resourceRef couldn't be resolved.");
		}

		return resource;
	}

	protected SystemConfigurationType getSystemConfiguration(OperationResult result)
			throws ObjectNotFoundException {
		OperationResult configResult = result.createSubresult(ModelControllerImpl.GET_SYSTEM_CONFIGURATION);
		SystemConfigurationType systemConfiguration = null;
		try {
			systemConfiguration = getModelController().getObject(SystemConfigurationType.class,
					SystemObjectsType.SYSTEM_CONFIGURATION.value(),
					ModelUtils.createPropertyReferenceListType("defaultUserTemplate"), result);
			configResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			configResult.recordFatalError("Couldn't get system configuration.", ex);
			throw ex;
		}

		return systemConfiguration;
	}

	protected UserType processUserTemplateForUser(UserType user, UserTemplateType userTemplate,
			Collection<String> excludedResourceOids, OperationResult result) {
		OperationResult subResult = result.createSubresult(ModelControllerImpl.PROCESS_USER_TEMPLATE);
		subResult.addParams(new String[] { "user", "userTemplate" }, user, userTemplate);
		if (userTemplate == null) {
			subResult.recordSuccess();
			return user;
		}

		try {
			user = schemaHandler.processPropertyConstructions(user, userTemplate, subResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER,
					"Couldn't process property construction from template {} on user {}", ex,
					userTemplate.getName(), user.getName());
		}
		processUserTemplateAccountConstruction(user, userTemplate, excludedResourceOids, subResult);
		subResult.computeStatus("Couldn't finish process user template.");

		// The user is now polluted with both accountRef and account elements. Get rid of the accounts, leave just accountRefs.
		user.getAccount().clear();
		
		return user;
	}

	private void processUserTemplateAccountConstruction(UserType user, UserTemplateType userTemplate,
			Collection<String> excludedResourceOids, OperationResult result) {
		for (AccountConstructionType construction : userTemplate.getAccountConstruction()) {
			processAccountConstruction(user, construction, userTemplate, excludedResourceOids, result);
		}
	}

	protected ObjectReferenceType processAccountConstruction(UserType user, AccountConstructionType construction, 
			ObjectType containingObject, Collection<String> excludedResourceOids, OperationResult result) {
		
		ObjectReferenceType resourceRef = construction.getResourceRef();
		
		if (excludedResourceOids != null && excludedResourceOids.contains(resourceRef.getOid())) {
			LOGGER.trace("Resource "+resourceRef.getOid()+" excluded, skipping");
			return null;
		}
		
		for (AccountShadowType account: user.getAccount()) {
			if (account.getResourceRef().getOid().equals(resourceRef.getOid())) {
				LOGGER.trace("Account on resource "+resourceRef.getOid()+" already exists for "+ObjectTypeUtil.toShortString(user));
				return null;
			}
		}
		
		OperationResult subResult = result.createSubresult(ModelControllerImpl.CREATE_ACCOUNT);
		subResult.addParams(new String[] { "user" }, user);
		subResult.addParam("exclusions", excludedResourceOids);
		
		ResourceType resource;
		try {
			resource = getObject(ResourceType.class, resourceRef.getOid(),
					new PropertyReferenceListType(), result);
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logException(LOGGER, "Cannot find resource (OID:{}) in account construction in {}", e,
					resourceRef.getOid(), ObjectTypeUtil.toShortString(containingObject));
			subResult.recordFatalError("Cannot find resource (OID:"+resourceRef.getOid()+") in account construction in " + 
					ObjectTypeUtil.toShortString(containingObject), e);
			return null;
		}

		AccountType accountType = ModelUtils.getAccountTypeFromHandling(construction.getType(), resource);

		AccountShadowType account = new AccountShadowType();
		account.setAttributes(new ResourceObjectShadowType.Attributes());
		account.setObjectClass(accountType.getObjectClass());
		// Should work without this, provisioning should create it
//		account.setName(resource.getName() + "-" + user.getName());
		account.setResourceRef(resourceRef);
		account.setActivation(user.getActivation());
		
		try {
			
			pushPasswordFromUserToAccount(user, account, result);
			
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logException(LOGGER, "Cannot find resource (OID:{}) in account construction in {}", e,
					account.getResourceRef().getOid(), ObjectTypeUtil.toShortString(containingObject));
			subResult.recordFatalError("Cannot find resource (OID:"+account.getResourceRef().getOid()+") in account construction in " + 
					ObjectTypeUtil.toShortString(containingObject), e);
			return null;
		}

		ObjectModificationType changes = processOutboundSchemaHandling(user, account, result);
		if (changes != null) {
			PatchXml patchXml = new PatchXml();
			String accountXml;
			try {
				accountXml = patchXml.applyDifferences(changes, account);
			} catch (PatchException e) {
				LoggingUtils.logException(LOGGER, "Cannot apply changes while processing account construction in {}", e,
						ObjectTypeUtil.toShortString(containingObject));
				subResult.recordFatalError("Cannot apply changes while processing account construction in " + 
						ObjectTypeUtil.toShortString(containingObject), e);
				return null;
			}
			try {
				account = JAXBUtil.unmarshal(AccountShadowType.class, accountXml).getValue();
			} catch (JAXBException e) {
				LoggingUtils.logException(LOGGER, "Error after aplying changes while processing account construction in {}", e,
						ObjectTypeUtil.toShortString(containingObject));
				subResult.recordFatalError("Cannot apply changes while processing account construction in " + 
						ObjectTypeUtil.toShortString(containingObject), e);
				return null;
			}
		}

		String accountOid;
		try {
			accountOid = getModelController().addObject(account, result);
		} catch (ObjectAlreadyExistsException e) {
			LoggingUtils.logException(LOGGER, "Error creating account based on account construction in {}", e,
					ObjectTypeUtil.toShortString(containingObject));
			subResult.recordFatalError("Error creating account based on account construction in " + 
					ObjectTypeUtil.toShortString(containingObject), e);
			return null;
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logException(LOGGER, "Error creating account based on account construction in {}", e,
					ObjectTypeUtil.toShortString(containingObject));
			subResult.recordFatalError("Error creating account based on account construction in " + 
					ObjectTypeUtil.toShortString(containingObject), e);
			return null;
		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "Error creating account based on account construction in {}", e,
					ObjectTypeUtil.toShortString(containingObject));
			subResult.recordFatalError("Error creating account based on account construction in " + 
					ObjectTypeUtil.toShortString(containingObject), e);
			return null;
		}

		ObjectReferenceType accountRef = ModelUtils.createReference(accountOid, ObjectTypes.ACCOUNT);
		// Adding the account in the temporary copy of the user, so other
		// invocations of this method will see the accounts and won't create it again
		user.getAccount().add(account);
		user.getAccountRef().add(accountRef);
		
		ObjectModificationType userModification = new ObjectModificationType();
		userModification.setOid(user.getOid());
		userModification.getPropertyModification().add(
				ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.add, null,
						SchemaConstants.I_ACCOUNT_REF, accountRef));
		try {
		
			getRepository().modifyObject(UserType.class, userModification, result);
			
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logException(LOGGER, "Error linking account based on account construction in {}", e,
					ObjectTypeUtil.toShortString(containingObject));
			subResult.recordFatalError("Error creating account based on account construction in " + 
					ObjectTypeUtil.toShortString(containingObject), e);
			return null;
		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "Error linking account based on account construction in {}", e,
					ObjectTypeUtil.toShortString(containingObject));
			subResult.recordFatalError("Error creating account based on account construction in " + 
					ObjectTypeUtil.toShortString(containingObject), e);
			return null;
		}
		
		subResult.recordSuccess();
		return accountRef;
	}

	@SuppressWarnings("unchecked")
	public <T extends ObjectType> T getObject(Class<T> clazz, String oid, PropertyReferenceListType resolve,
			OperationResult result) throws ObjectNotFoundException {
		T object = null;
		try {
			ObjectType objectType = null;
			if (ObjectTypes.isClassManagedByProvisioning(clazz)) {
				objectType = getProvisioning().getObject(clazz, oid, resolve, result);
			} else {
				objectType = getRepository().getObject(clazz, oid, resolve, result);
			}
			if (!clazz.isInstance(objectType)) {
				throw new ObjectNotFoundException("Bad object type returned for referenced oid '" + oid
						+ "'. Expected '" + clazz + "', but was '"
						+ (objectType == null ? "null" : objectType.getClass()) + "'.");
			} else {
				object = (T) objectType;
			}

			resolveObjectAttributes(object, resolve, result);
		} catch (SystemException ex) {
			throw ex;
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (CommonException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object with oid {}", ex, oid);
			// Add to result only a short version of the error, the details will be in subresults
			result.recordFatalError(
					"Couldn't get object with oid '" + oid + "': "+ex.getOperationResultMessage(), ex);
			throw new SystemException("Couldn't get object with oid '" + oid + "'.", ex);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object with oid {}, expected type was {}.", ex,
					oid, clazz);
			throw new SystemException("Couldn't get object with oid '" + oid + "'.", ex);
		} finally {
			result.computeStatus();
			LOGGER.trace(result.dump());
		}

		return object;
	}

	// TODO: change to protected
	public ObjectModificationType processOutboundSchemaHandling(UserType user,
			ResourceObjectShadowType object, OperationResult result) {
		ObjectModificationType change = null;
		if (user != null) {
			try {
				change = getSchemaHandler().processOutboundHandling(user, (ResourceObjectShadowType) object,
						result);
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't process outbound schema handling for {}", ex,
						object.getName());
			}
		} else {
			LOGGER.debug("Skipping outbound schema handling processing for {} (no user defined).",
					new Object[] { object.getName() });
		}

		return change;
	}

	protected void resolveObjectAttributes(ObjectType object, PropertyReferenceListType resolve,
			OperationResult result) {
		if (object == null) {
			return;
		}

		if (object instanceof UserType) {
			resolveUserAttributes((UserType) object, resolve, result);
		} else if (object instanceof AccountShadowType) {
			resolveAccountAttributes((AccountShadowType) object, resolve, result);
		}
	}

	private void resolveUserAttributes(UserType user, PropertyReferenceListType resolve,
			OperationResult result) {
		if (!Utils.haveToResolve("Account", resolve)) {
			return;
		}

		// TODO: error handling needs to be refactored
		
		List<ObjectReferenceType> refToBeDeleted = new ArrayList<ObjectReferenceType>();
		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			OperationResult subResult = result.createSubresult(ModelControllerImpl.RESOLVE_USER_ATTRIBUTES);
			subResult.addParams(new String[] { "user", "accountRef" }, user, accountRef);
			try {
				AccountShadowType account = getObject(AccountShadowType.class, accountRef.getOid(), resolve,
						subResult);
				user.getAccount().add(account);
				refToBeDeleted.add(accountRef);
				subResult.recordSuccess();
			} catch (SystemException ex) {
				// Already processed in getObject, nothing to do
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't resolve account with oid {}", ex,
						accountRef.getOid());
				subResult.recordFatalError(
						"Couldn't resolve account with oid '" + accountRef.getOid() + "': "+ex.getMessage(), ex);
			} finally {
				subResult.computeStatus("Couldn't resolve account with oid '" + accountRef.getOid() + "'.");
			}
		}
		user.getAccountRef().removeAll(refToBeDeleted);
	}

	private void resolveAccountAttributes(AccountShadowType account, PropertyReferenceListType resolve,
			OperationResult result) {
		if (!Utils.haveToResolve("Resource", resolve)) {
			return;
		}

		ObjectReferenceType reference = account.getResourceRef();
		if (reference == null || StringUtils.isEmpty(reference.getOid())) {
			LOGGER.debug("Skipping resolving resource for account {}, resource reference is null or "
					+ "doesn't contain oid.", new Object[] { account.getName() });
			return;
		}
		OperationResult subResult = result.createSubresult(ModelControllerImpl.RESOLVE_ACCOUNT_ATTRIBUTES);
		subResult.addParams(new String[] { "account", "resolve" }, account, resolve);
		try {
			ResourceType resource = getObject(ResourceType.class, account.getResourceRef().getOid(), resolve,
					result);
			account.setResource(resource);
			account.setResourceRef(null);
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils
					.logException(LOGGER, "Couldn't resolve resource with oid {}", ex, reference.getOid());
			subResult
					.recordFatalError("Couldn't resolve resource with oid '" + reference.getOid() + "'.", ex);
		} finally {
			subResult.computeStatus("Couldn't resolve resource with oid '" + reference.getOid() + "'.");
		}
	}

	/**
	 * MID-72, MID-73 password push from user to account
	 */
	protected void pushPasswordFromUserToAccount(UserType user, AccountShadowType account, OperationResult result)
			throws ObjectNotFoundException {
				ResourceType resource = resolveResource(account, result);
				// checking resource password capabilities for password push to account
				CredentialsCapabilityType credentialsCapability = ResourceTypeUtil.getEffectiveCapability(resource,
						CredentialsCapabilityType.class);
				if (credentialsCapability == null) {
					return;
				}
			
				PasswordCapabilityType passwordCapability = credentialsCapability.getPassword();
				if (passwordCapability == null) {
					return;
				}
			
				AccountType accountHandling = ModelUtils.getAccountTypeFromHandling(account, resource);
				boolean pushPasswordToAccount = false;
				// check also if account that is processed to add doesn't have set
				// different password as user -> account.getCredentials() in this case
				// is not null..
				if (accountHandling != null && accountHandling.getCredentials() != null
						&& accountHandling.getCredentials().isOutboundPassword() != null
						&& account.getCredentials() == null) {
					pushPasswordToAccount = accountHandling.getCredentials().isOutboundPassword();
				}
				if (pushPasswordToAccount && user.getCredentials() != null
						&& user.getCredentials().getPassword() != null) {
					CredentialsType credentials = account.getCredentials();
					if (credentials == null) {
						credentials = new CredentialsType();
						account.setCredentials(credentials);
					}
					credentials.setPassword(user.getCredentials().getPassword());
				}
			}
}
