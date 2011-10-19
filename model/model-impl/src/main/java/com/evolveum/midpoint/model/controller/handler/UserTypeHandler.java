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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.Delta;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.controller.ModelControllerImpl;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.model.controller.SchemaHandler;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ConsistencyViolationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.patch.PatchException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * THIS NEEDS TO BE SERIOUSLY REFACTORED!
 * 
 * @author lazyman
 * 
 */
public class UserTypeHandler extends BasicHandler {

	private static final Trace LOGGER = TraceManager.getTrace(UserTypeHandler.class);

	/**
	 * Enum is used during processing AssignmentType objects in user
	 */
	private static enum AssignOperation {
		ADD, DELETE
	}

	public UserTypeHandler(ModelController modelController, ProvisioningService provisioning,
			RepositoryService repository, SchemaHandler schemaHandler) {
		super(modelController, provisioning, repository, schemaHandler);
	}

	@SuppressWarnings("unchecked")
	public <T extends ObjectType> void modifyObject(Class<T> type,
			ObjectModificationType change, Collection<String> excludedResourceOids, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		LOGGER.trace("UserTypeHandler: modifyObjectWithExclusion: {}",change);
		UserType userBefore = getObject(UserType.class, change.getOid(), new PropertyReferenceListType(), result);
		// ADD and DELETE account changes
		List<PropertyModificationType> accountChanges = getAccountChanges(change);
		// we remove account changes, then we save user object
		change.getPropertyModification().removeAll(accountChanges);
		
		UserType userAfter = userBefore;
		if (!change.getPropertyModification().isEmpty()) {
			// not only account was added/deleted, other attributes has changed
			getRepository().modifyObject(UserType.class, change, result);

			try {
				PatchXml patchXml = new PatchXml();
				String u = patchXml.applyDifferences(change, userBefore);
				userAfter = ((JAXBElement<UserType>) JAXBUtil.unmarshal(u)).getValue();
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't patch user {}", ex, userAfter.getName());
			}
		}

		PropertyModificationType userActivationChanged = hasPropertyChanged(change,
				SchemaConstants.ACTIVATION);
		if (userActivationChanged != null) {
			LOGGER.trace("User activation status changed, enabling/disabling accounts in next step.");
		}
		PropertyModificationType userCredentialsChanged = hasPropertyChanged(change,
				SchemaConstants.I_CREDENTIALS);
		if (userCredentialsChanged != null) {
			LOGGER.trace("User credentials changed, updating accounts.");
		}
		// from now on we have updated user, next step is processing
		// outbound for every existing account or enable/disable account if
		// needed
		modifyAccountsAfterUserWithExclusion(userAfter, userActivationChanged, userCredentialsChanged, excludedResourceOids,
				result);

		// process add and delete accounts
		List<PropertyModificationType> userAccountChanges = processAddDeleteAccountFromChanges(accountChanges, userAfter,
				result);
		if (!userAccountChanges.isEmpty()) {			
			// update user by adding and removing accountRef elements
			change = new ObjectModificationType();
			change.setOid(userAfter.getOid());
			change.getPropertyModification().addAll(userAccountChanges);
			getRepository().modifyObject(UserType.class, change, result);
		}
		
		processModifyAssignments(userBefore, change, excludedResourceOids, result);
	}

	private List<PropertyModificationType> getAccountChanges(ObjectModificationType change) {
		List<PropertyModificationType> modifications = new ArrayList<PropertyModificationType>();
		if (change == null || change.getPropertyModification() == null) {
			return modifications;
		}

		for (PropertyModificationType propertyChange : change.getPropertyModification()) {
			if (propertyChange.getValue() == null || propertyChange.getValue().getAny().isEmpty()) {
				continue;
			}

			Object node = propertyChange.getValue().getAny().get(0);
			if (!SchemaConstants.I_ACCOUNT.equals(JAXBUtil.getElementQName(node))) {
				continue;
			}

			modifications.add(propertyChange);
		}

		return modifications;
	}

	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, Collection<String> excludedResourceOids, OperationResult result)
			throws ObjectNotFoundException, ConsistencyViolationException {
		try {
			UserType user = getObject(UserType.class, oid, null, result);
			processDeleteAssignments(user, user.getAssignment(), excludedResourceOids, result);
			deleteUserAccounts(user, result);

			getRepository().deleteObject(UserType.class, oid, result);
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
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(result.dump());
			}
		}
	}

	public String addObject(ObjectType object, Collection<String> excludedResourceOids, OperationResult result) throws ObjectAlreadyExistsException,
			ObjectNotFoundException {
		if (!(object instanceof UserType)) {
			throw new IllegalArgumentException("Can't add '" + object.getName() + "', type '"
					+ object.getClass().getSimpleName() + "' with '" + UserTypeHandler.class.getSimpleName()
					+ "'.");
		}

		return addUser((UserType) object, null, excludedResourceOids, result);
	}

	public String addUser(UserType user, UserTemplateType userTemplate, Collection<String> excludedResourceOids, OperationResult result)
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
			// TODO: process add account should be removed, we have to use only
			// assignments and there should be account if needed
			processAddAccountFromUser(user, result);
			processAddAssignments(user, user.getAssignment(), excludedResourceOids, result);

			user = processUserTemplateForUser(user, userTemplate, excludedResourceOids, result);
			oid = getRepository().addObject(user, result);
			result.recordSuccess();
		} catch (ObjectAlreadyExistsException ex) {
			result.recordFatalError("Couldn't add user '" + user.getName() + "', oid '" + user.getOid()
					+ "' because user already exists.", ex);
			throw ex;
		} catch (Exception ex) {
			if (userTemplate != null) {
				LoggingUtils.logException(LOGGER, "Couldn't add user {}, oid {} using template {}, oid {}",
						ex, user.getName(), user.getOid(), userTemplate.getName(), userTemplate.getOid());
				result.recordFatalError("Couldn't add user " + user.getName() + ", oid '" + user.getOid()
						+ "' using template " + userTemplate.getName() + ", oid '" + userTemplate.getOid()
						+ "'", ex);
			} else {
				LoggingUtils.logException(LOGGER, "Couldn't add user {}, oid {} without user template", ex,
						user.getName(), user.getOid());
				result.recordFatalError("Couldn't add user " + user.getName() + ", oid '" + user.getOid()
						+ "' without user template.", ex);
			}
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(result.dump());
			}
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

				String newAccountOid = getModelController().addObject(account, result);
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

	private void deleteUserAccounts(UserType user, OperationResult result) throws ObjectNotFoundException {
		List<AccountShadowType> accountsToBeDeleted = new ArrayList<AccountShadowType>();
		for (AccountShadowType account : user.getAccount()) {
			try {
				getModelController().deleteObject(AccountShadowType.class, account.getOid(), result);
				accountsToBeDeleted.add(account);
			} catch (ConsistencyViolationException ex) {
				// TODO: handle this
				LoggingUtils.logException(LOGGER, "TODO handle ConsistencyViolationException", ex);
			} catch (ObjectNotFoundException ex) {
				// Now if the account cannot be deleted, only partial result is
				// recorded
				// later, the account will be deleted or user re-created by
				// reconciliation
				result.recordPartialError("Couldn't delete account " + account.getName() + ".", ex);
			}
		}

		user.getAccount().removeAll(accountsToBeDeleted);

		List<ObjectReferenceType> refsToBeDeleted = new ArrayList<ObjectReferenceType>();
		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			try {
				getModelController().deleteObject(AccountShadowType.class, accountRef.getOid(), result);
				refsToBeDeleted.add(accountRef);
			} catch (ConsistencyViolationException ex) {
				// TODO handle this
				LoggingUtils.logException(LOGGER, "TODO handle ConsistencyViolationException", ex);
			} catch (ObjectNotFoundException ex) {
				// Now if the account cannot be deleted, only partial result is
				// recorded
				// later, the account will be deleted or user re-created by
				// reconciliation
				result.recordPartialError("Couldn't delete account " + accountRef.getOid() + ".", ex);
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
			// result.recordPartialError("Could", cause)
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

	private void modifyAccountsAfterUserWithExclusion(UserType user,
			PropertyModificationType userActivationChanged, PropertyModificationType userCredentials,
			Collection<String> excludedResourceOids, OperationResult result) {

		if (user.getAccountRef() == null) {
			return;
		}
		
		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			OperationResult subResult = result.createSubresult(ModelControllerImpl.CHANGE_ACCOUNT);
			subResult.addParams(new String[] { "accountOid", "object", "accountRef" }, excludedResourceOids, user,
					accountRef);

			try {
				AccountShadowType account = getModelController().getObject(AccountShadowType.class,
						accountRef.getOid(), ModelUtils.createPropertyReferenceListType("Resource"),
						subResult);

				if (excludedResourceOids != null 
						&& excludedResourceOids.contains(ResourceObjectShadowUtil.getResourceOid(account))) {
					subResult.computeStatus("Account excluded during modification, skipped.");
					// preventing cycles while updating resource object shadows
					continue;
				}
				
				ResourceType resource = getProvisioning().getObject(ResourceType.class,
						ResourceObjectShadowUtil.getResourceOid(account), null, subResult);

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
					if (ResourceTypeUtil.hasActivationCapability(resource)) {
						copyModification(userActivationChanged, accountChange);
					} else {
						LOGGER.debug(
								"User {} activation changed, but resource {} does not have activation capability, skipping",
								ObjectTypeUtil.toShortString(user), ObjectTypeUtil.toShortString(resource));
					}
				}

				if (userCredentials != null) {
					if (ResourceTypeUtil.hasCredentialsCapability(resource)) {
						copyModification(userCredentials, accountChange);
					} else {
						LOGGER.debug(
								"User {} credentials changed, but resource {} does not have credentials capability, skipping",
								ObjectTypeUtil.toShortString(user), ObjectTypeUtil.toShortString(resource));
					}
				}

				if (!ObjectTypeUtil.isEmpty(accountChange)) {

					getModelController().modifyObjectWithExclusion(AccountShadowType.class, accountChange,
							excludedResourceOids, subResult);
				}

			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't update account {}", ex, accountRef.getOid());
				subResult.recordFatalError(ex);
			} finally {
				subResult.computeStatus("Couldn't update account '" + accountRef.getOid() + "'.");
			}
		}
	}

	private void copyModification(PropertyModificationType from, ObjectModificationType here) {
		PropertyModificationType modification = new PropertyModificationType();
		modification.setModificationType(PropertyModificationTypeType.replace);
		modification.setPath(from.getPath());
		modification.setValue(from.getValue());
		here.getPropertyModification().add(modification);
	}

	private PropertyModificationType hasPropertyChanged(ObjectModificationType change, QName property) {
		for (PropertyModificationType modification : change.getPropertyModification()) {
			XPathHolder xpath = new XPathHolder(modification.getPath());
			List<XPathSegment> segments = xpath.toSegments();
			if (segments == null || segments.isEmpty()) {
				continue;
			}

			if (property.equals(segments.get(0).getQName())) {
				return modification;
			}
		}

		return null;
	}

	private List<PropertyModificationType> processAddDeleteAccountFromChanges(
			List<PropertyModificationType> accountChanges, UserType user, OperationResult result) {
		List<PropertyModificationType> userChanges = new ArrayList<PropertyModificationType>();
		for (PropertyModificationType change : accountChanges) {
			OperationResult subResult = result.createSubresult(ModelController.CHANGE_ACCOUNT);
			Object node = change.getValue().getAny().get(0);
			try {
				switch (change.getModificationType()) {
					case add:
						userChanges.add(processAddAccountFromChanges(change, node, user, subResult));
						break;
					case delete:
						userChanges.add(processDeleteAccountFromChanges(change, node, user, subResult));
						break;
					default:
						LOGGER.error("Unexpected modification type " + change.getModificationType()
								+ " while changing account, skipping it");
						break;
				}
				subResult.recordSuccess();
			} catch (Exception ex) {
				final String operation = change.getModificationType() == PropertyModificationTypeType.add ? "add"
						: "delete";
				subResult.recordFatalError("Couldn't " + operation + " account for user '" + user.getName()
						+ "'.", ex);
				LoggingUtils.logException(LOGGER, "Couldn't process {} account for user '{}'.", ex,
						operation, user.getName());
				// TODO: better error handling
			} finally {
				subResult.computeStatus();
			}
		}

		return userChanges;
	}

	private PropertyModificationType processDeleteAccountFromChanges(PropertyModificationType propertyChange,
			Object node, UserType userBeforeChange, OperationResult result) throws JAXBException,
			ObjectNotFoundException, CommunicationException, SchemaException {

		ObjectReferenceType accountRef = null;
		if (SchemaConstants.I_ACCOUNT.equals(JAXBUtil.getElementQName(node))) {
			AccountShadowType account = XsdTypeConverter.toJavaValue(node, AccountShadowType.class);
			accountRef = ModelUtils.createReference(account.getOid(), ObjectTypes.ACCOUNT);
		} else if (SchemaConstants.I_ACCOUNT_REF.equals(JAXBUtil.getElementQName(node))) {
			accountRef = XsdTypeConverter.toJavaValue(node, ObjectReferenceType.class);
		} else {
			LOGGER.error("Unexpected element " + JAXBUtil.getElementQName(node) + " in account change");
			throw new IllegalArgumentException("Unexpected element " + JAXBUtil.getElementQName(node)
					+ " in account change");
		}

		getProvisioning().deleteObject(AccountShadowType.class, accountRef.getOid(), null, result);

		PropertyModificationType deleteAccountRefChange = ObjectTypeUtil.createPropertyModificationType(
				PropertyModificationTypeType.delete, null, SchemaConstants.I_ACCOUNT_REF, accountRef);

		return deleteAccountRefChange;
	}

	@SuppressWarnings("unchecked")
	private PropertyModificationType processAddAccountFromChanges(PropertyModificationType propertyChange,
			Object node, UserType user, OperationResult result) throws JAXBException,
			ObjectNotFoundException, PatchException, ObjectAlreadyExistsException, SchemaException {

		AccountShadowType account = XsdTypeConverter.toJavaValue(node, AccountShadowType.class);
		pushPasswordFromUserToAccount(user, account, result);

		ObjectModificationType accountChange = processOutboundSchemaHandling(user, account, result);
		if (accountChange != null) {
			PatchXml patchXml = new PatchXml();
			String accountXml = patchXml.applyDifferences(accountChange, account);
			account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(accountXml)).getValue();
		}

		String newAccountOid = getModelController().addObject(account, result);

		ObjectReferenceType accountRef = ModelUtils.createReference(newAccountOid, ObjectTypes.ACCOUNT);
		PropertyModificationType addAccountRefChange = ObjectTypeUtil.createPropertyModificationType(
				PropertyModificationTypeType.add, null, SchemaConstants.I_ACCOUNT_REF, accountRef);
		return addAccountRefChange;
	}

	private void processAddAssignments(UserType user, List<AssignmentType> assignments, Collection<String> excludedResourceOids, 
			OperationResult result) {
		Set<Delta<AssignmentType>> assignmentDeltas = new HashSet<Delta<AssignmentType>>();
		Delta<AssignmentType> delta = new Delta<AssignmentType>(Delta.DeltaType.ADD);
		assignmentDeltas.add(delta);
		for (AssignmentType assignment : assignments) {
			delta.add(assignment);
		}
		synchronizeUser(user, assignmentDeltas, excludedResourceOids, result);
	}
	
	private void processModifyAssignments(UserType user, ObjectModificationType change, Collection<String> excludedResourceOids,
			OperationResult result) {
		
		Set<Delta<AssignmentType>> assignmentDeltas = new HashSet<Delta<AssignmentType>>();
		Delta<AssignmentType> deleteDelta = new Delta<AssignmentType>(Delta.DeltaType.DELETE);
		assignmentDeltas.add(deleteDelta);
		Delta<AssignmentType> addDelta = new Delta<AssignmentType>(Delta.DeltaType.ADD);
		assignmentDeltas.add(addDelta);

		for (PropertyModificationType modification : change.getPropertyModification()) {
			LOGGER.trace("Processing modification {}", modification);
			if (ObjectTypeUtil.isModificationOf(modification,SchemaConstants.C_ASSIGNMENT)) {
				for(Object element : modification.getValue().getAny()) {
					
					AssignmentType assignment = JAXBUtil.fromElement(element, AssignmentType.class);
					
					LOGGER.trace("Processing {}", ObjectTypeUtil.toShortString(assignment));
					
					if (modification.getModificationType() == PropertyModificationTypeType.add) {
						addDelta.add(assignment);
					} else if (modification.getModificationType() == PropertyModificationTypeType.add) {
						deleteDelta.add(assignment);
					} else if (modification.getModificationType() == PropertyModificationTypeType.replace) {
						// TODO FIXME: this is nor really correct
						addDelta.add(assignment);
					}
				}	
			}
			
		}
		
		synchronizeUser(user, assignmentDeltas, excludedResourceOids, result);		
	}

	private void processDeleteAssignments(UserType user, List<AssignmentType> assignments, Collection<String> excludedResourceOids,
			OperationResult result) {
		Set<Delta<AssignmentType>> assignmentDeltas = new HashSet<Delta<AssignmentType>>();
		Delta<AssignmentType> delta = new Delta<AssignmentType>(Delta.DeltaType.DELETE);
		assignmentDeltas.add(delta);
		for (AssignmentType assignment : assignments) {
			delta.add(assignment);
		}
		synchronizeUser(user, assignmentDeltas, excludedResourceOids, result);
	}

	/**
	 * Synchronize what user should have and what he has.
	 * @param excludedResourceOids 
	 */
	private void synchronizeUser(UserType user, Set<Delta<AssignmentType>> assignmentDeltas, Collection<String> excludedResourceOids, OperationResult result) {
		LOGGER.trace("synchronizeUser {} deltas {}.",
				new Object[] { ObjectTypeUtil.toShortString(user), assignmentDeltas });
		
		resolveAllAccounts(user, result);

		for (AssignmentType assignment : user.getAssignment()) {
			
			applyAssignment(user, assignment, user, excludedResourceOids, result);
			
		}

		for (Delta<AssignmentType> delta : assignmentDeltas) {
			
			if (delta.getType() == Delta.DeltaType.ADD) {
				for (AssignmentType assignment : delta.getChange()) {
					applyAssignment(user, assignment, user, excludedResourceOids, result);
				}
			}
			
		}
		
		// The user is now polluted with both accountRef and account elements. Get rid of the accounts, leave just accountRefs.
		user.getAccount().clear();
		
	}
		
	private void resolveAllAccounts(UserType user, OperationResult result) {
		
		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			resolveAccount(user, accountRef, result);
		}
	}
	
	private void resolveAccount(UserType user, ObjectReferenceType accountRef, OperationResult result) {
		for (AccountShadowType account: user.getAccount()) {
			if (accountRef.getOid().equals(account.getOid())) {
				// Already resolved
				return;
			}
		}
		
		try {
			AccountShadowType account = getRepository().getObject(AccountShadowType.class, accountRef.getOid(), null, result);
			user.getAccount().add(account);
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logException(LOGGER, "Couldn't resolve account reference {} in {}", e,
					ObjectTypeUtil.toShortString(accountRef), ObjectTypeUtil.toShortString(user));
		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "Couldn't resolve account reference {} in {}", e,
					ObjectTypeUtil.toShortString(accountRef), ObjectTypeUtil.toShortString(user));
		}
		// TODO: better error handling
	}

	private void applyAssignment(UserType user, AssignmentType assignment, ObjectType containingObject, Collection<String> excludedResourceOids, OperationResult result) {

		LOGGER.trace("Applying {} ({})",ObjectTypeUtil.toShortString(assignment),ObjectTypeUtil.toShortString(user));
		
		OperationResult subResult = result.createSubresult("Apply assignment");
		subResult.addParam("user", user);
		subResult.addParam("assignment", assignment);
		subResult.addParam("exclusions", excludedResourceOids);
		
		if (!ModelUtils.isActivationEnabled(assignment.getActivation())) {
			// This assignment is not active, skip it
			LOGGER.debug("Skipping inactive {} in {}",ObjectTypeUtil.toShortString(assignment),ObjectTypeUtil.toShortString(containingObject));
			return;
		}			
		
		try {
			
			if (assignment.getAccountConstruction() != null) {
				
				processAccountConstruction(user, assignment.getAccountConstruction(), containingObject, excludedResourceOids, subResult);
				
			} else if (assignment.getTarget() != null) {
				
				applyAssignmentTarget(user, assignment, assignment.getTarget(), excludedResourceOids, subResult);
				
			} else if (assignment.getTargetRef() != null) {
				
				applyAssignmentTarget(user, assignment, assignment.getTargetRef(), excludedResourceOids, subResult);
				
			} else {
				
				LOGGER.warn("Empty assignment in "+ObjectTypeUtil.toShortString(containingObject));
				
			}
			
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't process {} in {}", ex,
					ObjectTypeUtil.toShortString(assignment), ObjectTypeUtil.toShortString(containingObject));
			subResult.recordFatalError(ex.getMessage());
		} finally {
			subResult.computeStatus();
		}
	}
	
	private void applyAssignmentTarget(UserType user, AssignmentType assignment,
			ObjectReferenceType targetRef, Collection<String> excludedResourceOids, OperationResult result) throws ObjectNotFoundException {
		
		// Target is referenced, need to fetch it
		Class<? extends ObjectType> clazz = ObjectType.class;
		if (assignment.getTargetRef().getType() != null) {
			clazz = ObjectTypes.getObjectTypeFromTypeQName(assignment.getTargetRef().getType()).getClassDefinition();
		}
		ObjectType target = getObject(clazz, assignment.getTargetRef().getOid(), new PropertyReferenceListType(), result);
		applyAssignmentTarget(user, assignment, target, excludedResourceOids, result);
		
	}

	private void applyAssignmentTarget(UserType user, AssignmentType assignment, ObjectType target,
			Collection<String> excludedResourceOids, OperationResult result) {
		
		if (target instanceof RoleType) {
			
			applyRoleAssignment(user, (RoleType) target, excludedResourceOids, result);
			
//		} else if (target instanceof AccountShadowType) {
//			
//			assignAccount(user, (AccountShadowType) target, result);

		} else {
			LOGGER.error("Unexpected assignment type "+target.getClass().getName()+", skipping it");
			result.recordPartialError("Unexpected assignment type "+target.getClass().getName()+", skipping it");
			return;
		}
		
	}

	private void applyRoleAssignment(UserType user, RoleType role, Collection<String> excludedResourceOids, OperationResult result) {
		LOGGER.debug("Applying {} ({})",
				new Object[] { ObjectTypeUtil.toShortString(role), ObjectTypeUtil.toShortString(user) });
		
		OperationResult subResult = result.createSubresult("Apply role");
		subResult.addParam(OperationResult.PARAM_OBJECT, role);
		subResult.addParam("exclusions", excludedResourceOids);
		
		for (AssignmentType assignment : role.getAssignment()) {
			try {
				applyAssignment(user, assignment, role, excludedResourceOids, subResult);
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't process role '{}' assignments ({})", ex,
						role.getName(), role.getAssignment().size());
			} finally {
				subResult.computeStatus("Couldn't process assignment for role '" + role.getName() + "'.",
						"Some minor problem occured while processing assignment for role '" + role.getName()
								+ "'.");
			}
		}
	}


	/**
	 * Method takes {@link AccountShadowType} from assignment. In case of
	 * {@link AssignOperation} ADD, checks if is not already assigned to user
	 * and if not it adds account to provisioning and as accountRef to user. In
	 * case of {@link AssignOperation} DELETE method deletes account from
	 * system.
	 */
	private void assignAccount(UserType user, AccountShadowType account, OperationResult result,
			AssignOperation operation) {
		LOGGER.debug("Processing account {} assignment for user {}.",
				new Object[] { account.getName(), user.getName() });
		if (isAccountAssigned(user, account, result)) {
			switch (operation) {
				case DELETE:
					try {
						getModelController().deleteObject(AccountShadowType.class, account.getOid(), result);
					} catch (Exception ex) {
						LoggingUtils.logException(LOGGER, "Couldn't delete account {} from user {}", ex,
								account.getName(), user.getName());
					}
				case ADD:
					return;
			}
		}

		try {
			if (account.getActivation() == null) {
				account.setActivation(user.getActivation());
			}
			// MID-72
			pushPasswordFromUserToAccount(user, account, result);

			String newAccountOid = getModelController().addObject(account, result);
			ObjectReferenceType accountRef = ModelUtils.createReference(newAccountOid, ObjectTypes.ACCOUNT);
			user.getAccountRef().add(accountRef);
		} catch (SystemException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SystemException("Couldn't process add account.", ex);
		}
	}

	/**
	 * This method check if parameter account is not already assigned to user
	 * selected in parameter user
	 * 
	 * @return true if account was created and assigned before, otherwise false
	 */
	private boolean isAccountAssigned(UserType user, AccountShadowType account, OperationResult result) {
		for (AccountShadowType existingAccount : user.getAccount()) {
			if (areAccountsEqual(account, existingAccount)) {
				return true;
			}
		}

		for (ObjectReferenceType accountRef : user.getAccountRef()) {
			if (!ObjectTypes.ACCOUNT.getQName().equals(accountRef.getType())) {
				continue;
			}

			try {
				AccountShadowType refferedAccount = getObject(AccountShadowType.class, accountRef.getOid(),
						ModelUtils.createPropertyReferenceListType("resource"), result);
				if (areAccountsEqual(account, refferedAccount)) {
					return true;
				}
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't get account with oid '{}'", ex,
						accountRef.getOid());
			}
		}

		return false;
	}

	/**
	 * Method compare two accounts based on account name and referenced resource
	 * (oid)
	 * 
	 * @return true if two accounts are equal, otherwise false
	 */
	private boolean areAccountsEqual(AccountShadowType first, AccountShadowType second) {
		if (!areResourcesEqual(first, second)) {
			return false;
		}

		if (!first.getName().equals(second.getName())) {
			return false;
		}

		return true;
	}

	/**
	 * Method provides test for comparing resource oid values for two accounts
	 * 
	 * @return true if resource definition oid's in both accounts are equal
	 */
	private boolean areResourcesEqual(AccountShadowType first, AccountShadowType second) {
		String firstOid = null;
		if (first.getResourceRef() != null) {
			firstOid = first.getResourceRef().getOid();
		} else if (first.getResource() != null) {
			firstOid = first.getResource().getOid();
		}

		String secondOid = null;
		if (second.getResourceRef() != null) {
			secondOid = second.getResourceRef().getOid();
		} else if (first.getResource() != null) {
			secondOid = second.getResource().getOid();
		}

		return firstOid != null ? firstOid.equals(secondOid) : secondOid == null;
	}
}
