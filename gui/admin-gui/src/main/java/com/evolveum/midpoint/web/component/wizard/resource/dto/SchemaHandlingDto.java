/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
