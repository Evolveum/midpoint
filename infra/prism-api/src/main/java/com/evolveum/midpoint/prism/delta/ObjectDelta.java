/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Relative difference (delta) of the object.
 * <p>
 * This class describes how the object changes. It can describe either object addition, modification of deletion.
 * <p>
 * Addition described complete new (absolute) state of the object.
 * <p>
 * Modification contains a set property deltas that describe relative changes to individual properties
 * <p>
 * Deletion does not contain anything. It only marks object for deletion.
 * <p>
 * The OID is mandatory for modification and deletion.
 *
 * @author Radovan Semancik
 * @see PropertyDelta
 */
public interface ObjectDelta<O extends Objectable> extends DebugDumpable, PrismContextSensitive, Visitable, PathVisitable, Serializable, Freezable {

    void accept(Visitor visitor, boolean includeOldValues);

    @Override
    void accept(Visitor visitor, ItemPath path, boolean recursive);

    ChangeType getChangeType();

    void setChangeType(ChangeType changeType);

    static boolean isAdd(ObjectDelta<?> objectDelta) {
        return objectDelta != null && objectDelta.isAdd();
    }

    boolean isAdd();

    static boolean isDelete(ObjectDelta<?> objectDelta) {
        return objectDelta != null && objectDelta.isDelete();
    }

    boolean isDelete();

    static boolean isModify(ObjectDelta<?> objectDelta) {
        return objectDelta != null && objectDelta.isModify();
    }

    boolean isModify();

    String getOid();

    void setOid(String oid);

    void setPrismContext(PrismContext prismContext);

    PrismObject<O> getObjectToAdd();

    void setObjectToAdd(PrismObject<O> objectToAdd);

    @NotNull
    Collection<? extends ItemDelta<?,?>> getModifications();

    /**
     * Adds modification (itemDelta) and returns the modification that was added.
     * NOTE: the modification that was added may be different from the modification that was
     * passed into this method! E.g. in case if two modifications must be merged to keep the delta
     * consistent. Therefore always use the returned modification after this method is invoked.
     */
    <D extends ItemDelta> D addModification(D itemDelta);

    boolean containsModification(ItemDelta itemDelta);

    boolean containsModification(ItemDelta itemDelta, EquivalenceStrategy strategy);

    boolean containsAllModifications(Collection<? extends ItemDelta<?, ?>> itemDeltas, EquivalenceStrategy strategy);

    void addModifications(Collection<? extends ItemDelta> itemDeltas);

    void addModifications(ItemDelta<?, ?>... itemDeltas);

    /**
     * TODO specify this method!
     *
     * An attempt:
     *
     * Given this ADD or MODIFY object delta OD, finds an item delta ID such that "ID has the same effect on an item specified
     * by itemPath as OD" (simply said).
     *
     * More precisely,
     * - if OD is ADD delta: ID is ADD delta that adds values of the item present in the object being added
     * - if OD is MODIFY delta: ID is such delta that:
     *      1. Given ANY object O, let O' be O after application of OD.
     *      2. Let I be O(itemPath), I' be O'(itemPath).
     *      3. Then I' is the same as I after application of ID.
     *   ID is null if no such item delta exists - or cannot be found easily.
     *
     * Problem:
     * - If OD contains more than one modification that affects itemPath the results from findItemDelta can be differ
     *   from the above definition.
     */
    <IV extends PrismValue,ID extends ItemDefinition> ItemDelta<IV,ID> findItemDelta(ItemPath itemPath);

    <IV extends PrismValue,ID extends ItemDefinition> ItemDelta<IV,ID> findItemDelta(ItemPath itemPath, boolean strict);

    <IV extends PrismValue,ID extends ItemDefinition, I extends Item<IV,ID>,DD extends ItemDelta<IV,ID>>
            DD findItemDelta(ItemPath itemPath, Class<DD> deltaType, Class<I> itemType, boolean strict);

    <IV extends PrismValue,ID extends ItemDefinition> Collection<PartiallyResolvedDelta<IV,ID>> findPartial(
            ItemPath propertyPath);

    boolean hasItemDelta(ItemPath propertyPath);

    boolean hasItemOrSubitemDelta(ItemPath propertyPath);

    boolean hasCompleteDefinition();

    Class<O> getObjectTypeClass();

    void setObjectTypeClass(Class<O> objectTypeClass);

    /**
     * Top-level path is assumed.
     */
    <X> PropertyDelta<X> findPropertyDelta(ItemPath parentPath, QName propertyName);

    <X> PropertyDelta<X> findPropertyDelta(ItemPath propertyPath);

    <X extends Containerable> ContainerDelta<X> findContainerDelta(ItemPath propertyPath);

    ReferenceDelta findReferenceModification(ItemPath itemPath);

    /**
     * Returns all item deltas at or below a specified path.
     */
    Collection<? extends ItemDelta<?,?>> findItemDeltasSubPath(ItemPath itemPath);

    <D extends ItemDelta> void removeModification(ItemDelta<?, ?> itemDelta);

    void removeReferenceModification(ItemPath itemPath);

    void removeContainerModification(ItemPath itemName);

    void removePropertyModification(ItemPath itemPath);

    boolean isEmpty();

    void normalize();

    ObjectDelta<O> narrow(PrismObject<O> existingObject, @NotNull ParameterizedEquivalenceStrategy strategy, boolean assumeMissingItems);

    // TODO better name
    void applyDefinitionIfPresent(PrismObjectDefinition<O> definition, boolean tolerateNoDefinition) throws SchemaException;

    /**
     * Deep clone.
     */
    ObjectDelta<O> clone();

    /**
     * Merge provided delta into this delta.
     * This delta is assumed to be chronologically earlier, delta in the parameter is assumed to come chronologicaly later.
     */
    void merge(ObjectDelta<O> deltaToMerge) throws SchemaException;

    void mergeModifications(Collection<? extends ItemDelta> modificationsToMerge) throws SchemaException;

    void mergeModification(ItemDelta<?, ?> modificationToMerge) throws SchemaException;

    /**
     * Applies this object delta to specified object, returns updated object.
     * It modifies the provided object.
     */
    void applyTo(PrismObject<O> targetObject) throws SchemaException;

    void applyTo(PrismObject<O> targetObject, ParameterizedEquivalenceStrategy strategy) throws SchemaException;

    /**
     * Applies this object delta to specified object, returns updated object.
     * It leaves the original object unchanged.
     *
     * @param objectOld object before change
     * @return object with applied changes or null if the object should not exit (was deleted)
     */
    PrismObject<O> computeChangedObject(PrismObject<O> objectOld) throws SchemaException;

    /**
     * Incorporates the property delta into the existing property deltas
     * (regardless of the change type).
     */
    void swallow(ItemDelta<?, ?> newItemDelta) throws SchemaException;

    void swallow(List<ItemDelta<?, ?>> itemDeltas) throws SchemaException;

    <X> PropertyDelta<X> createPropertyModification(ItemPath path);

    <C> PropertyDelta<C> createPropertyModification(ItemPath path, PrismPropertyDefinition propertyDefinition);

    ReferenceDelta createReferenceModification(ItemPath path, PrismReferenceDefinition referenceDefinition);

    <C extends Containerable> ContainerDelta<C> createContainerModification(ItemPath path);

    <C extends Containerable> ContainerDelta<C> createContainerModification(ItemPath path,
            PrismContainerDefinition<C> containerDefinition);

    <X> PropertyDelta<X> addModificationReplaceProperty(ItemPath propertyPath, X... propertyValues);

    <X> void addModificationAddProperty(ItemPath propertyPath, X... propertyValues);

    <X> void addModificationDeleteProperty(ItemPath propertyPath, X... propertyValues);

    <C extends Containerable> void addModificationAddContainer(ItemPath propertyPath, C... containerables) throws SchemaException;

    <C extends Containerable> void addModificationAddContainer(ItemPath propertyPath, PrismContainerValue<C>... containerValues);

    <C extends Containerable> void addModificationDeleteContainer(ItemPath propertyPath, C... containerables) throws SchemaException;

    <C extends Containerable> void addModificationDeleteContainer(ItemPath propertyPath,
            PrismContainerValue<C>... containerValues);

    <C extends Containerable> void addModificationReplaceContainer(ItemPath propertyPath,
            PrismContainerValue<C>... containerValues);

    void addModificationAddReference(ItemPath path, PrismReferenceValue... refValues);

    void addModificationDeleteReference(ItemPath path, PrismReferenceValue... refValues);

    void addModificationReplaceReference(ItemPath path, PrismReferenceValue... refValues);

    ReferenceDelta createReferenceModification(ItemPath refPath);

    ObjectDelta<O> createReverseDelta() throws SchemaException;

    void checkConsistence();

    void checkConsistence(ConsistencyCheckScope scope);

    void checkConsistence(boolean requireOid, boolean requireDefinition, boolean prohibitRaw);

    void checkConsistence(boolean requireOid, boolean requireDefinition, boolean prohibitRaw, ConsistencyCheckScope scope);

    void assertDefinitions() throws SchemaException;

    void assertDefinitions(String sourceDescription) throws SchemaException;

    void assertDefinitions(boolean tolerateRawElements) throws SchemaException;

    /**
     * Assert that all the items has appropriate definition.
     */
    void assertDefinitions(boolean tolerateRawElements, String sourceDescription) throws SchemaException;

    void revive(PrismContext prismContext) throws SchemaException;

    void applyDefinition(PrismObjectDefinition<O> objectDefinition, boolean force) throws SchemaException;

    boolean equivalent(ObjectDelta other);

    /**
     * Returns short string identification of object type. It should be in a form
     * suitable for log messages. There is no requirement for the type name to be unique,
     * but it rather has to be compact. E.g. short element names are preferred to long
     * QNames or URIs.
     * @return
     */
    String toDebugType();

    static boolean isEmpty(ObjectDelta delta) {
        return delta == null || delta.isEmpty();
    }

    /**
     * Returns modifications that are related to the given paths; removes them from the original delta.
     * Applicable only to modify deltas.
     * Currently compares paths by "equals" predicate -- in the future we might want to treat sub/super/equivalent paths!
     * So consider this method highly experimental.
     */
    ObjectDelta<O> subtract(@NotNull Collection<ItemPath> paths);

    boolean isRedundant(PrismObject<O> object, @NotNull ParameterizedEquivalenceStrategy strategy, boolean assumeMissingItems) throws SchemaException;

    @Experimental
    void removeOperationalItems();

    @Experimental
    void removeEstimatedOldValues();

    class FactorOutResultMulti<T extends Objectable> {
        public final ObjectDelta<T> remainder;
        public final List<ObjectDelta<T>> offsprings = new ArrayList<>();

        public FactorOutResultMulti(ObjectDelta<T> remainder) {
            this.remainder = remainder;
        }
    }

    class FactorOutResultSingle<T extends Objectable> {
        public final ObjectDelta<T> remainder;
        public final ObjectDelta<T> offspring;

        public FactorOutResultSingle(ObjectDelta<T> remainder, ObjectDelta<T> offspring) {
            this.remainder = remainder;
            this.offspring = offspring;
        }
    }

    @NotNull
    FactorOutResultSingle<O> factorOut(Collection<? extends ItemPath> paths, boolean cloneDelta);

    @NotNull
    FactorOutResultMulti<O> factorOutValues(ItemPath path, boolean cloneDelta) throws SchemaException;

    /*
     * Some comments for modify deltas:
     *
     * Works if we are looking e.g. for modification to inducement item,
     * and delta contains e.g. REPLACE(inducement[1]/validTo, "...").
     *
     * Does NOT work the way around: if we are looking for modification to inducement/validTo and
     * delta contains e.g. ADD(inducement, ...). In such a case we would need to do more complex processing,
     * involving splitting value-to-be-added into remainder and offspring delta. It's probably doable,
     * but some conditions would have to be met, e.g. inducement to be added must have an ID.
     */

    /**
     * Checks if the delta tries to add (or set) a 'value' for the item identified by 'itemPath'. If yes, it removes it.
     *
     * TODO consider changing return value to 'incremental delta' (or null)
     *
     * @param itemPath
     * @param value
     * @param dryRun only testing if value could be subtracted; not changing anything
     * @return true if the delta originally contained an instruction to add (or set) 'itemPath' to 'value'.
     */
    boolean subtract(@NotNull ItemPath itemPath, @NotNull PrismValue value, boolean fromMinusSet, boolean dryRun);

    @NotNull
    List<ItemPath> getModifiedItems();

    List<PrismValue> getNewValuesFor(ItemPath itemPath);

    /**
     * Limitations:
     * (1) For DELETE object delta, we don't know what values were in the object's item.
     * (2) For REPLACE item delta, we don't know what values were in the object's item (but these deltas are quite rare
     * for multivalued items; and eventually there will be normalized into ADD+DELETE form)
     * (3) For DELETE item delta for PrismContainers, content of items deleted might not be known
     * (only ID could be provided on PCVs).
     */
    @SuppressWarnings("unused")            // used from scripts
    List<PrismValue> getDeletedValuesFor(ItemPath itemPath);

    void clear();

    boolean isImmutable();
}
