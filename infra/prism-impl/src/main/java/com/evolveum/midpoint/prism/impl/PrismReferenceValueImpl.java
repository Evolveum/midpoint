/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Radovan Semancik
 */
public class PrismReferenceValueImpl extends PrismValueImpl implements PrismReferenceValue {
    private static final long serialVersionUID = 1L;

    private static final QName F_OID = new QName(PrismConstants.NS_TYPES, "oid");
    private static final QName F_TYPE = new QName(PrismConstants.NS_TYPES, "type");
    private static final QName F_RELATION = new QName(PrismConstants.NS_TYPES, "relation");
    private String oid;
    private PrismObject<?> object = null;
    private QName targetType = null;
    private QName relation = null;
    private String description = null;
    private SearchFilterType filter = null;
    private EvaluationTimeType resolutionTime;
    private PolyString targetName = null;

    private Referencable referencable;

    public PrismReferenceValueImpl() {
        this(null,null,null);
    }

    public PrismReferenceValueImpl(String oid) {
        this(oid, null, null);
    }

    public PrismReferenceValueImpl(String oid, QName targetType) {
        this(oid, null, null);
        this.targetType = targetType;
    }

    public PrismReferenceValueImpl(String oid, OriginType type, Objectable source) {
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

    public PrismReferenceValueImpl relation(QName relation) {
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
        Object first = path.first();
        if (!ItemPath.isName(first)) {
            throw new IllegalArgumentException("Attempt to resolve inside the reference value using a non-name path "+path+" in "+this);
        }
        ItemName subName = ItemPath.toName(first);
        if (compareLocalPart(F_OID, subName)) {
            return this.getOid();
        } else if (compareLocalPart(F_TYPE, subName)) {
            return this.getTargetType();
        } else if (compareLocalPart(F_RELATION, subName)) {
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
        PrismObjectDefinition<? extends Objectable> objectDefinition = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(object.getCompileTimeClass());
        QName targetTypeName = definition.getTargetTypeName();
        if (objectDefinition == null) {
            if (targetTypeName == null) {
                if (object.getDefinition() != null) {
                    // Target type is not specified (e.g. as in task.objectRef) but we have at least some definition;
                    // so let's keep it. TODO reconsider this
                    return;
                } else {
                    throw new SchemaException("Cannot apply definition to composite object in reference "+getParent()
                            +": the object has no present definition; it's definition cannot be determined from it's class;"
                            + "and target type name is not specified in the reference schema");
                }
            }
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

        // We allow empty references that contain only the target name (see MID-5489)
        if (StringUtils.isBlank(oid) && object == null && filter == null && targetName == null) {
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
    public PrismReferenceValueImpl toCanonical() {
        PrismReferenceValueImpl can = new PrismReferenceValueImpl();
        can.setOid(getOid());
        // do NOT copy object
        can.setTargetType(getTargetType());
        can.setRelation(getRelation());
        can.setFilter(getFilter());
        can.setResolutionTime(getResolutionTime());
        can.setDescription(getDescription());
        return can;
    }

    public boolean equals(PrismValue other, @NotNull ParameterizedEquivalenceStrategy strategy) {
        return other instanceof PrismReferenceValue && equals((PrismReferenceValue) other, strategy);
    }

    public boolean equals(PrismReferenceValue other, @NotNull ParameterizedEquivalenceStrategy strategy) {
        if (!super.equals(other, strategy)) {
            return false;
        }
        if (this.getOid() == null) {
            if (other.getOid() != null) {
                return false;
            }
        } else if (!this.getOid().equals(other.getOid())) {
            return false;
        }
        // Special handling: if both oids are null we need to compare embedded objects
        boolean bothOidsNull = this.oid == null && other.getOid() == null;
        if (bothOidsNull) {
            if (this.object != null || other.getObject() != null) {
                if (this.object == null || other.getObject() == null) {
                    // one is null the other is not
                    return false;
                }
                if (!this.object.equals(other.getObject())) {
                    return false;
                }
            }
        }
        if (!equalsTargetType(other)) {
            return false;
        }
        if (!relationsEquivalent(relation, other.getRelation(), strategy.isLiteralDomComparison())) {
            return false;
        }
        if ((strategy.isConsideringReferenceFilters() || bothOidsNull) && !filtersEquivalent(filter, other.getFilter())) {
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
        // todo use equals if isLiteral is true
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
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        PrismReferenceValue other = (PrismReferenceValue) obj;
        return equals(other, getEqualsHashCodeStrategy());
    }

    // Just to make checkstyle happy
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // TODO take strategy into account
    @Override
    public int hashCode(ParameterizedEquivalenceStrategy strategy) {
        final int prime = 31;
        int result = super.hashCode(strategy);
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
        if (parent != null && parent.getDefinition() != null) {
            QName xsdType = parent.getDefinition().getTypeName();
            Class clazz = getPrismContext() != null ? getPrismContext().getSchemaRegistry().getCompileTimeClass(xsdType) : null;
            if (clazz != null) {
                try {
                    referencable = (Referencable) clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new SystemException("Couldn't create jaxb object instance of '" + clazz + "': " + e.getMessage(),
                            e);
                }
                referencable.setupReferenceValue(this);
                return referencable;
            }
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
    public PrismReferenceValueImpl clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public PrismReferenceValueImpl cloneComplex(CloneStrategy strategy) {
        PrismReferenceValueImpl clone = new PrismReferenceValueImpl(getOid(), getOriginType(), getOriginObject());
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, PrismReferenceValueImpl clone) {
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
