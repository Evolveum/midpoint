/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * TODO clean up these interfaces!
 */
public interface DeltaFactory {

    // please use DeltaBuilder instead
    @Deprecated // TODO decide on removal
    interface Property {

        @SuppressWarnings("unchecked")
        <T> PropertyDelta<T> createAddDelta(PrismObjectDefinition<? extends Objectable> objectDefinition,
                ItemName propertyName, T... realValues);

        @SuppressWarnings("unchecked")
        <T> PropertyDelta<T> createDeleteDelta(PrismObjectDefinition<? extends Objectable> objectDefinition,
                ItemName propertyName, T... realValues);

        <T> PropertyDelta<T> create(PrismPropertyDefinition<T> propertyDefinition);

        <T> PropertyDelta<T> create(ItemPath path, PrismPropertyDefinition<T> definition);

        @SuppressWarnings("unchecked")
        <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
                QName propertyName, T... realValues);

        // single-argument method just to avoid "unchecked" warnings
        <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
                QName propertyName, PrismPropertyValue<T> pValue);

        @SuppressWarnings("unchecked")
        <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
                QName propertyName, PrismPropertyValue<T>... pValues);

        <O extends Objectable> PropertyDelta createReplaceEmptyDelta(PrismObjectDefinition<O> objectDefinition,
                ItemPath propertyPath);

        <T> PropertyDelta<T> create(ItemPath itemPath, QName name, PrismPropertyDefinition<T> propertyDefinition);

        <O extends Objectable, T> PropertyDelta<T> createReplaceDeltaOrEmptyDelta(PrismObjectDefinition<O> objectDefinition,
                QName propertyName, T realValue);

        <O extends Objectable,T> PropertyDelta<T> createDelta(ItemPath propertyPath, PrismObjectDefinition<O> objectDefinition);

        <O extends Objectable,T> PropertyDelta<T> createDelta(ItemPath propertyPath, Class<O> compileTimeClass);

        @SuppressWarnings("unchecked")
        <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
                T... propertyValues);

        <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
                Collection<T> propertyValues);

        @SuppressWarnings("unchecked")
        <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath path, PrismPropertyDefinition propertyDefinition,
                T... propertyValues);

        @SuppressWarnings("unchecked")
        <T> PropertyDelta<T> createModificationAddProperty(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition,
                T... propertyValues);

        @SuppressWarnings("unchecked")
        <T> PropertyDelta<T> createModificationAddProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
                T... propertyValues);

        @SuppressWarnings("unchecked")
        <T> PropertyDelta<T> createModificationDeleteProperty(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition,
                T... propertyValues);

        @SuppressWarnings("unchecked")
        <T> PropertyDelta<T> createModificationDeleteProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
                T... propertyValues);

        Collection<? extends ItemDelta> createModificationReplacePropertyCollection(QName propertyName,
                PrismObjectDefinition<?> objectDefinition, java.lang.Object... propertyValues);
    }

    // please use DeltaBuilder instead
    @Deprecated // TODO decide on removal
    interface Reference {

        ReferenceDelta create(ItemPath path, PrismReferenceDefinition definition);

        ReferenceDelta create(PrismReferenceDefinition itemDefinition);

        ReferenceDelta create(ItemPath parentPath, QName name, PrismReferenceDefinition itemDefinition);

        ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition, String oid);

        <O extends Objectable> ReferenceDelta createModificationReplace(ItemPath path, Class<O> type, String oid);

        ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                PrismReferenceValue refValue);

        ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                Collection<PrismReferenceValue> refValues);

        Collection<? extends ItemDelta> createModificationAddCollection(ItemName propertyName,
                PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue);

        ReferenceDelta createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                String oid);

        ReferenceDelta createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                PrismReferenceValue refValue);

        ReferenceDelta createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                Collection<PrismReferenceValue> refValues);

        <T extends Objectable> ReferenceDelta createModificationAdd(Class<T> type, ItemName refName, PrismReferenceValue refValue);

        <T extends Objectable> Collection<? extends ItemDelta> createModificationAddCollection(Class<T> type, ItemName refName,
                String targetOid);

        <T extends Objectable> Collection<? extends ItemDelta> createModificationAddCollection(Class<T> type, ItemName refName,
                PrismReferenceValue refValue);

        <T extends Objectable> ReferenceDelta createModificationAdd(Class<T> type, ItemName refName,
                PrismObject<?> refTarget);

        <T extends Objectable> Collection<? extends ItemDelta> createModificationAddCollection(Class<T> type, ItemName refName,
                PrismObject<?> refTarget);

        Collection<? extends ItemDelta> createModificationDeleteCollection(QName propertyName,
                PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue);

        ReferenceDelta createModificationDelete(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                Collection<PrismReferenceValue> refValues);

        ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
                String oid);

        ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
                PrismObject<?> refTarget);

        ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
                PrismReferenceValue refValue);

        <T extends Objectable> ReferenceDelta createModificationDelete(Class<T> type, QName refName,
                PrismReferenceValue refValue);

        <T extends Objectable> Collection<? extends ItemDelta> createModificationDeleteCollection(Class<T> type, QName refName,
                PrismReferenceValue refValue);

        <T extends Objectable> ReferenceDelta createModificationDelete(Class<T> type, QName refName,
                PrismObject<?> refTarget);

        <T extends Objectable> Collection<? extends ItemDelta> createModificationDeleteCollection(Class<T> type, QName refName,
                PrismObject<?> refTarget);
    }

    // please use DeltaBuilder instead
    @Deprecated // TODO decide on removal
    interface Container {
        <C extends Containerable> ContainerDelta<C> create(ItemPath path, PrismContainerDefinition<C> definition);

        <C extends Containerable> ContainerDelta<C>  create(PrismContainerDefinition itemDefinition);

        <C extends Containerable> ContainerDelta<C> create(ItemPath parentPath, QName name,
                PrismContainerDefinition itemDefinition);

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createDelta(ItemPath containerPath,
                Class<O> type);

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createDelta(ItemPath containerPath,
                PrismObjectDefinition<O> objectDefinition);

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createDelta(ItemPath containerPath,
                PrismContainerDefinition<O> objectDefinition);

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationAdd(
                ItemPath containerPath,
                Class<O> type, T containerable) throws SchemaException;

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationAdd(
                ItemPath containerPath,
                Class<O> type, PrismContainerValue<T> cValue) throws SchemaException;

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationDelete(
                ItemPath containerPath,
                Class<O> type, T containerable) throws SchemaException;

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationDelete(
                ItemPath containerPath,
                Class<O> type, PrismContainerValue<T> cValue) throws SchemaException;

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationReplace(
                ItemPath containerPath,
                Class<O> type, T containerable) throws SchemaException;

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationReplace(ItemPath containerPath,
                Class<O> type, Collection<T> containerables) throws SchemaException;

        <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationReplace(
                ItemPath containerPath,
                Class<O> type, PrismContainerValue<T> cValue) throws SchemaException;

        // cValues should be parent-less
        @Deprecated // TODO decide on removal
        Collection<? extends ItemDelta> createModificationReplaceContainerCollection(ItemName containerName,
                PrismObjectDefinition<?> objectDefinition, PrismContainerValue... cValues);

        // cValues should be parent-less
        @Deprecated // TODO decide on removal
        <T extends Containerable> ContainerDelta<T> createModificationReplace(ItemName containerName,
                PrismObjectDefinition<?> objectDefinition, PrismContainerValue... cValues);
    }

    interface Object {

        <O extends Objectable> ObjectDelta<O> create(Class<O> type, ChangeType changeType);

        static <O extends Objectable> ObjectDelta<O> createAddDelta(PrismObject<O> objectToAdd) {
            ObjectDelta<O> objectDelta = objectToAdd.getPrismContext().deltaFactory().object().create(objectToAdd.getCompileTimeClass(), ChangeType.ADD);
            objectDelta.setOid(objectToAdd.getOid());
            objectDelta.setObjectToAdd(objectToAdd);
            return objectDelta;
        }

        @SuppressWarnings("unchecked")
        <O extends Objectable, X> ObjectDelta<O> createModificationReplaceProperty(Class<O> type, String oid,
                ItemPath propertyPath, X... propertyValues);

        <O extends Objectable> ObjectDelta<O> createEmptyDelta(Class<O> type, String oid,
                ChangeType changeType);

        <O extends Objectable> ObjectDelta<O> createEmptyDeleteDelta(Class<O> type, String oid);

        <O extends Objectable> ObjectDelta<O> createEmptyModifyDelta(Class<O> type, String oid);

        <O extends Objectable> ObjectDelta<O> createEmptyAddDelta(Class<O> type, String oid) throws
                                SchemaException;

        <T extends Objectable> ObjectDelta<T> createModifyDelta(String oid, ItemDelta modification,
                Class<T> objectTypeClass);

        <O extends Objectable> ObjectDelta<O> createDeleteDelta(Class<O> type, String oid);

        <T extends Objectable> ObjectDelta<T> createModifyDelta(String oid,
                Collection<? extends ItemDelta> modifications,
                Class<T> objectTypeClass);

        <O extends Objectable> ObjectDelta<O> createModificationDeleteReference(Class<O> type, String oid,
                QName propertyName,
                String... targetOids);

        <O extends Objectable> ObjectDelta<O> createModificationDeleteReference(Class<O> type, String oid,
                QName propertyName,
                PrismReferenceValue... referenceValues);

        @SuppressWarnings("unchecked")
        <O extends Objectable, X> ObjectDelta<O> createModificationDeleteProperty(Class<O> type, String oid,
                ItemPath propertyPath, X... propertyValues);

        @SuppressWarnings("unchecked")
        <O extends Objectable, X> ObjectDelta<O> createModificationAddProperty(Class<O> type, String oid,
                ItemPath propertyPath, X... propertyValues);

        <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid,
                QName propertyName,
                String... targetOids);

        <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid,
                QName propertyName,
                PrismReferenceValue... referenceValues);

        <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid,
                QName propertyName,
                PrismObject<?>... referenceObjects);

        @SuppressWarnings("unchecked")
        <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationReplaceContainer(Class<O> type,
                String oid, ItemPath containerPath,
                PrismContainerValue<C>... containerValues);

        @SuppressWarnings("unchecked")
        <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type,
                String oid,
                ItemPath propertyPath, C... containerValues) throws SchemaException;

        @SuppressWarnings("unchecked")
        <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type,
                String oid, ItemPath containerPath,
                PrismContainerValue<C>... containerValues);

        @SuppressWarnings("unchecked")
        <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationAddContainer(Class<O> type,
                String oid,
                ItemPath propertyPath, C... containerValues) throws SchemaException;

        @SuppressWarnings("unchecked")
        <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationAddContainer(Class<O> type,
                String oid,
                ItemPath propertyPath,
                PrismContainerValue<C>... containerValues);

        @SuppressWarnings("unchecked")
        <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationReplaceContainer(Class<O> type,
                String oid,
                ItemPath propertyPath, C... containerValues) throws SchemaException;

        <O extends Objectable> ObjectDelta<O> createModificationReplaceReference(Class<O> type, String oid,
                ItemPath refPath, PrismReferenceValue... refValues);
    }

    Property property();
    Reference reference();
    Container container();
    Object object();

    <T> DeltaSetTriple<T> createDeltaSetTriple();

    <V> DeltaSetTriple<V> createDeltaSetTriple(Collection<V> zeroSet, Collection<V> plusSet, Collection<V> minusSet);

    <V extends PrismValue> PrismValueDeltaSetTriple<V> createPrismValueDeltaSetTriple();

    <V extends PrismValue> PrismValueDeltaSetTriple<V> createPrismValueDeltaSetTriple(Collection<V> zeroSet,
            Collection<V> plusSet, Collection<V> minusSet);

    <K, V> DeltaMapTriple<K, V> createDeltaMapTriple();


}
