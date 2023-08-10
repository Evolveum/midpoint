/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RepositoryObjectSetSpecificationImpl implements ObjectSetSpecification {

    @NotNull private final ObjectSetType objectSetBean;

    RepositoryObjectSetSpecificationImpl(@NotNull ObjectSetType objectSetBean) {
        this.objectSetBean = objectSetBean;
    }

    public QName getObjectType() {
        return objectSetBean.getType();
    }

    public QueryType getQueryBean() {
        return objectSetBean.getQuery();
    }

    public @NotNull List<ObjectReferenceType> getExplicitObjectReferences() {
        return objectSetBean.getObjectRef();
    }

    @Override
    public SelectorQualifiedGetOptionsType getSearchOptionsBean() {
        return objectSetBean.getSearchOptions();
    }

    public Boolean isUseRepositoryDirectly() {
        return objectSetBean.isUseRepositoryDirectly();
    }

    public @Nullable String getArchetypeOid() throws ConfigurationException {
        ObjectReferenceType archetypeRef = objectSetBean.getArchetypeRef();
        if (archetypeRef == null) {
            return null;
        }
        String oid = archetypeRef.getOid();
        if (oid == null) {
            throw new ConfigurationException("Archetype reference without OID: " + archetypeRef);
        }
        return oid;
    }

    @Override
    public String toString() {
        return "RepositoryObjectSetSpecificationImpl{" +
                "objectSetBean=" + objectSetBean +
                '}';
    }
}
