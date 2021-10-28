/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.Collection;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Query diagnostics request: contains query to be executed (or at least translated) and some options.
 */
@Experimental
public class RepositoryQueryDiagRequest implements Serializable {

    private Class<? extends ObjectType> type;
    private ObjectQuery query;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    /**
     * We actually don't want this anymore.
     */
    @Deprecated
    private Serializable implementationLevelQuery;

    private boolean translateOnly;

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

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }

    public void setOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        this.options = options;
    }

    @Deprecated
    public Serializable getImplementationLevelQuery() {
        return implementationLevelQuery;
    }

    @Deprecated
    public void setImplementationLevelQuery(Serializable implementationLevelQuery) {
        this.implementationLevelQuery = implementationLevelQuery;
    }

    public boolean isTranslateOnly() {
        return translateOnly;
    }

    public void setTranslateOnly(boolean translateOnly) {
        this.translateOnly = translateOnly;
    }
}
