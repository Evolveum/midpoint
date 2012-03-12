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
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class ReferenceDelta extends ItemDelta<PrismReferenceValue> {

	public ReferenceDelta(PrismReferenceDefinition itemDefinition) {
		super(itemDefinition);
	}

	public ReferenceDelta(PropertyPath propertyPath, PrismReferenceDefinition itemDefinition) {
		super(propertyPath, itemDefinition);
	}

	public ReferenceDelta(PropertyPath parentPath, QName name, PrismReferenceDefinition itemDefinition) {
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
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
		if (!(definition instanceof PrismReferenceDefinition)) {
			throw new IllegalArgumentException("Cannot apply definition "+definition+" to reference delta "+this);
		}
		super.applyDefinition(definition);
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
    
    public static ReferenceDelta createModificationReplace(QName refName, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(refName, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(refName, referenceDefinition);
    	referenceDelta.setValueToReplace(refValue);
    	return referenceDelta;
    }
    
    public static Collection<? extends ItemDelta> createModificationAddCollection(QName propertyName,
    		PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
    	Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
    	ReferenceDelta delta = createModificationAdd(propertyName, objectDefinition, refValue);
    	((Collection)modifications).add(delta);
    	return modifications;
    }
    
    public static ReferenceDelta createModificationAdd(QName refName, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(refName, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(refName, referenceDefinition);
    	referenceDelta.addValueToAdd(refValue);
    	return referenceDelta;
    }

    public static Collection<? extends ItemDelta> createModificationDeleteCollection(QName propertyName,
    		PrismObjectDefinition<?> objectDefinition, PrismReferenceValue refValue) {
    	Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
    	ReferenceDelta delta = createModificationDelete(propertyName, objectDefinition, refValue);
    	((Collection)modifications).add(delta);
    	return modifications;
    }
    
    public static ReferenceDelta createModificationDelete(QName refName, PrismObjectDefinition<?> objectDefinition,
    		PrismReferenceValue refValue) {
    	PrismReferenceDefinition referenceDefinition = objectDefinition.findItemDefinition(refName, PrismReferenceDefinition.class);
    	ReferenceDelta referenceDelta = new ReferenceDelta(refName, referenceDefinition);
    	referenceDelta.addValueToDelete(refValue);
    	return referenceDelta;
    }

}
