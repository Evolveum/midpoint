/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Objects;

/**
 * Extension of PrismContainerValue that holds object-specific data (OID and version).
 * It was created to make methods returning/accepting ItemValue universally usable;
 * not losing OID/version data when object values are passed via such interfaces.
 *
 * This value is to be held by PrismObject. And such object should hold exactly one
 * PrismObjectValue.
 *
 * @author mederly
 */
public class PrismObjectValueImpl<O extends Objectable> extends PrismContainerValueImpl<O> implements PrismObjectValue<O> {

    protected String oid;
    protected String version;

    public PrismObjectValueImpl() {
    }

    public PrismObjectValueImpl(PrismContext prismContext) {
        super(prismContext);
    }

    public PrismObjectValueImpl(O objectable) {
        super(objectable);
    }

    public PrismObjectValueImpl(O objectable, PrismContext prismContext) {
        super(objectable, prismContext);
    }

    private PrismObjectValueImpl(OriginType type, Objectable source, PrismContainerable container, Long id,
            ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, String oid, String version) {
        super(type, source, container, id, complexTypeDefinition, prismContext);
        this.oid = oid;
        this.version = version;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        checkMutable();
        this.oid = oid;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        checkMutable();
        this.version = version;
    }

    public O asObjectable() {
        return asContainerable();
    }

    public PrismObject<O> asPrismObject() {
        return asObjectable().asPrismObject();
    }

    public PolyString getName() {
        return asPrismObject().getName();
    }

    public PrismContainer<?> getExtension() {
        return asPrismObject().getExtension();
    }

    @Override
    public PrismObjectValueImpl<O> clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public PrismObjectValueImpl<O> cloneComplex(CloneStrategy strategy) {
        PrismObjectValueImpl<O> clone = new PrismObjectValueImpl<>(
                getOriginType(), getOriginObject(), getParent(), getId(), complexTypeDefinition, this.prismContext, oid, version);
        copyValues(strategy, clone);
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrismObjectValueImpl)) return false;
        if (!super.equals(o)) return false;
        PrismObjectValueImpl<?> that = (PrismObjectValueImpl<?>) o;
        return Objects.equals(oid, that.oid);
    }

    // Just to make checkstyle happy
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // TODO consider the strategy
    @Override
    public int hashCode(@NotNull ParameterizedEquivalenceStrategy strategy) {
        return Objects.hash(super.hashCode(strategy), oid);
    }

    @Override
    public boolean equivalent(PrismContainerValue<?> other) {
        if (!(other instanceof PrismObjectValueImpl)) {
            return false;
        }
        PrismObjectValueImpl otherPov = (PrismObjectValueImpl) other;
        return StringUtils.equals(oid, otherPov.oid) && super.equivalent(other);
    }

    @Override
    public String toString() {
        // we don't delegate to PrismObject, because various exceptions during that process could in turn call this method
        StringBuilder sb = new StringBuilder();
        sb.append("POV:");
        if (getParent() != null) {
            sb.append(getParent().getElementName().getLocalPart()).append(":");
        } else if (getComplexTypeDefinition() != null) {
            sb.append(getComplexTypeDefinition().getTypeName().getLocalPart()).append(":");
        }
        sb.append(oid).append("(");
        PrismProperty nameProperty = findProperty(new ItemName(PrismConstants.NAME_LOCAL_NAME));
        sb.append(nameProperty != null ? nameProperty.getRealValue() : null);
        sb.append(")");
        return sb.toString();
    }

    @Override
    protected void detailedDebugDumpStart(StringBuilder sb) {
        sb.append("POV").append(": ");
    }

    @Override
    protected void debugDumpIdentifiers(StringBuilder sb) {
        sb.append("oid=").append(oid);
        sb.append(", version=").append(version);
    }

    @Override
    public String toHumanReadableString() {
        return "oid="+oid+": "+items.size()+" items";
    }

    @Override
    public PrismContainer<O> asSingleValuedContainer(@NotNull QName itemName) throws SchemaException {
        throw new UnsupportedOperationException("Not supported for PrismObjectValue yet.");
    }

    public static <T extends Objectable> T asObjectable(PrismObject<T> object) {
        return object != null ? object.asObjectable() : null;
    }
}
