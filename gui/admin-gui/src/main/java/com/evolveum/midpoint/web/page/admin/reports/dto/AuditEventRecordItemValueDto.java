/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
