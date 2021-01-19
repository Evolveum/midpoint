/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public final class OrgFilterImpl extends ObjectFilterImpl implements OrgFilter {

    // FIXME: Root could be singleton

    private final PrismReferenceValue baseOrgRef;
    private final Scope scope;
    private final boolean root;

    private OrgFilterImpl(PrismReferenceValue baseOrgRef, Scope scope) {
        this.baseOrgRef = baseOrgRef;
        this.scope = scope != null ? scope : Scope.SUBTREE;
        this.root = false;
    }

    private OrgFilterImpl(boolean root) {
        this.baseOrgRef = null;
        this.scope = null;
        this.root = root;
    }

    public static OrgFilter createOrg(PrismReferenceValue baseOrgRef, Scope scope) {
        return new OrgFilterImpl(baseOrgRef, scope);
    }

    public static OrgFilter createOrg(String baseOrgOid, Scope scope) {
        return new OrgFilterImpl(new PrismReferenceValueImpl(baseOrgOid), scope);
    }

    public static OrgFilterImpl createRootOrg() {
        OrgFilterImpl filter = new OrgFilterImpl(true);
        return filter;
    }

    @Override
    public PrismReferenceValue getOrgRef() {
        return baseOrgRef;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public boolean isRoot() {
        return root;
    }

    @Override
    public OrgFilterImpl clone() {
        if (isRoot()) {
            return createRootOrg();
        } else {
            return new OrgFilterImpl(getOrgRef(), getScope());
        }
    }

    @Override
    protected void performFreeze() {
        freeze(baseOrgRef);
    }

    @Override
    public void checkConsistence(boolean requireDefinitions) {
        if (!root && baseOrgRef == null) {
            throw new IllegalArgumentException("Null baseOrgRef in "+this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseOrgRef == null) ? 0 : baseOrgRef.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + (root ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj, boolean exact) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        OrgFilterImpl other = (OrgFilterImpl) obj;
        if (baseOrgRef == null) {
            if (other.baseOrgRef != null) return false;
        } else if (!baseOrgRef.equals(other.baseOrgRef)) {
            return false;
        }
        if (scope != other.scope) return false;
        if (root != other.root) return false;
        return true;
    }

    // Just to make checkstyle happy
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ORG: \n");
        if (isRoot()) {
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("ROOT\n");
        }
        if (getOrgRef() != null) {
            sb.append(getOrgRef().debugDump(indent + 1));
            sb.append("\n");
        } else {
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("null\n");
        }
        if (getScope() != null) {
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append(getScope());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ORG: ");
        if (getOrgRef() != null) {
            sb.append(getOrgRef().toString());
            sb.append(", ");
        }
        if (getScope() != null) {
            sb.append(getScope());
        }
        return sb.toString();
    }

    @Override
    public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        throw new UnsupportedOperationException("Matching object and ORG filter is not supported yet");
    }
}
