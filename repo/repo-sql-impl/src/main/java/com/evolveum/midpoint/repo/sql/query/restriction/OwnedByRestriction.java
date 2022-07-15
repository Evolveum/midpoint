/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query.restriction;

import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OwnedByFilter;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.ExistsCondition;
import com.evolveum.midpoint.repo.sql.query.resolution.HqlEntityInstance;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Restriction for {@link OwnedByFilter} which creates EXISTS subquery.
 * Note, that {@link #context} is the parent context for the outer query, subquery has its own
 * context which is created and available only inside {@link #interpret()} implementation.
 */
public class OwnedByRestriction extends Restriction<OwnedByFilter> {

    public static final Set<Class<?>> SUPPORTED_OWNED_TYPES = Set.of(
            AssignmentType.class,
            AccessCertificationCaseType.class,
            AccessCertificationWorkItemType.class,
            CaseWorkItemType.class);

    /** Owner type may come from filter or be derived from the owned entity. */
    private final Class<? extends Containerable> ownerType;

    public static OwnedByRestriction create(
            InterpretationContext context, OwnedByFilter filter, JpaEntityDefinition baseEntityDefinition)
            throws QueryException {
        Class<?> ownedType = Objects.requireNonNull(baseEntityDefinition.getJaxbClass());
        Class<? extends Containerable> ownerType = checkOwnedAndOwningTypesAndPath(ownedType, filter);
        return new OwnedByRestriction(context, filter, baseEntityDefinition, ownerType);
    }

    @NotNull
    private static Class<? extends Containerable> checkOwnedAndOwningTypesAndPath(Class<?> ownedType, OwnedByFilter filter)
            throws QueryException {
        if (!SUPPORTED_OWNED_TYPES.contains(ownedType)) {
            throw new QueryException("OwnedBy filter is not supported for type '"
                    + ownedType.getSimpleName() + "'; supported types are: " + SUPPORTED_OWNED_TYPES);
        }

        ItemPath path = filter.getPath();
        Class<? extends Containerable> expectedOwnerType;

        if (ownedType.equals(AssignmentType.class)) {
            expectedOwnerType = AbstractRoleType.F_INDUCEMENT.equivalent(path)
                    ? AbstractRoleType.class
                    : AssignmentHolderType.class;
            if (path != null
                    && !AbstractRoleType.F_INDUCEMENT.equivalent(path)
                    && !AssignmentHolderType.F_ASSIGNMENT.equivalent(path)) {
                throw new QueryException("OwnedBy filter for type '"
                        + ownedType.getSimpleName() + "' used with invalid path: " + path);
            }
        } else if (ownedType.equals(AccessCertificationCaseType.class)) {
            expectedOwnerType = AccessCertificationCampaignType.class;
            if (path != null && !AccessCertificationCampaignType.F_CASE.equivalent(path)) {
                throw new QueryException("OwnedBy filter for type '"
                        + ownedType.getSimpleName() + "' used with invalid path: " + path);
            }
        } else if (ownedType.equals(AccessCertificationWorkItemType.class)) {
            expectedOwnerType = AccessCertificationCaseType.class;
            if (path != null && !AccessCertificationCaseType.F_WORK_ITEM.equivalent(path)) {
                throw new QueryException("OwnedBy filter for type '"
                        + ownedType.getSimpleName() + "' used with invalid path: " + path);
            }
        } else if (ownedType.equals(CaseWorkItemType.class)) {
            expectedOwnerType = CaseType.class;
            if (path != null && !CaseType.F_WORK_ITEM.equivalent(path)) {
                throw new QueryException("OwnedBy filter for type '"
                        + ownedType.getSimpleName() + "' used with invalid path: " + path);
            }
        } else {
            throw new AssertionError("Missing if branch for SUPPORTED_OWNED_TYPES value!");
        }

        ComplexTypeDefinition ownerTypeDef = filter.getType();
        if (ownerTypeDef != null && ownerTypeDef.getCompileTimeClass() != null) {
            if (!expectedOwnerType.isAssignableFrom(ownerTypeDef.getCompileTimeClass())) {
                throw new QueryException("OwnedBy filter with invalid owning type '"
                        + ownerTypeDef.getCompileTimeClass().getSimpleName() + "', type '" + ownedType.getSimpleName()
                        + "' can be owned by '" + expectedOwnerType.getSimpleName() + "' or its subtype.");
            }
            return ownerTypeDef.getCompileTimeClass().asSubclass(Containerable.class);
        }

        return expectedOwnerType;
    }

    private OwnedByRestriction(
            InterpretationContext context,
            OwnedByFilter filter,
            JpaEntityDefinition baseEntityDefinition,
            Class<? extends Containerable> ownerType) {
        // We don't provide parent, not relevant here; and can even confuse inner filters.
        super(context, filter, baseEntityDefinition, null);

        this.ownerType = ownerType;
    }

    @Override
    public Condition interpret() throws QueryException {
        InterpretationContext subcontext = context.createSubcontext(ownerType);
        HqlEntityInstance ownedEntity = getBaseHqlEntity(); // owned entity = parent query

        ExistsCondition existsCondition = new ExistsCondition(subcontext);
        if (ownedEntity.getJpaDefinition().getJpaClass().equals(RAccessCertificationWorkItem.class)) {
            // Currently, the generic repo does not support AccCertWI owned by AccCert directly.
            // Subquery here is for RAccessCertificationCase, both id and oid must match.
            existsCondition.addCorrelationCondition("ownerOid", ownedEntity.getHqlPath() + ".ownerOwnerOid");
            existsCondition.addCorrelationCondition("id", ownedEntity.getHqlPath() + ".ownerId");
        } else {
            existsCondition.addCorrelationCondition("oid", ownedEntity.getHqlPath() + ".ownerOid");
        }

        // Consistency of path and type is checked before (see static factory method above).
        if (AbstractRoleType.F_INDUCEMENT.equals(filter.getPath())) {
            addAssignmentVsInducementCondition(subcontext, RAssignmentOwner.ABSTRACT_ROLE);
        } else if (AssignmentHolderType.F_ASSIGNMENT.equals(filter.getPath())) {
            addAssignmentVsInducementCondition(subcontext, RAssignmentOwner.FOCUS);
        }

        ObjectFilter innerFilter = filter.getFilter();
        if (!(innerFilter == null || innerFilter instanceof AllFilter)) {
            existsCondition.interpretFilter(innerFilter);
        }

        return existsCondition;
    }

    private void addAssignmentVsInducementCondition(InterpretationContext subcontext, RAssignmentOwner discriminator) {
        HibernateQuery subquery = subcontext.getHibernateQuery();
        // We're creating condition for the subquery, but limiting by the outer entity attribute.
        // Adding this directly to the outer query would "work" only if no other filters were combined (e.g. OR other ownedBy).
        subquery.addCondition(subquery.createSimpleComparisonCondition(
                getBaseHqlEntity().getHqlPath() + ".assignmentOwner", discriminator, "="));
    }
}
