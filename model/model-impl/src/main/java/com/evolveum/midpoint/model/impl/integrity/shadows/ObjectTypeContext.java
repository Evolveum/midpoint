/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TODO update the description + decide whether we want to keep object type definition or object type/class definition
 *
 * This is wrong: Checker context related to one object type (resource + object class).
 */
class ObjectTypeContext {

    private PrismObject<ResourceType> resource;
    private ResourceObjectTypeDefinition objectTypeDefinition;
    private final Map<QName, Map<String, Set<String>>> identifierValueMap = new HashMap<>();

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public ResourceObjectTypeDefinition getObjectTypeDefinition() {
        return objectTypeDefinition;
    }

    public void setObjectTypeDefinition(ResourceObjectTypeDefinition objectTypeDefinition) {
        this.objectTypeDefinition = objectTypeDefinition;
    }

    public Map<QName, Map<String, Set<String>>> getIdentifierValueMap() {
        return identifierValueMap;
    }
}
