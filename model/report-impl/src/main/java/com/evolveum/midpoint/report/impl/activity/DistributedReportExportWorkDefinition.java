/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Work definition for multi-node report export.
 */
public class DistributedReportExportWorkDefinition extends AbstractReportWorkDefinition {

    DistributedReportExportWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) throws ConfigurationException {
        super(info);
    }
}
