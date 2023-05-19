/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SubjectedObjectSelectorType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;

/**
 * Evaluates "requester" object selector clause.
 *
 * Note that applicability checking si provided by the repository service.
 */
public class Requester extends AbstractSelectorClauseEvaluation {

    /** TODO why is this used only for "is applicable" checking? */
    @NotNull private final SubjectedObjectSelectorType requestorSelector;

    public Requester(@NotNull SubjectedObjectSelectorType requestorSelector, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.requestorSelector = requestorSelector;
    }

    public boolean isApplicable(PrismValue value)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var object = asObjectTypeIfPossible(value);
        if (object == null) {
            return false; // TODO log?
        }
        PrismObject<? extends ObjectType> requestor = getRequestor(object);
        if (requestor == null) {
            LOGGER.trace("    requester object spec not applicable for {}, object OID {} because it has no requestor",
                    ctx.getDesc(), object.getOid());
            return false;
        }
        boolean requestorApplicable =
                ctx.isSelectorApplicable(
                        requestorSelector,
                        requestor,
                        ctx.getDelegatorsForRequestor(),
                        "requestor of " + ctx.getDesc());
        if (!requestorApplicable) {
            LOGGER.trace("    requester object spec not applicable for {}, object OID {} because requestor does not match (requestor={})",
                    ctx.getDesc(), object.getOid(), requestor);
        }
        return requestorApplicable;
    }

    private PrismObject<? extends ObjectType> getRequestor(ObjectType object) {
        if (object instanceof CaseType) {
            return ctx.resolveReference(((CaseType) object).getRequestorRef(), object, "requestor");
        } else {
            return null;
        }
    }

    public boolean applyFilter() {
        if (CaseType.class.isAssignableFrom(fCtx.getRefinedType())) {
            var increment = createFilter();
            LOGGER.trace("  applying requester filter {}", increment);
            fCtx.addConjunction(increment);
            return true;
        } else {
            LOGGER.trace("      Authorization not applicable for object because it has requester specification (this is not applicable for search for objects other than CaseType)");
            return false;
        }
    }

    private ObjectFilter createFilter() {
        return PrismContext.get().queryFor(CaseType.class)
                .item(CaseType.F_REQUESTOR_REF)
                .ref(ctx.getSelfAndOtherOids(ctx.getDelegatorsForRequestor()))
                .buildFilter();
    }
}
