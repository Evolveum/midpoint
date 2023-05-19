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
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubjectedObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.prism.PrismObjectValue.asObjectable;

/**
 * Evaluates "relatedObject" object selector clause.
 *
 * Note that applicability checking si provided by the repository service.
 */
public class RelatedObject extends AbstractSelectorClauseEvaluation {

    /** TODO why is this used only for "is applicable" checking? */
    @NotNull private final SubjectedObjectSelectorType relatedObjectSelector;

    public RelatedObject(@NotNull SubjectedObjectSelectorType relatedObjectSelector, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.relatedObjectSelector = relatedObjectSelector;
    }

    public boolean isApplicable(PrismObject<? extends ObjectType> object)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        PrismObject<? extends ObjectType> relatedObject = getRelatedObject(object);
        if (relatedObject == null) {
            LOGGER.trace("    related object spec not applicable for {}, object OID {} because it has no related object",
                    ctx.getDesc(), object.getOid());
            return false;
        }
        boolean relatedObjectApplicable = ctx.isSelectorApplicable(
                relatedObjectSelector, relatedObject, ctx.getDelegatorsForRelatedObjects(),
                "related object of " + ctx.getDesc());
        if (!relatedObjectApplicable) {
            LOGGER.trace("    related object spec not applicable for {}, object OID {} because related object does not match (related object={})",
                    ctx.getDesc(), object.getOid(), relatedObject);
        }
        return relatedObjectApplicable;
    }

    private PrismObject<? extends ObjectType> getRelatedObject(PrismObject<? extends ObjectType> object) {
        ObjectType objectBean = asObjectable(object);
        if (objectBean instanceof CaseType) {
            return ctx.resolveReference(((CaseType) objectBean).getObjectRef(), object, "related object");
        } else if (objectBean instanceof TaskType) {
            return ctx.resolveReference(((TaskType) objectBean).getObjectRef(), object, "related object");
        } else {
            return null;
        }
    }

    public boolean applyFilter() {
        Class<?> objectType = fCtx.getRefinedType();
        if (CaseType.class.isAssignableFrom(objectType)
                || TaskType.class.isAssignableFrom(objectType)) {
            //noinspection unchecked
            var increment = createFilter((Class<? extends ObjectType>) objectType);
            LOGGER.trace("  applying related object filter {}", increment);
            fCtx.addConjunction(increment);
            return true;
        } else {
            LOGGER.trace("      Authorization not applicable for object because it has related object specification (this is not applicable for search for objects other than CaseType and TaskType)");
            return false;
        }
    }

    private ObjectFilter createFilter(Class<? extends ObjectType> objectType) {
        // we assume CaseType.F_OBJECT_REF == TaskType.F_OBJECT_REF here
        return PrismContext.get().queryFor(objectType)
                .item(CaseType.F_OBJECT_REF)
                .ref(ctx.getSelfAndOtherOids(ctx.getDelegatorsForRelatedObjects()))
                .buildFilter();
    }
}
