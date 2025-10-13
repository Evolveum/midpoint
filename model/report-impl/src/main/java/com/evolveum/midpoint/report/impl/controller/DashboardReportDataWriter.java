/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Responsible for creating and manipulating text representation of an exported widget data.
 */
public interface DashboardReportDataWriter {

    /**
     * Sets the header row.
     */
    @NotNull Map<String, String> getWidgetsData();
}
