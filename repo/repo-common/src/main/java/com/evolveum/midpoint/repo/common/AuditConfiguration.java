/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditEventRecordingPropertyType;

/**
 * This class serves as simple POJO that is build from configuration available in {@link com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType}.
 * Data from this class is used in multiple audit helpers.
 *
 * @see AuditHelper
 */
public class AuditConfiguration {

    private final boolean recordResourceOids;

    private final List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord;

    private final ExpressionType eventRecordingExpression;

    private final boolean recordExternalServiceEvents;

    private final boolean recordExternalServiceData;

    public AuditConfiguration(boolean recordResourceOids, List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord,
            ExpressionType eventRecordingExpression, boolean recordExternalServiceEvents, boolean recordExternalServiceData) {
        this.recordResourceOids = recordResourceOids;
        this.propertiesToRecord = propertiesToRecord;
        this.eventRecordingExpression = eventRecordingExpression;
        this.recordExternalServiceEvents = recordExternalServiceEvents;
        this.recordExternalServiceData = recordExternalServiceData;
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

    public boolean isRecordExternalServiceEvents() {
        return recordExternalServiceEvents;
    }

    public boolean isRecordExternalServiceData() {
        return recordExternalServiceData;
    }
}
