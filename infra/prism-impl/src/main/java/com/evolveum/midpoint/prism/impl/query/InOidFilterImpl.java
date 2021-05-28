/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.query;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public final class InOidFilterImpl extends ObjectFilterImpl implements InOidFilter {

    private List<String> oids;
    private ExpressionWrapper expression;
    private final boolean considerOwner;

    private InOidFilterImpl(boolean considerOwner, Collection<String> oids) {
        this.considerOwner = considerOwner;
        setOids(oids);
    }

    private InOidFilterImpl(boolean considerOwner, ExpressionWrapper expression) {
        this.considerOwner = considerOwner;
        this.expression = expression;
    }

    public static InOidFilter createInOid(boolean considerOwner, Collection<String> oids) {
        return new InOidFilterImpl(considerOwner, oids);
    }

    public static InOidFilter createInOid(Collection<String> oids) {
        return new InOidFilterImpl(false, oids);
    }

    public static InOidFilter createInOid(String... oids) {
        return new InOidFilterImpl(false, Arrays.asList(oids));
    }

    public static InOidFilter createOwnerHasOidIn(Collection<String> oids) {
        return new InOidFilterImpl(true, oids);
    }

    public static InOidFilter createOwnerHasOidIn(String... oids) {
        return new InOidFilterImpl(true, Arrays.asList(oids));
    }

    public static InOidFilter createInOid(boolean considerOwner, ExpressionWrapper expression) {
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
            throw new IllegalArgumentException("Null oids in " + this);
        }
        for (String oid : oids) {
            if (StringUtils.isBlank(oid)) {
                throw new IllegalArgumentException("Empty oid in " + this);
            }
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("IN OID");
        if (considerOwner) {
            sb.append(" (for owner)");
        }
        sb.append(": ");
        if (getOids() != null) {
            for (String oid : getOids()) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent + 1);
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
        sb.append("IN OID");
        if (considerOwner) {
            sb.append(" (for owner)");
        }
        sb.append(": ");
        if (getOids() != null) {
            boolean first = true;
            for (String value : getOids()) {
                if (value == null) {
                    sb.append("null");
                } else {
                    sb.append(value);
                }
                if (first) {
                    first = false;
                } else {
                    sb.append("; ");
                }
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
            return false; // just for sure
        }

        // are we a prism object?
        if (value.getParent() instanceof PrismObject) {
            if (considerOwner) {
                return false;
            }
            String oid = ((PrismObject<?>) (value.getParent())).getOid();
            return StringUtils.isNotBlank(oid) && oids != null && oids.contains(oid);
        }

        PrismContainerValue<?> pcvToConsider;
        if (considerOwner) {
            if (!(value.getParent() instanceof PrismContainer)) {
                return false;
            }
            pcvToConsider = ((PrismContainer<?>) value.getParent()).getParent();
        } else {
            pcvToConsider = value;
        }
        return pcvToConsider != null
                && pcvToConsider.getId() != null
                && oids.contains(pcvToConsider.getId().toString());
    }

    @Override
    public boolean equals(Object o, boolean exact) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InOidFilterImpl that = (InOidFilterImpl) o;
        return considerOwner == that.considerOwner
                && Objects.equals(oids, that.oids)
                && Objects.equals(expression, that.expression);

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
