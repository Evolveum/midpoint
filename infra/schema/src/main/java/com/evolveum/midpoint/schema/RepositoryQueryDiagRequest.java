/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Query diagnostics request: contains query to be executed (or at least translated) and some options.
 */
@Experimental
public class RepositoryQueryDiagRequest implements Serializable {

    private Class<? extends Containerable> type;
    private ObjectQuery query;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    private boolean translateOnly;

    public Class<? extends Containerable> getType() {
        return type;
    }

    public void setType(Class<? extends Containerable> type) {
        this.type = type;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }

    public void setOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        this.options = options;
    }

    public boolean isTranslateOnly() {
        return translateOnly;
    }

    public void setTranslateOnly(boolean translateOnly) {
        this.translateOnly = translateOnly;
    }
}
