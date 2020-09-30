/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.function.Consumer;

import com.evolveum.midpoint.model.api.context.SynchronizationIntent;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentSpec;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;

/**
 * Lens context for a computation element - a focus or a projection.
 *
 * @author semancik
 */
public abstract class LensElementContext<O extends ObjectType> implements ModelElementContext<O>, Cloneable {

    private static final long serialVersionUID = 1649567559396392861L;

    private static final Trace LOGGER = TraceManager.getTrace(LensElementContext.class);

    /**
     * Type of object represented by this context.
     */
    @NotNull private final Class<O> objectTypeClass;

    /**
     * Definition of the object. Lazily evaluated.
     */
    private transient PrismObjectDefinition<O> objectDefinition;

    /**
     * OID of object represented by this context.
     *
     * Beware, it can change during the operation e.g. when handling ObjectNotFound exceptions
     * (or on many other occasions).
     */
    protected String oid;

    /**
     * "Old" state of the object i.e. the one that was present when the clockwork started.
     * It can be present on the beginning or filled-in during projector execution by the context loaded.
     *
     * It is used as an "old state" for resource object mappings (in constructions or resources),
     * persona mappings, notifications, policy rules, and so on.
     */
    protected PrismObject<O> objectOld;

    /**
     * "Current" state of the object i.e. the one that was present when the current projection
     * started. (I.e. when the Projector was entered, except for
     * {@link com.evolveum.midpoint.model.impl.lens.projector.Projector#projectAllWaves(LensContext, String, Task, OperationResult)}
     * method.
     *
     * It is typically filled-in by the context loader.
     *
     * It is used as an "old state" for focus mappings (in object template or assigned ones).
     */
    protected transient PrismObject<O> objectCurrent;

    /**
     * Expected state of the object after application of currentDelta i.e. item deltas computed
     * during current projection.
     *
     * This state is computed using the {@link #recompute()} method.
     */
    protected PrismObject<O> objectNew;

    /**
     * Indicates that the objectOld and objectCurrent contain relevant values.
     * Generally set to true by {@link com.evolveum.midpoint.model.impl.lens.projector.ContextLoader}
     * and reset to false by {@link LensContext#rot(String)} and {@link LensContext#rotIfNeeded()} methods.
     */
    private transient boolean isFresh;

    /**
     * Primary delta i.e. one that the caller specified that has to be executed.
     *
     * Sometimes it is also cleared e.g. in ConsolidationProcessor. TODO is this ok?
     */
    protected ObjectDelta<O> primaryDelta;

    /**
     * Secondary delta for current projection/execution wave.
     * For focus, it is archived and cleared after execution.
     */
    protected ObjectDelta<O> secondaryDelta;

    /**
     * List of all executed deltas (in fact, {@link LensObjectDeltaOperation} objects).
     * Updated by {@link ChangeExecutor}.
     */
    @NotNull private final List<LensObjectDeltaOperation<O>> executedDeltas = new ArrayList<>();

    /**
     * Intention of what we want to do with this projection context.
     * (The meaning for focus context is unknown.)
     *
     * Although there are more values, currently it seems that we use only UNLINK and DELETE ones.
     *
     * Set by the client (see {@link com.evolveum.midpoint.model.impl.sync.action.UnlinkAction}
     * and {@link com.evolveum.midpoint.model.impl.sync.action.DeleteShadowAction}) or automatically
     * determined from linkRef delete delta by {@link com.evolveum.midpoint.model.impl.lens.projector.ContextLoader}.
     */
    private SynchronizationIntent synchronizationIntent;

    /**
     * Current iteration when computing values for the object.
     *
     * The context can be recomputed several times. But we always want to use the same iterationToken if possible.
     * If there is a random part in the iterationToken expression that we need to avoid recomputing the token otherwise
     * the value can change all the time (even for the same inputs). Storing the token in the secondary delta is not
     * enough because secondary deltas can be dropped if the context is re-projected.
     */
    private int iteration;

    /**
     * Current iteration token.
     */
    private String iterationToken;

    /**
     * Security policy related to this object. (It looks like it is currently filled-in only for focus.)
     */
    private transient SecurityPolicyType securityPolicy;

    /**
     * Evaluated policy rules. Currently used only for focus objects.
     */
    private final Collection<EvaluatedPolicyRuleImpl> policyRules = new ArrayList<>();

    /**
     * Policy state modifications that should be applied.
     * Currently we apply them in ChangeExecutor.executeChanges only.
     *
     * In the future we plan to be able to apply some state modifications even
     * if the clockwork is exited in non-standard way (e.g. in primary state or with an exception).
     * But we must be sure what policy state to store, because some constraints might be triggered
     * because of expectation of future state (like conflicting assignment is added etc.)
     * ---
     * Although placed in LensElementContext, support for this data is currently implemented only
     * for focus, not for projections.
     */
    @NotNull private final List<ItemDelta<?,?>> pendingObjectPolicyStateModifications = new ArrayList<>();

    /**
     * Policy state modifications for assignments.
     *
     * Although we put here also deltas for assignments that are to be deleted, we do not execute these
     * (because we implement execution only for the standard exit-path from the clockwork).
     */
    @NotNull private final Map<AssignmentSpec, List<ItemDelta<?,?>>> pendingAssignmentPolicyStateModifications = new HashMap<>();

    /**
     * Link to the parent context.
     */
    private final LensContext<? extends ObjectType> lensContext;

    public LensElementContext(@NotNull Class<O> objectTypeClass, @NotNull LensContext<? extends ObjectType> lensContext) {
        Validate.notNull(objectTypeClass, "Object class is null");
        Validate.notNull(lensContext, "Lens context is null");
        this.lensContext = lensContext;
        this.objectTypeClass = objectTypeClass;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public String getIterationToken() {
        return iterationToken;
    }

    public void setIterationToken(String iterationToken) {
        this.iterationToken = iterationToken;
    }

    public SynchronizationIntent getSynchronizationIntent() {
        return synchronizationIntent;
    }

    public void setSynchronizationIntent(SynchronizationIntent synchronizationIntent) {
        this.synchronizationIntent = synchronizationIntent;
    }

    public LensContext<? extends ObjectType> getLensContext() {
        return lensContext;
    }

    @Override
    @NotNull
    public Class<O> getObjectTypeClass() {
        return objectTypeClass;
    }

    public boolean represents(Class<?> type) {
        return type.isAssignableFrom(objectTypeClass);
    }

    public PrismContext getPrismContext() {
        return lensContext.getPrismContext();
    }

    PrismContext getNotNullPrismContext() {
        return lensContext.getNotNullPrismContext();
    }

    @Override
    public PrismObject<O> getObjectOld() {
        return objectOld;
    }

    public void setObjectOld(PrismObject<O> objectOld) {
        this.objectOld = objectOld;
    }

    @Override
    public PrismObject<O> getObjectCurrent() {
        return objectCurrent;
    }

    public void setObjectCurrent(PrismObject<O> objectCurrent) {
        this.objectCurrent = objectCurrent;
    }

    public PrismObject<O> getObjectAny() {
        if (objectNew != null) {
            return objectNew;
        }
        if (objectCurrent != null) {
            return objectCurrent;
        }
        return objectOld;
    }

    /**
     * Sets current and possibly also old object. This method is used with
     * freshly loaded object. The object is set as current object.
     * If the old object was not initialized yet (and if it should be initialized)
     * then the object is also set as old object.
     */
    public void setLoadedObject(PrismObject<O> object) {
        setObjectCurrent(object);
        if (objectOld == null && !isAdd()) {
            setObjectOld(object.clone());
        }
    }

    @Override
    public PrismObject<O> getObjectNew() {
        return objectNew;
    }

    /**
     * Intentionally not public.
     * New state of the object should be updated only using {@link #recompute()} method.
     */
    void setObjectNew(PrismObject<O> objectNew) {
        this.objectNew = objectNew;
    }

    @Override
    public ObjectDelta<O> getPrimaryDelta() {
        return primaryDelta;
    }

    boolean hasPrimaryDelta() {
        return primaryDelta != null && !primaryDelta.isEmpty();
    }

    public void setPrimaryDelta(ObjectDelta<O> primaryDelta) {
        this.primaryDelta = primaryDelta;
    }

    public void addPrimaryDelta(ObjectDelta<O> delta) throws SchemaException {
        if (primaryDelta == null) {
            primaryDelta = delta;
        } else {
            primaryDelta.merge(delta);
        }
    }

    /**
     * Dangerous. DO NOT USE unless you know what you are doing.
     * Used from tests and from some scripting hooks.
     */
    @VisibleForTesting
    public void swallowToPrimaryDelta(ItemDelta<?,?> itemDelta) throws SchemaException {
        modifyOrCreatePrimaryDelta(
                delta -> delta.swallow(itemDelta),
                () -> {
                    ObjectDelta<O> newPrimaryDelta = getPrismContext().deltaFactory().object().create(getObjectTypeClass(), ChangeType.MODIFY);
                    newPrimaryDelta.setOid(oid);
                    newPrimaryDelta.addModification(itemDelta);
                    return newPrimaryDelta;
                });
    }

    void deleteSecondaryDeltas() {
        secondaryDelta = null;
    }

    @FunctionalInterface
    private interface DeltaModifier<O extends Objectable> {
        void modify(ObjectDelta<O> delta) throws SchemaException;
    }

    @FunctionalInterface
    private interface DeltaCreator<O extends Objectable> {
        ObjectDelta<O> create() throws SchemaException;
    }

    private void modifyOrCreatePrimaryDelta(DeltaModifier<O> modifier, DeltaCreator<O> creator) throws SchemaException {
        if (primaryDelta == null) {
            primaryDelta = creator.create();
        } else if (!primaryDelta.isImmutable()) {
            modifier.modify(primaryDelta);
        } else {
            primaryDelta = primaryDelta.clone();
            modifier.modify(primaryDelta);
            primaryDelta.freeze();
        }
    }

    public void swallowToSecondaryDelta(Collection<? extends ItemDelta<?, ?>> itemDeltas) throws SchemaException {
        for (ItemDelta<?, ?> itemDelta : itemDeltas) {
            swallowToSecondaryDelta(itemDelta);
        }
    }

    public void swallowToSecondaryDeltaUnchecked(ItemDelta<?, ?> itemDelta) {
        try {
            swallowToSecondaryDelta(itemDelta);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected SchemaException while swallowing secondary delta: " + e.getMessage(), e);
        }
    }

    public void swallowToSecondaryDelta(ItemDelta<?, ?> itemDelta) throws SchemaException {
        if (itemDelta == null || itemDelta.isInFactEmpty()) {
            return;
        }

        ObjectDelta<O> currentDelta = getCurrentDelta();
        // TODO change this
        if (currentDelta != null && currentDelta.containsModification(itemDelta, EquivalenceStrategy.DATA.exceptForValueMetadata())) {
            return;
        }

        LensUtil.setDeltaOldValue(this, itemDelta);

        if (secondaryDelta == null) {
            secondaryDelta = getPrismContext().deltaFactory().object().create(getObjectTypeClass(), ChangeType.MODIFY);
            secondaryDelta.setOid(getOid());
        }
        secondaryDelta.swallow(itemDelta);
    }

    @NotNull List<ItemDelta<?, ?>> getPendingObjectPolicyStateModifications() {
        return pendingObjectPolicyStateModifications;
    }

    public void clearPendingObjectPolicyStateModifications() {
        pendingObjectPolicyStateModifications.clear();
    }

    public void addToPendingObjectPolicyStateModifications(ItemDelta<?, ?> modification) {
        pendingObjectPolicyStateModifications.add(modification);
    }

    @NotNull Map<AssignmentSpec, List<ItemDelta<?, ?>>> getPendingAssignmentPolicyStateModifications() {
        return pendingAssignmentPolicyStateModifications;
    }

    public void clearPendingAssignmentPolicyStateModifications() {
        pendingAssignmentPolicyStateModifications.clear();
    }

    public void addToPendingAssignmentPolicyStateModifications(@NotNull AssignmentType assignment, @NotNull PlusMinusZero mode, @NotNull ItemDelta<?, ?> modification) {
        AssignmentSpec spec = new AssignmentSpec(assignment, mode);
        pendingAssignmentPolicyStateModifications.computeIfAbsent(spec, k -> new ArrayList<>()).add(modification);
    }

    public boolean isModify() {
        return ObjectDelta.isModify(getCurrentDelta());
    }

    @NotNull
    public SimpleOperationName getOperation() {
        if (isAdd()) {
            return SimpleOperationName.ADD;
        } else if (isDelete()) {
            return SimpleOperationName.DELETE;
        } else {
            return SimpleOperationName.MODIFY;
        }
    }

    @NotNull
    @Override
    public List<LensObjectDeltaOperation<O>> getExecutedDeltas() {
        return executedDeltas;
    }

    List<LensObjectDeltaOperation<O>> getExecutedDeltas(Boolean audited) {
        if (audited == null) {
            return executedDeltas;
        }
        List<LensObjectDeltaOperation<O>> deltas = new ArrayList<>();
        for (LensObjectDeltaOperation<O> delta: executedDeltas) {
            if (delta.isAudited() == audited) {
                deltas.add(delta);
            }
        }
        return deltas;
    }

    void markExecutedDeltasAudited() {
        for (LensObjectDeltaOperation<O> executedDelta: executedDeltas) {
            executedDelta.setAudited(true);
        }
    }

    void addToExecutedDeltas(LensObjectDeltaOperation<O> executedDelta) {
        executedDeltas.add(executedDelta.clone()); // must be cloned because e.g. for ADD deltas the object gets modified afterwards
    }

    @Override
    public ObjectDelta<O> getSummaryDelta() {
        return getCurrentDelta();
    }

    @Override
    public ObjectDelta<O> getCurrentDelta() {
        if (doesPrimaryDeltaApply()) {
            try {
                //noinspection unchecked
                return ObjectDeltaCollectionsUtil.union(primaryDelta, secondaryDelta);
            } catch (SchemaException e) {
                throw new SystemException("Unexpected schema exception while merging deltas: " + e.getMessage(), e);
            }
        } else {
            return CloneUtil.clone(secondaryDelta); // TODO do we need this?
        }
    }

    abstract boolean doesPrimaryDeltaApply();

    @Override
    public ObjectDelta<O> getSecondaryDelta() {
        return secondaryDelta;
    }

    public void setSecondaryDelta(ObjectDelta<O> secondaryDelta) {
        this.secondaryDelta = secondaryDelta;
    }

    public boolean wasAddExecuted() {
        for (LensObjectDeltaOperation<O> executedDeltaOperation : getExecutedDeltas()) {
            ObjectDelta<O> executedDelta = executedDeltaOperation.getObjectDelta();
            if (executedDelta.isAdd() && executedDelta.getObjectToAdd() != null &&
                    executedDelta.getObjectTypeClass().equals(getObjectTypeClass())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getOid() {
        if (oid == null) {
            oid = determineOid();
        }
        return oid;
    }

    private String determineOid() {
        if (getObjectOld() != null && getObjectOld().getOid() != null) {
            return getObjectOld().getOid();
        }
        if (getObjectCurrent() != null && getObjectCurrent().getOid() != null) {
            return getObjectCurrent().getOid();
        }
        if (getObjectNew() != null && getObjectNew().getOid() != null) {
            return getObjectNew().getOid();
        }
        if (getPrimaryDelta() != null && getPrimaryDelta().getOid() != null) {
            return getPrimaryDelta().getOid();
        }
        return null;
    }

    /**
     * Sets oid to the field but also to the deltas (if applicable).
     */
    public void setOid(String oid) {
        this.oid = oid;
        if (primaryDelta != null && !primaryDelta.isImmutable()) {
            primaryDelta.setOid(oid);
        }
        if (secondaryDelta != null) {
            secondaryDelta.setOid(oid);
        }
        // TODO What if primary delta is immutable ADD delta and objectNew was taken from it?
        //  It would be immutable as well in that case. We will see.
        if (objectNew != null) {
            objectNew.setOid(oid);
        }
    }

    public PrismObjectDefinition<O> getObjectDefinition() {
        if (objectDefinition == null) {
            if (objectOld != null) {
                objectDefinition = objectOld.getDefinition();
            } else if (objectCurrent != null) {
                objectDefinition = objectCurrent.getDefinition();
            } else if (objectNew != null) {
                objectDefinition = objectNew.getDefinition();
            } else {
                objectDefinition = getNotNullPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(getObjectTypeClass());
            }
        }
        return objectDefinition;
    }

    public boolean isFresh() {
        return isFresh;
    }

    public void setFresh(boolean isFresh) {
        this.isFresh = isFresh;
    }

    public void rot() {
        setFresh(false);
    }

    @NotNull
    public Collection<EvaluatedPolicyRuleImpl> getPolicyRules() {
        return policyRules;
    }

    public void addPolicyRule(EvaluatedPolicyRuleImpl policyRule) {
        this.policyRules.add(policyRule);
    }

    public void clearPolicyRules() {
        policyRules.clear();
    }

    public void triggerRule(@NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        LensUtil.triggerRule(rule, triggers);
    }

    /**
     * Returns security policy applicable to the object. This means security policy
     * applicable directory to focus or projection. It will NOT return global
     * security policy.
     */
    public SecurityPolicyType getSecurityPolicy() {
        return securityPolicy;
    }

    public void setSecurityPolicy(SecurityPolicyType securityPolicy) {
        this.securityPolicy = securityPolicy;
    }

    public CredentialsPolicyType getCredentialsPolicy() {
        return securityPolicy != null ? securityPolicy.getCredentials() : null;
    }

    public void recompute() throws SchemaException {
        ObjectDelta<O> currentDelta = getCurrentDelta();
        if (currentDelta == null) {
            objectNew = objectCurrent; // No change
        } else {
            objectNew = currentDelta.computeChangedObject(objectCurrent);
        }
    }

    public void checkConsistence() {
        checkConsistence(null);
    }

    public void checkConsistence(String contextDesc) {
        if (getObjectOld() != null) {
            checkConsistence(getObjectOld(), "old "+getElementDesc() , contextDesc);
        }
        if (getObjectCurrent() != null) {
            checkConsistence(getObjectCurrent(), "current "+getElementDesc() , contextDesc);
        }
        if (primaryDelta != null) {
            checkConsistence(primaryDelta, false, getElementDesc()+" primary delta in "+this + (contextDesc == null ? "" : " in " +contextDesc));
        }
        if (getObjectNew() != null) {
            checkConsistence(getObjectNew(), "new "+getElementDesc(), contextDesc);
        }
    }

    protected void checkConsistence(ObjectDelta<O> delta, boolean requireOid, String contextDesc) {
        try {
            delta.checkConsistence(requireOid, true, true, ConsistencyCheckScope.THOROUGH);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage()+"; in "+contextDesc, e);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage()+"; in "+contextDesc, e);
        }
        if (delta.isAdd()) {
            checkConsistence(delta.getObjectToAdd(), "add object", contextDesc);
        }
    }

    protected boolean isRequireSecondaryDeltaOid() {
        return primaryDelta == null;
    }

    protected void checkConsistence(PrismObject<O> object, String elementDesc, String contextDesc) {
        String desc = elementDesc+" in "+this + (contextDesc == null ? "" : " in " +contextDesc);
        try {
            object.checkConsistence(true, ConsistencyCheckScope.THOROUGH);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage()+"; in "+desc, e);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage()+"; in "+desc, e);
        }
        if (object.getDefinition() == null) {
            throw new IllegalStateException("No "+getElementDesc()+" definition "+desc);
        }
        O objectType = object.asObjectable();
        if (objectType instanceof ShadowType) {
            ShadowUtil.checkConsistence((PrismObject<? extends ShadowType>) object, desc);
        }
    }

    /**
     * Cleans up the contexts by removing some of the working state.
     */
    public abstract void cleanup();

    public void normalize() {
        if (objectNew != null) {
            objectNew.normalize();
        }
        if (objectOld != null) {
            objectOld.normalize();
        }
        if (objectCurrent != null) {
            objectCurrent.normalize();
        }
        if (primaryDelta != null && !primaryDelta.isImmutable()) {
            primaryDelta.normalize();
        }
        if (secondaryDelta != null) {
            secondaryDelta.normalize();
        }
    }

    public void adopt(PrismContext prismContext) throws SchemaException {
        if (objectNew != null) {
            prismContext.adopt(objectNew);
        }
        if (objectOld != null) {
            prismContext.adopt(objectOld);
        }
        if (objectCurrent != null) {
            prismContext.adopt(objectCurrent);
        }
        if (primaryDelta != null) {
            prismContext.adopt(primaryDelta);
        }
        if (secondaryDelta != null) {
            prismContext.adopt(secondaryDelta);
        }
        // TODO: object definition?
    }

    public abstract LensElementContext<O> clone(LensContext<? extends ObjectType> lensContext);

    void copyValues(LensElementContext<O> clone) {
        // This is de-facto immutable
        clone.objectDefinition = this.objectDefinition;
        clone.objectNew = cloneObject(this.objectNew);
        clone.objectOld = cloneObject(this.objectOld);
        clone.objectCurrent = cloneObject(this.objectCurrent);
        clone.oid = this.oid;
        clone.primaryDelta = cloneDelta(this.primaryDelta);
        clone.isFresh = this.isFresh;
        clone.iteration = this.iteration;
        clone.iterationToken = this.iterationToken;
        clone.securityPolicy = this.securityPolicy;
    }

    protected ObjectDelta<O> cloneDelta(ObjectDelta<O> thisDelta) {
        if (thisDelta == null) {
            return null;
        }
        return thisDelta.clone();
    }

    private PrismObject<O> cloneObject(PrismObject<O> thisObject) {
        if (thisObject == null) {
            return null;
        }
        return thisObject.clone();
    }

    void storeIntoLensElementContextType(LensElementContextType lensElementContextType, LensContext.ExportType exportType) throws SchemaException {
        if (objectOld != null && exportType != LensContext.ExportType.MINIMAL) {
            if (exportType == LensContext.ExportType.REDUCED) {
                lensElementContextType.setObjectOldRef(ObjectTypeUtil.createObjectRef(objectOld, getPrismContext()));
            } else {
                lensElementContextType.setObjectOld(objectOld.asObjectable().clone());
            }
        }
        if (objectCurrent != null && exportType == LensContext.ExportType.TRACE) {
            lensElementContextType.setObjectCurrent(objectCurrent.asObjectable().clone());
        }
        if (objectNew != null && exportType != LensContext.ExportType.MINIMAL) {
            if (exportType == LensContext.ExportType.REDUCED) {
                lensElementContextType.setObjectNewRef(ObjectTypeUtil.createObjectRef(objectNew, getPrismContext()));
            } else {
                lensElementContextType.setObjectNew(objectNew.asObjectable().clone());
            }
        }
        if (exportType != LensContext.ExportType.MINIMAL) {
            lensElementContextType.setPrimaryDelta(primaryDelta != null ? DeltaConvertor.toObjectDeltaType(primaryDelta.clone()) : null);
            lensElementContextType.setSecondaryDelta(secondaryDelta != null ? DeltaConvertor.toObjectDeltaType(secondaryDelta.clone()) : null);
            for (LensObjectDeltaOperation<?> executedDelta : executedDeltas) {
                lensElementContextType.getExecutedDeltas()
                        .add(LensContext.simplifyExecutedDelta(executedDelta).toLensObjectDeltaOperationType());
            }
            lensElementContextType.setObjectTypeClass(objectTypeClass.getName());
            lensElementContextType.setOid(oid);
            lensElementContextType.setIteration(iteration);
            lensElementContextType.setIterationToken(iterationToken);
            lensElementContextType.setSynchronizationIntent(
                    synchronizationIntent != null ? synchronizationIntent.toSynchronizationIntentType() : null);
        }
        lensElementContextType.setFresh(isFresh);
    }

    public void retrieveFromLensElementContextType(LensElementContextType lensElementContextType, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

        ObjectType objectTypeOld = lensElementContextType.getObjectOld();
        this.objectOld = objectTypeOld != null ? (PrismObject) objectTypeOld.asPrismObject() : null;
        fixProvisioningTypeInObject(this.objectOld, task, result);

        ObjectType objectTypeNew = lensElementContextType.getObjectNew();
        this.objectNew = objectTypeNew != null ? (PrismObject) objectTypeNew.asPrismObject() : null;
        fixProvisioningTypeInObject(this.objectNew, task, result);

        ObjectType object = objectTypeNew != null ? objectTypeNew : objectTypeOld;

        ObjectDeltaType primaryDeltaType = lensElementContextType.getPrimaryDelta();
        this.primaryDelta = primaryDeltaType != null ? (ObjectDelta) DeltaConvertor.createObjectDelta(primaryDeltaType, lensContext.getPrismContext()) : null;
        fixProvisioningTypeInDelta(this.primaryDelta, object, task, result);

        for (LensObjectDeltaOperationType eDeltaOperationType : lensElementContextType.getExecutedDeltas()) {
            LensObjectDeltaOperation objectDeltaOperation = LensObjectDeltaOperation.fromLensObjectDeltaOperationType(eDeltaOperationType, lensContext.getPrismContext());
            if (objectDeltaOperation.getObjectDelta() != null) {
                fixProvisioningTypeInDelta(objectDeltaOperation.getObjectDelta(), object, task, result);
            }
            this.executedDeltas.add(objectDeltaOperation);
        }

        this.oid = lensElementContextType.getOid();

        this.iteration = lensElementContextType.getIteration() != null ? lensElementContextType.getIteration() : 0;
        this.iterationToken = lensElementContextType.getIterationToken();
        this.synchronizationIntent = SynchronizationIntent.fromSynchronizationIntentType(lensElementContextType.getSynchronizationIntent());

        // note: objectTypeClass is already converted (used in the constructor)
    }

    protected void fixProvisioningTypeInDelta(ObjectDelta<O> delta, Objectable object, Task task, OperationResult result)  {
        if (delta != null && delta.getObjectTypeClass() != null && (ShadowType.class.isAssignableFrom(delta.getObjectTypeClass()) || ResourceType.class.isAssignableFrom(delta.getObjectTypeClass()))) {
            try {
                lensContext.getProvisioningService().applyDefinition(delta, object, task, result);
            } catch (Exception e) {
                LOGGER.warn("Error applying provisioning definitions to delta {}: {}", delta, e.getMessage());
                // In case of error just go on. Maybe we do not have correct definition here. But at least we can
                // display the GUI pages and maybe we can also salvage the operation.
                result.recordWarning(e);
            }
        }
    }

    private void fixProvisioningTypeInObject(PrismObject<O> object, Task task, OperationResult result)  {
        if (object != null && object.getCompileTimeClass() != null && (ShadowType.class.isAssignableFrom(object.getCompileTimeClass()) || ResourceType.class.isAssignableFrom(object.getCompileTimeClass()))) {
            try {
                lensContext.getProvisioningService().applyDefinition(object, task, result);
            } catch (Exception e) {
                LOGGER.warn("Error applying provisioning definitions to object {}: {}", object, e.getMessage());
                // In case of error just go on. Maybe we do not have correct definition here. But at least we can
                // display the GUI pages and maybe we can also salvage the operation.
                result.recordWarning(e);
            }
        }
    }

    public void checkEncrypted() {
        if (objectNew != null) {
            CryptoUtil.checkEncrypted(objectNew);
        }
        if (objectOld != null) {
            CryptoUtil.checkEncrypted(objectOld);
        }
        if (objectCurrent != null) {
            CryptoUtil.checkEncrypted(objectCurrent);
        }
        if (primaryDelta != null) {
            CryptoUtil.checkEncrypted(primaryDelta);
        }
        if (secondaryDelta != null) {
            CryptoUtil.checkEncrypted(secondaryDelta);
        }
    }

    public boolean operationMatches(ChangeTypeType operation) {
        switch (operation) {
            case ADD:
                return getOperation() == SimpleOperationName.ADD;
            case MODIFY:
                return getOperation() == SimpleOperationName.MODIFY;
            case DELETE:
                return getOperation() == SimpleOperationName.DELETE;
        }
        throw new IllegalArgumentException("Unknown operation "+operation);
    }

    protected abstract String getElementDefaultDesc();

    protected String getElementDesc() {
        PrismObject<O> object = getObjectNew();
        if (object == null) {
            object = getObjectOld();
        }
        if (object == null) {
            object = getObjectCurrent();
        }
        if (object == null) {
            return getElementDefaultDesc();
        }
        return object.toDebugType();
    }

    protected String getDebugDumpTitle() {
        return StringUtils.capitalize(getElementDesc());
    }

    protected String getDebugDumpTitle(String suffix) {
        return getDebugDumpTitle()+" "+suffix;
    }

    public abstract String getHumanReadableName();

    public String getObjectReadVersion() {
        // Do NOT use version from object current.
        // Current object may be re-read, but the computation
        // might be based on older data (objectOld).
//        if (getObjectCurrent() != null) {
//            return getObjectCurrent().getVersion();
//        }
        if (getObjectOld() != null) {
            return getObjectOld().getVersion();
        }
        return null;
    }

    PrismObject<O> getObjectCurrentOrOld() {
        return objectCurrent != null ? objectCurrent : objectOld;
    }

    @Override
    public boolean isOfType(Class<?> aClass) {
        if (aClass.isAssignableFrom(objectTypeClass)) {
            return true;
        }
        PrismObject<O> object = getObjectAny();
        return object != null && aClass.isAssignableFrom(object.asObjectable().getClass());
    }

    public S_ItemEntry deltaBuilder() throws SchemaException {
        return getPrismContext().deltaFor(getObjectTypeClass());
    }

    public void forEachObject(Consumer<PrismObject<O>> consumer) {
        if (objectCurrent != null) {
            consumer.accept(objectCurrent);
        }
        if (objectOld != null) {
            consumer.accept(objectOld);
        }
        if (objectNew != null) {
            consumer.accept(objectNew);
        }
    }

    public void forEachDelta(Consumer<ObjectDelta<O>> consumer) {
        if (primaryDelta != null) {
            consumer.accept(primaryDelta);
        }
        if (secondaryDelta != null) {
            consumer.accept(secondaryDelta);
        }
    }

    public void finishBuild() {
        if (primaryDelta != null) {
            primaryDelta.normalize();
            primaryDelta.freeze();
        }
    }
}
