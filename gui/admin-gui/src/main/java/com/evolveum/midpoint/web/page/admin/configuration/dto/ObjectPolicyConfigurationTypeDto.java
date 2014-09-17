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

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyConstraintType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shpood
 * */
public class ObjectPolicyConfigurationTypeDto implements Serializable{

    public static final String F_TEMPLATE_REF = "templateRef";
    public static final String F_TYPE = "type";
    public static final String F_CONSTRAINTS = "constraints";

    private ObjectReferenceType templateRef;
    private QName type;
    private List<PropertyConstraintTypeDto> constraints;

    public ObjectPolicyConfigurationTypeDto(){}

    public ObjectPolicyConfigurationTypeDto(ObjectPolicyConfigurationType policyConfig){
        templateRef = policyConfig.getObjectTemplateRef();
        type = policyConfig.getType();

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

    public List<PropertyConstraintTypeDto> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<PropertyConstraintTypeDto> constraints) {
        this.constraints = constraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectPolicyConfigurationTypeDto)) return false;

        ObjectPolicyConfigurationTypeDto that = (ObjectPolicyConfigurationTypeDto) o;

        if (constraints != null ? !constraints.equals(that.constraints) : that.constraints != null) return false;
        if (templateRef != null ? !templateRef.equals(that.templateRef) : that.templateRef != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = templateRef != null ? templateRef.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (constraints != null ? constraints.hashCode() : 0);
        return result;
    }
}
