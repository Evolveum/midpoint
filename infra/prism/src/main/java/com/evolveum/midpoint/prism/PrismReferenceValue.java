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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Radovan Semancik
 */
public class PrismReferenceValue extends PrismValue implements DebugDumpable, Serializable, ShortDumpable {
	private static final long serialVersionUID = 1L;

	private static final QName F_OID = new QName(PrismConstants.NS_TYPES, "oid");
    private static final QName F_TYPE = new QName(PrismConstants.NS_TYPES, "type");
    private static final QName F_RELATION = new QName(PrismConstants.NS_TYPES, "relation");
	private String oid = null;
    private PrismObject<?> object = null;
    private QName targetType = null;
    private QName relation = null;
    private String description = null;
    private SearchFilterType filter = null;
    private EvaluationTimeType resolutionTime;
    private PolyString targetName = null;

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
		checkMutability();
		this.oid = oid;
	}

	/**
	 * Returns object that this reference points to. The object is supposed to be used
	 * for caching and optimizations. Only oid and type of the object really matters for
	 * the reference.
	 *
	 * The object is transient. It will NOT be serialized. Therefore the client must
	 * expect that the object can disappear when serialization boundary is crossed.
	 * The client must expect that the object is null.
	 */
	public PrismObject getObject() {
		return object;
	}

	public void setObject(PrismObject object) {
		checkMutability();
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

    public void setTargetType(QName targetType) {
        setTargetType(targetType, false);
    }

    /**
     * @param targetType
     * @param allowEmptyNamespace This is an ugly hack. See comment in DOMUtil.validateNonEmptyQName.
     */
	public void setTargetType(QName targetType, boolean allowEmptyNamespace) {
		checkMutability();
		// Null value is OK
		if (targetType != null) {
			// But non-empty is not ..
			Itemable item = getParent();
			DOMUtil.validateNonEmptyQName(targetType, " in target type in reference "+ (item == null ? "(unknown)" : item.getElementName()), allowEmptyNamespace);
		}
		this.targetType = targetType;
	}

	/**
	 * Returns cached name of the target object.
     * This is a ephemeral value. It is usually not stored.
     * It may be computed at object retrieval time or it may not be present at all.
     * This is NOT an authoritative information. Setting it or changing it will
     * not influence the reference meaning. OID is the only authoritative linking
     * mechanism.
	 * @return cached name of the target object.
	 */
	public PolyString getTargetName() {
		if (targetName != null) {
			return targetName;
		}
		if (object != null) {
			return object.getName();
		}
		return null;
	}

	public void setTargetName(PolyString name) {
		checkMutability();
		this.targetName = name;
	}

	public void setTargetName(PolyStringType name) {
		checkMutability();
		if (name == null) {
			this.targetName = null;
		} else {
			this.targetName = name.toPolyString();
		}
	}

	// The PRV (this object) should have a parent with a prism context
    public Class<Objectable> getTargetTypeCompileTimeClass() {
		PrismContext prismContext = getPrismContext();
		return prismContext != null ? getTargetTypeCompileTimeClass(prismContext) : null;
	}

    public Class<Objectable> getTargetTypeCompileTimeClass(PrismContext prismContext) {
        QName type = getTargetType();
        if (type == null) {
            return null;
        } else {
			PrismObjectDefinition<Objectable> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
			return objDef != null ? objDef.getCompileTimeClass() : null;
        }
    }

    public QName getRelation() {
		return relation;
	}

	public void setRelation(QName relation) {
		checkMutability();
		this.relation = relation;
	}

	public PrismReferenceValue relation(QName relation) {
		setRelation(relation);
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		checkMutability();
		this.description = description;
	}

	public SearchFilterType getFilter() {
		return filter;
	}

	public void setFilter(SearchFilterType filter) {
		checkMutability();
		this.filter = filter;
	}

	public EvaluationTimeType getResolutionTime() {
		return resolutionTime;
	}

	public void setResolutionTime(EvaluationTimeType resolutionTime) {
		checkMutability();
		this.resolutionTime = resolutionTime;
	}

	@Override
	public PrismReferenceDefinition getDefinition() {
		return (PrismReferenceDefinition) super.getDefinition();
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
	public <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path) {
		if (path == null || path.isEmpty()) {
			return new PartiallyResolvedItem<>((Item<IV, ID>) getParent(), null);
		}
		return new PartiallyResolvedItem<>((Item<IV, ID>) getParent(), path);
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
		object.applyDefinition((PrismObjectDefinition)objectDefinition, force);
	}

	@Override
	public void recompute(PrismContext prismContext) {
		// Nothing to do
	}

	@Override
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
        if (!scope.isThorough()) {
            return;
        }

		ItemPath myPath = getPath();

		if (StringUtils.isBlank(oid) && object == null && filter == null) {
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
		return oid == null && object == null && filter == null && relation == null && targetType == null;
	}

	/**
	 * Returns a version of this value that is canonical, that means it has the minimal form.
	 * E.g. it will have only OID and no object.
	 */
	public PrismReferenceValue toCanonical() {
		PrismReferenceValue can = new PrismReferenceValue();
		can.setOid(getOid());
		// do NOT copy object
		can.setTargetType(getTargetType());
		can.setRelation(getRelation());
		can.setFilter(getFilter());
		can.setResolutionTime(getResolutionTime());
		can.setDescription(getDescription());
		return can;
	}

	@Override
	public boolean equalsComplex(PrismValue other, boolean ignoreMetadata, boolean isLiteral) {
		return other instanceof PrismReferenceValue
				&& equalsComplex((PrismReferenceValue) other, ignoreMetadata, isLiteral);
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
		boolean bothOidsNull = this.oid == null && other.oid == null;
		if (bothOidsNull) {
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
		if (!equalsTargetType(other)) {
			return false;
		}
		if (!relationsEquivalent(relation, other.relation, isLiteral)) {
			return false;
		}
		if ((isLiteral || bothOidsNull) && !filtersEquivalent(filter, other.filter)) {
			return false;
		}
		return true;
	}

	private boolean filtersEquivalent(SearchFilterType filter1, SearchFilterType filter2) {
		if (filter1 == null && filter2 == null) {
			return true;
		} else if (filter1 == null || filter2 == null) {
			return false;
		} else {
			return filter1.equals(filter2);
		}
	}

	private boolean relationsEquivalent(QName r1, QName r2, boolean isLiteral) {
		return QNameUtil.match(normalizedRelation(r1, isLiteral), normalizedRelation(r2, isLiteral));
	}

	private QName normalizedRelation(QName r, boolean isLiteral) {
		if (r != null) {
			return r;
		}
		if (isLiteral) {
			return null;
		}
		PrismContext prismContext = getPrismContext();
		return prismContext != null ? prismContext.getDefaultRelation() : null;
	}

	private boolean equalsTargetType(PrismReferenceValue other) {
		QName otherTargetType = other.getTargetType();
		if (otherTargetType == null && other.getDefinition() != null) {
			otherTargetType = other.getDefinition().getTargetTypeName();
		}
		QName thisTargetType = this.getTargetType();
		if (thisTargetType == null && this.getDefinition() != null) {
			thisTargetType = this.getDefinition().getTargetTypeName();
		}
		return QNameUtil.match(thisTargetType, otherTargetType);
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
		QName normalizedRelation = normalizedRelation(relation, false);
		if (normalizedRelation != null) {
			// Take just the local part to avoid problems with incomplete namespaces
			String relationLocal = normalizedRelation.getLocalPart();
			if (relationLocal != null) {
				result = prime * result + relationLocal.hashCode();
			}
		}
		return result;
	}

	@Override
	public boolean representsSameValue(PrismValue other, boolean lax) {
		if (other instanceof PrismReferenceValue) {
			return representsSameValue((PrismReferenceValue)other);
		} else {
			return false;
		}
	}

	public boolean representsSameValue(PrismReferenceValue other) {
		if (this.getOid() != null && other.getOid() != null) {
			return this.getOid().equals(other.getOid()) && relationsEquivalent(this.getRelation(), other.getRelation(), false);
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
			if (targetName != null) {
				sb.append(", targetName=").append(PrettyPrinter.prettyPrint(targetName.getOrig()));
			}
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
			sb.append(", filter");
		}
		if (resolutionTime != null) {
			sb.append(", resolutionTime=").append(resolutionTime);
		}
		sb.append(")");
		return sb.toString();
	}

	public Referencable asReferencable() {
		if (referencable != null) {
			return referencable;
		}

		Itemable parent = getParent();
		if (parent != null) {
			QName xsdType = parent.getDefinition().getTypeName();
			Class clazz = getPrismContext().getSchemaRegistry().getCompileTimeClass(xsdType);
			if (clazz != null) {
				try {
					referencable = (Referencable) clazz.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new SystemException("Couldn't create jaxb object instance of '" + clazz + "': " + e.getMessage(),
							e);
				}
			}
			referencable.setupReferenceValue(this);
		}

		// A hack, just to avoid crashes. TODO think about this!
		return new DefaultReferencableImpl(this);
	}

	@NotNull
	public static List<Referencable> asReferencables(@NotNull Collection<PrismReferenceValue> values) {
		return values.stream().map(prv -> prv.asReferencable()).collect(Collectors.toList());
	}

	@NotNull
	public static List<PrismReferenceValue> asReferenceValues(@NotNull Collection<? extends Referencable> referencables) {
		return referencables.stream().map(ref -> ref.asReferenceValue()).collect(Collectors.toList());
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
    	return cloneComplex(CloneStrategy.LITERAL);
	}
    
    @Override
    public PrismReferenceValue cloneComplex(CloneStrategy strategy) {
        PrismReferenceValue clone = new PrismReferenceValue(getOid(), getOriginType(), getOriginObject());
        copyValues(strategy, clone);
        return clone;
    }

	protected void copyValues(CloneStrategy strategy, PrismReferenceValue clone) {
		super.copyValues(strategy, clone);
		clone.targetType = this.targetType;
		if (this.object != null && strategy == CloneStrategy.LITERAL) {
			clone.object = this.object.clone();
		}
		clone.description = this.description;
		clone.filter = this.filter;
		clone.resolutionTime = this.resolutionTime;
        clone.relation = this.relation;
        clone.targetName = this.targetName;
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
		sb.append("oid=");
		shortDump(sb);
		return sb.toString();
	}

	@Override
	public Class<?> getRealClass() {
		return Referencable.class;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public Referencable getRealValue() {
		return asReferencable();
	}

	public static boolean containsOid(Collection<PrismReferenceValue> values, @NotNull String oid) {
		return values.stream().anyMatch(v -> oid.equals(v.getOid()));
	}

	@Override
	public void revive(PrismContext prismContext) throws SchemaException {
		super.revive(prismContext);
		if (object != null) {
			object.revive(prismContext);
		}
	}

	@Override
	public void shortDump(StringBuilder sb) {
		sb.append(oid);
		if (getTargetType() != null) {
			sb.append("(");
			sb.append(DebugUtil.formatElementName(getTargetType()));
			sb.append(")");
		}
		if (targetName != null) {
			sb.append("('").append(targetName).append("')");
		}
        if (getRelation() != null) {
            sb.append("[");
            sb.append(getRelation().getLocalPart());
            sb.append("]");
        }
		if (getObject() != null) {
			sb.append('*');
		}
	}

}
