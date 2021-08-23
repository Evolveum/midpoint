/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
