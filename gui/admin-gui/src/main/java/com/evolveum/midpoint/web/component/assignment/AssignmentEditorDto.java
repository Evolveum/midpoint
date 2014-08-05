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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AssignmentEditorDto extends SelectableBean implements Comparable<AssignmentEditorDto> {

    private static final String DOT_CLASS = AssignmentEditorDto.class.getName() + ".";
    private static final String OPERATION_LOAD_ORG_TENANT = DOT_CLASS + "loadTenantOrg";

    public static final String F_TYPE = "type";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_ACTIVATION = "activation";
    public static final String F_RELATION = "relation";
    public static final String F_TENANT_REF = "tenantRef";

    private String name;
    private AssignmentEditorDtoType type;
    private UserDtoStatus status;
    private AssignmentType oldAssignment;
    private ObjectViewDto<OrgType> tenantRef;

    private boolean showEmpty = false;
    private boolean minimized = true;

    private AssignmentType newAssignment;
    private List<ACAttributeDto> attributes;

    public AssignmentEditorDto(ObjectType targetObject, AssignmentEditorDtoType type, UserDtoStatus status, AssignmentType assignment, PageBase pageBase) {
        Validate.notNull(status, "User dto status must not be null.");
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(assignment, "Assignment must not be null.");

        this.type = type;
        this.status = status;
        this.oldAssignment = assignment;

        PrismContainerValue value = oldAssignment.asPrismContainerValue();

        //todo improve assignment clone, this doesn't look good
        PrismContainerValue newValue = value.clone();
        newAssignment = new AssignmentType();
        newAssignment.setupContainerValue(newValue);
        //TODO: is this really needed??construction is cloned earlier by value.clone()
//        if (AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(type)
//                && oldAssignment.getConstruction() != null) {
//            ConstructionType construction = oldAssignment.getConstruction();
//            newAssignment.setConstruction(construction.clone());
//        }

        this.tenantRef = loadTenantReference(targetObject, assignment, pageBase);

        this.name = getNameForTargetObject(targetObject);
    }

    private ObjectViewDto loadTenantReference(ObjectType object, AssignmentType assignment, PageBase page){
        ObjectViewDto dto;

        if(object instanceof RoleType){
            if(assignment.getTenantRef() != null){
                ObjectReferenceType ref = assignment.getTenantRef();

                OperationResult result = new OperationResult(OPERATION_LOAD_ORG_TENANT);
                PrismObject<OrgType> org = WebModelUtils.loadObject(OrgType.class, ref.getOid(), result, page);

                //TODO - show user some error about not loading role tenants of OrgType
                if(org == null){
                    dto = new ObjectViewDto();
                    dto.setType(OrgType.class);
                    return dto;
                }

                dto = new ObjectViewDto(ref.getOid(), WebMiscUtil.getName(org.asObjectable()));
                dto.setType(OrgType.class);
                return dto;
            }
        }

        dto = new ObjectViewDto();
        dto.setType(OrgType.class);
        return dto;
    }

    private String getNameForTargetObject(ObjectType object) {
        if (object == null) {
            return null;
        }

        String name = WebMiscUtil.getName(object);

        PolyStringType display = object instanceof OrgType ? ((OrgType)object).getDisplayName() : null;
        String displayName = WebMiscUtil.getOrigStringFromPoly(display);

        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(name)) {
            builder.append(name);
        }

        if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(displayName)) {
            builder.append(", ");
        }

        if (StringUtils.isNotEmpty(displayName)) {
            builder.append(displayName);
        }

        if (StringUtils.isNotEmpty(getRelation())) {
            builder.append(" (").append(getRelation()).append(')');
        }

        if(object instanceof RoleType){
            if(tenantRef != null && tenantRef.getOid() != null){
                builder.append(" - ").append(tenantRef.getName());
            }
        }

        return builder.toString();
    }

    public List<ACAttributeDto> getAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }
        return attributes;
    }

    public void setAttributes(List<ACAttributeDto> attributes) {
        this.attributes = attributes;
    }

    public boolean isMinimized() {
        return minimized;
    }

    public void setMinimized(boolean minimized) {
        this.minimized = minimized;
    }

    public boolean isShowEmpty() {
        return showEmpty;
    }

    public void setShowEmpty(boolean showEmpty) {
        this.showEmpty = showEmpty;
    }

    public UserDtoStatus getStatus() {
        return status;
    }

    public ActivationType getActivation() {
        ActivationType type = newAssignment.getActivation();
        if (type == null) {
            type = new ActivationType();
//            type.setAdministrativeStatus(ActivationStatusType.ENABLED);
            newAssignment.setActivation(type);
        }

        return type;
    }

    public String getName() {
        return name;
    }

    public ObjectReferenceType getTargetRef() {
        return newAssignment.getTargetRef();
    }

    public AssignmentEditorDtoType getType() {
        return type;
    }

    public void setStatus(UserDtoStatus status) {
        this.status = status;
    }

    public boolean isModified() throws SchemaException {
        return !getOldValue().equivalent(getNewValue());
    }

    public PrismContainerValue getOldValue() {
        return oldAssignment.asPrismContainerValue();
    }

    public PrismContainerValue getNewValue() throws SchemaException {
        //this removes activation element if it's empty
        ActivationType activation = newAssignment.getActivation();
        if (activation == null || activation.asPrismContainerValue().isEmpty()) {
            newAssignment.setActivation(null);
        }

        if(tenantRef != null && AssignmentEditorDtoType.ROLE.equals(this.type)){
            if(tenantRef.getOid() == null){
                newAssignment.setTenantRef(null);
            } else {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setOid(this.tenantRef.getOid());
                newAssignment.setTenantRef(ref);
            }
        }

        ConstructionType construction = newAssignment.getConstruction();
        if (construction == null) {
            return newAssignment.asPrismContainerValue();
        }

        construction.getAttribute().clear();

        for (ACAttributeDto attribute : getAttributes()) {
            if (attribute.isEmpty()) {
                continue;
            }

            construction.getAttribute().add(attribute.getConstruction());
        }

        return newAssignment.asPrismContainerValue();
    }

    public String getDescription() {
        return newAssignment.getDescription();
    }

    public String getRelation() {
        ObjectReferenceType ref = newAssignment.getTargetRef();
        if (ref == null || ref.getRelation() == null) {
            return null;
        }

        return ref.getRelation().getLocalPart();
    }

    public void setDescription(String description) {
        newAssignment.setDescription(description);
    }

    @Override
    public int compareTo(AssignmentEditorDto other) {
        Validate.notNull(other, "Can't compare assignment editor dto with null.");

        int value = getIndexOfType(getType()) - getIndexOfType(other.getType());
        if (value != 0) {
            return value;
        }

        String name1 = getName() != null ? getName() : "";
        String name2 = other.getName() != null ? other.getName() : "";

        return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
    }

    private int getIndexOfType(AssignmentEditorDtoType type) {
        if (type == null) {
            return 0;
        }

        AssignmentEditorDtoType[] values = AssignmentEditorDtoType.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(type)) {
                return i;
            }
        }

        return 0;
    }

    public ObjectViewDto<OrgType> getTenantRef() {
        return tenantRef;
    }

    public void setTenantRef(ObjectViewDto<OrgType> tenantRef) {
        this.tenantRef = tenantRef;
    }
}
