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

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/**
 *  @author shood
 * */
public class SchemaHandlingDto implements Serializable {

    public static final String F_OBJECT_TYPE_DTO_LIST = "objectTypeDtoList";
    public static final String F_SELECTED_OBJECT_TYPE_DTO = "selectedObjectTypeDto";
    public static final String F_SELECTED_ATTRIBUTE = "selectedAttribute";
    public static final String F_SELECTED_ASSOCIATION = "selectedAssociation";
    public static final String F_OBJECT_CLASS_NAME = "objectClassName";

    @NotNull private final List<ResourceObjectTypeDefinitionTypeDto> objectTypeDtoList;
	@NotNull private final List<QName> objectClassList;
    private ResourceObjectTypeDefinitionTypeDto selectedObjectTypeDto;
    private String objectClassName;
	private ResourceAttributeDefinitionType selectedAttribute;
	private ResourceObjectAssociationType selectedAssociation;

	public SchemaHandlingDto(@NotNull List<ResourceObjectTypeDefinitionTypeDto> list, @NotNull List<QName> objectClasses) {
		this.objectTypeDtoList = list;
		this.objectClassList = objectClasses;
	}

	@NotNull
	public List<ResourceObjectTypeDefinitionTypeDto> getObjectTypeDtoList() {
        return objectTypeDtoList;
    }

    public ResourceObjectTypeDefinitionTypeDto getSelectedObjectTypeDto() {
        return selectedObjectTypeDto;
    }

	public void setSelectedObjectTypeDto(ResourceObjectTypeDefinitionTypeDto selectedObjectTypeDto) {
        this.selectedObjectTypeDto = selectedObjectTypeDto;
		setObjectClassNameFrom(selectedObjectTypeDto);
    }

	private void setObjectClassNameFrom(ResourceObjectTypeDefinitionTypeDto objectType) {
		if (objectType == null) {
			objectClassName = null;
		} else {
			QName oc = objectType.getObjectType().getObjectClass();
			objectClassName = oc != null ? oc.getLocalPart() : null;
		}
	}

	@NotNull
	public List<QName> getObjectClassList() {
        return objectClassList;
    }

	@SuppressWarnings("unused")
    public String getObjectClassName() {
        return objectClassName;
    }

	@SuppressWarnings("unused")
    public void setObjectClassName(String objectClassName) {
        this.objectClassName = objectClassName;
		if (selectedObjectTypeDto != null) {
			selectedObjectTypeDto.getObjectType().setObjectClass(findObjectClassQName(objectClassName));	// update object class in selected objectType container
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

	public ResourceAttributeDefinitionType getSelectedAttribute() {
		return selectedAttribute;
	}

	public void setSelectedAttribute(ResourceAttributeDefinitionType selectedAttribute) {
		this.selectedAttribute = selectedAttribute;
	}

	public ResourceObjectAssociationType getSelectedAssociation() {
		return selectedAssociation;
	}

	public void setSelectedAssociation(ResourceObjectAssociationType selectedAssociation) {
		this.selectedAssociation = selectedAssociation;
	}
}
