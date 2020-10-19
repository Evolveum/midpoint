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
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
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

import org.apache.commons.collections4.CollectionUtils;
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

    private transient ArchetypePolicyType archetypePolicyType;

    private transient ArchetypeType archetype;

    private boolean primaryDeltaExecuted;

    // extracted from the template(s)
    // this is not to be serialized into XML, but let's not mark it as transient
    @NotNull private PathKeyedMap<ObjectTemplateItemDefinitionType> itemDefinitionsMap = new PathKeyedMap<>();

    public LensFocusContext(Class<O> objectTypeClass, LensContext<O> lensContext) {
        super(objectTypeClass, lensContext);
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

    public boolean isDelete() {
        return ObjectDelta.isDelete(primaryDelta);
    }

    public boolean isAdd() {
        return ObjectDelta.isAdd(primaryDelta);
    }

    @Override
    public ObjectDelta<O> getSummaryDelta() {
        try {
            List<ObjectDelta<O>> allDeltas = new ArrayList<>();
            CollectionUtils.addIgnoreNull(allDeltas, primaryDelta);
            addSecondaryDeltas(allDeltas);
            return ObjectDeltaCollectionsUtil.summarize(allDeltas);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception while merging deltas: " + e.getMessage(), e);
        }
    }

    @Override
    public ObjectDelta<O> getSummarySecondaryDelta() {
        try {
            List<ObjectDelta<O>> allSecondaryDeltas = new ArrayList<>();
            addSecondaryDeltas(allSecondaryDeltas);
            return ObjectDeltaCollectionsUtil.summarize(allSecondaryDeltas);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception while merging secondary deltas: " + e.getMessage(), e);
        }
    }

    private void addSecondaryDeltas(List<ObjectDelta<O>> allDeltas) {
        for (ObjectDelta<O> archivedSecondaryDelta : secondaryDeltas) {
            CollectionUtils.addIgnoreNull(allDeltas, archivedSecondaryDelta);
        }
        CollectionUtils.addIgnoreNull(allDeltas, secondaryDelta);
    }

    /**
     * Returns object-delta-object structure based on the current state.
     * I.e. objectCurrent - currentDelta - objectNew.
     */
    @NotNull
    public ObjectDeltaObject<O> getObjectDeltaObjectRelative() {
        return new ObjectDeltaObject<>(objectCurrent, getCurrentDelta(), objectNew, getObjectDefinition());
    }

    @NotNull
    public ObjectDeltaObject<O> getObjectDeltaObjectAbsolute() {
        return new ObjectDeltaObject<>(objectOld, getSummaryDelta(), objectNew, getObjectDefinition());
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

    public void resetDeltas(ObjectDelta<O> newSecondaryDelta) throws SchemaException {
        secondaryDelta = newSecondaryDelta;
        recompute();
    }

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
            Item<PrismValue,ItemDefinition> item = objectNew.findItem(itemPath);
            return item != null && !item.getValues().isEmpty();
        } else if (isDelete()) {
            // We do not care any more
            return false;
        } else {
            ObjectDelta<O> summaryDelta = getSummaryDelta();
            return summaryDelta != null && summaryDelta.hasItemDelta(itemPath);
        }
    }

    @Override
    public LensFocusContext<O> clone(LensContext lensContext) {
        //noinspection unchecked
        LensFocusContext<O> clone = new LensFocusContext<O>(getObjectTypeClass(), lensContext);
        copyValues(clone);
        return clone;
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
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("old"), objectOld, indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("current"), objectCurrent, indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("new"), objectNew, indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("primary delta"), primaryDelta, indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("secondary delta"), secondaryDelta, indent+1);

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append(getDebugDumpTitle("older secondary deltas")).append(":");
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

    LensFocusContextType toLensFocusContextType(PrismContext prismContext, LensContext.ExportType exportType) throws SchemaException {
        LensFocusContextType rv = new LensFocusContextType(prismContext);
        super.storeIntoLensElementContextType(rv, exportType);
        if (exportType != LensContext.ExportType.MINIMAL) {
            rv.setSecondaryDeltas(secondaryDeltas.toObjectDeltaWavesType());
        }
        return rv;
    }

    static LensFocusContext fromLensFocusContextType(LensFocusContextType focusContextType, LensContext lensContext, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

        String objectTypeClassString = focusContextType.getObjectTypeClass();
        if (StringUtils.isEmpty(objectTypeClassString)) {
            throw new SystemException("Object type class is undefined in LensFocusContextType");
        }
        LensFocusContext lensFocusContext;
        try {
            //noinspection unchecked
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
            //noinspection unchecked
            ObjectDelta<? extends ObjectType> delta = (ObjectDelta<? extends ObjectType>) o;
            if (delta != null) {
                //noinspection unchecked
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

    public void setItemDefinitionsMap(@NotNull PathKeyedMap<ObjectTemplateItemDefinitionType> itemDefinitionsMap) {
        this.itemDefinitionsMap = itemDefinitionsMap;
    }

    @NotNull
    public PathKeyedMap<ObjectTemplateItemDefinitionType> getItemDefinitionsMap() {
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

    @Override
    public void normalize() {
        super.normalize();
        secondaryDeltas.normalize();
    }

    @Override
    public void adopt(PrismContext prismContext) throws SchemaException {
        super.adopt(prismContext);
        secondaryDeltas.adopt(prismContext);
    }

    @Override
    boolean doesPrimaryDeltaApply() {
        // It is not sufficient to consider getExecutionWave() == 0 because the wave 0 can
        // be restarted in case of provisioning conflict.
        return !primaryDeltaExecuted;
    }

    ObjectDeltaWaves<O> getSecondaryDeltas() {
        return secondaryDeltas;
    }

    void resetDeltasAfterExecution() {
        secondaryDeltas.add(getLensContext().getExecutionWave(), secondaryDelta);
        secondaryDelta = null;
        primaryDeltaExecuted = true;
    }

    @Override
    void deleteSecondaryDeltas() {
        super.deleteSecondaryDeltas();
        secondaryDeltas.clear();
    }

    boolean primaryItemDeltaExists(ItemPath path) {
        return primaryDelta != null && !ItemDelta.isEmpty(primaryDelta.findItemDelta(path));
    }

    public boolean isPrimaryDeltaExecuted() {
        return primaryDeltaExecuted;
    }

    public void setPrimaryDeltaExecuted(boolean primaryDeltaExecuted) {
        this.primaryDeltaExecuted = primaryDeltaExecuted;
    }
}
