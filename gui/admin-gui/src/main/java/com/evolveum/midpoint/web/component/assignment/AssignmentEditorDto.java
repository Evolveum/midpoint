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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class AssignmentEditorDto extends SelectableBean implements Comparable<AssignmentEditorDto> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentEditorDto.class);

    private static final String DOT_CLASS = AssignmentEditorDto.class.getName() + ".";
    private static final String OPERATION_LOAD_ORG_TENANT = DOT_CLASS + "loadTenantOrg";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_LOAD_ATTRIBUTES = DOT_CLASS + "loadAttributes";

    public static final String F_TYPE = "type";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_ACTIVATION = "activation";
    public static final String F_RELATION = "relation";
    public static final String F_TENANT_REF = "tenantRef";
    public static final String F_ORG_REF = "orgRef";
    public static final String F_ALT_NAME = "altName";
    public static final String F_IS_ORG_UNIT_MANAGER = "isOrgUnitManager";

    private String name;
    private String altName;
    private AssignmentEditorDtoType type;
    private UserDtoStatus status;
    private AssignmentType oldAssignment;
    private ObjectViewDto<OrgType> tenantRef;
    private ObjectViewDto<OrgType> orgRef;

    private boolean showEmpty = false;
    private boolean minimized = true;

    private Boolean isOrgUnitManager = Boolean.FALSE;
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
        this.orgRef = loadOrgReference(targetObject, assignment, pageBase);

        this.name = getNameForTargetObject(targetObject);
        this.altName = getAlternativeName(assignment);

        this.attributes = prepareAssignmentAttributes(assignment, pageBase);
        this.isOrgUnitManager = determineUserOrgRelation(assignment);
    }

    private Boolean determineUserOrgRelation(AssignmentType assignment){
        if(!AssignmentEditorDtoType.ORG_UNIT.equals(getType())){
            return Boolean.FALSE;
        }

        if(assignment == null || assignment.getTargetRef() == null || assignment.getTargetRef().getRelation() == null){
            return Boolean.FALSE;
        }

        if(SchemaConstants.ORG_MANAGER.equals(assignment.getTargetRef().getRelation())){
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    private List<ACAttributeDto> prepareAssignmentAttributes(AssignmentType assignment, PageBase pageBase){
        List<ACAttributeDto> acAtrList = new ArrayList<>();

        if(assignment == null || assignment.getConstruction() == null || assignment.getConstruction().getAttribute() == null
                || assignment.getConstruction() == null){
            return acAtrList;
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_ATTRIBUTES);
        ConstructionType construction = assignment.getConstruction();

        PrismObject<ResourceType> resource = construction.getResource() != null
                ? construction.getResource().asPrismObject() : null;
        if (resource == null) {
            resource = getReference(construction.getResourceRef(), result, pageBase);
        }

        try {
            PrismContext prismContext = pageBase.getPrismContext();
            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
                    LayerType.PRESENTATION, prismContext);
            RefinedObjectClassDefinition objectClassDefinition = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, construction.getIntent());

            if(objectClassDefinition == null){
                return attributes;
            }

            PrismContainerDefinition definition = objectClassDefinition.toResourceAttributeContainerDefinition();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Refined definition for {}\n{}", construction, definition.debugDump());
            }

            Collection<ItemDefinition> definitions = definition.getDefinitions();
            for(ResourceAttributeDefinitionType attribute: assignment.getConstruction().getAttribute()){

                for(ItemDefinition attrDef: definitions){
                    if (attrDef instanceof PrismPropertyDefinition) {
                        PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) attrDef;

                        if (propertyDef.isOperational() || propertyDef.isIgnored()) {
                            continue;
                        }

                        if(ItemPathUtil.getOnlySegmentQName(attribute.getRef()).equals(propertyDef.getName())){
                            acAtrList.add(ACAttributeDto.createACAttributeDto(propertyDef, attribute, prismContext));
                            break;
                        }
                    }
                }
            }

            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Exception occurred during assignment attribute loading", ex);
            result.recordFatalError("Exception occurred during assignment attribute loading.", ex);
        } finally {
            result.recomputeStatus();
        }

        return acAtrList;
    }

    private PrismObject getReference(ObjectReferenceType ref, OperationResult result, PageBase pageBase) {
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_RESOURCE);
        subResult.addParam("targetRef", ref.getOid());
        PrismObject target = null;
        try {
            Task task = pageBase.createSimpleTask(OPERATION_LOAD_RESOURCE);
            Class type = ObjectType.class;
            if (ref.getType() != null){
                type = pageBase.getPrismContext().getSchemaRegistry().determineCompileTimeClass(ref.getType());
            }
            target = pageBase.getModelService().getObject(type, ref.getOid(), null, task,
                    subResult);
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get account construction resource ref", ex);
            subResult.recordFatalError("Couldn't get account construction resource ref.", ex);
        }

        return target;
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
                    dto = new ObjectViewDto(ObjectViewDto.BAD_OID);
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
    
    private ObjectViewDto loadOrgReference(ObjectType object, AssignmentType assignment, PageBase page){
        ObjectViewDto dto;

        if(object instanceof RoleType){
            if(assignment.getOrgRef() != null){
                ObjectReferenceType ref = assignment.getOrgRef();

                OperationResult result = new OperationResult(OPERATION_LOAD_ORG_TENANT);
                PrismObject<OrgType> org = WebModelUtils.loadObject(OrgType.class, ref.getOid(), result, page);

                //TODO - show user some error about not loading role tenants of OrgType
                if(org == null){
                    dto = new ObjectViewDto(ObjectViewDto.BAD_OID);
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
            if(tenantRef != null){
                if(ObjectViewDto.BAD_OID.equals(tenantRef.getOid())){
                    builder.append(" - ").append("(tenant not found)");
                } else if(tenantRef.getOid() != null){
                    builder.append(" - ").append(tenantRef.getName());
                }
            }
            if(orgRef != null){
                if(ObjectViewDto.BAD_OID.equals(orgRef.getOid())){
                    builder.append(" - ").append("(org not found)");
                } else if(orgRef.getOid() != null){
                    builder.append(" - ").append(orgRef.getName());
                }
            }

        }

        return builder.toString();
    }

    private String getAlternativeName(AssignmentType assignment){
        if(assignment == null){
            return null;
        }

        if(assignment.getFocusMappings() != null){
            return "(focus mapping)";
        }

        return null;
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
        if(AssignmentEditorDtoType.ORG_UNIT.equals(getType())){
            if(isOrgUnitManager()){
                newAssignment.getTargetRef().setRelation(SchemaConstants.ORG_MANAGER);
            } else {
                newAssignment.getTargetRef().setRelation(null);
            }
        }

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
                ref.setType(OrgType.COMPLEX_TYPE);
                newAssignment.setTenantRef(ref);
            }
        }
        
        if(orgRef != null && AssignmentEditorDtoType.ROLE.equals(this.type)){
            if(orgRef.getOid() == null){
                newAssignment.setOrgRef(null);;
            } else {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setOid(this.orgRef.getOid());
                ref.setType(OrgType.COMPLEX_TYPE);
                newAssignment.setOrgRef(ref);
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

    public Boolean isOrgUnitManager() {
        return isOrgUnitManager;
    }

    public void setOrgUnitManager(Boolean orgUnitManager) {
        isOrgUnitManager = orgUnitManager;
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
    
    public ObjectViewDto<OrgType> getOrgRef() {
		return orgRef;
	}
    
    public void setOrgRef(ObjectViewDto<OrgType> orgRef) {
		this.orgRef = orgRef;
	}

    public String getAltName() {
        return altName;
    }

    public void setAltName(String altName) {
        this.altName = altName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentEditorDto)) return false;

        AssignmentEditorDto that = (AssignmentEditorDto) o;

        if (isOrgUnitManager != that.isOrgUnitManager) return false;
        if (minimized != that.minimized) return false;
        if (showEmpty != that.showEmpty) return false;
        if (altName != null ? !altName.equals(that.altName) : that.altName != null) return false;
        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (newAssignment != null ? !newAssignment.equals(that.newAssignment) : that.newAssignment != null)
            return false;
        if (oldAssignment != null ? !oldAssignment.equals(that.oldAssignment) : that.oldAssignment != null)
            return false;
        if (status != that.status) return false;
        if (tenantRef != null ? !tenantRef.equals(that.tenantRef) : that.tenantRef != null) return false;
        if (orgRef != null ? !orgRef.equals(that.orgRef) : that.orgRef != null) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (altName != null ? altName.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (oldAssignment != null ? oldAssignment.hashCode() : 0);
        result = 31 * result + (tenantRef != null ? tenantRef.hashCode() : 0);
        result = 31 * result + (orgRef != null ? orgRef.hashCode() : 0);
        result = 31 * result + (showEmpty ? 1 : 0);
        result = 31 * result + (minimized ? 1 : 0);
        result = 31 * result + (isOrgUnitManager ? 1 : 0);
        result = 31 * result + (newAssignment != null ? newAssignment.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }
}
