/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
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
 * Processor for focus credentials.
 *
 * Has two main responsibilities corresponding to its entry points:
 *
 * 1. processFocusCredentials(..)
 *
 *     Validates the credentials, checks policies (complexity, history, etc.), adds metadata, etc.
 *     It is used during Projector execution - as part of FocusProcessor run.
 *     (Note that these activities are more or less delegated to CredentialPolicyEvaluator.)
 *
 * 2. transformFocusExecutionDelta(..):
 *
 *     Modifies the execution deltas to hash or remove credentials if needed.
 *     This is done during change execution, called from ChangeExecutor.
 *
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = FocusType.class)
public class CredentialsProcessor implements ProjectorProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(CredentialsProcessor.class);

    @Autowired private PrismContext prismContext;
    @Autowired private OperationalDataManager metadataManager;
    @Autowired private ModelObjectResolver resolver;
    @Autowired private ValuePolicyProcessor valuePolicyProcessor;
    @Autowired private Protector protector;
    @Autowired private LocalizationService localizationService;
    @Autowired private ContextLoader contextLoader;

    @ProcessorMethod
    public <F extends FocusType> void processFocusCredentials(LensContext<F> context,
            XMLGregorianCalendar now, Task task, OperationResult result) throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LensFocusContext<F> focusContext = context.getFocusContext();

        contextLoader.reloadSecurityPolicyIfNeeded(context, focusContext, task, result);

        processFocusPassword(context, now, task, result);
        processFocusNonce(context, now, task, result);
        processFocusSecurityQuestions(context, now, task, result);
    }

    private <F extends FocusType> void processFocusPassword(LensContext<F> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        createEvaluator(new PasswordPolicyEvaluator.Builder<>(), context, now, task, result)
                .process();
    }

    private <F extends FocusType> void processFocusNonce(LensContext<F> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        createEvaluator(new NoncePolicyEvaluator.Builder<>(), context, now, task, result)
                .process();
    }

    private <F extends FocusType> void processFocusSecurityQuestions(LensContext<F> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        createEvaluator(new SecurityQuestionsPolicyEvaluator.Builder<>(), context, now, task, result)
                .process();
    }

    private <F extends FocusType> CredentialPolicyEvaluator<?, ?, F> createEvaluator(
            CredentialPolicyEvaluator.Builder<F> builder, LensContext<F> context, XMLGregorianCalendar now, Task task,
            OperationResult result) {
        return builder
                .context(context)
                .metadataManager(metadataManager)
                .now(now)
                .prismContext(prismContext)
                .protector(protector)
                .localizationService(localizationService)
                .resolver(resolver)
                .valuePolicyProcessor(valuePolicyProcessor)
                .task(task)
                .result(result)
                .build();
    }

    /**
     * Called from ChangeExecutor. Will modify the execution deltas to hash or remove credentials if needed.
     */
    public <O extends ObjectType> ObjectDelta<O> transformFocusExecutionDelta(LensContext<O> context, ObjectDelta<O> focusDelta)
            throws SchemaException, EncryptionException {
        LensFocusContext<O> focusContext = context.getFocusContext();
        CredentialsPolicyType credentialsPolicy = focusContext.getCredentialsPolicy();
        if (credentialsPolicy == null) {
            return focusDelta;
        } else {
            ObjectDelta<O> transformedDelta = focusDelta.clone();
            transformFocusExecutionDeltaForCredential(credentialsPolicy, credentialsPolicy.getPassword(),
                    "password", SchemaConstants.PATH_PASSWORD_VALUE, transformedDelta);
            // TODO: nonce and others

            return transformedDelta;
        }
    }

    // TODO generalize for nonce and others

    @SuppressWarnings("SameParameterValue")
    private <O extends ObjectType> void transformFocusExecutionDeltaForCredential(CredentialsPolicyType credentialsPolicy,
            CredentialPolicyType specificCredentialPolicy, String specificCredentialName, ItemPath valuePath,
            ObjectDelta<O> delta) throws SchemaException, EncryptionException {
        if (delta.isDelete()) {
            return;
        }
        CredentialPolicyType defaultCredentialPolicy = credentialsPolicy.getDefault();
        CredentialsStorageMethodType storageMethod =
                SecurityUtil.getCredentialPolicyItem(defaultCredentialPolicy, specificCredentialPolicy, CredentialPolicyType::getStorageMethod);
        LOGGER.trace("Credential {}, processing storage method: {}", specificCredentialName, storageMethod);
        if (storageMethod == null) {
            return;
        }
        CredentialsStorageTypeType storageType = storageMethod.getStorageType();
        if (storageType == null || storageType == CredentialsStorageTypeType.ENCRYPTION) {
            LOGGER.trace("Credential {} should be encrypted, nothing to do", specificCredentialName);
        } else if (storageType == CredentialsStorageTypeType.HASHING) {
            LOGGER.trace("Hashing credential: {}", specificCredentialName);
            hashCredentialValue(valuePath, delta);
        } else if (storageType == CredentialsStorageTypeType.NONE) {
            LOGGER.trace("Removing credential: {}", specificCredentialName);
            removeCredentialValue(valuePath, delta);
        } else {
            throw new SchemaException("Unknown storage type "+storageType);
        }
    }

    private <O extends ObjectType> void hashCredentialValue(ItemPath valuePath, ObjectDelta<O> delta) throws SchemaException, EncryptionException {
        if (delta.isAdd()) {
            PrismProperty<ProtectedStringType> prop = delta.getObjectToAdd().findProperty(valuePath);
            if (prop != null) {
                hashValues(prop.getValues());
            }
        } else {
            //noinspection unchecked
            PropertyDelta<ProtectedStringType> valueDelta = delta.findItemDelta(valuePath, PropertyDelta.class, PrismProperty.class, true);
            if (valueDelta != null) {
                hashValues(valueDelta.getValuesToAdd());
                hashValues(valueDelta.getValuesToReplace());
                hashValues(valueDelta.getValuesToDelete());  // TODO sure?
                return;
            }
            ItemPath abstractCredentialPath = valuePath.allExceptLast();
            //noinspection unchecked
            ContainerDelta<PasswordType> abstractCredentialDelta = delta.findItemDelta(abstractCredentialPath,
                    ContainerDelta.class, PrismContainer.class, true);
            if (abstractCredentialDelta != null) {
                hashPasswordPcvs(abstractCredentialDelta.getValuesToAdd());
                hashPasswordPcvs(abstractCredentialDelta.getValuesToReplace());
                // TODO what about delete? probably nothing
                return;
            }
            ItemPath credentialsPath = abstractCredentialPath.allExceptLast();
            //noinspection unchecked
            ContainerDelta<CredentialsType> credentialsDelta = delta.findItemDelta(credentialsPath, ContainerDelta.class,
                    PrismContainer.class, true);
            if (credentialsDelta != null) {
                hashCredentialsPcvs(credentialsDelta.getValuesToAdd());
                hashCredentialsPcvs(credentialsDelta.getValuesToReplace());
                // TODO what about delete? probably nothing
            }
        }
    }

    private <O extends ObjectType> void removeCredentialValue(ItemPath valuePath, ObjectDelta<O> delta) {
        if (delta.isAdd()) {
            delta.getObjectToAdd().removeProperty(valuePath);
        } else {
            PropertyDelta<ProtectedStringType> propDelta = delta.findPropertyDelta(valuePath);
            if (propDelta != null) {
                // Replace with nothing. We need this to clear any existing value that there might be.
                propDelta.setValueToReplace();
            }
        }
        // TODO remove password also when the whole credentials or credentials/password container is added/replaced
    }

    private void hashValues(Collection<PrismPropertyValue<ProtectedStringType>> values)
            throws SchemaException, EncryptionException {
        if (values != null) {
            for (PrismPropertyValue<ProtectedStringType> pval : values) {
                ProtectedStringType ps = pval.getValue();
                if (!ps.isHashed()) {
                    protector.hash(ps);
                }
            }
        }
    }

    private void hashPasswordPcvs(Collection<PrismContainerValue<PasswordType>> values)
            throws SchemaException, EncryptionException {
        if (values != null) {
            for (PrismContainerValue<PasswordType> pval : values) {
                PasswordType password = pval.getValue();
                if (password != null && password.getValue() != null) {
                    if (!password.getValue().isHashed()) {
                        protector.hash(password.getValue());
                    }
                }
            }
        }
    }

    private void hashCredentialsPcvs(Collection<PrismContainerValue<CredentialsType>> values)
            throws SchemaException, EncryptionException {
        if (values != null) {
            for (PrismContainerValue<CredentialsType> pval : values) {
                CredentialsType credentials = pval.getValue();
                if (credentials != null && credentials.getPassword() != null) {
                    ProtectedStringType passwordValue = credentials.getPassword().getValue();
                    if (passwordValue != null && !passwordValue.isHashed()) {
                        protector.hash(passwordValue);
                    }
                }
            }
        }
    }

    /**
     * Legacy. Invoked from mappings. TODO: fix
     */
    public <F extends ObjectType> ValuePolicyType determinePasswordPolicy(LensFocusContext<F> focusContext) {
        if (focusContext != null) {
            return SecurityUtil.getPasswordPolicy(focusContext.getSecurityPolicy());
        } else {
            return null;
        }
    }
}
