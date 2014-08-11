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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class ObjectPolicyDialogDto implements Serializable{

    private static final String DOT_CLASS = ObjectPolicyDialogDto.class.getName() + ".";

    private static final String OPERATION_LOAD_OBJECT_TEMPLATE = "loadObjectTemplate";

    public static final String F_CONFIG = "config";
    public static final String F_TEMPLATE_REF = "templateRef";
    public static final String F_TYPE = "type";
    public static final String F_PROPERTY_LIST = "propertyConstraintsList";

    private List<PropertyConstraintTypeDto> propertyConstraintsList;
    private ObjectPolicyConfigurationTypeDto config;
    private QName type;
    private ObjectTemplateConfigTypeReferenceDto templateRef;

    public ObjectPolicyDialogDto(ObjectPolicyConfigurationTypeDto config, PageBase page){
        this.config = config;
        type = config.getType();

        propertyConstraintsList = new ArrayList<>();

        if(config != null && config.getConstraints() != null){
            propertyConstraintsList.addAll(config.getConstraints());
        } else {
            propertyConstraintsList.add(new PropertyConstraintTypeDto(null));
        }

        if(config.getTemplateRef() != null){
            ObjectReferenceType ref = config.getTemplateRef();
            templateRef = new ObjectTemplateConfigTypeReferenceDto(ref.getOid(), getObjectTemplateName(ref.getOid(), page));
        }
    }

    public ObjectPolicyConfigurationTypeDto preparePolicyConfig(){
        ObjectPolicyConfigurationTypeDto newConfig = new ObjectPolicyConfigurationTypeDto();

        newConfig.setConstraints(propertyConstraintsList);
        newConfig.setType(type);

        ObjectReferenceType ref = new ObjectReferenceType();
        if(templateRef != null){
            ref.setOid(templateRef.getOid());
            ref.setType(ObjectTemplateType.COMPLEX_TYPE);
        }

        newConfig.setTemplateRef(ref);

        return newConfig;
    }

    public List<PropertyConstraintTypeDto> getPropertyConstraintsList() {
        return propertyConstraintsList;
    }

    public void setPropertyConstraintsList(List<PropertyConstraintTypeDto> propertyConstraintsList) {
        this.propertyConstraintsList = propertyConstraintsList;
    }

    public QName getType() {
        return type;
    }

    public void setType(QName type) {
        this.type = type;
    }

    private String getObjectTemplateName(String oid, PageBase page){
        OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT_TEMPLATE);

        PrismObject<ObjectTemplateType> templatePrism =  WebModelUtils.loadObject(ObjectTemplateType.class, oid, result, page);

        if(templatePrism != null){
            return WebMiscUtil.getName(templatePrism);
        }

        return "";
    }

    public ObjectPolicyConfigurationTypeDto getConfig() {
        return config;
    }

    public void setConfig(ObjectPolicyConfigurationTypeDto config) {
        this.config = config;
    }

    public ObjectTemplateConfigTypeReferenceDto getTemplateRef() {
        return templateRef;
    }

    public void setTemplateRef(ObjectTemplateConfigTypeReferenceDto templateRef) {
        this.templateRef = templateRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectPolicyDialogDto)) return false;

        ObjectPolicyDialogDto that = (ObjectPolicyDialogDto) o;

        if (config != null ? !config.equals(that.config) : that.config != null) return false;
        if (propertyConstraintsList != null ? !propertyConstraintsList.equals(that.propertyConstraintsList) : that.propertyConstraintsList != null)
            return false;
        if (templateRef != null ? !templateRef.equals(that.templateRef) : that.templateRef != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = propertyConstraintsList != null ? propertyConstraintsList.hashCode() : 0;
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (templateRef != null ? templateRef.hashCode() : 0);
        return result;
    }
}
