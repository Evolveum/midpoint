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
package com.evolveum.midpoint.model.controller;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.ProvisioningTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
@Component
@Scope
public class ModelController {

	private static final Trace LOGGER = TraceManager.getTrace(ModelController.class);
	@Autowired(required = true)
	private transient ProvisioningService provisioning;
	@Autowired(required = true)
	private transient RepositoryService repository;

	public String addObject(ObjectType object, OperationResult result) {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		Validate.notEmpty(object.getName(), "Object name must not be null or empty.");
		LOGGER.debug("Adding object {} with oid {} and name {}.", new Object[] {
				object.getClass().getSimpleName(), object.getOid(), object.getName() });

		OperationResult subResult = new OperationResult("Add Object");
		result.addSubresult(subResult);
		String oid = null;
		try {
			if (ProvisioningTypes.isManagedByProvisioning(object)) {
				oid = addProvisioningObject(object, subResult);
			} else {
				oid = addRepositoryObject(object, subResult);
			}
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add object", ex, object.getName());
			subResult.recordFatalError("Couldn't add object '" + object.getName() + "'.", ex);
		}

		LOGGER.debug(subResult.debugDump());
		return oid;
	}

	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(resolve, "Property reference list must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Getting object with oid {}.", new Object[] { oid });

		OperationResult subResult = new OperationResult("Get Object");
		result.addSubresult(subResult);
		ObjectType object = null;
		try {
			object = getObjectFromRepository(oid, resolve, subResult, ObjectType.class);
			if (ProvisioningTypes.isManagedByProvisioning(object)) {
				object = getObjectFromProvisioning(oid, resolve, subResult, ObjectType.class);
			}
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			// TODO: logging
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object {}", ex, oid);
			subResult.recordFatalError("Couldn't get object with oid '" + oid + "'.", ex);
		}

		resolveObjectAttributes(object, resolve, subResult);

		LOGGER.debug(subResult.debugDump());
		return object;
	}

	public ObjectListType listObjects(String objectType, PagingType paging, OperationResult result) {
		Validate.notEmpty(objectType, "Object type must not be null or empty.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);
		LOGGER.debug("Listing objects of type {} from {} to {} ordered {} by {}.", new Object[] { objectType,
				paging.getOffset(), paging.getMaxSize(), paging.getOrderDirection(), paging.getOrderBy() });

		OperationResult subResult = new OperationResult("List Objects");
		result.addSubresult(subResult);
		ObjectListType list = null;
		try {
			if (ProvisioningTypes.isObjectTypeManagedByProvisioning(objectType)) {
				list = provisioning
						.listObjects(ObjectTypes.getObjectTypeClass(objectType), paging, subResult);
			} else {
				list = repository.listObjects(ObjectTypes.getObjectTypeClass(objectType), paging, subResult);
			}
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list objects", ex);
			subResult.recordFatalError("Couldn't list objects.", ex);
		}

		if (list == null) {
			list = new ObjectListType();
			list.setCount(0);
		}

		LOGGER.debug(subResult.debugDump());
		return list;
	}

	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult result) {
		Validate.notNull(query, "Query must not be null.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);
		LOGGER.debug("Searching objects from {} to {} ordered {} by {} (query in TRACE).", new Object[] {
				paging.getOffset(), paging.getMaxSize(), paging.getOrderDirection(), paging.getOrderBy() });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(DebugUtil.prettyPrint(query));
		}

		OperationResult subResult = new OperationResult("Search Objects");
		result.addSubresult(subResult);
		ObjectListType list = null;
		try {
			list = repository.searchObjects(query, paging, subResult);
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't search objects in repository", ex);
			subResult.recordFatalError("Couldn't search objects in repository.", ex);
		}

		if (list == null) {
			list = new ObjectListType();
			list.setCount(0);
		}

		LOGGER.debug(subResult.debugDump());
		return list;
	}

	public void modifyObject(ObjectModificationType change, OperationResult result)
			throws ObjectNotFoundException {
		modifyObjectWithExclusion(change, null, result);
	}

	public void modifyObjectWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result) throws ObjectNotFoundException {
		Validate.notNull(change, "Object modification must not be null.");
		Validate.notEmpty(change.getOid(), "Change oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Modifying object with oid {} with exclusion account oid {} (change in TRACE).",
				new Object[] { change.getOid(), accountOid });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(DebugUtil.prettyPrint(change));
		}

		if (change.getPropertyModification().isEmpty()) {
			return;
		}

		OperationResult subResult = new OperationResult("Modify Object With Exclusion");
		result.addSubresult(subResult);

		try {
			ObjectType object = getObjectFromRepository(change.getOid(), new PropertyReferenceListType(),
					subResult, ObjectType.class);
			if (ProvisioningTypes.isManagedByProvisioning(object)) {
				modifyProvisioningObjectWithExclusion(change, accountOid, subResult, object);
			} else {
				modifyRepositoryObjectWithExclusion(change, accountOid, subResult, object);
			}
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			// TODO: error handling
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't update object with oid {}", ex, change.getOid());
			subResult.recordFatalError("Couldn't update object with oid '" + change.getOid() + "'.", ex);
		}

		LOGGER.debug(subResult.debugDump());
	}

	public boolean deleteObject(String oid, OperationResult result) throws ObjectNotFoundException {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Deleting object with oid {}.", new Object[] { oid });

		OperationResult subResult = new OperationResult("Delete Object");
		result.addSubresult(subResult);

		boolean deleted = false;
		try {
			ObjectType object = getObjectFromRepository(oid, new PropertyReferenceListType(), subResult,
					ObjectType.class);

			if (ProvisioningTypes.isManagedByProvisioning(object)) {
				ScriptsType scripts = getScripts(object, subResult);
				provisioning.deleteObject(oid, scripts, subResult);
			} else {
				if (object instanceof UserType) {
					deleteUserAccounts((UserType) object, subResult);
				}

				repository.deleteObject(oid, subResult);
			}
			deleted = true;
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete object with oid {}", ex, oid);
			subResult.recordFatalError("Couldn't find object with oid '" + oid + "'.", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete object with oid {}", ex, oid);
			subResult.recordFatalError("Couldn't delete object with oid '" + oid + "'.", ex);
		}

		LOGGER.debug(subResult.debugDump());
		return deleted;
	}

	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult result) {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(properties, "Property reference list must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Getting property available values for object with oid {} (properties in TRACE).",
				new Object[] { oid });
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(DebugUtil.prettyPrint(properties));
		}

		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public UserType listAccountShadowOwner(String accountOid, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notEmpty(accountOid, "Account oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Listing account shadow owner for account with oid {}.", new Object[] { accountOid });

		OperationResult subResult = new OperationResult("List Account Shadow Owner");
		result.addSubresult(subResult);

		UserType user = null;
		try {
			user = repository.listAccountShadowOwner(accountOid, subResult);
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Account with oid {} doesn't exists", ex, accountOid);
			subResult.recordFatalError("Account with oid '" + accountOid + "' doesn't exists", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
					+ " for account with oid {}", ex, accountOid);
			subResult.recordFatalError("Couldn't list account shadow owner for account with oid '"
					+ accountOid + "'.", ex);
		}

		LOGGER.debug(subResult.debugDump());
		return user;
	}

	public List<ResourceObjectShadowType> listResourceObjectShadows(String resourceOid,
			String resourceObjectShadowType, OperationResult result) throws ObjectNotFoundException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Listing resource object shadows \"{}\" for resource with oid {}.", new Object[] {
				resourceObjectShadowType, resourceOid });

		OperationResult subResult = new OperationResult("List Resource Object Shadows");
		result.addSubresult(subResult);

		List<ResourceObjectShadowType> list = null;
		try {
			list = repository
					.listResourceObjectShadows(resourceOid,
							ObjectTypes.getObjectTypeFromUri(resourceObjectShadowType).getDeclaringClass(),
							subResult);
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			// TODO: error handling
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list resource object shadows type "
					+ "{} from repository for resource with oid {}", ex, resourceObjectShadowType,
					resourceOid);
			// TODO: error handling
		}

		if (list == null) {
			list = new ArrayList<ResourceObjectShadowType>();
		}

		LOGGER.debug(subResult.debugDump());
		return list;
	}

	public ObjectListType listResourceObjects(String resourceOid, String objectType, PagingType paging,
			OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notEmpty(objectType, "Object type must not be null or empty.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);
		LOGGER.debug(
				"Listing resource objects {} from resource with oid {} from {} to {} ordered {} by {}.",
				new Object[] { objectType, resourceOid, paging.getOffset(), paging.getMaxSize(),
						paging.getOrderDirection(), paging.getOrderDirection() });

		OperationResult subResult = new OperationResult("List Resource Objects");
		result.addSubresult(subResult);

		ObjectListType list = null;
		try {
			list = provisioning.listResourceObjects(resourceOid, objectType, paging, subResult);
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list resource objects of type {} for resource "
					+ "with oid {}", ex, objectType, resourceOid);
			// TODO: error handling
		}

		if (list == null) {
			list = new ObjectListType();
			list.setCount(0);
		}

		LOGGER.debug(subResult.debugDump());
		return list;
	}

	public void testResource(String resourceOid, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Testing resource with oid {}.", new Object[] { resourceOid });

		OperationResult subResult = null;
		try {
			subResult = provisioning.testResource(resourceOid);
			result.addSubresult(subResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't test status for resource {}", ex, resourceOid);

			subResult = new OperationResult("Test Resource");
			subResult.recordFatalError("Couldn't test status for resource with oid '" + resourceOid + "'.",
					ex);
			result.addSubresult(subResult);
		}

		if (subResult != null) {
			LOGGER.debug(subResult.debugDump());
		} else {
			LOGGER.debug("Operation sub result was null (Error occured).");
		}
	}

	public void launchImportFromResource(String resourceOid, String objectClass, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notEmpty(objectClass, "Object class must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Launching import from resource with oid {} for object class {}.", new Object[] {
				resourceOid, objectClass });

		OperationResult subResult = new OperationResult("Launch Import From Resource");
		result.addSubresult(subResult);

		try {
			QName objectClassName = null; // TODO: get object class name
			provisioning.launchImportFromResource(resourceOid, objectClassName, subResult);
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			// TODO: error handling
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't launch import for objects of type {} on resource "
					+ "with oid {}", ex, objectClass, resourceOid);
			// TODO: error handling
		}

		LOGGER.debug(subResult.debugDump());
	}

	public TaskStatusType getImportStatus(String resourceOid, OperationResult result)
			throws ObjectNotFoundException {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");
		LOGGER.debug("Getting import status for resource with oid {}.", new Object[] { resourceOid });

		OperationResult subResult = new OperationResult("Get Import Status");
		result.addSubresult(subResult);

		TaskStatusType status = null;
		try {
			status = provisioning.getImportStatus(resourceOid, subResult);
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			// TODO: error handling
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get import status for resource {}", ex, resourceOid);
			// TODO: error handling

			throw new RuntimeException();
		}

		LOGGER.debug(subResult.debugDump());
		return status;
	}

	private <T> T getObjectFromRepository(String oid, PropertyReferenceListType resolve,
			OperationResult result, Class<T> clazz) throws ObjectNotFoundException {
		return getObject(oid, resolve, result, clazz, false);
	}

	private <T> T getObjectFromProvisioning(String oid, PropertyReferenceListType resolve,
			OperationResult result, Class<T> clazz) throws ObjectNotFoundException {
		return getObject(oid, resolve, result, clazz, true);
	}

	@SuppressWarnings("unchecked")
	private <T> T getObject(String oid, PropertyReferenceListType resolve, OperationResult result,
			Class<T> clazz, boolean fromProvisioning) throws ObjectNotFoundException {
		T object = null;

		try {
			ObjectType objectType = null;
			if (fromProvisioning) {
				objectType = provisioning.getObject(oid, resolve, result);
			} else {
				objectType = repository.getObject(oid, resolve, result);
			}
			if (!clazz.isInstance(objectType)) {
				// TODO: throw better exception...
				throw new RuntimeException("Bad object type returned for referenced oid.");
			} else {
				object = (T) objectType;
			}
		} catch (ObjectNotFoundException ex) {
			// TODO: logging
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "...", ex, oid);
			// TODO: error handling

			throw new RuntimeException();
		}

		return object;
	}

	private String addProvisioningObject(ObjectType object, OperationResult result)
			throws ObjectNotFoundException {
		if (object instanceof AccountShadowType) {
			AccountShadowType account = (AccountShadowType) object;
			preprocessAddAccount(account, result);
		}

		try {
			ScriptsType scripts = getScripts(object, result);
			return provisioning.addObject(object, scripts, result);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add object {} to provisioning", ex, object.getName());
			// TODO: error handling

			throw new RuntimeException();
		}
	}

	private String addRepositoryObject(ObjectType object, OperationResult result) {
		if (object instanceof UserType) {
			UserType user = (UserType) object;
			preprocessAddUser(user, result);
		}

		try {
			return repository.addObject(object, result);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add object {} to repository", ex, object.getName());
			// TODO: error handling

			throw new RuntimeException();
		}
	}

	private void resolveObjectAttributes(ObjectType object, PropertyReferenceListType resolve,
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

		List<ObjectReferenceType> refToBeDeleted = new ArrayList<ObjectReferenceType>();
		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			try {
				AccountShadowType account = getObjectFromProvisioning(accountRef.getOid(), resolve, result,
						AccountShadowType.class);
				user.getAccount().add(account);
				refToBeDeleted.add(accountRef);

				resolveAccountAttributes(account, resolve, result);
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't resolve account with oid {}", ex,
						accountRef.getOid());
				// TODO: error handling
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
		}

		try {
			ResourceType resource = getObjectFromProvisioning(account.getResourceRef().getOid(), resolve,
					result, ResourceType.class);
			account.setResource(resource);
			account.setResourceRef(null);
		} catch (Exception ex) {
			LoggingUtils
					.logException(LOGGER, "Couldn't resolve resource with oid {}", ex, reference.getOid());
			// TODO: error handling
		}
	}

	private ScriptsType getScripts(ObjectType object, OperationResult result) throws ObjectNotFoundException {
		ScriptsType scripts = null;
		if (object instanceof ResourceType) {
			ResourceType resource = (ResourceType) object;
			scripts = resource.getScripts();
		} else if (object instanceof ResourceObjectShadowType) {
			ResourceObjectShadowType resourceObject = (ResourceObjectShadowType) object;
			if (resourceObject.getResource() != null) {
				scripts = resourceObject.getResource().getScripts();
			} else {
				ObjectReferenceType reference = resourceObject.getResourceRef();
				ResourceType resObject = getObjectFromProvisioning(reference.getOid(),
						new PropertyReferenceListType(), result, ResourceType.class);
				scripts = resObject.getScripts();
			}
		}

		if (scripts == null) {
			scripts = new ScriptsType();
		}

		return scripts;
	}

	private void deleteUserAccounts(UserType user, OperationResult result) throws ObjectNotFoundException {
		List<AccountShadowType> accountsToBeDeleted = new ArrayList<AccountShadowType>();
		for (AccountShadowType account : user.getAccount()) {
			if (deleteObject(account.getOid(), result)) {
				accountsToBeDeleted.add(account);
			}
		}
		user.getAccount().removeAll(accountsToBeDeleted);

		List<ObjectReferenceType> refsToBeDeleted = new ArrayList<ObjectReferenceType>();
		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			if (deleteObject(accountRef.getOid(), result)) {
				refsToBeDeleted.add(accountRef);
			}
		}
		user.getAccountRef().removeAll(refsToBeDeleted);

		// TODO: save updated user, create property changes
		ObjectModificationType change = createUserModification(accountsToBeDeleted, refsToBeDeleted);
		change.setOid(user.getOid());
		try {
			repository.modifyObject(change, result);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't update user {} after accounts was deleted", ex,
					user.getName());
			// TODO: error handling
		}
	}

	private ObjectModificationType createUserModification(List<AccountShadowType> accountsToBeDeleted,
			List<ObjectReferenceType> refsToBeDeleted) {
		ObjectModificationType change = new ObjectModificationType();
		for (ObjectReferenceType reference : refsToBeDeleted) {
			PropertyModificationType propertyChangeType = ObjectTypeUtil.createPropertyModificationType(
					PropertyModificationTypeType.delete, null, new QName(SchemaConstants.NS_C, "accountRef"),
					reference);

			change.getPropertyModification().add(propertyChangeType);
		}

		for (AccountShadowType account : accountsToBeDeleted) {
			PropertyModificationType propertyChangeType = ObjectTypeUtil.createPropertyModificationType(
					PropertyModificationTypeType.delete, null, new QName(SchemaConstants.NS_C, "account"),
					account.getOid());

			change.getPropertyModification().add(propertyChangeType);
		}

		return change;
	}

	private void preprocessAddAccount(AccountShadowType account, OperationResult result)
			throws ObjectNotFoundException {
		// TODO insert credentials to account if needed
		ResourceType resource = account.getResource();
		if (resource == null) {
			resource = getObjectFromProvisioning(account.getResourceRef().getOid(),
					new PropertyReferenceListType(), result, ResourceType.class);
		}

		if (resource == null || resource.getSchemaHandling() == null) {
			return;
		}

		AccountType accountType = ModelUtils.getAccountTypeDefinitionFromSchemaHandling(account, resource);
		if (accountType == null || accountType.getCredentials() == null) {
			return;
		}

		AccountType.Credentials credentials = accountType.getCredentials();
		int randomPasswordLength = -1;
		if (credentials.getRandomPasswordLength() != null) {
			randomPasswordLength = credentials.getRandomPasswordLength().intValue();
		}

		if (randomPasswordLength != -1) {
			ModelUtils.generatePassword(account, randomPasswordLength);
		}
	}

	private void modifyProvisioningObjectWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result, ObjectType object) throws ObjectNotFoundException, SchemaException {
		if (object instanceof ResourceObjectShadowType) {
			//TODO: outbound schema handling for this object
		}
		// TODO Auto-generated method stub
		ScriptsType scripts = getScripts(object, result);
		if (StringUtils.isNotEmpty(change.getOid())) {
			provisioning.modifyObject(change, scripts, result);
		}
	}

	private void modifyRepositoryObjectWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result, ObjectType object) throws ObjectNotFoundException, SchemaException {
		// TODO Auto-generated method stub

		repository.modifyObject(change, result);
	}

	// TODO: REFACTOR !!!!!
	/**
	 * User preprocessing is used during synchronization, when we're adding user
	 * and we want to create default accounts for him, or something like that.
	 * Nobody knows, it needs to be reviewed :)
	 */
	private void preprocessAddUser(UserType user, OperationResult result) {
		LOGGER.debug("Preprocessing user {}.", new Object[] { user.getName() });

		List<AccountShadowType> accountsToDelete = new ArrayList<AccountShadowType>();
		for (AccountShadowType emptyAccount : user.getAccount()) {
			ObjectReferenceType resourceRef = emptyAccount.getResourceRef();
			// we're looking for accounts (now only resource references) which
			// have to be created after user is saved
			if (!(emptyAccount.getName() == null && emptyAccount.getOid() == null && resourceRef != null && ObjectTypes.RESOURCE
					.getQName().equals(resourceRef.getType()))) {
				continue;
			}
			accountsToDelete.add(emptyAccount);

			OperationResult subResult = new OperationResult("Create Account");
			result.addSubresult(subResult);

			AccountShadowType account = new AccountShadowType();
			account.setName(resourceRef.getOid() + "-" + user.getName());
			account.setResourceRef(resourceRef);
			try {
				ResourceType resource = getObjectFromProvisioning(resourceRef.getOid(),
						new PropertyReferenceListType(), result, ResourceType.class);
				if (resource == null) {
					subResult.recordFatalError("Couln't get resource with oid '" + resourceRef.getOid()
							+ "'.");
					continue;
				}
				// TODO: account object class from where?
				account.setObjectClass(new QName(resource.getNamespace(), "Account"));

				// TODO: schema handling outbound - desing schemahandling first
				// account = (AccountShadowType)
				// schemaHandling.applyOutboundSchemaHandlingOnAccount(user,
				// account, resource);

				String oid = addObject(account, subResult);
				ObjectReferenceType accountRef = new ObjectReferenceType();
				accountRef.setOid(oid);
				accountRef.setType(SchemaConstants.I_ACCOUNT_SHADOW_TYPE);
				user.getAccountRef().add(accountRef);
			} catch (Exception ex) {
				subResult.recordFatalError("aaaaaaaaaaaaaaaaaaaa", ex);
			}
		}
		user.getAccount().removeAll(accountsToDelete);
	}
}
