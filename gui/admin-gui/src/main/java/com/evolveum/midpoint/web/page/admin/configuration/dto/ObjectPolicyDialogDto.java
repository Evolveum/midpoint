/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.io.Serializable;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemConstraintType;
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

    private List<ItemConstraintType> propertyConstraintsList;
    private ObjectPolicyConfigurationType config;
    private QName type;
    private String subtype;
    private ObjectTemplateConfigTypeReferenceDto templateRef;
    PageBase page;

    public ObjectPolicyDialogDto(ObjectPolicyConfigurationType config, PageBase page) {
        this.config = config;
        type = config.getType();
        subtype = config.getSubtype();
        this.page =page;

//        for (PropertyConstraintType constraint : config.getPropertyConstraint()) {
//            propertyConstraintsList.add(new PropertyConstraintTypeDto(constraint));
//        }

        propertyConstraintsList = config.getPropertyConstraint();

        if (propertyConstraintsList.isEmpty()) {
                propertyConstraintsList.add(new ItemConstraintType());
        }

        if(config.getObjectTemplateRef() != null){
            ObjectReferenceType ref = config.getObjectTemplateRef();
            templateRef = new ObjectTemplateConfigTypeReferenceDto(ref.getOid(), getObjectTemplateName(ref.getOid(), page));
        }
    }

    public ObjectPolicyConfigurationType preparePolicyConfig(OperationResult result){
        ObjectPolicyConfigurationType newConfig = new ObjectPolicyConfigurationType();

        for (ItemConstraintType constraintType : propertyConstraintsList) {
                PrismContainerValue<ItemConstraintType> constraint = constraintType.asPrismContainerValue();
                if (BooleanUtils.isTrue(constraintType.isOidBound()) && constraintType.getPath() == null) {
                    result.recordWarning(page.createStringResource("ObjectPolicyDialogDto.message.preparePolicyConfig.warning").getString());
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

    public List<ItemConstraintType> getPropertyConstraintsList() {
        return propertyConstraintsList;
    }

    public void setPropertyConstraintsList(List<ItemConstraintType> propertyConstraintsList) {
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
