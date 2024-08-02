/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogQuery implements Serializable {

    public RoleCatalogQuery() {
    }

    private Class<? extends AbstractRoleType> type;

    private ObjectQuery query;

    private ObjectReferenceType parent;

    public Class<? extends AbstractRoleType> getType() {
        return type;
    }

    public void setType(Class<? extends AbstractRoleType> type) {
        this.type = type;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;

        this.parent = null;
    }

    public ObjectReferenceType getParent() {
        return parent;
    }

    public void setParent(ObjectReferenceType parent) {
        this.parent = parent;
    }

    public RoleCatalogQuery copy() {
        RoleCatalogQuery copy = new RoleCatalogQuery();
        copy.setType(type);
        copy.setQuery(query);
        copy.setParent(parent);
        return copy;
    }
}
