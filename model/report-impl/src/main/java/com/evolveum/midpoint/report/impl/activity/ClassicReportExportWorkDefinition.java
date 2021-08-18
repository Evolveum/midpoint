/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Work definition for classic report export.
 */
class ClassicReportExportWorkDefinition extends AbstractReportWorkDefinition {

    ClassicReportExportWorkDefinition(WorkDefinitionSource source) throws SchemaException {
        super(source);
    }

}
