/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class ObjectDeltaFactoryImpl implements DeltaFactory.Object {

	@NotNull private final PrismContext prismContext;

	ObjectDeltaFactoryImpl(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	@SafeVarargs
	public static <O extends Objectable, X> PropertyDelta<X> fillInModificationReplaceProperty(ObjectDelta<O> objectDelta,
			ItemPath propertyPath, X... propertyValues) {
		PropertyDelta<X> propertyDelta = objectDelta.createPropertyModification(propertyPath);
		if (propertyValues != null) {
			Collection<PrismPropertyValue<X>> valuesToReplace = PrismValueCollectionsUtil
					.toPrismPropertyValues(objectDelta.getPrismContext(), propertyValues);
			propertyDelta.setValuesToReplace(valuesToReplace);
			objectDelta.addModification(propertyDelta);
		}

		return propertyDelta;
	}

	@SafeVarargs
	static <O extends Objectable, X> void fillInModificationAddProperty(ObjectDelta<O> objectDelta,
			ItemPath propertyPath, X... propertyValues) {
		PropertyDelta<X> propertyDelta = objectDelta.createPropertyModification(propertyPath);
		if (propertyValues != null) {
			Collection<PrismPropertyValue<X>> valuesToAdd = PrismValueCollectionsUtil
					.toPrismPropertyValues(objectDelta.getPrismContext(), propertyValues);
			propertyDelta.addValuesToAdd(valuesToAdd);
			objectDelta.addModification(propertyDelta);
		}
	}

	@SafeVarargs
	public static <O extends Objectable, X> void fillInModificationDeleteProperty(ObjectDelta<O> objectDelta,
			ItemPath propertyPath, X... propertyValues) {
		PropertyDelta<X> propertyDelta = objectDelta.createPropertyModification(propertyPath);
		if (propertyValues != null) {
			Collection<PrismPropertyValue<X>> valuesToDelete = PrismValueCollectionsUtil
					.toPrismPropertyValues(objectDelta.getPrismContext(), propertyValues);
			propertyDelta.addValuesToDelete(valuesToDelete);
			objectDelta.addModification(propertyDelta);
		}
	}

	public static <O extends Objectable> void fillInModificationReplaceReference(ObjectDelta<O> objectDelta,
			ItemPath refPath, PrismReferenceValue... refValues) {
	    ReferenceDelta refDelta = objectDelta.createReferenceModification(refPath);
	    if (refValues != null) {
	        refDelta.setValuesToReplace(refValues);
	        objectDelta.addModification(refDelta);
	    }
	}

	public static <O extends Objectable> void fillInModificationAddReference(ObjectDelta<O> objectDelta,
			ItemPath refPath, PrismReferenceValue... refValues) {
			ReferenceDelta refDelta = objectDelta.createReferenceModification(refPath);
			if (refValues != null) {
				refDelta.addValuesToAdd(refValues);
				objectDelta.addModification(refDelta);
			}
		}

	static <O extends Objectable> void fillInModificationDeleteReference(ObjectDelta<O> objectDelta,
			ItemPath refPath, PrismReferenceValue... refValues) {
				ReferenceDelta refDelta = objectDelta.createReferenceModification(refPath);
				if (refValues != null) {
					refDelta.addValuesToDelete(refValues);
					objectDelta.addModification(refDelta);
				}
			}

	@SafeVarargs
	public static <O extends Objectable, C extends Containerable> void fillInModificationDeleteContainer(
			ObjectDelta<O> objectDelta,
			ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
		ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
		if (containerValues != null && containerValues.length > 0) {
			containerDelta.addValuesToDelete(containerValues);
		}
	}

	@SafeVarargs
	public static <O extends Objectable, C extends Containerable> void fillInModificationAddContainer(
			ObjectDelta<O> objectDelta,
			ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
		ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
		if (containerValues != null && containerValues.length > 0) {
			containerDelta.addValuesToAdd(containerValues);
		}
	}

	@SafeVarargs
	static <O extends Objectable, C extends Containerable> void fillInModificationAddContainer(
			ObjectDelta<O> objectDelta,
			ItemPath propertyPath, PrismContext prismContext, C... containerables) throws SchemaException {
		ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
		if (containerables != null) {
			for (C containerable: containerables) {
				prismContext.adopt(containerable, objectDelta.getObjectTypeClass(), propertyPath);
				PrismContainerValue<C> prismContainerValue = containerable.asPrismContainerValue();
				containerDelta.addValueToAdd(prismContainerValue);
			}
		}
	}

	@SafeVarargs
	static <O extends Objectable, C extends Containerable> void fillInModificationDeleteContainer(
			ObjectDelta<O> objectDelta,
			ItemPath propertyPath, PrismContext prismContext, C... containerables) throws SchemaException {
		ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
		if (containerables != null) {
			for (C containerable: containerables) {
				prismContext.adopt(containerable, objectDelta.getObjectTypeClass(), propertyPath);
				PrismContainerValue<C> prismContainerValue = containerable.asPrismContainerValue();
				containerDelta.addValueToDelete(prismContainerValue);
			}
		}
	}

	@SafeVarargs
	static <O extends Objectable, C extends Containerable> void fillInModificationReplaceContainer(
			ObjectDelta<O> objectDelta,
			ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
		ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
		if (containerValues != null && containerValues.length > 0) {
			containerDelta.setValuesToReplace(containerValues);
		} else {
			// Means: clear all values
			containerDelta.setValuesToReplace();
		}
	}

	@SafeVarargs
	private static <O extends Objectable, C extends Containerable> void fillInModificationReplaceContainer(
			ObjectDelta<O> objectDelta,
			ItemPath propertyPath, C... containerValues) throws SchemaException {
		if (containerValues != null) {
			ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
			Collection<PrismContainerValue<C>> valuesToReplace = PrismValueCollectionsUtil
					.toPrismContainerValues(objectDelta.getObjectTypeClass(), propertyPath, objectDelta.getPrismContext(), containerValues);
			containerDelta.setValuesToReplace(valuesToReplace);
			objectDelta.addModification(containerDelta);
		}
	}

	@Override
	public <O extends Objectable> ObjectDelta<O> create(Class<O> type, ChangeType changeType) {
		return new ObjectDeltaImpl<>(type, changeType, prismContext);
	}

	/**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method.
     */
	@SuppressWarnings("unchecked")
	@Override
	public <O extends Objectable, X> ObjectDelta<O> createModificationReplaceProperty(Class<O> type, String oid,
			ItemPath propertyPath, X... propertyValues) {
	    ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
	    objectDelta.setOid(oid);
	    fillInModificationReplaceProperty(objectDelta, propertyPath, propertyValues);
	    return objectDelta;
    }

	@Override
	public <O extends Objectable> ObjectDelta<O> createEmptyDelta(Class<O> type, String oid,
			ChangeType changeType) {
		ObjectDelta<O> objectDelta = create(type, changeType);
		objectDelta.setOid(oid);
		return objectDelta;
	}

	@Override
	public <O extends Objectable> ObjectDelta<O> createEmptyDeleteDelta(Class<O> type, String oid) {
		return this.createEmptyDelta(type, oid, ChangeType.DELETE);
	}

	@Override
	public <O extends Objectable> ObjectDelta<O> createEmptyModifyDelta(Class<O> type, String oid) {
		return this.createEmptyDelta(type, oid, ChangeType.MODIFY);
	}

	@Override
	public <O extends Objectable> ObjectDelta<O> createEmptyAddDelta(Class<O> type, String oid) throws
			SchemaException {
		ObjectDelta<O> objectDelta = this.createEmptyDelta(type, oid, ChangeType.ADD);
		PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		PrismObject<O> objectToAdd = objDef.instantiate();
		objectDelta.setObjectToAdd(objectToAdd);
		return objectDelta;
	}

	@Override
	public <T extends Objectable> ObjectDelta<T> createModifyDelta(String oid, ItemDelta modification,
			Class<T> objectTypeClass) {
		Collection modifications = new ArrayList<ItemDelta>(1);
		modifications.add(modification);
		return createModifyDelta(oid, modifications, objectTypeClass);
	}

	@Override
	public <O extends Objectable> ObjectDelta<O> createDeleteDelta(Class<O> type, String oid) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.DELETE);
		objectDelta.setOid(oid);
		return objectDelta;
	}

	@Override
	public <T extends Objectable> ObjectDelta<T> createModifyDelta(String oid,
			Collection<? extends ItemDelta> modifications,
			Class<T> objectTypeClass) {
		ObjectDelta<T> objectDelta = create(objectTypeClass, ChangeType.MODIFY);
		objectDelta.addModifications(modifications);
		objectDelta.setOid(oid);
		return objectDelta;
	}

	@Override
	public <O extends Objectable> ObjectDelta<O> createModificationDeleteReference(Class<O> type, String oid,
			QName propertyName,
			String... targetOids) {
		PrismReferenceValue[] referenceValues = new PrismReferenceValue[targetOids.length];
		for (int i=0; i < targetOids.length; i++) {
			referenceValues[i] = prismContext.itemFactory().createReferenceValue(targetOids[i]);
		}
		return createModificationDeleteReference(type, oid, propertyName, referenceValues);
	}

	/**
	 * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
	 * to justify a separate method.
	 */
	@Override
	public <O extends Objectable> ObjectDelta<O> createModificationDeleteReference(Class<O> type, String oid,
			QName propertyName,
			PrismReferenceValue... referenceValues) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		PrismReferenceDefinition refDef = objDef.findReferenceDefinition(ItemName.fromQName(propertyName));
		ReferenceDelta referenceDelta = objectDelta.createReferenceModification(ItemName.fromQName(propertyName), refDef);
		Collection<PrismReferenceValue> valuesToDelete = new ArrayList<>(referenceValues.length);
		for (PrismReferenceValue refVal: referenceValues) {
			valuesToDelete.add(refVal);
		}
		referenceDelta.addValuesToDelete(valuesToDelete);
		return objectDelta;
	}

	@Override
	public <O extends Objectable, X> ObjectDelta<O> createModificationDeleteProperty(Class<O> type, String oid,
			ItemPath propertyPath, X... propertyValues) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationDeleteProperty(objectDelta, propertyPath, propertyValues);
		return objectDelta;
	}

	@Override
	public <O extends Objectable, X> ObjectDelta<O> createModificationAddProperty(Class<O> type, String oid,
			ItemPath propertyPath, X... propertyValues) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationAddProperty(objectDelta, propertyPath, propertyValues);
		return objectDelta;
	}

	@Override
	public <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid,
			QName propertyName,
			String... targetOids) {
		PrismReferenceValue[] referenceValues = new PrismReferenceValue[targetOids.length];
		for(int i=0; i < targetOids.length; i++) {
			referenceValues[i] = prismContext.itemFactory().createReferenceValue(targetOids[i]);
		}
		return createModificationAddReference(type, oid, propertyName, referenceValues);
	}

	/**
	 * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
	 * to justify a separate method.
	 */
	@Override
	public <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid,
			QName propertyName,
			PrismReferenceValue... referenceValues) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		PrismReferenceDefinition refDef = objDef.findReferenceDefinition(ItemName.fromQName(propertyName));
		ReferenceDelta referenceDelta = objectDelta.createReferenceModification(ItemName.fromQName(propertyName), refDef);
		Collection<PrismReferenceValue> valuesToAdd = new ArrayList<>(referenceValues.length);
		for (PrismReferenceValue refVal: referenceValues) {
			valuesToAdd.add(refVal);
		}
		referenceDelta.addValuesToAdd(valuesToAdd);
		return objectDelta;
	}

	/**
	 * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
	 * to justify a separate method.
	 */
	@Override
	public <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid,
			QName propertyName,
			PrismObject<?>... referenceObjects) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		PrismReferenceDefinition refDef = objDef.findReferenceDefinition(ItemName.fromQName(propertyName));
		ReferenceDelta referenceDelta = objectDelta.createReferenceModification(ItemName.fromQName(propertyName), refDef);
		Collection<PrismReferenceValue> valuesToReplace = new ArrayList<>(referenceObjects.length);
		for (PrismObject<?> refObject: referenceObjects) {
			valuesToReplace.add(prismContext.itemFactory().createReferenceValue(refObject));
		}
		referenceDelta.setValuesToReplace(valuesToReplace);
		return objectDelta;
	}

	@Override
	public <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationReplaceContainer(Class<O> type,
			String oid, ItemPath containerPath,
			PrismContainerValue<C>... containerValues) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationReplaceContainer(objectDelta, containerPath, containerValues);
		return objectDelta;
	}

	@Override
	public <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type,
			String oid,
			ItemPath propertyPath, C... containerValues) throws SchemaException {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		PrismContainerValue<C>[] containerPValues = new PrismContainerValue[containerValues.length];
		for (int i=0; i<containerValues.length; i++) {
			C containerable = containerValues[i];
			prismContext.adopt(containerable, type, propertyPath);
			containerPValues[i] = containerable.asPrismContainerValue();
		}
		fillInModificationDeleteContainer(objectDelta, propertyPath, containerPValues);
		return objectDelta;
	}

	@Override
	public <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type,
			String oid, ItemPath containerPath,
			PrismContainerValue<C>... containerValues) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationDeleteContainer(objectDelta, containerPath, containerValues);
		return objectDelta;
	}

	@Override
	public <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationAddContainer(Class<O> type,
			String oid,
			ItemPath propertyPath, C... containerValues) throws SchemaException {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		PrismContainerValue<C>[] containerPValues = new PrismContainerValue[containerValues.length];
		for (int i=0; i<containerValues.length; i++) {
			C containerable = containerValues[i];
			prismContext.adopt(containerable, type, propertyPath);
			containerPValues[i] = containerable.asPrismContainerValue();
		}
		fillInModificationAddContainer(objectDelta, propertyPath, containerPValues);
		return objectDelta;
	}

	@Override
	public <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationAddContainer(Class<O> type,
			String oid,
			ItemPath propertyPath,
			PrismContainerValue<C>... containerValues) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationAddContainer(objectDelta, propertyPath, containerValues);
		return objectDelta;
	}

	@Override
	public <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationReplaceContainer(Class<O> type,
			String oid,
			ItemPath propertyPath, C... containerValues) throws SchemaException {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationReplaceContainer(objectDelta, propertyPath, containerValues);
		return objectDelta;
	}

	/**
	 * Convenience method for quick creation of object deltas that replace a single object reference.
	 */
	@Override
	public <O extends Objectable> ObjectDelta<O> createModificationReplaceReference(Class<O> type, String oid,
			ItemPath refPath, PrismReferenceValue... refValues) {
		ObjectDelta<O> objectDelta = create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationReplaceReference(objectDelta, refPath, refValues);
		return objectDelta;
	}

}
