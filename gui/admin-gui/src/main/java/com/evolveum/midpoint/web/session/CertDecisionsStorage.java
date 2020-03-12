/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;

/**
 *  @author honchar
 * */
public class CertDecisionsStorage implements PageStorage {
    private static final long serialVersionUID = 1L;

    private ObjectPaging certDecisionsPaging;
    private Search search;
    private Boolean showNotDecidedOnly = Boolean.TRUE;

    public Boolean getShowNotDecidedOnly() {
        return showNotDecidedOnly;
    }

    public void setShowNotDecidedOnly(Boolean showNotDecidedOnly) {
        this.showNotDecidedOnly = showNotDecidedOnly;
    }

    @Override
    public ObjectPaging getPaging() {
        return certDecisionsPaging;
    }

    @Override
    public void setPaging(ObjectPaging certDecisionsPaging) {
        this.certDecisionsPaging = certDecisionsPaging;
    }

    @Override
    public Search getSearch() {
        return search;
    }

    @Override
    public void setSearch(Search search) {
        this.search = search;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("CertDecisionsStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "showNotDecidedOnly", showNotDecidedOnly, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "campaignsPaging", certDecisionsPaging, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "search", search, indent+1);
        return sb.toString();
    }

}
