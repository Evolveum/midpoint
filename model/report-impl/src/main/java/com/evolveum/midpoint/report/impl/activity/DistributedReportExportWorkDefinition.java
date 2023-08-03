/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * Work definition for multi-node report export.
 */
public class DistributedReportExportWorkDefinition extends AbstractReportWorkDefinition {

    DistributedReportExportWorkDefinition(@NotNull WorkDefinitionBean source, @NotNull QName activityTypeName)
            throws SchemaException {
        super(source, activityTypeName);
    }
}
