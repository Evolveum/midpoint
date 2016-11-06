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
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.prism.delta.ChangeType.MODIFY;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType.WEAK;

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

	@Autowired
	private PrismContext prismContext;

	@Autowired
	private MappingFactory mappingFactory;

	@Autowired
	private MappingEvaluator mappingEvaluator;

	@Autowired
	private PasswordPolicyProcessor passwordPolicyProcessor;

	public <F extends FocusType> void processFocusCredentials(LensContext<F> context,
			XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException,
					ObjectNotFoundException, SchemaException, PolicyViolationException {
		processFocusPassword(context, now, task, result);
		processFocusNonce(context, now, task, result);
	}

	private <F extends FocusType> void processFocusPassword(LensContext<F> context, XMLGregorianCalendar now,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();

		processFocusCredentialsCommon(context,
				new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD), now, task, result);

		passwordPolicyProcessor.processPasswordPolicy(focusContext, context, task, result);
	}

	<F extends ObjectType> void processProjectionCredentials(LensContext<F> context,
			LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task,
			OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext != null && FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			@SuppressWarnings("unchecked")
			LensContext<? extends FocusType> contextOfFocus = (LensContext<? extends FocusType>) context;
			processProjectionPassword(contextOfFocus, projectionContext, now,
					task, result);
		}

		passwordPolicyProcessor.processPasswordPolicy(projectionContext, context, task, result);
	}
	
	//for now just saving metadata
	private <F extends FocusType> void processFocusNonce(LensContext<F> context, XMLGregorianCalendar now,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException {
		
		processFocusCredentialsCommon(context, SchemaConstants.PATH_NONCE, now, task, result);
//		
//
//		ObjectDelta<F> focusDelta = focusContext.getDelta();
//		
//		if (hasValueDelta(focusDelta, SchemaConstants.PATH_NONCE)) {
//			Collection<? extends ItemDelta<?, ?>> metaDeltas = LensUtil.createModifyMetadataDeltas(
//					context, SchemaConstants.PATH_NONCE.subPath(AbstractCredentialType.F_METADATA),
//					focusContext.getObjectDefinition(), now, task);
//			for (ItemDelta<?, ?> metaDelta : metaDeltas) {
//				context.getFocusContext().swallowToSecondaryDelta(metaDelta);
//			}
//		}

	}

	private <F extends FocusType> void processProjectionPassword(LensContext<F> context,
			final LensProjectionContext projCtx, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();

		PrismObject<F> userNew = focusContext.getObjectNew();
		if (userNew == null) {
			// This must be a user delete or something similar. No point in proceeding
			LOGGER.trace("userNew is null, skipping credentials processing");
			return;
		}

		PrismObjectDefinition<ShadowType> accountDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(ShadowType.class);
		PrismPropertyDefinition<ProtectedStringType> projPasswordPropertyDefinition = accountDefinition
				.findPropertyDefinition(SchemaConstants.PATH_PASSWORD_VALUE);

		ResourceShadowDiscriminator rsd = projCtx.getResourceShadowDiscriminator();

		RefinedObjectClassDefinition refinedProjDef = projCtx.getStructuralObjectClassDefinition();
		if (refinedProjDef == null) {
			LOGGER.trace("No RefinedObjectClassDefinition, therefore also no password outbound definition, skipping credentials processing for projection {}", rsd);
			return;
		}

		MappingType outboundMappingType = refinedProjDef.getPasswordOutbound();
		if (outboundMappingType == null) {
			LOGGER.trace("No outbound definition in password definition in credentials in account type {}, skipping credentials processing", rsd);
			return;
		}

		final ObjectDelta<ShadowType> projDelta = projCtx.getDelta();
		final PropertyDelta<ProtectedStringType> projPasswordDelta;
		if (projDelta != null && projDelta.getChangeType() == MODIFY) {
			projPasswordDelta = projDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
		} else {
			projPasswordDelta = null;
		}
		checkExistingDeltaSanity(projCtx, projPasswordDelta);

		if (outboundMappingType.getStrength() == WEAK && projPasswordDelta != null) {
			LOGGER.trace("Outbound password is weak and a priori projection password delta exists; skipping credentials processing for {}", rsd);
			return;
		}

		final ItemDeltaItem<PrismPropertyValue<PasswordType>, PrismPropertyDefinition<ProtectedStringType>> userPasswordIdi = focusContext
				.getObjectDeltaObject().findIdi(SchemaConstants.PATH_PASSWORD_VALUE);

		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			@Override
			public void setOutputPath(ItemPath outputPath) {
			}
			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
			}
			@Override
			public StringPolicyType resolve() {
				ValuePolicyType passwordPolicy = projCtx.getEffectivePasswordPolicy();
				if (passwordPolicy == null) {
					return null;
				}
				return passwordPolicy.getStringPolicy();
			}
		};

		Mapping<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>> mapping =
				mappingFactory.<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>>createMappingBuilder()
						.mappingType(outboundMappingType)
						.contextDescription("outbound password mapping in account type " + rsd)
						.defaultTargetDefinition(projPasswordPropertyDefinition)
						.defaultSource(new Source<>(userPasswordIdi, ExpressionConstants.VAR_INPUT))
						.sourceContext(focusContext.getObjectDeltaObject())
						.originType(OriginType.OUTBOUND)
						.originObject(projCtx.getResource())
						.stringPolicyResolver(stringPolicyResolver)
						.addVariableDefinitions(Utils.getDefaultExpressionVariables(context, projCtx).getMap())
						.build();

		if (!mapping.isApplicableToChannel(context.getChannel())) {
			return;
		}

		mappingEvaluator.evaluateMapping(mapping, context, task, result);
		final PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = mapping.getOutputTriple();

		// TODO review all this code !! MID-3156
		if (outputTriple == null) {
			LOGGER.trace("Credentials 'password' expression resulted in null output triple, skipping credentials processing for {}", rsd);
			return;
		}

		boolean projectionIsNew = projDelta != null && (projDelta.getChangeType() == ChangeType.ADD
				|| projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD);

		final Collection<PrismPropertyValue<ProtectedStringType>> newValues;
		if (outputTriple.hasPlusSet()) {
			newValues = outputTriple.getPlusSet();
		} else if (projectionIsNew) {
			// when adding new account, synchronize password regardless of whether the source data was changed or not.
			newValues = outputTriple.getNonNegativeValues();
		} else if (outputTriple.hasMinusSet()) {
			// Also, if the password value is to be removed, let's trigger its change (presumably to an empty value); except for WEAK mappings.
			// (We cannot check if the value being removed is the same as the current value. So let's take the risk and do it.)
			if (mapping.getStrength() != WEAK) {
				newValues = outputTriple.getNonNegativeValues();
			} else {
				LOGGER.trace("Credentials 'password' expression resulting in password deletion but the mapping is weak: skipping credentials processing for {}", rsd);
				return;
			}
		} else {
			// no plus set, no minus set
			LOGGER.trace("Credentials 'password' expression resulted in no change, skipping credentials processing for {}", rsd);
			return;
		}
		assert newValues != null;

		ItemDelta<?,?> projPasswordDeltaNew =
				DeltaBuilder.deltaFor(ShadowType.class, prismContext)
						.item(SchemaConstants.PATH_PASSWORD_VALUE).replace(newValues)
						.asItemDelta();
		LOGGER.trace("Adding new password delta for account {}", rsd);
		projCtx.swallowToSecondaryDelta(projPasswordDeltaNew);
	}

	private void checkExistingDeltaSanity(LensProjectionContext projCtx,
			PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
		if (passwordDelta != null && (passwordDelta.isAdd() || passwordDelta.isDelete())) {
			throw new SchemaException("Password for projection " + projCtx.getResourceShadowDiscriminator()
					+ " cannot be added or deleted, it can only be replaced");
		}
	}

	private <F extends FocusType> void processFocusCredentialsCommon(LensContext<F> context,
			ItemPath credentialsPath, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		LensFocusContext<F> focusContext = context.getFocusContext();
		PrismObject<F> focus = focusContext.getObjectAny();
		if (focusContext.isAdd()) {
			PrismContainer<AbstractCredentialType> credentialsContainer = focus
					.findContainer(credentialsPath);
			if (credentialsContainer != null) {
				for (PrismContainerValue<AbstractCredentialType> cVal : credentialsContainer.getValues()) {
					processCredentialsCommonAdd(focus, context, credentialsPath, cVal, now, task, result);
				}
			}
		} else if (focusContext.isModify()) {
			ObjectDelta<F> focusDelta = focusContext.getDelta();
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

		return passwordPolicy.getLifetime().getPasswordHistoryLength();
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
					.findContainer(new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD));
			if (password == null || password.isEmpty()) {
				return;
			}
			PrismContainer<PasswordHistoryEntryType> historyEntries = password
					.findOrCreateContainer(PasswordType.F_HISTORY_ENTRY);

			int maxPasswordsToSave = getMaxPasswordsToSave(context.getFocusContext(), context, task, result);
			
			List<PasswordHistoryEntryType> historyEntryValues = getSortedHistoryList(historyEntries);
			createDeleteHistoryDeltasIfNeeded(historyEntryValues, maxPasswordsToSave, context, task, result);
			if (maxPasswordsToSave > 0) {
				createAddHistoryDelta(context, password, now);
			}
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
			return new ArrayList<>();
		}
		List<PasswordHistoryEntryType> historyEntryValues = (List<PasswordHistoryEntryType>) historyEntries.getRealValues();

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
