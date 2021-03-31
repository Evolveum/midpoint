/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class TaskErrorSelectableBeanImpl<O extends ObjectType> extends SelectableBeanImpl<O> {
    public static final String F_OBJECT_REF_NAME = "objectName";
    public static final String F_STATUS = "status";
    public static final String F_MESSAGE = "message";
    public static final String F_ERROR_TIMESTAMP = "errorTimestamp";
    public static final String F_RECORD_TYPE = "recordType";
    public static final String F_REAL_OWNER_DESCRIPTION = "realOwnerDescription";

    private O realOwner;
    private OperationResultStatusType status;
    private String message;
    private String taskOid;
    private XMLGregorianCalendar errorTimestamp;
    private OperationExecutionRecordTypeType recordType;
    private String objectName;

    public TaskErrorSelectableBeanImpl() {
    }

    public TaskErrorSelectableBeanImpl(@NotNull O object, @NotNull String taskOid) {
        //TODO: better identification? e.g. if it is shadow, display also resource for the shadow?
        // if it is user, display type? or rather 'midpoint' representing local repo?
        // of would it be better to have sepparate column for it?
        this.realOwner = object;

        for (OperationExecutionType execution : object.getOperationExecution()) {
            if (execution.getTaskRef() == null || !taskOid.equals(execution.getTaskRef().getOid())) {
                continue;
            }
            status = execution.getStatus();
            message = extractMessages(execution);
            errorTimestamp = execution.getTimestamp();
            recordType = execution.getRecordType();
            objectName = extractRealOwner(execution, object);
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

    private String extractRealOwner(@NotNull  OperationExecutionType execution, O object) {
        OperationExecutionRecordRealOwnerType realOwnerType = execution.getRealOwner();
        if (realOwnerType == null) {
            return WebComponentUtil.getName(object);
        }

        String identification = realOwnerType.getIdentification();
        String type = getOwnerType(realOwnerType);
        if (identification == null) {
            return type;
        }

        return identification;
    }

    private String getOwnerType(@NotNull  OperationExecutionRecordRealOwnerType realOwnerType) {
        QName objectType = realOwnerType.getObjectType();
        if (objectType == null) {
            return "Unknown type";
        }
        return objectType.getLocalPart();

    }

    public PrismObject<O> getRealOwner() {
        return (PrismObject<O>) realOwner.asPrismObject();
    }

    public String getRealOwnerDescription() {
        return WebComponentUtil.getName(realOwner) + " (" + realOwner.getClass().getSimpleName() + ")";
    }
}
