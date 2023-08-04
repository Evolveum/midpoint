/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import static com.evolveum.midpoint.prism.PrismContainerValue.asContainerable;
import static com.evolveum.midpoint.prism.delta.ChangeType.MODIFY;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ProjectionMappingSetEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.*;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.stringpolicy.ObjectValuePolicyEvaluator;
import com.evolveum.midpoint.model.common.stringpolicy.ShadowValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.Source;
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

        processProjectionCredentials(context, projectionContext, now, task, result);
        context.checkConsistenceIfNeeded();

        projectionContext.recompute();
        context.checkConsistenceIfNeeded();

        medic.traceContext(LOGGER, activityDescription, "projection values and credentials of "+projectionContext.getDescription(), false, context, true);
    }

    private <F extends FocusType> void processProjectionCredentials(LensContext<F> context,
            LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task,
            OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
                    SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {

        SecurityPolicyType securityPolicy = determineSecurityPolicy(context, projectionContext);

        processProjectionPasswordMapping(context, projectionContext, securityPolicy, now, task, result);

        validateProjectionPassword(projectionContext, securityPolicy, now, task, result);

        applyMetadata(context, projectionContext, now, task);
    }

    private <F extends FocusType> void processProjectionPasswordMapping(LensContext<F> context,
            final LensProjectionContext projCtx, final SecurityPolicyType securityPolicy, XMLGregorianCalendar now, Task task, OperationResult result)
                    throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
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

        List<MappingType> outboundMappingBeans = objectDefinition.getPasswordOutbound();
        if (outboundMappingBeans.isEmpty()) {
            LOGGER.trace("No outbound password mapping for {}, skipping credentials processing", key);
            return;
        }

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

                    boolean projectionIsNew = projDelta != null && (projDelta.getChangeType() == ChangeType.ADD
                            || projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD);

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

        // FIXME Undetermined because of resource/object type inheritance
        var originProvider = OriginProvider.undetermined();

        MappingEvaluatorParams<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>, ShadowType, F> params = new MappingEvaluatorParams<>();
        params.setMappingBeans(ConfigurationItem.ofList(outboundMappingBeans, originProvider));
        params.setMappingDesc("password mapping" + " in projection " + projCtxDesc);
        params.setNow(now);
        params.setInitializer(internalInitializer);
        params.setProcessor(processor);
        params.setTargetLoader(new ProjectionMappingLoader<>(projCtx, contextLoader));
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
        params.setHasFullTargetObject(projCtx.hasFullShadow());
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
        // Password not readable. Therefore evaluate weak mappings only during add operaitons.
        // We do not know whether there is a password already set on the resource. And we do not
        // want to overwrite it every time.
        return projCtx.isAdd();
    }

    private <F extends FocusType> void validateProjectionPassword(
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

//        boolean isValid = valuePolicyProcessor.validateValue(passwordValue, securityPolicy, getOriginResolver(accountShadow), "projection password policy", task, result);

        if (!validationResult.isSuccess()) {
            LOGGER.debug("Password for projection {} is not valid (policy={}): {}", projectionContext.getHumanReadableName(), securityPolicy, validationResult.getUserFriendlyMessage());
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

    private <F extends FocusType> void applyMetadata(LensContext<F> context, LensProjectionContext projectionContext,
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
            MetadataType metadataType = operationalDataManager.createCreateMetadata(context, now, task);
            ContainerDelta<MetadataType> metadataDelta = prismContext.deltaFactory().container()
                    .createDelta(SchemaConstants.PATH_PASSWORD_METADATA, projectionContext.getObjectDefinition());
            PrismContainerValue<?> cval = metadataType.asPrismContainerValue();
            cval.setOriginTypeRecursive(OriginType.OUTBOUND);
            metadataDelta.addValuesToAdd(metadataType.asPrismContainerValue());
            projectionContext.swallowToSecondaryDelta(metadataDelta);

        } else if (projectionContext.isModify()) {
            ContainerDelta<MetadataType> metadataDelta = accountDelta.findContainerDelta(SchemaConstants.PATH_PASSWORD_METADATA);
            if (metadataDelta == null) {
                MetadataType currentMetadata = getCurrentPasswordMetadata(projectionContext);
                Collection<? extends ItemDelta<?,?>> modifyMetadataDeltas = operationalDataManager.createModifyMetadataDeltas(
                        context, currentMetadata, SchemaConstants.PATH_PASSWORD_METADATA, projectionContext.getObjectTypeClass(),
                        now, task);
                for (ItemDelta<?, ?> itemDelta: modifyMetadataDeltas) {
                    itemDelta.setOriginTypeRecursive(OriginType.OUTBOUND);
                    projectionContext.swallowToSecondaryDelta(itemDelta);
                }
            }
        }
    }

    private MetadataType getCurrentPasswordMetadata(LensProjectionContext projectionContext) {
        PrismObject<ShadowType> objectCurrent = projectionContext.getObjectCurrent();
        if (objectCurrent != null) {
            PrismContainer<MetadataType> metadataContainer = objectCurrent.findContainer(SchemaConstants.PATH_PASSWORD_METADATA);
            return metadataContainer != null && metadataContainer.hasAnyValue() ?
                    asContainerable(metadataContainer.getValue()) : null;
        } else {
            return null;
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
                throw new SystemException("Failed to process password for focus: " + ex.getMessage(), ex);
            }
        }

        return passwordStr;
    }

    private void checkExistingDeltaSanity(LensProjectionContext projCtx,
            PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
        if (passwordDelta != null && (passwordDelta.isAdd() || passwordDelta.isDelete())) {
            throw new SchemaException("Password for projection " + projCtx.getKey()
                    + " cannot be added or deleted, it can only be replaced");
        }
    }
}
