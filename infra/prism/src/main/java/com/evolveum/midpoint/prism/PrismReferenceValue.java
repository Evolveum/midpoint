/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.w3c.dom.Element;

/**
 * @author Radovan Semancik
 */
public class PrismReferenceValue extends PrismValue implements DebugDumpable, Serializable {

    private static final QName F_OID = new QName(PrismConstants.NS_TYPES, "oid");
    private static final QName F_TYPE = new QName(PrismConstants.NS_TYPES, "type");
    private static final QName F_RELATION = new QName(PrismConstants.NS_TYPES, "relation");
	private String oid = null;
    private PrismObject<?> object = null;
    private QName targetType = null;
    private QName relation = null;
    private String description = null;
    private SearchFilterType filter = null;
    
    
    private Referencable referencable;
    
    public PrismReferenceValue() {
        this(null,null,null);
    }

    public PrismReferenceValue(String oid) {
        this(oid, null, null);
    }
    
    public PrismReferenceValue(String oid, QName targetType) {
        this(oid, null, null);
        this.targetType = targetType;
    }

    public PrismReferenceValue(String oid, OriginType type, Objectable source) {
    	super(type,source);
        this.oid = oid;
    }

	/**
	 * OID of the object that this reference refers to (reference target).
	 * 
	 * May return null, but the reference is in that case incomplete and
	 * unusable.
	 * 
	 * @return the target oid
	 */
    public String getOid() {
		if (oid != null) {
			return oid;
		}
		if (object != null) {
			return object.getOid();
		}
		return null;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}
	
	public PrismObject getObject() {
		return object;
	}

	public void setObject(PrismObject object) {
		this.object = object;
	}

	/**
	 * Returns XSD type of the object that this reference refers to. It may be
	 * used in XPath expressions and similar filters.
	 * 
	 * May return null if the type name is not set.
	 * 
	 * @return the target type name
	 */
	public QName getTargetType() {
		if (targetType != null) {
			return targetType;
		}
		if (object != null && object.getDefinition() != null) {
			return object.getDefinition().getTypeName();
		}
		return null;
	}

    public Class<Objectable> getTargetTypeCompileTimeClass() {
        QName type = getTargetType();
        if (type == null) {
            return null;
        } else {
            return getPrismContext().getSchemaRegistry().findObjectDefinitionByType(type).getCompileTimeClass();
        }
    }

	public void setTargetType(QName targetType) {
		// Null value is OK
		if (targetType != null) {
			// But non-empty is not ..
			Itemable item = getParent();
			DOMUtil.validateNonEmptyQName(targetType, " in target type in reference "+ (item == null ? "(unknown)" : item.getElementName()));
		}
		this.targetType = targetType;
	}

    public QName getRelation() {
		return relation;
	}

	public void setRelation(QName relation) {
		this.relation = relation;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SearchFilterType getFilter() {
		return filter;
	}

	public void setFilter(SearchFilterType filter) {
		this.filter = filter;
	}
	
	@Override
	public boolean isRaw() {
		// Reference value cannot be raw
		return false;
	}

	@Override
	public Object find(ItemPath path) {
		if (path == null || path.isEmpty()) {
			return this;
		}
		ItemPathSegment first = path.first();
    	if (!(first instanceof NameItemPathSegment)) {
    		throw new IllegalArgumentException("Attempt to resolve inside the reference value using a non-name path "+path+" in "+this);
    	}
    	QName subName = ((NameItemPathSegment)first).getName();
    	if (compareLocalPart(F_OID,subName)) {
    		return this.getOid();
    	} else if (compareLocalPart(F_TYPE,subName)) {
    		return this.getTargetType();
    	} else if (compareLocalPart(F_RELATION,subName)) {
    		return this.getRelation();
    	} else {
    		throw new IllegalArgumentException("Attempt to resolve inside the reference value using a unrecognized path "+path+" in "+this);
    	}
	}

	@Override
	public <X extends PrismValue> PartiallyResolvedItem<X> findPartial(ItemPath path) {
		if (path == null || path.isEmpty()) {
			return new PartiallyResolvedItem<X>((Item<X>)getParent(), null);
		}
		return new PartiallyResolvedItem<X>((Item<X>)getParent(), path);
	}

	private boolean compareLocalPart(QName a, QName b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.getLocalPart().equals(b.getLocalPart());
	}

	@Override
	public void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException {
		if (!(definition instanceof PrismReferenceDefinition)) {
			throw new IllegalArgumentException("Cannot apply "+definition+" to a reference value");
		}
		applyDefinition((PrismReferenceDefinition)definition, force);
	}

	public void applyDefinition(PrismReferenceDefinition definition, boolean force) throws SchemaException {
		super.applyDefinition(definition, force);
		if (object == null) {
			return;
		}
		if (object.getDefinition() != null && !force) {
			return;
		}
		PrismContext prismContext = definition.getPrismContext();
		QName targetTypeName = definition.getTargetTypeName();
		if (targetTypeName == null) {
			throw new SchemaException("Cannot apply definition to composite object in reference "+getParent()
					+": the target type name is not specified in the reference schema");
		}

        PrismObjectDefinition<? extends Objectable> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(object.getCompileTimeClass());
        if (objectDefinition == null) {
		    objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(targetTypeName);
        }
		if (objectDefinition == null) {
			throw new SchemaException("Cannot apply definition to composite object in reference "+getParent()
					+": no definition for object type "+targetTypeName);
		}
		// this should do it
		object.applyDefinition(objectDefinition, force);
	}

	@Override
	public void recompute(PrismContext prismContext) {
		// Nothing to do
	}

	@Override
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw) {
		ItemPath myPath = getPath();

		if (oid == null && object == null && filter == null) {
            boolean mayBeEmpty = false;
            if (getParent() != null && getParent().getDefinition() != null) {
                ItemDefinition itemDefinition = getParent().getDefinition();
                if (itemDefinition instanceof PrismReferenceDefinition) {
                    PrismReferenceDefinition prismReferenceDefinition = (PrismReferenceDefinition) itemDefinition;
                    mayBeEmpty = prismReferenceDefinition.isComposite();
                }
            }
            if (!mayBeEmpty) {
			    throw new IllegalStateException("Neither OID, object nor filter specified in reference value "+this+" ("+myPath+" in "+rootItem+")");
            }
		}

		if (object != null) {
			try {
				object.checkConsistence();
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+" in reference "+myPath+" in "+rootItem, e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+" in reference "+myPath+" in "+rootItem, e);
			}
		}
	}

	@Override
	public boolean isEmpty() {
		return oid == null && object == null;
	}
	
	/**
	 * Returns a version of this value that is cannonical, that means it has the minimal form.
	 * E.g. it will have only OID and no object.
	 */
	public PrismReferenceValue toCannonical() {
		PrismReferenceValue can = new PrismReferenceValue();
		can.setOid(getOid());
		// do NOT copy object
		can.setTargetType(getTargetType());
		can.setRelation(getRelation());
		can.setFilter(getFilter());
		can.setDescription(getDescription());
		return can;
	}

	@Override
	public boolean equalsComplex(PrismValue other, boolean ignoreMetadata, boolean isLiteral) {
		if (other == null || !(other instanceof PrismReferenceValue)) {
			return false;
		}
		return equalsComplex((PrismReferenceValue)other, ignoreMetadata, isLiteral);
	}

	public boolean equalsComplex(PrismReferenceValue other, boolean ignoreMetadata, boolean isLiteral) {
		if (!super.equalsComplex(other, ignoreMetadata, isLiteral)) {
			return false;
		}
		if (this.getOid() == null) {
			if (other.getOid() != null)
				return false;
		} else if (!this.getOid().equals(other.getOid()))
			return false;
		// Special handling: if both oids are null we need to compare embedded objects
		if (this.oid == null && other.oid == null) {
			if (this.object != null || other.object != null) {
				if (this.object == null || other.object == null) {
					// one is null the other is not
					return false;
				}
				if (!this.object.equals(other.object)) {
					return false;
				}
			}
		}
		if (this.getTargetType() == null) {
			if (other.getTargetType() != null)
				return false;
		} else if (!this.getTargetType().equals(other.getTargetType()))
			return false;
		if (this.relation == null) {
			if (other.relation != null)
				return false;
		} else if (!this.relation.equals(other.relation))
			return false;
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrismReferenceValue other = (PrismReferenceValue) obj;
		return equalsComplex(other, false, false);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((oid == null) ? 0 : oid.hashCode());
		result = prime * result + ((targetType == null) ? 0 : targetType.hashCode());
		result = prime * result + ((relation == null) ? 0 : relation.hashCode());
		return result;
	}
		
	@Override
	public boolean representsSameValue(PrismValue other) {
		if (other instanceof PrismPropertyValue) {
			return representsSameValue((PrismReferenceValue)other);
		} else {
			return false;
		}
	}
	
	public boolean representsSameValue(PrismReferenceValue other) {
		if (this.getOid() != null && other.getOid() != null) {
			return this.getOid().equals(other.getOid());
		}
		return false;
	}
	
	public static PrismReferenceValue createFromTarget(PrismObject<?> refTarget) {
		PrismReferenceValue refVal = new PrismReferenceValue(refTarget.getOid());
		refVal.setObject(refTarget);
		if (refTarget.getDefinition() != null) {
			refVal.setTargetType(refTarget.getDefinition().getTypeName());
		}
		return refVal;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PRV(");
		if (object == null) {
			sb.append("oid=").append(oid);
			sb.append(", targetType=").append(PrettyPrinter.prettyPrint(targetType));
		} else {
			sb.append("object=").append(object);
		}
		if (getRelation() != null) {
			sb.append(", relation=").append(PrettyPrinter.prettyPrint(getRelation()));
		}
		if (getOriginType() != null) {
			sb.append(", type=").append(getOriginType());
		}
		if (getOriginObject() != null) {
			sb.append(", source=").append(getOriginObject());
		}
		if (filter != null) {
			sb.append(", (filter)");
		}
		sb.append(")");
		return sb.toString();
	}

	public Referencable asReferencable(){
		if (referencable == null){
			Itemable parent = getParent();
			QName xsdType = parent.getDefinition().getTypeName();
			Class clazz = getPrismContext().getSchemaRegistry().getCompileTimeClass(xsdType);
			if (clazz != null){
				try {
					referencable = (Referencable) clazz.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new SystemException("Couldn't create jaxb object instance of '" + clazz + "': "+e.getMessage(), e);
				}
			}
			referencable.setupReferenceValue(this);
		}
		return referencable;
		
	}
	
	@Override
    public String debugDump() {
        return toString();
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, false);
    }
    
    public String debugDump(int indent, boolean expandObject) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(toString());
        if (expandObject && object != null) {
        	sb.append("\n");
        	sb.append(object.debugDump(indent + 1));
        }

        return sb.toString();
    }

    @Override
    public PrismReferenceValue clone() {
        PrismReferenceValue clone = new PrismReferenceValue(getOid(), getOriginType(), getOriginObject());
        copyValues(clone);
        return clone;
    }

	protected void copyValues(PrismReferenceValue clone) {
		super.copyValues(clone);
		clone.targetType = this.targetType;
		if (this.object != null) { 
			clone.object = this.object.clone();
		}
		clone.description = this.description;
		clone.filter = this.filter;
        clone.relation = this.relation;
	}

	@Override
	public boolean match(PrismValue otherValue) {
		return equalsRealValue(otherValue);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.PrismValue#getHumanReadableDump()
	 */
	@Override
	public String toHumanReadableString() {
		StringBuilder sb = new StringBuilder();
		sb.append("oid=").append(oid);
		if (getTargetType() != null) {
			sb.append("(");
			sb.append(DebugUtil.formatElementName(getTargetType()));
			sb.append(")");
		}
		return sb.toString();
	}
    
}
