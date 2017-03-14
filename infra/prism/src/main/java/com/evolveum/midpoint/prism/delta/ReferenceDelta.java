/*
 * Copyright (c) 2010-2017 Evolveum
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

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class ReferenceDelta extends ItemDelta<PrismReferenceValue,PrismReferenceDefinition> {

	public ReferenceDelta(PrismReferenceDefinition itemDefinition, PrismContext prismContext) {
		super(itemDefinition, prismContext);
	}

	public ReferenceDelta(ItemPath propertyPath, PrismReferenceDefinition itemDefinition, PrismContext prismContext) {
		super(propertyPath, itemDefinition, prismContext);
	}

	public ReferenceDelta(ItemPath parentPath, QName name, PrismReferenceDefinition itemDefinition, PrismContext prismContext) {
		super(parentPath, name, itemDefinition, prismContext);
	}

	public ReferenceDelta(QName name, PrismReferenceDefinition itemDefinition, PrismContext prismContext) {
		super(name, itemDefinition, prismContext);
	}
	
	@Override
	public Class<PrismReference> getItemClass() {
		return PrismReference.class;
	}

	@Override
	public void setDefinition(PrismReferenceDefinition definition) {
		if (!(definition instanceof PrismReferenceDefinition)) {
			throw new IllegalArgumentException("Cannot apply "+definition+" to reference delta");
		}
		super.setDefinition(definition);
	}

	@Override
	public void applyDefinition(PrismReferenceDefinition definition) throws SchemaException {
		if (!(definition instanceof PrismReferenceDefinition)) {
			throw new IllegalArgumentException("Cannot apply definition "+definition+" to reference delta "+this);
		}
		super.applyDefinition(definition);
	}
	
	@Override
	protected boolean isApplicableToType(Item item) {
		return item instanceof PrismReference;
	}
	
	@Override
	public ReferenceDelta clone() {
		ReferenceDelta clone = new ReferenceDelta(getPath(), (PrismReferenceDefinition)getDefinition(), getPrismContext());
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

    public static <O extends Objectable> ReferenceDelta createModificationReplace(QName refName, Class<O> type, PrismContext ctx , String oid) {
    	PrismObjectDefinition<O> objectDefinition = ctx.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createModificationReplace(refName, objectDefinition, new PrismReferenceValue(oid));
	}
    
    public static <O extends Objectable> ReferenceDelta createModificationReplace(ItemPath path, Class<O> type, PrismContext ctx, String oid) {
    	PrismObjectDefinition<O> objectDefinition = ctx.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		return createModificationReplace(path, objectDefinition, new PrismReferenceValue(oid));
	}

    public static ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
    	if (refValue == null) {
    		referenceDelta.setValueToReplace();
    	} else {
    		referenceDelta.setValueToReplace(refValue);
    	}
    	return referenceDelta;
    }

    public static ReferenceDelta createModificationReplace(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                                                           Collection<PrismReferenceValue> refValues) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
        ReferenceDelta referenceDelta = new ReferenceDelta(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        referenceDelta.setValuesToReplace(refValues);
        return referenceDelta;
    }
    
    public static ReferenceDelta createModificationReplace(QName refName, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(refName, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(refName, referenceDefinition, objectDefinition.getPrismContext());              // hoping the prismContext is there
    	referenceDelta.setValueToReplace(refValue);
    	return referenceDelta;
    }

    public static Collection<? extends ItemDelta> createModificationAddCollection(QName propertyName,
    		PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
    	Collection<? extends ItemDelta<?,?>> modifications = createModificationsCollection(1);
    	ReferenceDelta delta = createModificationAdd(propertyName, objectDefinition, refValue);
    	((Collection)modifications).add(delta);
    	return modifications;
    }
    
	private static Collection<? extends ItemDelta<?,?>> createModificationsCollection(int initSize) {
		return new ArrayList<ItemDelta<?,?>>(initSize);
	}

	public static ReferenceDelta createModificationAdd(QName refName, PrismObjectDefinition<?> objectDefinition,
    		String oid) {
		return createModificationAdd(refName, objectDefinition, new PrismReferenceValue(oid));
	}

    public static ReferenceDelta createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                                                       String oid) {
        return createModificationAdd(path, objectDefinition, new PrismReferenceValue(oid));
    }

    public static ReferenceDelta createModificationAdd(QName refName, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(refName, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(refName, referenceDefinition, objectDefinition.getPrismContext());          // hoping the prismContext is there
    	referenceDelta.addValueToAdd(refValue);
    	return referenceDelta;
    }

    public static ReferenceDelta createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                                                       PrismReferenceValue refValue) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
        ReferenceDelta referenceDelta = new ReferenceDelta(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        referenceDelta.addValueToAdd(refValue);
        return referenceDelta;
    }

    public static ReferenceDelta createModificationAdd(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                                                       Collection<PrismReferenceValue> refValues) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
        ReferenceDelta referenceDelta = new ReferenceDelta(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        referenceDelta.addValuesToAdd(refValues);
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
    	Collection<? extends ItemDelta<?,?>> modifications = createModificationsCollection(1);
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
    	Collection<? extends ItemDelta<?,?>> modifications = createModificationsCollection(1);
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

    public static ReferenceDelta createModificationDelete(ItemPath path, PrismObjectDefinition<?> objectDefinition,
                                                           Collection<PrismReferenceValue> refValues) {
        PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(path, PrismReferenceDefinition.class);
        ReferenceDelta referenceDelta = new ReferenceDelta(path, referenceDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
        referenceDelta.addValuesToDelete(refValues);
        return referenceDelta;
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
    	ReferenceDelta referenceDelta = new ReferenceDelta(refName, referenceDefinition, objectDefinition.getPrismContext());              // hoping the prismContext is there
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
    	Collection<? extends ItemDelta<?,?>> modifications = createModificationsCollection(1);
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
    	Collection<? extends ItemDelta<?,?>> modifications = createModificationsCollection(1);
    	ReferenceDelta delta = createModificationDelete(type, refName, prismContext, refTarget);
    	((Collection)modifications).add(delta);
    	return modifications;
    }

}
