/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.reports.dto;

/**
 * Temporary implementation. In the future, we might distinguish between property and reference values.
 */
public class AuditEventRecordItemValueDto {

    private final String name;
    private final String value;

    public static final String F_NAME = "name";
    public static final String F_VALUE = "value";

    public AuditEventRecordItemValueDto(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
