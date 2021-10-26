/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

public class RepositoryObjectSetSpecificationImpl implements ObjectSetSpecification {

    private final ObjectSetType objectSetBean;

    RepositoryObjectSetSpecificationImpl(ObjectSetType objectSetBean) {
        this.objectSetBean = objectSetBean;
    }

    public QName getObjectType() {
        return objectSetBean.getType();
    }

    public QueryType getQueryBean() {
        return objectSetBean.getQuery();
    }

    @Override
    public SelectorQualifiedGetOptionsType getSearchOptionsBean() {
        return objectSetBean.getSearchOptions();
    }

    public Boolean isUseRepositoryDirectly() {
        return objectSetBean.isUseRepositoryDirectly();
    }

    @Override
    public String toString() {
        return "RepositoryObjectSetSpecificationImpl{" +
                "objectSetBean=" + objectSetBean +
                '}';
    }
}
