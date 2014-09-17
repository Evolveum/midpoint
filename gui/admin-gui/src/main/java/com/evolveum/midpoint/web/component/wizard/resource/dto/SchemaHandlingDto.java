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

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class SchemaHandlingDto implements Serializable{

    public static final String F_OBJECT_TYPES = "objectTypeList";
    public static final String F_SELECTED = "selected";
    public static final String F_SELECTED_OBJECT_CLASS = "selectedObjectClass";

    private List<ResourceObjectTypeDefinitionTypeDto> objectTypeList = new ArrayList<>();
    private ResourceObjectTypeDefinitionType selected;
    private String selectedObjectClass;
    private List<QName> objectClassList;

    public List<ResourceObjectTypeDefinitionTypeDto> getObjectTypeList() {
        return objectTypeList;
    }

    public void setObjectTypeList(List<ResourceObjectTypeDefinitionTypeDto> objectTypeList) {
        this.objectTypeList = objectTypeList;
    }

    public ResourceObjectTypeDefinitionType getSelected() {
        return selected;
    }

    public void setSelected(ResourceObjectTypeDefinitionType selected) {
        this.selected = selected;

        if(selected == null){
            selectedObjectClass = null;
        } else if(selected.getObjectClass() != null){
            selectedObjectClass = selected.getObjectClass().getLocalPart();
        } else if(selected.getObjectClass() == null){
            selectedObjectClass = null;
        }
    }

    public List<QName> getObjectClassList() {
        return objectClassList;
    }

    public void setObjectClassList(List<QName> objectClassList) {
        this.objectClassList = objectClassList;
    }

    public String getSelectedObjectClass() {
        return selectedObjectClass;
    }

    public void setSelectedObjectClass(String selectedObjectClass) {
        this.selectedObjectClass = selectedObjectClass;

        if(selectedObjectClass == null && selected != null){
            selected.setObjectClass(null);
        }

        if(selected != null){
            for(QName q: objectClassList){
                if(q.getLocalPart().equals(selectedObjectClass)){
                    selected.setObjectClass(q);
                }
            }
        }
    }
}
