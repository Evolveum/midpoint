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

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/**
 *  @author shood
 * */
public class SchemaHandlingDto implements Serializable {

    public static final String F_OBJECT_TYPES = "objectTypeList";
    public static final String F_SELECTED = "selected";
    public static final String F_OBJECT_CLASS_NAME = "objectClassName";

    @NotNull private final List<ResourceObjectTypeDefinitionTypeDto> objectTypeList;
    private ResourceObjectTypeDefinitionTypeDto selected;
    private String objectClassName;
    private List<QName> objectClassList;

	public SchemaHandlingDto(@NotNull List<ResourceObjectTypeDefinitionTypeDto> list) {
		this.objectTypeList = list;
	}

	public List<ResourceObjectTypeDefinitionTypeDto> getObjectTypeList() {
        return objectTypeList;
    }

    public ResourceObjectTypeDefinitionTypeDto getSelected() {
        return selected;
    }

	public int getSelectedIndex() {
		return selected != null ? objectTypeList.indexOf(selected) : -1;
	}

	public void setSelectedIndex(int index) {
		if (index >= 0 && index < objectTypeList.size()) {
			setSelected(objectTypeList.get(index));
		}
	}

	public void setSelected(ResourceObjectTypeDefinitionTypeDto selected) {
        this.selected = selected;
		setObjectClassNameFrom(selected);
    }

	private void setObjectClassNameFrom(ResourceObjectTypeDefinitionTypeDto objectType) {
		if (objectType == null) {
			objectClassName = null;
		} else {
			QName oc = objectType.getObjectType().getObjectClass();
			objectClassName = oc != null ? oc.getLocalPart() : null;
		}
	}

	public List<QName> getObjectClassList() {
        return objectClassList;
    }

    public void setObjectClassList(List<QName> objectClassList) {
        this.objectClassList = objectClassList;
    }

    public String getObjectClassName() {
        return objectClassName;
    }

    public void setObjectClassName(String objectClassName) {
        this.objectClassName = objectClassName;
		if (selected != null) {
			selected.getObjectType().setObjectClass(findObjectClassQName(objectClassName));	// update object class in selected objectType container
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
