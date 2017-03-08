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
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SchemaFailableProcessor;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
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
import java.util.function.Consumer;

import static com.evolveum.midpoint.prism.delta.ChangeType.MODIFY;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType.WEAK;

/**
 * Processor that takes password from user and synchronizes it to accounts.
 *
 * @author Radovan Semancik
 */
@Component
public class CredentialsProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(CredentialsProcessor.class);

	@Autowired(required=true)
	private PrismContext prismContext;

	@Autowired(required=true)
	private MappingFactory mappingFactory;

	@Autowired(required=true)
	private MappingEvaluator mappingEvaluator;

	@Autowired(required=true)
	private PasswordPolicyProcessor passwordPolicyProcessor;
	
	@Autowired(required=true)
	private OperationalDataManager metadataManager;
	
	@Autowired(required = true)
	private SecurityHelper securityHelper;
	
	@Autowired(required = true)
	Protector protector;

	public <F extends FocusType> void processFocusCredentials(LensContext<F> context,
			XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException,
					ObjectNotFoundException, SchemaException, PolicyViolationException {
		processSecurityPolicy(context, now, task, result);
		processFocusPassword(context, now, task, result);
		processFocusNonce(context, now, task, result);
	}
	
	private <F extends FocusType> void processSecurityPolicy(LensContext<F> context, XMLGregorianCalendar now,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		SecurityPolicyType securityPolicy = focusContext.getSecurityPolicy();
		if (securityPolicy == null) {
			securityPolicy = securityHelper.locateSecurityPolicy(focusContext.getObjectAny(), context.getSystemConfiguration(), task, result);
			if (securityPolicy == null) {
				// store empty policy to avoid repeated lookups
				securityPolicy = new SecurityPolicyType();
			}
			focusContext.setSecurityPolicy(securityPolicy);
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Security policy:\n{}", securityPolicy==null?null:securityPolicy.asPrismObject().debugDump(1));
		} else {
			LOGGER.debug("Security policy: {}", securityPolicy);
		}
	}
		
	private <F extends FocusType> void processFocusPassword(LensContext<F> context, XMLGregorianCalendar now,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();

		processFocusCredentialsCommon(context,
				new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD),
				now, task, result);
		

		passwordPolicyProcessor.processPasswordPolicy(focusContext, context, now, task, result);
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
				ValuePolicyType passwordPolicy = passwordPolicyProcessor.determinePasswordPolicy(context, projCtx, task, result);
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

	private <F extends FocusType, R extends AbstractCredentialType> void processFocusCredentialsCommon(LensContext<F> context,
			ItemPath credentialsPath, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		LensFocusContext<F> focusContext = context.getFocusContext();
		PrismObject<F> focus = focusContext.getObjectAny();
		if (focusContext.isAdd()) {
			PrismContainer<R> credentialsContainer = focus
					.findContainer(credentialsPath);
			if (credentialsContainer != null) {
				for (PrismContainerValue<R> cVal : credentialsContainer.getValues()) {
					// null lambda here. This is not a change.
					processCredentialsCommonAdd(focus, context, credentialsPath, cVal, now, task, result);
				}
			}
		} else if (focusContext.isModify()) {
			ObjectDelta<F> focusDelta = focusContext.getDelta();
			ContainerDelta<R> containerDelta = focusDelta
					.findContainerDelta(credentialsPath);
			if (containerDelta != null) {
				if (containerDelta.isAdd()) {
					for (PrismContainerValue<R> cVal : containerDelta.getValuesToAdd()) {
						processCredentialsCommonAdd(focus, context, credentialsPath, cVal, now, task, result);
					}
				}
				if (containerDelta.isReplace()) {
					for (PrismContainerValue<R> cVal : containerDelta
							.getValuesToReplace()) {
						processCredentialsCommonAdd(focus, context, credentialsPath, cVal, now, task, result);
					}
				}
			} else {
				if (hasValueDelta(focusDelta, credentialsPath)) {
					Collection<? extends ItemDelta<?, ?>> metaDeltas = metadataManager.createModifyMetadataDeltas(
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
	}

	private <F extends FocusType> boolean hasValueDelta(ObjectDelta<F> focusDelta, ItemPath credentialsPath) {
		if (focusDelta == null) {
			return false;
		}
		for (PartiallyResolvedDelta<PrismValue, ItemDefinition> partialDelta : focusDelta
				.findPartial(credentialsPath)) {
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

	private <F extends FocusType, R extends AbstractCredentialType> void processCredentialsCommonAdd(PrismObject<F> focus,
			LensContext<F> context, ItemPath credentialsPath,
			PrismContainerValue<R> cVal, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		if (hasValueChange(cVal)) {
			if (!hasMetadata(cVal)) {
				MetadataType metadataType = metadataManager.createCreateMetadata(context, now, task);
				ContainerDelta<MetadataType> metadataDelta = ContainerDelta.createModificationAdd(
						credentialsPath.subPath(AbstractCredentialType.F_METADATA), UserType.class, prismContext,
						metadataType);
				context.getFocusContext().swallowToSecondaryDelta(metadataDelta);
			}
			
		}
	}


	private <R extends AbstractCredentialType> boolean hasValueChange(PrismContainerValue<R> cVal) {
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

	private <R extends AbstractCredentialType> boolean hasMetadata(PrismContainerValue<R> cVal) {
		for (Item<?, ?> item : cVal.getItems()) {
			QName itemName = item.getElementName();
			if (itemName.equals(AbstractCredentialType.F_METADATA)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Called from ChangeExecutor. Will modify the execution deltas to hash or remove credentials if needed.
	 */
	public <O extends ObjectType> ObjectDelta<O> transformFocusExectionDelta(LensContext<O> context, ObjectDelta<O> focusDelta) throws SchemaException, EncryptionException {
		LensFocusContext<O> focusContext = context.getFocusContext();
		SecurityPolicyType securityPolicy = focusContext.getSecurityPolicy();
		if (securityPolicy == null) {
			return focusDelta;
		}
		CredentialsPolicyType credsType = securityPolicy.getCredentials();
		if (credsType == null) {
			return focusDelta;
		}
		ObjectDelta<O> transformedDelta = focusDelta.clone();
		transformFocusExectionDeltaCredential(context, credsType, credsType.getPassword(), SchemaConstants.PATH_PASSWORD_VALUE, transformedDelta);
		// TODO: nonce and others
		
		return transformedDelta;
	}

	private <O extends ObjectType> void transformFocusExectionDeltaCredential(LensContext<O> context,
			CredentialsPolicyType credsType, CredentialPolicyType credPolicyType,
			ItemPath valuePropertyPath, ObjectDelta<O> delta) throws SchemaException, EncryptionException {
		if (delta.isDelete()) {
			return;
		}
		CredentialPolicyType defaltCredPolicyType = credsType.getDefault();
		CredentialsStorageMethodType storageMethod = null;
		if (credPolicyType != null && credPolicyType.getStorageMethod() != null) {
			storageMethod = credPolicyType.getStorageMethod();
		} else if (defaltCredPolicyType != null && defaltCredPolicyType.getStorageMethod() != null) {
			storageMethod = defaltCredPolicyType.getStorageMethod();
		}
		if (storageMethod == null) {
			return;
		}
		CredentialsStorageTypeType storageType = storageMethod.getStorageType();
		if (storageType == null || storageType == CredentialsStorageTypeType.ENCRYPTION) {
			return;
		} else if (storageType == CredentialsStorageTypeType.HASHING) {
			PrismPropertyValue<ProtectedStringType> pval = null;
			if (delta.isAdd()) {
				PrismProperty<ProtectedStringType> prop = delta.getObjectToAdd().findProperty(valuePropertyPath);
				hashValues(prop.getValues(), storageMethod);
			} else {
				PropertyDelta<ProtectedStringType> propDelta = delta.findPropertyDelta(valuePropertyPath);
				if (propDelta != null) {
					hashValues(propDelta.getValuesToAdd(), storageMethod);
					hashValues(propDelta.getValuesToReplace(), storageMethod);
					hashValues(propDelta.getValuesToDelete(), storageMethod);
				}
			}
		} else if (storageType == CredentialsStorageTypeType.NONE) {
			if (delta.isAdd()) {
				delta.getObjectToAdd().removeProperty(valuePropertyPath);
			} else {
				PropertyDelta<ProtectedStringType> propDelta = delta.findPropertyDelta(valuePropertyPath);
				if (propDelta != null) {
					// Replace with nothing. We need this to clear any existing value that there might be.
					propDelta.setValueToReplace();
				}
			}
		} else {
			throw new SchemaException("Unkwnon storage type "+storageType);
		}
		
	}

	private void hashValues(Collection<PrismPropertyValue<ProtectedStringType>> values,
			CredentialsStorageMethodType storageMethod) throws SchemaException, EncryptionException {
		if (values == null) {
			return;
		}
		for (PrismPropertyValue<ProtectedStringType> pval: values) {
			protector.hash(pval.getValue());
		}
	}

}
