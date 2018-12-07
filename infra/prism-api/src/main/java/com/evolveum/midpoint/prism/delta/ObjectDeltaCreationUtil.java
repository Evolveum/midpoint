/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;

/**
 *  Methods dealing with object delta creation.
 *
 *  These are probably to be replaced by Delta Builder interface.
 *
 *  TODO decide on this!
 */
public class ObjectDeltaCreationUtil {

	/**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method.
     */
    @SafeVarargs
    public static <O extends Objectable, X> ObjectDelta<O> createModificationReplaceProperty(Class<O> type, String oid,
		    ItemPath propertyPath, PrismContext prismContext, X... propertyValues) {
    	ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
    	objectDelta.setOid(oid);
    	fillInModificationReplaceProperty(objectDelta, propertyPath, propertyValues);
    	return objectDelta;
    }

	/**
	 * Convenience method for quick creation of object deltas that replace a single object reference.
	 */
	public static <O extends Objectable> ObjectDelta<O> createModificationReplaceReference(Class<O> type, String oid,
			ItemPath refPath, PrismContext prismContext, PrismReferenceValue... refValues) {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationReplaceReference(objectDelta, refPath, refValues);
		return objectDelta;
	}

	@SafeVarargs
	public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationReplaceContainer(Class<O> type,
			String oid,
			ItemPath propertyPath, PrismContext prismContext, C... containerValues) throws SchemaException {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationReplaceContainer(objectDelta, propertyPath, containerValues);
		return objectDelta;
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

	protected static <O extends Objectable, X> void fillInModificationAddProperty(ObjectDelta<O> objectDelta,
			ItemPath propertyPath, X... propertyValues) {
		PropertyDelta<X> propertyDelta = objectDelta.createPropertyModification(propertyPath);
		if (propertyValues != null) {
			Collection<PrismPropertyValue<X>> valuesToAdd = PrismValueCollectionsUtil
					.toPrismPropertyValues(objectDelta.getPrismContext(), propertyValues);
			propertyDelta.addValuesToAdd(valuesToAdd);
			objectDelta.addModification(propertyDelta);
		}
	}

	protected static <O extends Objectable, X> void fillInModificationDeleteProperty(ObjectDelta<O> objectDelta,
			ItemPath propertyPath, X... propertyValues) {
		PropertyDelta<X> propertyDelta = objectDelta.createPropertyModification(propertyPath);
		if (propertyValues != null) {
			Collection<PrismPropertyValue<X>> valuesToDelete = PrismValueCollectionsUtil
					.toPrismPropertyValues(objectDelta.getPrismContext(), propertyValues);
			propertyDelta.addValuesToDelete(valuesToDelete);
			objectDelta.addModification(propertyDelta);
		}
	}

	protected static <O extends Objectable> void fillInModificationReplaceReference(ObjectDelta<O> objectDelta,
			ItemPath refPath, PrismReferenceValue... refValues) {
	    ReferenceDelta refDelta = objectDelta.createReferenceModification(refPath);
	    if (refValues != null) {
	        refDelta.setValuesToReplace(refValues);
	        objectDelta.addModification(refDelta);
	    }
	}

	protected static <O extends Objectable> void fillInModificationAddReference(ObjectDelta<O> objectDelta,
			ItemPath refPath, PrismReferenceValue... refValues) {
			ReferenceDelta refDelta = objectDelta.createReferenceModification(refPath);
			if (refValues != null) {
				refDelta.addValuesToAdd(refValues);
				objectDelta.addModification(refDelta);
			}
		}

	protected static <O extends Objectable> void fillInModificationDeleteReference(ObjectDelta<O> objectDelta,
			ItemPath refPath, PrismReferenceValue... refValues) {
			ReferenceDelta refDelta = objectDelta.createReferenceModification(refPath);
			if (refValues != null) {
				refDelta.addValuesToDelete(refValues);
				objectDelta.addModification(refDelta);
			}
		}

	@SafeVarargs
	public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationAddContainer(Class<O> type,
			String oid,
			ItemPath propertyPath, PrismContext prismContext, PrismContainerValue<C>... containerValues) {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationAddContainer(objectDelta, propertyPath, containerValues);
		return objectDelta;
	}

	@SafeVarargs
	public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationAddContainer(Class<O> type,
			String oid,
			ItemPath propertyPath, PrismContext prismContext, C... containerValues) throws SchemaException {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
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

	@SafeVarargs
	public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type,
			String oid, ItemPath containerPath,
			PrismContext prismContext, PrismContainerValue<C>... containerValues) {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationDeleteContainer(objectDelta, containerPath, containerValues);
		return objectDelta;
	}

	@SafeVarargs
	public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type,
			String oid,
			ItemPath propertyPath, PrismContext prismContext, C... containerValues) throws SchemaException {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
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

	@SafeVarargs
	protected static <O extends Objectable, C extends Containerable> void fillInModificationDeleteContainer(
			ObjectDelta<O> objectDelta,
			ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
		ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
		if (containerValues != null && containerValues.length > 0) {
			containerDelta.addValuesToDelete(containerValues);
		}
	}

	@SafeVarargs
	public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationReplaceContainer(Class<O> type,
			String oid, ItemPath containerPath,
			PrismContext prismContext, PrismContainerValue<C>... containerValues) {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
		objectDelta.setOid(oid);
		fillInModificationReplaceContainer(objectDelta, containerPath, containerValues);
		return objectDelta;
	}

	@SafeVarargs
	protected static <O extends Objectable, C extends Containerable> void fillInModificationAddContainer(
			ObjectDelta<O> objectDelta,
			ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
		ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
		if (containerValues != null && containerValues.length > 0) {
			containerDelta.addValuesToAdd(containerValues);
		}
	}

	@SafeVarargs
	protected static <O extends Objectable, C extends Containerable> void fillInModificationAddContainer(
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
	protected static <O extends Objectable, C extends Containerable> void fillInModificationDeleteContainer(
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
	protected static <O extends Objectable, C extends Containerable> void fillInModificationReplaceContainer(
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
	protected static <O extends Objectable, C extends Containerable> void fillInModificationReplaceContainer(
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

	/**
	 * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
	 * to justify a separate method.
	 */
	public static <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid,
			QName propertyName,
			PrismContext prismContext, PrismObject<?>... referenceObjects) {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
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

	public static <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid,
			QName propertyName,
			PrismContext prismContext, String... targetOids) {
		PrismReferenceValue[] referenceValues = new PrismReferenceValue[targetOids.length];
		for(int i=0; i < targetOids.length; i++) {
			referenceValues[i] = prismContext.itemFactory().createReferenceValue(targetOids[i]);
		}
		return createModificationAddReference(type, oid, propertyName, prismContext, referenceValues);
	}

	/**
	 * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
	 * to justify a separate method.
	 */
	public static <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid,
			QName propertyName,
			PrismContext prismContext, PrismReferenceValue... referenceValues) {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
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

	public static <O extends Objectable> ObjectDelta<O> createModificationDeleteReference(Class<O> type, String oid,
			QName propertyName,
			PrismContext prismContext, String... targetOids) {
		PrismReferenceValue[] referenceValues = new PrismReferenceValue[targetOids.length];
		for (int i=0; i < targetOids.length; i++) {
			referenceValues[i] = prismContext.itemFactory().createReferenceValue(targetOids[i]);
		}
		return createModificationDeleteReference(type, oid, propertyName, prismContext, referenceValues);
	}

	/**
	 * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
	 * to justify a separate method.
	 */
	public static <O extends Objectable> ObjectDelta<O> createModificationDeleteReference(Class<O> type, String oid,
			QName propertyName,
			PrismContext prismContext, PrismReferenceValue... referenceValues) {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
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

	public static <T extends Objectable> ObjectDelta<T> createModifyDelta(String oid, ItemDelta modification,
			Class<T> objectTypeClass, PrismContext prismContext) {
		Collection modifications = new ArrayList<ItemDelta>(1);
		modifications.add(modification);
		return createModifyDelta(oid, modifications, objectTypeClass, prismContext);
	}

	public static <T extends Objectable> ObjectDelta<T> createModifyDelta(String oid,
			Collection<? extends ItemDelta> modifications,
			Class<T> objectTypeClass, PrismContext prismContext) {
		ObjectDelta<T> objectDelta = prismContext.deltaFactory().object().create(objectTypeClass, ChangeType.MODIFY);
		objectDelta.addModifications(modifications);
		objectDelta.setOid(oid);
		return objectDelta;
	}

	public static <O extends Objectable> ObjectDelta<O> createEmptyAddDelta(Class<O> type, String oid, PrismContext prismContext) throws SchemaException {
		ObjectDelta<O> objectDelta = createEmptyDelta(type, oid, prismContext, ChangeType.ADD);
		PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		PrismObject<O> objectToAdd = objDef.instantiate();
		objectDelta.setObjectToAdd(objectToAdd);
		return objectDelta;
	}

	public static <O extends Objectable> ObjectDelta<O> createEmptyModifyDelta(Class<O> type, String oid,
			PrismContext prismContext) {
		return createEmptyDelta(type, oid, prismContext, ChangeType.MODIFY);
	}

	public static <O extends Objectable> ObjectDelta<O> createEmptyDeleteDelta(Class<O> type, String oid,
			PrismContext prismContext) {
		return createEmptyDelta(type, oid, prismContext, ChangeType.DELETE);
	}

	public static <O extends Objectable> ObjectDelta<O> createEmptyDelta(Class<O> type, String oid, PrismContext prismContext,
			ChangeType changeType) {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, changeType);
		objectDelta.setOid(oid);
		return objectDelta;
	}

	public static <O extends Objectable> ObjectDelta<O> createAddDelta(PrismObject<O> objectToAdd) {
		ObjectDelta<O> objectDelta = objectToAdd.getPrismContext().deltaFactory().object().create(objectToAdd.getCompileTimeClass(), ChangeType.ADD);
		objectDelta.setOid(objectToAdd.getOid());
		objectDelta.setObjectToAdd(objectToAdd);
		return objectDelta;
	}

	public static <O extends Objectable> ObjectDelta<O> createDeleteDelta(Class<O> type, String oid, PrismContext prismContext) {
		ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.DELETE);
		objectDelta.setOid(oid);
		return objectDelta;
	}

	@SafeVarargs
	public static <O extends Objectable, X> ObjectDelta<O> createModificationAddProperty(Class<O> type, String oid,
			ItemPath propertyPath, PrismContext prismContext, X... propertyValues) {
    	ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
    	objectDelta.setOid(oid);
    	fillInModificationAddProperty(objectDelta, propertyPath, propertyValues);
    	return objectDelta;
    }

	@SafeVarargs
	public static <O extends Objectable, X> ObjectDelta<O> createModificationDeleteProperty(Class<O> type, String oid,
			ItemPath propertyPath, PrismContext prismContext, X... propertyValues) {
    	ObjectDelta<O> objectDelta = prismContext.deltaFactory().object().create(type, ChangeType.MODIFY);
    	objectDelta.setOid(oid);
    	fillInModificationDeleteProperty(objectDelta, propertyPath, propertyValues);
    	return objectDelta;
    }
}
