/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.session.PageStorage;

import javax.xml.namespace.QName;

/**
 * @author skublik
 */
public class ContainerTabStorage implements PageStorage{

    private static final long serialVersionUID = 1L;

    private QName type = null;
    private ObjectPaging objectPaging;


    @Override
    public Search getSearch() {
        return null;
    }

    @Override
    public void setSearch(Search search) {
    }

    @Override
    public ObjectPaging getPaging() {
        return objectPaging;
    }

    @Override
    public void setPaging(ObjectPaging objectPaging) {
        this.objectPaging = objectPaging;
    }

    public QName getType() {
        return type;
    }

    public void setType(QName type) {
        this.type = type;
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
