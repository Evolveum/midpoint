/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.util.Collection;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Represents a source of data for a collection-based report.
 */
public interface ReportDataSource<T> {

    /**
     * Initializes the data source - by specifying the search it should execute later.
     */
    void initialize(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options);

    /**
     * Executes the search and feeds the handler with the data.
     */
    void run(ObjectHandler<T> handler, OperationResult result) throws CommonException;
}
