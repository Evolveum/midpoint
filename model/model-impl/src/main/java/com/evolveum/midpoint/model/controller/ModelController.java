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
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.ProvisioningTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
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
//@Component
//@Scope
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

		OperationResult subResult = new OperationResult("Add Object");
		result.addSubresult(subResult);
		String oid = null;
		if (ProvisioningTypes.isManagedByProvisioning(object)) {
			oid = addProvisioningObject(object, subResult);
		} else {
			oid = addRepositoryObject(object, subResult);
		}

		return oid;
	}

	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult result) {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(resolve, "Property reference list must not be null.");
		Validate.notNull(result, "Result type must not be null.");

		OperationResult subResult = new OperationResult("Get Object");
		result.addSubresult(subResult);
		ObjectType object = null;
		try {
			object = getObjectFromRepository(oid, resolve, subResult, ObjectType.class);
			if (ProvisioningTypes.isManagedByProvisioning(object)) {
				object = getObjectFromProvisioning(oid, resolve, subResult, ObjectType.class);
			}
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object", ex);
			// TODO: error handling ?????

			throw new RuntimeException();
		}

		resolveObjectAttributes(object, resolve, result);

		return object;
	}

	public ObjectListType listObjects(String objectType, PagingType paging, OperationResult result) {
		Validate.notEmpty(objectType, "Object type must not be null or empty.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

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

		return list;
	}

	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult result) {
		Validate.notNull(query, "Query must not be null.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

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

		return list;
	}

	public void modifyObject(ObjectModificationType change, OperationResult result) {
		modifyObjectWithExclusion(change, null, result);
	}

	public void modifyObjectWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result) {
		Validate.notNull(change, "Object modification must not be null.");
		Validate.notEmpty(change.getOid(), "Change oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		if (change.getPropertyModification().isEmpty()) {
			return;
		}

		ObjectType object = getObjectFromRepository(change.getOid(), new PropertyReferenceListType(), result,
				ObjectType.class);
		if (ProvisioningTypes.isManagedByProvisioning(object)) {
			modifyProvisioningObjectWithExclusion(change, accountOid, result, object);
		} else {
			modifyRepositoryObjectWithExclusion(change, accountOid, result, object);
		}
	}

	public boolean deleteObject(String oid, OperationResult result) {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

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
			// TODO: error handling
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete object with oid {}", ex, oid);
			// TODO: error handling
		}

		return deleted;
	}

	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult result) {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(properties, "Property reference list must not be null.");
		Validate.notNull(result, "Result type must not be null.");

		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public UserType listAccountShadowOwner(String accountOid, OperationResult result) {
		Validate.notEmpty(accountOid, "Account oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		OperationResult subResult = new OperationResult("List Account Shadow Owner");
		result.addSubresult(subResult);
		try {
			UserType user = repository.listAccountShadowOwner(accountOid, subResult);
			subResult.recordSuccess();

			return user;
		} catch (ObjectNotFoundException ex) {
			// TODO: error handling
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
					+ " for account with oid {}", ex, accountOid);
			// TODO: error handling

			throw new RuntimeException();
		}

		return null;
	}

	public List<ResourceObjectShadowType> listResourceObjectShadows(String resourceOid,
			String resourceObjectShadowType, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		OperationResult subResult = new OperationResult("List Resource Object Shadows");
		result.addSubresult(subResult);

		List<ResourceObjectShadowType> list = null;
		try {
			list = repository.listResourceObjectShadows(resourceOid,
					ObjectTypes.getObjectTypeClass(resourceObjectShadowType), subResult);
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			// TODO: error handling
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list resource object shadows type "
					+ "{} from repository for resource with oid {}", ex, resourceObjectShadowType,
					resourceOid);
			// TODO: error handling

			throw new RuntimeException();
		}

		if (list == null) {
			list = new ArrayList<ResourceObjectShadowType>();
		}

		return list;
	}

	public ObjectListType listResourceObjects(String resourceOid, String objectType, PagingType paging,
			OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notEmpty(objectType, "Object type must not be null or empty.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

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

		return list;
	}

	public ResourceTestResultType testResource(String resourceOid, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		OperationResult subResult = new OperationResult("Test Resource");
		result.addSubresult(subResult);

		// try {
		// TODO: remove Holder add there OperationResult 'result' after
		// provisioning is updated

		// TODO: WTF???
		// return provisioning.testResource(resourceOid);
		throw new RuntimeException();
		// } catch (FaultMessage ex) {
		// LoggingUtils.logException(LOGGER,
		// "Couldn't test status for resource {}", ex, resourceOid);
		// // TODO: error handling
		//
		// throw new RuntimeException();
		// }
	}

	public void launchImportFromResource(String resourceOid, String objectClass, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notEmpty(objectClass, "Object class must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

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
	}

	public TaskStatusType getImportStatus(String resourceOid, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		OperationResult subResult = new OperationResult("Get Import Status");
		result.addSubresult(subResult);

		TaskStatusType status = null;
		try {
			status = provisioning.getImportStatus(resourceOid, subResult);
			subResult.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			// TODO: error handling
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get import status for resource {}", ex, resourceOid);
			// TODO: error handling

			throw new RuntimeException();
		}

		return status;
	}

	private <T> T getObjectFromRepository(String oid, PropertyReferenceListType resolve,
			OperationResult result, Class<T> clazz) {
		return getObject(oid, resolve, result, clazz, false);
	}

	private <T> T getObjectFromProvisioning(String oid, PropertyReferenceListType resolve,
			OperationResult result, Class<T> clazz) {
		return getObject(oid, resolve, result, clazz, true);
	}

	@SuppressWarnings("unchecked")
	private <T> T getObject(String oid, PropertyReferenceListType resolve, OperationResult result,
			Class<T> clazz, boolean fromProvisioning) {
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
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "...", ex, oid);
			// TODO: error handling

			throw new RuntimeException();
		}

		return object;
	}

	private String addProvisioningObject(ObjectType object, OperationResult result) {
		if (object instanceof AccountShadowType) {
			AccountShadowType account = (AccountShadowType) object;
			preprocessAccount(account, result);
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
			preprocessUser(user, result);
		}

		ObjectContainerType container = new ObjectContainerType();
		container.setObject(object);

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

	private ScriptsType getScripts(ObjectType object, OperationResult result) {
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

	private void deleteUserAccounts(UserType user, OperationResult result) {
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

	private void preprocessAccount(AccountShadowType account, OperationResult result) {
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
			OperationResult result, ObjectType object) {
		// TODO Auto-generated method stub
	}

	private void modifyRepositoryObjectWithExclusion(ObjectModificationType change, String accountOid,
			OperationResult result, ObjectType object) {
		// TODO Auto-generated method stub
	}

	private void preprocessUser(UserType user, OperationResult result) {
		for (AccountShadowType account : user.getAccount()) {
			ObjectReferenceType ref = account.getResourceRef();
			if (account.getName() == null && account.getOid() == null && ref != null
					&& SchemaConstants.I_RESOURCE_TYPE.equals(ref.getType())) {

			}
		}

	}
}
