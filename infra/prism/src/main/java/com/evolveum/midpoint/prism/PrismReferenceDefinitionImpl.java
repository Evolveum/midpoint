/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.util.QNameUtil;
import org.jetbrains.annotations.NotNull;

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
public class PrismReferenceDefinitionImpl extends ItemDefinitionImpl<PrismReference> implements PrismReferenceDefinition {

	private static final long serialVersionUID = 2427488779612517600L;
	private QName targetTypeName;
	private QName compositeObjectElementName;
	private boolean isComposite = false;

	public PrismReferenceDefinitionImpl(QName elementName, QName typeName, PrismContext prismContext) {
		super(elementName, typeName, prismContext);
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
	@Override
	public QName getTargetTypeName() {
		return targetTypeName;
	}

	public void setTargetTypeName(QName targetTypeName) {
		this.targetTypeName = targetTypeName;
	}

	@Override
	public QName getCompositeObjectElementName() {
		return compositeObjectElementName;
	}

	public void setCompositeObjectElementName(QName compositeObjectElementName) {
		this.compositeObjectElementName = compositeObjectElementName;
	}
	
	@Override
	public boolean isComposite() {
		return isComposite;
	}

	public void setComposite(boolean isComposite) {
		this.isComposite = isComposite;
	}

	@Override
	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
		return isValidFor(elementQName, clazz, false);
	}

	@Override
	public boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition> clazz, boolean caseInsensitive) {
		if (!clazz.isAssignableFrom(this.getClass())) {
    		return false;
    	}
        if (QNameUtil.match(elementQName, getName(), caseInsensitive)) {
        	return true;
        }
        if (QNameUtil.match(elementQName, getCompositeObjectElementName(), caseInsensitive)) {
        	return true;
        }
        return false;
	}

	@Override
	public <T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
		if (path.isEmpty() || !(path.first() instanceof ObjectReferencePathSegment)) {
			return super.findItemDefinition(path, clazz);
		} else {
			ItemPath rest = path.rest();
			PrismObjectDefinition referencedObjectDefinition = getSchemaRegistry().determineReferencedObjectDefinition(targetTypeName, rest);
			return (T) referencedObjectDefinition.findItemDefinition(rest, clazz);
		}
	}

	@NotNull
	@Override
    public PrismReference instantiate() {
        return instantiate(getName());
    }

    @NotNull
	@Override
    public PrismReference instantiate(QName name) {
        name = addNamespaceIfApplicable(name);
        return new PrismReference(name, this, prismContext);
    }
    
    @Override
	public ItemDelta createEmptyDelta(ItemPath path) {
		return new ReferenceDelta(path, this, prismContext);
	}

	@NotNull
	@Override
	public PrismReferenceDefinition clone() {
    	PrismReferenceDefinitionImpl clone = new PrismReferenceDefinitionImpl(getName(), getTypeName(), getPrismContext());
    	copyDefinitionData(clone);
    	return clone;
	}
    
    protected void copyDefinitionData(PrismReferenceDefinitionImpl clone) {
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

    @Override
    public String getDocClassName() {
        return "reference";
    }

	@Override
	protected void extendToString(StringBuilder sb) {
		super.extendToString(sb);
		if (isComposite) {
			sb.append(",composite");
		}
	}
}
