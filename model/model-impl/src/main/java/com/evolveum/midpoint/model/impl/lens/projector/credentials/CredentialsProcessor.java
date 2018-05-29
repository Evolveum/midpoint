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

import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.model.impl.lens.projector.ContextLoader;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Processor for focus credentials. It validates the credentials, checks
 * policies (complexity, history, etc.), adds metadata and so on.
 *
 * @author Radovan Semancik
 */
@Component
public class CredentialsProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(CredentialsProcessor.class);

	@Autowired private PrismContext prismContext;
	@Autowired private OperationalDataManager metadataManager;
	@Autowired private ModelObjectResolver resolver;
	@Autowired private ValuePolicyProcessor valuePolicyProcessor;
	@Autowired private Protector protector;
	@Autowired private LocalizationService localizationService;
	@Autowired private ContextLoader contextLoader;

	public <F extends FocusType> void processFocusCredentials(LensContext<F> context,
			XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException,
					ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null || !UserType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			LOGGER.trace("Skipping processing credentials because focus is not user");
			return;
		}
		
		contextLoader.reloadSecurityPolicyIfNeeded(context, task, result);

		processFocusPassword((LensContext<UserType>) context, now, task, result);
		processFocusNonce((LensContext<UserType>) context, now, task, result);
		processFocusSecurityQuestions((LensContext<UserType>) context, now, task, result);
	}
	


	private <F extends FocusType> void processFocusPassword(LensContext<UserType> context, XMLGregorianCalendar now,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

		PasswordPolicyEvaluator evaluator = new PasswordPolicyEvaluator();
		evaluator.setContext(context);
		evaluator.setMetadataManager(metadataManager);
		evaluator.setNow(now);
		evaluator.setPrismContext(prismContext);
		evaluator.setProtector(protector);
		evaluator.setLocalizationService(localizationService);
		evaluator.setResolver(resolver);
		evaluator.setResult(result);
		evaluator.setTask(task);
		evaluator.setValuePolicyProcessor(valuePolicyProcessor);

		evaluator.process();
	}

	//for now just saving metadata
	private void processFocusNonce(LensContext<UserType> context, XMLGregorianCalendar now,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

		NoncePolicyEvaluator evaluator = new NoncePolicyEvaluator();
		evaluator.setContext(context);
		evaluator.setMetadataManager(metadataManager);
		evaluator.setNow(now);
		evaluator.setPrismContext(prismContext);
		evaluator.setProtector(protector);
		evaluator.setLocalizationService(localizationService);
		evaluator.setResolver(resolver);
		evaluator.setResult(result);
		evaluator.setTask(task);
		evaluator.setValuePolicyProcessor(valuePolicyProcessor);

		evaluator.process();

	}

	private void processFocusSecurityQuestions(LensContext<UserType> context, XMLGregorianCalendar now,
			Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

		SecurityQuestionsPolicyEvaluator evaluator = new SecurityQuestionsPolicyEvaluator();
		evaluator.setContext(context);
		evaluator.setMetadataManager(metadataManager);
		evaluator.setNow(now);
		evaluator.setPrismContext(prismContext);
		evaluator.setProtector(protector);
		evaluator.setLocalizationService(localizationService);
		evaluator.setResolver(resolver);
		evaluator.setResult(result);
		evaluator.setTask(task);
		evaluator.setValuePolicyProcessor(valuePolicyProcessor);

		evaluator.process();

	}


	/**
	 * Called from ChangeExecutor. Will modify the execution deltas to hash or remove credentials if needed.
	 */
	public <O extends ObjectType> ObjectDelta<O> transformFocusExecutionDelta(LensContext<O> context, ObjectDelta<O> focusDelta) throws SchemaException, EncryptionException {
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
		transformFocusExecutionDeltaForPasswords(context, credsType, credsType.getPassword(), SchemaConstants.PATH_PASSWORD_VALUE, transformedDelta, "password");
		// TODO: nonce and others

		return transformedDelta;
	}

	// TODO generalize for nonce and others
	private <O extends ObjectType> void transformFocusExecutionDeltaForPasswords(LensContext<O> context,
			CredentialsPolicyType credsType, CredentialPolicyType credPolicyType,
			ItemPath valuePropertyPath, ObjectDelta<O> delta, String credentialName) throws SchemaException, EncryptionException {
		if (delta.isDelete()) {
			return;
		}
		CredentialPolicyType defaltCredPolicyType = credsType.getDefault();
		CredentialsStorageMethodType storageMethod =
				SecurityUtil.getCredPolicyItem(defaltCredPolicyType, credPolicyType, pol -> pol.getStorageMethod());
		LOGGER.trace("Credential {}, processing storage method: {}", credentialName, storageMethod);
		if (storageMethod == null) {
			return;
		}
		CredentialsStorageTypeType storageType = storageMethod.getStorageType();
		if (storageType == null || storageType == CredentialsStorageTypeType.ENCRYPTION) {
			LOGGER.trace("Credential {} should be encrypted, nothing to do", credentialName);
			return;
		} else if (storageType == CredentialsStorageTypeType.HASHING) {
			LOGGER.trace("Hashing credential", credentialName);
			if (delta.isAdd()) {
				PrismProperty<ProtectedStringType> prop = delta.getObjectToAdd().findProperty(valuePropertyPath);
				if (prop != null) {
					hashValues(prop.getValues(), storageMethod);
				}
			} else {
				//noinspection unchecked
				PropertyDelta<ProtectedStringType> valueDelta = delta.findItemDelta(valuePropertyPath, PropertyDelta.class, PrismProperty.class, true);
				if (valueDelta != null) {
					hashValues(valueDelta.getValuesToAdd(), storageMethod);
					hashValues(valueDelta.getValuesToReplace(), storageMethod);
					hashValues(valueDelta.getValuesToDelete(), storageMethod);  // TODO sure?
					return;
				}
				ItemPath abstractCredentialPath = valuePropertyPath.allExceptLast();
				//noinspection unchecked
				ContainerDelta<PasswordType> abstractCredentialDelta = delta.findItemDelta(abstractCredentialPath,
						ContainerDelta.class, PrismContainer.class, true);
				if (abstractCredentialDelta != null) {
					hashPasswordPcvs(abstractCredentialDelta.getValuesToAdd(), storageMethod);
					hashPasswordPcvs(abstractCredentialDelta.getValuesToReplace(), storageMethod);
					// TODO what about delete? probably nothing
					return;
				}
				ItemPath credentialsPath = abstractCredentialPath.allExceptLast();
				//noinspection unchecked
				ContainerDelta<CredentialsType> credentialsDelta = delta.findItemDelta(credentialsPath, ContainerDelta.class,
						PrismContainer.class, true);
				if (credentialsDelta != null) {
					hashCredentialsPcvs(credentialsDelta.getValuesToAdd(), storageMethod);
					hashCredentialsPcvs(credentialsDelta.getValuesToReplace(), storageMethod);
					// TODO what about delete? probably nothing
					return;
				}
			}
		} else if (storageType == CredentialsStorageTypeType.NONE) {
			LOGGER.trace("Removing credential", credentialName);
			if (delta.isAdd()) {
				delta.getObjectToAdd().removeProperty(valuePropertyPath);
			} else {
				PropertyDelta<ProtectedStringType> propDelta = delta.findPropertyDelta(valuePropertyPath);
				if (propDelta != null) {
					// Replace with nothing. We need this to clear any existing value that there might be.
					propDelta.setValueToReplace();
				}
			}
			// TODO remove password also when the whole credentials or credentials/password container is added/replaced
		} else {
			throw new SchemaException("Unknown storage type "+storageType);
		}
	}

	private void hashValues(Collection<PrismPropertyValue<ProtectedStringType>> values,
			CredentialsStorageMethodType storageMethod) throws SchemaException, EncryptionException {
		if (values == null) {
			return;
		}
		for (PrismPropertyValue<ProtectedStringType> pval: values) {
			ProtectedStringType ps = pval.getValue();
			if (!ps.isHashed()) {
				protector.hash(ps);
			}
		}
	}

	private void hashPasswordPcvs(Collection<PrismContainerValue<PasswordType>> values,
			CredentialsStorageMethodType storageMethod) throws SchemaException, EncryptionException {
		if (values == null) {
			return;
		}
		for (PrismContainerValue<PasswordType> pval: values) {
			PasswordType password = pval.getValue();
			if (password != null && password.getValue() != null) {
				if (!password.getValue().isHashed()) {
					protector.hash(password.getValue());
				}
			}
		}
	}

	private void hashCredentialsPcvs(Collection<PrismContainerValue<CredentialsType>> values,
			CredentialsStorageMethodType storageMethod) throws SchemaException, EncryptionException {
		if (values == null) {
			return;
		}
		for (PrismContainerValue<CredentialsType> pval: values) {
			CredentialsType credentials = pval.getValue();
			if (credentials != null && credentials.getPassword() != null) {
				ProtectedStringType passwordValue = credentials.getPassword().getValue();
				if (passwordValue != null && !passwordValue.isHashed()) {
					protector.hash(passwordValue);
				}
			}
		}
	}

	/**
	 * Legacy. Invoked from mappings. TODO: fix
	 */
	public <F extends ObjectType> ValuePolicyType determinePasswordPolicy(LensFocusContext<F> focusContext, Task task, OperationResult result) {
		if (focusContext == null) {
			return null;
		}
		return SecurityUtil.getPasswordPolicy(focusContext.getSecurityPolicy());
	}
}
