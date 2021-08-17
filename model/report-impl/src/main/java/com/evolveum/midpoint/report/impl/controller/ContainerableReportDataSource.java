/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.impl.activity.ExportActivitySupport;
import com.evolveum.midpoint.report.impl.controller.ReportDataSource;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Handler;

import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class ContainerableReportDataSource implements ReportDataSource<Containerable> {

    private Class<Containerable> type;
    private ObjectQuery query;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    @NotNull private final ExportActivitySupport support;

    public ContainerableReportDataSource(ExportActivitySupport support) {
        this.support = support;
    }

    @Override
    public void initialize(Class<Containerable> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) {
        this.type = type;
        this.query = query;
        this.options = options;
    }

    @NotNull
    public Class<Containerable> getType() {
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
    public void run(Handler<Containerable> handler, OperationResult result) throws CommonException {
        List<? extends Containerable> objects = support.searchRecords(
                type,
                query,
                options,
                result);
        objects.forEach(handler::handle);
    }
}
