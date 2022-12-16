/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.ElementState.CurrentObjectAdjuster;

import com.evolveum.midpoint.model.impl.lens.ElementState.ObjectDefinitionRefiner;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentSpec;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Lens context for a computation element - a focus or a projection.
 *
 * @author semancik
 */
@SuppressWarnings("CloneableClassWithoutClone")
public abstract class LensElementContext<O extends ObjectType> implements ModelElementContext<O>, Cloneable {

    private static final long serialVersionUID = 1649567559396392861L;

    private static final Trace LOGGER = TraceManager.getTrace(LensElementContext.class);

    /**
     * State of the element, i.e. object old, current, new, along with the respective deltas
     * (primary, secondary, current, summary).
     */
    @NotNull protected final ElementState<O> state;

    /** Keeps temporary PCV IDs to be applied on delta execution. */
    private TemporaryContainerIdStore<O> temporaryContainerIdStore;

    /**
     * List of all executed deltas (in fact, {@link LensObjectDeltaOperation} objects).
     * Updated by {@link ChangeExecutor}.
     */
    @NotNull private final List<LensObjectDeltaOperation<O>> executedDeltas = new ArrayList<>();

    /**
     * Were there any deltas executed during the last call to {@link ChangeExecutor}?
     *
     * Used to determine whether the context should be rotten.
     * (But currently only for focus context. Projections are treated in the original way.)
     */
     private transient boolean anyDeltasExecuted;

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
     * Everything related to policy rules evaluation and processing.
     */
    @NotNull final PolicyRulesContext policyRulesContext = new PolicyRulesContext();

    /**
     * Link to the parent context.
     */
    @NotNull protected final LensContext<? extends ObjectType> lensContext;

    public LensElementContext(@NotNull Class<O> objectTypeClass, @NotNull LensContext<? extends ObjectType> lensContext) {
        this.state = new ElementState<>(
                objectTypeClass,
                getCurrentObjectAdjuster(),
                getObjectDefinitionRefiner());
        this.lensContext = lensContext;
    }

    public LensElementContext(@NotNull ElementState<O> elementState, @NotNull LensContext<? extends ObjectType> lensContext) {
        this.state = elementState;
        this.lensContext = lensContext;
    }

    //region Behavior customization

    @NotNull CurrentObjectAdjuster<O> getCurrentObjectAdjuster() {
        return o -> o;
    }

    @NotNull ObjectDefinitionRefiner<O> getObjectDefinitionRefiner() {
        return o -> o;
    }

    //endregion

    //region Basic getters and setters and similar methods (no surprises here)

    public @NotNull LensContext<? extends ObjectType> getLensContext() {
        return lensContext;
    }

    @Override
    public @NotNull ModelContext<?> getModelContext() {
        return lensContext;
    }

    @Override
    public @NotNull Class<O> getObjectTypeClass() {
        return state.getObjectTypeClass();
    }

    public @NotNull PrismObjectDefinition<O> getObjectDefinition() {
        return state.getObjectDefinition();
    }

    public boolean represents(Class<?> type) {
        return type.isAssignableFrom(getObjectTypeClass());
    }

    @Override
    public boolean isOfType(Class<?> aClass) {
        return state.isOfType(aClass);
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

    @Override
    public String getOid() {
        return state.getOid();
    }

    @Override
    public PrismObject<O> getObjectOld() {
        return state.getOldObject();
    }

    @Override
    public PrismObject<O> getObjectCurrent() {
        return state.getCurrentObject();
    }

    @Override
    public PrismObject<O> getObjectNew() {
        return state.getNewObject();
    }

    public @Nullable PrismObject<O> getObjectNewOrCurrentOrOld() {
        return state.getNewOrCurrentOrOld();
    }

    public @Nullable PrismObject<O> getObjectCurrentOrNew() {
        return state.getCurrentOrNewObject();
    }

    public @NotNull PrismObject<O> getObjectNewOrCurrentRequired() {
        return state.getNewOrCurrentObjectRequired();
    }

    public @Nullable PrismObject<O> getObjectCurrentOrOld() {
        return state.getCurrentOrOldObject();
    }

    @Override
    public ObjectDelta<O> getPrimaryDelta() {
        return state.getPrimaryDelta();
    }

    boolean hasPrimaryDelta() {
        return state.hasPrimaryDelta();
    }

    @Override
    public ObjectDelta<O> getSecondaryDelta() {
        return state.getSecondaryDelta();
    }

    boolean hasSecondaryDelta() {
        return state.hasSecondaryDelta();
    }

    @Override
    public ObjectDelta<O> getCurrentDelta() {
        return state.getCurrentDelta();
    }

    @Override
    public ObjectDelta<O> getSummaryDelta() {
        return state.getSummaryDelta();
    }

    @NotNull ObjectDeltaWaves<O> getArchivedSecondaryDeltas() {
        return state.getArchivedSecondaryDeltas();
    }

    public String getObjectReadVersion() {
        // Do NOT use version from object current.
        // Current object may be re-read, but the computation
        // might be based on older data (objectOld).
        if (getObjectOld() != null) {
            return getObjectOld().getVersion();
        }
        return null;
    }
    //endregion

    //region Object state (old, current) changing methods
    /**
     * Sets OID of the new object but also to the deltas (if applicable).
     */
    public void setOid(String oid) {
        state.setOid(oid);
    }

    /**
     * Sets the value of an object that should be present on the clockwork start:
     * both objectCurrent and objectOld.
     *
     * Assumes that clockwork has not started yet.
     */
    public void setInitialObject(@NotNull PrismObject<O> object) {
        setInitialObject(object, null);
    }

    /**
     * Sets the value of an object that should be present on the clockwork start:
     * both objectCurrent, and (if delta is not "add") also objectOld.
     *
     * Assumes that clockwork has not started yet.
     */
    private void setInitialObject(@NotNull PrismObject<O> object, @Nullable ObjectDelta<O> objectDelta) {
        lensContext.checkNotStarted("set initial object value", this);
        state.setInitialObject(object, ObjectDelta.isAdd(objectDelta));
    }

    /**
     * Sets current and possibly also old object. This method is used with
     * freshly loaded object. The object is set as current object.
     * If the old object should be initialized, then the object is also set as old object.
     *
     * Should be used only from the context loader!
     */
    public abstract void setLoadedObject(@NotNull PrismObject<O> object);

    /**
     * Updates the current object.
     *
     * Should be called only from the context loader.
     */
    public void setCurrentObject(@Nullable PrismObject<O> objectCurrent) {
        state.setCurrentObject(objectCurrent);
    }

    /**
     * Clears the current state, e.g. when determining that the object does not exist anymore.
     *
     * Should be used only from the context loader.
     */
    public void clearCurrentObject() {
        state.clearCurrentObject();
    }

    /**
     * Used to update current object and also the OID. (In cases when the OID might have changed as well.)
     *
     * Should be called only from the context loader.
     */
    public void setCurrentObjectAndOid(@NotNull PrismObject<O> object) {
        state.setCurrentObjectAndOid(object);
    }

    /**
     * Replaces OID, old, and current object state. Deltas (primary, secondary) are kept untouched.
     *
     * Currently used when doing some magic with resolving conflicts while iterating during the projection of projections.
     *
     * Very dangerous! Use at your own risk!
     */
    public void replaceOldAndCurrentObject(String oid, PrismObject<O> objectOld, PrismObject<O> objectCurrent) {
        state.replaceOldAndCurrentObject(oid, objectOld, objectCurrent);
    }
    //endregion

    //region Primary delta changing methods
    /**
     * Assumes clockwork was not started.
     */
    public void setPrimaryDelta(ObjectDelta<O> primaryDelta) {
        lensContext.checkNotStarted("set primary delta", this);
        state.setPrimaryDelta(primaryDelta);
    }

    /**
     * Sets the primary delta. Does not check for clockwork not being started, so use with care!
     *
     * TODO The check should be perhaps reduced to "context was not yet used" in the future.
     */
    public void setPrimaryDeltaAfterStart(ObjectDelta<O> primaryDelta) {
        state.setPrimaryDelta(primaryDelta);
    }

    /**
     * Adds (merges) a delta into primary delta.
     *
     * Use with care! (This method is 100% safe only when the clockwork has not started.)
     */
    public void addToPrimaryDelta(ObjectDelta<O> delta) throws SchemaException {
        if (ObjectDelta.isEmpty(delta)) {
            // no op
        } else if (delta.isAdd() || delta.isDelete()) {
            stateCheck(ObjectDelta.isEmpty(state.getPrimaryDelta()),
                    "Cannot add ADD or DELETE delta (%s) to existing primary delta (%s)",
                    delta, state.getPrimaryDelta());
            state.setPrimaryDelta(delta);
        } else {
            state.modifyPrimaryDelta(
                    primaryDelta -> {
                        primaryDelta.merge(delta);
                        // The following should be perhaps included in delta.merge method.
                        if (primaryDelta.getOid() == null && delta.getOid() != null) {
                            primaryDelta.setOid(delta.getOid());
                        }
                    });
        }
    }

    /**
     * Adds an item delta to primary delta.
     *
     * Dangerous. DO NOT USE unless you know what you are doing.
     * Used from tests and from some scripting hooks.
     */
    @SuppressWarnings("unused") // called from scripts
    @VisibleForTesting
    public void swallowToPrimaryDelta(ItemDelta<?,?> itemDelta) throws SchemaException {
        if (!ItemDelta.isEmpty(itemDelta)) {
            state.modifyPrimaryDelta(
                    primaryDelta -> primaryDelta.swallow(itemDelta));
        }
    }

    /**
     * Modifies the primary delta.
     *
     * Dangerous! Primary delta is generally supposed to be immutable. Use with utmost care!
     */
    public void modifyPrimaryDelta(DeltaModifier<O> modifier) throws SchemaException {
        state.modifyPrimaryDelta(modifier);
    }

    public void setEstimatedOldValuesInPrimaryDelta() throws SchemaException {
        if (getPrimaryDelta() != null && getObjectOld() != null && isModify()) {
            state.modifyPrimaryDelta(delta -> {
                for (ItemDelta<?,?> itemDelta: delta.getModifications()) {
                    LensUtil.setDeltaOldValue(this, itemDelta);
                }
            });
        }
    }
    //endregion

    //region Secondary delta changing methods
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
        state.swallowToSecondaryDelta(this, itemDelta);
    }
    //endregion

    //region Complex state-setting methods
    /**
     * Initializes the state of the element: sets old/current state and primary delta, clears the secondary delta.
     *
     * Use with care!
     */
    public void initializeElementState(String oid, PrismObject<O> objectOld, PrismObject<O> objectCurrent,
            ObjectDelta<O> primaryDelta) {
        state.initializeState(oid, objectOld, objectCurrent, primaryDelta);
    }

    void finishBuild() {
        state.normalizePrimaryDelta();
        state.freezePrimaryDelta();
    }
    //endregion

    //region Storing and restoring element state
    public RememberedElementState<O> rememberElementState() {
        return state.rememberState();
    }

    public void restoreElementState(@NotNull RememberedElementState<O> rememberedState) {
        state.restoreState(rememberedState);
    }
    //endregion

    //region Policy rules-related things
    @NotNull
    public List<ItemDelta<?, ?>> getPendingObjectPolicyStateModifications() {
        return policyRulesContext.getPendingObjectPolicyStateModifications();
    }

    public void clearPendingPolicyStateModifications() {
        policyRulesContext.clearPendingPolicyStateModifications();
    }

    public void addToPendingObjectPolicyStateModifications(ItemDelta<?, ?> modification) {
        policyRulesContext.addToPendingObjectPolicyStateModifications(modification);
    }

    @NotNull
    public Map<AssignmentSpec, List<ItemDelta<?, ?>>> getPendingAssignmentPolicyStateModifications() {
        return policyRulesContext.getPendingAssignmentPolicyStateModifications();
    }

    public void addToPendingAssignmentPolicyStateModifications(@NotNull AssignmentType assignment, @NotNull PlusMinusZero mode,
            @NotNull ItemDelta<?, ?> modification) {
        policyRulesContext.addToPendingAssignmentPolicyStateModifications(assignment, mode, modification);
    }

    public Integer getPolicyRuleCounter(String policyRuleIdentifier) {
        return policyRulesContext.getCounter(policyRuleIdentifier);
    }

    public void setPolicyRuleCounter(String policyRuleIdentifier, int value) {
        policyRulesContext.setCounter(policyRuleIdentifier, value);
    }

    public @NotNull Collection<EvaluatedPolicyRuleImpl> getObjectPolicyRules() {
        return policyRulesContext.getObjectPolicyRules();
    }

    public void addObjectPolicyRule(EvaluatedPolicyRuleImpl policyRule) {
        policyRulesContext.addObjectPolicyRule(policyRule);
    }

    public void clearObjectPolicyRules() {
        policyRulesContext.clearObjectPolicyRules();
    }

    public void triggerRule(@NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        LensUtil.triggerRule(rule, triggers);
    }
    //endregion

    //region Kinds of operations
    public abstract boolean isAdd();

    public abstract boolean isDelete();

    /**
     * TODO description
     */
    public boolean isModify() {
        return ObjectDelta.isModify(getCurrentDelta());
    }

    /**
     * Returns a characterization of current operation (add, delete, modify).
     */
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
    //endregion

    //region Executed deltas
    @Override
    public @NotNull List<LensObjectDeltaOperation<O>> getExecutedDeltas() {
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

    public void addToExecutedDeltas(LensObjectDeltaOperation<O> executedDelta) {
        executedDeltas.add(executedDelta.clone()); // must be cloned because e.g. for ADD deltas the object gets modified afterwards
    }

    /**
     * See {@link LensContext#wasAnythingExecuted()}.
     */
    @Experimental
    boolean wasAnythingReallyExecuted() {
        return executedDeltas.stream()
                .anyMatch(ObjectDeltaOperation::wasReallyExecuted);
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

    void clearAnyDeltasExecutedFlag() {
        anyDeltasExecuted = false;
    }

    public void setAnyDeltasExecutedFlag() {
        anyDeltasExecuted = true;
    }

    boolean getAnyDeltasExecutedFlag() {
        return anyDeltasExecuted;
    }
    //endregion

    //region Freshness and clean-ups
    public boolean isFresh() {
        return state.isFresh();
    }

    public void setFresh(boolean fresh) {
        state.setFresh(fresh);
    }

    public void rot() {
        setFresh(false);
    }

    /**
     * Removes results of any previous computations from the context.
     * (Expecting that transient values are not present. So deals only with non-transient ones.
     * Currently this means deletion of secondary deltas.)
     */
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting // used also by scripts
    public void deleteNonTransientComputationResults() {
        state.clearSecondaryDelta();
    }

    /**
     * Cleans up the contexts by removing some of the working state.
     */
    public abstract void cleanup();
    //endregion

    //region Security policy
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
    //endregion

    //region Maintenance and auxiliary methods
    /**
     * There is no longer explicit "recompute" action. The validity of computed parts (current/summary delta,
     * adjusted current object, new object) is continuously monitored, and these parts are (re)computed
     * as necessary. So here we may have to call {@link #getObjectNew()} to trigger the recomputation, if needed.
     *
     * We might even remove this method altogether.
     */
    public void recompute() throws SchemaException {
        getObjectNew();
    }

    public void normalize() {
        state.normalize();
    }

    public void adopt(PrismContext prismContext) throws SchemaException {
        state.adopt(prismContext);
    }

    void copyValues(LensElementContext<O> clone) {
        CloneUtil.cloneMembersToCollection(clone.executedDeltas, executedDeltas);
        clone.iteration = this.iteration;
        clone.iterationToken = this.iterationToken;
        clone.securityPolicy = this.securityPolicy;
        clone.policyRulesContext.copyFrom(this.policyRulesContext);
    }

    public void checkEncrypted() {
        state.checkEncrypted();
    }

    public void forEachObject(Consumer<PrismObject<O>> consumer) {
        state.forEachObject(consumer);
    }

    public void forEachDelta(Consumer<ObjectDelta<O>> consumer) {
        state.forEachDelta(consumer);
    }
    //endregion

    //region Diagnostics
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
    //endregion

    //region Consistency checks
    public final void checkConsistence() {
        checkConsistence(null);
    }

    public abstract void checkConsistence(String contextDesc);

    abstract void doExtraObjectConsistenceCheck(@NotNull PrismObject<O> object, String elementDesc, String contextDesc);
    //endregion

    //region XML serialization and deserialization
    void storeIntoBean(LensElementContextType bean, LensContext.ExportType exportType) throws SchemaException {
        PrismObject<O> objectOld = state.getOldObject();
        if (objectOld != null && exportType != LensContext.ExportType.MINIMAL) {
            if (exportType == LensContext.ExportType.REDUCED) {
                bean.setObjectOldRef(ObjectTypeUtil.createObjectRef(objectOld, PrismContext.get()));
            } else {
                bean.setObjectOld(objectOld.asObjectable().clone());
            }
        }
        PrismObject<O> objectCurrent = state.getCurrentObject();
        if (objectCurrent != null && exportType == LensContext.ExportType.TRACE) {
            bean.setObjectCurrent(objectCurrent.asObjectable().clone());
        }
        PrismObject<O> objectNew = state.getNewObject();
        if (objectNew != null && exportType != LensContext.ExportType.MINIMAL) {
            if (exportType == LensContext.ExportType.REDUCED) {
                bean.setObjectNewRef(ObjectTypeUtil.createObjectRef(objectNew, PrismContext.get()));
            } else {
                bean.setObjectNew(objectNew.asObjectable().clone());
            }
        }
        if (exportType != LensContext.ExportType.MINIMAL) {
            ObjectDelta<O> primaryDelta = state.getPrimaryDelta();
            ObjectDelta<O> secondaryDelta = state.getSecondaryDelta();
            bean.setPrimaryDelta(primaryDelta != null ? DeltaConvertor.toObjectDeltaType(primaryDelta.clone()) : null);
            bean.setSecondaryDelta(secondaryDelta != null ? DeltaConvertor.toObjectDeltaType(secondaryDelta.clone()) : null);
            for (LensObjectDeltaOperation<?> executedDelta : executedDeltas) {
                bean.getExecutedDeltas()
                        .add(LensContext.simplifyExecutedDelta(executedDelta).toLensObjectDeltaOperationType());
            }
            bean.setObjectTypeClass(state.getObjectTypeClass().getName());
            bean.setOid(state.getOid());
            bean.setIteration(iteration);
            bean.setIterationToken(iterationToken);
        }
        bean.setFresh(state.isFresh());
    }

    void retrieveFromLensElementContextBean(LensElementContextType bean, Task task, OperationResult result)
            throws SchemaException {

        PrismObject<O> oldObject = asPrismObjectCast(bean.getObjectOld());
        applyProvisioningDefinition(oldObject, task, result);

        PrismObject<O> currentObject = asPrismObjectCast(bean.getObjectCurrent());
        applyProvisioningDefinition(currentObject, task, result);

        // Not using "getAnyObject" because the state is not yet set up.
        @Nullable ObjectType object = asObjectable(currentObject != null ? currentObject : oldObject);

        ObjectDeltaType primaryDeltaBean = bean.getPrimaryDelta();
        ObjectDelta<O> primaryDelta = primaryDeltaBean != null ?
                DeltaConvertor.createObjectDelta(primaryDeltaBean, PrismContext.get()) : null;
        applyProvisioningDefinition(primaryDelta, object, task, result);

        state.initializeState(bean.getOid(), oldObject, currentObject, primaryDelta);
        // New object is not set. It is computed on demand.

        for (LensObjectDeltaOperationType eDeltaOperationBean : bean.getExecutedDeltas()) {
            //noinspection unchecked
            LensObjectDeltaOperation<O> objectDeltaOperation =
                    LensObjectDeltaOperation.fromLensObjectDeltaOperationType(eDeltaOperationBean);
            if (objectDeltaOperation.getObjectDelta() != null) {
                applyProvisioningDefinition(objectDeltaOperation.getObjectDelta(), object, task, result);
            }
            this.executedDeltas.add(objectDeltaOperation);
        }

        this.iteration = bean.getIteration() != null ? bean.getIteration() : 0;
        this.iterationToken = bean.getIterationToken();

        // note: objectTypeClass is already converted (used in the constructor)
    }

    private PrismObject<O> asPrismObjectCast(ObjectType bean) {
        //noinspection unchecked
        return (PrismObject<O>) asPrismObject(bean);
    }

    void applyProvisioningDefinition(ObjectDelta<O> delta, Objectable object, Task task, OperationResult result)  {
        if (delta != null && isShadowOrResource(delta.getObjectTypeClass())) {
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

    private boolean isShadowOrResource(Class<O> clazz) {
        return clazz != null &&
                (ShadowType.class.isAssignableFrom(clazz) || ResourceType.class.isAssignableFrom(clazz));
    }

    private void applyProvisioningDefinition(PrismObject<O> object, Task task, OperationResult result)  {
        Objectable objectable = asObjectable(object);
        if (objectable instanceof ShadowType || objectable instanceof ResourceType) {
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
    //endregion

    //region Misc

    public int getTemporaryContainerId(@NotNull ItemPath itemPath) {
        if (temporaryContainerIdStore == null) {
            temporaryContainerIdStore = new TemporaryContainerIdStore<>();
        }
        return temporaryContainerIdStore.getTemporaryId(itemPath);
    }

    public void resolveTemporaryContainerIds(ObjectDelta<O> objectDelta) throws SchemaException {
        if (temporaryContainerIdStore != null) {
            temporaryContainerIdStore.resolveTemporaryIds(objectDelta);
            temporaryContainerIdStore = null;
        }
    }

    /** TODO */
    abstract @NotNull Collection<String> getEventTags();
    //endregion
}
