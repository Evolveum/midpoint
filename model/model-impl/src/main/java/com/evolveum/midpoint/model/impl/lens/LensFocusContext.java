/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;

import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.executor.ItemChangeApplicationModeConfiguration;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.model.api.indexing.IndexingConfiguration;
import com.evolveum.midpoint.model.common.LinkManager;
import com.evolveum.midpoint.model.impl.lens.identities.IdentitiesManager;
import com.evolveum.midpoint.model.impl.lens.indexing.IndexingConfigurationImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ArchetypeTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * @author semancik
 *
 */
public class LensFocusContext<O extends ObjectType> extends LensElementContext<O> {

    private static final Trace LOGGER = TraceManager.getTrace(LensFocusContext.class);

    /**
     * True if the focus object was deleted by our processing.
     *
     * (Note we do not currently provide this kind of flag on the projection contexts, because of not being
     * sure if deleted projection cannot be somehow "resurrected" during the processing. For focal objects nothing like
     * this should happen.)
     *
     * Used to clarify context (re)loading for deleted focus situations. See MID-4856.
     */
    protected boolean deleted;

    /**
     * Resolved {@link ArchetypeType} objects relevant for the focus object. Both structural and auxiliary ones.
     *
     * TODO do we (still) need to maintain this information? It looks like it is not used anywhere, except when
     *  determining {@link #archetypePolicy}.
     */
    private transient List<ArchetypeType> archetypes;

    /** Archetype policy driving the working with the focus object. */
    private transient ArchetypePolicyType archetypePolicy;

    /** Object template relevant for the focus object. Unexpanded (legacy) form. */
    private transient ObjectTemplateType focusTemplate;

    /** Object template relevant for the focus object. Expanded (new) form. */
    private transient ObjectTemplateType expandedFocusTemplate;

    private transient IdentityManagementConfiguration identityManagementConfiguration; // TODO
    private transient IndexingConfiguration indexingConfiguration; // TODO

    private boolean primaryDeltaExecuted;

    /**
     * True if we should set also the "old" object state upon loading the object.
     *
     * Reason: When the clockwork is started, it may be provided by the focus object (old/current/new). However, this object
     * can be (and usually is) incomplete, e.g. a user is without `jpegPhoto`. For various reasons we do our own loading in the
     * initial stages of the clockwork. To be consistent, we'll set also the "old" object state. But only once! This is
     * controlled by this flag.
     *
     * See also MID-7916.
     */
    private boolean rewriteOldObject = true;

    // extracted from the template(s)
    // this is not to be serialized into XML, but let's not mark it as transient
    @NotNull private PathKeyedMap<ObjectTemplateItemDefinitionType> itemDefinitionsMap = new PathKeyedMap<>();

    public LensFocusContext(Class<O> objectTypeClass, LensContext<O> lensContext) {
        super(objectTypeClass, lensContext);
    }

    public LensFocusContext(ElementState<O> elementState, LensContext<O> lensContext) {
        super(elementState, lensContext);
    }

    @Override
    public void setLoadedObject(@NotNull PrismObject<O> object) {
        state.setCurrentAndOptionallyOld(object, shouldSetOldObject());
        rewriteOldObject = false;
    }

    private boolean shouldSetOldObject() {
        if (isAdd()) {
            LOGGER.trace("Operation is ADD, old state of the object is expected to be null");
            return false;
        } else if (rewriteOldObject) {
            LOGGER.trace("Old state of the object has not been set yet; let's do it now");
            return true;
        } else if (state.hasOldObject()) {
            LOGGER.trace("There's already an old state of the object; not rewriting it");
            return false;
        } else {
            LOGGER.trace("Huh? We should have already set the old state of the object and it's not set. But let's do that.");
            return true; // Just because this was the pre-4.6 behavior.
        }
    }

    public ArchetypePolicyType getArchetypePolicy() {
        return archetypePolicy;
    }

    public void setArchetypePolicy(ArchetypePolicyType value) {
        this.archetypePolicy = value;
    }

    public ArchetypeType getArchetype() {
        try {
            return ArchetypeTypeUtil.getStructuralArchetype(archetypes);
        } catch (SchemaException e) {
            LOGGER.error("Cannot get structural archetype, {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<ArchetypeType> getArchetypes() {
        return archetypes;
    }

    public void setArchetypes(List<ArchetypeType> archetypes) {
        this.archetypes = archetypes;
    }

    /** Precondition: context is "complete" i.e. {@link #archetypes} are filled in. */
    public ObjectReferenceType getStructuralArchetypeRef() throws SchemaException {
        return ObjectTypeUtil.createObjectRef(
                ArchetypeTypeUtil.getStructuralArchetype(
                        MiscUtil.stateNonNull(archetypes, () -> "Information about archetypes is not present")));
    }

    public ObjectTemplateType getFocusTemplate() {
        return focusTemplate;
    }

    public void setFocusTemplate(ObjectTemplateType focusTemplate) {
        this.focusTemplate = focusTemplate;
    }

    public void setExpandedFocusTemplate(ObjectTemplateType expandedFocusTemplate) {
        this.expandedFocusTemplate = expandedFocusTemplate;
        identityManagementConfiguration = null;
        indexingConfiguration = null;
        itemChangeApplicationModeConfiguration = null;
    }

    public boolean isFocusTemplateSetExplicitly() {
        return lensContext.getExplicitFocusTemplateOid() != null;
    }

    // preliminary version
    public @NotNull IdentityManagementConfiguration getIdentityManagementConfiguration() throws ConfigurationException {
        if (identityManagementConfiguration == null) {
            identityManagementConfiguration = IdentitiesManager.createIdentityConfiguration(expandedFocusTemplate);
        }
        return identityManagementConfiguration;
    }

    // preliminary version
    public @NotNull IndexingConfiguration getIndexingConfiguration() throws ConfigurationException {
        if (indexingConfiguration == null) {
            indexingConfiguration = IndexingConfigurationImpl.of(expandedFocusTemplate);
        }
        return indexingConfiguration;
    }

    @Override
    @NotNull
    ItemChangeApplicationModeConfiguration createItemChangeApplicationModeConfiguration() throws ConfigurationException {
        return ItemChangeApplicationModeConfiguration.of(expandedFocusTemplate);
    }

    public LifecycleStateModelType getLifecycleModel() {
        if (archetypePolicy == null) {
            return null;
        }
        return archetypePolicy.getLifecycleStateModel();
    }

    public boolean isDelete() {
        return ObjectDelta.isDelete(state.getPrimaryDelta());
    }

    @Override
    public ObjectDelta<O> getSummarySecondaryDelta() {
        return state.getSummarySecondaryDelta();
    }

    public boolean isAdd() {
        return ObjectDelta.isAdd(state.getPrimaryDelta());
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted() {
        deleted = true;
    }

    /**
     * Returns object-delta-object structure based on the current state.
     * I.e. objectCurrent - currentDelta - objectNew.
     */
    @NotNull
    public ObjectDeltaObject<O> getObjectDeltaObjectRelative() {
        return new ObjectDeltaObject<>(getObjectCurrent(), getCurrentDelta(), getObjectNew(), getObjectDefinition())
                .normalizeValuesToDelete(true); // FIXME temporary solution
    }

    @NotNull
    public ObjectDeltaObject<O> getObjectDeltaObjectAbsolute() {
        return new ObjectDeltaObject<>(getObjectOld(), getSummaryDelta(), getObjectNew(), getObjectDefinition())
                .normalizeValuesToDelete(true); // FIXME temporary solution
    }

    // This method may be useful for hooks. E.g. if a hook wants to insert a special secondary delta to avoid
    // splitting the changes to several audit records. It is not entirely clean and we should think about a better
    // solution in the future. But it is good enough for now.
    //
    // The name is misleading but we keep it for compatibility reasons.
    @SuppressWarnings("unused")
    @Deprecated
    public void swallowToWave0SecondaryDelta(ItemDelta<?,?> itemDelta) throws SchemaException {
        swallowToSecondaryDelta(itemDelta);
    }

    @Override
    public void cleanup() {
        // Clean up only delta in current wave. The deltas in previous waves are already done.
        // FIXME: this somehow breaks things. don't know why. but don't really care. the waves will be gone soon anyway
//        if (secondaryDeltas.get(getWave()) != null) {
//            secondaryDeltas.remove(getWave());
//        }
    }

//    @Override
//    public void reset() {
//        super.reset();
//        secondaryDeltas = new ObjectDeltaWaves<O>();
//    }

    /**
     * Returns true if there is any change in organization membership.
     * I.e. in case that there is a change in parentOrgRef.
     */
    public boolean hasOrganizationalChange() {
        return hasChangeInItem(SchemaConstants.PATH_PARENT_ORG_REF);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean hasChangeInItem(ItemPath itemPath) {
        if (isAdd()) {
            PrismObject<O> objectNew = getObjectNew();
            if (objectNew == null) {
                return false;
            }
            Item<PrismValue,ItemDefinition<?>> item = objectNew.findItem(itemPath);
            return item != null && !item.getValues().isEmpty();
        } else if (isDelete()) {
            // We do not care any more
            return false;
        } else {
            ObjectDelta<O> summaryDelta = getSummaryDelta();
            return summaryDelta != null && summaryDelta.hasItemDelta(itemPath);
        }
    }

    public LensFocusContext<O> clone(LensContext<O> lensContext) {
        LensFocusContext<O> clone = new LensFocusContext<>(state.clone(), lensContext);
        copyValues(clone);
        return clone;
    }

    private void copyValues(LensFocusContext<O> clone) {
        super.copyValues(clone);
        clone.deleted = deleted;
        clone.archetypePolicy = archetypePolicy;
        clone.archetypes = archetypes != null ? new ArrayList<>(archetypes) : null;
        clone.primaryDeltaExecuted = primaryDeltaExecuted;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getDebugDumpTitle());
        if (!isFresh()) {
            sb.append(", NOT FRESH");
        } else {
            sb.append(", fresh");
        }
        if (deleted) {
            sb.append(", DELETED");
        }
        sb.append(", oid=");
        sb.append(getOid());
        if (getIteration() != 0) {
            sb.append(", iteration=").append(getIteration()).append(" (").append(getIterationToken()).append(")");
        }
        sb.append("\n");

        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("old"), getObjectOld(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("current"), getObjectCurrent(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("new"), getObjectNew(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("deleted"), deleted, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("primary delta"), getPrimaryDelta(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("secondary delta"), getSecondaryDelta(), indent+1);
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append(getDebugDumpTitle("older secondary deltas")).append(":");
        ObjectDeltaWaves<O> secondaryDeltas = state.getArchivedSecondaryDeltas();
        if (secondaryDeltas.isEmpty()) {
            sb.append(" empty");
        } else {
            sb.append("\n");
            sb.append(secondaryDeltas.debugDump(indent + 2));
        }
        sb.append("\n");

        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("executed deltas"), getExecutedDeltas(), indent+1);
        DebugUtil.debugDumpWithLabel(sb, "Policy rules context", policyRulesContext, indent + 1);
        return sb.toString();
    }

    @Override
    protected String getElementDefaultDesc() {
        return "focus";
    }

    @Override
    public String toString() {
        return "LensFocusContext(" + getObjectTypeClass().getSimpleName() + ":" + getOid() + ")";
    }

    public String getHumanReadableName() {
        StringBuilder sb = new StringBuilder();
        sb.append("focus(");
        PrismObject<O> object = getObjectNew();
        if (object == null) {
            object = getObjectOld();
        }
        if (object == null) {
            sb.append(getOid());
        } else {
            sb.append(object);
        }
        sb.append(")");
        return sb.toString();
    }

    LensFocusContextType toBean(LensContext.ExportType exportType) throws SchemaException {
        LensFocusContextType rv = new LensFocusContextType();
        super.storeIntoBean(rv, exportType);
        if (exportType != LensContext.ExportType.MINIMAL) {
            rv.setSecondaryDeltas(state.getArchivedSecondaryDeltas().toObjectDeltaWavesBean());
        }
        return rv;
    }

    static <O extends ObjectType> LensFocusContext<O> fromLensFocusContextBean(
            LensFocusContextType focusContextType, LensContext<O> lensContext, Task task, OperationResult result)
            throws SchemaException {

        String objectTypeClassString = focusContextType.getObjectTypeClass();
        if (StringUtils.isEmpty(objectTypeClassString)) {
            throw new SystemException("Object type class is undefined in LensFocusContextType");
        }
        LensFocusContext<O> lensFocusContext;
        try {
            //noinspection unchecked
            lensFocusContext = new LensFocusContext<>((Class<O>) Class.forName(objectTypeClassString), lensContext);
        } catch (ClassNotFoundException e) {
            throw new SystemException("Couldn't instantiate LensFocusContext because object type class couldn't be found", e);
        }

        lensFocusContext.retrieveFromLensElementContextBean(focusContextType, task, result);
        ObjectDeltaWaves.fillObjectDeltaWaves(
                lensFocusContext.state.getArchivedSecondaryDeltas(),
                focusContextType.getSecondaryDeltas());

        // fixing provisioning type in delta (however, this is not usually needed, unless primary object is shadow or resource
        Objectable object;
        if (lensFocusContext.getObjectNew() != null) {
            object = lensFocusContext.getObjectNew().asObjectable();
        } else if (lensFocusContext.getObjectOld() != null) {
            object = lensFocusContext.getObjectOld().asObjectable();
        } else {
            object = null;
        }
        for (ObjectDelta<O> delta : lensFocusContext.state.getArchivedSecondaryDeltas()) {
            if (delta != null) {
                lensFocusContext.applyProvisioningDefinition(delta, object, task, result);
            }
        }

        return lensFocusContext;
    }

    @Override
    public void checkConsistence(String desc) {
        state.checkConsistence(this, desc);

        // all executed deltas should have the same oid (if any)
        String oid = null;
        for (LensObjectDeltaOperation<?> operation : getExecutedDeltas()) {
            String oid1 = operation.getObjectDelta().getOid();
            if (oid == null) {
                if (oid1 != null) {
                    oid = oid1;
                }
            } else {
                if (oid1 != null && !oid.equals(oid1)) {
                    String m = "Different OIDs in focus executed deltas: " + oid + ", " + oid1;
                    LOGGER.error("{}: context = \n{}", m, this.debugDump());
                    throw new IllegalStateException(m);
                }
            }
        }
    }

    @Override
    void doExtraObjectConsistenceCheck(@NotNull PrismObject<O> object, String elementDesc, String contextDesc) {
    }

    public void setItemDefinitionsMap(@NotNull PathKeyedMap<ObjectTemplateItemDefinitionType> itemDefinitionsMap) {
        this.itemDefinitionsMap = itemDefinitionsMap;
    }

    @NotNull
    public PathKeyedMap<ObjectTemplateItemDefinitionType> getItemDefinitionsMap() {
        return itemDefinitionsMap;
    }

    // preliminary implementation
    public LinkTypeDefinitionType getSourceLinkTypeDefinition(
            @NotNull String linkTypeName, LinkManager linkManager, OperationResult result)
            throws SchemaException, ConfigurationException {
        return linkManager.getSourceLinkTypeDefinition(
                linkTypeName,
                Arrays.asList(getObjectNew(), getObjectCurrent(), getObjectOld()),
                result);
    }

    // preliminary implementation
    public LinkTypeDefinitionType getTargetLinkTypeDefinition(
            @NotNull String linkTypeName, LinkManager linkManager, OperationResult result)
            throws SchemaException, ConfigurationException {
        return linkManager.getTargetLinkTypeDefinition(
                linkTypeName,
                Arrays.asList(getObjectNew(), getObjectCurrent(), getObjectOld()),
                result);
    }

    /**
     * Updates the state to reflect that a delta was executed.
     *
     * CURRENTLY CALLED ONLY FOR FOCUS. ASSUMES SUCCESSFUL EXECUTION.
     */
    void updateAfterExecution() throws SchemaException {
        state.updateAfterExecution(lensContext.getTaskExecutionMode(), lensContext.getExecutionWave());
    }

    boolean primaryItemDeltaExists(ItemPath path) {
        ObjectDelta<O> primaryDelta = getPrimaryDelta();
        return primaryDelta != null &&
                !ItemDelta.isEmpty(primaryDelta.findItemDelta(path));
    }

    public void deleteEmptyPrimaryDelta() {
        state.deleteEmptyPrimaryDelta();
    }

    public @NotNull LensContext<O> getLensContext() {
        //noinspection unchecked
        return (LensContext<O>) lensContext;
    }
}
