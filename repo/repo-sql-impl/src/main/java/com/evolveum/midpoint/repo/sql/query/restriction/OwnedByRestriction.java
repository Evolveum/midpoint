/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query.restriction;

import java.util.Objects;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OwnedByFilter;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaReferenceDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.EntityReference;
import com.evolveum.midpoint.repo.sql.query.hqm.GenericProjectionElement;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.resolution.HqlEntityInstance;
import com.evolveum.midpoint.repo.sql.query.resolution.ProperDataSearchResult;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class OwnedByRestriction extends Restriction<OwnedByFilter> {

    // TODO: Extend to cases, work items, etc...
    public static final Set<Class<?>> SUPPORTED_OWNED_TYPES = Set.of(AssignmentType.class);

    private final JpaEntityDefinition owningEntityDefinition;

    public static OwnedByRestriction create(
            InterpretationContext context, OwnedByFilter filter, JpaEntityDefinition baseEntityDefinition)
            throws QueryException {
        Class<?> ownedType = Objects.requireNonNull(baseEntityDefinition.getJaxbClass());
        if (!SUPPORTED_OWNED_TYPES.contains(ownedType)) {
            throw new QueryException("OwnedBy filter is not supported for type '"
                    + ownedType.getSimpleName() + "'; supported types are: " + SUPPORTED_OWNED_TYPES);
        }

        ItemPath path = filter.getPath();
        ComplexTypeDefinition type = filter.getType();
        // TODO better defaults based on owned type and optional path
        QName typeName = type != null ? type.getTypeName() : ObjectType.COMPLEX_TYPE;
        JpaEntityDefinition owningEntityDefinition =
                QueryDefinitionRegistry.getInstance().findEntityDefinition(typeName);
        if (path != null) {
            //noinspection unchecked
            ProperDataSearchResult<?> searchResult = context.getItemPathResolver().findProperDataDefinition(
                    owningEntityDefinition, path, null, JpaDataNodeDefinition.class, context.getPrismContext());

            if (searchResult == null) {
                throw new QueryException("Path for OwnedByFilter (" + path +
                        ") doesn't point to a hibernate entity or property within " + owningEntityDefinition);
            }
            JpaDataNodeDefinition<?> pathTargetDefinition = Objects.requireNonNull(searchResult.getTargetDefinition());
            if (pathTargetDefinition instanceof JpaReferenceDefinition<?>) {
                throw new QueryException("Path for OwnedByFilter (" + path +
                        ") is a reference, not a container of expected type '" + ownedType.getSimpleName() + "'.");
            }
            Class<?> targetType = pathTargetDefinition.getJaxbClass();
            if (!ownedType.equals(targetType)) {
                throw new QueryException("Path for OwnedByFilter (" + path + ") points to a type '"
                        + (targetType != null ? targetType.getSimpleName() : "?")
                        + "', expected type is '" + ownedType.getSimpleName() + "'.");
            }
        }
        return new OwnedByRestriction(context, filter, baseEntityDefinition, owningEntityDefinition);
    }

    private OwnedByRestriction(
            InterpretationContext context,
            OwnedByFilter filter,
            JpaEntityDefinition baseEntityDefinition,
            JpaEntityDefinition owningEntityDefinition) {
        // We don't provide parent, not relevant here; it is not harmful here either, but
        // see interpretFilter() where it would be - so we just keep it consistently null.
        super(context, filter, baseEntityDefinition, null);

        this.owningEntityDefinition = owningEntityDefinition;
    }

    @Override
    public Condition interpret() throws QueryException {
        //noinspection ConstantConditions
        InterpretationContext subcontext = context.createSubcontext(
                owningEntityDefinition.getJaxbClass().asSubclass(Containerable.class));
        HqlEntityInstance ownedEntity = getBaseHqlEntity();
        HibernateQuery subquery = subcontext.getHibernateQuery();
        EntityReference subqueryEntity = subquery.getPrimaryEntity();

        subquery.addProjectionElement(new GenericProjectionElement("1")); // select 1
        subquery.addCondition(
                subquery.createCompareXY(
                        subqueryEntity.getAlias() + ".oid", ownedEntity.getHqlPath() + ".ownerOid", "=", false));
        ObjectFilter innerFilter = filter.getFilter();
        if (innerFilter != null) {
            subquery.addCondition(
                    // Don't provide parent, it would only confuse filter evaluation.
                    subcontext.getInterpreter().interpretFilter(subcontext, filter.getFilter(), null));
        }

        // TODO introduce new ExistsCondition with subquery
        return new Condition(subquery) {
            @Override
            public void dumpToHql(StringBuilder sb, int indent) {
                HibernateQuery.indent(sb, indent);
                sb.append("exists (\n");
                hibernateQuery.dumpToHql(sb, indent + 1, false);
                sb.append('\n');
                HibernateQuery.indent(sb, indent);
                sb.append(')');

                System.out.println("sb = " + sb); // TODO out
            }
        };
    }
}
