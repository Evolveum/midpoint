/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Handler;

import java.util.Collection;

/**
 * Represents a source of data for a collection-based report.
 */
public interface ReportDataSource<C extends Containerable> {

    /**
     * Initializes the data source - by specifying the search it should execute later.
     */
    void initialize(Class<C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options);

    /**
     * Executes the search and feeds the handler with the data.
     *
     * TODO not used yet
     */
    void run(Handler<C> handler, OperationResult result);
}
