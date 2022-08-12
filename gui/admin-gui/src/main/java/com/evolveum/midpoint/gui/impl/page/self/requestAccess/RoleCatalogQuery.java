/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogQuery implements Serializable {

    public RoleCatalogQuery() {
    }

    private Class<? extends ObjectType> type;

    private ObjectQuery query;

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public void setType(Class<? extends ObjectType> type) {
        this.type = type;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }
}
