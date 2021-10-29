/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.run.processing.CorrelatableProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.processing.GenericProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.IterativeActivityRun;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for export dashboard report line object.
 *
 * (This is needed for the activity framework to process {@link ExportDashboardReportLine} objects.)
 */
public class ExportDashboardReportLineProcessingRequest
        extends GenericProcessingRequest<ExportDashboardReportLine<Containerable>>
        implements CorrelatableProcessingRequest {

    ExportDashboardReportLineProcessingRequest(@NotNull ExportDashboardReportLine<Containerable> item,
            @NotNull IterativeActivityRun<ExportDashboardReportLine<Containerable>, ?, ?, ?> activityRun) {
        super(item.getLineNumber(), item, activityRun);
    }

    @Override
    public Object getCorrelationValue() {
        return getName();
    }

    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        return new IterationItemInformation(
                getName(),
                null,
                item.getContainer().asPrismContainerValue().getTypeName(),
                getItemOid());
    }

    private String getName() {
        ObjectType object = null;
        if (item.getContainer() instanceof ObjectType) {
            object = ((ObjectType) item.getContainer());
        }
        return (item.getWidgetIdentifier() != null ? ("widget " + item.getWidgetIdentifier()) : "") +
                (object != null ? ("object " + object.getName() + "(" + object.getOid() + ")") : "") +
                "line #" + item.getLineNumber();
    }

    @Override
    public @Nullable String getItemOid() {
        if (item.getContainer() instanceof ObjectType) {
            return ((ObjectType) item.getContainer()).getOid();
        }
        return super.getItemOid();
    }
}
