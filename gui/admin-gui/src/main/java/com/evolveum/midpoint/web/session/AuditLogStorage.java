/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Created by honchar.
 */
public class AuditLogStorage implements PageStorage {
    private AuditEventRecordType auditRecord;
    private AuditSearchDto searchDto;
    private long pageNumber = 0;

    public AuditLogStorage() {
    }

    public Search getSearch() {
        return null;
    }

    public void setSearch(Search search) {

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

    @Override
    public ObjectPaging getPaging() {
        return null;
    }

    @Override
    public void setPaging(ObjectPaging auditLogPaging) {

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
