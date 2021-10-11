/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.ldap;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public abstract class AbstractResourceController {

    protected PrismObject<ResourceType> resource;

    public String getNamespace() {
        return ResourceTypeUtil.getResourceNamespace(resource);
    }

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public ResourceType getResourceType() {
        return resource.asObjectable();
    }

    public QName getAttributeQName(String attributeName) {
        return new QName(getNamespace(), attributeName);
    }

    public ItemPath getAttributePath(String attributeName) {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeQName(attributeName));
    }

}
