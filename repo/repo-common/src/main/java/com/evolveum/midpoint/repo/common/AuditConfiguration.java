/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditEventRecordingPropertyType;

public class AuditConfiguration {

    private final boolean recordResourceOids;

    private final List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord;

    private final ExpressionType eventRecordingExpression;

    public AuditConfiguration(boolean recordResourceOids, List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord, ExpressionType eventRecordingExpression) {
        this.recordResourceOids = recordResourceOids;
        this.propertiesToRecord = propertiesToRecord;
        this.eventRecordingExpression = eventRecordingExpression;
    }

    public boolean isRecordResourceOids() {
        return recordResourceOids;
    }

    public List<SystemConfigurationAuditEventRecordingPropertyType> getPropertiesToRecord() {
        return propertiesToRecord;
    }

    public ExpressionType getEventRecordingExpression() {
        return eventRecordingExpression;
    }
}
