/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.dto;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Task error bean for native repository.
 */
public class TaskErrorSelectableBeanImpl extends SelectableBeanImpl<OperationExecutionType> {
    public static final String F_OBJECT_REF_NAME = "objectName";
    public static final String F_STATUS = "status";
    public static final String F_MESSAGE = "message";
    public static final String F_ERROR_TIMESTAMP = "errorTimestamp";
    public static final String F_RECORD_TYPE = "recordType";
    public static final String F_REAL_OWNER_DESCRIPTION = "realOwnerDescription";

    private ObjectType realOwner;
    private OperationResultStatusType status;
    private String message;
    private String taskOid;
    private XMLGregorianCalendar errorTimestamp;
    private OperationExecutionRecordTypeType recordType;
    private String objectName;

    public TaskErrorSelectableBeanImpl() {
    }

    public TaskErrorSelectableBeanImpl(@NotNull OperationExecutionType opex) {
        //TODO: better identification? e.g. if it is shadow, display also resource for the shadow?
        // if it is user, display type? or rather 'midpoint' representing local repo?
        // of would it be better to have separate column for it?
        realOwner = ObjectTypeUtil.getParentObject(opex);

        status = opex.getStatus();
        message = opex.getMessage();
        errorTimestamp = opex.getTimestamp();
        recordType = opex.getRecordType();
        // TODO: make up our mind about "real" real owner? realOwner is probably just "owner"
        //  see OperationExecutionRecordRealOwnerType about really real owner
        objectName = extractRealOwner(opex, realOwner);
    }

    public XMLGregorianCalendar getErrorTimestamp() {
        return errorTimestamp;
    }

    private String extractRealOwner(@NotNull OperationExecutionType execution, ObjectType object) {
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

    private String getOwnerType(@NotNull OperationExecutionRecordRealOwnerType realOwnerType) {
        QName objectType = realOwnerType.getObjectType();
        if (objectType == null) {
            return "Unknown type";
        }
        return objectType.getLocalPart();

    }

    public PrismObject<ObjectType> getRealOwner() {
        //noinspection unchecked
        return realOwner != null ? (PrismObject<ObjectType>) realOwner.asPrismObject() : null;
    }

    public String getRealOwnerDescription() {
        if (realOwner == null) {
            return "";
        }
        return WebComponentUtil.getName(realOwner) + " (" + realOwner.getClass().getSimpleName() + ")";
    }
}
