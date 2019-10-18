/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.search.Search;

/**
 * Created by honchar
 */
public class InducementsTabStorage implements PageStorage{

    private ObjectPaging inducementsPaging;


    @Override
    public Search getSearch() {
        return null;
    }

    @Override
    public void setSearch(Search search) {
    }

    @Override
    public ObjectPaging getPaging() {
        return inducementsPaging;
    }

    @Override
    public void setPaging(ObjectPaging inducementsPaging) {
        this.inducementsPaging = inducementsPaging;
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
