/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class AssociationSearchQueryKey extends QueryKey {

    private final QName mappingName;

    AssociationSearchQueryKey(
            Class<? extends ObjectType> type,
            Collection<ObjectQuery> queries,
            ObjectSearchStrategyType searchStrategy,
            ExpressionEvaluationContext eeCtx) {
        super(type, queries, searchStrategy);
        mappingName = eeCtx != null ? eeCtx.getMappingQName() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AssociationSearchQueryKey that = (AssociationSearchQueryKey) o;

        return Objects.equals(mappingName, that.mappingName);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mappingName != null ? mappingName.hashCode() : 0);
        return result;
    }
}
