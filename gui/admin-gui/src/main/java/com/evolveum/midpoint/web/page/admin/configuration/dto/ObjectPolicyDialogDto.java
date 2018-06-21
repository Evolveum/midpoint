/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyConstraintType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 *  @author shood
 * */
public class ObjectPolicyDialogDto implements Serializable{
	private static final long serialVersionUID = 1L;

	private static final String DOT_CLASS = ObjectPolicyDialogDto.class.getName() + ".";

    private static final String OPERATION_LOAD_OBJECT_TEMPLATE = DOT_CLASS + "loadObjectTemplate";

    public static final String F_CONFIG = "config";
    public static final String F_TEMPLATE_REF = "templateRef";
    public static final String F_TYPE = "type";
    public static final String F_SUBTYPE = "subtype";
    public static final String F_PROPERTY_LIST = "propertyConstraintsList";

    private List<PropertyConstraintType> propertyConstraintsList;
    private ObjectPolicyConfigurationType config;
    private QName type;
    private String subtype;
    private ObjectTemplateConfigTypeReferenceDto templateRef;

    public ObjectPolicyDialogDto(ObjectPolicyConfigurationType config, PageBase page) {
        this.config = config;
        type = config.getType();
        subtype = config.getSubtype();

//        for (PropertyConstraintType constraint : config.getPropertyConstraint()) {
//        	propertyConstraintsList.add(new PropertyConstraintTypeDto(constraint));
//        }
        
        propertyConstraintsList = config.getPropertyConstraint();
        
        if (propertyConstraintsList.isEmpty()) {
        		propertyConstraintsList.add(new PropertyConstraintType());
        }

        if(config.getObjectTemplateRef() != null){
            ObjectReferenceType ref = config.getObjectTemplateRef();
            templateRef = new ObjectTemplateConfigTypeReferenceDto(ref.getOid(), getObjectTemplateName(ref.getOid(), page));
        }
    }

    public ObjectPolicyConfigurationType preparePolicyConfig(OperationResult result){
        ObjectPolicyConfigurationType newConfig = new ObjectPolicyConfigurationType();

        for (PropertyConstraintType constraintType : propertyConstraintsList) {
        		PrismContainerValue<PropertyConstraintType> constraint = constraintType.asPrismContainerValue();
        		if (BooleanUtils.isTrue(constraintType.isOidBound()) && constraintType.getPath() == null) {
        			result.recordWarning("Skipping setting property constraint, no path was defined.");
        		}
        		if (!constraint.isEmpty() && constraintType.getPath() != null) {
        			newConfig.getPropertyConstraint().add(constraint.clone().asContainerable());
        		}
        }
        newConfig.setType(type);
        newConfig.setSubtype(subtype);
        newConfig.setConflictResolution(config.getConflictResolution());

        if (templateRef != null) {
	        ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(templateRef.getOid());
            ref.setType(ObjectTemplateType.COMPLEX_TYPE);
            ref.setTargetName(new PolyStringType(templateRef.getName()));
	        newConfig.setObjectTemplateRef(ref);
        }
        
        result.recordSuccessIfUnknown();
        return newConfig;
    }

    public List<PropertyConstraintType> getPropertyConstraintsList() {
        return propertyConstraintsList;
    }

    public void setPropertyConstraintsList(List<PropertyConstraintType> propertyConstraintsList) {
        this.propertyConstraintsList = propertyConstraintsList;
    }

    public QName getType() {
        return type;
    }

    public void setType(QName type) {
        this.type = type;
    }

    public String getSubtype() {
		return subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	private String getObjectTemplateName(String oid, PageBase page){
    	Task task = page.createSimpleTask(OPERATION_LOAD_OBJECT_TEMPLATE);
        OperationResult result = task.getResult();

        PrismObject<ObjectTemplateType> templatePrism =  WebModelServiceUtils.loadObject(ObjectTemplateType.class, oid,
        		page, task, result);

        if(templatePrism != null){
            return WebComponentUtil.getName(templatePrism);
        }

        return "";
    }

    public ObjectPolicyConfigurationType getConfig() {
        return config;
    }

    public void setConfig(ObjectPolicyConfigurationType config) {
        this.config = config;
    }

    public ObjectTemplateConfigTypeReferenceDto getTemplateRef() {
        return templateRef;
    }

    public void setTemplateRef(ObjectTemplateConfigTypeReferenceDto templateRef) {
        this.templateRef = templateRef;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((config == null) ? 0 : config.hashCode());
		result = prime * result
				+ ((propertyConstraintsList == null) ? 0 : propertyConstraintsList.hashCode());
		result = prime * result + ((subtype == null) ? 0 : subtype.hashCode());
		result = prime * result + ((templateRef == null) ? 0 : templateRef.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ObjectPolicyDialogDto other = (ObjectPolicyDialogDto) obj;
		if (config == null) {
			if (other.config != null) {
				return false;
			}
		} else if (!config.equals(other.config)) {
			return false;
		}
		if (propertyConstraintsList == null) {
			if (other.propertyConstraintsList != null) {
				return false;
			}
		} else if (!propertyConstraintsList.equals(other.propertyConstraintsList)) {
			return false;
		}
		if (subtype == null) {
			if (other.subtype != null) {
				return false;
			}
		} else if (!subtype.equals(other.subtype)) {
			return false;
		}
		if (templateRef == null) {
			if (other.templateRef != null) {
				return false;
			}
		} else if (!templateRef.equals(other.templateRef)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ObjectPolicyDialogDto(propertyConstraintsList=" + propertyConstraintsList + ", config="
				+ config + ", type=" + type + ", subtype=" + subtype + ", templateRef=" + templateRef + ")";
	}
	
	
}
