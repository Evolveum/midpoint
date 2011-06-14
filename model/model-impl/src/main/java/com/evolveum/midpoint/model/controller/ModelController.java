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

import javax.xml.ws.Holder;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ProvisioningTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

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
	private transient ProvisioningPortType provisioning;
	@Autowired(required = true)
	private transient RepositoryPortType repository;

	public String addObject(ObjectType object, OperationResult result) {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		Validate.notEmpty(object.getName(), "Object name must not be null or empty.");

		String oid = null;
		if (ProvisioningTypes.isManagedByProvisioning(object)) {
			oid = addProvisioningObject(object, result);
		} else {
			oid = addRepositoryObject(object, result);
		}

		return oid;
	}

	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult result) {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(resolve, "Property reference list must not be null.");
		Validate.notNull(result, "Result type must not be null.");

		// TODO: error handling
		ObjectType object = null;
		try {
			object = getObjectFromRepository(oid, resolve);
			if (ProvisioningTypes.isManagedByProvisioning(object)) {
				// TODO: remove Holder add there OperationResult 'result' after
				// provisioning is updated
				ObjectContainerType container = provisioning.getObject(oid, resolve,
						new Holder<OperationalResultType>());
				object = container.getObject();
			}
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object from provisioning", ex);
			// TODO: error handling
		}

		resolveObjectAttributes(object, resolve, result);

		return object;
	}

	public ObjectListType listObjects(String objectType, PagingType paging, OperationResult result) {
		Validate.notEmpty(objectType, "Object type must not be null or empty.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

		// TODO: error handling
		ObjectListType list = null;
		try {
			if (ProvisioningTypes.isObjectTypeManagedByProvisioning(objectType)) {
				// TODO: remove Holder add there OperationResult 'result' after
				// provisioning is updated
				list = provisioning.listObjects(objectType, paging, new Holder<OperationalResultType>());
			} else {
				list = repository.listObjects(objectType, paging);
			}
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list objects from provisioning", ex);
			// TODO: error handling
		} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list objects from repository", ex);
			// TODO: error handling
		}

		// TODO: list variable must not be null here (check error handling)

		return list;
	}

	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult result) {
		Validate.notNull(query, "Query must not be null.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

		// TODO: error handling
		ObjectListType list = null;
		try {
			repository.searchObjects(query, paging);
		} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't search objects in repository", ex);
			// TODO: error handling
		}

		// TODO: list variable must not be null here (check error handling)

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

		ObjectType object = getObjectFromRepository(change.getOid(), new PropertyReferenceListType());
		if (ProvisioningTypes.isManagedByProvisioning(object)) {
			modifyProvisioningObjectWithExclusion(change, accountOid, result, object);
		} else {
			modifyRepositoryObjectWithExclusion(change, accountOid, result, object);
		}
	}

	public void deleteObject(String oid, OperationResult result) {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		ObjectType object = getObjectFromRepository(oid, new PropertyReferenceListType());
		try {
			if (ProvisioningTypes.isManagedByProvisioning(object)) {
				// TODO: remove Holder add there OperationResult after
				// provisioning is updated
				ScriptsType scripts = getScripts(object);
				provisioning.deleteObject(oid, scripts, new Holder<OperationalResultType>());
			} else {
				if (object instanceof UserType) {
					deleteUserAccounts((UserType) object, result);
				}

				// TODO: error handling and operation result
				repository.deleteObject(oid);
			}
		} catch (FaultMessage ex) {
			LoggingUtils
					.logException(LOGGER, "Couldn't delete object with oid {} from provisioning", ex, oid);
			// TODO: error handling
		} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete object with oid {} from repository", ex, oid);
			// TODO: error handling
		}
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

		// TODO: add operation result...
		try {
			UserContainerType container = repository.listAccountShadowOwner(accountOid);
			return container.getUser();
		} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list account shadow owner from repository"
					+ " for account with oid {}", ex, accountOid);
			// TODO: error handling

			throw new RuntimeException();
		}
	}

	public ResourceObjectShadowListType listResourceObjectShadows(String resourceOid,
			String resourceObjectShadowType, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		// TODO: use operation result
		try {
			return repository.listResourceObjectShadows(resourceOid, resourceObjectShadowType);
		} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list resource object shadows type "
					+ "{} from repository for resource with oid {}", ex, resourceObjectShadowType,
					resourceOid);
			// TODO: error handling

			throw new RuntimeException();
		}
	}

	public ObjectListType listResourceObjects(String resourceOid, String objectType, PagingType paging,
			OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notEmpty(objectType, "Object type must not be null or empty.");
		Validate.notNull(paging, "Paging must not be null.");
		Validate.notNull(result, "Result type must not be null.");
		ModelUtils.validatePaging(paging);

		try {
			// TODO: remove Holder add there OperationResult 'result' after
			// provisioning is updated
			return provisioning.listResourceObjects(resourceOid, objectType, paging,
					new Holder<OperationalResultType>());
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list resource objects of type {} for resource "
					+ "with oid {}", ex, objectType, resourceOid);
			// TODO: error handling

			throw new RuntimeException();
		}
	}

	public ResourceTestResultType testResource(String resourceOid, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		try {
			// TODO: remove Holder add there OperationResult 'result' after
			// provisioning is updated
			return provisioning.testResource(resourceOid);
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't test status for resource {}", ex, resourceOid);
			// TODO: error handling

			throw new RuntimeException();
		}
	}

	public void launchImportFromResource(String resourceOid, String objectClass, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notEmpty(objectClass, "Object class must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		try {
			// TODO: add there OperationResult after provisioning is updated
			provisioning.launchImportFromResource(resourceOid, objectClass);
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't launch import for objects of type {} on resource "
					+ "with oid {}", ex, objectClass, resourceOid);
			// TODO: error handling
		}
	}

	public TaskStatusType getImportStatus(String resourceOid, OperationResult result) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(result, "Result type must not be null.");

		try {
			// TODO: add there OperationResult after provisioning is updated
			return provisioning.getImportStatus(resourceOid);
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get import status for resource {}", ex, resourceOid);
			// TODO: error handling

			throw new RuntimeException();
		}
	}

	private ObjectType getObjectFromRepository(String oid, PropertyReferenceListType resolve) {
		ObjectType object = null;

		// TODO: fix error handling and operation result
		try {
			ObjectContainerType container = repository.getObject(oid, resolve);
			if (container != null) {
				object = container.getObject();
			} else {
				// TODO: throw some exception...
			}
		} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get object with oid {} from repository", ex, oid);
		}

		return object;
	}

	private String addProvisioningObject(ObjectType object, OperationResult result) {
		if (object instanceof AccountShadowType) {
			AccountShadowType account = (AccountShadowType) object;
			preprocessAccount(account, result);
		}

		ObjectContainerType container = new ObjectContainerType();
		container.setObject(object);

		try {
			// TODO: remove Holder add there OperationResult 'result' after
			// provisioning is updated
			ScriptsType scripts = getScripts(object);
			return provisioning.addObject(container, scripts, new Holder<OperationalResultType>());
		} catch (FaultMessage ex) {
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
			// TODO: add there OperationResult after repository is updated
			return repository.addObject(container);
		} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
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
			
		} else if (object instanceof AccountShadowType) {
			
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

	private void deleteUserAccounts(UserType object, OperationResult result) {
		// TODO Auto-generated method stub

	}

	private void preprocessUser(UserType user, OperationResult result) {
		// TODO Auto-generated method stub

	}

	private void preprocessAccount(AccountShadowType account, OperationResult result) {
		// TODO insert credentials to account if needed

	}

	private ScriptsType getScripts(ObjectType object) {
		// TODO Auto-generated method stub

		return new ScriptsType();
	}
}
