/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import java.util.*;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public final class InOidFilterImpl extends ObjectFilterImpl implements InOidFilter {

    private List<String> oids;
    private ExpressionWrapper expression;
    private boolean considerOwner;                // temporary hack (checks owner OID)

    private InOidFilterImpl(boolean considerOwner, Collection<String> oids) {
        this.considerOwner = considerOwner;
        setOids(oids);
    }

    private InOidFilterImpl(boolean considerOwner, ExpressionWrapper expression){
        this.considerOwner = considerOwner;
        this.expression = expression;
    }

    public static InOidFilter createInOid(boolean considerOwner, Collection<String> oids){
        return new InOidFilterImpl(considerOwner, oids);
    }

    public static InOidFilter createInOid(Collection<String> oids){
        return new InOidFilterImpl(false, oids);
    }

    public static InOidFilter createInOid(String... oids){
        return new InOidFilterImpl(false, Arrays.asList(oids));
    }

    public static InOidFilter createOwnerHasOidIn(Collection<String> oids){
        return new InOidFilterImpl(true, oids);
    }

    public static InOidFilter createOwnerHasOidIn(String... oids){
        return new InOidFilterImpl(true, Arrays.asList(oids));
    }

    public static InOidFilter createInOid(boolean considerOwner, ExpressionWrapper expression){
        return new InOidFilterImpl(considerOwner, expression);
    }

    @Override
    public Collection<String> getOids() {
        return oids;
    }

    @Override
    public void setOids(Collection<String> oids) {
        checkMutable();
        this.oids = oids != null ? new ArrayList<>(oids) : null;
    }

    @Override
    public boolean isConsiderOwner() {
        return considerOwner;
    }

    @Override
    public ExpressionWrapper getExpression() {
        return expression;
    }

    @Override
    public void setExpression(ExpressionWrapper expression) {
        checkMutable();
        this.expression = expression;
    }

    @Override
    protected void performFreeze() {
        oids = freezeNullableList(oids);
        freeze(expression);
    }

    @Override
    public void checkConsistence(boolean requireDefinitions) {
        if (oids == null) {
            throw new IllegalArgumentException("Null oids in "+this);
        }
        for (String oid: oids) {
            if (StringUtils.isBlank(oid)) {
                throw new IllegalArgumentException("Empty oid in "+this);
            }
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("IN OID: ");
        if (considerOwner) {
            sb.append("(for owner)");
        }
        sb.append("VALUE:");
        if (getOids() != null) {
            for (String oid : getOids()) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent+1);
                sb.append(oid);
            }
        } else {
            sb.append(" null");
        }

        return sb.toString();

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IN OID: ");
        if (getOids() != null){
            for (String value : getOids()) {
                if (value == null) {
                    sb.append("null");
                } else {
                    sb.append(value);
                }
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    @Override
    public InOidFilterImpl clone() {
        InOidFilterImpl inOid = new InOidFilterImpl(considerOwner, getOids());
        inOid.setExpression(getExpression());
        return inOid;
    }

    @Override
    public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        if (value == null) {
            return false;            // just for sure
        }

        // are we a prism object?
        if (value.getParent() instanceof PrismObject) {
            if (considerOwner) {
                return false;
            }
            String oid = ((PrismObject) (value.getParent())).getOid();
            return StringUtils.isNotBlank(oid) && oids != null && oids.contains(oid);
        }

        final PrismContainerValue pcvToConsider;
        if (considerOwner) {
            if (!(value.getParent() instanceof PrismContainer)) {
                return false;
            }
            PrismContainer container = (PrismContainer) value.getParent();
            if (!(container.getParent() instanceof PrismContainerValue)) {
                return false;
            }
            pcvToConsider = container.getParent();
        } else {
            pcvToConsider = value;
        }
        return pcvToConsider.getId() != null && oids.contains(pcvToConsider.getId());
    }

    @Override
    public boolean equals(Object o, boolean exact) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InOidFilterImpl that = (InOidFilterImpl) o;

        if (considerOwner != that.considerOwner) return false;
        if (oids != null ? !oids.equals(that.oids) : that.oids != null) {
            return false;
        }
        return expression != null ? expression.equals(that.expression) : that.expression == null;

    }

    // Just to make checkstyle happy
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        int result = oids != null ? oids.hashCode() : 0;
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        result = 31 * result + (considerOwner ? 1 : 0);
        return result;
    }
}
