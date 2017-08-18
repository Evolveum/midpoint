/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyConstraintType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  @author shpood
 * */
public class ObjectPolicyConfigurationTypeDto implements Serializable{
	private static final long serialVersionUID = 1L;

	public static final String F_TEMPLATE_REF = "templateRef";
    public static final String F_TYPE = "type";
    public static final String F_SUBTYPE = "subtype";
    public static final String F_CONSTRAINTS = "constraints";

    private ObjectReferenceType templateRef;
    private QName type;
    private String subtype;
    private List<PropertyConstraintTypeDto> constraints;
    private ConflictResolutionType conflictResolution;

    public ObjectPolicyConfigurationTypeDto(){}

    public ObjectPolicyConfigurationTypeDto(ObjectPolicyConfigurationType policyConfig){
        templateRef = policyConfig.getObjectTemplateRef();
        type = policyConfig.getType();
        subtype = policyConfig.getSubtype();

        constraints = new ArrayList<>();
        if(policyConfig.getPropertyConstraint() != null){
            if(policyConfig.getPropertyConstraint().isEmpty()){
                policyConfig.getPropertyConstraint().add(new PropertyConstraintType());
            }

            for(PropertyConstraintType property: policyConfig.getPropertyConstraint()){
                constraints.add(new PropertyConstraintTypeDto(property));
            }
        } else {
            constraints.add(new PropertyConstraintTypeDto(null));
        }
        conflictResolution = policyConfig.getConflictResolution();
    }

    public ObjectReferenceType getTemplateRef() {
        return templateRef;
    }

    public void setTemplateRef(ObjectReferenceType templateRef) {
        this.templateRef = templateRef;
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

	public List<PropertyConstraintTypeDto> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<PropertyConstraintTypeDto> constraints) {
        this.constraints = constraints;
    }

	public ConflictResolutionType getConflictResolution() {
		return conflictResolution;
	}

	public void setConflictResolution(ConflictResolutionType conflictResolution) {
		this.conflictResolution = conflictResolution;
	}

	public boolean isEmpty(){
    	return type == null && subtype == null && constraints == null && templateRef == null && conflictResolution == null;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ObjectPolicyConfigurationTypeDto))
			return false;
		ObjectPolicyConfigurationTypeDto that = (ObjectPolicyConfigurationTypeDto) o;
		return Objects.equals(templateRef, that.templateRef) &&
				Objects.equals(type, that.type) &&
				Objects.equals(subtype, that.subtype) &&
				Objects.equals(constraints, that.constraints) &&
				Objects.equals(conflictResolution, that.conflictResolution);
	}

	@Override
	public int hashCode() {
		return Objects.hash(templateRef, type, subtype, constraints, conflictResolution);
	}

	@Override
	public String toString() {
		return "ObjectPolicyConfigurationTypeDto(templateRef=" + templateRef + ", type=" + type
				+ ", subtype=" + subtype + ", constraints=" + constraints
				+ (conflictResolution != null ? ",conflictResolution" : "")
				+ ")";
	}

    
}
