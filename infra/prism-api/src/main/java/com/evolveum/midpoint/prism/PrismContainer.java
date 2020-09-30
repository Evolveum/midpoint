/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.*;
import java.util.function.Consumer;

/**
 * <p>
 * Prism container groups items into logical blocks. The reason for
 * grouping may be as simple as better understandability of data structure. But
 * the group usually means different meaning, source or structure of the data.
 * For example, the a container is frequently used to hold properties
 * that are dynamic, not fixed by a static schema. Such grouping also naturally
 * translates to XML and helps to "quarantine" such properties to avoid Unique
 * Particle Attribute problems.
 * </p><p>
 * Container contains a set of (potentially multi-valued) properties, references,
 * or inner containers. The order of properties is not significant, regardless
 * of the fact that it may be fixed in the XML representation. In the XML representation,
 * each element inside the container must be either property, reference, or a container.
 * </p><p>
 * A container is mutable.
 * </p>
 *
 * @author Radovan Semancik
 */
public interface PrismContainer<C extends Containerable>
        extends Item<PrismContainerValue<C>, PrismContainerDefinition<C>>, PrismContainerable<C> {


    @Override
    PrismContainerDefinition<C> getDefinition();

    /**
     * Returns the static type of data represented by values of this container,
     * if known and applicable. (There are containers that are purely dynamic, i.e.
     * without any compile time class.)
     */
    @Override
    @Nullable
    Class<C> getCompileTimeClass();

    /**
     * Returns true if values of this container can be represented as specified compile-time class.
     * For example, PrismContainer of AbstractRoleType has:
     * - canRepresent(AbstractRoleType.class) = true
     * - canRepresent(FocusType.class) = true
     * - canRepresent(ObjectType.class) = true
     * - canRepresent(TaskType.class) = false
     * - canRepresent(RoleType.class) = false
     */
    boolean canRepresent(@NotNull Class<?> compileTimeClass);

    /**
     * Returns true if values of this container can be presented as specified type (from compile-time or runtime schema).
     * In particular, returns true if type of this container or any of its supertypes match given type.
     */
    boolean canRepresent(QName type);

    /**
     * @return List of current values. The list itself is freely modifiable - it is independent on the list of values
     * in this container. However, values themselves are directly linked to the PCVs.
     */
    @NotNull
    @Override
    Collection<C> getRealValues();

    @Override
    @NotNull
    C getRealValue();

    void setRealValue(C value) throws SchemaException;

    void setValue(@NotNull PrismContainerValue<C> value) throws SchemaException;

    @Override
    @NotNull
    PrismContainerValue<C> getValue();

    PrismContainerValue<C> getValue(Long id);

    <T> void setPropertyRealValue(QName propertyName, T realValue) throws SchemaException;

    <C extends Containerable> void setContainerRealValue(QName containerName, C realValue) throws SchemaException;

    <T> void setPropertyRealValues(QName propertyName, T... realValues) throws SchemaException;

    <T> T getPropertyRealValue(ItemPath propertyPath, Class<T> type);

    /**
     * Convenience method. Works only on single-valued containers.
     */
    void add(Item<?, ?> item) throws SchemaException;

    PrismContainerValue<C> createNewValue();

    void mergeValues(PrismContainer<C> other) throws SchemaException;

    void mergeValues(Collection<PrismContainerValue<C>> otherValues) throws SchemaException;

    void mergeValue(PrismContainerValue<C> otherValue) throws SchemaException;

    /**
     * Remove all empty values
     */
    void trim();

    /**
     * Sets applicable property container definition.
     *
     * @param definition the definition to set
     */
    @Override
    void setDefinition(PrismContainerDefinition<C> definition);

    @Override
    void applyDefinition(PrismContainerDefinition<C> definition) throws SchemaException;

    <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findItem(QName itemQName, Class<I> type);

    @Override
    <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

    <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findCreateItem(QName itemQName, Class<I> type, boolean create) throws SchemaException;

    <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findItem(ItemPath path, Class<I> type);

    <IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findItem(ItemPath path);

    boolean containsItem(ItemPath itemPath, boolean acceptEmptyItem) throws SchemaException;

    <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findCreateItem(ItemPath itemPath, Class<I> type, ID itemDefinition, boolean create) throws SchemaException;

    PrismContainerValue<C> findValue(long id);

    <T extends Containerable> PrismContainer<T> findContainer(ItemPath path);

    <T> PrismProperty<T> findProperty(ItemPath path);

    PrismReference findReference(ItemPath path);

    <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(ItemPath containerPath,
            Class<I> type) throws SchemaException;

    // The "definition" parameter provides definition of item to create, in case that the container does not have
    // the definition (e.g. in case of "extension" containers)
    <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(ItemPath containerPath,
            Class<I> type, ID definition) throws SchemaException;

    <T extends Containerable> PrismContainer<T> findOrCreateContainer(ItemPath containerPath) throws SchemaException;

    <T> PrismProperty<T> findOrCreateProperty(ItemPath propertyPath) throws SchemaException;

    PrismReference findOrCreateReference(ItemPath propertyPath) throws SchemaException;

    /**
     * Convenience method. Works only on single-valued containers.
     */
    void remove(Item<?, ?> item);

    void removeProperty(ItemPath path);

    void removeContainer(ItemPath path);

    void removeReference(ItemPath path);

    <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> void removeItem(ItemPath path, Class<I> itemType);

    // Expects that the "self" path segment is NOT included in the basePath
    // is this method used anywhere?
//    void addItemPathsToList(ItemPath basePath, Collection<ItemPath> list) {
//        boolean addIds = true;
//        if (getDefinition() != null) {
//            if (getDefinition().isSingleValue()) {
//                addIds = false;
//            }
//        }
//        for (PrismContainerValue<V> pval: getValues()) {
//            ItemPath subpath = null;
//            ItemPathSegment segment = null;
//            if (addIds) {
//                subpath = basePath.subPath(new IdItemPathSegment(pval.getId())).subPath(new NameItemPathSegment(getElementName()));
//            } else {
//                subpath = basePath.subPath(new NameItemPathSegment(getElementName()));
//            }
//            pval.addItemPathsToList(subpath, list);
//        }
//    }

    @Override
    ContainerDelta<C> createDelta();

    @Override
    ContainerDelta<C> createDelta(ItemPath path);

    ContainerDelta<C> diff(PrismContainer<C> other);

    ContainerDelta<C> diff(PrismContainer<C> other, ParameterizedEquivalenceStrategy strategy);

    List<? extends ItemDelta> diffModifications(PrismContainer<C> other, ParameterizedEquivalenceStrategy strategy);

    @Override
    PrismContainer<C> clone();

    @Override
    PrismContainer<C> createImmutableClone();

    @Override
    PrismContainer<C> cloneComplex(CloneStrategy strategy);

    PrismContainerDefinition<C> deepCloneDefinition(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction);

    @Override
    void accept(Visitor visitor, ItemPath path, boolean recursive);


    /**
     * This method ignores some part of the object during comparison (e.g. source demarcation in values)
     * These methods compare the "meaningful" parts of the objects.
     */
    boolean equivalent(Object obj);

    static <V extends Containerable> PrismContainer<V> newInstance(PrismContext prismContext, QName type) throws SchemaException {
        PrismContainerDefinition<V> definition = prismContext.getSchemaRegistry().findContainerDefinitionByType(type);
        if (definition == null) {
            throw new SchemaException("Definition for " + type + " couldn't be found");
        }
        return definition.instantiate();
    }

    static <V extends PrismContainerValue> void createParentIfNeeded(V value, ItemDefinition definition) throws SchemaException {
        if (value.getParent() != null) {
            return;
        }
        if (!(definition instanceof PrismContainerDefinition)) {
            throw new SchemaException("Missing or invalid definition for a PrismContainer: " + definition);
        }
        PrismContainer<?> rv = (PrismContainer) definition.instantiate();
        rv.add(value);
    }

    /**
     * Optimizes (trims) definition tree by removing any definitions not corresponding to items in this container.
     * Works recursively by sub-containers of this one.
     * USE WITH CARE. Make sure the definitions are not shared by other objects!
     */
    void trimDefinitionTree(Collection<? extends ItemPath> alwaysKeep);
}
