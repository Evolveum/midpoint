/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.function.Consumer;

import com.evolveum.midpoint.model.common.LinkManager;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentSpec;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class LensFocusContext<O extends ObjectType> extends LensElementContext<O> {

    private static final Trace LOGGER = TraceManager.getTrace(LensFocusContext.class);

    private ObjectDeltaWaves<O> secondaryDeltas = new ObjectDeltaWaves<>();

    private boolean primaryDeltaConsolidated;

    transient private ArchetypePolicyType archetypePolicyType;
    transient private ArchetypeType archetype;

    // extracted from the template(s)
    // this is not to be serialized into XML, but let's not mark it as transient
    @NotNull private Map<UniformItemPath, ObjectTemplateItemDefinitionType> itemDefinitionsMap = new HashMap<>();

    public LensFocusContext(Class<O> objectTypeClass, LensContext<O> lensContext) {
        super(objectTypeClass, lensContext);
    }

    private int getProjectionWave() {
        return getLensContext().getProjectionWave();
    }

    public ArchetypePolicyType getArchetypePolicyType() {
        return archetypePolicyType;
    }

    public void setArchetypePolicyType(ArchetypePolicyType objectPolicyConfigurationType) {
        this.archetypePolicyType = objectPolicyConfigurationType;
    }

    public ArchetypeType getArchetype() {
        return archetype;
    }

    public void setArchetype(ArchetypeType archetype) {
        this.archetype = archetype;
    }

    public LifecycleStateModelType getLifecycleModel() {
        if (archetypePolicyType == null) {
            return null;
        }
        return archetypePolicyType.getLifecycleStateModel();
    }

    @Override
    public void setOid(String oid) {
        super.setOid(oid);
        secondaryDeltas.setOid(oid);
    }

    public ObjectDelta<O> getProjectionWavePrimaryDelta() throws SchemaException {
        if (getProjectionWave() == 0) {
            return getFixedPrimaryDelta();
        } else {
            return secondaryDeltas.getMergedDeltas(getFixedPrimaryDelta(), getProjectionWave());
        }
    }

    public boolean isDelete() {
        return ObjectDelta.isDelete(getPrimaryDelta());
    }

    boolean isSecondaryDelete() throws SchemaException {
        ObjectDelta<?> userSecondaryDelta = getProjectionWaveSecondaryDelta();
        return userSecondaryDelta != null && ChangeType.DELETE.equals(userSecondaryDelta.getChangeType());
    }

    public boolean isAdd() {
        return ObjectDelta.isAdd(getPrimaryDelta());
    }

    @Override
    public ObjectDelta<O> getSecondaryDelta() {
        try {
            return secondaryDeltas.getMergedDeltas();
        } catch (SchemaException e) {
            // This should not happen
            throw new SystemException("Unexpected delta merging problem: "+e.getMessage(), e);
        }
    }

    public ObjectDelta<O> getSecondaryDelta(int wave) {
        return secondaryDeltas.get(wave);
    }

    public ObjectDeltaWaves<O> getSecondaryDeltas() {
        return secondaryDeltas;
    }

    @Override
    public Collection<ObjectDelta<O>> getAllDeltas() {
        List<ObjectDelta<O>> deltas = new ArrayList<>(secondaryDeltas.size() + 1);
        ObjectDelta<O> primaryDelta = getPrimaryDelta();
        if (primaryDelta != null) {
            deltas.add(primaryDelta);
        }
        for (ObjectDelta<O> secondaryDelta : secondaryDeltas) {
            if (secondaryDelta != null) {
                deltas.add(secondaryDelta);
            }
        }
        return deltas;
    }

    public ObjectDelta<O> getProjectionWaveSecondaryDelta() throws SchemaException {
        return getWaveSecondaryDelta(getProjectionWave());
    }

    public ObjectDelta<O> getWaveSecondaryDelta(int wave) throws SchemaException {
        return secondaryDeltas.get(wave);
    }

    // preliminary implementation - deletes all secondary deltas
    @Override
    public void deleteSecondaryDeltas() {
        secondaryDeltas.deleteDeltas();
    }

    @Override
    public ObjectDeltaObject<O> getObjectDeltaObject() throws SchemaException {
        return new ObjectDeltaObject<>(getObjectOld(), getDelta(), getObjectNew(), getObjectDefinition());
    }

    @Override
    public void setSecondaryDelta(ObjectDelta<O> secondaryDelta) {
        throw new UnsupportedOperationException("Cannot set secondary delta to focus without a wave number");
    }

    public void setSecondaryDelta(ObjectDelta<O> secondaryDelta, int wave) {
        this.secondaryDeltas.set(wave, secondaryDelta);
    }

    public void setProjectionWaveSecondaryDelta(ObjectDelta<O> secondaryDelta) {
        this.secondaryDeltas.set(getProjectionWave(), secondaryDelta);
    }

    public void swallowToProjectionWaveSecondaryDelta(ItemDelta<?,?> propDelta) throws SchemaException {

        ObjectDelta<O> secondaryDelta = getProjectionWaveSecondaryDelta();

        if (secondaryDelta == null) {
            secondaryDelta = getPrismContext().deltaFactory().object().create(getObjectTypeClass(), ChangeType.MODIFY);
            secondaryDelta.setOid(getOid());
            setProjectionWaveSecondaryDelta(secondaryDelta);
        } else if (secondaryDelta.containsModification(propDelta, EquivalenceStrategy.LITERAL_IGNORE_METADATA)) {   // todo why literal?
            return;
        }

        secondaryDelta.swallow(propDelta);
    }

    // This method may be useful for hooks. E.g. if a hook wants to insert a special secondary delta to avoid
    // splitting the changes to several audit records. It is not entirely clean and we should think about a better
    // solution in the future. But it is good enough for now.
    @SuppressWarnings("unused")
    public void swallowToWave0SecondaryDelta(ItemDelta<?,?> propDelta) throws SchemaException {
          ObjectDelta<O> secondaryDelta = getSecondaryDelta(0);
          if (secondaryDelta == null) {
            secondaryDelta = getPrismContext().deltaFactory().object().create(getObjectTypeClass(), ChangeType.MODIFY);
            secondaryDelta.setOid(getOid());
            setSecondaryDelta(secondaryDelta, 0);
        } else if (secondaryDelta.containsModification(propDelta, EquivalenceStrategy.LITERAL_IGNORE_METADATA)) {   // todo why literal?
              return;
          }

        secondaryDelta.swallow(propDelta);
    }

    @Override
    public void swallowToSecondaryDelta(ItemDelta<?, ?> itemDelta) throws SchemaException {
        swallowToProjectionWaveSecondaryDelta(itemDelta);
    }

    public boolean alreadyHasDelta(ItemDelta<?,?> itemDelta) {
        ObjectDelta<O> primaryDelta = getPrimaryDelta();
        if (primaryDelta != null && primaryDelta.containsModification(itemDelta, EquivalenceStrategy.LITERAL_IGNORE_METADATA)) {    // todo why literal?
            return true;
        }
        if (secondaryDeltas != null) {
            for (ObjectDelta<O> waveSecondaryDelta: secondaryDeltas) {
                if (waveSecondaryDelta != null && waveSecondaryDelta.containsModification(itemDelta, EquivalenceStrategy.LITERAL_IGNORE_METADATA)) {    // todo why literal?
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasAnyDelta() {
        if (getPrimaryDelta() != null && !getPrimaryDelta().isEmpty()) {
            return true;
        }
        if (secondaryDeltas != null) {
            for (ObjectDelta<O> waveSecondaryDelta: secondaryDeltas) {
                if (waveSecondaryDelta != null && !waveSecondaryDelta.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns user delta, both primary and secondary (merged together) for a current wave.
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     */
    public ObjectDelta<O> getProjectionWaveDelta() throws SchemaException {
        return getWaveDelta(getProjectionWave());
    }

    public ObjectDelta<O> getWaveDelta(int wave) throws SchemaException {
        if (wave == 0) {
            // Primary delta is executed only in the first wave (wave 0)
            return ObjectDeltaCollectionsUtil.union(getFixedPrimaryDelta(), getWaveSecondaryDelta(wave));
        } else {
            return getWaveSecondaryDelta(wave);
        }
    }

    // HIGHLY EXPERIMENTAL
    public ObjectDelta<O> getAggregatedWaveDelta(int wave) throws SchemaException {
        ObjectDelta<O> result = null;
        for (int w = 0; w <= wave; w++) {
            ObjectDelta<O> delta = getWaveDelta(w);
            if (delta == null) {
                continue;
            }
            if (result == null) {
                result = delta.clone();
            } else {
                result.merge(delta);
            }
        }
        LOGGER.trace("Aggregated wave delta for wave {} = {}", wave, result != null ? result.debugDump() : "(null)");
        return result;
    }

    public ObjectDelta<O> getWaveExecutableDelta(int wave) throws SchemaException {
        if (wave == 0) {
            if (getFixedPrimaryDelta() != null && getFixedPrimaryDelta().isAdd()) {
                ObjectDelta delta = getFixedPrimaryDelta().clone();
                for (ObjectDelta<O> secondary : getSecondaryDeltas()) {
                    if (secondary != null) {
                        secondary.applyTo(delta.getObjectToAdd());
                    }
                }
                return delta;
            }
        }
        return getWaveDelta(wave);
    }

    @Override
    public void cleanup() {
        // Clean up only delta in current wave. The deltas in previous waves are already done.
        // FIXME: this somehow breaks things. don't know why. but don't really care. the waves will be gone soon anyway
//        if (secondaryDeltas.get(getWave()) != null) {
//            secondaryDeltas.remove(getWave());
//        }
    }


    @Override
    public void normalize() {
        super.normalize();
        if (secondaryDeltas != null) {
            secondaryDeltas.normalize();
        }
    }

//    @Override
//    public void reset() {
//        super.reset();
//        secondaryDeltas = new ObjectDeltaWaves<O>();
//    }

    @Override
    public void adopt(PrismContext prismContext) throws SchemaException {
        super.adopt(prismContext);
        if (secondaryDeltas != null) {
            secondaryDeltas.adopt(prismContext);
        }
    }

    public void clearIntermediateResults() {
        // Nothing to do
    }

    public void applyProjectionWaveSecondaryDeltas(Collection<ItemDelta<?,?>> itemDeltas) throws SchemaException {
        ObjectDelta<O> wavePrimaryDelta = getProjectionWavePrimaryDelta();
        ObjectDelta<O> waveSecondaryDelta = getProjectionWaveSecondaryDelta();
        for (ItemDelta<?,?> itemDelta: itemDeltas) {
            if (itemDelta != null && !itemDelta.isEmpty()) {
                if (wavePrimaryDelta == null || !wavePrimaryDelta.containsModification(itemDelta)) {
                    if (waveSecondaryDelta == null) {
                        waveSecondaryDelta = getPrismContext().deltaFactory().object().create(getObjectTypeClass(), ChangeType.MODIFY);
                        if (getObjectNew() != null && getObjectNew().getOid() != null){
                            waveSecondaryDelta.setOid(getObjectNew().getOid());
                        }
                        setProjectionWaveSecondaryDelta(waveSecondaryDelta);
                    }
                    waveSecondaryDelta.mergeModification(itemDelta);
                }
            }
        }
    }

    /**
     * Returns true if there is any change in organization membership.
     * I.e. in case that there is a change in parentOrgRef.
     */
    public boolean hasOrganizationalChange() {
        return hasChangeInItem(SchemaConstants.PATH_PARENT_ORG_REF);
    }

    public boolean hasChangeInItem(ItemPath itemPath) {
        if (isAdd()) {
            PrismObject<O> objectNew = getObjectNew();
            if (objectNew == null) {
                return false;
            }
            Item<PrismValue,ItemDefinition> item = objectNew.findItem(itemPath);
            if (item == null) {
                return false;
            }
            List<PrismValue> values = item.getValues();
            if (values == null || values.isEmpty()) {
                return false;
            }
            return true;
        } else if (isDelete()) {
            // We do not care any more
            return false;
        } else {
            for (ObjectDelta<O> delta : getAllDeltas()) {
                if (delta.hasItemDelta(itemPath)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public LensFocusContext<O> clone(LensContext lensContext) {
        LensFocusContext<O> clone = new LensFocusContext<O>(getObjectTypeClass(), lensContext);
        copyValues(clone, lensContext);
        return clone;
    }

    protected void copyValues(LensFocusContext<O> clone, LensContext lensContext) {
        super.copyValues(clone, lensContext);
        if (this.secondaryDeltas != null) {
            clone.secondaryDeltas = this.secondaryDeltas.clone();
        }
    }

    public String dump(boolean showTriples) {
        return debugDump(0, showTriples);
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, true);
    }

    public String debugDump(int indent, boolean showTriples) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getDebugDumpTitle());
        if (!isFresh()) {
            sb.append(", NOT FRESH");
        }
        sb.append(", oid=");
        sb.append(getOid());
        if (getIteration() != 0) {
            sb.append(", iteration=").append(getIteration()).append(" (").append(getIterationToken()).append(")");
        }
        sb.append(", syncIntent=").append(getSynchronizationIntent());

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("old"), getObjectOld(), indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("current"), getObjectCurrent(), indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("new"), getObjectNew(), indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("primary delta"), getPrimaryDelta(), indent+1);

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append(getDebugDumpTitle("secondary delta")).append(":");
        if (secondaryDeltas.isEmpty()) {
            sb.append(" empty");
        } else {
            sb.append("\n");
            sb.append(secondaryDeltas.debugDump(indent + 2));
        }

        // pending policy state modifications (object + assignments)
        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append(getDebugDumpTitle("pending object policy state modifications")).append(":");
        if (getPendingObjectPolicyStateModifications().isEmpty()) {
            sb.append(" empty");
        } else {
            sb.append("\n");
            sb.append(DebugUtil.debugDump(getPendingObjectPolicyStateModifications(), indent + 2));
        }

        for (Map.Entry<AssignmentSpec, List<ItemDelta<?, ?>>> entry : getPendingAssignmentPolicyStateModifications().entrySet()) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append(getDebugDumpTitle("pending assignment policy state modifications for ")).append(entry.getKey()).append(":");
            if (entry.getValue().isEmpty()) {
                sb.append(" empty");
            } else {
                sb.append("\n");
                sb.append(DebugUtil.debugDump(entry.getValue(), indent + 2));
            }
        }

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("executed deltas"), getExecutedDeltas(), indent+1);

        LensContext.dumpRules(sb, getDebugDumpTitle("policy rules"), indent+1, getPolicyRules());
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
            sb.append(object.toString());
        }
        sb.append(")");
        return sb.toString();
    }

    public LensFocusContextType toLensFocusContextType(PrismContext prismContext, LensContext.ExportType exportType) throws SchemaException {
        LensFocusContextType rv = new LensFocusContextType(prismContext);
        super.storeIntoLensElementContextType(rv, exportType);
        if (exportType != LensContext.ExportType.MINIMAL) {
            rv.setSecondaryDeltas(secondaryDeltas.toObjectDeltaWavesType());
        }
        return rv;
    }

    public static LensFocusContext fromLensFocusContextType(LensFocusContextType focusContextType, LensContext lensContext, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

        String objectTypeClassString = focusContextType.getObjectTypeClass();
        if (StringUtils.isEmpty(objectTypeClassString)) {
            throw new SystemException("Object type class is undefined in LensFocusContextType");
        }
        LensFocusContext lensFocusContext;
        try {
            lensFocusContext = new LensFocusContext(Class.forName(objectTypeClassString), lensContext);
        } catch (ClassNotFoundException e) {
            throw new SystemException("Couldn't instantiate LensFocusContext because object type class couldn't be found", e);
        }

        lensFocusContext.retrieveFromLensElementContextType(focusContextType, task, result);
        lensFocusContext.secondaryDeltas = ObjectDeltaWaves.fromObjectDeltaWavesType(focusContextType.getSecondaryDeltas(), lensContext.getPrismContext());

        // fixing provisioning type in delta (however, this is not usually needed, unless primary object is shadow or resource
        Objectable object;
        if (lensFocusContext.getObjectNew() != null) {
            object = lensFocusContext.getObjectNew().asObjectable();
        } else if (lensFocusContext.getObjectOld() != null) {
            object = lensFocusContext.getObjectOld().asObjectable();
        } else {
            object = null;
        }
        for (Object o : lensFocusContext.secondaryDeltas) {
            ObjectDelta<? extends ObjectType> delta = (ObjectDelta<? extends ObjectType>) o;
            if (delta != null) {
                lensFocusContext.fixProvisioningTypeInDelta(delta, object, task, result);
            }
        }

        return lensFocusContext;
    }

    @Override
    public void checkEncrypted() {
        super.checkEncrypted();
        secondaryDeltas.checkEncrypted("secondary delta");
    }

    @Override
    public void checkConsistence(String desc) {
        super.checkConsistence(desc);

        // all executed deltas should have the same oid (if any)
        String oid = null;
        for (LensObjectDeltaOperation operation : getExecutedDeltas()) {
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

    public void setItemDefinitionsMap(@NotNull Map<UniformItemPath, ObjectTemplateItemDefinitionType> itemDefinitionsMap) {
        this.itemDefinitionsMap = itemDefinitionsMap;
    }

    @NotNull
    public Map<UniformItemPath, ObjectTemplateItemDefinitionType> getItemDefinitionsMap() {
        return itemDefinitionsMap;
    }

    @Override
    public void forEachDelta(Consumer<ObjectDelta<O>> consumer) {
        super.forEachDelta(consumer);
        for (ObjectDelta<O> secondaryDelta : secondaryDeltas) {
            consumer.accept(secondaryDelta);
        }
    }

    // preliminary implementation
    public LinkTypeDefinitionType getSourceLinkTypeDefinition(@NotNull String linkTypeName, LinkManager linkManager,
            OperationResult result) throws SchemaException, ConfigurationException {
        PrismObject<O> objectAny = getObjectAny();
        return objectAny != null ? linkManager.getSourceLinkTypeDefinition(linkTypeName, objectAny, result) : null;
    }

    // preliminary implementation
    public LinkTypeDefinitionType getTargetLinkTypeDefinition(@NotNull String linkTypeName, LinkManager linkManager,
            OperationResult result) throws SchemaException, ConfigurationException {
        PrismObject<O> objectAny = getObjectAny();
        return objectAny != null ? linkManager.getTargetLinkTypeDefinition(linkTypeName, objectAny, result) : null;
    }

    public boolean isPrimaryDeltaConsolidated() {
        return primaryDeltaConsolidated;
    }

    public void setPrimaryDeltaConsolidated(boolean primaryDeltaConsolidated) {
        this.primaryDeltaConsolidated = primaryDeltaConsolidated;
    }
}
