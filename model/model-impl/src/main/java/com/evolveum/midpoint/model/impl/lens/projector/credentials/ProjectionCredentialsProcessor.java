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

import static com.evolveum.midpoint.prism.delta.ChangeType.MODIFY;

import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.MappingInitializer;
import com.evolveum.midpoint.model.impl.lens.projector.MappingOutputProcessor;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.repo.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Processor for projection credentials. Which at this moment means just the password.
 *
 * @author Radovan Semancik
 */
@Component
public class ProjectionCredentialsProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(ProjectionCredentialsProcessor.class);

	@Autowired private PrismContext prismContext;
	@Autowired private ContextLoader contextLoader;
	@Autowired private MappingFactory mappingFactory;
	@Autowired private MappingEvaluator mappingEvaluator;
	@Autowired private ValuePolicyProcessor valuePolicyProcessor;
	@Autowired private Protector protector;
	@Autowired private OperationalDataManager operationalDataManager;

	public <F extends ObjectType> void processProjectionCredentials(LensContext<F> context,
			LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task,
			OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		if (projectionContext.isDelete()) {
			return;
		}
		
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext != null && FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			
			processProjectionCredentialsFocus((LensContext<? extends FocusType>) context, projectionContext, now, task, result);
			
		}
	}
	
	private <F extends FocusType> void processProjectionCredentialsFocus(LensContext<F> context,
			LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task,
			OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		ValuePolicyType passwordPolicy = determinePasswordPolicy(context, projectionContext, now, task, result);
		
		processProjectionPasswordMapping(context, projectionContext, passwordPolicy, now, task, result);
				
		validateProjectionPassword(context, projectionContext, passwordPolicy, now, task, result);
		
		applyMetadata(context, projectionContext, now, task, result);
	}
	
	private <F extends FocusType> void processProjectionPasswordMapping(LensContext<F> context,
			final LensProjectionContext projCtx, final ValuePolicyType passwordPolicy, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
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

		List<MappingType> outboundMappingTypes = refinedProjDef.getPasswordOutbound();
		if (outboundMappingTypes == null || outboundMappingTypes.isEmpty()) {
			LOGGER.trace("No outbound password mapping for {}, skipping credentials processing", rsd);
			return;
		}
		
		// HACK
		if (!projCtx.isDoReconciliation() && !projCtx.isAdd() && !isActivated(outboundMappingTypes, focusContext.getDelta())) {
			LOGGER.trace("Outbound password mappings not activated for type {}, skipping credentials processing", rsd);
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

		boolean evaluateWeak = getEvaluateWeak(projCtx);

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
				if (passwordPolicy == null) {
					return null;
				}
				return passwordPolicy.getStringPolicy();
			}
		};

		MappingInitializer<PrismPropertyValue<ProtectedStringType>,PrismPropertyDefinition<ProtectedStringType>> initializer = 
			(builder) -> {
				builder.defaultTargetDefinition(projPasswordPropertyDefinition);
				builder.defaultSource(new Source<>(userPasswordIdi, ExpressionConstants.VAR_INPUT));
				builder.stringPolicyResolver(stringPolicyResolver);
				return builder;
			};
			
		MappingOutputProcessor<PrismPropertyValue<ProtectedStringType>> processor = 
				(mappingOutputPath, outputStruct) -> {
					PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = outputStruct.getOutputTriple();
					if (outputTriple == null) {
						LOGGER.trace("Credentials 'password' expression resulted in null output triple, skipping credentials processing for {}", rsd);
						return false;
					}

					boolean projectionIsNew = projDelta != null && (projDelta.getChangeType() == ChangeType.ADD
							|| projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD);

					Collection<PrismPropertyValue<ProtectedStringType>> newValues = outputTriple.getPlusSet();
					if (projectionIsNew) {
						newValues = outputTriple.getNonNegativeValues();
					} else {
						newValues = outputTriple.getPlusSet();
					}
					
					if (!canGetCleartext(newValues)) {
						ObjectDelta<ShadowType> projectionPrimaryDelta = projCtx.getPrimaryDelta();
						if (projectionPrimaryDelta != null) {
							PropertyDelta<ProtectedStringType> passwordPrimaryDelta = projectionPrimaryDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
							if (passwordPrimaryDelta != null) {
								// We have only hashed value coming from the mapping. There are not very useful
								// for provisioning. But we have primary projection delta - and that is very likely
								// to be better.
								// Skip all password mappings in this case. Primary delta trumps everything. 
								// No weak, normal or even strong mapping can change that.
								// We need to disregard even strong mapping in this case. If we would heed the strong
								// mapping then account initialization won't be possible.
								LOGGER.trace("We have primary password delta in projection, skipping credentials processing");
								return false;
							}
						}
					}

					return true;
				};
		
		
		mappingEvaluator.evaluateOutboundMapping(context, projCtx, outboundMappingTypes, 
				SchemaConstants.PATH_PASSWORD_VALUE, SchemaConstants.PATH_PASSWORD_VALUE, initializer, processor,
				now, true, evaluateWeak, "password mapping", task, result);
		
	}
	
	private <F extends FocusType> boolean isActivated(List<MappingType> outboundMappingTypes, ObjectDelta<F> focusDelta) {
		if (focusDelta == null) {
			return false;
		}
		for (MappingType outboundMappingType: outboundMappingTypes) {
			List<VariableBindingDefinitionType> sources = outboundMappingType.getSource();
			if (sources.isEmpty()) {
				// Default source
				if (focusDelta.hasItemDelta(SchemaConstants.PATH_PASSWORD_VALUE)) {
					return true;
				}
			}
			for (VariableBindingDefinitionType source: sources) {
				ItemPathType pathType = source.getPath();
				ItemPath path = pathType.getItemPath().stripVariableSegment();
				if (focusDelta.hasItemDelta(path)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean canGetCleartext(Collection<PrismPropertyValue<ProtectedStringType>> pvals) {
		if (pvals == null) {
			return false;
		}
		for (PrismPropertyValue<ProtectedStringType> pval: pvals) {
			if (pval.getValue().canGetCleartext()) {
				return true;
			}
		}
		return false;
	}

	private boolean getEvaluateWeak(LensProjectionContext projCtx) {
		CredentialsCapabilityType credentialsCapabilityType = ResourceTypeUtil.getEffectiveCapability(projCtx.getResource(), CredentialsCapabilityType.class);
		if (credentialsCapabilityType != null) {
			PasswordCapabilityType passwordCapabilityType = credentialsCapabilityType.getPassword();
			if (passwordCapabilityType != null) {
				if (passwordCapabilityType.isEnabled() != Boolean.FALSE) {
					Boolean readable = passwordCapabilityType.isReadable();
					if (readable != null && readable) {
						// If we have readable password then we can evaluate the weak mappings
						// normally (even if the reads return incomplete values).
						return true;
					}
				}
			}
		}
		// Password not readable. Therefore evaluate weak mappings only during add operaitons.
		// We do not know whether there is a password already set on the resource. And we do not
		// want to overwrite it every time.
		return projCtx.isAdd();
	}

	private <F extends FocusType> void validateProjectionPassword(LensContext<F> context,
			final LensProjectionContext projectionContext, final ValuePolicyType passwordPolicy, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		
		if (passwordPolicy == null) {
			LOGGER.trace("Skipping processing password policies. Password policy not specified.");
			return;
		}
		
		ObjectDelta<ShadowType> accountDelta = projectionContext.getDelta();
		
		if (accountDelta == null){
			LOGGER.trace("Skipping processing password policies. Shadow delta not specified.");
			return;
		}
		
		if (accountDelta.isDelete()) {
			return;
		}
		
		PrismObject<ShadowType> accountShadow = null;
		PrismProperty<ProtectedStringType> password = null;
		if (accountDelta.isAdd()) {
			accountShadow = accountDelta.getObjectToAdd();
			if (accountShadow != null){
				password = accountShadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
			}
		}
		if (accountDelta.isModify() || password == null) {
			PropertyDelta<ProtectedStringType> passwordValueDelta =
					accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
			// Modification sanity check
			if (accountDelta.getChangeType() == ChangeType.MODIFY && passwordValueDelta != null
					&& (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
				throw new SchemaException("Shadow password value cannot be added or deleted, it can only be replaced");
			}
			if (passwordValueDelta == null) {
				LOGGER.trace("Skipping processing password policies. Shadow delta does not contain password change.");
				return;
			}
			password = (PrismProperty<ProtectedStringType>) passwordValueDelta.getItemNewMatchingPath(null);
		}
				
		if (accountShadow == null) {
			accountShadow = projectionContext.getObjectNew();
		}
		
        String passwordValue = determinePasswordValue(password);
       
        boolean isValid = valuePolicyProcessor.validateValue(passwordValue, passwordPolicy, accountShadow, "projection password policy", task, result);

		if (!isValid) {
			result.computeStatus();
			throw new PolicyViolationException("Provided password does not satisfy password policies in " + projectionContext.getHumanReadableName() + ": " + result.getMessage());
		}
	}
	
	private <F extends FocusType> void applyMetadata(LensContext<F> context,
			final LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		
		ObjectDelta<ShadowType> accountDelta = projectionContext.getDelta();
				
		if (projectionContext.isDelete()) {
			return;
		}
		
		if (accountDelta == null){
			LOGGER.trace("Skipping application of password metadata. Shadow delta not specified.");
			return;
		}

		PropertyDelta<ProtectedStringType> passwordValueDelta =
				accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
		if (passwordValueDelta == null) {
			LOGGER.trace("Skipping application of password metadata. No password change.");
			return;
		}
		
		if (projectionContext.isAdd()) {
			MetadataType metadataType = operationalDataManager.createCreateMetadata(context, now, task);
			ContainerDelta<MetadataType> metadataDelta = ContainerDelta.createDelta(SchemaConstants.PATH_PASSWORD_METADATA, projectionContext.getObjectDefinition());
			PrismContainerValue cval = metadataType.asPrismContainerValue();
			cval.setOriginTypeRecursive(OriginType.OUTBOUND);
			metadataDelta.addValuesToAdd(metadataType.asPrismContainerValue());
			projectionContext.swallowToSecondaryDelta(metadataDelta);

		} else if (projectionContext.isModify()) {
			ContainerDelta<MetadataType> metadataDelta = accountDelta.findContainerDelta(SchemaConstants.PATH_PASSWORD_METADATA);
			if (metadataDelta == null) {
				Collection<? extends ItemDelta<?,?>> modifyMetadataDeltas = operationalDataManager.createModifyMetadataDeltas(context, SchemaConstants.PATH_PASSWORD_METADATA, projectionContext.getObjectDefinition(), now, task);
				for (ItemDelta itemDelta: modifyMetadataDeltas) {
					itemDelta.setOriginTypeRecursive(OriginType.OUTBOUND);
					projectionContext.swallowToSecondaryDelta(itemDelta);
				}
			}
		}
						
	}
	
	private <F extends FocusType> ValuePolicyType determinePasswordPolicy(LensContext<F> context,
			final LensProjectionContext projCtx, XMLGregorianCalendar now, Task task, OperationResult result) {
		ValuePolicyType passwordPolicy = projCtx.getAccountPasswordPolicy();
		if (passwordPolicy != null) {
			return passwordPolicy;
		}
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return null;
		}
		return SecurityUtil.getPasswordPolicy(focusContext.getSecurityPolicy());
	}

	// On missing password this returns empty string (""). It is then up to password policy whether it allows empty passwords or not.
	private String determinePasswordValue(PrismProperty<ProtectedStringType> password) {
		if (password == null || password.getValue(ProtectedStringType.class) == null) {
			return null;
		}

		ProtectedStringType passValue = password.getRealValue();

		return determinePasswordValue(passValue);
	}
	
	private String determinePasswordValue(ProtectedStringType passValue) {
		if (passValue == null) {
			return null;
		}

		String passwordStr = passValue.getClearValue();

		if (passwordStr == null && passValue.getEncryptedDataType () != null) {
			// TODO: is this appropriate handling???
			try {
				passwordStr = protector.decryptString(passValue);
			} catch (EncryptionException ex) {
				throw new SystemException("Failed to process password for user: " , ex);
			}
		}

		return passwordStr;
	}
	
	private void checkExistingDeltaSanity(LensProjectionContext projCtx,
			PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
		if (passwordDelta != null && (passwordDelta.isAdd() || passwordDelta.isDelete())) {
			throw new SchemaException("Password for projection " + projCtx.getResourceShadowDiscriminator()
					+ " cannot be added or deleted, it can only be replaced");
		}
	}

}
