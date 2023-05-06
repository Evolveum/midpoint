/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

/**
 * Evaluates "archetypeRef" object selector clause.
 *
 * Note that applicability check is provided by {@link RepositoryService#selectorMatches(ObjectSelectorType, PrismObject,
 * ObjectFilterExpressionEvaluator, Trace, String)} method.
 */
public class ArchetypeRef extends AbstractSelectorClauseEvaluation {

    @NotNull private final List<ObjectReferenceType> selectorArchetypeRefs;

    public ArchetypeRef(@NotNull List<ObjectReferenceType> selectorArchetypeRefs, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.selectorArchetypeRefs = selectorArchetypeRefs;
    }

    public void applyFilter() throws ConfigurationException {
        Collection<String> archetypeOids = ObjectTypeUtil.getOidsFromRefs(selectorArchetypeRefs);
        configCheck(!archetypeOids.contains(null), "Archetype reference without OID"); // TODO error location
        ObjectFilter increment = PrismContext.get().queryFor(AssignmentHolderType.class)
                .item(AssignmentHolderType.F_ARCHETYPE_REF)
                .ref(archetypeOids.toArray(new String[0]))
                .buildFilter();
        fCtx.addConjunction(increment);
        LOGGER.trace("      applying archetype filter {}", increment);
    }
}
