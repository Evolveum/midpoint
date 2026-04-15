/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.dialog.privacy;

import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.web.component.util.Describable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public enum DataAccessPermission implements Describable {
    STATISTICS_ACCESS(stringResource("DataAccessPermission.statisticalData.title"),
            stringResource("DataAccessPermission.statisticalData.description")),
    SCHEMA_ACCESS(stringResource("DataAccessPermission.schema.title"),
            stringResource("DataAccessPermission.schema.description")),
    RAW_DATA_ACCESS(stringResource("DataAccessPermission.rawData.title"),
            stringResource("DataAccessPermission.rawData.description"));

    private final StringResourceModel title;
    private final StringResourceModel description;

    DataAccessPermission(StringResourceModel title, StringResourceModel description) {
        this.title = title;
        this.description = description;
    }

    public StringResourceModel title() {
        return this.title;
    }

    public StringResourceModel description() {
        return this.description;
    }

    private static StringResourceModel stringResource(String key) {
        return new StringResourceModel(key).setDefaultValue(key);
    }

    public DataAccessPermissionType toSchemaType() {
        return switch (this) {
            case SCHEMA_ACCESS -> DataAccessPermissionType.SCHEMA_ACCESS;
            case RAW_DATA_ACCESS -> DataAccessPermissionType.RAW_DATA_ACCESS;
            case STATISTICS_ACCESS -> DataAccessPermissionType.STATISTICS_ACCESS;
        };
    }
}
