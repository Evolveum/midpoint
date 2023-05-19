/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SpecialObjectSpecificationType;

/**
 * Evaluates "special" object selector clause.
 */
public class Special extends AbstractSelectorClauseEvaluation {

    private final Collection<SpecialObjectSpecificationType> specials;

    public Special(
            @NotNull Collection<SpecialObjectSpecificationType> specials,
            @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.specials = specials;
    }

    public boolean isApplicable(@NotNull PrismValue value) throws SchemaException {
        var object = ObjectTypeUtil.asObjectTypeIfPossible(value);
        if (object == null) {
            return false;
        }
        assert !specials.isEmpty();
        for (SpecialObjectSpecificationType special : specials) {
            if (special == SpecialObjectSpecificationType.SELF) {
                String principalOid = ctx.getPrincipalOid();
                if (principalOid == null) {
                    // This is a rare case. It should not normally happen. But it may happen in tests
                    // or during initial import. Therefore we are not going to die here. Just ignore it.
                    LOGGER.trace("    'self' authorization not applicable for {} (no principal OID)", ctx.getDesc());
                    return false;
                } else {
                    String objectOid = object.getOid();
                    if (principalOid.equals(objectOid)) {
                        LOGGER.trace("    'self' authorization applicable for {} - match on principal OID ({})",
                                ctx.getDesc(), principalOid);
                        return true;
                    } else if (ctx.getOtherSelfOids().contains(objectOid)) {
                        LOGGER.trace("    'self' authorization applicable for {} - match on other 'self OID' ({})",
                                ctx.getDesc(), objectOid);
                        return true;
                    } else {
                        LOGGER.trace("    'self' authorization not applicable for {}, principal OID: {} (other accepted self OIDs: {}), {} OID {}",
                                ctx.getDesc(), principalOid, ctx.getOtherSelfOids(), ctx.getDesc(), objectOid);
                        return false;
                    }
                }
            } else {
                throw new SchemaException(String.format(
                        "Unsupported special %s specification specified in %s: %s", ctx.getDesc(), ctx.getAutzDesc(), special));
            }
        }
        throw new IllegalStateException("Not expected to get here");
    }

    // TODO self OIDs?
    public ObjectFilter createFilter() throws SchemaException {
        assert !specials.isEmpty();
        ObjectFilter filter = null;
        // TODO create single "in oid" filter
        for (SpecialObjectSpecificationType special : specials) {
            if (special == SpecialObjectSpecificationType.SELF) {
                String principalOid = ctx.getPrincipalOid();
                filter = ObjectQueryUtil.filterOr(
                        filter,
                        PrismContext.get().queryFactory().createInOid(principalOid));
            } else {
                throw new SchemaException("Unsupported special object specification specified in authorization: " + special);
            }
        }
        assert filter != null;
        return filter;
    }
}
