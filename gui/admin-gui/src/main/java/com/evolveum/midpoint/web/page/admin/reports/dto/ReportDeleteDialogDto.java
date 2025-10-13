/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.reports.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

import java.io.Serializable;
import java.util.List;

public class ReportDeleteDialogDto implements Serializable{

    public enum Operation{
        DELETE_SINGLE, DELETE_SELECTED, DELETE_ALL
    }

    private Operation operation;
    private List<ReportDataType> objects;

    public ReportDeleteDialogDto(Operation op, List<ReportDataType> objects){
        this.operation = op;
        this.objects = objects;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public List<ReportDataType> getObjects() {
        return objects;
    }

    public void setObjects(List<ReportDataType> objects) {
        this.objects = objects;
    }
}
