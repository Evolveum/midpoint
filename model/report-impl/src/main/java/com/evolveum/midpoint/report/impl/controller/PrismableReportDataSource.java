/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.impl.activity.ExportActivitySupport;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * TODO:
 * Currently this is any T, but we want this to support some future Prism-able.
 * At this moment there is no common type for Referencable and Containerable.
 */
public class PrismableReportDataSource<T> implements ReportDataSource<T> {

    private Class<T> type;
    private ObjectQuery query;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    @NotNull private final ExportActivitySupport support;

    public PrismableReportDataSource(@NotNull ExportActivitySupport support) {
        this.support = support;
    }

    @Override
    public void initialize(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) {
        this.type = type;
        this.query = query;
        this.options = options;
    }

    @NotNull
    public Class<T> getType() {
        return type;
    }

    @NotNull
    public ObjectQuery getQuery() {
        return query;
    }

    @NotNull
    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }

    @Override
    public void run(ObjectHandler<T> handler, OperationResult result) throws CommonException {
        support.searchRecordsIteratively(type, query, handler, options, result);
    }
}
