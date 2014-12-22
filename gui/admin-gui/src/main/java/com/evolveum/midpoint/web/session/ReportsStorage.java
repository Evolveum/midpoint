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
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportOutputSearchDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportSearchDto;

/**
 * @author shood
 */
public class ReportsStorage extends PageStorage {

    /**
     * DTO used for search purposes in {@link com.evolveum.midpoint.web.page.admin.reports.PageReports}
     */
    private ReportSearchDto reportSearch;

    /**
     * Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.reports.PageReports}
     */
    private ObjectPaging reportsPaging;

    /**
     * DTO used for search purposes in {@link com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports}
     */
    private ReportOutputSearchDto reportOutputSearch;

    /**
     * Paging DTO used in table on page {@link com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports}
     */
    private ObjectPaging reportOutputsPaging;

    public ReportSearchDto getReportSearch() {
        return reportSearch;
    }

    public void setReportSearch(ReportSearchDto reportSearch) {
        this.reportSearch = reportSearch;
    }

    public ObjectPaging getReportsPaging() {
        return reportsPaging;
    }

    public void setReportsPaging(ObjectPaging reportsPaging) {
        this.reportsPaging = reportsPaging;
    }

    public ObjectPaging getReportOutputsPaging() {
        return reportOutputsPaging;
    }

    public void setReportOutputsPaging(ObjectPaging reportOutputsPaging) {
        this.reportOutputsPaging = reportOutputsPaging;
    }

    public ReportOutputSearchDto getReportOutputSearch() {
        return reportOutputSearch;
    }

    public void setReportOutputSearch(ReportOutputSearchDto reportOutputSearch) {
        this.reportOutputSearch = reportOutputSearch;
    }
}
