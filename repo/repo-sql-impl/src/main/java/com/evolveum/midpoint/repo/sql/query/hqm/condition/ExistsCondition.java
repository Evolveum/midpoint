/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query.hqm.condition;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.hqm.GenericProjectionElement;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * Generic EXISTS condition with subquery provided as a parameter.
 * This produces "exists (select 1 from ...)" type query.
 * This condition also handles RObject vs AssignmentHolderType discrepancy and filters out shadows if necessary.
 */
public class ExistsCondition extends Condition {

    private final InterpretationContext subcontext;

    public ExistsCondition(InterpretationContext subcontext) {
        super(subcontext.getHibernateQuery()); // -> this.hibernateQuery
        this.subcontext = subcontext;

        hibernateQuery.addProjectionElement(new GenericProjectionElement("1")); // select 1

        // AssignmentHolderType is mapped to RObject which also includes RShadow, so little fix is needed:
        if (subcontext.getType().equals(AssignmentHolderType.class)) {
            hibernateQuery.addCondition(
                    hibernateQuery.createNot(
                            hibernateQuery.createEq(
                                    entityAlias() + '.' + RObject.F_OBJECT_TYPE_CLASS,
                                    RObjectType.SHADOW)));
        }
    }

    /**
     * Convenient method to add simple correlation equals condition.
     *
     * @param subqueryAttribute "column" name on the subquery entity (part after .)
     * @param otherPath whole path for the other part of the equals (`outerEntity.column`)
     */
    public void addCorrelationCondition(String subqueryAttribute, String otherPath) {
        hibernateQuery.addCondition(
                hibernateQuery.createCompareXY(
                        entityAlias() + '.' + subqueryAttribute,
                        otherPath,
                        "=", false));
    }

    public void interpretFilter(@Nullable ObjectFilter filter) throws QueryException {
        if (filter != null) {
            hibernateQuery.addCondition(
                    // We don't provide parent, it would only confuse filter evaluation.
                    subcontext.getInterpreter().interpretFilter(subcontext, filter, null));
        }
    }

    /** Returns the name of alias for main entity of the subquery. */
    public String entityAlias() {
        return hibernateQuery.getPrimaryEntity().getAlias();
    }

    @Override
    public void dumpToHql(StringBuilder sb, int indent) {
        HibernateQuery.indent(sb, indent);
        sb.append("exists (\n");
        hibernateQuery.dumpToHql(sb, indent + 1, false);
        sb.append('\n');
        HibernateQuery.indent(sb, indent);
        sb.append(')');
    }
}
