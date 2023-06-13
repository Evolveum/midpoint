/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.selector.spec.SelectorClause;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;

/** TODO describe */
public interface FilterCollector {

    void addConjunct(SelectorClause clause, ObjectFilter conjunct);

    ObjectFilter getFilter();

    static FilterCollector defaultOne() {
        return new Default();
    }

    void nullToAll();

    class Default implements FilterCollector {

        private TypeFilter emptyTypeFilter;
        private ObjectFilter filter;

        private Default() {
        }

        @Override
        public void addConjunct(SelectorClause clause, ObjectFilter conjunct) {
            if (conjunct instanceof TypeFilter && ((TypeFilter) conjunct).getFilter() == null) {
                if (emptyTypeFilter == null) {
                    emptyTypeFilter = ((TypeFilter) conjunct);
                } else {
                    // This could occur if the "filter" clause provides a type filter; but it should not.
                    throw new IllegalStateException("Two type filters?! " + emptyTypeFilter + " and " + conjunct);
                }
            } else {
                filter = ObjectQueryUtil.filterAnd(filter, conjunct);
            }
        }

        @Override
        public ObjectFilter getFilter() {
            if (emptyTypeFilter != null) {
                var typeFilter = emptyTypeFilter.clone();
                typeFilter.setFilter(filter);
                return typeFilter;
            } else {
                return filter;
            }
        }

        @Override
        public void nullToAll() {
            if (filter == null) {
                filter = FilterCreationUtil.createAll();
            }
        }
    }
}
