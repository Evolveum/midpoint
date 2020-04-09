/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismContextSensitive;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.Serializable;

public interface ObjectFilter extends DebugDumpable, Serializable, Revivable, PrismContextSensitive {

    /**
     * Does a SHALLOW clone.
     */
    ObjectFilter clone();

    boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException;

    void accept(Visitor visitor);

    @Override
    void revive(PrismContext prismContext) throws SchemaException;

    void checkConsistence(boolean requireDefinitions);

    boolean equals(Object o, boolean exact);
}
