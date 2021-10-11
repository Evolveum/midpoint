/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;
import java.util.Collection;

/**
 * Query diagnostics request: contains query to be executed (or at least translated) and some options.
 *
 * EXPERIMENTAL, will probably change
 *
 * @author mederly
 */
@Experimental
public class RepositoryQueryDiagRequest implements Serializable {

    private Class<? extends ObjectType> type;
    private ObjectQuery query;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    private Serializable implementationLevelQuery;                // this is used if specified

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

    public Serializable getImplementationLevelQuery() {
        return implementationLevelQuery;
    }

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
