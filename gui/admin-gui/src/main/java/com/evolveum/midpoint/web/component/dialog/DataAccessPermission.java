/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public enum DataAccessPermission {
    SCHEMA_ACCESS,
    RAW_DATA_ACCESS,
    STATISTICS_ACCESS;

    public DataAccessPermissionType toSchemaType() {
        return switch (this) {
            case SCHEMA_ACCESS -> DataAccessPermissionType.SCHEMA_ACCESS;
            case RAW_DATA_ACCESS -> DataAccessPermissionType.RAW_DATA_ACCESS;
            case STATISTICS_ACCESS -> DataAccessPermissionType.STATISTICS_ACCESS;
        };
    }
}
