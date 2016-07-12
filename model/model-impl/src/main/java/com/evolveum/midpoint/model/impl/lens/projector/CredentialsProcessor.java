/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PartiallyResolvedDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordHistoryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Processor that takes password from user and synchronizes it to accounts.
 * <p/>
 * The implementation is very simple now. It only cares about password value,
 * not expiration or other password facets. It completely ignores other
 * credential types.
 *
 * @author Radovan Semancik
 */
@Component
public class CredentialsProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(CredentialsProcessor.class);

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private MappingFactory mappingFactory;

	@Autowired(required = true)
	private MappingEvaluator mappingEvaluator;

	@Autowired(required = true)
	private PasswordPolicyProcessor passwordPolicyProcessor;

	public <F extends ObjectType> void processFocusCredentials(LensContext<F> context,
			XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException,
					ObjectNotFoundException, SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext != null && FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			processFocusPassword((LensContext<? extends FocusType>) context, now, task, result);
		}
	}

	private <F extends FocusType> void processFocusPassword(LensContext<F> context, XMLGregorianCalendar now,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();

		processFocusCredentialsCommon(context,
				new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD), now, task, result);

		passwordPolicyProcessor.processPasswordPolicy(focusContext, context, task, result);
	}

	public <F extends ObjectType> void processProjectionCredentials(LensContext<F> context,
			LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task,
			OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext != null && FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			processProjectionPassword((LensContext<? extends FocusType>) context, projectionContext, now,
					task, result);
		}

		passwordPolicyProcessor.processPasswordPolicy(projectionContext, context, task, result);
	}

	private <F extends FocusType> void processProjectionPassword(LensContext<F> context,
			final LensProjectionContext accCtx, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		ObjectDelta<F> focusDelta = focusContext.getDelta();

		PropertyDelta<PasswordType> userPasswordValueDelta = null;
		if (focusDelta != null) {
			userPasswordValueDelta = focusDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
		}

		PrismObject<F> userNew = focusContext.getObjectNew();
		if (userNew == null) {
			// This must be a user delete or something similar. No point in
			// proceeding
			LOGGER.trace("userNew is null, skipping credentials processing");
			return;
		}

		PrismObjectDefinition<ShadowType> accountDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(ShadowType.class);
		PrismPropertyDefinition<ProtectedStringType> accountPasswordPropertyDefinition = accountDefinition
				.findPropertyDefinition(SchemaConstants.PATH_PASSWORD_VALUE);

		ResourceShadowDiscriminator rat = accCtx.getResourceShadowDiscriminator();

		ObjectDelta<ShadowType> accountDelta = accCtx.getDelta();
		PropertyDelta<ProtectedStringType> accountPasswordValueDelta = null;
		if (accountDelta != null) {
			accountPasswordValueDelta = accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
		}
		if (accountDelta != null && accountDelta.getChangeType() == ChangeType.MODIFY) {
			if (accountPasswordValueDelta != null
					&& (accountPasswordValueDelta.isAdd() || accountDelta.isDelete())) {
				throw new SchemaException("Password for account " + rat
						+ " cannot be added or deleted, it can only be replaced");
			}
		}
		if (accountDelta != null && (accountDelta.getChangeType() == ChangeType.ADD
				|| accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD)) {
			// adding new account, synchronize password regardless whether the
			// password was changed or not.
		} else if (userPasswordValueDelta != null) {
			// user password was changed. synchronize it regardless of the
			// account change.
		} else {
			LOGGER.trace(
					"No change in password and the account is not added, skipping credentials processing for account "
							+ rat);
			return;
		}

		RefinedObjectClassDefinition refinedAccountDef = accCtx.getStructuralObjectClassDefinition();
		if (refinedAccountDef == null) {
			LOGGER.trace(
					"No RefinedAccountDefinition, therefore also no password outbound definition, skipping credentials processing for account "
							+ rat);
			return;
		}

		MappingType outboundMappingType = refinedAccountDef.getPasswordOutbound();

		if (outboundMappingType == null) {
			LOGGER.trace(
					"No outbound definition in password definition in credentials in account type {}, skipping credentials processing",
					rat);
			return;
		}

		Mapping<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>> passwordMapping = mappingFactory
				.createMapping(outboundMappingType, "outbound password mapping in account type " + rat);
		if (!passwordMapping.isApplicableToChannel(context.getChannel())) {
			return;
		}

		passwordMapping.setDefaultTargetDefinition(accountPasswordPropertyDefinition);
		ItemDeltaItem<PrismPropertyValue<PasswordType>, PrismPropertyDefinition<ProtectedStringType>> userPasswordIdi = focusContext
				.getObjectDeltaObject().findIdi(SchemaConstants.PATH_PASSWORD_VALUE);
		Source<PrismPropertyValue<PasswordType>, PrismPropertyDefinition<ProtectedStringType>> source = new Source<>(
				userPasswordIdi, ExpressionConstants.VAR_INPUT);
		passwordMapping.setDefaultSource(source);
		passwordMapping.setOriginType(OriginType.OUTBOUND);
		passwordMapping.setOriginObject(accCtx.getResource());

		if (passwordMapping.getStrength() != MappingStrengthType.STRONG) {
			if (accountPasswordValueDelta != null && !accountPasswordValueDelta.isEmpty()) {
				return;
			}
		}

		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			private ItemPath outputPath;
			private ItemDefinition outputDefinition;

			@Override
			public void setOutputPath(ItemPath outputPath) {
				this.outputPath = outputPath;
			}

			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
				this.outputDefinition = outputDefinition;
			}

			@Override
			public StringPolicyType resolve() {
				ValuePolicyType passwordPolicy = accCtx.getEffectivePasswordPolicy();
				if (passwordPolicy == null) {
					return null;
				}
				return passwordPolicy.getStringPolicy();
			}
		};
		passwordMapping.setStringPolicyResolver(stringPolicyResolver);

		mappingEvaluator.evaluateMapping(passwordMapping, context, task, result);

		// TODO review all this code !! MID-3156
		// Originally here was "do nothing if output is null or empty".
		// But because of MID-3111 we have to be a bit more cautious
		PrismProperty<ProtectedStringType> accountPasswordNew = (PrismProperty) passwordMapping.getOutput();
		if (accountPasswordNew == null || accountPasswordNew.isEmpty()) {
			if (passwordMapping.getOutputTriple() == null) {
				LOGGER.trace(
						"Credentials 'password' expression resulted in null output triple, skipping credentials processing for {}",
						rat);
				return;
			}
			if (passwordMapping.getStrength() != MappingStrengthType.STRONG) {
				LOGGER.trace(
						"Credentials 'password' expression resulted in 'no value' via non-strong mapping, skipping credentials processing for {}",
						rat);
				return;
			}
		}
		PropertyDelta<ProtectedStringType> accountPasswordDeltaNew = new PropertyDelta<>(
				SchemaConstants.PATH_PASSWORD_VALUE, accountPasswordPropertyDefinition, prismContext);
		if (accountPasswordNew != null) {
			accountPasswordDeltaNew.setValuesToReplace(accountPasswordNew.getClonedValues());
		} else {
			accountPasswordDeltaNew
					.setValuesToReplace(Collections.<PrismPropertyValue<ProtectedStringType>> emptyList());
		}
		LOGGER.trace("Adding new password delta for account {}", rat);
		accCtx.swallowToSecondaryDelta(accountPasswordDeltaNew);

	}

	private <F extends FocusType> void processFocusCredentialsCommon(LensContext<F> context,
			ItemPath credentialsPath, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		LensFocusContext<F> focusContext = context.getFocusContext();

		ObjectDelta<F> focusDelta = focusContext.getDelta();
		PrismObject<F> focus = focusContext.getObjectAny();
//		if (focus == null) {
//			focus = focusContext.getObjectCurrent();
//		}
//		if (focus == null) {
//			focus = focusContext.getObjectNew();
//		}
		if (focusContext.isAdd()) {
			focus = focusContext.getObjectAny();
			PrismContainer<AbstractCredentialType> credentialsContainer = focus
					.findContainer(credentialsPath);
			if (credentialsContainer != null) {
				for (PrismContainerValue<AbstractCredentialType> cVal : credentialsContainer.getValues()) {
					processCredentialsCommonAdd(focus, context, credentialsPath, cVal, now, task, result);
				}
			}
		} else if (focusContext.isModify()) {
			ContainerDelta<AbstractCredentialType> containerDelta = focusDelta
					.findContainerDelta(credentialsPath);
			if (containerDelta != null) {
				if (containerDelta.isAdd()) {
					for (PrismContainerValue<AbstractCredentialType> cVal : containerDelta.getValuesToAdd()) {
						processCredentialsCommonAdd(focus, context, credentialsPath, cVal, now, task, result);
					}
				}
				if (containerDelta.isReplace()) {
					for (PrismContainerValue<AbstractCredentialType> cVal : containerDelta
							.getValuesToReplace()) {
						processCredentialsCommonAdd(focus, context, credentialsPath, cVal, now, task, result);
					}
				}
			} else {
				if (hasValueDelta(focusDelta, credentialsPath)) {
					Collection<? extends ItemDelta<?, ?>> metaDeltas = LensUtil.createModifyMetadataDeltas(
							context, credentialsPath.subPath(AbstractCredentialType.F_METADATA),
							focusContext.getObjectDefinition(), now, task);
					for (ItemDelta<?, ?> metaDelta : metaDeltas) {
						context.getFocusContext().swallowToSecondaryDelta(metaDelta);
					}
					
					
				}
			}
		}
		if (focusContext.isDelete()){
			return;
		}
		processPasswordHistoryDeltas(focus, context, now, task, result);
	}

	private <F extends FocusType> int getMaxPasswordsToSave(LensFocusContext<F> focusContext,
			LensContext<F> context, Task task, OperationResult result) throws SchemaException {
		ValuePolicyType passwordPolicy;
		if (focusContext.getOrgPasswordPolicy() == null) {
			passwordPolicy = passwordPolicyProcessor.determineValuePolicy(focusContext.getDelta(),
					focusContext.getObjectAny(), context, task, result);
			focusContext.setOrgPasswordPolicy(passwordPolicy);
		} else {
			passwordPolicy = focusContext.getOrgPasswordPolicy();
		}

		if (passwordPolicy == null) {
			return 0;
		}

		if (passwordPolicy.getLifetime() == null) {
			return 0;
		}

		if (passwordPolicy.getLifetime().getPasswordHistoryLength() == null) {
			return 0;
		}

		return passwordPolicy.getLifetime().getPasswordHistoryLength().intValue();
	}

	private <F extends FocusType> boolean hasValueDelta(ObjectDelta<F> focusDelta, ItemPath credentialsPath) {
		if (focusDelta == null) {
			return false;
		}
		for (PartiallyResolvedDelta<PrismValue, ItemDefinition> partialDelta : focusDelta
				.findPartial(credentialsPath)) {
			LOGGER.trace("Residual delta:\n{}", partialDelta.debugDump());
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

	private <F extends FocusType> void processCredentialsCommonAdd(PrismObject<F> focus,
			LensContext<F> context, ItemPath credentialsPath,
			PrismContainerValue<AbstractCredentialType> cVal, XMLGregorianCalendar now, Task task,
			OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		if (hasValueChange(cVal) && !hasMetadata(cVal)) {
			MetadataType metadataType = LensUtil.createCreateMetadata(context, now, task);
			ContainerDelta<MetadataType> metadataDelta = ContainerDelta.createModificationAdd(
					credentialsPath.subPath(AbstractCredentialType.F_METADATA), UserType.class, prismContext,
					metadataType);
			context.getFocusContext().swallowToSecondaryDelta(metadataDelta);

		}
	}

	private <F extends FocusType> void processPasswordHistoryDeltas(PrismObject<F> focus,
			LensContext<F> context, XMLGregorianCalendar now, Task task, OperationResult result)
					throws SchemaException {
		Validate.notNull(focus, "Focus object must not be null");
		if (focus.getCompileTimeClass().equals(UserType.class)) {
			PrismContainer<PasswordType> password = focus
					.findOrCreateContainer(new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD));
			if (password == null || password.isEmpty()) {
				return;
			}
			PrismContainer<PasswordHistoryEntryType> historyEntries = password
					.findOrCreateContainer(PasswordType.F_HISTORY_ENTRY);

			int maxPasswordsToSave = getMaxPasswordsToSave(context.getFocusContext(), context, task, result);
			
			List<PasswordHistoryEntryType> historyEntryValues = getSortedHistoryList(historyEntries);
			createDeleteHistoryDeltasIfNeeded(historyEntryValues, maxPasswordsToSave, context, task, result);
			if (maxPasswordsToSave == 0) {
				return;
			}
			createAddHistoryDelta(context, password, now);

		}

	}

	private <F extends FocusType> void createAddHistoryDelta(LensContext<F> context,
			PrismContainer<PasswordType> password, XMLGregorianCalendar now) throws SchemaException {
		PrismContainerValue<PasswordType> passwordValue = password.getValue();
		PasswordType passwordRealValue = passwordValue.asContainerable();
		
		PrismContainerDefinition<PasswordHistoryEntryType> historyEntryDefinition = password.getDefinition().findContainerDefinition(PasswordType.F_HISTORY_ENTRY);
		PrismContainer<PasswordHistoryEntryType> historyEntry = historyEntryDefinition.instantiate();
		
		PrismContainerValue<PasswordHistoryEntryType> hisotryEntryValue = historyEntry.createNewValue();
		
		PasswordHistoryEntryType entryType = hisotryEntryValue.asContainerable();
		entryType.setValue(passwordRealValue.getValue());
		entryType.setMetadata(passwordRealValue.getMetadata());
		entryType.setChangeTimestamp(now);
	
		ContainerDelta<PasswordHistoryEntryType> addHisotryDelta = ContainerDelta
				.createModificationAdd(new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_HISTORY_ENTRY), UserType.class, prismContext, entryType.clone());
		context.getFocusContext().swallowToSecondaryDelta(addHisotryDelta);

	}

	private <F extends FocusType> void createDeleteHistoryDeltasIfNeeded(
			List<PasswordHistoryEntryType> historyEntryValues, int maxPasswordsToSave, LensContext<F> context, Task task,
			OperationResult result) throws SchemaException {
		
		if (historyEntryValues.size() == 0) {
			return;
		}

		int numberOfHistoryEntriesToDelete = historyEntryValues.size() - maxPasswordsToSave;
		
		if (numberOfHistoryEntriesToDelete >= 0) {
			for (int i = 0; i <= numberOfHistoryEntriesToDelete; i++) {
				ContainerDelta<PasswordHistoryEntryType> deleteHistoryDelta = ContainerDelta
						.createModificationDelete(
								new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD,
										PasswordType.F_HISTORY_ENTRY),
								UserType.class, prismContext,
								historyEntryValues.get(i).clone());
				context.getFocusContext().swallowToSecondaryDelta(deleteHistoryDelta);
			}
		}

	}

	private List<PasswordHistoryEntryType> getSortedHistoryList(
			PrismContainer<PasswordHistoryEntryType> historyEntries) {
		if (historyEntries.isEmpty()) {
			return new ArrayList<PasswordHistoryEntryType>();
		}
		List<PasswordHistoryEntryType> historyEntryValues = PrismContainerValue
				.fromPcvList(historyEntries.getValues());

		Collections.sort(historyEntryValues, new Comparator<PasswordHistoryEntryType>() {

			@Override
			public int compare(PasswordHistoryEntryType o1, PasswordHistoryEntryType o2) {
				XMLGregorianCalendar changeTimestampFirst = o1.getChangeTimestamp();
				XMLGregorianCalendar changeTimestampSecond = o2.getChangeTimestamp();

				return changeTimestampFirst.compare(changeTimestampSecond);
			}
		});
		return historyEntryValues;
	}

	private boolean hasValueChange(PrismContainerValue<AbstractCredentialType> cVal) {
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

	private boolean hasMetadata(PrismContainerValue<AbstractCredentialType> cVal) {
		for (Item<?, ?> item : cVal.getItems()) {
			QName itemName = item.getElementName();
			if (itemName.equals(AbstractCredentialType.F_METADATA)) {
				return true;
			}
		}
		return false;
	}

}
