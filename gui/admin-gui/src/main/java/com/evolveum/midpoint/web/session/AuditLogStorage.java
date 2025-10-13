/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.gui.impl.component.search.Search;

/**
 * Created by honchar.
 */
public class AuditLogStorage implements PageStorage {
    private ObjectPaging auditLogPaging;

    private Search search;

    public AuditLogStorage() {
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
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

}
