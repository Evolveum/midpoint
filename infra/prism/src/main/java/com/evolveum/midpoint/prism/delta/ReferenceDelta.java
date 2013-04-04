/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.delta;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class ReferenceDelta extends ItemDelta<PrismReferenceValue> {

	public ReferenceDelta(PrismReferenceDefinition itemDefinition) {
		super(itemDefinition);
	}

	public ReferenceDelta(ItemPath propertyPath, PrismReferenceDefinition itemDefinition) {
		super(propertyPath, itemDefinition);
	}

	public ReferenceDelta(ItemPath parentPath, QName name, PrismReferenceDefinition itemDefinition) {
		super(parentPath, name, itemDefinition);
	}

	public ReferenceDelta(QName name, PrismReferenceDefinition itemDefinition) {
		super(name, itemDefinition);
	}
	
	@Override
	public Class<PrismReference> getItemClass() {
		return PrismReference.class;
	}

	@Override
	public void setDefinition(ItemDefinition definition) {
		if (!(definition instanceof PrismReferenceDefinition)) {
			throw new IllegalArgumentException("Cannot apply "+definition+" to reference delta");
		}
		super.setDefinition(definition);
	}

	@Override
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
		if (!(definition instanceof PrismReferenceDefinition)) {
			throw new IllegalArgumentException("Cannot apply definition "+definition+" to reference delta "+this);
		}
		super.applyDefinition(definition);
	}

	@Override
	public void applyTo(Item item) throws SchemaException {
		if (!(item instanceof PrismReference)) {
			throw new SchemaException("Cannot apply reference delta "+this+" to item "+item+" of type "+item.getClass());
		}
		super.applyTo(item);
	}
	
	@Override
	public ReferenceDelta clone() {
		ReferenceDelta clone = new ReferenceDelta(getPath(), (PrismReferenceDefinition)getDefinition());
		copyValues(clone);
		return clone;
	}
	
	protected void copyValues(ReferenceDelta clone) {
		super.copyValues(clone);
	}

	/**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static Collection<? extends ItemDelta> createModificationReplaceCollection(QName propertyName,
    		PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
    	Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
    	ReferenceDelta delta = createModificationReplace(propertyName, objectDefinition, refValue);
    	((Collection)modifications).add(delta);
    	return modifications;
    }
    
    public static ReferenceDelta createModificationReplace(QName refName, PrismObjectDefinition<?> objectDefinition, String oid) {
		return createModificationReplace(refName, objectDefinition, new PrismReferenceValue(oid));
	}
    
    public static ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition, String oid) {
		return createModificationReplace(path, objectDefinition, new PrismReferenceValue(oid));
	}
    
    
    public static ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(path, referenceDefinition);
    	referenceDelta.setValueToReplace(refValue);
    	return referenceDelta;
    }
    
    public static ReferenceDelta createModificationReplace(QName refName, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(refName, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(refName, referenceDefinition);
    	referenceDelta.setValueToReplace(refValue);
    	return referenceDelta;
    }
    
    public static Collection<? extends ItemDelta> createModificationAddCollection(QName propertyName,
    		PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
    	Collection<? extends ItemDelta<?>> modifications = createModificationsCollection(1);
    	ReferenceDelta delta = createModificationAdd(propertyName, objectDefinition, refValue);
    	((Collection)modifications).add(delta);
    	return modifications;
    }
    
	private static Collection<? extends ItemDelta<?>> createModificationsCollection(int initSize) {
		return new ArrayList<ItemDelta<?>>(initSize);
	}

	public static ReferenceDelta createModificationAdd(QName refName, PrismObjectDefinition<?> objectDefinition,
    		String oid) {
		return createModificationAdd(refName, objectDefinition, new PrismReferenceValue(oid));
	}
	
	public static ReferenceDelta createModificationAdd(QName refName, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(refName, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(refName, referenceDefinition);
    	referenceDelta.addValueToAdd(refValue);
    	return referenceDelta;
    }

    public static <T extends Objectable> ReferenceDelta createModificationAdd(Class<T> type, QName refName, PrismContext prismContext,
    		PrismReferenceValue refValue) {
    	PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    	return createModificationAdd(refName, objectDefinition, refValue);
    }
    
    public static <T extends Objectable> Collection<? extends ItemDelta> createModificationAddCollection(Class<T> type, QName refName, PrismContext prismContext,
    		String targetOid) { 
    	PrismReferenceValue refValue = new PrismReferenceValue(targetOid);
		return createModificationAddCollection(type, refName, prismContext, refValue );
    }
    	    
    public static <T extends Objectable> Collection<? extends ItemDelta> createModificationAddCollection(Class<T> type, QName refName, PrismContext prismContext,
    		PrismReferenceValue refValue) { 
    	Collection<? extends ItemDelta<?>> modifications = createModificationsCollection(1);
    	ReferenceDelta delta = createModificationAdd(type, refName, prismContext, refValue);
    	((Collection)modifications).add(delta);
    	return modifications;
    }

    public static <T extends Objectable> ReferenceDelta createModificationAdd(Class<T> type, QName refName, PrismContext prismContext,
    		PrismObject<?> refTarget) {
    	PrismReferenceValue refValue = PrismReferenceValue.createFromTarget(refTarget);
    	return createModificationAdd(type, refName, prismContext, refValue);
    }

    public static <T extends Objectable> Collection<? extends ItemDelta> createModificationAddCollection(Class<T> type, QName refName, PrismContext prismContext,
    		PrismObject<?> refTarget) { 
    	Collection<? extends ItemDelta<?>> modifications = createModificationsCollection(1);
    	ReferenceDelta delta = createModificationAdd(type, refName, prismContext, refTarget);
    	((Collection)modifications).add(delta);
    	return modifications;
    }
    
    public static Collection<? extends ItemDelta> createModificationDeleteCollection(QName propertyName,
    		PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
    	Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
    	ReferenceDelta delta = createModificationDelete(propertyName, objectDefinition, refValue);
    	((Collection)modifications).add(delta);
    	return modifications;
    }
    
    public static ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
    		String oid) {
    	return createModificationDelete(refName, objectDefinition, new PrismReferenceValue(oid));
    }
    
    
    public static ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
    		PrismObject<?> refTarget) {
    	PrismReferenceValue refValue = PrismReferenceValue.createFromTarget(refTarget);
    	return createModificationDelete(refName, objectDefinition, refValue);
	}

    public static ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(refName, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(refName, referenceDefinition);
    	referenceDelta.addValueToDelete(refValue);
    	return referenceDelta;
    }
    
    public static <T extends Objectable> ReferenceDelta createModificationDelete(Class<T> type, QName refName, PrismContext prismContext,
    		PrismReferenceValue refValue) {
    	PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    	return createModificationDelete(refName, objectDefinition, refValue);
    }
    
    public static <T extends Objectable> Collection<? extends ItemDelta> createModificationDeleteCollection(Class<T> type, QName refName, PrismContext prismContext,
    		PrismReferenceValue refValue) { 
    	Collection<? extends ItemDelta<?>> modifications = createModificationsCollection(1);
    	ReferenceDelta delta = createModificationDelete(type, refName, prismContext, refValue);
    	((Collection)modifications).add(delta);
    	return modifications;
    }

    public static <T extends Objectable> ReferenceDelta createModificationDelete(Class<T> type, QName refName, PrismContext prismContext,
    		PrismObject<?> refTarget) {
    	PrismReferenceValue refValue = PrismReferenceValue.createFromTarget(refTarget);
    	return createModificationDelete(type, refName, prismContext, refValue);
    }

    public static <T extends Objectable> ReferenceDelta createModificationDelete(Class<T> type, QName refName, PrismObject<?> refTarget) {
    	PrismReferenceValue refValue = PrismReferenceValue.createFromTarget(refTarget);
    	return createModificationDelete(type, refName, refTarget.getPrismContext(), refValue);
    }
    
    public static <T extends Objectable> Collection<? extends ItemDelta> createModificationDeleteCollection(Class<T> type, QName refName, PrismContext prismContext,
    		PrismObject<?> refTarget) { 
    	Collection<? extends ItemDelta<?>> modifications = createModificationsCollection(1);
    	ReferenceDelta delta = createModificationDelete(type, refName, prismContext, refTarget);
    	((Collection)modifications).add(delta);
    	return modifications;
    }

}
