/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.prism.delta.ChangeType.MODIFY;

import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.impl.lens.projector.util.ErrorHandlingUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.common.stringpolicy.ObjectValuePolicyEvaluator;
import com.evolveum.midpoint.model.common.stringpolicy.ShadowValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ProjectionMappingSetEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.*;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Processor for projection credentials. Which at this moment means just the password.
 *
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = FocusType.class, skipWhenProjectionDeleted = true)
public class ProjectionCredentialsProcessor implements ProjectorProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionCredentialsProcessor.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ProjectionMappingSetEvaluator projectionMappingSetEvaluator;
    @Autowired private ValuePolicyProcessor valuePolicyProcessor;
    @Autowired private Protector protector;
    @Autowired private OperationalDataManager operationalDataManager;
    @Autowired private ModelObjectResolver modelObjectResolver;
    @Autowired private ClockworkMedic medic;
    @Autowired private ContextLoader contextLoader;

    @ProcessorMethod
    public <F extends FocusType> void processProjectionCredentials(LensContext<F> context,
            LensProjectionContext projectionContext, String activityDescription, XMLGregorianCalendar now, Task task,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

        try {
            processProjectionCredentials(context, projectionContext, now, task, result);
            context.checkConsistenceIfNeeded();
        } catch (MappingLoader.NotLoadedException e) {
            // Just like for activation processor.
            ErrorHandlingUtil.processProjectionNotLoadedException(e, projectionContext);
        }

        medic.traceContext(LOGGER, activityDescription,
                "projection values and credentials of " + projectionContext.getDescription(),
                false, context, true);
    }

    private <F extends FocusType> void processProjectionCredentials(LensContext<F> context,
            LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task,
            OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, PolicyViolationException, CommunicationException, ConfigurationException,
            SecurityViolationException, MappingLoader.NotLoadedException {

        SecurityPolicyType securityPolicy = determineSecurityPolicy(context, projectionContext);

        processProjectionPasswordMapping(context, projectionContext, securityPolicy, now, task, result);

        validateProjectionPassword(projectionContext, securityPolicy, now, task, result);

        applyMetadata(context, projectionContext, now, task);
    }

    private <F extends FocusType> void processProjectionPasswordMapping(LensContext<F> context,
            LensProjectionContext projCtx, SecurityPolicyType securityPolicy, XMLGregorianCalendar now,
            Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, MappingLoader.NotLoadedException {
        LensFocusContext<F> focusContext = context.getFocusContext();

        PrismObject<F> focusNew = focusContext.getObjectNew();
        if (focusNew == null) {
            // This must be a focus delete or something similar. No point in proceeding
            LOGGER.trace("focusNew is null, skipping credentials processing");
            return;
        }

        PrismObjectDefinition<ShadowType> accountDefinition = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(ShadowType.class);
        PrismPropertyDefinition<ProtectedStringType> projPasswordPropertyDefinition = accountDefinition
                .findPropertyDefinition(SchemaConstants.PATH_PASSWORD_VALUE);

        ProjectionContextKey key = projCtx.getKey();

        ResourceObjectDefinition objectDefinition = projCtx.getStructuralObjectDefinition();
        if (objectDefinition == null) {
            LOGGER.trace("No ResourceObjectTypeDefinition, therefore also no password outbound definition,"
                    + " skipping credentials processing for projection {}", key);
            return;
        }

        // [EP:M:OM] DONE as the mapping obviously belong to the resource
        List<MappingType> outboundMappingBeans = objectDefinition.getPasswordOutbound();
        if (outboundMappingBeans.isEmpty()) {
            LOGGER.trace("No outbound password mapping for {}, skipping credentials processing", key);
            return;
        }
        OriginProvider<MappingType> originProvider =
                item -> ConfigurationItemOrigin.inResourceOrAncestor(projCtx.getResourceRequired());

        ObjectDeltaObject<F> objectDeltaObject = focusContext.getObjectDeltaObjectAbsolute();

        // HACK
        if (!projCtx.isDoReconciliation()
                && !projCtx.isAdd()
                && !isActivated(outboundMappingBeans, objectDeltaObject.getObjectDelta())) {
            LOGGER.trace("Outbound password mappings not activated for type {}, skipping credentials processing", key);
            return;
        }

        ObjectDelta<ShadowType> projDelta = projCtx.getCurrentDelta();
        PropertyDelta<ProtectedStringType> projPasswordDelta;
        if (projDelta != null && projDelta.getChangeType() == MODIFY) {
            projPasswordDelta = projDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
        } else {
            projPasswordDelta = null;
        }
        checkExistingDeltaSanity(projCtx, projPasswordDelta);

        boolean evaluateWeak = getEvaluateWeak(projCtx);

        ItemDeltaItem<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>> focusPasswordIdi =
                objectDeltaObject.findIdi(SchemaConstants.PATH_PASSWORD_VALUE); // TODO wave

        ConfigurableValuePolicySupplier valuePolicySupplier = (result1) -> SecurityUtil.getPasswordPolicy(securityPolicy);

        MappingInitializer<PrismPropertyValue<ProtectedStringType>,PrismPropertyDefinition<ProtectedStringType>> initializer =
            (builder) -> {
                builder.mappingKind(MappingKindType.OUTBOUND)
                        .implicitSourcePath(SchemaConstants.PATH_PASSWORD_VALUE)
                        .implicitTargetPath(SchemaConstants.PATH_PASSWORD_VALUE);
                builder.defaultTargetDefinition(projPasswordPropertyDefinition);
                builder.defaultSource(new Source<>(focusPasswordIdi, ExpressionConstants.VAR_INPUT_QNAME));
                builder.valuePolicySupplier(valuePolicySupplier);
                return builder;
            };

        MappingOutputProcessor<PrismPropertyValue<ProtectedStringType>> processor =
                (mappingOutputPath, outputStruct) -> {
                    PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = outputStruct.getOutputTriple();
                    if (outputTriple == null) {
                        LOGGER.trace("Credentials 'password' expression resulted in null output triple, skipping credentials processing for {}", key);
                        return false;
                    }

                    boolean projectionIsNew =
                            projDelta != null
                                    && (projDelta.getChangeType() == ChangeType.ADD || projCtx.isSynchronizationDecisionAdd());

                    Collection<PrismPropertyValue<ProtectedStringType>> newValues;
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

                    Collection<PrismPropertyValue<ProtectedStringType>> minusSet = outputTriple.getMinusSet();
                    if (!minusSet.isEmpty()) {
                        if (!canGetCleartext(minusSet)) {
                            // We have hashed values in minus set. That is not great, we won't be able to get
                            // cleartext from that if we need it (e.g. for runAs in provisioning).
                            // Therefore try to get old value from focus password delta. If that matches with
                            // hashed value then we have the cleartext.
                            ProtectedStringType oldProjectionPassword = minusSet.iterator().next().getRealValue();
                            PropertyDelta<ProtectedStringType> focusPasswordDelta = (PropertyDelta<ProtectedStringType>) focusPasswordIdi.getDelta();
                            Collection<PrismPropertyValue<ProtectedStringType>> focusPasswordDeltaOldValues = focusPasswordDelta.getEstimatedOldValues();
                            if (focusPasswordDeltaOldValues != null && !focusPasswordDeltaOldValues.isEmpty()) {
                                ProtectedStringType oldFocusPassword =
                                        requireNonNull(focusPasswordDeltaOldValues.iterator().next().getRealValue());
                                try {
                                    if (oldFocusPassword.canGetCleartext() && protector.compareCleartext(oldFocusPassword, oldProjectionPassword)) {
                                        outputTriple.clearMinusSet();
                                        outputTriple.addToMinusSet(prismContext.itemFactory().createPropertyValue(oldFocusPassword));
                                    }
                                } catch (EncryptionException e) {
                                    throw new SystemException(e.getMessage(), e);
                                }
                            }
                        }
                    }

                    return true;
                };

        String projCtxDesc = projCtx.toHumanReadableString();
        PrismObject<ShadowType> shadowNew = projCtx.getObjectNew();

        MappingInitializer<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>> internalInitializer =
                builder -> {

                    builder.addVariableDefinitions(ModelImplUtils.getDefaultVariablesMap(context, projCtx, true));

                    builder.mappingKind(MappingKindType.OUTBOUND);
                    builder.originType(OriginType.OUTBOUND);
                    builder.implicitTargetPath(SchemaConstants.PATH_PASSWORD_VALUE);
                    builder.originObject(projCtx.getResource());

                    initializer.initialize(builder);

                    return builder;
                };

        MappingEvaluatorParams<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>, ShadowType, F> params = new MappingEvaluatorParams<>();
        params.setMappingConfigItems( // [EP:M:OM] DONE, see above
                ConfigurationItem.ofList(outboundMappingBeans, originProvider, MappingConfigItem.class));
        params.setMappingDesc("password mapping" + " in projection " + projCtxDesc);
        params.setNow(now);
        params.setInitializer(internalInitializer);
        params.setProcessor(processor);
        params.setTargetLoader(new ProjectionMappingLoader(projCtx, contextLoader, projCtx::isPasswordValueLoaded));
        params.setTargetValueAvailable(projCtx.isPasswordValueLoaded());
        params.setAPrioriTargetObject(shadowNew);
        params.setAPrioriTargetDelta(LensUtil.findAPrioriDelta(context, projCtx));
        params.setTargetContext(projCtx);
        params.setDefaultTargetItemPath(SchemaConstants.PATH_PASSWORD_VALUE);
        if (context.getFocusContext() != null) {
            params.setSourceContext(context.getFocusContext().getObjectDeltaObjectAbsolute());
        }
        params.setEvaluateCurrent(MappingTimeEval.CURRENT);
        params.setEvaluateWeak(evaluateWeak);
        params.setContext(context);
        projectionMappingSetEvaluator.evaluateMappingsToTriples(params, task, result);
    }

    private <F extends FocusType> boolean isActivated(List<MappingType> outboundMappingBeans, ObjectDelta<F> focusDelta) {
        if (focusDelta == null) {
            return false;
        }
        for (MappingType outboundMappingBean: outboundMappingBeans) {
            List<VariableBindingDefinitionType> sources = outboundMappingBean.getSource();
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
        CredentialsCapabilityType credentialsCapabilityType =
                ResourceTypeUtil.getEnabledCapability(projCtx.getResource(), CredentialsCapabilityType.class);
        if (CapabilityUtil.isPasswordReadable(credentialsCapabilityType)) {
            return true;
        }
        // Password not readable. Therefore evaluate weak mappings only during add operations.
        // We do not know whether there is a password already set on the resource. And we do not
        // want to overwrite it every time.
        return projCtx.isAdd();
    }

    private void validateProjectionPassword(
            LensProjectionContext projectionContext,
            SecurityPolicyType securityPolicy,
            XMLGregorianCalendar now,
            Task task,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        if (securityPolicy == null) {
            LOGGER.trace("Skipping processing password policies. Security policy not specified.");
            return;
        }

        ObjectDelta<ShadowType> accountDelta = projectionContext.getCurrentDelta();

        if (accountDelta == null) {
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
            if (passwordValueDelta == null) {
                LOGGER.trace("Skipping processing password policies. Shadow delta does not contain password change.");
                return;
            }
            // Modification sanity check
            if (accountDelta.getChangeType() == ChangeType.MODIFY
                    && (passwordValueDelta.isAdd() || passwordValueDelta.isDelete())) {
                throw new SchemaException("Shadow password value cannot be added or deleted, it can only be replaced");
            }
            password = (PrismProperty<ProtectedStringType>) passwordValueDelta.getItemNewMatchingPath(null);
        }

        if (accountShadow == null) {
            accountShadow = projectionContext.getObjectNew();
        }

        String passwordValue = determinePasswordClearValue(password);

        ObjectValuePolicyEvaluator objectValuePolicyEvaluator = new ObjectValuePolicyEvaluator.Builder()
                .now(now)
                .originResolver(getOriginResolver(accountShadow))
                .protector(protector)
                .securityPolicy(securityPolicy)
                .shortDesc("password for " + accountShadow)
                .task(task)
                .valueItemPath(SchemaConstants.PATH_PASSWORD_VALUE)
                .valuePolicyProcessor(valuePolicyProcessor)
                .build();
        OperationResult validationResult = objectValuePolicyEvaluator.validateStringValue(passwordValue, result);

        if (!validationResult.isSuccess()) {
            LOGGER.debug("Password for projection {} is not valid (policy={}): {}",
                    projectionContext.getHumanReadableName(), securityPolicy, validationResult.getUserFriendlyMessage());
            result.computeStatus();
            throw new PolicyViolationException(
                    new LocalizableMessageBuilder()
                            .key("PolicyViolationException.message.projectionPassword")
                            .arg(projectionContext.getHumanReadableName())
                            .arg(validationResult.getUserFriendlyMessage())
                            .build());
        }
    }

    private ShadowValuePolicyOriginResolver getOriginResolver(PrismObject<ShadowType> accountShadow) {
        return new ShadowValuePolicyOriginResolver(accountShadow, modelObjectResolver);
    }

    private <F extends FocusType> void applyMetadata(
            LensContext<F> context, LensProjectionContext projectionContext,
            XMLGregorianCalendar now, Task task) throws SchemaException {

        if (projectionContext.isDelete()) {
            return;
        }

        ObjectDelta<ShadowType> accountDelta = projectionContext.getCurrentDelta();
        if (accountDelta == null) {
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
            var metadataToSet = operationalDataManager.createCreateMetadata(context, now, task);
            metadataToSet.asPrismContainerValue().setOriginTypeRecursive(OriginType.OUTBOUND);
            projectionContext.swallowToSecondaryDelta(
                    prismContext.deltaFor(ShadowType.class)
                            .item(SchemaConstants.PATH_PASSWORD_METADATA)
                            .add(metadataToSet)
                            .asItemDelta());

        } else if (projectionContext.isModify()) {
            if (!accountDelta.hasItemDelta(SchemaConstants.PATH_PASSWORD_METADATA)) {
                var deltas = operationalDataManager.createCredentialsModificationRelatedStorageMetadataDeltas(
                        context,
                        SchemaConstants.PATH_PASSWORD,
                        getCurrentPasswordContainerValue(projectionContext),
                        context.getFocusClass(), now, task);

                for (ItemDelta<?, ?> delta : deltas) {
                    delta.setOriginTypeRecursive(OriginType.OUTBOUND);
                    projectionContext.swallowToSecondaryDelta(delta);
                }
            }
        }
    }

    private AbstractCredentialType getCurrentPasswordContainerValue(LensProjectionContext projectionContext) {
        var passwordContainer = projectionContext.getObjectCurrentRequired().findContainer(SchemaConstants.PATH_PASSWORD);
        if (passwordContainer == null || passwordContainer.hasNoValues()) {
            return null;
        } else {
            return (AbstractCredentialType) passwordContainer.getRealValue();
        }
    }

    private <F extends FocusType> SecurityPolicyType determineSecurityPolicy(
            LensContext<F> context, LensProjectionContext projCtx) {
        SecurityPolicyType securityPolicy = projCtx.getProjectionSecurityPolicy();
        if (securityPolicy != null) {
            return securityPolicy;
        } else {
            return context.getGlobalSecurityPolicy();
        }
    }

    private String determinePasswordClearValue(PrismProperty<ProtectedStringType> password) {
        var prismValue = password != null ? password.getValue(ProtectedStringType.class) : null;
        var realValue = prismValue != null ? prismValue.getRealValue() : null;
        return determinePasswordClearValue(realValue);
    }

    private String determinePasswordClearValue(ProtectedStringType passValue) {
        if (passValue == null) {
            return null;
        }

        String clearValue = passValue.getClearValue();
        if (clearValue != null) {
            return clearValue;
        } else if (passValue.isEncrypted()) {
            // TODO: is this appropriate handling???
            try {
                return protector.decryptString(passValue);
            } catch (EncryptionException ex) {
                throw new SystemException("Failed to process password for focus: " + ex.getMessage(), ex);
            }
        } else {
            return null;
        }
    }

    private void checkExistingDeltaSanity(LensProjectionContext projCtx,
            PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
        if (passwordDelta != null && (passwordDelta.isAdd() || passwordDelta.isDelete())) {
            throw new SchemaException("Password for projection " + projCtx.getKey()
                    + " cannot be added or deleted, it can only be replaced");
        }
    }
}
