/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.dto;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;

import javax.xml.datatype.XMLGregorianCalendar;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

/**
 *  @author shood
 * */
public class UserReportDto implements Serializable{

    public static final String F_NAME = "name";
    public static final String F_FROM_GREG = "fromG";
    public static final String F_TO_GREG = "toG";
    public static final String F_FROM = "from";
    public static final String F_TO = "to";
    public static final String F_DESCRIPTION = "description";
    public static final String F_EXPORT_TYPE = "exportType";

    private XMLGregorianCalendar fromG;
    private XMLGregorianCalendar toG;
    private String name;
    private Date from;
    private Date to;
    private ExportType exportType;
    private String description;

    public XMLGregorianCalendar getFromG() {
        return MiscUtil.asXMLGregorianCalendar(from);
    }

    public void setFromG(XMLGregorianCalendar fromG) {
        this.from = MiscUtil.asDate(fromG);
        this.fromG = fromG;
    }

    public XMLGregorianCalendar getToG() {
        return MiscUtil.asXMLGregorianCalendar(to);
    }

    public void setToG(XMLGregorianCalendar toG) {
        this.to = MiscUtil.asDate(toG);
        this.toG = toG;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExportType getExportType() {
        return exportType;
    }

    public void setExportType(ExportType exportType) {
        this.exportType = exportType;
    }

    public Date getFrom() {
        if (from == null) {
            from = new Date();
        }
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        if (to == null) {
            to = new Date();
        }
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public ExportType getExport() {
        return exportType;
    }

    public void setExport(ExportType export) {
        this.exportType = export;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getDateFrom() {
        return new Timestamp(getFrom().getTime());
    }

    public Timestamp getDateTo() {
        return new Timestamp(getTo().getTime());
    }
}
