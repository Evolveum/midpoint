/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.session;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxConfigurationType;

/**
 *  @author shood
 * */
public interface PageStorage extends Serializable, DebugDumpable {

    Search getSearch();

    void setSearch(Search search);

    void setPaging(ObjectPaging paging);

    ObjectPaging getPaging();
}
