/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.delta.ReferenceDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Object Reference is a property that describes reference to an object. It is
 * used to represent association between objects. For example reference from
 * User object to Account objects that belong to the user. The reference is a
 * simple uni-directional link using an OID as an identifier.
 *
 * This type should be used for all object references so the implementations can
 * detect them and automatically resolve them.
 *
 * @author semancik
 *
 */
public class PrismReferenceImpl extends ItemImpl<PrismReferenceValue, PrismReferenceDefinition> implements PrismReference {
    private static final long serialVersionUID = 1872343401395762657L;

    @SuppressWarnings("unused") // called dynamically from ItemImpl.createNewDefinitionlessItem
    public PrismReferenceImpl(QName name) {
        super(name);
    }

    PrismReferenceImpl(QName name, PrismReferenceDefinition definition, PrismContext prismContext) {
        super(name, definition, prismContext);
    }

    @NotNull
    @Override
    public PrismReferenceValue getValue() {
        // I know of no reason why we should not return a value if it's only one (even for multivalued items) (see MID-3922)
        // TODO reconsider this
        if (getValues().size() == 1) {
            return getValues().get(0);
        }
        // We are not sure about multiplicity if there is no definition or the definition is dynamic
        if (getDefinition() != null && !getDefinition().isDynamic()) {
            if (getDefinition().isMultiValue()) {
                throw new IllegalStateException("Attempt to get single value from property " + elementName
                        + " with multiple values");
            }
        }
        if (getValues().size() > 1) {
            throw new IllegalStateException("Attempt to get single value from property " + elementName
                    + " with multiple values");
        }
        if (getValues().isEmpty()) {
            // Insert first empty value. This simulates empty single-valued reference. It the reference exists
            // it is clear that it has at least one value (and that value is empty).
            PrismReferenceValue rval = new PrismReferenceValueImpl();
            try {
                add(rval);
            } catch (SchemaException e) {
                throw new IllegalStateException("Unexpected SchemaException while creating new value: " + e.getMessage(), e);
            }
            return rval;
        }
        return getValues().iterator().next();
    }

    private PrismReferenceValue getValue(String oid) {
        // We need to tolerate null OIDs here. Because of JAXB.
        for (PrismReferenceValue val: getValues()) {
            if (MiscUtil.equals(oid, val.getOid())) {
                return val;
            }
        }
        return null;
    }

    @Override
    public Referencable getRealValue() {
        return getValue().getRealValue();
    }

    @NotNull
    @Override
    public Collection<Referencable> getRealValues() {
        List<Referencable> realValues = new ArrayList<>(getValues().size());
        for (PrismReferenceValue refVal : getValues()) {
            realValues.add(refVal.asReferencable());
        }
        return realValues;
    }

    public boolean merge(PrismReferenceValue value) {
        try {
            String newOid = value.getOid();
            // We need to tolerate null OIDs here. Because of JAXB.
            PrismReferenceValue existingValue = getValue(newOid);
            if (existingValue == null) {
                return add(value);
            }

            // if there is newValue containing object (instead of oid only) and also
            // old value containing object (instead of oid only) we need to compare
            // these two object if they are equals..this can avoid of bad resolving
            // (e.g. creating user and adding two or more accounts at the same time)
            if (value.getObject() != null && existingValue.getObject() != null && !value
                    .equals(existingValue, EquivalenceStrategy.NOT_LITERAL)) {
                return add(value);
            }

            // in the value.getObject() is not null, it it probably only resolving
            // of reference, so only change oid to object
            if (value.getObject() != null) {
                existingValue.setObject(value.getObject());
                return true;
            }

            // in the case, if the existing value and new value are not equal, add
            // also another reference alhtrough one with the same oid exist. It is
            // needed for parent org refs, becasue there can exist more than one
            // reference with the same oid, but they should be different (e.g. user
            // is member and also manager of the org. unit.)
            if (!value.equals(existingValue, EquivalenceStrategy.NOT_LITERAL)) {
                return add(value);
            }

            if (value.getTargetType() != null) {
                existingValue.setTargetType(value.getTargetType());
                //            return true;
            }
            // No need to copy OID as OIDs match
            return true;
        } catch (SchemaException e) {
            // todo or publish SchemaException in the interface?
            throw new IllegalStateException("Unexpected SchemaException while merging: " + e.getMessage(), e);
        }
    }


    public String getOid() {
        return getValue().getOid();
    }

    public PolyString getTargetName() {
        return getValue().getTargetName();
    }

    public PrismReferenceValue findValueByOid(String oid) {
        for (PrismReferenceValue pval: getValues()) {
            if (oid.equals(pval.getOid())) {
                return pval;
            }
        }
        return null;
    }

    @Override
    public Object find(ItemPath path) {
        if (path == null || path.isEmpty()) {
            return this;
        }
        if (!isSingleValue()) {
            throw new IllegalStateException("Attempt to resolve sub-path '"+path+"' on multi-value reference " + getElementName());
        }
        PrismReferenceValue value = getValue();
        return value.find(path);
    }



    @Override
    public <IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path) {
        if (path == null || path.isEmpty()) {
            return new PartiallyResolvedItem<>((Item<IV, ID>) this, null);
        }
        if (!isSingleValue()) {
            throw new IllegalStateException("Attempt to resolve sub-path '"+path+"' on multi-value reference " + getElementName());
        }
        PrismReferenceValue value = getValue();
        return value.findPartial(path);
    }

    @Override
    public ReferenceDelta createDelta() {
        return new ReferenceDeltaImpl(getPath(), getDefinition(), prismContext);
    }

    @Override
    public ReferenceDelta createDelta(ItemPath path) {
        return new ReferenceDeltaImpl(path, getDefinition(), prismContext);
    }

    @Override
    protected void checkDefinition(PrismReferenceDefinition def) {
        if (def == null) {
            throw new IllegalArgumentException("Cannot apply null definition to reference "+this);
        }
    }

    @Override
    public PrismReferenceImpl clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public PrismReference createImmutableClone() {
        return (PrismReference) super.createImmutableClone();
    }

    @Override
    public PrismReferenceImpl cloneComplex(CloneStrategy strategy) {
        PrismReferenceImpl clone = new PrismReferenceImpl(getElementName(), getDefinition(), prismContext);
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, PrismReferenceImpl clone) {
        super.copyValues(strategy, clone);
        for (PrismReferenceValue value : getValues()) {
            try {
                clone.addIgnoringEquivalents(value.cloneComplex(strategy));
            } catch (SchemaException e) {
                throw new IllegalStateException("Unexpected SchemaException while copying values: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + PrettyPrinter.prettyPrint(getElementName()) + "):" + getValues();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        PrismReferenceDefinition definition = getDefinition();
        boolean multiline = true;
        if (definition != null) {
            multiline = definition.isMultiValue() || definition.isComposite();
        }
        if (DebugUtil.isDetailedDebugDump()) {
            sb.append(getDebugDumpClassName()).append(": ");
        }
        sb.append(DebugUtil.formatElementName(getElementName()));
        sb.append(": ");
        List<PrismReferenceValue> values = getValues();
        if (values.isEmpty()) {
            sb.append("[ ]");
        } else {
            if (definition != null && DebugUtil.isDetailedDebugDump()) {
                sb.append(" def ");
            }
            for (PrismReferenceValue value : values) {
                if (multiline) {
                    sb.append("\n");
                    DebugUtil.indentDebugDump(sb, indent + 1);
                }
                if (DebugUtil.isDetailedDebugDump()) {
                    if (multiline) {
                        sb.append(value.debugDump(indent + 1, true));
                    } else {
                        sb.append(value.toString());
                    }
                } else {
                    sb.append(value.toHumanReadableString());
                }
            }
        }

        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    protected String getDebugDumpClassName() {
        return "PR";
    }
}
