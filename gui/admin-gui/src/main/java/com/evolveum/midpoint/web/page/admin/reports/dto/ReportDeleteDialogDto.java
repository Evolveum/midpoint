/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;

import java.io.Serializable;
import java.util.List;

public class ReportDeleteDialogDto implements Serializable{

    public enum Operation{
        DELETE_SINGLE, DELETE_SELECTED, DELETE_ALL
    }

    private Operation operation;
    private List<ReportOutputType> objects;

    public ReportDeleteDialogDto(Operation op, List<ReportOutputType> objects){
        this.operation = op;
        this.objects = objects;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public List<ReportOutputType> getObjects() {
        return objects;
    }

    public void setObjects(List<ReportOutputType> objects) {
        this.objects = objects;
    }
}
