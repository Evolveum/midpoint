/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignsSearchDto;

/**
 *  @author shood
 * */
public class CertCampaignsStorage implements PageStorage {
    private static final long serialVersionUID = 1L;

    private CertCampaignsSearchDto campaignsSearch;
    private ObjectPaging campaignsPaging;
    private Search search;
    private ViewToggle viewToggle = ViewToggle.TILE;

    public CertCampaignsSearchDto getCampaignsSearch() {
        return campaignsSearch;
    }

    public void setCampaignsSearch(CertCampaignsSearchDto campaignsSearch) {
        this.campaignsSearch = campaignsSearch;
    }

    @Override
    public ObjectPaging getPaging() {
        return campaignsPaging;
    }

    @Override
    public void setPaging(ObjectPaging tasksPaging) {
        this.campaignsPaging = tasksPaging;
    }

    @Override
    public Search getSearch() {
        return search;
    }

    @Override
    public void setSearch(Search search) {
        this.search = search;
    }

    public ViewToggle getViewToggle() {
        return viewToggle;
    }

    public void setViewToggle(ViewToggle viewToggle) {
        this.viewToggle = viewToggle;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("CertCampaignsStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "campaignsSearch", campaignsSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "campaignsPaging", campaignsPaging, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "search", search, indent+1);
        return sb.toString();
    }
}
