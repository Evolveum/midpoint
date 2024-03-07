/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.ldap;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public abstract class AbstractResourceController {

    protected PrismObject<ResourceType> resource;

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public AbstractResourceController setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
        return this;
    }

    public AbstractResourceController setResource(ResourceType resource) {
        this.resource = resource.asPrismObject();
        return this;
    }

    public ResourceType getResourceType() {
        return resource.asObjectable();
    }

    public ItemName getAttributeQName(String attributeName) {
        return new ItemName(MidPointConstants.NS_RI, attributeName);
    }

    public ItemPath getAttributePath(String attributeName) {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeQName(attributeName));
    }
}
