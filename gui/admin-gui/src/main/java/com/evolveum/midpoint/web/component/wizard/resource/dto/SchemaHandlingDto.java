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
    private ResourceObjectTypeDefinitionTypeDto selected;
    private String selectedObjectClass;
    private List<QName> objectClassList;

    public List<ResourceObjectTypeDefinitionTypeDto> getObjectTypeList() {
        return objectTypeList;
    }

    public void setObjectTypeList(List<ResourceObjectTypeDefinitionTypeDto> objectTypeList) {
        this.objectTypeList = objectTypeList;
    }

    public ResourceObjectTypeDefinitionTypeDto getSelected() {
        return selected;
    }

    public void setSelected(ResourceObjectTypeDefinitionTypeDto selected) {
        this.selected = selected;

        if (selected == null) {
            selectedObjectClass = null;
        } else if (selected.getObjectType().getObjectClass() != null) {
            selectedObjectClass = selected.getObjectType().getObjectClass().getLocalPart();
        } else if (selected.getObjectType().getObjectClass() == null) {
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
		if (selected != null) {
			selected.getObjectType().setObjectClass(findObjectClassQName(selectedObjectClass));	// update object class in selected objectType container
		}
    }

	private QName findObjectClassQName(String localName) {
		if (localName == null) {
			return null;
		}
		for (QName q: objectClassList) {
			if (localName.equals(q.getLocalPart())) {
				return q;
			}
		}
		return null;
		//throw new IllegalStateException("No " + localName + " in object class list: " + objectClassList);
	}
}
