/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS;

import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author katkav
 *
 */
@Component
public class FocusProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(FocusProcessor.class);

    private PrismContainerDefinition<ActivationType> activationDefinition;
    private PrismPropertyDefinition<Integer> failedLoginsDefinition;

    @Autowired private PrismContext prismContext;
    @Autowired private CredentialsProcessor credentialsProcessor;
    @Autowired private ActivationComputer activationComputer;

    <AH extends AssignmentHolderType, F extends FocusType> void processActivationBeforeAssignments(LensContext<AH> context, XMLGregorianCalendar now,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {

        if (isFocus(context)) {
            //noinspection unchecked
            processActivationBasic((LensContext<F>) context, now, result);
        } else {
            LOGGER.trace("Skipping activation processing. Not a focus.");
        }
    }

    private <AH extends AssignmentHolderType> boolean isFocus(LensContext<AH> context) {
        LensFocusContext<AH> focusContext = context.getFocusContext();
        return focusContext != null && focusContext.represents(FocusType.class);
    }

    <AH extends AssignmentHolderType, F extends FocusType> void processActivationAfterAssignments(LensContext<AH> context, XMLGregorianCalendar now,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {

        processActivationBeforeAssignments(context, now, result);

        processAssignmentActivation(context, now, result);
    }

    <AH extends AssignmentHolderType, F extends FocusType> void processCredentials(LensContext<AH> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

        if (isFocus(context)) {
            //noinspection unchecked
            credentialsProcessor.processFocusCredentials((LensContext<F>) context, now, task, result);
        } else {
            LOGGER.trace("Skipping credentials processing. Not a focus.");
        }
    }
    private <F extends FocusType> void processActivationBasic(LensContext<F> context, XMLGregorianCalendar now,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
        LensFocusContext<F> focusContext = context.getFocusContext();

        if (focusContext.isDelete()) {
            LOGGER.trace("Skipping processing of focus activation: focus delete");
            return;
        }

        processActivationAdministrativeAndValidity(focusContext, now, result);

        if (focusContext.represents(UserType.class)) {
            //noinspection unchecked
            processActivationLockout((LensFocusContext<UserType>) focusContext, now, result);
        }
    }

    private <F extends AssignmentHolderType> void processAssignmentActivation(LensContext<F> context, XMLGregorianCalendar now,
            OperationResult result) throws SchemaException {
        DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null) {
            // Code path that should not normally happen. But is used in some tests and may
            // happen during partial processing.
            return;
        }
        // We care only about existing assignments here. New assignments will be taken care of in the executor
        // (OperationalDataProcessor). And why care about deleted assignments?
        Collection<EvaluatedAssignmentImpl<?>> zeroSet = evaluatedAssignmentTriple.getZeroSet();
        LensFocusContext<F> focusContext = context.getFocusContext();
        for (EvaluatedAssignmentImpl<?> evaluatedAssignment: zeroSet) {
            if (evaluatedAssignment.isVirtual()) {
                continue;
            }
            AssignmentType assignment = evaluatedAssignment.getAssignmentType();
            ActivationType currentActivation = assignment.getActivation();
            ActivationStatusType currentEffectiveStatus = currentActivation != null ? currentActivation.getEffectiveStatus() : null;
            ActivationStatusType expectedEffectiveStatus = activationComputer.getEffectiveStatus(assignment.getLifecycleState(),
                    currentActivation, null);
            if (currentEffectiveStatus != expectedEffectiveStatus) {
                PrismPropertyDefinition<ActivationStatusType> effectiveStatusPropertyDef = focusContext.getObjectDefinition()
                        .findPropertyDefinition(SchemaConstants.PATH_ASSIGNMENT_ACTIVATION_EFFECTIVE_STATUS);
                PropertyDelta<ActivationStatusType> effectiveStatusDelta = effectiveStatusPropertyDef.createEmptyDelta(
                        ItemPath.create(FocusType.F_ASSIGNMENT, assignment.getId(), AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
                effectiveStatusDelta.setRealValuesToReplace(expectedEffectiveStatus);
                focusContext.swallowToSecondaryDelta(effectiveStatusDelta);
            }
        }
    }

    private <F extends FocusType> void processActivationAdministrativeAndValidity(LensFocusContext<F> focusContext, XMLGregorianCalendar now,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {

        TimeIntervalStatusType validityStatusNew = null;
        TimeIntervalStatusType validityStatusCurrent = null;
        XMLGregorianCalendar validityChangeTimestamp = null;

        String lifecycleStateNew = null;
        String lifecycleStateCurrent = null;
        ActivationType activationNew = null;
        ActivationType activationCurrent = null;

        PrismObject<F> focusNew = focusContext.getObjectNew();
        if (focusNew != null) {
            F focusTypeNew = focusNew.asObjectable();
            activationNew = focusTypeNew.getActivation();
            if (activationNew != null) {
                validityStatusNew = activationComputer.getValidityStatus(activationNew, now);
                validityChangeTimestamp = activationNew.getValidityChangeTimestamp();
            }
            lifecycleStateNew = focusTypeNew.getLifecycleState();
        }

        PrismObject<F> focusCurrent = focusContext.getObjectCurrent();
        if (focusCurrent != null) {
            F focusCurrentType = focusCurrent.asObjectable();
            activationCurrent = focusCurrentType.getActivation();
            if (activationCurrent != null) {
                validityStatusCurrent = activationComputer.getValidityStatus(activationCurrent, validityChangeTimestamp);
            }
            lifecycleStateCurrent = focusCurrentType.getLifecycleState();
        }

        if (validityStatusCurrent == validityStatusNew) {
            // No change, (almost) no work
            if (validityStatusNew != null && activationNew.getValidityStatus() == null) {
                // There was no validity change. But the status is not recorded. So let's record it so it can be used in searches.
                recordValidityDelta(focusContext, validityStatusNew, now);
            } else {
                LOGGER.trace("Skipping validity processing because there was no change ({} -> {})", validityStatusCurrent, validityStatusNew);
            }
        } else {
            LOGGER.trace("Validity change {} -> {}", validityStatusCurrent, validityStatusNew);
            recordValidityDelta(focusContext, validityStatusNew, now);
        }

        LifecycleStateModelType lifecycleModel = focusContext.getLifecycleModel();
        ActivationStatusType effectiveStatusNew = activationComputer.getEffectiveStatus(lifecycleStateNew, activationNew, validityStatusNew, lifecycleModel);
        ActivationStatusType effectiveStatusCurrent = activationComputer.getEffectiveStatus(lifecycleStateCurrent, activationCurrent, validityStatusCurrent, lifecycleModel);

        if (effectiveStatusCurrent == effectiveStatusNew) {
            // No change, (almost) no work
            if (effectiveStatusNew != null && (activationNew == null || activationNew.getEffectiveStatus() == null)) {
                // There was no effective status change. But the status is not recorded. So let's record it so it can be used in searches.
                recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
            } else {
                if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().hasItemDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)) {
                    LOGGER.trace("Forcing effective status delta even though there was no change ({} -> {}) because there is explicit administrativeStatus delta", effectiveStatusCurrent, effectiveStatusNew);
                    // We need this to force the change down to the projections later in the activation processor
                    // some of the mappings will use effectiveStatus as a source, therefore there has to be a delta for the mapping to work correctly
                    recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
                } else {
                    //check computed effective status current with the saved one - e.g. there can be some inconsistencies so we need to check and force the change.. in other cases, effectvie status will be stored with
                    // incorrect value. Maybe another option is to not compute effectiveStatusCurrent if there is an existing (saved) effective status in the user.. TODO
                    if (activationCurrent != null && activationCurrent.getEffectiveStatus() != null) {
                        ActivationStatusType effectiveStatusSaved = activationCurrent.getEffectiveStatus();
                        if (effectiveStatusSaved != effectiveStatusNew) {
                            recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
                        }
                    }
                    LOGGER.trace("Skipping effective status processing because there was no change ({} -> {})", effectiveStatusCurrent, effectiveStatusNew);
                }
            }
        } else {
            LOGGER.trace("Effective status change {} -> {}", effectiveStatusCurrent, effectiveStatusNew);
            recordEffectiveStatusDelta(focusContext, effectiveStatusNew, now);
        }


    }

    private <F extends FocusType> void processActivationLockout(LensFocusContext<UserType> focusContext, XMLGregorianCalendar now,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {

        ObjectDelta<UserType> focusPrimaryDelta = focusContext.getPrimaryDelta();
        if (focusPrimaryDelta != null) {
            PropertyDelta<LockoutStatusType> lockoutStatusDelta = focusContext.getPrimaryDelta().findPropertyDelta(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
            if (lockoutStatusDelta != null) {
                if (lockoutStatusDelta.isAdd()) {
                    for (PrismPropertyValue<LockoutStatusType> pval: lockoutStatusDelta.getValuesToAdd()) {
                        if (pval.getValue() == LockoutStatusType.LOCKED) {
                            throw new SchemaException("Lockout status cannot be changed to LOCKED value");
                        }
                    }
                } else if (lockoutStatusDelta.isReplace()) {
                    for (PrismPropertyValue<LockoutStatusType> pval: lockoutStatusDelta.getValuesToReplace()) {
                        if (pval.getValue() == LockoutStatusType.LOCKED) {
                            throw new SchemaException("Lockout status cannot be changed to LOCKED value");
                        }
                    }
                }
            }
        }

        ActivationType activationNew = null;
        ActivationType activationCurrent = null;

        LockoutStatusType lockoutStatusNew = null;
        LockoutStatusType lockoutStatusCurrent = null;

        PrismObject<UserType> focusNew = focusContext.getObjectNew();
        if (focusNew != null) {
            activationNew = focusNew.asObjectable().getActivation();
            if (activationNew != null) {
                lockoutStatusNew = activationNew.getLockoutStatus();
            }
        }

        PrismObject<UserType> focusCurrent = focusContext.getObjectCurrent();
        if (focusCurrent != null) {
            activationCurrent = focusCurrent.asObjectable().getActivation();
            if (activationCurrent != null) {
                lockoutStatusCurrent = activationCurrent.getLockoutStatus();
            }
        }

        if (lockoutStatusNew == lockoutStatusCurrent) {
            // No change, (almost) no work
            LOGGER.trace("Skipping lockout processing because there was no change ({} -> {})", lockoutStatusCurrent, lockoutStatusNew);
            return;
        }

        LOGGER.trace("Lockout change {} -> {}", lockoutStatusCurrent, lockoutStatusNew);

        if (lockoutStatusNew == LockoutStatusType.NORMAL) {

            CredentialsType credentialsTypeNew = focusNew.asObjectable().getCredentials();
            if (credentialsTypeNew != null) {
                resetFailedLogins(focusContext, credentialsTypeNew.getPassword(), SchemaConstants.PATH_CREDENTIALS_PASSWORD_FAILED_LOGINS);
                resetFailedLogins(focusContext, credentialsTypeNew.getNonce(), SchemaConstants.PATH_CREDENTIALS_NONCE_FAILED_LOGINS);
                resetFailedLogins(focusContext, credentialsTypeNew.getSecurityQuestions(), SchemaConstants.PATH_CREDENTIALS_SECURITY_QUESTIONS_FAILED_LOGINS);
            }

            if (activationNew != null && activationNew.getLockoutExpirationTimestamp() != null) {
                PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();
                PrismPropertyDefinition<XMLGregorianCalendar> lockoutExpirationTimestampDef = activationDefinition.findPropertyDefinition(ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP);
                PropertyDelta<XMLGregorianCalendar> lockoutExpirationTimestampDelta
                        = lockoutExpirationTimestampDef.createEmptyDelta(ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP));
                lockoutExpirationTimestampDelta.setValueToReplace();
                focusContext.swallowToProjectionWaveSecondaryDelta(lockoutExpirationTimestampDelta);
            }
        }

    }

    private void resetFailedLogins(LensFocusContext<UserType> focusContext, AbstractCredentialType credentialTypeNew, ItemPath path) throws SchemaException{
        if (credentialTypeNew != null) {
            Integer failedLogins = credentialTypeNew.getFailedLogins();
            if (failedLogins != null && failedLogins != 0) {
                PrismPropertyDefinition<Integer> failedLoginsDef = getFailedLoginsDefinition();
                PropertyDelta<Integer> failedLoginsDelta = failedLoginsDef.createEmptyDelta(path);
                failedLoginsDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(0, OriginType.USER_POLICY, null));
                focusContext.swallowToProjectionWaveSecondaryDelta(failedLoginsDelta);
            }
        }
    }

    private <F extends ObjectType> void recordValidityDelta(LensFocusContext<F> focusContext, TimeIntervalStatusType validityStatusNew,
            XMLGregorianCalendar now) throws SchemaException {
        PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();

        PrismPropertyDefinition<TimeIntervalStatusType> validityStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_STATUS);
        PropertyDelta<TimeIntervalStatusType> validityStatusDelta
                = validityStatusDef.createEmptyDelta(ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS));
        if (validityStatusNew == null) {
            validityStatusDelta.setValueToReplace();
        } else {
            validityStatusDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(validityStatusNew, OriginType.USER_POLICY, null));
        }
        focusContext.swallowToProjectionWaveSecondaryDelta(validityStatusDelta);

        PrismPropertyDefinition<XMLGregorianCalendar> validityChangeTimestampDef = activationDefinition.findPropertyDefinition(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP);
        PropertyDelta<XMLGregorianCalendar> validityChangeTimestampDelta
                = validityChangeTimestampDef.createEmptyDelta(ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP));
        validityChangeTimestampDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(now, OriginType.USER_POLICY, null));
        focusContext.swallowToProjectionWaveSecondaryDelta(validityChangeTimestampDelta);
    }

    private <F extends ObjectType> void recordEffectiveStatusDelta(LensFocusContext<F> focusContext,
            ActivationStatusType effectiveStatusNew, XMLGregorianCalendar now)
            throws SchemaException {
        PrismContainerDefinition<ActivationType> activationDefinition = getActivationDefinition();

        // We always want explicit delta for effective status even if there is no real change
        // we want to propagate enable/disable events to all the resources, even if we are enabling
        // already enabled user (some resources may be disabled)
        // This may produce duplicate delta, but that does not matter too much. The duplicate delta
        // will be filtered out later.
        PrismPropertyDefinition<ActivationStatusType> effectiveStatusDef = activationDefinition.findPropertyDefinition(ActivationType.F_EFFECTIVE_STATUS);
        PropertyDelta<ActivationStatusType> effectiveStatusDelta
                = effectiveStatusDef.createEmptyDelta(PATH_ACTIVATION_EFFECTIVE_STATUS);
        effectiveStatusDelta.setValueToReplace(prismContext.itemFactory().createPropertyValue(effectiveStatusNew, OriginType.USER_POLICY, null));
        if (!focusContext.alreadyHasDelta(effectiveStatusDelta)){
            focusContext.swallowToProjectionWaveSecondaryDelta(effectiveStatusDelta);
        }

        // It is not enough to check alreadyHasDelta(). The change may happen in previous waves
        // and the secondary delta may no longer be here. When it comes to disableTimestamp we even
        // cannot rely on natural filtering of already executed deltas as the timestamp here may
        // be off by several milliseconds. So explicitly check for the change here.
        PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
        if (objectCurrent != null) {
            PrismProperty<ActivationStatusType> effectiveStatusPropCurrent = objectCurrent.findProperty(PATH_ACTIVATION_EFFECTIVE_STATUS);
            if (effectiveStatusPropCurrent != null && effectiveStatusNew.equals(effectiveStatusPropCurrent.getRealValue())) {
                LOGGER.trace("Skipping setting disableTimestamp because there was no change");
                return;
            }
        }

        PropertyDelta<XMLGregorianCalendar> timestampDelta = LensUtil.createActivationTimestampDelta(effectiveStatusNew, now, activationDefinition, OriginType.USER_POLICY,
                prismContext);
        if (!focusContext.alreadyHasDelta(timestampDelta)) {
            focusContext.swallowToProjectionWaveSecondaryDelta(timestampDelta);
        }
    }


    private PrismContainerDefinition<ActivationType> getActivationDefinition() {
        if (activationDefinition == null) {
            ComplexTypeDefinition focusDefinition = prismContext.getSchemaRegistry().findComplexTypeDefinition(FocusType.COMPLEX_TYPE);
            activationDefinition = focusDefinition.findContainerDefinition(FocusType.F_ACTIVATION);
        }
        return activationDefinition;
    }

    private PrismPropertyDefinition<Integer> getFailedLoginsDefinition() {
        if (failedLoginsDefinition == null) {
            PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
            failedLoginsDefinition = userDef.findPropertyDefinition(SchemaConstants.PATH_CREDENTIALS_PASSWORD_FAILED_LOGINS);
        }
        return failedLoginsDefinition;
    }


}
