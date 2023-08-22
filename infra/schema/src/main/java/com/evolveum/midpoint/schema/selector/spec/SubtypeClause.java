/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import java.util.List;

import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Originally, this was used for evaluation of `subtype` on objects.
 * This property is deprecated and will be eventually removed.
 *
 * However, there are still assignment subtypes that are alive and well, at least for now.
 */
public class SubtypeClause extends SelectorClause {

    @NotNull private final String subtype;

    private SubtypeClause(@NotNull String subtype) {
        this.subtype = subtype;
    }

    static SubtypeClause of(@NotNull String subtype) throws ConfigurationException {
        return new SubtypeClause(subtype);
    }

    @Override
    public @NotNull String getName() {
        return "subtype";
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx) {
        Object realValue = value.getRealValueIfExists();
        List<String> actualSubtypes;
        if (realValue instanceof ObjectType) {
            actualSubtypes = ((ObjectType) realValue).getSubtype();
        } else if (realValue instanceof AssignmentType) {
            actualSubtypes = ((AssignmentType) realValue).getSubtype();
        } else {
            traceNotApplicable(
                    ctx, "subtype mismatch, expected '%s' but object has none (it is neither object nor assignment)",
                    subtype);
            return false;
        }

        if (actualSubtypes.contains(subtype)) {
            return true;
        } else {
            traceNotApplicable(
                    ctx, "subtype mismatch, expected %s, was %s",
                    subtype, actualSubtypes);
            return false;
        }
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) throws SchemaException {
        throw new UnsupportedOperationException("Filtering on subtypes is not supported");
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("subtype: ").append(subtype);
    }

    @Override
    public String toString() {
        return "SubtypeClause{" +
                "subtype='" + subtype + '\'' +
                "}";
    }
}
