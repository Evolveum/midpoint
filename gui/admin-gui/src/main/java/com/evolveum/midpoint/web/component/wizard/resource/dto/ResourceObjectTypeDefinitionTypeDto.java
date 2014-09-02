/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class ResourceObjectTypeDefinitionTypeDto implements Serializable{

    public static final String F_SELECTED = "selected";
    public static final String F_OBJECT_TYPE = "objectType";

    private boolean selected = false;
    private ResourceObjectTypeDefinitionType objectType;

    public ResourceObjectTypeDefinitionTypeDto(){}

    public ResourceObjectTypeDefinitionTypeDto(ResourceObjectTypeDefinitionType objectType){
        this.objectType = objectType;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public ResourceObjectTypeDefinitionType getObjectType() {
        return objectType;
    }

    public void setObjectType(ResourceObjectTypeDefinitionType objectType) {
        this.objectType = objectType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceObjectTypeDefinitionTypeDto)) return false;

        ResourceObjectTypeDefinitionTypeDto that = (ResourceObjectTypeDefinitionTypeDto) o;

        if (selected != that.selected) return false;
        if (objectType != null ? !objectType.equals(that.objectType) : that.objectType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (selected ? 1 : 0);
        result = 31 * result + (objectType != null ? objectType.hashCode() : 0);
        return result;
    }
}
