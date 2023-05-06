/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluates "delegator" object selector clause.
 *
 * (Only applicability check is provided now; search processing is not supported yet.)
 */
public class Delegator extends AbstractSelectorClauseEvaluation {

    @NotNull
    private final SubjectedObjectSelectorType delegatorSelector;

    public Delegator(
            @NotNull SubjectedObjectSelectorType delegatorSelector, @NotNull ClauseEvaluationContext ctx) {
        super(ctx);
        this.delegatorSelector = delegatorSelector;
    }

    public boolean isApplicable(@NotNull PrismObject<? extends ObjectType> object) throws SchemaException {
        if (!isSelfSelector()) {
            throw new SchemaException("Unsupported non-self delegator clause");
        }
        if (!object.canRepresent(UserType.class)) {
            LOGGER.trace("    delegator object spec not applicable for {}, because the object is not user", ctx.getDesc());
            return false;
        }
        boolean found = false;
        String principalOid = ctx.getPrincipalOid();
        if (principalOid != null) {
            for (ObjectReferenceType objectDelegatedRef : ((UserType) object.asObjectable()).getDelegatedRef()) {
                if (principalOid.equals(objectDelegatedRef.getOid())) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            if (BooleanUtils.isTrue(delegatorSelector.isAllowInactive())) {
                for (AssignmentType objectAssignment : ((UserType) object.asObjectable()).getAssignment()) {
                    ObjectReferenceType objectAssignmentTargetRef = objectAssignment.getTargetRef();
                    if (objectAssignmentTargetRef == null) {
                        continue;
                    }
                    if (principalOid != null && principalOid.equals(objectAssignmentTargetRef.getOid())) {
                        if (SchemaService.get().relationRegistry().isDelegation(objectAssignmentTargetRef.getRelation())) {
                            found = true;
                            break;
                        }
                    }
                }
            }

            if (!found) {
                LOGGER.trace("    delegator object spec not applicable for {}, object OID {} because delegator does not match",
                        ctx.getDesc(), object.getOid());
            }
        }
        return found;
    }

    private boolean isSelfSelector() throws SchemaException {
        if (delegatorSelector.getFilter() != null
                || delegatorSelector.getOrgRef() != null
                || delegatorSelector.getOrgRelation() != null
                || delegatorSelector.getRoleRelation() != null) {
            return false;
        }
        List<SpecialObjectSpecificationType> specSpecial = delegatorSelector.getSpecial();
        for (SpecialObjectSpecificationType special : specSpecial) {
            if (special == SpecialObjectSpecificationType.SELF) {
                return true;
            } else {
                throw new SchemaException("Unsupported special object specification specified in authorization: " + special);
            }
        }
        return false;
    }
}
