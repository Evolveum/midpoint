/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Filter designed to explicitly match nothing. It is used in some special cases, e.g.
 * a security component explicitly indicating that no object should be returned.
 *
 * @author Radovan Semancik
 */
// FIXME: This could be singleton
public class NoneFilterImpl extends ObjectFilterImpl implements NoneFilter {

    public NoneFilterImpl() {
        super();
    }

    public static NoneFilter createNone() {
        return new NoneFilterImpl();
    }

    @Override
    public NoneFilterImpl clone() {
        return new NoneFilterImpl();
    }

    @Override
    protected void performFreeze() {
        // NOOP
    }

    @Override
    public void checkConsistence(boolean requireDefinitions) {
        // nothing to do
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("NONE");
        return sb.toString();

    }

    @Override
    public String toString() {
        return "NONE";
    }

    @Override
    public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        return false;
    }

    @Override
    public boolean equals(Object obj, boolean exact) {
        return obj instanceof NoneFilter;
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
