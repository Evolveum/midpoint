/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import static java.util.Collections.emptySet;

/**
 * Evaluates "owner" object selector clause.
 */
public class Owner extends AbstractSelectorClauseEvaluation {

    @NotNull
    private final SubjectedObjectSelectorType ownerSelector;

    public Owner(@NotNull SubjectedObjectSelectorType ownerSelector, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.ownerSelector = ownerSelector;
    }

    public boolean isApplicable(PrismObject<? extends ObjectType> object)
            throws ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException,
            SchemaException, ObjectNotFoundException {
        OwnerResolver ownerResolver = ctx.getOwnerResolver();
        if (ownerResolver == null) {
            LOGGER.trace("    owner object spec not applicable for {}, object OID {} because there is no owner resolver",
                    ctx.getDesc(), object.getOid());
            return false;
        }
        PrismObject<? extends FocusType> owner = ownerResolver.resolveOwner(object);
        if (owner == null) {
            LOGGER.trace("    owner object spec not applicable for {}, object OID {} because it has no owner",
                    ctx.getDesc(), object.getOid());
            return false;
        }
        boolean ownerApplicable =
                ctx.isSelectorApplicable(
                        ownerSelector, owner, emptySet(), "owner of " + ctx.getDesc());
        if (!ownerApplicable) {
            LOGGER.trace("    owner object spec not applicable for {}, object OID {} because owner does not match (owner={})",
                    ctx.getDesc(), object.getOid(), owner);
            return false;
        }
        return true;
    }

    public boolean applyFilter() {
        // TODO: MID-3899
        // TODO what if owner is specified not as "self" ?
        if (TaskType.class.isAssignableFrom(fCtx.getObjectType())) {
            var increment = applyOwnerFilterOwnerRef(
                    TaskType.F_OWNER_REF, fCtx.getObjectDefinition(), ctx.getPrincipalFocus());
            LOGGER.trace("  applying owner filter {}", increment);
            fCtx.addConjunction(increment);
            return true;
        } else {
            LOGGER.trace("      Object selector is not applicable because it has owner specification (this is not applicable for search)");
            return false; // TODO what about applicability?
        }
    }

    // TODO review this legacy code
    private ObjectFilter applyOwnerFilterOwnerRef(
            ItemPath ownerRefPath, PrismObjectDefinition<?> objectDefinition, FocusType principalFocus) {
        PrismReferenceDefinition ownerRefDef = objectDefinition.findReferenceDefinition(ownerRefPath);
        S_FilterExit builder = PrismContext.get().queryFor(AbstractRoleType.class)
                .item(ownerRefPath, ownerRefDef).ref(principalFocus.getOid());
        // TODO don't understand this code
        for (ObjectReferenceType subjectParentOrgRef : principalFocus.getParentOrgRef()) {
            if (PrismContext.get().isDefaultRelation(subjectParentOrgRef.getRelation())) {
                builder = builder.or().item(ownerRefPath, ownerRefDef).ref(subjectParentOrgRef.getOid());
            }
        }
        return builder.buildFilter();
    }
}
