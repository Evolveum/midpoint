/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.api.info;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicResourceObjectSetType;

import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Information about the status of a smart integration operation executing in the background.
 *
 * @param token Identification token of the operation.
 * @param status Status of the operation, such as {@link OperationResultStatus#IN_PROGRESS} (must be set if the operation
 * is still in progress), {@link OperationResultStatus#SUCCESS} (operation was successfully completed),
 * {@link OperationResultStatus#FATAL_ERROR} (operation failed).
 * @param message Human-readable explanation of the status of the operation, if available.
 * @param request What does the operation deals with? TODO generalize this beyond the resource object set.
 * @param started When the operation was started. Normally not null, but may be null if there are some issues with scheduling.
 * @param finished When the operation was finished, successfully or not. Null if the operation is still in progress.
 * @param result Final result of the operation, if available.
 * @param <T> Type of the result.
 */
public record StatusInfo<T>(
        String token,
        OperationResultStatus status,
        @Nullable LocalizableMessage message,
        @Nullable BasicResourceObjectSetType request,
        @Nullable XMLGregorianCalendar started,
        @Nullable XMLGregorianCalendar finished,
        @Nullable T result) implements Serializable {

    public @Nullable QName getObjectClassName() {
        return request != null ? request.getObjectclass() : null;
    }

    public @Nullable String getObjectClassNameLocalPart() {
        var objectClassName = getObjectClassName();
        return objectClassName != null ? objectClassName.getLocalPart() : null;
    }
}
