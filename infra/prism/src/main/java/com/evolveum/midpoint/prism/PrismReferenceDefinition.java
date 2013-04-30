/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;


/**
 * Object Reference Schema Definition.
 * 
 * Object Reference is a property that describes reference to an object. It is
 * used to represent association between objects. For example reference from
 * User object to Account objects that belong to the user. The reference is a
 * simple uni-directional link using an OID as an identifier.
 * 
 * This type should be used for all object references so the implementations can
 * detect them and automatically resolve them.
 * 
 * This class represents schema definition for object reference. See
 * {@link Definition} for more details.
 * 
 * @author Radovan Semancik
 * 
 */
public class PrismReferenceDefinition extends ItemDefinition {

	private static final long serialVersionUID = 2427488779612517600L;
	private QName targetTypeName;
	private QName compositeObjectElementName;
	private boolean isComposite = false;

	public PrismReferenceDefinition(QName name, QName defaultName, QName typeName, PrismContext prismContext) {
		super(name, defaultName, typeName, prismContext);
	}

	/**
	 * Returns valid XSD object types whose may be the targets of the reference.
	 * 
	 * Corresponds to "targetType" XSD annotation.
	 * 
	 * Returns empty set if not specified. Must not return null.
	 * 
	 * @return set of target type names
	 */
	public QName getTargetTypeName() {
		return targetTypeName;
	}

	public void setTargetTypeName(QName targetTypeName) {
		this.targetTypeName = targetTypeName;
	}

	public QName getCompositeObjectElementName() {
		return compositeObjectElementName;
	}

	public void setCompositeObjectElementName(QName compositeObjectElementName) {
		this.compositeObjectElementName = compositeObjectElementName;
	}
	
	public boolean isComposite() {
		return isComposite;
	}

	public void setComposite(boolean isComposite) {
		this.isComposite = isComposite;
	}

	@Override
	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
		if (!clazz.isAssignableFrom(this.getClass())) {
    		return false;
    	}
        if (elementQName.equals(getName())) {
        	return true;
        }
        if (elementQName.equals(getCompositeObjectElementName())) {
        	return true;
        }
        return false;
	}
	
    @Override
    public PrismReference instantiate() {
        return instantiate(getNameOrDefaultName());
    }

    @Override
    public PrismReference instantiate(QName name) {
        return new PrismReference(name, this, prismContext);
    }
    
    @Override
	public ItemDelta createEmptyDelta(ItemPath path) {
		return new ReferenceDelta(path, this);
	}

	@Override
	public PrismReferenceDefinition clone() {
    	PrismReferenceDefinition clone = new PrismReferenceDefinition(getName(), getDefaultName(), getTypeName(), getPrismContext());
    	copyDefinitionData(clone);
    	return clone;
	}
    
    protected void copyDefinitionData(PrismReferenceDefinition clone) {
    	super.copyDefinitionData(clone);
    	clone.targetTypeName = this.targetTypeName;
    	clone.compositeObjectElementName = this.compositeObjectElementName;
    	clone.isComposite = this.isComposite;
    }

	/**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PRD";
    }
}
