/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 *  TODO - get rid of XMLGregorianCalendar - Date conversions
 *
 * @author lazyman
 */
public class AuditReportDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_FROM_GREG = "fromG";
    public static final String F_TO_GREG = "toG";
    public static final String F_FROM = "from";
    public static final String F_TO = "to";
    public static final String F_AUDITEVENTTYPE = "auditEventType";
    public static final String F_DESCRIPTION = "description";
    public static final String F_EXPORT_TYPE = "exportType";

    private XMLGregorianCalendar fromG;
    private XMLGregorianCalendar toG;
    private String name;
    private Date from;
    private Date to;
    private AuditEventType auditEventType;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public AuditEventType getAuditEventType() {
        return auditEventType;
    }

    public void setAuditEventType(AuditEventType auditEventType) {
        this.auditEventType = auditEventType;
    }
    public Timestamp getDateFrom() {
        return new Timestamp(getFrom().getTime());
    }

    public Timestamp getDateTo() {
        return new Timestamp(getTo().getTime());
    }
}
