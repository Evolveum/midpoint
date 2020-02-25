/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class TaskErrorSelectableBeanImpl<O extends ObjectType> extends SelectableBeanImpl<O> {
    public static final String F_OBJECT_REF_NAME = "objectRefName";
    public static final String F_STATUS = "status";
    public static final String F_MESSAGE = "message";
    public static final String F_ERROR_TIMESTAMP = "errorTimestamp";

    private String objectRefName;
    private OperationResultStatusType status;
    private String message;
    private String taskOid;
    private XMLGregorianCalendar errorTimestamp;

    public TaskErrorSelectableBeanImpl() {
    }

    public TaskErrorSelectableBeanImpl(@NotNull O object, @NotNull String taskOid) {
        objectRefName = object.getName().getOrig();

        for (OperationExecutionType execution : object.getOperationExecution()) {
            if (execution.getTaskRef() == null || !taskOid.equals(execution.getTaskRef().getOid())) {
                continue;
            }
            status = execution.getStatus();
            message = extractMessages(execution);
            errorTimestamp = execution.getTimestamp();
        }
    }

    private String extractMessages(OperationExecutionType execution) {
        List<String> messages = new ArrayList<>();
        for (ObjectDeltaOperationType deltaOperation : execution.getOperation()) {
            OperationResultType result = deltaOperation.getExecutionResult();
            if (result == null || result.getMessage() == null) {
                continue;
            }
            OperationResultStatusType status = result.getStatus();
            if (status != OperationResultStatusType.WARNING && status != OperationResultStatusType.FATAL_ERROR && status != OperationResultStatusType.PARTIAL_ERROR) {
                continue;
            }
            messages.add(result.getMessage());
        }
        return StringUtils.join(messages, "; ");
    }

    public XMLGregorianCalendar getErrorTimestamp() {
        return errorTimestamp;
    }

}
