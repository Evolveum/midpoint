/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Relative difference (delta) of the object.
 * <p/>
 * This class describes how the object changes. It can describe either object addition, modification of deletion.
 * <p/>
 * Addition described complete new (absolute) state of the object.
 * <p/>
 * Modification contains a set property deltas that describe relative changes to individual properties
 * <p/>
 * Deletion does not contain anything. It only marks object for deletion.
 * <p/>
 * The OID is mandatory for modification and deletion.
 *
 * @author Radovan Semancik
 * @see PropertyDelta
 */
public class ObjectDelta<T extends Objectable> implements Dumpable, DebugDumpable {

    private ChangeType changeType;

    /**
     * OID of the object that this delta applies to.
     */
    private String oid;

    /**
     * New object to add. Valid only if changeType==ADD
     */
    private PrismObject<T> objectToAdd;

    /**
     * Set of relative property deltas. Valid only if changeType==MODIFY
     */
    private Collection<? extends ItemDelta> modifications;

    /**
     * Class of the object that we describe.
     */
    private Class<T> objectTypeClass;

    public ObjectDelta(Class<T> objectTypeClass, ChangeType changeType) {
        this.changeType = changeType;
        this.objectTypeClass = objectTypeClass;
        objectToAdd = null;
        modifications = createEmptyModifications();
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public PrismObject<T> getObjectToAdd() {
        return objectToAdd;
    }

    public void setObjectToAdd(PrismObject<T> objectToAdd) {
        this.objectToAdd = objectToAdd;
        if (objectToAdd != null) {
            this.objectTypeClass = objectToAdd.getCompileTimeClass();
        }
    }

    public Collection<? extends ItemDelta> getModifications() {
        return modifications;
    }

    public void addModification(ItemDelta itemDelta) {
        ((Collection)modifications).add(itemDelta);
    }
    
    public void addModifications(Collection<? extends ItemDelta> itemDeltas) {
    	for (ItemDelta modDelta: itemDeltas) {
    		addModification(modDelta);
    	}
    }
    
    private <D extends ItemDelta, I extends Item> D findItemDelta(PropertyPath propertyPath, Class<D> deltaType, Class<I> itemType) {
        if (changeType == ChangeType.ADD) {
            I item = objectToAdd.findItem(propertyPath, itemType);
            if (item == null) {
                return null;
            }
            D itemDelta = createEmptyDelta(propertyPath, item.getDefinition(), deltaType, itemType);
            itemDelta.addValuesToAdd(item.getClonedValues());
            return itemDelta;
        } else if (changeType == ChangeType.MODIFY) {
            return findModification(propertyPath, deltaType);
        } else {
            return null;
        }
    }
    
    private <D extends ItemDelta, I extends Item> D createEmptyDelta(PropertyPath propertyPath, ItemDefinition itemDef,
    		Class<D> deltaType, Class<I> itemType) {
    
    	if (PrismProperty.class.isAssignableFrom(itemType)) {
    		return (D) new PropertyDelta(propertyPath, (PrismPropertyDefinition)itemDef);
    	} else if (PrismContainer.class.isAssignableFrom(itemType)) {
    		return (D) new ContainerDelta(propertyPath, (PrismContainerDefinition)itemDef);
    	} else if (PrismReference.class.isAssignableFrom(itemType)) {
    		return (D) new ReferenceDelta(propertyPath, (PrismReferenceDefinition)itemDef);
    	} else {
    		throw new IllegalArgumentException("Unknown item type "+itemType);
    	}
    }

    public Class<T> getObjectTypeClass() {
        return objectTypeClass;
    }

    public void setObjectTypeClass(Class<T> objectTypeClass) {
        this.objectTypeClass = objectTypeClass;
    }

    /**
     * Top-level path is assumed.
     */
    public PropertyDelta findPropertyDelta(QName propertyName) {
        return findPropertyDelta(new PropertyPath(propertyName));
    }

    public PropertyDelta findPropertyDelta(PropertyPath parentPath, QName propertyName) {
        return findPropertyDelta(new PropertyPath(parentPath, propertyName));
    }
    
    public PropertyDelta findPropertyDelta(PropertyPath propertyPath) {
    	return findItemDelta(propertyPath, PropertyDelta.class, PrismProperty.class);
    }
    
    public <X extends Containerable> ContainerDelta<X> findContainerDelta(PropertyPath propertyPath) {
    	return findItemDelta(propertyPath, ContainerDelta.class, PrismContainer.class);
    }

    public <X extends Containerable> ContainerDelta<X> findContainerDelta(QName name) {
    	return findContainerDelta(new PropertyPath(name));
    }

    private <D extends ItemDelta> D findModification(PropertyPath propertyPath, Class<D> deltaType) {
        if (modifications == null) {
            return null;
        }
        for (ItemDelta delta : modifications) {
            if (deltaType.isAssignableFrom(delta.getClass()) && delta.getPath().equals(propertyPath)) {
                return (D) delta;
            }
        }
        return null;
    }
    
    private  <D extends ItemDelta> D findModification(QName itemName, Class<D> deltaType) {
    	return findModification(new PropertyPath(itemName), deltaType);
    }
    
    public ReferenceDelta findReferenceModification(QName itemName) {
    	return findModification(itemName, ReferenceDelta.class);
    }

    public boolean isEmpty() {
    	if (getChangeType() == ChangeType.DELETE) {
    		// Delete delta is never empty
    		return false;
    	}
        return (objectToAdd == null && (modifications == null || modifications.isEmpty()));
    }
    
    public void applyDefinition(PrismObjectDefinition<T> definition) throws SchemaException {
    	if (objectToAdd != null) {
    		objectToAdd.applyDefinition(definition);
    	}
    	ItemDelta.applyDefinition(getModifications(), definition);
    }

    /**
     * Semi-deep clone.
     */
    public ObjectDelta<T> clone() {
        ObjectDelta<T> clone = new ObjectDelta<T>(this.objectTypeClass, this.changeType);
        clone.oid = this.oid;
        clone.modifications = createEmptyModifications();
        clone.modifications.addAll((Collection)this.modifications);
        if (this.objectToAdd == null) {
            clone.objectToAdd = null;
        } else {
            clone.objectToAdd = this.objectToAdd.clone();
        }
        return clone;
    }

    /**
     * Merge provided delta into this delta.
     * This delta is assumed to be chronologically earlier.
     */
    public void merge(ObjectDelta<T> deltaToMerge) throws SchemaException {
        if (changeType == ChangeType.ADD) {
            if (deltaToMerge.changeType == ChangeType.ADD) {
                // Maybe we can, be we do not want. This is usually an error anyway.
                throw new IllegalArgumentException("Cannot merge two ADD deltas: " + this + ", " + deltaToMerge);
            } else if (deltaToMerge.changeType == ChangeType.MODIFY) {
                if (objectToAdd == null) {
                    throw new IllegalStateException("objectToAdd is null");
                }
                deltaToMerge.applyTo(objectToAdd);
            } else if (deltaToMerge.changeType == ChangeType.DELETE) {
                this.changeType = ChangeType.DELETE;
            }
        } else if (changeType == ChangeType.MODIFY) {
            if (deltaToMerge.changeType == ChangeType.ADD) {
                throw new IllegalArgumentException("Cannot merge 'add' delta to a 'modify' object delta");
            } else if (deltaToMerge.changeType == ChangeType.MODIFY) {
                // TODO: merge changes to the object to create
                throw new UnsupportedOperationException();
            } else if (deltaToMerge.changeType == ChangeType.DELETE) {
                this.changeType = ChangeType.DELETE;
            }
        } else { // DELETE
            if (deltaToMerge.changeType == ChangeType.ADD) {
                this.changeType = ChangeType.ADD;
                // TODO: clone?
                this.objectToAdd = deltaToMerge.objectToAdd;
            } else if (deltaToMerge.changeType == ChangeType.MODIFY) {
                // Just ignore the modification of a deleted object
            } else if (deltaToMerge.changeType == ChangeType.DELETE) {
                // Nothing to do
            }
        }
    }

    /**
     * Union of several object deltas. The deltas are merged to create a single delta
     * that contains changes from all the deltas.
     */
    public static <T extends Objectable> ObjectDelta<T> union(ObjectDelta<T>... deltas) throws SchemaException {
        List<ObjectDelta<T>> modifyDeltas = new ArrayList<ObjectDelta<T>>(deltas.length);
        ObjectDelta<T> addDelta = null;
        ObjectDelta<T> deleteDelta = null;
        for (ObjectDelta<T> delta : deltas) {
            if (delta == null) {
                continue;
            }
            if (delta.changeType == ChangeType.MODIFY) {
                modifyDeltas.add(delta);
            } else if (delta.changeType == ChangeType.ADD) {
                if (addDelta != null) {
                    // Maybe we can, be we do not want. This is usually an error anyway.
                    throw new IllegalArgumentException("Cannot merge two add deltas: " + addDelta + ", " + delta);
                }
                addDelta = delta;
            } else if (delta.changeType == ChangeType.DELETE) {
                deleteDelta = delta;
            }

        }

        if (deleteDelta != null && addDelta == null) {
            // Merging DELETE with anything except ADD is still a DELETE
            return deleteDelta.clone();
        }

        if (deleteDelta != null && addDelta != null) {
            throw new IllegalArgumentException("Cannot merge add and delete deltas: " + addDelta + ", " + deleteDelta);
        }

        if (addDelta != null) {
            return mergeToDelta(addDelta, modifyDeltas);
        } else {
            if (modifyDeltas.size() == 0) {
                return null;
            }
            if (modifyDeltas.size() == 1) {
                return modifyDeltas.get(0);
            }
            return mergeToDelta(modifyDeltas.get(0), modifyDeltas.subList(1, modifyDeltas.size()));
        }
    }

    private static <T extends Objectable> ObjectDelta<T> mergeToDelta(ObjectDelta<T> firstDelta,
            List<ObjectDelta<T>> modifyDeltas) throws SchemaException {
        if (modifyDeltas.size() == 0) {
            return firstDelta;
        }
        ObjectDelta<T> delta = firstDelta.clone();
        for (ObjectDelta<T> modifyDelta : modifyDeltas) {
            if (modifyDelta == null) {
                continue;
            }
            if (modifyDelta.changeType != ChangeType.MODIFY) {
                throw new IllegalArgumentException("Can only merge MODIFY changes, got " + modifyDelta.changeType);
            }
            delta.mergeModifications(modifyDelta.modifications);
        }
        return delta;
    }

    private void mergeModifications(Collection<? extends ItemDelta> modificationsToMerge) throws SchemaException {
        for (ItemDelta propDelta : modificationsToMerge) {
            if (changeType == ChangeType.ADD) {
                propDelta.applyTo(objectToAdd);
            } else if (changeType == ChangeType.MODIFY) {
            	ItemDelta myDelta = findModification(propDelta.getPath(), ItemDelta.class);
                if (myDelta == null) {
                    ((Collection)modifications).add(propDelta);
                } else {
                    myDelta.merge(propDelta);
                }
            } // else it is DELETE. There's nothing to do. Merging anything to delete is still delete
        }
    }


    /**
     * Applies this object delta to specified object, returns updated object.
     * It modifies the provided object.
     */
    public void applyTo(PrismObject<T> targetObject) throws SchemaException {
    	if (isEmpty()) {
    		// nothing to do
    		return;
    	}
        if (changeType != ChangeType.MODIFY) {
            throw new IllegalStateException("Can apply only MODIFY delta to object, got " + changeType + " delta");
        }
        for (ItemDelta itemDelta : modifications) {
            itemDelta.applyTo(targetObject);
        }
    }

    /**
     * Applies this object delta to specified object, returns updated object.
     * It leaves the original object unchanged.
     *
     * @param objectOld object before change
     * @return object with applied changes or null if the object should not exit (was deleted)
     */
    public PrismObject<T> computeChangedObject(PrismObject<T> objectOld) throws SchemaException {
        if (objectOld == null) {
            if (getChangeType() == ChangeType.ADD) {
                objectOld = getObjectToAdd();
            } else {
                //throw new IllegalStateException("Cannot apply "+getChangeType()+" delta to a null old object");
                // This seems to be quite OK
                return null;
            }
        }
        if (getChangeType() == ChangeType.DELETE) {
            return null;
        }
        // MODIFY change
        PrismObject<T> objectNew = objectOld.clone();
        for (ItemDelta modification : modifications) {
            modification.applyTo(objectNew);
        }
        return objectNew;
    }

    /**
     * Incorporates the property delta into the existing property deltas
     * (regardless of the change type).
     */
    public void swallow(PropertyDelta newPropertyDelta) throws SchemaException {
        if (changeType == ChangeType.MODIFY) {
            // TODO: check for conflict
            addModification(newPropertyDelta);
        } else if (changeType == ChangeType.ADD) {
//        	Class<?> valueClass = newPropertyDelta.getValueClass();
            PrismProperty<?> property = objectToAdd.findOrCreateProperty(new PropertyPath(newPropertyDelta.getParentPath(), newPropertyDelta.getName()));
            newPropertyDelta.applyTo(property);
        }
        // nothing to do for DELETE
    }

    private Collection<PropertyDelta> createEmptyModifications() {
    	// Lists are easier to debug
        return new ArrayList<PropertyDelta>();
    }
    
    public <X> PropertyDelta<X> createPropertyModification(QName name, PrismPropertyDefinition propertyDefinition) {
    	PropertyDelta<X> propertyDelta = new PropertyDelta<X>(name, propertyDefinition);
    	addModification(propertyDelta);
    	return propertyDelta;
    }
    
    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static <O extends Objectable, X> ObjectDelta<O> createModificationReplaceProperty(Class<O> type, String oid, QName propertyName,
    		PrismContext prismContext, X... propertyValues) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY);
    	objectDelta.setOid(oid);
    	PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    	PrismPropertyDefinition propDef = objDef.findPropertyDefinition(propertyName);
    	PropertyDelta<X> propertyDelta = objectDelta.createPropertyModification(propertyName, propDef);
    	Collection<PrismPropertyValue<X>> valuesToReplace = new ArrayList<PrismPropertyValue<X>>(propertyValues.length);
    	for (X val: propertyValues) {
    		PrismPropertyValue<X> pval = new PrismPropertyValue<X>(val);
    		valuesToReplace.add(pval);
    	}
    	propertyDelta.setValuesToReplace(valuesToReplace);
    	return objectDelta;
    }
    
    public static <T extends Objectable> ObjectDelta<T> createModifyDelta(String oid, Collection<? extends ItemDelta> modifications,
    		Class<T> objectTypeClass) {
    	ObjectDelta<T> objectDelta = new ObjectDelta<T>(objectTypeClass, ChangeType.MODIFY);
    	objectDelta.addModifications(modifications);
    	objectDelta.setOid(oid);
    	return objectDelta;
    }
    
    public void checkConsistence() {
    	checkConsistence(true);
    }
    
    public void checkConsistence(boolean requireOid) {
    	if (getChangeType() == ChangeType.ADD) {
			if (getModifications() != null && !getModifications().isEmpty()) {
				throw new IllegalStateException("Modifications present in ADD delta "+this);
			}
			if (getObjectToAdd() != null) {
				PrismAsserts.assertParentConsistency(getObjectToAdd());
			} else {
				throw new IllegalStateException("User primary delta is ADD, but there is not object to add in "+this);
			}
		} else if (getChangeType() == ChangeType.MODIFY) {
	    	if (requireOid && getOid() == null) {
	    		throw new IllegalStateException("Null oid in delta "+this);
	    	}
			if (getObjectToAdd() != null) {
				throw new IllegalStateException("Object to add present in MODIFY delta "+this);
			}
			if (getModifications() == null) {
				throw new IllegalStateException("Null modification in MODIFY delta "+this);
			}
			ItemDelta.checkConsistence(getModifications());
		} else if (getChangeType() == ChangeType.DELETE) {
	    	if (requireOid && getOid() == null) {
	    		throw new IllegalStateException("Null oid in delta "+this);
	    	}
			if (getObjectToAdd() != null) {
				throw new IllegalStateException("Object to add present in DELETE delta "+this);
			}
			if (getModifications() != null && !getModifications().isEmpty()) {
				throw new IllegalStateException("Modifications present in DELETE delta "+this);
			}			
		} else {
			throw new IllegalStateException("Unknown change type "+getChangeType()+" in delta "+this);
		}
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ObjectDelta(oid=");
        sb.append(oid).append(",").append(changeType).append(": ");
        if (changeType == ChangeType.ADD) {
            if (objectToAdd == null) {
                sb.append("null");
            } else {
                sb.append(objectToAdd.toString());
            }
        } else if (changeType == ChangeType.MODIFY) {
            Iterator<? extends ItemDelta> i = modifications.iterator();
            while (i.hasNext()) {
                sb.append(i.next().toString());
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        // Nothing to print for delete
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ObjectDelta<").append(objectTypeClass.getSimpleName()).append(">:");
        sb.append(oid).append(",").append(changeType).append("):\n");
        if (changeType == ChangeType.ADD) {
            if (objectToAdd == null) {
            	DebugUtil.indentDebugDump(sb, indent + 1);
                sb.append("null");
            } else {
                sb.append(objectToAdd.debugDump(indent + 1));
            }
        } else if (changeType == ChangeType.MODIFY) {
            Iterator<? extends ItemDelta> i = modifications.iterator();
            while (i.hasNext()) {
                sb.append(i.next().debugDump(indent + 1));
                if (i.hasNext()) {
                    sb.append("\n");
                }
            }
        }
        // Nothing to print for delete
        return sb.toString();
    }

    @Override
    public String dump() {
        return debugDump(0);
    }

}
