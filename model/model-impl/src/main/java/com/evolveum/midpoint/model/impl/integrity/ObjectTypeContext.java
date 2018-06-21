/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Checker context related to one object type (resource + kind).
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
