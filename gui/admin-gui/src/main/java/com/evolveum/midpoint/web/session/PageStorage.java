/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.session;

import java.io.Serializable;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.gui.impl.component.search.Search;

/**
 *  @author shood
 * */
public interface PageStorage extends Serializable, DebugDumpable {

    Search getSearch();

    void setSearch(Search search);

    void setPaging(ObjectPaging paging);

    ObjectPaging getPaging();

    /**
     * Detaches the models of the stored search item wrappers, e.g. so that their labels are reloaded after
     * the GUI locale has changed.
     */
    default void detachSearchItemWrappersModels() {
        Search search = getSearch();
        if (search != null) {
            search.detachSearchItemWrappersModels();
        }
    }
}
