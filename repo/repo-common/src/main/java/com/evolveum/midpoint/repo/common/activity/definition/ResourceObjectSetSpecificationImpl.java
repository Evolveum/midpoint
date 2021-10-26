/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ResourceObjectSetSpecificationImpl implements ObjectSetSpecification {

    @NotNull private final ResourceObjectSetType resourceObjectSetBean;

    ResourceObjectSetSpecificationImpl(@NotNull ResourceObjectSetType resourceObjectSetBean) {
        this.resourceObjectSetBean = resourceObjectSetBean;
    }

    public @NotNull ResourceObjectSetType getResourceObjectSetBean() {
        return resourceObjectSetBean;
    }

    public QName getObjectType() {
        return ShadowType.COMPLEX_TYPE;
    }

    @Override
    public SelectorQualifiedGetOptionsType getSearchOptionsBean() {
        return resourceObjectSetBean.getSearchOptions();
    }

    @Override
    public String toString() {
        return "ResourceObjectSetSpecificationImpl{" +
                "resourceObjectSetBean=" + resourceObjectSetBean +
                '}';
    }
}
