/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

/**
 * Shared assignment query builders reused by runtime panel overrides and self-dashboard trimming.
 *
 * <p>Keeping these builders in one place avoids trim-only query hooks on panels while preserving the same query
 * semantics in both runtime and trim-time code paths.</p>
 */
public final class AssignmentPanelQueries {

    private AssignmentPanelQueries() {
    }

    /**
     * Default assignment query used by standard assignment panels.
     */
    public static ObjectQuery defaultAssignments(QName targetType) {
        // Keep this aligned with AbstractAssignmentTypePanel.getTargetTypeFilter():
        // current assignment panels derive target filtering only from getAssignmentType().
        Collection<QName> delegationRelations = MidPointApplication.get().getRelationRegistry()
                .getAllRelationsFor(RelationKindType.DELEGATION);
        PrismContext prismContext = PrismContext.get();

        //do not show archetype assignments
        ObjectReferenceType archetypeRef = new ObjectReferenceType();
        archetypeRef.setType(ArchetypeType.COMPLEX_TYPE);
        archetypeRef.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
        RefFilter archetypeFilter = (RefFilter) prismContext.queryFor(AssignmentType.class)
                .item(AssignmentType.F_TARGET_REF)
                .ref(archetypeRef.asReferenceValue())
                .buildFilter();
        archetypeFilter.setOidNullAsAny(true);

        ObjectFilter relationFilter = prismContext.queryFor(AssignmentType.class)
                .not()
                .item(AssignmentType.F_TARGET_REF)
                .refRelation(delegationRelations.toArray(new QName[0]))
                .buildFilter();

        ObjectQuery query = prismContext.queryFactory().createQuery(relationFilter);
        query.addFilter(prismContext.queryFactory().createNot(archetypeFilter));

        if (targetType != null) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setType(targetType);
            ort.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
            RefFilter targetRefFilter = (RefFilter) prismContext.queryFor(AssignmentType.class)
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(ort.asReferenceValue())
                    .buildFilter();
            targetRefFilter.setOidNullAsAny(true);
            query.addFilter(targetRefFilter);
        }
        return query;
    }

    /**
     * Query used by construction assignment widgets and panels.
     */
    public static ObjectQuery constructions() {
        return PrismContext.get().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_CONSTRUCTION)
                .build();
    }

    /**
     * Query used by focus mappings assignment widgets and panels.
     */
    public static ObjectQuery focusMappings() {
        return PrismContext.get().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_FOCUS_MAPPINGS)
                .build();
    }

    /**
     * Query used by the access-organizations/data-protection assignment panel variant.
     */
    public static ObjectQuery accessOrganizations() {
        return QueryBuilder.queryFor(AssignmentType.class, getPrismContext())
                .ref(AssignmentType.F_TARGET_REF, OrgType.COMPLEX_TYPE, null)
                .item(ObjectType.F_SUBTYPE).contains("access")
                .build();
    }

    /**
     * Query used by GDPR/consent assignment widgets and panels.
     */
    public static ObjectQuery gdprAssignments() {
        return PrismContext.get().queryFor(AssignmentType.class)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .refRelation(SchemaConstants.ORG_CONSENT)
                .endBlock()
                .build();
    }

    /**
     * Query used by delegated-to-me widgets. The trim plan preserves all matching assignments.
     */
    public static ObjectQuery delegatedToMe() {
        PrismContext prismContext = PrismContext.get();
        return prismContext.queryFactory().createQuery(
                prismContext.queryFor(AssignmentType.class)
                        .item(AssignmentType.F_TARGET_REF)
                        .refType(UserType.COMPLEX_TYPE)
                        .buildFilter());
    }
}
