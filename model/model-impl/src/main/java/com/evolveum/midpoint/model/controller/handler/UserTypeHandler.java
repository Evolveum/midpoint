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
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.controller.ModelControllerImpl;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.model.controller.SchemaHandler;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ConsistencyViolationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.patch.PatchException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class UserTypeHandler extends BasicHandler {

	private static final Trace LOGGER = TraceManager.getTrace(UserTypeHandler.class);

	public UserTypeHandler(ModelController modelController, ProvisioningService provisioning,
			RepositoryService repository, SchemaHandler schemaHandler) {
		super(modelController, provisioning, repository, schemaHandler);
	}

	@SuppressWarnings("unchecked")
	public <T extends ObjectType> void modifyObjectWithExclusion(Class<T> type,
			ObjectModificationType change, String accountOid, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		UserType user = (UserType) getObject(UserType.class, change.getOid(),
				new PropertyReferenceListType(), result);

		// processing add account
		processAddDeleteAccountFromChanges(change, user, result);

		getRepository().modifyObject(UserType.class, change, result);
		try {
			PatchXml patchXml = new PatchXml();
			String u = patchXml.applyDifferences(change, user);
			user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(u)).getValue();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't patch user {}", ex, user.getName());
		}

		PropertyModificationType userActivationChanged = hasUserActivationChanged(change);
		if (userActivationChanged != null) {
			LOGGER.debug("User activation status changed, enabling/disabling accounts in next step.");
		}

		// from now on we have updated user, next step is processing
		// outbound for every account or enable/disable account if needed
		modifyAccountsAfterUserWithExclusion(user, change, userActivationChanged, accountOid, result);
	}

	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result)
			throws ObjectNotFoundException, ConsistencyViolationException {
		try {
			UserType object = getObject(UserType.class, oid, new PropertyReferenceListType(), result);
			deleteUserAccounts((UserType) object, result);

			getRepository().deleteObject(type, oid, result);
			result.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't delete object with oid {}", ex, oid);
			result.recordFatalError("Couldn't find object with oid '" + oid + "'.", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER,
					"Couldn't delete object with oid {}, potential consistency violation", ex, oid);
			result.recordFatalError("Couldn't delete object with oid '" + oid
					+ "', potential consistency violation");
			throw new ConsistencyViolationException("Couldn't delete object with oid '" + oid
					+ "', potential consistency violation", ex);
		} finally {
			LOGGER.debug(result.dump());
		}
	}

	public String addObject(ObjectType object, OperationResult result) throws ObjectAlreadyExistsException,
			ObjectNotFoundException {
		UserType user = (UserType) object;
		processAddAccountFromUser(user, result);
		// At first we get default user template from system configuration
		SystemConfigurationType systemConfiguration = getSystemConfiguration(result);
		UserTemplateType userTemplate = systemConfiguration.getDefaultUserTemplate();
		object = processUserTemplateForUser(user, userTemplate, result);

		try {
			return getRepository().addObject(object, result);
		} catch (ObjectAlreadyExistsException ex) {
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add object {} to repository", ex, object.getName());
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	public String addUser(UserType user, UserTemplateType userTemplate, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException {
		if (userTemplate == null) {
			SystemConfigurationType systemConfiguration = getSystemConfiguration(result);
			userTemplate = systemConfiguration.getDefaultUserTemplate();
		}
		result.addParams(new String[] { "user", "userTemplate" }, user, userTemplate);

		if (userTemplate != null) {
			LOGGER.debug("Adding user {}, oid {} using template {}, oid {}.", new Object[] { user.getName(),
					user.getOid(), userTemplate.getName(), userTemplate.getOid() });
		} else {
			LOGGER.debug("Adding user {}, oid {} using no template.",
					new Object[] { user.getName(), user.getOid() });
		}

		String oid = null;
		try {
			processAddAccountFromUser(user, result);
			user = processUserTemplateForUser(user, userTemplate, result);
			oid = getRepository().addObject(user, result);
			result.recordSuccess();
		} catch (ObjectAlreadyExistsException ex) {
			result.recordFatalError("Couldn't add user '" + user.getName() + "', oid '" + user.getOid()
					+ "' because user already exists.", ex);
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add user {}, oid {} using template {}, oid {}", ex,
					user.getName(), user.getOid(), userTemplate.getName(), userTemplate.getOid());
			result.recordFatalError("Couldn't add user " + user.getName() + ", oid '" + user.getOid()
					+ "' using template " + userTemplate.getName() + ", oid '" + userTemplate.getOid() + "'",
					ex);
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			LOGGER.debug(result.dump());
		}

		return oid;
	}

	private void processAddAccountFromUser(UserType user, OperationResult result) {
		List<AccountShadowType> accountsToDelete = new ArrayList<AccountShadowType>();
		for (AccountShadowType account : user.getAccount()) {
			try {
				if (account.getActivation() == null) {
					account.setActivation(user.getActivation());
				}
				// MID-72
				pushPasswordFromUserToAccount(user, account, result);

				String newAccountOid = addObject(account, result);
				ObjectReferenceType accountRef = ModelUtils.createReference(newAccountOid,
						ObjectTypes.ACCOUNT);
				user.getAccountRef().add(accountRef);
				accountsToDelete.add(account);
			} catch (SystemException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new SystemException("Couldn't process add account.", ex);
			}
		}
		user.getAccount().removeAll(accountsToDelete);
	}

	/**
	 * MID-72, MID-73 password push from user to account
	 */
	private void pushPasswordFromUserToAccount(UserType user, AccountShadowType account,
			OperationResult result) throws ObjectNotFoundException {
		ResourceType resource = account.getResource();
		if (resource == null) {
			resource = getObject(ResourceType.class, account.getResourceRef().getOid(),
					new PropertyReferenceListType(), result);
		}
		AccountType accountHandling = ModelUtils.getAccountTypeFromHandling(account, resource);
		boolean pushPasswordToAccount = false;
		if (accountHandling.getCredentials() != null
				&& accountHandling.getCredentials().isOutboundPassword() != null) {
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

	private void deleteUserAccounts(UserType user, OperationResult result) throws ObjectNotFoundException {
		List<AccountShadowType> accountsToBeDeleted = new ArrayList<AccountShadowType>();
		for (AccountShadowType account : user.getAccount()) {
			try {
				deleteObject(AccountShadowType.class, account.getOid(), result);
				accountsToBeDeleted.add(account);
			} catch (ConsistencyViolationException ex) {
				// TODO: handle this
				LoggingUtils.logException(LOGGER, "TODO handle ConsistencyViolationException", ex);
			}
		}

		user.getAccount().removeAll(accountsToBeDeleted);

		List<ObjectReferenceType> refsToBeDeleted = new ArrayList<ObjectReferenceType>();
		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			try {
				deleteObject(AccountShadowType.class, accountRef.getOid(), result);
				refsToBeDeleted.add(accountRef);
			} catch (ConsistencyViolationException ex) {
				// TODO handle this
				LoggingUtils.logException(LOGGER, "TODO handle ConsistencyViolationException", ex);
			}
		}
		user.getAccountRef().removeAll(refsToBeDeleted);

		// If list is empty then skip processing user have no accounts.
		if (accountsToBeDeleted.isEmpty() && refsToBeDeleted.isEmpty()) {
			return;
		}

		ObjectModificationType change = createUserModification(accountsToBeDeleted, refsToBeDeleted);
		change.setOid(user.getOid());
		try {
			getRepository().modifyObject(UserType.class, change, result);
		} catch (ObjectNotFoundException ex) {
			throw ex;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't update user {} after accounts was deleted", ex,
					user.getName());
			throw new SystemException(ex.getMessage(), ex);
		}
	}

	private ObjectModificationType createUserModification(List<AccountShadowType> accountsToBeDeleted,
			List<ObjectReferenceType> refsToBeDeleted) {
		ObjectModificationType change = new ObjectModificationType();
		for (ObjectReferenceType reference : refsToBeDeleted) {
			PropertyModificationType propertyChangeType = ObjectTypeUtil.createPropertyModificationType(
					PropertyModificationTypeType.delete, null, SchemaConstants.I_ACCOUNT_REF, reference);

			change.getPropertyModification().add(propertyChangeType);
		}

		for (AccountShadowType account : accountsToBeDeleted) {
			PropertyModificationType propertyChangeType = ObjectTypeUtil.createPropertyModificationType(
					PropertyModificationTypeType.delete, null, SchemaConstants.I_ACCOUNT, account.getOid());

			change.getPropertyModification().add(propertyChangeType);
		}

		return change;
	}

	private void modifyAccountsAfterUserWithExclusion(UserType user, ObjectModificationType change,
			PropertyModificationType userActivationChanged, String accountOid, OperationResult result) {
		List<ObjectReferenceType> accountRefs = user.getAccountRef();
		for (ObjectReferenceType accountRef : accountRefs) {
			OperationResult subResult = result.createSubresult(ModelControllerImpl.UPDATE_ACCOUNT);
			subResult.addParams(new String[] { "change", "accountOid", "object", "accountRef" }, change,
					accountOid, user, accountRef);
			if (StringUtils.isNotEmpty(accountOid) && accountOid.equals(accountRef.getOid())) {
				subResult.computeStatus("Account excluded during modification, skipped.");
				// preventing cycles while updating resource object shadows
				continue;
			}

			try {
				AccountShadowType account = getObject(AccountShadowType.class, accountRef.getOid(),
						ModelUtils.createPropertyReferenceListType("Resource"), subResult);

				ObjectModificationType accountChange = null;
				try {
					accountChange = getSchemaHandler().processOutboundHandling(user, account, subResult);
				} catch (SchemaException ex) {
					LoggingUtils.logException(LOGGER, "Couldn't update outbound handling for account {}", ex,
							accountRef.getOid());
					subResult.recordFatalError(ex);
				}

				if (accountChange == null) {
					accountChange = new ObjectModificationType();
					accountChange.setOid(account.getOid());
				}

				if (userActivationChanged != null) {
					PropertyModificationType modification = new PropertyModificationType();
					modification.setModificationType(PropertyModificationTypeType.replace);
					modification.setPath(userActivationChanged.getPath());
					modification.setValue(userActivationChanged.getValue());
					accountChange.getPropertyModification().add(modification);
				}

				getModelController().modifyObjectWithExclusion(AccountShadowType.class, accountChange, accountOid, subResult);
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't update account {}", ex, accountRef.getOid());
				subResult.recordFatalError(ex);
			} finally {
				subResult.computeStatus("Couldn't update account '" + accountRef.getOid() + "'.");
			}
		}
	}

	private PropertyModificationType hasUserActivationChanged(ObjectModificationType change) {
		for (PropertyModificationType modification : change.getPropertyModification()) {
			XPathHolder xpath = new XPathHolder(modification.getPath());
			List<XPathSegment> segments = xpath.toSegments();
			if (segments == null || segments.isEmpty() || segments.size() > 1) {
				continue;
			}

			if (SchemaConstants.ACTIVATION.equals(segments.get(0).getQName())) {
				return modification;
			}
		}

		return null;
	}

	private void processAddDeleteAccountFromChanges(ObjectModificationType change, UserType userBeforeChange,
			OperationResult result) {
		// MID-72, MID-73 password push from user to account
		// TODO: look for password property modification and next use it while
		// creating account. If account schemahandling credentials
		// outboundPassword is true, we have to push password
		for (PropertyModificationType propertyChange : change.getPropertyModification()) {
			if (propertyChange.getValue() == null || propertyChange.getValue().getAny().isEmpty()) {
				continue;
			}

			Object node = propertyChange.getValue().getAny().get(0);
			if (!SchemaConstants.I_ACCOUNT.equals(JAXBUtil.getElementQName(node))) {
				continue;
			}

			try {
				switch (propertyChange.getModificationType()) {
					case add:
						processAddAccountFromChanges(propertyChange, node, userBeforeChange, result);
						break;
					case delete:
						processDeleteAccountFromChanges(propertyChange, node, userBeforeChange, result);
						break;
				}
			} catch (SystemException ex) {
				throw ex;
			} catch (Exception ex) {
				final String message = propertyChange.getModificationType() == PropertyModificationTypeType.add ? "Couldn't process add account."
						: "Couldn't process delete account.";
				throw new SystemException(message, ex);
			}
		}
	}

	private void processDeleteAccountFromChanges(PropertyModificationType propertyChange, Object node,
			UserType userBeforeChange, OperationResult result) throws JAXBException {
		AccountShadowType account = XsdTypeConverter.toJavaValue(node, AccountShadowType.class);

		ObjectReferenceType accountRef = ModelUtils.createReference(account.getOid(), ObjectTypes.ACCOUNT);
		PropertyModificationType deleteAccountRefChange = ObjectTypeUtil.createPropertyModificationType(
				PropertyModificationTypeType.delete, null, SchemaConstants.I_ACCOUNT_REF, accountRef);

		propertyChange.setPath(deleteAccountRefChange.getPath());
		propertyChange.setValue(deleteAccountRefChange.getValue());
	}

	@SuppressWarnings("unchecked")
	private void processAddAccountFromChanges(PropertyModificationType propertyChange, Object node,
			UserType userBeforeChange, OperationResult result) throws JAXBException, ObjectNotFoundException,
			PatchException, ObjectAlreadyExistsException {

		AccountShadowType account = XsdTypeConverter.toJavaValue(node, AccountShadowType.class);
		pushPasswordFromUserToAccount(userBeforeChange, account, result);

		ObjectModificationType accountChange = processOutboundSchemaHandling(userBeforeChange, account,
				result);
		if (accountChange != null) {
			PatchXml patchXml = new PatchXml();
			String accountXml = patchXml.applyDifferences(accountChange, account);
			account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(accountXml)).getValue();
		}

		String newAccountOid = addObject(account, result);
		ObjectReferenceType accountRef = ModelUtils.createReference(newAccountOid, ObjectTypes.ACCOUNT);
		Element accountRefElement = JAXBUtil.jaxbToDom(accountRef, SchemaConstants.I_ACCOUNT_REF,
				DOMUtil.getDocument());

		propertyChange.getValue().getAny().clear();
		propertyChange.getValue().getAny().add(accountRefElement);
	}
}
