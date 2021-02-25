/*

 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;
import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.common.refinery.RefinedDefinitionUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Synchronization service receives change notifications from provisioning. It
 * decides which synchronization policy to use and evaluates it (correlation,
 * confirmation, situations, reaction, ...)
 * <p>
 * Note: don't autowire this bean by implementing class, as it is
 * proxied by Spring AOP. Use the interface instead.
 *
 * @author lazyman
 * @author Radovan Semancik
 */
@Service(value = "synchronizationService")
public class SynchronizationServiceImpl implements SynchronizationService {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationServiceImpl.class);

    private static final String CLASS_NAME_WITH_DOT = SynchronizationServiceImpl.class.getName() + ".";
    private static final String NOTIFY_CHANGE = CLASS_NAME_WITH_DOT + "notifyChange";

    @Autowired private ActionManager<Action> actionManager;
    @Autowired private SynchronizationExpressionsEvaluator synchronizationExpressionsEvaluator;
    @Autowired private ContextFactory contextFactory;
    @Autowired private Clockwork clockwork;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private PrismContext prismContext;
    @Autowired private Clock clock;
    @Autowired private ClockworkMedic clockworkMedic;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Override
    public void notifyChange(ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {
        validate(change);
        Validate.notNull(parentResult, "Parent operation result must not be null.");

        boolean logDebug = isLogDebug(change);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("SYNCHRONIZATION: received change notification:\n{}", change.debugDump(1));
        } else if (logDebug) {
            LOGGER.debug("SYNCHRONIZATION: received change notification {}", change);
        }

        OperationResult subResult = parentResult.subresult(NOTIFY_CHANGE)
                .addArbitraryObjectAsParam("change", change)
                .addArbitraryObjectAsContext("task", task)
                .build();

        if (change.isCleanDeadShadow()) {
            cleanDeadShadow(change, subResult);
            subResult.computeStatus();
            return;
        }

        PrismObject<ShadowType> currentShadow = change.getShadowedResourceObject();
        String applicableShadowOid = currentShadow != null ? currentShadow.getOid() : null;

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        try {
            PrismObject<SystemConfigurationType> configuration = systemObjectCache.getSystemConfiguration(subResult);
            SynchronizationContext<?> syncCtx = loadSynchronizationContext(
                    currentShadow, currentShadow, change.getObjectDelta(), change.getResource(),
                    change.getSourceChannel(), change.getItemProcessingIdentifier(), configuration, task, subResult);
            LOGGER.trace("SYNCHRONIZATION determined policy: {}", syncCtx);

            if (!checkSynchronizationPolicy(syncCtx, subResult) || !checkProtected(syncCtx, subResult)) {
                return;
            }

            LOGGER.trace("Synchronization is enabled, focus class: {}, found applicable policy: {}", syncCtx.getFocusClass(),
                    syncCtx.getPolicyName());

            setupSituation(syncCtx, change, subResult);

            task.onSynchronizationStart(change.getItemProcessingIdentifier(), applicableShadowOid, syncCtx.getSituation());

            boolean isDryRun = TaskUtil.isDryRun(syncCtx.getTask());
            saveSyncMetadata(syncCtx, change, !isDryRun, now, subResult);

            if (isDryRun) {
                LOGGER.debug("SYNCHRONIZATION: DONE (dry run) for {}", syncCtx.getApplicableShadow());
                subResult.recordSuccess();
            } else {
                LOGGER.trace("Synchronization context:\n{}", syncCtx.debugDumpLazily(1));
                reactToChange(syncCtx, change, logDebug, subResult);
                LOGGER.debug("SYNCHRONIZATION: DONE for {}", currentShadow);
                subResult.computeStatus();
            }
        } catch (SystemException ex) {
            // avoid unnecessary re-wrap
            subResult.recordFatalError(ex);
            throw ex;
        } catch (Exception ex) {
            subResult.recordFatalError(ex);
            throw new SystemException(ex);
        } finally {
            task.markObjectActionExecutedBoundary();
        }
    }

    private <F extends FocusType> void cleanDeadShadow(ResourceObjectShadowChangeDescription change, OperationResult subResult) {
        LOGGER.trace("Cleaning old dead shadows, checking for old links, cleaning them up");
        String shadowOid = change.getShadowOid();
        if (shadowOid == null) {
            LOGGER.trace("No shadow oid, nothing to clean up.");
            return;
        }

        PrismObject<F> currentOwner = repositoryService.searchShadowOwner(shadowOid,
                SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()), subResult);
        if (currentOwner == null) {
            LOGGER.trace("Nothing to do, shadow doesn't have any owner.");
            return;
        }

        try {

            F ownerType = currentOwner.asObjectable();
            for (ObjectReferenceType linkRef : ownerType.getLinkRef()) {
                if (shadowOid.equals(linkRef.getOid())) {
                    Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFactory().reference().createModificationDeleteCollection(FocusType.F_LINK_REF, currentOwner.getDefinition(), linkRef.asReferenceValue().clone());
                    repositoryService.modifyObject(UserType.class, currentOwner.getOid(), modifications, subResult);
                    break;
                }
            }
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            LOGGER.error("SYNCHRONIZATION: Error in synchronization - clean up dead shadows. Change: {}", change, e);
            subResult.recordFatalError("Error while cleaning dead shadow, " + e.getMessage(), e);
            //nothing more to do. and we don't want to trow exception to not cancel the whole execution.
        }

        subResult.computeStatus();
    }

    @Override
    public <F extends FocusType> SynchronizationContext<F> loadSynchronizationContext(PrismObject<ShadowType> applicableShadow,
            PrismObject<ShadowType> resourceObject, ObjectDelta<ShadowType> resourceObjectDelta,
            PrismObject<ResourceType> resource, String sourceChanel,
            String itemProcessingIdentifier, PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        SynchronizationContext<F> syncCtx = new SynchronizationContext<>(applicableShadow, resourceObject, resourceObjectDelta,
                resource, sourceChanel, prismContext, expressionFactory, task, itemProcessingIdentifier);
        syncCtx.setSystemConfiguration(configuration);

        SynchronizationType synchronization = resource.asObjectable().getSynchronization();
        if (synchronization == null) {
            return syncCtx;
        }

        ObjectSynchronizationDiscriminatorType synchronizationDiscriminator = determineObjectSynchronizationDiscriminatorType(syncCtx, task, result);
        if (synchronizationDiscriminator != null) {
            syncCtx.setForceIntentChange(true);
            LOGGER.trace("Setting synchronization situation to synchronization context: {}", synchronizationDiscriminator.getSynchronizationSituation());
            syncCtx.setSituation(synchronizationDiscriminator.getSynchronizationSituation());
            F owner = syncCtx.getCurrentOwner();
            if (owner != null && alreadyLinked(owner, syncCtx.getApplicableShadow())) {
                LOGGER.trace("Setting owner to synchronization context: {}", synchronizationDiscriminator.getOwner());
                //noinspection unchecked
                syncCtx.setCurrentOwner((F) synchronizationDiscriminator.getOwner());
            }
            LOGGER.trace("Setting correlated owner to synchronization context: {}", synchronizationDiscriminator.getOwner());
            //noinspection unchecked
            syncCtx.setCorrelatedOwner((F) synchronizationDiscriminator.getOwner());
        }

        for (ObjectSynchronizationType objectSynchronization : synchronization.getObjectSynchronization()) {
            if (isPolicyApplicable(objectSynchronization, synchronizationDiscriminator, syncCtx, result)) {
                syncCtx.setObjectSynchronization(objectSynchronization);
                break;
            }
        }

        processTag(syncCtx, result);

        return syncCtx;
    }

    private <F extends FocusType> void processTag(SynchronizationContext<F> syncCtx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        PrismObject<ShadowType> applicableShadow = syncCtx.getApplicableShadow();
        if (applicableShadow == null) {
            return;
        }
        if (applicableShadow.asObjectable().getTag() != null) {
            return;
        }
        RefinedObjectClassDefinition rOcd = syncCtx.findRefinedObjectClassDefinition();
        if (rOcd == null) {
            // We probably do not have kind/intent yet.
            return;
        }
        ResourceObjectMultiplicityType multiplicity = rOcd.getMultiplicity();
        if (!RefinedDefinitionUtil.isMultiaccount(multiplicity)) {
            return;
        }
        String tag = synchronizationExpressionsEvaluator.generateTag(multiplicity, applicableShadow,
                syncCtx.getResource(), syncCtx.getSystemConfiguration(), "tag expression for " + applicableShadow, syncCtx.getTask(), result);
        LOGGER.debug("SYNCHRONIZATION: TAG generated: {}", tag);
        syncCtx.setTag(tag);
    }

    private <F extends FocusType> ObjectSynchronizationDiscriminatorType determineObjectSynchronizationDiscriminatorType(SynchronizationContext<F> syncCtx, Task task, OperationResult subResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        SynchronizationType synchronizationType = syncCtx.getResource().asObjectable().getSynchronization();
        if (synchronizationType == null) {
            return null;
        }

        ObjectSynchronizationSorterType sorter = synchronizationType.getObjectSynchronizationSorter();
        if (sorter == null) {
            return null;
        }

        return evaluateSynchronizationSorter(sorter, syncCtx, task, subResult);

    }

    private <F extends FocusType> boolean isPolicyApplicable(ObjectSynchronizationType synchronizationPolicy,
            ObjectSynchronizationDiscriminatorType synchronizationDiscriminator, SynchronizationContext<F> syncCtx,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return SynchronizationServiceUtils.isPolicyApplicable(synchronizationPolicy, synchronizationDiscriminator, expressionFactory, syncCtx, result);
    }

    private <F extends FocusType> ObjectSynchronizationDiscriminatorType evaluateSynchronizationSorter(ObjectSynchronizationSorterType synchronizationSorterType,
            SynchronizationContext<F> syncCtx, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (synchronizationSorterType.getExpression() == null) {
            return null;
        }
        ExpressionType classificationExpression = synchronizationSorterType.getExpression();
        String desc = "synchronization divider type ";
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(null, syncCtx.getApplicableShadow(), null,
                syncCtx.getResource(), syncCtx.getSystemConfiguration(), null, syncCtx.getPrismContext());
        variables.put(ExpressionConstants.VAR_CHANNEL, syncCtx.getChannel(), String.class);
        try {
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
            //noinspection unchecked
            PrismPropertyDefinition<ObjectSynchronizationDiscriminatorType> discriminatorDef = prismContext.getSchemaRegistry()
                    .findPropertyDefinitionByElementName(new QName(SchemaConstants.NS_C, "objectSynchronizationDiscriminator"));
            PrismPropertyValue<ObjectSynchronizationDiscriminatorType> evaluateDiscriminator = ExpressionUtil.evaluateExpression(variables, discriminatorDef,
                    classificationExpression, syncCtx.getExpressionProfile(), expressionFactory, desc, task, result);
            return getRealValue(evaluateDiscriminator);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    private <F extends FocusType> boolean checkSynchronizationPolicy(SynchronizationContext<F> syncCtx,
            OperationResult result) throws SchemaException {
        Task task = syncCtx.getTask();

        if (!syncCtx.hasApplicablePolicy()) {
            String message = "SYNCHRONIZATION no matching policy for " + syncCtx.getApplicableShadow() + " ("
                    + syncCtx.getApplicableShadow().asObjectable().getObjectClass() + ") " + " on " + syncCtx.getResource()
                    + ", ignoring change from channel " + syncCtx.getChannel();
            LOGGER.debug(message);
            List<PropertyDelta<?>> modifications = createShadowIntentAndSynchronizationTimestampDelta(syncCtx, false);
            executeShadowModifications(syncCtx.getApplicableShadow(), modifications, task, result);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, message);
            task.onSynchronizationExclusion(syncCtx.getItemProcessingIdentifier(), SynchronizationExclusionReasonType.NO_SYNCHRONIZATION_POLICY);
            return false;
        }

        if (!syncCtx.isSynchronizationEnabled()) {
            String message = "SYNCHRONIZATION is not enabled for " + syncCtx.getResource()
                    + " ignoring change from channel " + syncCtx.getChannel();
            LOGGER.debug(message);
            List<PropertyDelta<?>> modifications = createShadowIntentAndSynchronizationTimestampDelta(syncCtx, true);
            executeShadowModifications(syncCtx.getApplicableShadow(), modifications, task, result);
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, message);
            task.onSynchronizationExclusion(syncCtx.getItemProcessingIdentifier(), SynchronizationExclusionReasonType.SYNCHRONIZATION_DISABLED);
            return false;
        }

        return true;
    }

    private <F extends FocusType> boolean checkProtected(SynchronizationContext<F> syncCtx,
            OperationResult result) throws SchemaException {
        if (syncCtx.isProtected()) {
            Task task = syncCtx.getTask();
            List<PropertyDelta<?>> modifications = createShadowIntentAndSynchronizationTimestampDelta(syncCtx, true);
            executeShadowModifications(syncCtx.getApplicableShadow(), modifications, task, result);
            result.recordSuccess();
            task.onSynchronizationExclusion(syncCtx.getItemProcessingIdentifier(), SynchronizationExclusionReasonType.PROTECTED);
            LOGGER.debug("SYNCHRONIZATION: DONE for protected shadow {}", syncCtx.getApplicableShadow());
            return false;
        }
        return true;
    }

    private <F extends FocusType> List<PropertyDelta<?>> createShadowIntentAndSynchronizationTimestampDelta(SynchronizationContext<F> syncCtx, boolean saveIntent) throws SchemaException {
        Validate.notNull(syncCtx.getApplicableShadow(), "No current nor old shadow present: ");
        ShadowType applicableShadowType = syncCtx.getApplicableShadow().asObjectable();
        List<PropertyDelta<?>> modifications = SynchronizationUtils.createSynchronizationTimestampsDelta(syncCtx.getApplicableShadow(),
                prismContext);
        if (saveIntent) {
            if (StringUtils.isNotBlank(syncCtx.getIntent()) && !syncCtx.getIntent().equals(applicableShadowType.getIntent())) {
                PropertyDelta<String> intentDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(ShadowType.F_INTENT,
                        syncCtx.getApplicableShadow().getDefinition(), syncCtx.getIntent());
                modifications.add(intentDelta);
            }
            if (StringUtils.isNotBlank(syncCtx.getTag()) && !syncCtx.getTag().equals(applicableShadowType.getTag())) {
                PropertyDelta<String> tagDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(ShadowType.F_TAG,
                        syncCtx.getApplicableShadow().getDefinition(), syncCtx.getTag());
                modifications.add(tagDelta);
            }
        }
        return modifications;
    }

    private void executeShadowModifications(PrismObject<? extends ShadowType> object, List<PropertyDelta<?>> modifications,
            Task task, OperationResult subResult) {
        try {
            repositoryService.modifyObject(ShadowType.class, object.getOid(), modifications, subResult);
            task.recordObjectActionExecuted(object, ChangeType.MODIFY, null);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(object, ChangeType.MODIFY, t);
        } finally {
            task.markObjectActionExecutedBoundary();
        }
    }

    private <F extends FocusType> boolean alreadyLinked(F focus, PrismObject<ShadowType> shadow) {
        return focus.getLinkRef().stream().anyMatch(link -> link.getOid().equals(shadow.getOid()));
    }

    private boolean isLogDebug(ResourceObjectShadowChangeDescription change) {
        // Reconciliation changes are routine. Do not let them pollute the log files.
        return !SchemaConstants.CHANNEL_RECON_URI.equals(change.getSourceChannel());
    }

    private void validate(ResourceObjectShadowChangeDescription change) {
        Validate.notNull(change, "Resource object shadow change description must not be null.");
        Validate.notNull(change.getShadowedResourceObject(), "Current shadow must not be null.");
        Validate.notNull(change.getResource(), "Resource in change must not be null.");

        if (consistencyChecks) {
            if (change.getShadowedResourceObject() != null) {
                change.getShadowedResourceObject().checkConsistence();
                ShadowUtil.checkConsistence(change.getShadowedResourceObject(),
                        "current shadow in change description");
            }
            if (change.getObjectDelta() != null) {
                change.getObjectDelta().checkConsistence();
            }
        }
    }

    /**
     * XXX: in situation when one account belongs to two different idm users
     * repository returns only first user, see method findShadowOwner. It
     * should be changed because otherwise we can't find
     * {@link SynchronizationSituationType#DISPUTED} situation
     */
    private <F extends FocusType> void setupSituation(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, OperationResult result) {

        Task task = syncCtx.getTask();
        OperationResult subResult = result.subresult(CLASS_NAME_WITH_DOT + "setupSituation")
                .setMinor()
                .addArbitraryObjectAsParam("syncCtx", syncCtx)
                .addArbitraryObjectAsParam("change", change)
                .build();
        LOGGER.trace("Determining situation for resource object shadow.");

        try {
            String shadowOid = change.getShadowOid();
            Validate.notEmpty(shadowOid, "Couldn't get resource object shadow oid from change.");

            F currentOwner;
            if (syncCtx.getCurrentOwner() != null) {
                currentOwner = syncCtx.getCurrentOwner();
            } else {
                PrismObject<F> currentOwnerObject = repositoryService.searchShadowOwner(shadowOid,
                        SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()), subResult);
                currentOwner = currentOwnerObject != null ? currentOwnerObject.asObjectable() : null;
            }

            F correlatedOwner = syncCtx.getCorrelatedOwner();
            if (!isCorrelatedOwnerSameAsCurrentOwner(correlatedOwner, currentOwner)) {
                LOGGER.error("Cannot synchronize {}, current owner and expected owner are not the same. Current owner: {}, expected owner: {}", syncCtx.getApplicableShadow(), currentOwner, correlatedOwner);
                String msg = "Cannot synchronize " + syncCtx.getApplicableShadow()
                        + ", current owner and expected owner are not the same. Current owner: " + currentOwner
                        + ", expected owner: " + correlatedOwner;
                result.recordFatalError(msg);
                throw new ConfigurationException(msg);
            }

            if (currentOwner != null) {

                LOGGER.trace("Shadow OID {} does have owner: {}", shadowOid, currentOwner.getName());

                syncCtx.setCurrentOwner(currentOwner);

                if (syncCtx.getSituation() != null) {
                    return;
                }

                SynchronizationSituationType state;
                ChangeType changeType = getModificationType(change);
                switch (changeType) {
                    case ADD:
                    case MODIFY:
                        // if user is found it means account/group is linked to
                        // resource
                        state = SynchronizationSituationType.LINKED;
                        break;
                    case DELETE:
                        state = SynchronizationSituationType.DELETED;
                        break;
                    default:
                        throw new AssertionError(changeType);
                }
                syncCtx.setSituation(state);
            } else {
                LOGGER.trace("Resource object shadow doesn't have owner.");
                determineSituationWithCorrelation(syncCtx, change, task, result);
            }
        } catch (Exception ex) {
            LOGGER.error("Error occurred during resource object shadow owner lookup.");
            throw new SystemException(
                    "Error occurred during resource object shadow owner lookup, reason: " + ex.getMessage(), ex);
        } finally {
            subResult.computeStatus();
            String syncSituationValue = syncCtx.getSituation() != null ? syncCtx.getSituation().value() : null;
            if (isLogDebug(change)) {
                LOGGER.debug("SYNCHRONIZATION: SITUATION: '{}', currentOwner={}, correlatedOwner={}",
                        syncSituationValue, syncCtx.getCurrentOwner(),
                        syncCtx.getCorrelatedOwner());
            } else {
                LOGGER.trace("SYNCHRONIZATION: SITUATION: '{}', currentOwner={}, correlatedOwner={}",
                        syncSituationValue, syncCtx.getCurrentOwner(),
                        syncCtx.getCorrelatedOwner());
            }
        }
    }

    private <F extends FocusType> boolean isCorrelatedOwnerSameAsCurrentOwner(F expectedOwner, F currentOwnerType) {
        return expectedOwner == null || currentOwnerType == null || expectedOwner.getOid().equals(currentOwnerType.getOid());
    }

    /**
     * Tries to match specified focus and shadow. Return true if it matches,
     * false otherwise.
     */
    @Override
    public <F extends FocusType> boolean matchUserCorrelationRule(PrismObject<ShadowType> shadow,
            PrismObject<F> focus, ResourceType resourceType,
            PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result)
            throws ConfigurationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException {

        SynchronizationContext<F> synchronizationContext = loadSynchronizationContext(shadow, shadow, null,
                resourceType.asPrismObject(), task.getChannel(), null, configuration, task, result);
        return synchronizationExpressionsEvaluator.matchFocusByCorrelationRule(synchronizationContext, focus, result);
    }

    /**
     * account is not linked to user. you have to use correlation and
     * confirmation rule to be sure user for this account doesn't exists
     * resourceShadow only contains the data that were in the repository before
     * the change. But the correlation/confirmation should work on the updated
     * data. Therefore let's apply the changes before running
     * correlation/confirmation
     */
    private <F extends FocusType> void determineSituationWithCorrelation(SynchronizationContext<F> syncCtx, ResourceObjectShadowChangeDescription change,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        if (ChangeType.DELETE.equals(getModificationType(change))) {
            // account was deleted and we know it didn't have owner
            if (syncCtx.getSituation() == null) {
                syncCtx.setSituation(SynchronizationSituationType.DELETED);
            }
            return;
        }

        F user = syncCtx.getCorrelatedOwner();
        LOGGER.trace("Correlated owner present in synchronization context: {}", user);
        if (user != null) {
            if (syncCtx.getSituation() != null) {
                return;
            }
            syncCtx.setSituation(getSynchronizationSituationFromChange(change));
            return;
        }

        PrismObject<? extends ShadowType> resourceShadow = change.getShadowedResourceObject();

        ObjectDelta<ShadowType> syncDelta = change.getObjectDelta();
        if (resourceShadow == null && syncDelta != null && ChangeType.ADD.equals(syncDelta.getChangeType())) {
            LOGGER.trace("Trying to compute current shadow from change delta add.");
            PrismObject<ShadowType> shadow = syncDelta.computeChangedObject(syncDelta.getObjectToAdd());
            resourceShadow = shadow;
            change.setShadowedResourceObject(shadow);
        }
        Validate.notNull(resourceShadow, "Current shadow must not be null.");

        ResourceType resource = change.getResource().asObjectable();
        validateResourceInShadow(resourceShadow.asObjectable(), resource);

        SynchronizationSituationType state;
        LOGGER.trace("SYNCHRONIZATION: CORRELATION: Looking for list of {} objects based on correlation rule.",
                syncCtx.getFocusClass().getSimpleName());
        List<PrismObject<F>> users = synchronizationExpressionsEvaluator.findFocusesByCorrelationRule(syncCtx.getFocusClass(),
                resourceShadow.asObjectable(), syncCtx.getCorrelation(), resource,
                syncCtx.getSystemConfiguration().asObjectable(), task, result);
        if (users == null) {
            users = new ArrayList<>();
        }

        if (users.size() > 1) {
            if (syncCtx.getConfirmation() == null) {
                LOGGER.trace("SYNCHRONIZATION: CONFIRMATION: no confirmation defined.");
            } else {
                LOGGER.debug("SYNCHRONIZATION: CONFIRMATION: Checking objects from correlation with confirmation rule.");
                users = synchronizationExpressionsEvaluator.findUserByConfirmationRule(syncCtx.getFocusClass(), users,
                        resourceShadow.asObjectable(), resource, syncCtx.getSystemConfiguration().asObjectable(),
                        syncCtx.getConfirmation(), task, result);
            }
        }

        switch (users.size()) {
            case 0:
                state = SynchronizationSituationType.UNMATCHED;
                break;
            case 1:
                state = getSynchronizationSituationFromChange(change);

                user = users.get(0).asObjectable();
                break;
            default:
                state = SynchronizationSituationType.DISPUTED;
        }

        syncCtx.setCorrelatedOwner(user);
        syncCtx.setSituation(state);
    }

    private SynchronizationSituationType getSynchronizationSituationFromChange(ResourceObjectShadowChangeDescription change) {
        switch (getModificationType(change)) {
            case ADD:
            case MODIFY:
                return SynchronizationSituationType.UNLINKED;
            case DELETE:
                return SynchronizationSituationType.DELETED;
        }

        return null;
    }

    private void validateResourceInShadow(ShadowType shadow, ResourceType resource) {
        if (shadow.getResourceRef() != null) {
            return;
        }

        ObjectReferenceType reference = new ObjectReferenceType();
        reference.setOid(resource.getOid());
        reference.setType(ObjectTypes.RESOURCE.getTypeQName());

        shadow.setResourceRef(reference);
    }

    /**
     * @return method checks change type in object delta if available, otherwise
     * returns {@link ChangeType#ADD}
     */
    private ChangeType getModificationType(ResourceObjectShadowChangeDescription change) {
        if (change.getObjectDelta() != null) {
            return change.getObjectDelta().getChangeType();
        } else {
            return ChangeType.ADD;
        }
    }

    private <F extends FocusType> void reactToChange(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, boolean logDebug,
            OperationResult parentResult)
            throws ConfigurationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException {

        SynchronizationReactionType reaction = syncCtx.getReaction(parentResult);
        if (reaction == null) {
            LOGGER.trace("No reaction is defined for situation {} in {}", syncCtx.getSituation(), syncCtx.getResource());
            return;
        }

        if (isSynchronize(reaction)) {
            Task task = syncCtx.getTask();

            ModelExecuteOptions options = createOptions(syncCtx, change);
            LensContext<F> lensContext = createLensContext(syncCtx, change, options, parentResult);
            lensContext.setDoReconciliationForAllProjections(BooleanUtils.isTrue(reaction.isReconcileAll()));
            LOGGER.trace("---[ SYNCHRONIZATION context before action execution ]-------------------------\n"
                    + "{}\n------------------------------------------", lensContext.debugDumpLazily());

            // there's no point in calling executeAction without context - so
            // the actions are executed only if we are doing the synchronization
            executeActions(syncCtx, lensContext, BeforeAfterType.BEFORE, logDebug, task, parentResult);

            Iterator<LensProjectionContext> iterator = lensContext.getProjectionContextsIterator();
            LensProjectionContext originalProjectionContext = iterator.hasNext() ? iterator.next() : null;

            if (originalProjectionContext != null) {
                originalProjectionContext.setSynchronizationSource(true);
            }

            try {

                clockworkMedic.enterModelMethod(false);
                try {
                    if (change.isSimulate()) {
                        clockwork.previewChanges(lensContext, null, task, parentResult);
                    } else {
                        clockwork.run(lensContext, task, parentResult);
                    }
                } finally {
                    clockworkMedic.exitModelMethod(false);
                }

            } catch (Exception e) {
                LOGGER.error("SYNCHRONIZATION: Error in synchronization on {} for situation {}: {}: {}. Change was {}",
                        syncCtx.getResource(), syncCtx.getSituation(), e.getClass().getSimpleName(), e.getMessage(), change, e);
//                parentResult.recordFatalError("Error during sync", e);
                // what to do here? We cannot throw the error back. All that the notifyChange method
                // could do is to convert it to SystemException. But that indicates an internal error and it will
                // break whatever code called the notifyChange in the first place. We do not want that.
                // If the clockwork could not do anything with the exception then perhaps nothing can be done at all.
                // So just log the error (the error should be remembered in the result and task already)
                // and then just go on.
            }

            // note: actions "AFTER" seem to be useless here (basically they
            // modify lens context - which is relevant only if followed by
            // clockwork run)
            executeActions(syncCtx, lensContext, BeforeAfterType.AFTER, logDebug, task, parentResult);

        } else {
            LOGGER.trace("Skipping clockwork run on {} for situation {}, synchronize is set to false.",
                    syncCtx.getResource(), syncCtx.getSituation());
        }
    }

    @NotNull
    private <F extends FocusType> ModelExecuteOptions createOptions(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change) {

        ModelExecuteOptionsType explicitOptions = syncCtx.getExecuteOptions();
        ModelExecuteOptions options = explicitOptions != null ?
                ModelExecuteOptions.fromModelExecutionOptionsType(explicitOptions) :
                ModelExecuteOptions.create(prismContext);

        if (options.getReconcile() == null) {
            Boolean doReconciliation = syncCtx.isDoReconciliation();
            if (doReconciliation != null) {
                options.reconcile(doReconciliation);
            } else {
                // We have to do reconciliation if we have got a full shadow and no delta.
                // There is no other good way how to reflect the changes from the shadow.
                if (change.getObjectDelta() == null) {
                    options.reconcile();
                }
            }
        }

        if (options.getLimitPropagation() == null) {
            options.limitPropagation(syncCtx.isLimitPropagation());
        }

        return options;
    }

    @NotNull
    private <F extends FocusType> LensContext<F> createLensContext(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, ModelExecuteOptions options,
            OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

        LensContext<F> context = contextFactory.createSyncContext(syncCtx.getFocusClass(), change);
        context.setLazyAuditRequest(true);
        context.setSystemConfiguration(syncCtx.getSystemConfiguration());
        context.setOptions(options);
        context.setItemProcessingIdentifier(syncCtx.getItemProcessingIdentifier());

        ResourceType resource = change.getResource().asObjectable();
        if (ModelExecuteOptions.isLimitPropagation(options)) {
            context.setTriggeredResource(resource);
        }

        context.rememberResource(resource);
        PrismObject<ShadowType> shadow = change.getShadowedResourceObject();
        if (shadow == null) {
            throw new IllegalStateException("No shadow in change: " + change);
        }
        if (InternalsConfig.consistencyChecks) {
            shadow.checkConsistence();
        }

        // Projection context
        ShadowKindType kind = getKind(shadow, syncCtx.getKind());
        String intent = getIntent(shadow, syncCtx.getIntent());
        boolean tombstone = isTombstone(change);
        ResourceShadowDiscriminator discriminator = new ResourceShadowDiscriminator(resource.getOid(), kind, intent, shadow.asObjectable().getTag(), tombstone);
        LensProjectionContext projectionContext = context.createProjectionContext(discriminator);
        projectionContext.setResource(resource);
        projectionContext.setOid(change.getShadowOid());
        projectionContext.setSynchronizationSituationDetected(syncCtx.getSituation());
        projectionContext.setShadowExistsInRepo(syncCtx.isShadowExistsInRepo());

        // insert object delta if available in change
        ObjectDelta<ShadowType> delta = change.getObjectDelta();
        if (delta != null) {
            projectionContext.setSyncDelta(delta);
        } else {
            projectionContext.setSyncAbsoluteTrigger(true);
        }

        // we insert account if available in change
        projectionContext.setLoadedObject(shadow);

        if (!tombstone && !containsIncompleteItems(shadow)) {
            projectionContext.setFullShadow(true);
        }
        projectionContext.setFresh(true);

        projectionContext.setExists(delta == null || !delta.isDelete());

        projectionContext.setDoReconciliation(ModelExecuteOptions.isReconcile(options));

        // Focus context
        if (syncCtx.getCurrentOwner() != null) {
            F focusType = syncCtx.getCurrentOwner();
            LensFocusContext<F> focusContext = context.createFocusContext();
            //noinspection unchecked
            PrismObject<F> focusOld = (PrismObject<F>) focusType.asPrismObject();
            focusContext.setLoadedObject(focusOld);
        }

        // Global stuff
        if (syncCtx.getObjectTemplateRef() != null) {
            ObjectTemplateType objectTemplate = repositoryService
                    .getObject(ObjectTemplateType.class, syncCtx.getObjectTemplateRef().getOid(), null, parentResult)
                    .asObjectable();
            context.setFocusTemplate(objectTemplate);
            context.setFocusTemplateExternallySet(true);        // we do not want to override this template e.g. when subtype changes
        }

        return context;
    }

    private boolean containsIncompleteItems(PrismObject<ShadowType> shadow) {
        ShadowAttributesType attributes = shadow.asObjectable().getAttributes();
        //noinspection SimplifiableIfStatement
        if (attributes == null) {
            return false;   // strictly speaking this is right; but we perhaps should not consider this shadow as fully loaded :)
        } else {
            return ((PrismContainerValue<?>) (attributes.asPrismContainerValue())).getItems().stream()
                    .anyMatch(Item::isIncomplete);
        }
    }

    private ShadowKindType getKind(PrismObject<ShadowType> shadow, ShadowKindType objectSynchronizationKind) {
        ShadowKindType shadowKind = shadow.asObjectable().getKind();
        if (shadowKind != null) {
            return shadowKind;
        }
        return objectSynchronizationKind;
    }

    private String getIntent(PrismObject<ShadowType> shadow,
            String objectSynchronizationIntent) {
        String shadowIntent = shadow.asObjectable().getIntent();
        if (shadowIntent != null) {
            return shadowIntent;
        }
        return objectSynchronizationIntent;
    }

    private boolean isTombstone(ResourceObjectShadowChangeDescription change) {
        PrismObject<? extends ShadowType> shadow = change.getShadowedResourceObject();
        if (shadow.asObjectable().isDead() != null) {
            return shadow.asObjectable().isDead();
        }
        ObjectDelta<? extends ShadowType> objectDelta = change.getObjectDelta();
        return objectDelta != null && objectDelta.isDelete();
    }

    private boolean isSynchronize(SynchronizationReactionType reactionDefinition) {
        if (reactionDefinition.isSynchronize() != null) {
            return reactionDefinition.isSynchronize();
        }
        return !reactionDefinition.getAction().isEmpty();
    }

    /**
     * Saves situation, timestamps, kind and intent (if needed)
     */
    private <F extends FocusType> void saveSyncMetadata(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, boolean full, XMLGregorianCalendar now, OperationResult result) {
        PrismObject<ShadowType> shadow = syncCtx.getApplicableShadow();
        if (shadow == null) {
            return;
        }

        Task task = syncCtx.getTask();

        try {
            ShadowType shadowBean = shadow.asObjectable();
            // new situation description
            List<PropertyDelta<?>> deltas = SynchronizationUtils
                    .createSynchronizationSituationAndDescriptionDelta(shadow, syncCtx.getSituation(),
                            change.getSourceChannel(), full, now, prismContext);

            if (ShadowUtil.isNotKnown(shadowBean.getKind())) {
                PropertyDelta<ShadowKindType> kindDelta = prismContext.deltaFactory().property().createReplaceDelta(shadow.getDefinition(),
                        ShadowType.F_KIND, syncCtx.getKind());
                deltas.add(kindDelta);
            }

            if (shouldSaveIntent(syncCtx)) {
                PropertyDelta<String> intentDelta = prismContext.deltaFactory().property().createReplaceDelta(shadow.getDefinition(),
                        ShadowType.F_INTENT, syncCtx.getIntent());
                deltas.add(intentDelta);
            }

            if (shadowBean.getTag() == null && syncCtx.getTag() != null) {
                PropertyDelta<String> tagDelta = prismContext.deltaFactory().property().createReplaceDelta(shadow.getDefinition(),
                        ShadowType.F_TAG, syncCtx.getTag());
                deltas.add(tagDelta);
            }

            repositoryService.modifyObject(shadowBean.getClass(), shadow.getOid(), deltas, result);
            ItemDeltaCollectionsUtil.applyTo(deltas, shadow);
            task.recordObjectActionExecuted(shadow, ChangeType.MODIFY, null);
        } catch (ObjectNotFoundException ex) {
            task.recordObjectActionExecuted(shadow, ChangeType.MODIFY, ex);
            // This may happen e.g. during some recon-livesync interactions.
            // If the shadow is gone then it is gone. No point in recording the
            // situation any more.
            LOGGER.debug(
                    "Could not update situation in account, because shadow {} does not exist any more (this may be harmless)",
                    shadow.getOid());
            syncCtx.setShadowExistsInRepo(false);
            result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
        } catch (ObjectAlreadyExistsException | SchemaException ex) {
            task.recordObjectActionExecuted(shadow, ChangeType.MODIFY, ex);
            LoggingUtils.logException(LOGGER,
                    "### SYNCHRONIZATION # notifyChange(..): Save of synchronization situation failed: could not modify shadow "
                            + shadow.getOid() + ": " + ex.getMessage(),
                    ex);
            result.recordFatalError("Save of synchronization situation failed: could not modify shadow "
                    + shadow.getOid() + ": " + ex.getMessage(), ex);
            throw new SystemException("Save of synchronization situation failed: could not modify shadow "
                    + shadow.getOid() + ": " + ex.getMessage(), ex);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(shadow, ChangeType.MODIFY, t);
            throw t;
        }
    }

    private <F extends FocusType> boolean shouldSaveIntent(SynchronizationContext<F> syncCtx) throws SchemaException {
        ShadowType shadow = syncCtx.getApplicableShadow().asObjectable();
        if (shadow.getIntent() == null) {
            return true;
        }

        if (SchemaConstants.INTENT_UNKNOWN.equals(shadow.getIntent())) {
            return true;
        }

        if (syncCtx.isForceIntentChange()) {
            String objectSyncIntent = syncCtx.getIntent();
            //noinspection RedundantIfStatement
            if (!MiscSchemaUtil.equalsIntent(shadow.getIntent(), objectSyncIntent)) {
                return true;
            }
        }

        return false;
    }

    private <F extends FocusType> void executeActions(SynchronizationContext<F> syncCtx, LensContext<F> context,
            BeforeAfterType order, boolean logDebug, Task task, OperationResult parentResult)
            throws ConfigurationException, SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {

        SynchronizationReactionType reaction = syncCtx.getReaction(parentResult);
        for (SynchronizationActionType actionDef : reaction.getAction()) {
            if ((actionDef.getOrder() == null && order == BeforeAfterType.BEFORE)
                    || (actionDef.getOrder() != null && actionDef.getOrder() == order)) {

                String handlerUri = actionDef.getHandlerUri();
                if (handlerUri == null) {
                    LOGGER.error("Action definition in resource {} doesn't contain handler URI", syncCtx.getResource());
                    throw new ConfigurationException(
                            "Action definition in resource " + syncCtx.getResource() + " doesn't contain handler URI");
                }

                Action action = actionManager.getActionInstance(handlerUri);
                if (action == null) {
                    LOGGER.warn("Couldn't create action with uri '{}' in resource {}, skipping action.", handlerUri,
                            syncCtx.getResource());
                    continue;
                }

                if (logDebug) {
                    LOGGER.debug("SYNCHRONIZATION: ACTION: Executing: {}.", action.getClass());
                } else {
                    LOGGER.trace("SYNCHRONIZATION: ACTION: Executing: {}.", action.getClass());
                }
                SynchronizationSituation<F> situation = new SynchronizationSituation<>(syncCtx.getCurrentOwner(), syncCtx.getCorrelatedOwner(), syncCtx.getSituation());
                action.handle(context, situation, null, task, parentResult);
            }
        }
    }

    @Override
    public String getName() {
        return "model synchronization service";
    }
}
