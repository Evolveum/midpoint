/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * Processor for focus credentials.
 *
 * Validates the credentials, checks policies (complexity, history, etc.), adds metadata, etc.
 * It is used during `Projector` execution - as part of FocusProcessor run.
 * (Note that these activities are more or less delegated to {@link CredentialPolicyEvaluator}.)
 *
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = FocusType.class)
public class CredentialsProcessor implements ProjectorProcessor {

    @Autowired private PrismContext prismContext;
    @Autowired private OperationalDataManager metadataManager;
    @Autowired private ModelObjectResolver resolver;
    @Autowired private ValuePolicyProcessor valuePolicyProcessor;
    @Autowired private Protector protector;
    @Autowired private LocalizationService localizationService;
    @Autowired private ContextLoader contextLoader;

    @ProcessorMethod
    public <F extends FocusType> void processFocusCredentials(
            LensContext<F> context, XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

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
