/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query.restriction;

import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.OwnedByFilter;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.EntityReference;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.resolution.HqlEntityInstance;
import com.evolveum.midpoint.repo.sql.query.resolution.ProperDataSearchResult;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class OwnedByRestriction extends Restriction<OwnedByFilter> {

    public static final Set<Class<?>> SUPPORTED_OWNED_TYPES = Set.of(AssignmentType.class);

    private final JpaEntityDefinition owningEntityDefinition;

    public static OwnedByRestriction create(InterpretationContext context, OwnedByFilter filter,
            JpaEntityDefinition baseEntityDefinition, Restriction<?> parent)
            throws QueryException {
        Class<?> ownedType = baseEntityDefinition.getJaxbClass();
        if (!SUPPORTED_OWNED_TYPES.contains(ownedType)) {
            throw new QueryException("OwnedBy filter is not supported for type '"
                    + (ownedType != null ? ownedType.getSimpleName() : null)
                    + "'; supported types are: " + SUPPORTED_OWNED_TYPES);
        }

        ItemPath path = filter.getPath();
        ComplexTypeDefinition type = filter.getType();
        // TODO better defaults based on owned type and optional path
        QName typeName = type != null ? type.getTypeName() : ObjectType.COMPLEX_TYPE;
        JpaEntityDefinition owningEntityDefinition =
                QueryDefinitionRegistry.getInstance().findEntityDefinition(typeName);
        if (path != null) {
            ProperDataSearchResult<?> searchResult = context.getItemPathResolver().findProperDataDefinition(
                    owningEntityDefinition, path, null, JpaDataNodeDefinition.class, context.getPrismContext());

            if (searchResult == null) {
                throw new QueryException("Path for OwnedByFilter (" + path +
                        ") doesn't point to a hibernate entity or property within " + owningEntityDefinition);
            }
        }
        return new OwnedByRestriction(context, filter, baseEntityDefinition, parent, owningEntityDefinition);
    }

    private OwnedByRestriction(
            InterpretationContext context,
            OwnedByFilter filter,
            JpaEntityDefinition baseEntityDefinition,
            Restriction<?> parent,
            JpaEntityDefinition owningEntityDefinition) {
        // TODO is parent even necessary for constructing EXISTS? Let's just pass it as is...
        super(context, filter, baseEntityDefinition, parent);

        this.owningEntityDefinition = owningEntityDefinition;
    }

    @Override
    public Condition interpret() throws QueryException {
        /* TODO old code from ExistsRestriction
        HqlDataInstance dataInstance = getItemPathResolver()
                // definition needed only for ANY, not our concern here
                .resolveItemPath(filter.getPath(), null, getBaseHqlEntity(), false);

        boolean isAll = filter.getFilter() == null || filter.getFilter() instanceof AllFilter;
        JpaDataNodeDefinition jpaDefinition = dataInstance.getJpaDefinition();
        if (!isAll) {
            if (!(jpaDefinition instanceof JpaEntityDefinition)) {    // partially checked already (for non-null-ness)
                throw new QueryException("ExistsRestriction with non-empty subfilter points to non-entity node: " + jpaDefinition);
            }
            setHqlDataInstance(dataInstance);
            QueryInterpreter interpreter = context.getInterpreter();
            return interpreter.interpretFilter(context, filter.getFilter(), this);
        } else if (jpaDefinition instanceof JpaPropertyDefinition && (((JpaPropertyDefinition) jpaDefinition).isCount())) {
            RootHibernateQuery hibernateQuery = context.getHibernateQuery();
            return hibernateQuery.createSimpleComparisonCondition(dataInstance.getHqlPath(), 0, ">");
        } else {
            // TODO support exists also for other properties (single valued or multi valued)
            throw new UnsupportedOperationException("Exists filter with 'all' subfilter is currently not supported");
        }
        */

        //noinspection ConstantConditions
        InterpretationContext subcontext = context.createSubcontext(
                owningEntityDefinition.getJaxbClass().asSubclass(Containerable.class));
        HqlEntityInstance ownedEntity = getBaseHqlEntity();
        EntityReference subqueryEntity = subcontext.getHibernateQuery().getPrimaryEntity();

        return new Condition(context.getHibernateQuery()) {
            @Override
            public void dumpToHql(StringBuilder sb, int indent) {
                String subAlias = subqueryEntity.getAlias();
                // TODO building the query should be done by the subcontext somehow, including inner filter interpretation
                sb.append("exists (select 1 from ").append(subqueryEntity.getName()).append(' ').append(subAlias)
                        .append(" where ").append(subAlias).append(".oid = ")
                        .append(ownedEntity.getHqlPath()).append(".ownerOid)");
                System.out.println("sb = " + sb);
            }
        };
    }
}
