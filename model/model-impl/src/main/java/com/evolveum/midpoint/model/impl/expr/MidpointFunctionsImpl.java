/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import static com.evolveum.midpoint.schema.GetOperationOptions.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

import static com.evolveum.midpoint.prism.delta.ObjectDelta.isAdd;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType.RUNNABLE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.common.AvailableLocale;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.query.PreparedQuery;
import com.evolveum.midpoint.schema.query.TypedQuery;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.expr.OptimizingTriggerCreator;
import com.evolveum.midpoint.model.common.ConstantsManager;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.correlation.CorrelationCaseManager;
import com.evolveum.midpoint.model.impl.correlation.CorrelationServiceImpl;
import com.evolveum.midpoint.model.impl.expr.triggerSetter.OptimizingTriggerCreatorImpl;
import com.evolveum.midpoint.model.impl.expr.triggerSetter.TriggerCreatorGlobalState;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.messaging.MessageWrapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 */
@SuppressWarnings("unused")
@Component
public class MidpointFunctionsImpl implements MidpointFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointFunctionsImpl.class);

    public static final String CLASS_DOT = MidpointFunctions.class.getName() + ".";
    private static final String OP_FIND_CANDIDATE_OWNERS = CLASS_DOT + "findCandidateOwners";

    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private ModelService modelService;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private ModelObjectResolver modelObjectResolver;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private Protector protector;
    @Autowired private OrgStructFunctionsImpl orgStructFunctions;
    @Autowired private LinkedObjectsFunctions linkedObjectsFunctions;
    @Autowired private CaseService caseService;
    @Autowired private ConstantsManager constantsManager;
    @Autowired private LocalizationService localizationService;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ModelBeans beans;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private TriggerCreatorGlobalState triggerCreatorGlobalState;
    @Autowired private TaskManager taskManager;
    @Autowired private SchemaService schemaService;
    @Autowired private CorrelationCaseManager correlationCaseManager;
    @Autowired private CorrelationServiceImpl correlationService;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ContextLoader contextLoader;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    public String hello(String name) {
        return "Hello " + name;
    }

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public RelationRegistry getRelationRegistry() {
        return relationRegistry;
    }

    @Override
    public List<String> toList(String... s) {
        return Arrays.asList(s);
    }

    @Override
    public UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(UserType.class, oid, null, new OperationResult("getUserByOid")).asObjectable();
    }

    @Override
    public boolean isMemberOf(UserType user, String orgOid) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (orgOid.equals(objectReferenceType.getOid())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getPlaintextUserPassword(FocusType user) throws EncryptionException {
        if (user == null || user.getCredentials() == null || user.getCredentials().getPassword() == null) {
            return null;        // todo log a warning here?
        }
        ProtectedStringType protectedStringType = user.getCredentials().getPassword().getValue();
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public String getPlaintextAccountPassword(ShadowType account) throws EncryptionException {
        if (account == null || account.getCredentials() == null || account.getCredentials().getPassword() == null) {
            return null;        // todo log a warning here?
        }
        ProtectedStringType protectedStringType = account.getCredentials().getPassword().getValue();
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public String getPlaintextAccountPasswordFromDelta(ObjectDelta<? extends ShadowType> delta) throws EncryptionException {

        if (delta.isAdd()) {
            ShadowType newShadow = delta.getObjectToAdd().asObjectable();
            return getPlaintextAccountPassword(newShadow);
        }
        if (!delta.isModify()) {
            return null;
        }

        List<ProtectedStringType> passwords = new ArrayList<>();
        for (ItemDelta<?, ?> itemDelta : delta.getModifications()) {
            takePasswordsFromItemDelta(passwords, itemDelta);
        }
        LOGGER.trace("Found " + passwords.size() + " password change value(s)");
        if (!passwords.isEmpty()) {
            return protector.decryptString(passwords.get(passwords.size() - 1));
        } else {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private void takePasswordsFromItemDelta(List<ProtectedStringType> passwords, ItemDelta itemDelta) {
        if (itemDelta.isDelete()) {
            return;
        }

        if (itemDelta.getPath().equivalent(PATH_CREDENTIALS_PASSWORD_VALUE)) {
            LOGGER.trace("Found password value add/modify delta");
            //noinspection unchecked
            Collection<PrismPropertyValue<ProtectedStringType>> values = itemDelta.isAdd() ?
                    itemDelta.getValuesToAdd() :
                    itemDelta.getValuesToReplace();
            for (PrismPropertyValue<ProtectedStringType> value : values) {
                passwords.add(value.getValue());
            }
        } else if (itemDelta.getPath().equivalent(PATH_CREDENTIALS_PASSWORD)) {
            LOGGER.trace("Found password add/modify delta");
            //noinspection unchecked
            Collection<PrismContainerValue<PasswordType>> values = itemDelta.isAdd() ?
                    itemDelta.getValuesToAdd() :
                    itemDelta.getValuesToReplace();
            for (PrismContainerValue<PasswordType> value : values) {
                if (value.asContainerable().getValue() != null) {
                    passwords.add(value.asContainerable().getValue());
                }
            }
        } else if (itemDelta.getPath().equivalent(ShadowType.F_CREDENTIALS)) {
            LOGGER.trace("Found credentials add/modify delta");
            //noinspection unchecked
            Collection<PrismContainerValue<CredentialsType>> values = itemDelta.isAdd() ?
                    itemDelta.getValuesToAdd() :
                    itemDelta.getValuesToReplace();
            for (PrismContainerValue<CredentialsType> value : values) {
                if (value.asContainerable().getPassword() != null && value.asContainerable().getPassword().getValue() != null) {
                    passwords.add(value.asContainerable().getPassword().getValue());
                }
            }
        }
    }

    @Override
    public String getPlaintextUserPasswordFromDeltas(List<ObjectDelta<? extends FocusType>> objectDeltas) throws EncryptionException {

        List<ProtectedStringType> passwords = new ArrayList<>();

        for (ObjectDelta<? extends FocusType> delta : objectDeltas) {

            if (delta.isAdd()) {
                FocusType newObject = delta.getObjectToAdd().asObjectable();
                return getPlaintextUserPassword(newObject); // for simplicity we do not look for other values
            }

            if (!delta.isModify()) {
                continue;
            }

            for (ItemDelta<?, ?> itemDelta : delta.getModifications()) {
                takePasswordsFromItemDelta(passwords, itemDelta);
            }
        }
        LOGGER.trace("Found " + passwords.size() + " password change value(s)");
        if (!passwords.isEmpty()) {
            return protector.decryptString(passwords.get(passwords.size() - 1));
        } else {
            return null;
        }
    }

    @Override
    public <F extends ObjectType> boolean hasLinkedAccount(String resourceOid) {
        ModelContext<?> ctx = ModelExpressionThreadLocalHolder.getLensContextRequired();
        ProjectionContextFilter filter =
                new ProjectionContextFilter(resourceOid, ShadowKindType.ACCOUNT, null);
        for (ModelProjectionContext projectionContext : ctx.findProjectionContexts(filter)) {
            LOGGER.trace("hasLinkedAccount: considering a projection context {} for {}", projectionContext, filter);
            if (isProjectionConsideredLinked(projectionContext)) {
                return true;
            }
        }
        return isThereDeletedAccountContextForEvaluateOld(ctx, resourceOid);
    }

    private boolean isThereDeletedAccountContextForEvaluateOld(ModelContext<?> ctx, String resourceOid) {
        ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();
        if (scriptContext == null || scriptContext.isEvaluateNew()) {
            return false; // Deleted context counts only if we are evaluating the old state
        }
        for (ProjectionContextKey deletedOne : ctx.getHistoricResourceObjects()) {
            if (resourceOid.equals(deletedOne.getResourceOid()) && deletedOne.getKind() == ShadowKindType.ACCOUNT) {
                LOGGER.trace("Found deleted one: {}", deletedOne);
                return true;
            }
        }
        return false;
    }

    private boolean isProjectionConsideredLinked(
            ModelProjectionContext projectionContext) {
        if (projectionContext.isGone()) {
            return false;
        }

        ModelElementContext<?> focusContext = projectionContext.getModelContext().getFocusContextRequired();
        ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();

        SynchronizationPolicyDecision synchronizationPolicyDecision = projectionContext.getSynchronizationPolicyDecision();
        SynchronizationIntent synchronizationIntent = projectionContext.getSynchronizationIntent();
        if (scriptContext == null) {
            if (synchronizationPolicyDecision == null) {
                return synchronizationIntent != SynchronizationIntent.DELETE && synchronizationIntent != SynchronizationIntent.UNLINK;
            } else {
                return synchronizationPolicyDecision != SynchronizationPolicyDecision.DELETE && synchronizationPolicyDecision != SynchronizationPolicyDecision.UNLINK;
            }
        } else if (scriptContext.isEvaluateNew()) {
            // Evaluating new state
            if (focusContext.isDelete()) {
                return false;
            }
            if (synchronizationPolicyDecision == null) {
                return synchronizationIntent != SynchronizationIntent.DELETE && synchronizationIntent != SynchronizationIntent.UNLINK;
            } else {
                return synchronizationPolicyDecision != SynchronizationPolicyDecision.DELETE && synchronizationPolicyDecision != SynchronizationPolicyDecision.UNLINK;
            }
        } else {
            // Evaluating old state
            if (focusContext.isAdd()) {
                return false;
            }
            if (synchronizationPolicyDecision == null) {
                return synchronizationIntent != SynchronizationIntent.ADD;
            } else {
                return synchronizationPolicyDecision != SynchronizationPolicyDecision.ADD;
            }
        }
    }

    @Override
    public boolean isCurrentProjectionBeingEnabled() {
        return isProjectionActivationChanged(true);
    }

    @Override
    public boolean isCurrentProjectionBeingDisabled() {
        return isProjectionActivationChanged(false);
    }

    private boolean isProjectionActivationChanged(boolean newState) {
        return isProjectionActivationChanged(ModelExpressionThreadLocalHolder.getProjectionContextRequired(), newState);
    }

    private boolean isProjectionActivationChanged(ModelProjectionContext projectionContext, boolean newState) {
        // Shadows are not re-loaded after delta application, hence "object current" is as good as "object old".
        // We do not use object old, because the newly-loaded full object is not stored there.
        ShadowType objectCurrent = asObjectable(projectionContext.getObjectCurrent());
        if (objectCurrent == null) {
            LOGGER.trace("Considering admin status changed (to '{}') for {}: no current object", newState, projectionContext);
            return false;
        }
        ShadowType objectNew = asObjectable(projectionContext.getObjectNew());
        if (objectNew == null) {
            LOGGER.trace("Considering admin status changed (to '{}') for {}: no new object", newState, projectionContext);
            return false;
        }
        ActivationStatusType statusNew = getAdministrativeStatus(objectNew);
        if (statusNew == null) {
            LOGGER.trace("Considering admin status changed (to '{}') for {}: No new value for admin status, probably we"
                            + "don't have full shadow (and there is no change) or the activation is not supported at all",
                    newState, projectionContext);
            return false;
        }
        boolean enabledNew = statusNew == ActivationStatusType.ENABLED;

        ActivationStatusType statusCurrent = getAdministrativeStatus(objectCurrent);
        if (statusCurrent == null) {
            LOGGER.trace("Considering admin status changed (to '{}') for {}: No current value for admin status, probably we"
                            + "don't have full shadow. But there is new value ({}) so there must be the delta",
                    newState, projectionContext, statusNew);
            return enabledNew == newState;
        }
        boolean enabledCurrent = statusCurrent == ActivationStatusType.ENABLED;
        boolean matches = enabledNew == newState && enabledCurrent != newState;
        LOGGER.trace("Considering admin status changed (to '{}') for {}: Current value: {}, new value: {}, matches: {}",
                newState, projectionContext, statusCurrent, statusNew, matches);
        return matches;
    }

    private static @Nullable ActivationStatusType getAdministrativeStatus(ShadowType shadow) {
        ActivationType activationNew = shadow.getActivation();
        return activationNew != null ? activationNew.getAdministrativeStatus() : null;
    }

    @Override
    public boolean isCurrentProjectionActivated()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        boolean wasActive = wasCurrentProjectionActive();
        boolean willBeActive = willCurrentProjectionBeActive();
        LOGGER.trace("isCurrentProjectionActivated: wasActive = {}, willBeActive = {}", wasActive, willBeActive);
        return !wasActive && willBeActive;
    }

    @Override
    public boolean isCurrentProjectionDeactivated()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        boolean wasActive = wasCurrentProjectionActive();
        boolean willBeActive = willCurrentProjectionBeActive();
        LOGGER.trace("isCurrentProjectionDeactivated: wasActive = {}, willBeActive = {}", wasActive, willBeActive);
        return wasActive && !willBeActive;
    }

    private boolean wasCurrentProjectionActive()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        LensProjectionContext projCtx = getCurrentProjectionContextRequired();
        if (projCtx.isAdministrativeStatusSupported()) {
            ensureActivationInformationAvailable(projCtx);
            ShadowType objectCurrent = asObjectable(projCtx.getObjectCurrent());
            return objectCurrent != null && isShadowEnabled(objectCurrent);
        } else {
            // No need to load the shadow, as the administrative status is not relevant here
            return projCtx.getObjectCurrent() != null;
        }
    }

    private boolean willCurrentProjectionBeActive()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        // "objectNew" is not a good indicator here - we have to look at sync decision instead
        LensProjectionContext projCtx = getCurrentProjectionContextRequired();
        if (projCtx.isAdministrativeStatusSupported()) {
            ensureActivationInformationAvailable(projCtx);
            ShadowType objectNew = asObjectable(projCtx.getObjectNew());
            return objectNew != null && !projCtx.isDelete() && isShadowEnabled(objectNew);
        } else {
            // No need to load the shadow, as the administrative status is not relevant here
            return projCtx.getObjectNew() != null && !projCtx.isDelete();
        }
    }

    private static boolean isShadowEnabled(@NotNull ShadowType shadow) {
        ActivationStatusType status = getAdministrativeStatus(shadow);
        return status == null || status == ActivationStatusType.ENABLED;
    }

    private void ensureActivationInformationAvailable(LensProjectionContext projCtx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (!projCtx.isActivationLoaded()) {
            LOGGER.trace("Will load full shadow in order to determine the activation status: {}", projCtx);
            contextLoader.loadFullShadowNoDiscovery(
                    projCtx, "projection activation determination", getCurrentTaskRequired(), getCurrentResult());
        }
    }

    private @NotNull LensProjectionContext getCurrentProjectionContextRequired() {
        return (LensProjectionContext) ModelExpressionThreadLocalHolder.getProjectionContextRequired();
    }

    @Override
    public boolean isFocusActivated() {
        boolean wasActive = wasFocusActive();
        boolean willBeActive = willFocusBeActive();
        LOGGER.trace("isFocusActivated: wasActive = {}, willBeActive = {}", wasActive, willBeActive);
        return !wasActive && willBeActive;
    }

    @Override
    public boolean isFocusDeactivated() {
        boolean wasActive = wasFocusActive();
        boolean willBeActive = willFocusBeActive();
        LOGGER.trace("isFocusDeactivated: wasActive = {}, willBeActive = {}", wasActive, willBeActive);
        return wasActive && !willBeActive;
    }

    private boolean wasFocusActive() {
        return isEffectivelyEnabled(
                asObjectable(
                        getFocusContextRequired().getObjectOld()));
    }

    private boolean willFocusBeActive() {
        return isEffectivelyEnabled(
                asObjectable(
                        getFocusContextRequired().getObjectNew()));
    }

    @Override
    public boolean isFocusDeleted() {
        var lensContext = ModelExpressionThreadLocalHolder.getLensContext();
        var focusContext = lensContext != null ? lensContext.getFocusContext() : null;
        return focusContext != null && focusContext.isDelete();
    }

    @NotNull
    private static <O extends ObjectType> LensFocusContext<O> getFocusContextRequired() {
        //noinspection unchecked
        return (LensFocusContext<O>) ModelExpressionThreadLocalHolder.getLensContextRequired().getFocusContextRequired();
    }

    @Override
    public <F extends FocusType> boolean isDirectlyAssigned(F focus, String targetOid) {
        for (AssignmentType assignment : focus.getAssignment()) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null && targetRef.getOid().equals(targetOid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <F extends FocusType> boolean isDirectlyAssigned(F focusType, ObjectType target) {
        return isDirectlyAssigned(focusType, target.getOid());
    }

    @Override
    public boolean isDirectlyAssigned(String targetOid) {
        ModelContext<?> ctx = ModelExpressionThreadLocalHolder.getLensContextRequired();
        ModelElementContext<?> focusContext = ctx.getFocusContextRequired();

        PrismObject<?> focus;
        ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();
        if (scriptContext == null) {
            focus = focusContext.getObjectAny();
        } else if (scriptContext.isEvaluateNew()) {
            // Evaluating new state
            if (focusContext.isDelete()) {
                return false;
            }
            focus = focusContext.getObjectNew();
        } else {
            // Evaluating old state
            if (focusContext.isAdd()) {
                return false;
            }
            focus = focusContext.getObjectOld();
        }
        var focusBean = asObjectable(focus);
        return focusBean instanceof FocusType focusTypedBean
                && isDirectlyAssigned(focusTypedBean, targetOid);
    }

    @Override
    public boolean isDirectlyAssigned(ObjectType target) {
        return isDirectlyAssigned(target.getOid());
    }

    // EXPERIMENTAL!!
    @SuppressWarnings("unused")
    @Experimental
    public boolean hasActiveAssignmentTargetSubtype(String roleSubtype) {
        ModelContext<?> lensContext = ModelExpressionThreadLocalHolder.getLensContextRequired();
        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = lensContext.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null) {
            throw new UnsupportedOperationException("hasActiveAssignmentRoleSubtype works only with evaluatedAssignmentTriple");
        }
        Collection<? extends EvaluatedAssignment> nonNegativeEvaluatedAssignments = evaluatedAssignmentTriple.getNonNegativeValues(); // MID-6403
        for (EvaluatedAssignment nonNegativeEvaluatedAssignment : nonNegativeEvaluatedAssignments) {
            PrismObject<?> target = nonNegativeEvaluatedAssignment.getTarget();
            if (target == null) {
                continue;
            }
            //noinspection unchecked,rawtypes
            Collection<String> targetSubtypes = FocusTypeUtil.determineSubTypes((PrismObject) target);
            if (targetSubtypes.contains(roleSubtype)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull List<ShadowType> getLinkedShadows(FocusType focus, String resourceOid, boolean repositoryObjectOnly)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (focus == null) {
            return emptyList();
        }

        List<ShadowType> shadows = new ArrayList<>();

        List<ObjectReferenceType> linkRefs = focus.getLinkRef();
        for (ObjectReferenceType linkRef : linkRefs) {
            ShadowType shadow;
            try {
                shadow = getObject(ShadowType.class, linkRef.getOid(),
                        SelectorOptions.createCollection(GetOperationOptions.createNoFetch()));
            } catch (ObjectNotFoundException e) {
                // Shadow is gone in the meantime. MidPoint will resolve that by itself.
                // It is safe to ignore this error in this method.
                LOGGER.trace("Ignoring shadow {} linked in {} because it no longer exists in repository",
                        linkRef.getOid(), focus);
                continue;
            }
            if (shadow.getResourceRef().getOid().equals(resourceOid)) {
                // We have repo shadow here. Re-read resource shadow if necessary.
                if (!repositoryObjectOnly) {
                    try {
                        shadow = getObject(ShadowType.class, shadow.getOid());
                    } catch (ObjectNotFoundException e) {
                        // Shadow is gone in the meantime. MidPoint will resolve that by itself.
                        // It is safe to ignore this error in this method.
                        LOGGER.trace("Ignoring shadow {} linked in {} because it no longer exists on the resource",
                                linkRef.getOid(), focus);
                        continue;
                    }
                }
                shadows.add(shadow);
            }
        }
        return shadows;
    }

    @Override
    public ShadowType getLinkedShadow(
            FocusType focus, String resourceOid, ShadowKindType kind, String intent, boolean repositoryObjectOnly)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        List<ObjectReferenceType> linkRefs = focus.getLinkRef();
        for (ObjectReferenceType linkRef : linkRefs) {
            ShadowType shadow;
            try {
                shadow = getObject(ShadowType.class, linkRef.getOid(), createNoFetchCollection());
            } catch (ObjectNotFoundException e) {
                // Shadow is gone in the meantime. MidPoint will resolve that by itself.
                // It is safe to ignore this error in this method.
                LOGGER.trace("Ignoring shadow {} linked in {} because it no longer exists in repository",
                        linkRef.getOid(), focus);
                continue;
            }
            if (ShadowUtil.matches(shadow, resourceOid, kind, intent)) {
                // We have repo shadow here. Re-read resource shadow if necessary.
                if (repositoryObjectOnly) {
                    return shadow;
                } else {
                    try {
                        return getObject(ShadowType.class, shadow.getOid());
                    } catch (ObjectNotFoundException e) {
                        // Shadow is gone in the meantime. MidPoint will resolve that by itself.
                        // It is safe to ignore this error in this method.
                        LOGGER.trace("Ignoring shadow {} linked in {} because it no longer exists on resource",
                                linkRef.getOid(), focus);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean isFullShadow() {
        ModelProjectionContext projectionContext = getProjectionContext();
        if (projectionContext == null) {
            LOGGER.debug("Call to isFullShadow while there is no projection context");
            return false;
        }
        return projectionContext.isFullShadow();
    }

    @Override
    public boolean isAttributeLoaded(QName name) throws SchemaException, ConfigurationException {
        ModelProjectionContext projectionContext = getProjectionContext();
        if (projectionContext == null) {
            LOGGER.warn("Call to isAttributeLoaded while there is no projection context");
            return false;
        }
        return ((LensProjectionContext) projectionContext).isAttributeLoaded(name);
    }

    @Override
    public boolean isProjectionExists() {
        ModelProjectionContext projectionContext = getProjectionContext();
        if (projectionContext == null) {
            return false;
        }
        return projectionContext.isExists();
    }

    @Override
    public <T> Integer countAccounts(String resourceOid, QName attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = getCurrentResult(CLASS_DOT + "countAccounts");
        // Intentionally ignoring authorizations, as we are not interested in the resource as such.
        ResourceType resource = modelObjectResolver.getObjectSimple(ResourceType.class, resourceOid, null, null, result);
        return countAccounts(resource, attributeName, attributeValue, getCurrentTask(), result);
    }

    @Override
    public <T> Integer countAccounts(ResourceType resourceType, QName attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "countAccounts");
        return countAccounts(resourceType, attributeName, attributeValue, getCurrentTask(), result);
    }

    @Override
    public <T> Integer countAccounts(ResourceType resourceType, String attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "countAccounts");
        QName attributeQName = getRiAttributeQName(attributeName);
        return countAccounts(resourceType, attributeQName, attributeValue, getCurrentTask(), result);
    }

    @NotNull
    private QName getRiAttributeQName(String attributeName) {
        return new QName(MidPointConstants.NS_RI, attributeName);
    }

    private <T> Integer countAccounts(
            ResourceType resource, QName attributeName, T attributeValue, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        ObjectQuery query = createAttributeQuery(resource, attributeName, attributeValue);
        return modelService.countObjects(ShadowType.class, query, null, task, result);
    }

    @Override
    public <T> boolean isUniquePropertyValue(ObjectType objectType, String propertyPathString, T propertyValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notEmpty(propertyPathString, "Empty property path");
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "isUniquePropertyValue");
        ItemPath propertyPath = prismContext.itemPathParser().asItemPath(propertyPathString);
        return isUniquePropertyValue(objectType, propertyPath, propertyValue, getCurrentTask(), result);
    }

    private <T> boolean isUniquePropertyValue(
            ObjectType object, ItemPath propertyPath, T propertyValue, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        List<? extends ObjectType> conflictingObjects = getObjectsInConflictOnPropertyValue(
                object, propertyPath, propertyValue, null, false, task, result);
        return conflictingObjects.isEmpty();
    }

    @Override
    public <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(
            O object, String propertyPathString, T propertyValue, boolean getAllConflicting)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return getObjectsInConflictOnPropertyValue(object, propertyPathString, propertyValue,
                PrismConstants.DEFAULT_MATCHING_RULE_NAME.getLocalPart(), getAllConflicting);
    }

    private <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(
            O object, String propertyPathString, T propertyValue, String matchingRuleName, boolean getAllConflicting)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notEmpty(propertyPathString, "Empty property path");
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "getObjectsInConflictOnPropertyValue");
        ItemPath propertyPath = prismContext.itemPathParser().asItemPath(propertyPathString);
        QName matchingRuleQName = new QName(matchingRuleName);      // no namespace for now
        return getObjectsInConflictOnPropertyValue(object, propertyPath, propertyValue, matchingRuleQName, getAllConflicting,
                getCurrentTask(), result);
    }

    private <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(
            @NotNull O object,
            @NotNull ItemPath propertyPath,
            @NotNull T propertyValue,
            QName matchingRule,
            boolean getAllConflicting,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notNull(propertyPath, "Null property path");
        Validate.notNull(propertyValue, "Null property value"); // we must check this explicitly
        PrismPropertyDefinition<T> propertyDefinition =
                object.asPrismObject().getDefinition().findPropertyDefinition(propertyPath);
        if (matchingRule == null) {
            if (propertyDefinition != null && PolyStringType.COMPLEX_TYPE.equals(propertyDefinition.getTypeName())) {
                matchingRule = PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME;
            } else {
                matchingRule = PrismConstants.DEFAULT_MATCHING_RULE_NAME;
            }
        }
        ObjectQuery query = prismContext.queryFor(object.getClass())
                .item(propertyPath, propertyDefinition).eq(propertyValue).matching(matchingRule)
                .build();
        LOGGER.trace("Determining uniqueness of property {} using query:\n{}", propertyPath, query.debugDumpLazily());

        final List<O> conflictingObjects = new ArrayList<>();
        ResultHandler<O> handler = (foundObject, lResult) -> {
            if (object.getOid() != null && object.getOid().equals(foundObject.getOid())) {
                // We have found ourselves. No conflict (yet). Just go on.
                return true;
            } else {
                // We have found someone else. Conflict.
                conflictingObjects.add(foundObject.asObjectable());
                return getAllConflicting;
            }
        };

        //noinspection unchecked
        modelService.searchObjectsIterative((Class<O>) object.getClass(), query, handler, null, task, result);

        LOGGER.trace("Found {} conflicting objects", conflictingObjects.size());
        return conflictingObjects;
    }

    @Override
    public <T> boolean isUniqueAccountValue(
            ResourceType resource, ShadowType shadow, String attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notEmpty(attributeName, "Empty attribute name");
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "isUniqueAccountValue");
        QName attributeQName = getRiAttributeQName(attributeName);
        return isUniqueAccountValue(resource, shadow, attributeQName, attributeValue, getCurrentTask(), result);
    }

    private <T> boolean isUniqueAccountValue(
            ResourceType resource, ShadowType shadow, QName attributeName, T attributeValue,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notNull(resource, "Null resource");
        Validate.notNull(shadow, "Null shadow");
        Validate.notNull(attributeName, "Null attribute name");
        Validate.notNull(attributeValue, "Null attribute value");

        ObjectQuery query = createAttributeQuery(resource, attributeName, attributeValue);
        LOGGER.trace("Determining uniqueness of attribute {} using query:\n{}", attributeName, query.debugDumpLazily());

        final Holder<Boolean> isUniqueHolder = new Holder<>(true);
        ResultHandler<ShadowType> handler = (object, parentResult) -> {
            if (shadow.getOid() != null && shadow.getOid().equals(object.getOid())) {
                // We have found ourselves. No conflict (yet). Just go on.
                return true;
            } else {
                // We have found someone else. Conflict.
                isUniqueHolder.setValue(false);
                return false;
            }
        };

        modelService.searchObjectsIterative(ShadowType.class, query, handler, readOnly(), task, result);

        return isUniqueHolder.getValue();
    }

    private <T> ObjectQuery createAttributeQuery(ResourceType resourceType, QName attributeName, T attributeValue)
            throws SchemaException, ConfigurationException {
        ResourceSchema rSchema = ResourceSchemaFactory.getCompleteSchema(resourceType);
        ResourceObjectDefinition rAccountDef = rSchema.findDefaultDefinitionForKindRequired(ShadowKindType.ACCOUNT);
        ShadowSimpleAttributeDefinition<?> attrDef = rAccountDef.findSimpleAttributeDefinition(attributeName);
        if (attrDef == null) {
            throw new SchemaException("No attribute '" + attributeName + "' in " + rAccountDef);
        }
        return prismContext.queryFor(ShadowType.class)
                .itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getItemName()).eq(attributeValue)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(rAccountDef.getObjectClassName())
                .and().item(ShadowType.F_RESOURCE_REF).ref(resourceType.getOid())
                .build();
    }

    @Override
    public <F extends ObjectType> ModelContext<F> getModelContext() {
        //noinspection unchecked
        return (ModelContext<F>) ModelExpressionThreadLocalHolder.getLensContext();
    }

    @Override
    public <F extends ObjectType> ModelElementContext<F> getFocusContext() {
        var lensContext = ModelExpressionThreadLocalHolder.getLensContext();
        if (lensContext == null) {
            return null;
        }
        //noinspection unchecked
        return (ModelElementContext<F>) lensContext.getFocusContext();
    }

    @Override
    public ModelProjectionContext getProjectionContext() {
        return ModelExpressionThreadLocalHolder.getProjectionContext();
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>> Mapping<V, D> getMapping() {
        return ModelExpressionThreadLocalHolder.getMapping();
    }

    @Override
    public Task getCurrentTask() {
        Task fromModelHolder = ExpressionEnvironmentThreadLocalHolder.getCurrentTask();
        if (fromModelHolder != null) {
            return fromModelHolder;
        }

        // fallback (MID-4130): but maybe we should instead make sure ModelExpressionThreadLocalHolder is set up correctly
        ScriptExpressionEvaluationContext ctx = ScriptExpressionEvaluationContext.getThreadLocal();
        if (ctx != null) {
            return ctx.getTask();
        }

        return null;
    }

    public @NotNull Task getCurrentTaskRequired() {
        return MiscUtil.requireNonNull(getCurrentTask(), () -> new IllegalStateException("no current task"));
    }

    @Override
    public OperationResult getCurrentResult() {
        // This is the most current operation result, reflecting e.g. the fact that mapping evaluation was started.
        ScriptExpressionEvaluationContext ctx = ScriptExpressionEvaluationContext.getThreadLocal();
        if (ctx != null) {
            return ctx.getResult();
        } else {
            // This is a bit older. But better than nothing.
            return ExpressionEnvironmentThreadLocalHolder.getCurrentResult();
        }
    }

    @Override
    public OperationResult getCurrentResult(String operationName) {
        OperationResult currentResult = getCurrentResult();
        if (currentResult != null) {
            return currentResult;
        } else {
            LOGGER.warn("No operation result for {}, creating a new one", operationName);
            return new OperationResult(operationName);
        }
    }

    // functions working with ModelContext

    @Override
    public ModelContext<?> unwrapModelContext(LensContextType lensContextType)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return LensContext.fromLensContextBean(lensContextType, getCurrentTask(),
                getCurrentResult(MidpointFunctions.class.getName() + "getObject"));
    }

    @Override
    public LensContextType wrapModelContext(ModelContext<?> lensContext) throws SchemaException {
        return ((LensContext<?>) lensContext).toBean();
    }

    // Convenience functions

    @Override
    public <T extends ObjectType> T createEmptyObject(Class<T> type) throws SchemaException {
        PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        PrismObject<T> object = objectDefinition.instantiate();
        return object.asObjectable();
    }

    @Override
    public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, String name) throws SchemaException {
        T objectType = createEmptyObject(type);
        objectType.setName(new PolyStringType(name));
        return objectType;
    }

    @Override
    public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyString name) throws SchemaException {
        T objectType = createEmptyObject(type);
        objectType.setName(new PolyStringType(name));
        return objectType;
    }

    @Override
    public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyStringType name) throws SchemaException {
        T objectType = createEmptyObject(type);
        objectType.setName(name);
        return objectType;
    }

    // Functions accessing modelService

    @Override
    public <T extends ObjectType> T resolveReference(ObjectReferenceType reference)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return resolveReferenceInternal(reference, false);
    }

    <T extends ObjectType> T resolveReferenceInternal(ObjectReferenceType reference, boolean allowNotFound)
            throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (reference == null) {
            return null;
        }
        QName type = reference.getType(); // TODO what about implicitly specified types, like in resourceRef?
        PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry()
                .findObjectDefinitionByType(reference.getType());
        if (objectDefinition == null) {
            throw new SchemaException("No definition for type " + type);
        }
        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .executionPhase()
                .allowNotFound(allowNotFound)
                .build();
        return modelService.getObject(objectDefinition.getCompileTimeClass(), reference.getOid(), options,
                        getCurrentTask(), getCurrentResult())
                .asObjectable();
    }

    @Override
    public <T extends ObjectType> T resolveReferenceIfExists(ObjectReferenceType reference)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        try {
            return resolveReferenceInternal(reference, true);
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    @Override
    public <T extends ObjectType> T getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return modelService.getObject(type, oid, options, getCurrentTask(), getCurrentResult()).asObjectable();
    }

    @Override
    public <T extends ObjectType> T getObject(Class<T> type, String oid) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        PrismObject<T> prismObject = modelService.getObject(type, oid,
                getDefaultGetOptionCollection(),
                getCurrentTask(), getCurrentResult());
        return prismObject.asObjectable();
    }

    @Override
    public void executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        modelService.executeChanges(deltas, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public void executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        modelService.executeChanges(deltas, null, getCurrentTask(), getCurrentResult());
    }

    @SafeVarargs
    @Override
    public final void executeChanges(ObjectDelta<? extends ObjectType>... deltas)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(deltas);
        modelService.executeChanges(deltaCollection, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> newObject,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        ObjectDelta<T> delta = DeltaFactory.Object.createAddDelta(newObject);
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(delta);
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges = modelService.executeChanges(deltaCollection, options, getCurrentTask(), getCurrentResult());
        String oid = ObjectDeltaOperation.findAddDeltaOid(executedChanges, newObject);
        newObject.setOid(oid);
        return oid;
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> newObject)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        return addObject(newObject, null);
    }

    @Override
    public <T extends ObjectType> String addObject(T newObject,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        return addObject(newObject.asPrismObject(), options);
    }

    @Override
    public <T extends ObjectType> String addObject(T newObject)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        return addObject(newObject.asPrismObject(), null);
    }

    @Override
    public <T extends ObjectType> void modifyObject(ObjectDelta<T> modifyDelta,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(modifyDelta);
        modelService.executeChanges(deltaCollection, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> void modifyObject(ObjectDelta<T> modifyDelta)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(modifyDelta);
        modelService.executeChanges(deltaCollection, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        ObjectDelta<T> deleteDelta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(deleteDelta);
        modelService.executeChanges(deltaCollection, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        ObjectDelta<T> deleteDelta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(deleteDelta);
        modelService.executeChanges(deltaCollection, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <F extends FocusType> void recompute(Class<F> type, String oid) throws SchemaException, PolicyViolationException,
            ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        modelService.recompute(type, oid, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <F extends FocusType> PrismObject<F> searchShadowOwner(String accountOid)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException, CommunicationException {
        //noinspection unchecked
        return (PrismObject<F>) modelService.searchShadowOwner(accountOid, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> List<T> searchObjects(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        return MiscSchemaUtil.toObjectableList(
                modelService.searchObjects(type, query, options, getCurrentTask(), getCurrentResult()));
    }

    @Override
    public <T extends ObjectType> List<T> searchObjects(
            Class<T> type, ObjectQuery query) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return MiscSchemaUtil.toObjectableList(
                modelService.searchObjects(type, query,
                        getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult()));
    }

    @Override
    public <T extends ObjectType> void searchObjectsIterative(Class<T> type,
            ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        modelService.searchObjectsIterative(type, query, handler, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> void searchObjectsIterative(Class<T> type,
            ObjectQuery query, ResultHandler<T> handler)
            throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        modelService.searchObjectsIterative(type, query, handler,
                getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> T searchObjectByName(Class<T> type, String name)
            throws SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SchemaException, ExpressionEvaluationException {
        ObjectQuery nameQuery = ObjectQueryUtil.createNameQuery(name);
        List<PrismObject<T>> foundObjects = modelService
                .searchObjects(type, nameQuery,
                        getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
        if (foundObjects.isEmpty()) {
            return null;
        }
        if (foundObjects.size() > 1) {
            throw new IllegalStateException("More than one object found for type " + type + " and name '" + name + "'");
        }
        return foundObjects.iterator().next().asObjectable();
    }

    @Override
    public <T extends ObjectType> T searchObjectByName(Class<T> type, PolyString name)
            throws SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SchemaException, ExpressionEvaluationException {
        ObjectQuery nameQuery = ObjectQueryUtil.createNameQuery(name);
        List<PrismObject<T>> foundObjects = modelService
                .searchObjects(type, nameQuery,
                        getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
        if (foundObjects.isEmpty()) {
            return null;
        }
        if (foundObjects.size() > 1) {
            throw new IllegalStateException("More than one object found for type " + type + " and name '" + name + "'");
        }
        return foundObjects.iterator().next().asObjectable();
    }

    @Override
    public <T extends ObjectType> T searchObjectByName(Class<T> type, PolyStringType name)
            throws SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SchemaException, ExpressionEvaluationException {
        ObjectQuery nameQuery = ObjectQueryUtil.createNameQuery(name);
        List<PrismObject<T>> foundObjects = modelService
                .searchObjects(type, nameQuery,
                        getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
        if (foundObjects.isEmpty()) {
            return null;
        }
        if (foundObjects.size() > 1) {
            throw new IllegalStateException("More than one object found for type " + type + " and name '" + name + "'");
        }
        return foundObjects.iterator().next().asObjectable();
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, ConfigurationException,
            CommunicationException, ExpressionEvaluationException {
        return modelService.countObjects(type, query, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type,
            ObjectQuery query) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, ConfigurationException,
            CommunicationException, ExpressionEvaluationException {
        return modelService.countObjects(type, query,
                getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
    }

    @Override
    public OperationResult testResource(String resourceOid)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException {
        return modelService.testResource(resourceOid, getCurrentTask(), getCurrentResult("testResource"));
    }

    @Override
    public ObjectDeltaType getResourceDelta(ModelContext<?> context, String resourceOid) throws SchemaException {
        List<ObjectDelta<ShadowType>> deltas = new ArrayList<>();
        for (Object modelProjectionContextObject : context.getProjectionContexts()) {
            LensProjectionContext lensProjectionContext = (LensProjectionContext) modelProjectionContextObject;
            if (resourceOid.equals(lensProjectionContext.getKey().getResourceOid())) {
                deltas.add(lensProjectionContext.getSummaryDelta());   // union of primary and secondary deltas
            }
        }
        ObjectDelta<ShadowType> sum = ObjectDeltaCollectionsUtil.summarize(deltas);
        return DeltaConvertor.toObjectDeltaType(sum);
    }

    @Override
    public long getSequenceCounter(String sequenceOid) throws ObjectNotFoundException, SchemaException {
        return SequentialValueExpressionEvaluator.getSequenceCounterValue(sequenceOid, repositoryService, getCurrentResult());
    }

    // orgstruct related methods

    @Override
    public Collection<String> getManagersOids(UserType user)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagersOids(user, false);
    }

    @Override
    public Collection<String> getOrgUnits(UserType user, QName relation) {
        return orgStructFunctions.getOrgUnits(user, relation, false);
    }

    @Override
    public OrgType getParentOrgByOrgType(ObjectType object, String orgType) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgByOrgType(object, orgType, false);
    }

    @Override
    public OrgType getParentOrgByArchetype(ObjectType object, String archetypeOid) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgByArchetype(object, archetypeOid, false);
    }

    @Override
    public OrgType getOrgByOid(String oid) throws SchemaException {
        return orgStructFunctions.getOrgByOid(oid, false);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgs(object, false);
    }

    @Override
    public Collection<String> getOrgUnits(UserType user) {
        return orgStructFunctions.getOrgUnits(user, false);
    }

    @Override
    public Collection<UserType> getManagersOfOrg(String orgOid) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagersOfOrg(orgOid, false);
    }

    @Override
    public boolean isManagerOfOrgType(UserType user, String orgType) throws SchemaException {
        return orgStructFunctions.isManagerOfOrgType(user, orgType, false);
    }

    @Override
    public Collection<UserType> getManagers(UserType user) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagers(user, false);
    }

    @Override
    public Collection<UserType> getManagersByOrgType(UserType user, String orgType)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagersByOrgType(user, orgType, false);
    }

    @Override
    public boolean isManagerOf(UserType user, String orgOid) {
        return orgStructFunctions.isManagerOf(user, orgOid, false);
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, String relation)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgsByRelation(object, relation, false);
    }

    @Override
    public Collection<UserType> getManagers(UserType user, String orgType, boolean allowSelf)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagers(user, orgType, allowSelf, false);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, String relation, String orgType)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgs(object, relation, orgType, false);
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(UserType user)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagersOidsExceptUser(user, false);
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(@NotNull Collection<ObjectReferenceType> userRefList)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        return orgStructFunctions.getManagersOidsExceptUser(userRefList, false);
    }

    @Override
    public OrgType getOrgByName(String name) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getOrgByName(name, false);
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, QName relation)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgsByRelation(object, relation, false);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, QName relation, String orgType)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgs(object, relation, orgType, false);
    }

    @Override
    public boolean isManager(UserType user) {
        return orgStructFunctions.isManager(user);
    }

    @Override
    public Protector getProtector() {
        return protector;
    }

    @Override
    public String getPlaintext(ProtectedStringType protectedStringType) throws EncryptionException {
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String> parseXmlToMap(String xml) {
        Map<String, String> resultingMap = new HashMap<>();
        if (xml != null && !xml.isEmpty()) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            String value = "";
            String startName = "";
            InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            boolean isRootElement = true;
            try {
                XMLEventReader eventReader = factory.createXMLEventReader(stream);
                while (eventReader.hasNext()) {

                    XMLEvent event = eventReader.nextEvent();
                    int code = event.getEventType();
                    if (code == XMLStreamConstants.START_ELEMENT) {

                        StartElement startElement = event.asStartElement();
                        startName = startElement.getName().getLocalPart();
                        if (!isRootElement) {
                            resultingMap.put(startName, null);
                        } else {
                            isRootElement = false;
                        }
                    } else if (code == XMLStreamConstants.CHARACTERS) {
                        Characters characters = event.asCharacters();
                        if (!characters.isWhiteSpace()) {

                            StringBuilder valueBuilder;
                            if (value != null) {
                                valueBuilder = new StringBuilder(value).append(" ").append(characters.getData());
                            } else {
                                valueBuilder = new StringBuilder(characters.getData());
                            }
                            value = valueBuilder.toString();
                        }
                    } else if (code == XMLStreamConstants.END_ELEMENT) {

                        EndElement endElement = event.asEndElement();
                        String endName = endElement.getName().getLocalPart();

                        if (endName.equals(startName)) {
                            if (value != null) {
                                resultingMap.put(endName, value);
                                value = null;
                            }
                        } else {
                            LOGGER.trace("No value between xml tags, tag name : {}", endName);
                        }

                    } else if (code == XMLStreamConstants.END_DOCUMENT) {
                        isRootElement = true;
                    }
                }
            } catch (XMLStreamException e) {
                throw new SystemException(
                        "Xml stream exception wile parsing xml string" + e.getLocalizedMessage());
            }
        } else {
            LOGGER.trace("Input xml string null or empty.");
        }
        return resultingMap;
    }

    @Override
    public List<ObjectReferenceType> getMembersAsReferences(String orgOid) throws SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {
        return getMembers(orgOid).stream()
                .map(obj -> createObjectRef(obj))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserType> getMembers(String orgOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .isDirectChildOf(orgOid)
                .build();
        return searchObjects(UserType.class, query, null);
    }

    @Override
    public <F extends FocusType> ShadowPurposeType computeDefaultProjectionPurpose(F focus, ShadowType shadow, ResourceType resource) {
        if (focus == null || shadow == null) {
            return null;
        }
        if (!(focus instanceof UserType)) {
            return null;
        }
        if (shadow.getKind() != null && shadow.getKind() != ShadowKindType.ACCOUNT) {
            return null;
        }
        ProtectedStringType focusPasswordPs = FocusTypeUtil.getPasswordValue(focus);
        if (focusPasswordPs != null && focusPasswordPs.canGetCleartext()) {
            return null;
        }
        CredentialsCapabilityType credentialsCapabilityType =
                ResourceTypeUtil.getEnabledCapability(resource, CredentialsCapabilityType.class);
        if (credentialsCapabilityType == null) {
            return null;
        }
        PasswordCapabilityType passwordCapabilityType = credentialsCapabilityType.getPassword();
        if (passwordCapabilityType == null) {
            return null;
        }
        if (passwordCapabilityType.isEnabled() == Boolean.FALSE) {
            return null;
        }
        return ShadowPurposeType.INCOMPLETE;
    }

    public MidPointPrincipal getPrincipal() throws SecurityViolationException {
        return securityContextManager.getPrincipal();
    }

    @Override
    public String getPrincipalOid() {
        return securityContextManager.getPrincipalOid();
    }

    @Override
    public String getChannel() {
        Task task = getCurrentTask();
        return task != null ? task.getChannel() : null;
    }

    @Override
    public CaseService getWorkflowService() {
        return caseService;
    }

    @Override
    public List<ShadowType> getShadowsToActivate(Collection<? extends ModelProjectionContext> projectionContexts) {
        List<ShadowType> shadows = new ArrayList<>();
        for (ModelProjectionContext projectionCtx : projectionContexts) {
            for (ObjectDeltaOperation<ShadowType> executedOperation : projectionCtx.getExecutedDeltas()) {
                ObjectDelta<ShadowType> shadowDelta = executedOperation.getObjectDelta();
                if (isAdd(shadowDelta) && executedOperation.getStatus() == OperationResultStatus.SUCCESS) {
                    ShadowType shadow = shadowDelta.getObjectToAdd().asObjectable();
                    if (shadow.getPurpose() == ShadowPurposeType.INCOMPLETE) {
                        shadows.add(shadow);
                    }
                }
            }
        }
        return shadows;
    }

    @Override
    public String createRegistrationConfirmationLink(UserType userType) {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(userType.asPrismObject());
        if (securityPolicy != null
                && securityPolicy.getAuthentication() != null
                && securityPolicy.getAuthentication().getSequence() != null
                && !securityPolicy.getAuthentication().getSequence().isEmpty()) {
            SelfRegistrationPolicyType selfRegistrationPolicy = SecurityPolicyUtil.getSelfRegistrationPolicy(securityPolicy);
            if (selfRegistrationPolicy != null) {
                String selfReqistrationSequenceName = selfRegistrationPolicy.getAdditionalAuthenticationSequence();
                if (selfReqistrationSequenceName != null) {
                    String prefix = createPrefixLinkByAuthSequence(
                            SchemaConstants.CHANNEL_SELF_REGISTRATION_URI,
                            selfReqistrationSequenceName,
                            securityPolicy.getAuthentication().getSequence());
                    if (prefix != null) {
                        return createTokenConfirmationLink(prefix, userType);
                    }
                }
            }
        }
        return createTokenConfirmationLink(
                SchemaConstants.AUTH_MODULE_PREFIX + SchemaConstants.REGISTRATION_PREFIX, userType);
    }

    @Override
    public String createInvitationLink(UserType user) {
        try {
            SecurityPolicyType securityPolicy = resolveSecurityPolicy(user.asPrismObject());
            String invitationSequenceId = SecurityUtil.getInvitationSequenceIdentifier(securityPolicy);
            if (StringUtils.isNotEmpty(invitationSequenceId)) {
                Task task = taskManager.createTaskInstance("save nonce to user");
                OperationResult result = new OperationResult("save nonce to user");
                NonceType nonce = generateNonce(user, task, result);
                saveNonceToUser(user, nonce, task, result);

                String prefix = createPrefixLinkByAuthSequence(SchemaConstants.CHANNEL_INVITATION_URI, invitationSequenceId, securityPolicy.getAuthentication().getSequence(), false);
                return createBaseConfirmationLink(prefix, user.getName().getOrig()) + "&" + SchemaConstants.TOKEN + "=" + getPlaintext(nonce.getValue());
            }
        } catch (Exception e) {
            LOGGER.error("Could not create invitation link for the user: {}", e.getMessage(), e);
        }
        return null;
    }

    private void saveNonceToUser(UserType user, NonceType nonce, Task task, OperationResult result) {
        try {
            List<ItemDelta<?, ?>> modifications = new ArrayList<>();
            modifications.add(PrismContext.get().deltaFor(UserType.class)
                    .item(ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_NONCE))
                    .replace(nonce)
                    .asItemDelta());
            repositoryService.modifyObject(UserType.class, user.getOid(), modifications, result);
        } catch (Exception e) {
            LOGGER.error("Could not save nonce to the user: {}", e.getMessage(), e);
        }
    }

    private NonceType generateNonce(UserType user, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, EncryptionException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ProtectedStringType nonceCredentials = new ProtectedStringType();
        String nonceValue = modelInteractionService.generateNonce(getNonceCredentialsPolicy(user, task, result), task, result);
        nonceCredentials.setClearValue(nonceValue);
        protector.encrypt(nonceCredentials);

        NonceType nonceType = new NonceType();
        nonceType.setValue(nonceCredentials);

        return nonceType;
    }

    private NonceCredentialsPolicyType getNonceCredentialsPolicy(UserType user, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        SecurityPolicyType securityPolicy = modelInteractionService.getSecurityPolicy(user.asPrismObject(), task, result);
        if (securityPolicy == null) {
            return null;
        }
        CredentialsPolicyType credentialPolicy = securityPolicy.getCredentials();
        if (credentialPolicy == null) {
            return null;
        }
        List<NonceCredentialsPolicyType> nonceCredentialsPolicies = credentialPolicy.getNonce();
        String invitationCredentialName = getNonceCredentialsPolicyName(user, task, result);
        if (invitationCredentialName == null) {
            return null;
        }
        return nonceCredentialsPolicies
                .stream()
                .filter(n -> invitationCredentialName.equals(n.getName()))
                .findFirst()
                .orElse(null);
    }

    private String getNonceCredentialsPolicyName(UserType user, Task task, OperationResult result) throws
            SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        SecurityPolicyType securityPolicy = modelInteractionService.getSecurityPolicy(user.asPrismObject(), task, result);
        if (securityPolicy.getFlow() == null) {
            return null;
        }
        String sequenceIdentifier = SecurityUtil.getInvitationSequenceIdentifier(securityPolicy);
        AuthenticationSequenceType invitationAuthSequence = securityPolicy.getAuthentication()
                .getSequence()
                .stream()
                .filter(seq -> sequenceIdentifierMatch(seq, sequenceIdentifier))
                .findFirst()
                .orElse(null);
        if (invitationAuthSequence == null || invitationAuthSequence.getModule().isEmpty()) {
            return null;
        }
        AuthenticationSequenceModuleType module = invitationAuthSequence.getModule().get(0);
        String moduleIdentifier = module.getIdentifier() != null ? module.getIdentifier() : module.getName();
        if (StringUtils.isEmpty(moduleIdentifier)) {
            return null;
        }

        MailNonceAuthenticationModuleType invitationAuthModule = securityPolicy.getAuthentication()
                .getModules()
                .getMailNonce()
                .stream()
                .filter(m -> moduleIdentifierMatch(m, moduleIdentifier))
                .findFirst()
                .orElse(null);

        return invitationAuthModule != null ? invitationAuthModule.getCredentialName() : null;
    }

    private boolean sequenceIdentifierMatch(AuthenticationSequenceType seq, String sequenceIdentifier) {
        return sequenceIdentifier.equals(seq.getName()) || sequenceIdentifier.equals(seq.getIdentifier());
    }

    private boolean moduleIdentifierMatch(MailNonceAuthenticationModuleType module, String moduleIdentifier) {
        String mailNonceModuleId = module.getIdentifier() != null ? module.getIdentifier() : module.getName();
        return moduleIdentifier.equals(mailNonceModuleId);
    }

    @Override
    public String createPasswordResetLink(UserType userType) {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(userType.asPrismObject());
        if (securityPolicy != null
                && securityPolicy.getAuthentication() != null
                && securityPolicy.getAuthentication().getSequence() != null
                && !securityPolicy.getAuthentication().getSequence().isEmpty()) {
            if (securityPolicy.getCredentialsReset() != null
                    && securityPolicy.getCredentialsReset().getAuthenticationSequenceName() != null) {
                String resetPasswordSequenceName = securityPolicy.getCredentialsReset().getAuthenticationSequenceName();
                String prefix = createPrefixLinkByAuthSequence(
                        SchemaConstants.CHANNEL_RESET_PASSWORD_URI,
                        resetPasswordSequenceName,
                        securityPolicy.getAuthentication().getSequence());
                if (prefix != null) {
                    return createTokenConfirmationLink(prefix, userType);
                }
            }
        }
        return createTokenConfirmationLink(SchemaConstants.PASSWORD_RESET_CONFIRMATION_PREFIX, userType);
    }

    @Override
    public @Nullable String createWorkItemCompletionLink(@NotNull WorkItemId workItemId) {
        String publicHttpUrlPattern = getPublicHttpUrlPattern();
        if (publicHttpUrlPattern == null || publicHttpUrlPattern.isBlank()) {
            return null;
        } else {
            return publicHttpUrlPattern + SchemaConstants.WORK_ITEM_URL_PREFIX + workItemId.caseOid + ":" + workItemId.id;
        }
    }

    @Override
    public String createAccountActivationLink(UserType userType) {
        return createBaseConfirmationLink(SchemaConstants.ACCOUNT_ACTIVATION_PREFIX, userType.getOid());
    }

    private String createBaseConfirmationLink(String prefix, UserType userType) {
        return getPublicHttpUrlPattern() + prefix + "?" + SchemaConstants.USER_ID + "=" + userType.getName().getOrig();
    }

    @SuppressWarnings("SameParameterValue")
    private String createBaseConfirmationLink(String prefix, String oid) {
        return getPublicHttpUrlPattern() + prefix + "?" + SchemaConstants.USER_ID + "=" + oid;
    }

    private String createTokenConfirmationLink(String prefix, UserType userType) {
        try {
            var urlEncodedNonce = URLEncoder.encode(getNonce(userType), StandardCharsets.UTF_8);
            return createBaseConfirmationLink(prefix, userType) + "&" + SchemaConstants.TOKEN + "=" + urlEncodedNonce;
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    private String createPrefixLinkByAuthSequence(String channel, String nameOfSequence,
            Collection<AuthenticationSequenceType> sequences) {
        return createPrefixLinkByAuthSequence(channel, nameOfSequence, sequences, true);
    }

    private String createPrefixLinkByAuthSequence(String channel, String nameOfSequence,
            Collection<AuthenticationSequenceType> sequences, boolean useAuthPrefix) {
        AuthenticationSequenceType sequenceByName = null;
        AuthenticationSequenceType defaultSequence = null;
        for (AuthenticationSequenceType sequenceType : sequences) {
            String sequenceIdentifier = StringUtils.isNotEmpty(sequenceType.getIdentifier()) ? sequenceType.getIdentifier() : sequenceType.getName();
            if (StringUtils.equals(sequenceIdentifier, nameOfSequence)) {
                sequenceByName = sequenceType;
                break;
            } else if (sequenceType.getChannel().getChannelId().equals(channel)
                    && Boolean.TRUE.equals(sequenceType.getChannel().isDefault())) {
                defaultSequence = sequenceType;
            }
        }
        AuthenticationSequenceType usedSequence = sequenceByName != null ? sequenceByName : defaultSequence;
        if (usedSequence != null) {
            String sequenceSuffix = usedSequence.getChannel().getUrlSuffix();
            String prefix = (sequenceSuffix.startsWith("/")) ? sequenceSuffix : ("/" + sequenceSuffix);
            return useAuthPrefix ? SchemaConstants.AUTH_MODULE_PREFIX + prefix : prefix;
        }
        return null;
    }

    private SecurityPolicyType resolveSecurityPolicy(PrismObject<UserType> user) {
        return securityContextManager.runPrivileged(new Producer<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public SecurityPolicyType run() {

                Task task = taskManager.createTaskInstance("load security policy");

                Task currentTask = getCurrentTask();
                task.setChannel(currentTask != null ? currentTask.getChannel() : null);

                OperationResult result = new OperationResult("load security policy");

                try {
                    return modelInteractionService.getSecurityPolicy(user, task, result);
                } catch (CommonException e) {
                    LOGGER.error("Could not retrieve security policy: {}", e.getMessage(), e);
                    return null;
                }

            }

        });
    }

    private String getPublicHttpUrlPattern() {
        SystemConfigurationType systemConfiguration;
        try {
            systemConfiguration = modelInteractionService.getSystemConfiguration(getCurrentResult());
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.error("Error while getting system configuration. ", e);
            return null;
        }
        if (systemConfiguration == null) {
            LOGGER.trace("No system configuration defined. Skipping link generation.");
            return null;
        }
        String host = null;
        HttpConnectionInformation connectionInf = SecurityUtil.getCurrentConnectionInformation();
        if (connectionInf != null) {
            host = connectionInf.getServerName();
        }
        String publicHttpUrlPattern = SystemConfigurationTypeUtil.getPublicHttpUrlPattern(systemConfiguration, host);
        if (StringUtils.isBlank(publicHttpUrlPattern)) {
            LOGGER.error("No public HTTP URL pattern defined. It can break link generation.");
        }

        return publicHttpUrlPattern;
    }

    private String getNonce(UserType user) {
        if (user.getCredentials() == null) {
            return null;
        }

        if (user.getCredentials().getNonce() == null) {
            return null;
        }

        if (user.getCredentials().getNonce().getValue() == null) {
            return null;
        }

        try {
            return getPlaintext(user.getCredentials().getNonce().getValue());
        } catch (EncryptionException e) {
            return null;
        }
    }

    @Override
    public String getConst(String name) {
        return constantsManager.getConstantValue(name);
    }

    @Override
    public ShadowType resolveEntitlement(ShadowAssociationValueType associationValue) {
        if (associationValue == null) {
            LOGGER.trace("No association");
            return null;
        }
        ObjectReferenceType shadowRef = ShadowAssociationsUtil.getSingleObjectRefRelaxed(associationValue);
        if (shadowRef == null) {
            LOGGER.trace("No unique shadowRef in association {}", associationValue);
            return null;
        }
        if (shadowRef.getObject() != null) {
            return (ShadowType) shadowRef.asReferenceValue().getObject().asObjectable();
        }

        LensProjectionContext projectionCtx = (LensProjectionContext) getProjectionContext();
        if (projectionCtx == null) {
            LOGGER.trace("No projection found. Skipping resolving entitlement");
            return null;
        }

        Map<String, PrismObject<ShadowType>> entitlementMap = projectionCtx.getEntitlementMap();
        if (entitlementMap == null) {
            LOGGER.trace("No entitlement map present in projection context {}", projectionCtx);
            return null;
        }

        PrismObject<ShadowType> entitlement = entitlementMap.get(shadowRef.getOid());
        if (entitlement == null) {
            LOGGER.trace("No entitlement with oid {} found among resolved entitlement {}.", shadowRef, entitlementMap);
            return null;
        }
        LOGGER.trace("Returning resolved entitlement: {}", entitlement);
        return entitlement.asObjectable();
    }

    @Override
    public ExtensionType collectExtensions(AssignmentPathType path, int startAt)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return AssignmentPath.collectExtensions(path, startAt, modelService, getCurrentTask(), getCurrentResult());
    }

    @Override
    public TaskType executeChangesAsynchronously(Collection<ObjectDelta<?>> deltas, ModelExecuteOptions options,
            String templateTaskOid) throws SecurityViolationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        return executeChangesAsynchronously(deltas, options, templateTaskOid, getCurrentTask(), getCurrentResult());
    }

    @Override
    public TaskType executeChangesAsynchronously(Collection<ObjectDelta<?>> deltas, ModelExecuteOptions options,
            String templateTaskOid, Task opTask, OperationResult result) throws SecurityViolationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        if (principal == null) {
            throw new SecurityViolationException("No current user");
        }
        TaskType newTask;
        if (templateTaskOid != null) {
            newTask = modelService.getObject(TaskType.class, templateTaskOid,
                    getDefaultGetOptionCollection(), opTask, result).asObjectable();
        } else {
            newTask = new TaskType();
            newTask.setName(PolyStringType.fromOrig("Execute changes"));
        }
        newTask.setName(PolyStringType.fromOrig(newTask.getName().getOrig() + " " + (int) (Math.random() * 10000)));
        newTask.setOid(null);
        newTask.setTaskIdentifier(null);
        newTask.setOwnerRef(createObjectRef(principal.getFocus()));
        newTask.setExecutionState(RUNNABLE);
        newTask.setSchedulingState(TaskSchedulingStateType.READY);
        newTask.setActivity(null);
        ExplicitChangeExecutionWorkDefinitionType workDef = newTask.beginActivity()
                .beginWork()
                .beginExplicitChangeExecution();
        workDef.getDelta().addAll(
                getDeltaBeans(deltas));
        if (options != null) {
            workDef.setExecutionOptions(
                    options.toModelExecutionOptionsType());
        }

        ObjectDelta<TaskType> taskAddDelta = DeltaFactory.Object.createAddDelta(newTask.asPrismObject());
        Collection<ObjectDeltaOperation<? extends ObjectType>> operations = modelService
                .executeChanges(singleton(taskAddDelta), null, opTask, result);
        return (TaskType) operations.iterator().next().getObjectDelta().getObjectToAdd().asObjectable();
    }

    @NotNull
    private List<ObjectDeltaType> getDeltaBeans(Collection<ObjectDelta<?>> deltas) throws SchemaException {
        if (deltas.isEmpty()) {
            throw new IllegalArgumentException("No deltas to execute");
        }
        List<ObjectDeltaType> deltasBeans = new ArrayList<>();
        for (ObjectDelta<?> delta : deltas) {
            deltasBeans.add(DeltaConvertor.toObjectDeltaType(delta));
        }
        return deltasBeans;
    }

    @Override
    @Deprecated
    public TaskType submitTaskFromTemplate(String templateTaskOid, List<Item<?, ?>> extensionItems)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        return modelInteractionService.submitTaskFromTemplate(templateTaskOid, extensionItems, getCurrentTask(), getCurrentResult());
    }

    @Override
    @Deprecated
    public TaskType submitTaskFromTemplate(String templateTaskOid, Map<QName, Object> extensionValues)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        return modelInteractionService.submitTaskFromTemplate(templateTaskOid, extensionValues, getCurrentTask(), getCurrentResult());
    }

    @Override
    public @NotNull String submitTaskFromTemplate(@NotNull String templateOid, @NotNull ActivityCustomization customization)
            throws CommonException {
        return modelInteractionService.submitTaskFromTemplate(templateOid, customization, getCurrentTask(), getCurrentResult());
    }

    @Override
    public String translate(String key, Objects... args) {
        return translate(new SingleLocalizableMessage(key, args, key));
    }

    @Override
    public String translate(LocalizableMessage message) {
        return translate(message, true);
    }

    @Override
    public String translate(LocalizableMessage message, boolean useDefaultLocale) {
        Locale locale = findProperLocale(useDefaultLocale);

        return localizationService.translate(message, locale);
    }

    @Override
    public String translate(LocalizableMessageType message) {
        return translate(message, true);
    }

    @Override
    public String translate(LocalizableMessageType message, boolean useDefaultLocale) {
        Locale locale = findProperLocale(useDefaultLocale);

        return localizationService.translate(LocalizationUtil.toLocalizableMessage(message), locale);
    }

    @NotNull
    private Locale findProperLocale(boolean useDefaultLocale) {
        if (useDefaultLocale) {
            return AvailableLocale.getDefaultLocale();
        }

        MidPointPrincipal principal = SecurityUtil.getPrincipalSilent();
        Locale result = principal != null ? principal.getLocale() : null;

        return result != null ? result : AvailableLocale.getDefaultLocale();
    }

    @Override
    public Object executeAdHocProvisioningScript(ResourceType resource, String language, String code)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException, ObjectAlreadyExistsException {
        return executeAdHocProvisioningScript(resource.getOid(), language, code);
    }

    @Override
    public Object executeAdHocProvisioningScript(String resourceOid, String language, String code)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException, ObjectAlreadyExistsException {
        OperationProvisioningScriptType script = new OperationProvisioningScriptType();
        script.setCode(code);
        script.setLanguage(language);
        script.setHost(ProvisioningScriptHostType.RESOURCE);

        return provisioningService.executeScript(resourceOid, script, getCurrentTask(), getCurrentResult());
    }

    @Override
    public Boolean isEvaluateNew() {
        ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();
        if (scriptContext == null) {
            return null;
        }
        return scriptContext.isEvaluateNew();
    }

    @Override
    public Boolean getEvaluateNew() {
        return isEvaluateNew();
    }

    @Override
    @NotNull
    public Collection<PrismValue> collectAssignedFocusMappingsResults(@NotNull ItemPath path) throws SchemaException {
        ModelContext<?> lensContext = ModelExpressionThreadLocalHolder.getLensContextRequired();
        Collection<PrismValue> rv = new HashSet<>();
        for (EvaluatedAssignment evaluatedAssignment : lensContext.getNonNegativeEvaluatedAssignments()) {
            if (evaluatedAssignment.isValid()) {
                for (Mapping<?, ?> mapping : evaluatedAssignment.getFocusMappings()) {
                    if (path.equivalent(mapping.getOutputPath())) {
                        PrismValueDeltaSetTriple<?> outputTriple = mapping.getOutputTriple();
                        if (outputTriple != null) {
                            rv.addAll(outputTriple.getNonNegativeValues());
                        }
                    }
                }
            }
        }
//        // Ugly hack - MID-4452 - When having an assignment giving focusMapping, and the assignment is being deleted, the
//        // focus mapping is evaluated in wave 0 (results correctly being pushed to the minus set), but also in wave 1.
//        // The results are sent to zero set; and they are not applied only because they are already part of a priori delta.
//        // This causes problems here.
//        //
//        // Until MID-4452 is fixed, here we manually delete the values from the result.
//        ModelElementContext<ObjectType> focusContext = lensContext.getFocusContext();
//        if (focusContext != null) {
//            ObjectDelta<ObjectType> delta = focusContext.getDelta();
//            if (delta != null) {
//                ItemDelta<PrismValue, ItemDefinition> targetItemDelta = delta.findItemDelta(path);
//                if (targetItemDelta != null) {
//                    rv.removeAll(emptyIfNull(targetItemDelta.getValuesToDelete()));
//                }
//            }
//        }
        return rv;
    }

    private Collection<SelectorOptions<GetOperationOptions>> getDefaultGetOptionCollection() {
        return SelectorOptions.createCollection(GetOperationOptions.createExecutionPhase());
    }

    @Override
    public @NotNull <F extends FocusType> List<F> findCandidateOwners(
            @NotNull Class<F> focusType,
            @NotNull ShadowType shadow,
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent) throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {

        Preconditions.checkArgument(ShadowUtil.isKnown(kind), "kind is not known: %s", kind);
        Preconditions.checkArgument(ShadowUtil.isKnown(intent), "intent is not known: %s", intent);

        Task task = getCurrentTaskRequired();
        OperationResult result = getCurrentResult(OP_FIND_CANDIDATE_OWNERS)
                .createSubresult(OP_FIND_CANDIDATE_OWNERS);
        try {
            // We create a clone to avoid touching the shadow.
            ShadowType shadowClone = shadow.clone()
                    .kind(kind)
                    .intent(intent)
                    .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE);

            return correlationService.correlate(shadowClone, task, result)
                    .getAllCandidates(focusType);

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private <F extends FocusType> Class<F> getMoreSpecificType(
            @NotNull Class<? extends FocusType> class1, @NotNull Class<? extends FocusType> class2) {
        if (class1.isAssignableFrom(class2)) {
            //noinspection unchecked
            return (Class<F>) class2;
        } else if (class2.isAssignableFrom(class1)) {
            //noinspection unchecked
            return (Class<F>) class1;
        } else {
            throw new IllegalArgumentException(
                    "Classes " + class1.getName() + " and " + class2.getName() + " are not compatible");
        }
    }

    @Override
    public <F extends ObjectType> ModelContext<F> previewChanges(Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SchemaException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        return previewChanges(deltas, options, getCurrentResult());
    }

    @Override
    public <F extends ObjectType> ModelContext<F> previewChanges(Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SchemaException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        return modelInteractionService.previewChanges(deltas, options, getCurrentTask(), result);
    }

    @Override
    public <O extends ObjectType> void applyDefinition(O object)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (object instanceof ShadowType || object instanceof ResourceType) {
            provisioningService.applyDefinition(object.asPrismObject(), getCurrentTask(), getCurrentResult());
        }
    }

    @Override
    public <C extends Containerable> S_ItemEntry deltaFor(Class<C> objectClass) throws SchemaException {
        return prismContext.deltaFor(objectClass);
    }

    // MID-5243

    /**
     * DEPRECATED use getArchetypes(object) or getStructuralArchetype(object)
     */
    @Override
    @Deprecated
    public <O extends ObjectType> ArchetypeType getArchetype(O object) throws SchemaException {
        return ArchetypeTypeUtil.getStructuralArchetype(
                archetypeManager.determineArchetypes(object, getCurrentResult()));
    }

    @Override
    public @Nullable <O extends AssignmentHolderType> ArchetypeType getStructuralArchetype(O object)
            throws SchemaException {
        return archetypeManager.determineStructuralArchetype(object, getCurrentResult());
    }

    @NotNull
    public <O extends ObjectType> List<ArchetypeType> getArchetypes(O object) throws SchemaException {
        return archetypeManager.determineArchetypes(object, getCurrentResult());
    }

    // MID-5243

    /**
     * DEPRECATED use getArchetypeOids(object)
     */
    @Override
    @Deprecated
    public <O extends ObjectType> String getArchetypeOid(O object) throws SchemaException {
        List<String> archetypeOids = getArchetypeOids(object);
        return MiscUtil.extractSingleton(
                archetypeOids,
                () -> new SchemaException(
                        "Only a single archetype for an object is expected here: " + archetypeOids + " in " + object));
    }

    @NotNull
    public List<String> getArchetypeOids(ObjectType object) {
        return new ArrayList<>(
                archetypeManager.determineArchetypeOids(object));
    }

    // temporary
    public MessageWrapper wrap(AsyncUpdateMessageType message) {
        return new MessageWrapper(message, prismContext);
    }

    // temporary
    @SuppressWarnings("unused")
    public Map<String, Object> getMessageBodyAsMap(AsyncUpdateMessageType message) throws IOException {
        return wrap(message).getBodyAsMap();
    }

    // temporary
    @SuppressWarnings("unused")
    public Item<?, ?> getMessageBodyAsPrismItem(AsyncUpdateMessageType message) throws SchemaException {
        return wrap(message).getBodyAsPrismItem(PrismContext.LANG_XML);
    }

    @Override
    public <O extends ObjectType> void addRecomputeTrigger(PrismObject<O> object, Long timestamp,
            TriggerCustomizer triggerCustomizer)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        TriggerType trigger = new TriggerType()
                .handlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .timestamp(XmlTypeConverter.createXMLGregorianCalendar(timestamp != null ? timestamp : System.currentTimeMillis()));
        if (triggerCustomizer != null) {
            triggerCustomizer.customize(trigger);
        }
        Class<? extends ObjectType> objectType = object.asObjectable().getClass();
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(objectType)
                .item(ObjectType.F_TRIGGER).add(trigger)
                .asItemDeltas();
        repositoryService.modifyObject(
                objectType, object.getOid(), itemDeltas, getCurrentResult(CLASS_DOT + "addRecomputeTrigger"));
    }

    @Override
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @NotNull
    @Override
    public OptimizingTriggerCreator getOptimizingTriggerCreator(long fireAfter, long safetyMargin) {
        return new OptimizingTriggerCreatorImpl(
                triggerCreatorGlobalState,
                this,
                fireAfter,
                safetyMargin);
    }

    @NotNull
    @Override
    public <T> ShadowSimpleAttributeDefinition<T> getAttributeDefinition(PrismObject<ResourceType> resource, QName objectClassName,
                                                                         QName attributeName) throws SchemaException, ConfigurationException {
        var resourceSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        if (resourceSchema == null) {
            throw new SchemaException("No resource schema in " + resource);
        }
        ResourceObjectDefinition ocDef = resourceSchema.findDefinitionForObjectClass(objectClassName);
        if (ocDef == null) {
            throw new SchemaException("No definition of object class " + objectClassName + " in " + resource);
        }
        ShadowSimpleAttributeDefinition<?> attrDef = ocDef.findSimpleAttributeDefinition(attributeName);
        if (attrDef == null) {
            throw new SchemaException("No definition of attribute " + attributeName + " in object class " + objectClassName
                    + " in " + resource);
        }
        //noinspection unchecked
        return (ShadowSimpleAttributeDefinition<T>) attrDef;
    }

    @NotNull
    @Override
    public <T> ShadowSimpleAttributeDefinition<T> getAttributeDefinition(PrismObject<ResourceType> resource, String objectClassName,
                                                                         String attributeName) throws SchemaException, ConfigurationException {
        return getAttributeDefinition(resource, new QName(objectClassName), new QName(attributeName));
    }

    @Experimental
    public <T extends AssignmentHolderType> T findLinkedSource(Class<T> type) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return linkedObjectsFunctions.findLinkedSource(type);
    }

    @Experimental
    public <T extends AssignmentHolderType> T findLinkedSource(String linkType) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return linkedObjectsFunctions.findLinkedSource(linkType);
    }

    @Experimental
    public <T extends AssignmentHolderType> List<T> findLinkedSources(Class<T> type) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        return linkedObjectsFunctions.findLinkedSources(type);
    }

    @Experimental
    public <T extends AssignmentHolderType> List<T> findLinkedSources(String linkTypeName) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        return linkedObjectsFunctions.findLinkedSources(linkTypeName);
    }

    // Should be used after assignment evaluation!
    @Experimental
    public <T extends AssignmentHolderType> T findLinkedTarget(Class<T> type, String archetypeOid)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return linkedObjectsFunctions.findLinkedTarget(type, archetypeOid);
    }

    // Should be used after assignment evaluation!
    @Experimental
    public <T extends AssignmentHolderType> T findLinkedTarget(String linkTypeName)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return linkedObjectsFunctions.findLinkedTarget(linkTypeName);
    }

    // Should be used after assignment evaluation!
    @Experimental
    @NotNull
    public <T extends AssignmentHolderType> List<T> findLinkedTargets(Class<T> type, String archetypeOid)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return linkedObjectsFunctions.findLinkedTargets(type, archetypeOid);
    }

    // Should be used after assignment evaluation!
    @Experimental
    @NotNull
    public <T extends AssignmentHolderType> List<T> findLinkedTargets(String linkTypeName)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return linkedObjectsFunctions.findLinkedTargets(linkTypeName);
    }

    @Experimental
    public <T extends AssignmentHolderType> T createLinkedSource(String linkType) throws SchemaException, ConfigurationException {
        return linkedObjectsFunctions.createLinkedSource(linkType);
    }

    @Experimental
    public @Nullable ObjectReferenceType getFocusObjectReference() {
        ObjectType focusObject = getFocusObjectAny();
        String oid = focusObject.getOid();
        return oid != null ? ObjectTypeUtil.createObjectRef(focusObject) : null;
    }

    @Experimental
    @NotNull
    private <T extends ObjectType> T getFocusObjectAny() {
        //noinspection unchecked
        LensContext<T> lensContext = (LensContext<T>) getModelContext();
        if (lensContext == null) {
            throw new IllegalStateException("No model context present. Are you calling this method within model operation?");
        }
        LensFocusContext<T> focusContext = lensContext.getFocusContextRequired();
        PrismObject<T> object = focusContext.getObjectAny();
        if (object == null) {
            throw new IllegalStateException("No old, current, nor new object in focus context");
        }
        return object.asObjectable();
    }

    @Override
    public void createRecomputeTrigger(Class<? extends ObjectType> type, String oid) throws SchemaException,
            ObjectAlreadyExistsException, ObjectNotFoundException {
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + ".createRecomputeTrigger");

        TriggerType trigger = new TriggerType()
                .handlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .timestamp(XmlTypeConverter.createXMLGregorianCalendar());
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(type)
                .item(ObjectType.F_TRIGGER).add(trigger)
                .asItemDeltas();
        repositoryService.modifyObject(type, oid, itemDeltas, result);
    }

    public boolean extensionOptionIsNotFalse(String localName) {
        return BooleanUtils.isNotFalse(getBooleanExtensionOption(localName));
    }

    public boolean extensionOptionIsTrue(String localName) {
        return BooleanUtils.isTrue(getBooleanExtensionOption(localName));
    }

    public Boolean getBooleanExtensionOption(String localName) {
        return getExtensionOptionRealValue(localName, Boolean.class);
    }

    public Object getExtensionOptionRealValue(String localName) {
        return ModelExecuteOptions.getExtensionItemRealValue(getModelContext().getOptions(), new ItemName(localName), Object.class);
    }

    public <T> T getExtensionOptionRealValue(String localName, Class<T> type) {
        return ModelExecuteOptions.getExtensionItemRealValue(getModelContext().getOptions(), new ItemName(localName), type);
    }

    @Override
    public @Nullable CaseType getCorrelationCaseForShadow(@Nullable ShadowType shadow) throws SchemaException {
        if (shadow == null) {
            return null;
        } else {
            return correlationCaseManager.findCorrelationCase(shadow, false, getCurrentResult());
        }
    }

    @Override
    public String describeResourceObjectSetLong(ResourceObjectSetType set)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return describeResourceObjectSet(set, true);
    }

    @Override
    public String describeResourceObjectSetShort(ResourceObjectSetType set)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return describeResourceObjectSet(set, false);
    }

    private String describeResourceObjectSet(ResourceObjectSetType set, boolean longVersion)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (set == null) {
            return null;
        }

        ObjectReferenceType ref = set.getResourceRef();
        if (ref == null) {
            return null;
        }

        ObjectType object = resolveReferenceInternal(ref, true);
        if (!(object instanceof ResourceType resource)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(resource.getName().getOrig());

        ShadowKindType kind = set.getKind();
        String intent = set.getIntent();
        QName objectclass = set.getObjectclass();

        if (kind != null) {

            sb.append(": ");

            var definition = getResourceObjectDefinition(resource, kind, intent, objectclass);
            ResourceObjectTypeIdentification typeIdentification = definition != null ? definition.getTypeIdentification() : null;

            String typeDisplayName = definition != null ? definition.getDisplayName() : null;
            if (typeDisplayName != null) {
                sb.append(typeDisplayName);
                if (longVersion) {
                    sb.append(" (");
                    sb.append(getTypeIdDescription(kind, intent, typeIdentification, true));
                    sb.append(")");
                }
            } else {
                sb.append(getTypeIdDescription(kind, intent, typeIdentification, longVersion));
            }
        }

        // Actually, kind/intent + object class should not be used both
        if (objectclass != null && (longVersion || kind == null)) {
            sb.append(" [").append(objectclass.getLocalPart()).append("]");
        }

        return sb.toString();
    }

    private @NotNull String getTypeIdDescription(
            ShadowKindType origKind, String origIntent, ResourceObjectTypeIdentification resolvedTypeId, boolean longVersion) {
        if (origIntent != null) {
            return localizedTypeId(origKind, origIntent);
        } else {
            if (resolvedTypeId != null) {
                if (longVersion) {
                    return localizationService.translate(
                            "TypeIdDescription.asTheDefaultIntent",
                            new Object[] { localizedTypeId(resolvedTypeId.getKind(), resolvedTypeId.getIntent()) },
                            localizationService.getDefaultLocale());
                } else {
                    return localizedTypeId(resolvedTypeId.getKind(), resolvedTypeId.getIntent());
                }
            } else {
                return localizationService.translate(
                        "TypeIdDescription.defaultIntentFor",
                        new Object[] { SingleLocalizableMessage.forEnum(origKind) },
                        localizationService.getDefaultLocale());
            }
        }
    }

    private String localizedTypeId(@NotNull ShadowKindType kind, @NotNull String intent) {
        return localizationService.translate(
                "TypeIdDescription.typeId",
                new Object[] { SingleLocalizableMessage.forEnum(kind), intent },
                localizationService.getDefaultLocale());
    }

    private ResourceObjectDefinition getResourceObjectDefinition(
            ResourceType resource, ShadowKindType kind, String intent, QName objectclass) {
        ResourceSchema completeSchema;
        try {
            completeSchema = Resource.of(resource).getCompleteSchema();
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't get schema for {}", e, resource);
            return null;
        }
        if (completeSchema == null) {
            return null;
        }

        try {
            // This is the same method that is currently used when doing the processing in import/recon tasks.
            return ResourceSchemaUtil.findDefinitionForBulkOperation(resource, kind, intent, objectclass);
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't get definition for {}/{}/{} in {}",
                    e, kind, intent, objectclass, resource);
            return null;
        }
    }

    @Override
    public Collection<PrismValue> selectIdentityItemValues(
            @Nullable Collection<FocusIdentityType> identities,
            @Nullable FocusIdentitySourceType source,
            @NotNull ItemPath itemPath) {
        return beans.identitiesManager.selectIdentityItemValue(identities, source, itemPath);
    }

    @Override
    public <T> TypedQuery<T> queryFor(Class<T> type, String query) throws SchemaException {
        return TypedQuery.parse(type, query);
    }

    public <T> PreparedQuery<T> preparedQueryFor(Class<T> type, String query) throws SchemaException {
        return PreparedQuery.parse(type, query);
    }
    @Override
    public <T extends ObjectType> List<T> searchObjects(TypedQuery<T> query) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return MiscSchemaUtil.toObjectableList(
                modelService.searchObjects(query, getCurrentTask(), getCurrentResult()));
    }

    @Override
    public @Nullable ObjectReferenceType getObjectRef(@Nullable ShadowAssociationValueType associationValueBean) {
        return associationValueBean != null ?
                ShadowAssociationsUtil.getSingleObjectRefRelaxed(associationValueBean) :
                null;
    }
}
