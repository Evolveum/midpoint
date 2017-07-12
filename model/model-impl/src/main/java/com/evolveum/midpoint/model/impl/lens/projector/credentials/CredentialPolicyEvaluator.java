/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.stringpolicy.ObjectValuePolicyEvaluator;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PartiallyResolvedDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsStorageTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordHistoryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;


/**
 * Processor for evaluating credential policies. This class is processing the credential-related settings
 * of security policy: credential lifetime, history and so on. This class is supposed to be
 * quite generic. It should be able to operate on all credential types.
 * 
 * This class does NOT deal with value policies, validation and generation. That task is
 * delegated to ValuePolicyProcessor.
 * 
 * @author mamut
 * @author katkav
 * @author semancik
 *
 */
public abstract class CredentialPolicyEvaluator<R extends AbstractCredentialType, P extends CredentialPolicyType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(CredentialPolicyEvaluator.class);
	
	private static final ItemPath CREDENTIAL_RELATIVE_VALUE_PATH = new ItemPath(PasswordType.F_VALUE);
	
	// Configuration
	
	private PrismContext prismContext;
	
	private Protector protector;
	
	private OperationalDataManager metadataManager;
	
	private ValuePolicyProcessor valuePolicyProcessor;
	
	private ModelObjectResolver resolver;
	
	private LensContext<UserType> context;
	
	private XMLGregorianCalendar now;
	
	private Task task;
	
	private OperationResult result;
	
	// State
	
	private ObjectValuePolicyEvaluator objectValuePolicyEvaluator;
	private P credentialPolicy;
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public Protector getProtector() {
		return protector;
	}

	public void setProtector(Protector protector) {
		this.protector = protector;
	}

	public OperationalDataManager getMetadataManager() {
		return metadataManager;
	}

	public void setMetadataManager(OperationalDataManager metadataManager) {
		this.metadataManager = metadataManager;
	}

	public ValuePolicyProcessor getValuePolicyProcessor() {
		return valuePolicyProcessor;
	}

	public void setValuePolicyProcessor(ValuePolicyProcessor valuePolicyProcessor) {
		this.valuePolicyProcessor = valuePolicyProcessor;
	}

	public ModelObjectResolver getResolver() {
		return resolver;
	}

	public void setResolver(ModelObjectResolver resolver) {
		this.resolver = resolver;
	}

	public LensContext<UserType> getContext() {
		return context;
	}

	public void setContext(LensContext<UserType> context) {
		this.context = context;
	}

	public XMLGregorianCalendar getNow() {
		return now;
	}

	public void setNow(XMLGregorianCalendar now) {
		this.now = now;
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public OperationResult getResult() {
		return result;
	}

	public void setResult(OperationResult result) {
		this.result = result;
	}
	
	/**
	 * E.g. "credentials/password"
	 */
	protected abstract ItemPath getCredentialsContainerPath();
	
	/**
	 * E.g. "value"
	 */
	protected ItemPath getCredentialRelativeValuePath() {
		return CREDENTIAL_RELATIVE_VALUE_PATH;
	}

	/**
	 * E.g. "credentials/password/value"
	 */
	protected ItemPath getCredentialValuePath() {
		return getCredentialsContainerPath().subPath(getCredentialRelativeValuePath());
	}

	protected abstract String getCredentialHumanReadableName();
	
	protected boolean supportsHistory() {
		return false;
	}
	
	protected P getCredentialPolicy() throws SchemaException {
		if (credentialPolicy == null) {
			credentialPolicy = determineEffectiveCredentialPolicy();
		}
		return credentialPolicy;
	}
	
	protected abstract P determineEffectiveCredentialPolicy() throws SchemaException;
	
	protected SecurityPolicyType getSecurityPolicy() {
		return context.getFocusContext().getSecurityPolicy();
	}
	
	private ObjectValuePolicyEvaluator getObjectValuePolicyEvaluator() {
		if (objectValuePolicyEvaluator == null) {
			PrismObject<UserType> user = getUser();
			
			objectValuePolicyEvaluator = new ObjectValuePolicyEvaluator();
			objectValuePolicyEvaluator.setNow(now);
			objectValuePolicyEvaluator.setObject(user);
			objectValuePolicyEvaluator.setProtector(protector);
			objectValuePolicyEvaluator.setSecurityPolicy(getSecurityPolicy());
			objectValuePolicyEvaluator.setShortDesc(getCredentialHumanReadableName() + " for " + user);
			objectValuePolicyEvaluator.setTask(task);
			objectValuePolicyEvaluator.setValueItemPath(getCredentialValuePath());
			objectValuePolicyEvaluator.setValuePolicyProcessor(valuePolicyProcessor);
			
			PrismContainer<R> currentCredentialContainer = getOldCredentialContainer();
			if (currentCredentialContainer != null) {
				objectValuePolicyEvaluator.setOldCredentialType(currentCredentialContainer.getRealValue());
			}

		}
		return objectValuePolicyEvaluator;
	}

	
	private PrismObject<UserType> getUser() {
		return context.getFocusContext().getObjectAny();
	}

	public void process() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		PrismObject<UserType> focus = focusContext.getObjectAny();
		
		if (focusContext.isAdd()) {
			if (focusContext.wasAddExecuted()) {
				LOGGER.trace("Skipping processing {} policies. User addition was already executed.", getCredentialHumanReadableName());
				return;
			}
			PrismContainer<R> credentialsContainer = focus.findContainer(getCredentialsContainerPath());
			if (credentialsContainer != null) {
				for (PrismContainerValue<R> cVal : credentialsContainer.getValues()) {
					processCredentialContainerValue(focus, cVal);
				}
			}
		
		} else if (focusContext.isModify()) {
			boolean credentialValueChanged = false;
			ObjectDelta<UserType> focusDelta = focusContext.getDelta();
			ContainerDelta<R> containerDelta = focusDelta.findContainerDelta(getCredentialsContainerPath());
			if (containerDelta != null) {
				if (containerDelta.isAdd()) {
					for (PrismContainerValue<R> cVal : containerDelta.getValuesToAdd()) {
						credentialValueChanged = true;
						processCredentialContainerValue(focus, cVal);
					}
				}
				if (containerDelta.isReplace()) {
					for (PrismContainerValue<R> cVal : containerDelta.getValuesToReplace()) {
						credentialValueChanged = true;
						processCredentialContainerValue(focus, cVal);
					}
				}
			} else {
				if (hasValueDelta(focusDelta, getCredentialsContainerPath())) {
					credentialValueChanged = true;
					processValueDelta(focusDelta);
					addMetadataDelta();
				}
			}
			
			if (credentialValueChanged) {
				addHistoryDeltas();
			}
			
		} else if (focusContext.isDelete()) {
			LOGGER.trace("Skipping processing {} policies. User will be deleted.", getCredentialHumanReadableName());
			return;
		}
	}
	

	/**
	 * Process values from credential deltas that add/replace the whole container.
	 * E.g. $user/credentials/password, $user/credentials/securityQuestions   
	 */
	protected void processCredentialContainerValue(PrismObject<UserType> focus, PrismContainerValue<R> cVal)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		addMissingMetadata(cVal);
		validateCredentialContainerValues(cVal);
	}

	/**
	 * Process values from credential deltas that add/replace values below value container
	 * E.g. $user/credentials/password/value, 
	 *      $user/credentials/securityQuestions/questionAnswer
	 *      $user/credentials/securityQuestions/questionAnswer/questionAnswer
	 *      
	 *  This implementation is OK for the password, nonce and similar simple cases. It needs to be
	 *  overridden for more complex cases.
	 */
	protected void processValueDelta(ObjectDelta<UserType> focusDelta) throws PolicyViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		PropertyDelta<ProtectedStringType> valueDelta = focusDelta.findPropertyDelta(getCredentialValuePath());
		if (valueDelta == null) {
			LOGGER.trace("Skipping processing {} policies. User delta does not contain value change.", getCredentialHumanReadableName());
			return;
		}
		processPropertyValueCollection(valueDelta.getValuesToAdd());
		processPropertyValueCollection(valueDelta.getValuesToReplace());
	}
	
	private void processPropertyValueCollection(Collection<PrismPropertyValue<ProtectedStringType>> collection) throws PolicyViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		if (collection == null) {
			return;
		}
		for (PrismPropertyValue<ProtectedStringType> val: collection) {
			validateProtectedStringValue(val.getValue());
		}
	}

	protected void validateCredentialContainerValues(PrismContainerValue<R> cVal) throws PolicyViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		PrismProperty<ProtectedStringType> credentialValueProperty = cVal.findProperty(getCredentialRelativeValuePath());
		if (credentialValueProperty != null) {
			for (PrismPropertyValue<ProtectedStringType> credentialValuePropertyValue : credentialValueProperty.getValues()) {
				validateProtectedStringValue(credentialValuePropertyValue.getValue());
			}
		}
	}

	protected void validateProtectedStringValue(ProtectedStringType value) throws PolicyViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		OperationResult validationResult = getObjectValuePolicyEvaluator().validateProtectedStringValue(value);
		result.addSubresult(validationResult);
		
		if (!validationResult.isAcceptable()) {
			throw new PolicyViolationException("Provided "+getCredentialHumanReadableName()+" does not satisfy the policies: " + validationResult.getMessage());
		}
	}

	private void addMetadataDelta() throws SchemaException {
		Collection<? extends ItemDelta<?, ?>> metaDeltas = metadataManager.createModifyMetadataDeltas(
				context, getCredentialsContainerPath().subPath(AbstractCredentialType.F_METADATA),
				context.getFocusContext().getObjectDefinition(), now, task);
		for (ItemDelta<?, ?> metaDelta : metaDeltas) {
			context.getFocusContext().swallowToSecondaryDelta(metaDelta);
		}
	}
	
	private void addMissingMetadata(PrismContainerValue<R> cVal)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		if (hasValueChange(cVal)) {
			if (!hasMetadata(cVal)) {
				MetadataType metadataType = metadataManager.createCreateMetadata(context, now, task);
				ContainerDelta<MetadataType> metadataDelta = ContainerDelta.createModificationAdd(
						getCredentialsContainerPath().subPath(AbstractCredentialType.F_METADATA), UserType.class, prismContext,
						metadataType);
				context.getFocusContext().swallowToSecondaryDelta(metadataDelta);
			}
		}
	}
	
	private <F extends FocusType> boolean hasValueDelta(ObjectDelta<UserType> focusDelta, ItemPath credentialsPath) {
		if (focusDelta == null) {
			return false;
		}
		for (PartiallyResolvedDelta<PrismValue, ItemDefinition> partialDelta : focusDelta.findPartial(credentialsPath)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Residual delta:\n{}", partialDelta.debugDump());
			}
			ItemPath residualPath = partialDelta.getResidualPath();
			if (residualPath == null || residualPath.isEmpty()) {
				continue;
			}
			LOGGER.trace("PATH: {}", residualPath);
			QName name = ItemPath.getFirstName(residualPath);
			LOGGER.trace("NAME: {}", name);
			if (isValueElement(name)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean hasValueChange(PrismContainerValue<R> cVal) {
		for (Item<?, ?> item : cVal.getItems()) {
			QName itemName = item.getElementName();
			if (isValueElement(itemName)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isValueElement(QName itemName) {
		return !itemName.equals(AbstractCredentialType.F_FAILED_LOGINS)
				&& !itemName.equals(AbstractCredentialType.F_LAST_FAILED_LOGIN)
				&& !itemName.equals(AbstractCredentialType.F_LAST_SUCCESSFUL_LOGIN)
				&& !itemName.equals(AbstractCredentialType.F_METADATA)
				&& !itemName.equals(AbstractCredentialType.F_PREVIOUS_SUCCESSFUL_LOGIN);
	}
	
	private boolean hasMetadata(PrismContainerValue<R> cVal) {
		for (Item<?, ?> item : cVal.getItems()) {
			QName itemName = item.getElementName();
			if (itemName.equals(AbstractCredentialType.F_METADATA)) {
				return true;
			}
		}
		return false;
	}
	
	protected PrismContainer<R> getOldCredentialContainer() {
		PrismObject<UserType> objectOld = context.getFocusContext().getObjectOld();
		if (objectOld == null) {
			return null;
		}
		return objectOld.findContainer(getCredentialsContainerPath());
	}
		
	private void addHistoryDeltas() throws SchemaException {
		if (!supportsHistory()) {
			return;
		}
		int historyLength = SecurityUtil.getCredentialHistoryLength(getCredentialPolicy());
		
		
		PrismContainer<R> oldCredentialContainer = getOldCredentialContainer();
		if (oldCredentialContainer == null) {
			return;
		}
			
		int addedValues = 0;
		// Note: historyLength=1 means that we need just compare with current password
		// The real number of values stored in the history is historyLength-1
		if (historyLength > 1) {
			addedValues = createAddHistoryDelta(oldCredentialContainer);
		}
		
		createDeleteHistoryDeltasIfNeeded(historyLength, addedValues, oldCredentialContainer);		
	}
	
	// TODO: generalize for other credentials
	private <F extends FocusType> int createAddHistoryDelta(PrismContainer<R> oldCredentialContainer) throws SchemaException {
		R oldCredentialContainerType = oldCredentialContainer.getValue().asContainerable();
		MetadataType oldCredentialMetadata = oldCredentialContainerType.getMetadata();
		
		PrismProperty<ProtectedStringType> oldValueProperty = oldCredentialContainer.findProperty(getCredentialRelativeValuePath());
		if (oldValueProperty == null) {
			return 0;
		}
		ProtectedStringType newHistoryValue = oldValueProperty.getRealValue();
		ProtectedStringType passwordPsForStorage = newHistoryValue.clone();
		
		CredentialsStorageTypeType storageType = SecurityUtil.getCredentialStoragetTypeType(getCredentialPolicy().getHistoryStorageMethod());
		if (storageType == null) {
			storageType = CredentialsStorageTypeType.HASHING;
		}
		prepareProtectedStringForStorage(passwordPsForStorage, storageType);
		
		PrismContainerDefinition<PasswordHistoryEntryType> historyEntryDefinition = oldCredentialContainer.getDefinition().findContainerDefinition(PasswordType.F_HISTORY_ENTRY);
		PrismContainer<PasswordHistoryEntryType> historyEntry = historyEntryDefinition.instantiate();
		
		PrismContainerValue<PasswordHistoryEntryType> hisotryEntryValue = historyEntry.createNewValue();
		
		PasswordHistoryEntryType entryType = hisotryEntryValue.asContainerable();
		entryType.setValue(passwordPsForStorage);
		entryType.setMetadata(oldCredentialMetadata==null?null:oldCredentialMetadata.clone());
		entryType.setChangeTimestamp(now);
	
		ContainerDelta<PasswordHistoryEntryType> addHisotryDelta = ContainerDelta
				.createModificationAdd(new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_HISTORY_ENTRY), UserType.class, prismContext, entryType.clone());
		context.getFocusContext().swallowToSecondaryDelta(addHisotryDelta);
		
		return 1;
	}
	
	// TODO: generalize for other credentials
	private <F extends FocusType> void createDeleteHistoryDeltasIfNeeded(int historyLength, int addedValues, PrismContainer<R> currentCredentialContainer) throws SchemaException {

		PrismContainer<PasswordHistoryEntryType> historyEntries = currentCredentialContainer.findOrCreateContainer(PasswordType.F_HISTORY_ENTRY);
		List<PrismContainerValue<PasswordHistoryEntryType>> historyEntryValues = historyEntries.getValues();
				
		if (historyEntries.size() == 0) {
			return;
		}

		// We need to delete one more entry than intuitively expected - because we are computing from the history entries 
		// in the old object. In the new object there will be one new history entry for the changed password.
		int numberOfHistoryEntriesToDelete = historyEntries.size() - historyLength + addedValues + 1;
		
		for (int i = 0; i < numberOfHistoryEntriesToDelete; i++) {
			ContainerDelta<PasswordHistoryEntryType> deleteHistoryDelta = ContainerDelta
					.createModificationDelete(
							new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD,
									PasswordType.F_HISTORY_ENTRY),
							UserType.class, prismContext,
							historyEntryValues.get(i).clone());
			context.getFocusContext().swallowToSecondaryDelta(deleteHistoryDelta);
		}
	}
	
	private void prepareProtectedStringForStorage(ProtectedStringType ps, CredentialsStorageTypeType storageType) throws SchemaException {
		try {
			switch (storageType) {
				case ENCRYPTION: 
					if (ps.isEncrypted()) {
						break;
					}
					if (ps.isHashed()) {
						throw new SchemaException("Cannot store hashed value in an encrypted form");
					}
					protector.encrypt(ps);
					break;
					
				case HASHING:
					if (ps.isHashed()) {
						break;
					}
					protector.hash(ps);
					break;
					
				case NONE:
					throw new SchemaException("Cannot store value on NONE storage form");
					
				default:
					throw new SchemaException("Unknown storage type: "+storageType);
			}
		} catch (EncryptionException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

}
