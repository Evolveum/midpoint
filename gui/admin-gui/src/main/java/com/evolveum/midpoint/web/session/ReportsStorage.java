/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportOutputSearchDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportSearchDto;

/**
 * @author shood
 */
public class ReportsStorage implements PageStorage {
    private static final long serialVersionUID = 1L;

    /**
     * DTO used for search purposes in
     * {@link com.evolveum.midpoint.web.page.admin.reports.PageReports}
     */
    private ReportSearchDto reportSearch;

    /**
     * Paging DTO used in table on page
     * {@link com.evolveum.midpoint.web.page.admin.reports.PageReports}
     */
    private ObjectPaging reportsPaging;

    /**
     * DTO used for search purposes in
     * {@link com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports}
     */
    private ReportOutputSearchDto reportOutputSearch;

    /**
     * Paging DTO used in table on page
     * {@link com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports}
     */
    private ObjectPaging reportOutputsPaging;

    private Search search;

    public Search getSearch() {
        return search;
    }

    @Override
    public void setSearch(Search search) {
        this.search = search;
    }

    public ReportSearchDto getReportSearch() {
        return reportSearch;
    }

    public void setReportSearch(ReportSearchDto reportSearch) {
        this.reportSearch = reportSearch;
    }

    @Override
    public ObjectPaging getPaging() {
        return reportsPaging;
    }

    @Override
    public void setPaging(ObjectPaging reportsPaging) {
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

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ReportsStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "reportSearch", reportSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "reportsPaging", reportsPaging, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "reportOutputSearch", reportOutputSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "reportOutputsPaging", reportOutputsPaging, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "search", search, indent+1);
        return sb.toString();
    }
}
