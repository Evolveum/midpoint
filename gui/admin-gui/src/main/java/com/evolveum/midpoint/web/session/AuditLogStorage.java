/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by honchar.
 */
public class AuditLogStorage implements PageStorage {
    private AuditEventRecordType auditRecord;
    private AuditSearchDto searchDto;
    private long pageNumber = 0;
    private ObjectPaging auditLogPaging;

    private Search search;
    private XMLGregorianCalendar fromDate;
    private XMLGregorianCalendar toDate;

    public AuditLogStorage() {
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
    }

    public AuditSearchDto getSearchDto() {
        if (searchDto == null){
            searchDto = new AuditSearchDto();
        }
        return searchDto;
    }

    public void setSearchDto(AuditSearchDto searchDto) {
        this.searchDto = searchDto;
    }

    public long getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(long pageNumber) {
        this.pageNumber = pageNumber;
    }

    public XMLGregorianCalendar getFromDate() {
        return fromDate;
    }

    public void setFromDate(XMLGregorianCalendar fromDate) {
        this.fromDate = fromDate;
    }

    public XMLGregorianCalendar getToDate() {
        return toDate;
    }

    public void setToDate(XMLGregorianCalendar toDate) {
        this.toDate = toDate;
    }

    @Override
    public ObjectPaging getPaging() {
        return auditLogPaging;
    }

    @Override
    public void setPaging(ObjectPaging auditLogPaging) {
        this.auditLogPaging = auditLogPaging;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        return "";
    }

    public AuditEventRecordType getAuditRecord() {
        return auditRecord;
    }

    public void setAuditRecord(AuditEventRecordType auditRecord) {
        this.auditRecord = auditRecord;
    }
}
