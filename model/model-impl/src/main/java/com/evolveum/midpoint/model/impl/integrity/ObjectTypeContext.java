/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Checker context related to one object type (resource + object class).
 *
 * @author Pavol Mederly
 */
class ObjectTypeContext {

    private PrismObject<ResourceType> resource;
    private RefinedObjectClassDefinition objectClassDefinition;
    private Map<QName, Map<String, Set<String>>> identifierValueMap = new HashMap<>();

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public RefinedObjectClassDefinition getObjectClassDefinition() {
        return objectClassDefinition;
    }

    public void setObjectClassDefinition(RefinedObjectClassDefinition objectClassDefinition) {
        this.objectClassDefinition = objectClassDefinition;
    }

    public Map<QName, Map<String, Set<String>>> getIdentifierValueMap() {
        return identifierValueMap;
    }
}
