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

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class OrgFilter extends ObjectFilter {

    public enum Scope {
        ONE_LEVEL,
        SUBTREE,
        ANCESTORS       // EXPERIMENTAL; OID has to belong to an OrgType!
    }

    private PrismReferenceValue baseOrgRef;
    private Scope scope;
    private boolean root;

    private OrgFilter(PrismReferenceValue baseOrgRef, Scope scope) {
        this.baseOrgRef = baseOrgRef;
        this.scope = scope != null ? scope : Scope.SUBTREE;
    }

    private OrgFilter() {
    }

    public static OrgFilter createOrg(PrismReferenceValue baseOrgRef, Scope scope) {
        return new OrgFilter(baseOrgRef, scope);
    }

    public static OrgFilter createOrg(String baseOrgOid, Scope scope) {
        return new OrgFilter(new PrismReferenceValue(baseOrgOid), scope);
    }

    public static OrgFilter createRootOrg() {
        OrgFilter filter = new OrgFilter();
        filter.setRoot(true);
        return filter;
    }

    public PrismReferenceValue getOrgRef() {
        return baseOrgRef;
    }

    public Scope getScope() {
        return scope;
    }

    private void setRoot(boolean root) {
        this.root = root;
    }

    public boolean isRoot() {
        return root;
    }

    @Override
    public OrgFilter clone() {
        if (isRoot()) {
            return createRootOrg();
        } else {
            return new OrgFilter(getOrgRef(), getScope());
        }
    }
    
    

    @Override
	public void checkConsistence(boolean requireDefinitions) {
		if (baseOrgRef == null) {
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OrgFilter other = (OrgFilter) obj;
        if (baseOrgRef == null) {
            if (other.baseOrgRef != null)
                return false;
        } else if (!baseOrgRef.equals(other.baseOrgRef))
            return false;
        if (scope != other.scope)
            return false;
        if (root != other.root)
            return false;
        return true;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
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
        return false;
    }
}
