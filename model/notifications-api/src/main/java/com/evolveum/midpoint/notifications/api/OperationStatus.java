/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;

/**
 * The purpose of this class is unclear. Consider replacing by {@link OperationResultStatus}.
 */
public enum OperationStatus {
    SUCCESS, IN_PROGRESS, FAILURE, OTHER;

    public boolean matchesEventStatusType(EventStatusType eventStatusType) {
        switch (eventStatusType) {
            case ALSO_SUCCESS:
            case SUCCESS: return this == OperationStatus.SUCCESS;
            case ONLY_FAILURE:
            case FAILURE: return this == OperationStatus.FAILURE;
            case IN_PROGRESS: return this == OperationStatus.IN_PROGRESS;
            default: throw new IllegalStateException("Invalid eventStatusType: " + eventStatusType);
        }
    }

    public static OperationStatus fromOperationResultStatus(OperationResultStatus status) {
        if (status == null) {
            return null;
        } else if (status.isConsideredSuccess()) {
            return SUCCESS;
        } else if (status.isError()) {
            return FAILURE; // TODO or only fatal error?
        } else {
            return OTHER;
        }
    }
}
