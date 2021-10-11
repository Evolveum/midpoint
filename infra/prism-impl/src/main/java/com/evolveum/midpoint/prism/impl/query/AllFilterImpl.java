/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Filter designed to explicitly match everything. It is used in some special cases, e.g.
 * a security component explicitly indicating that all objects should be returned.
 *
 * @author Radovan Semancik
 */
public class AllFilterImpl extends ObjectFilterImpl implements AllFilter {

    public AllFilterImpl() {
        super();
    }

    public static AllFilter createAll() {
        return new AllFilterImpl();
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public AllFilterImpl clone() {
        return new AllFilterImpl();
    }

    @Override
    public void checkConsistence(boolean requireDefinitions) {
        // nothing to do
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ALL");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ALL";
    }

    @Override
    public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        return true;
    }

    @Override
    public boolean equals(Object obj, boolean exact) {
        return obj instanceof AllFilter;
    }

    // Just to make checkstyle happy
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
