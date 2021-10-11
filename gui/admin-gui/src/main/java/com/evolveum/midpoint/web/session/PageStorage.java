/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.session;

import java.io.Serializable;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.web.component.search.Search;

/**
 *  @author shood
 * */
public interface PageStorage extends Serializable, DebugDumpable {

    Search getSearch();

    void setSearch(Search search);

    void setPaging(ObjectPaging paging);

    ObjectPaging getPaging();

}
