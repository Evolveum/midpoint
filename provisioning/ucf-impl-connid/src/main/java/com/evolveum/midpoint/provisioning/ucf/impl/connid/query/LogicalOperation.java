/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid.query;

import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class LogicalOperation extends Operation {

    private static final Trace LOGGER = TraceManager.getTrace(LogicalOperation.class);

    LogicalOperation(FilterInterpreter interpreter) {
        super(interpreter);
    }

    @Override
    public <T> Filter interpret(ObjectFilter objectFilter, ConnIdNameMapper icfNameMapper) throws SchemaException {

        if (objectFilter instanceof NotFilter) {
            NotFilter not = (NotFilter) objectFilter;
            if (not.getFilter() == null) {
                LOGGER.debug("Not filter does not contain any condition. Skipping processing not filter.");
                return null;
            }

            Filter f = getInterpreter().interpret(not.getFilter(), icfNameMapper);
            return FilterBuilder.not(f);
        } else {

            NaryLogicalFilter nAry = (NaryLogicalFilter) objectFilter;
            List<? extends ObjectFilter> conditions =  nAry.getConditions();
            if (conditions == null || conditions.isEmpty()){
                LOGGER.debug("No conditions specified for logical filter. Skipping processing logical filter.");
                return null;
            }
            if (conditions.size() < 2) {
                LOGGER.debug("Logical filter contains only one condition. Skipping processing logical filter and process simple operation of type {}.", conditions.get(0).getClass().getSimpleName());
                return getInterpreter().interpret(conditions.get(0), icfNameMapper);
            }

            List<Filter> filters = new ArrayList<>();
            for (ObjectFilter objFilter : nAry.getConditions()){
                Filter f = getInterpreter().interpret(objFilter, icfNameMapper);
                filters.add(f);
            }

            Filter nAryFilter = null;
            if (filters.size() >= 2) {
                if (nAry instanceof AndFilter) {
                    nAryFilter = interpretAnd(filters.get(0), filters.subList(1, filters.size()));
                } else if (nAry instanceof OrFilter) {
                    nAryFilter = interpretOr(filters.get(0), filters.subList(1, filters.size()));
                }
            }
            return nAryFilter;
        }

    }

    private Filter interpretAnd(Filter andF, List<Filter> filters) {

        if (filters.size() == 0) {
            return andF;
        }

        andF = FilterBuilder.and(andF, filters.get(0));
        andF = interpretAnd(andF, filters.subList(1, filters.size()));
        return andF;
    }

    private Filter interpretOr(Filter orF, List<Filter> filters) {

        if (filters.size() == 0) {
            return orF;
        }

        orF = FilterBuilder.or(orF, filters.get(0));
        orF = interpretOr(orF, filters.subList(1, filters.size()));
        return orF;
    }
}
