/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.io.Serializable;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * TODO: unify with AssignmentItemDto
 *
 * @author lazyman
 */
public class AssignmentEditorDto extends SelectableBeanImpl implements Comparable<AssignmentEditorDto>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentEditorDto.class);

    private static final String DOT_CLASS = AssignmentEditorDto.class.getName() + ".";
    private static final String OPERATION_LOAD_ORG_TENANT = DOT_CLASS + "loadTenantOrg";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_LOAD_REFERENCE_OBJECT = DOT_CLASS + "loadReferenceObject";
    private static final String OPERATION_LOAD_ATTRIBUTES = DOT_CLASS + "loadAttributes";
    private static final String OPERATION_LOAD_RELATION_DEFINITIONS = DOT_CLASS + "loadRelationDefinitions";

    public static final String F_TYPE = "type";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_ACTIVATION = "activation";
    public static final String F_RELATION = "relation";
    public static final String F_FOCUS_TYPE = "focusType";
    public static final String F_TENANT_REF = "tenantRef";
    public static final String F_ORG_REF = "orgRef";
    public static final String F_NEW_ASSIGNMENT = "newAssignment";
    public static final String F_ALT_NAME = "altName";
    public static final String F_IS_ORG_UNIT_MANAGER = "isOrgUnitManager";

    private final String name;
    private final AssignmentEditorDtoType type;
    private final AssignmentType oldAssignment;
    private String altName;
    private UserDtoStatus status;
    private List<AssignmentInfoDto> privilegeLimitationList;
    private ObjectViewDto<OrgType> tenantRef;
    private ObjectViewDto<OrgType> orgRef;

    private boolean showEmpty = false;
    private boolean minimized = true;
    private boolean editable = true;
    private boolean simpleView = false;

    private boolean isAlreadyAssigned = false;        //used only for role request functionality
    private AssignmentConstraintsType defaultAssignmentConstraints;                //used only for role request functionality
    private List<QName> assignedRelationsList = new ArrayList<>(); //used only for role request functionalityp

    private Boolean isOrgUnitManager;
    private final AssignmentType newAssignment;
    private List<ACAttributeDto> attributes;
    private final PageBase pageBase;
    private UserType delegationOwner;

    public AssignmentEditorDto(UserDtoStatus status, AssignmentType assignment, PageBase pageBase) {
        this(status, assignment, pageBase, null);
    }

    public AssignmentEditorDto(UserDtoStatus status, AssignmentType assignment, PageBase pageBase, UserType delegationOwner) {
        Validate.notNull(status, "User dto status must not be null.");
        Validate.notNull(assignment, "Assignment must not be null.");

        this.type = getType(assignment);
        Validate.notNull(type, "Type must not be null.");
        this.status = status;
        this.oldAssignment = assignment;
        this.pageBase = pageBase;
        this.delegationOwner = delegationOwner;

        PrismContainerValue value = oldAssignment.asPrismContainerValue();

        // todo improve assignment clone, this doesn't look good
        PrismContainerValue newValue = value.clone();
        newAssignment = new AssignmentType();
        newAssignment.setupContainerValue(newValue);
        // TODO: is this really needed??construction is cloned earlier by
        // value.clone()
        // if (AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(type)
        // && oldAssignment.getConstruction() != null) {
        // ConstructionType construction = oldAssignment.getConstruction();
        // newAssignment.setConstruction(construction.clone());
        // }
        Collection<Item> items = newValue.getItems();
        if (!items.isEmpty()) {
            Item item = items.iterator().next();
            if (item != null && item.getDefinition() != null) {
                this.editable = item.getDefinition().canAdd() || item.getDefinition().canModify();
            }
        }

        this.tenantRef = loadTenantOrgReference(assignment, assignment.getTenantRef());
        this.orgRef = loadTenantOrgReference(assignment, assignment.getOrgRef());

        this.name = getNameForTargetObject(assignment);
        this.altName = getAlternativeName(assignment);

        this.attributes = prepareAssignmentAttributes(assignment, pageBase);
        this.isOrgUnitManager = isOrgUnitManager(assignment);
        this.privilegeLimitationList = getAssignmentPrivilegesList(assignment);
    }

    public static AssignmentEditorDto createDtoAddFromSelectedObject(ObjectType object, PageBase pageBase) {
        return createDtoAddFromSelectedObject(object, null, pageBase);
    }

    public static AssignmentEditorDto createDtoAddFromSelectedObject(ObjectType object,
            QName relation, PageBase pageBase) {
        return createDtoAddFromSelectedObject(object, relation, pageBase, null);
    }

    public static AssignmentEditorDto createDtoAddFromSelectedObject(ObjectType object, QName relation,
            PageBase pageBase, UserType delegationOwner) {
        AssignmentEditorDto dto = createDtoFromObject(object, UserDtoStatus.ADD, relation, pageBase);
        dto.setDelegationOwner(delegationOwner);
        if (pageBase.getRelationRegistry().isDelegation(relation)) {
            OtherPrivilegesLimitationType limitations = new OtherPrivilegesLimitationType()
                    // "approval work items" item is deprecated, "case management" is the replacement
                    .caseManagementWorkItems(
                            new WorkItemSelectorType()
                                    .all(true))
                    .certificationWorkItems(
                            new WorkItemSelectorType()
                                    .all(true));

            dto.setPrivilegesLimitation(limitations);

            dto.setMinimized(false);
        } else {
            dto.setMinimized(true);
        }
        dto.setShowEmpty(true);

        return dto;
    }

    public static AssignmentEditorDto createDtoFromObject(ObjectType object, UserDtoStatus status,
            PageBase pageBase) {

        return createDtoFromObject(object, status, null, pageBase);
    }

    public static AssignmentEditorDto createDtoFromObject(ObjectType object, UserDtoStatus status,
            QName relation, PageBase pageBase) {
        AssignmentEditorDtoType aType = AssignmentEditorDtoType.getType(object.getClass());

        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(object.getOid());
        targetRef.setType(aType.getQname());
        targetRef.setDescription(object.getDescription());
        targetRef.setTargetName(object.getName());
        if (relation != null) {
            targetRef.setRelation(relation);
        }
        targetRef.asReferenceValue().setObject(object.asPrismObject());

        AssignmentType assignment = new AssignmentType();
        assignment.setTargetRef(targetRef);

        return new AssignmentEditorDto(status, assignment, pageBase);
    }

    private AssignmentEditorDtoType getType(AssignmentType assignment) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        PrismObject<? extends ObjectType> object = targetRef.asReferenceValue().getObject();
        if (object != null) {
            // object assignment
            return AssignmentEditorDtoType.getType(object.getCompileTimeClass());
        } else if (assignment.getTargetRef() != null) {
            return AssignmentEditorDtoType.getType(assignment.getTargetRef().getType());
        }
        if (assignment.asPrismContainerValue() != null
                && getPolicyRuleContainer(assignment) != null) {
            return AssignmentEditorDtoType.POLICY_RULE;
        }
        // account assignment through account construction
        return AssignmentEditorDtoType.CONSTRUCTION;

    }

    private List<AssignmentInfoDto> getAssignmentPrivilegesList(AssignmentType assignment) {
        List<AssignmentInfoDto> list = new ArrayList<>();
        AssignmentSelectorType assignmentSelectorType = assignment.getLimitTargetContent();
        if (assignmentSelectorType != null && assignmentSelectorType.getTargetRef() != null) {
            for (ObjectReferenceType objectRef : assignmentSelectorType.getTargetRef()) {
                if (objectRef == null || objectRef.getType() == null) {
                    continue;
                }
                AssignmentInfoDto dto = new AssignmentInfoDto();
                Class<? extends ObjectType> targetClass = ObjectTypes.getObjectTypeFromTypeQName(objectRef.getType()).getClassDefinition();
                dto.setTargetClass(targetClass);
                dto.setTargetName(WebModelServiceUtils.resolveReferenceName(objectRef, pageBase,
                        pageBase.createSimpleTask(OPERATION_LOAD_REFERENCE_OBJECT),
                        new OperationResult(OPERATION_LOAD_REFERENCE_OBJECT)));
                dto.setTargetOid(objectRef.getOid());
                list.add(dto);
            }
        }
        return list;
    }

    private boolean isOrgUnitManager(AssignmentType assignment) {
        RelationRegistry relationRegistry = pageBase.getRelationRegistry();
        return assignment != null
                && assignment.getTargetRef() != null
                && relationRegistry.isManager(assignment.getTargetRef().getRelation());
    }

    private List<ACAttributeDto> prepareAssignmentAttributes(AssignmentType assignment, PageBase pageBase) {
        List<ACAttributeDto> acAtrList = new ArrayList<>();

        if (assignment == null || assignment.getConstruction() == null
                || assignment.getConstruction().getAttribute() == null
                || assignment.getConstruction() == null) {
            return acAtrList;
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_ATTRIBUTES);
        ConstructionType construction = assignment.getConstruction();

        ObjectReferenceType resourceRef = construction.getResourceRef();
        PrismObject<ResourceType> resource = resourceRef.asReferenceValue().getObject();
        if (resource == null) {
            resource = getReference(construction.getResourceRef(), result, pageBase);
        }

        try {
            PrismContext prismContext = pageBase.getPrismContext();
            ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource, LayerType.PRESENTATION);
            ResourceObjectDefinition objectClassDefinition = refinedSchema.findDefinitionForConstruction(construction);

            if (objectClassDefinition == null) {
                return attributes;
            }

            PrismContainerDefinition<?> definition = objectClassDefinition
                    .toResourceAttributeContainerDefinition();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Refined definition for {}\n{}", construction, definition.debugDump());
            }

            Collection<? extends ItemDefinition<?>> definitions = definition.getDefinitions();
            for (ResourceAttributeDefinitionType attribute : assignment.getConstruction().getAttribute()) {

                for (ItemDefinition attrDef : definitions) {
                    if (attrDef instanceof PrismPropertyDefinition) {
                        PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) attrDef;

                        if (propertyDef.isOperational() || propertyDef.isIgnored()) {
                            continue;
                        }

                        if (ItemPathTypeUtil.asSingleNameOrFail(attribute.getRef())
                                .equals(propertyDef.getItemName())) {
                            acAtrList.add(ACAttributeDto.createACAttributeDto(propertyDef, attribute,
                                    prismContext));
                            break;
                        }
                    }
                }
            }

            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Exception occurred during assignment attribute loading", ex);
            result.recordFatalError(pageBase.createStringResource("AssignmentEditorDto.message.prepareAssignmentAttributes.fatalError").getString(), ex);
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
            if (ref.getType() != null) {
                type = pageBase.getPrismContext().getSchemaRegistry()
                        .determineCompileTimeClass(ref.getType());
            }
            target = pageBase.getModelService().getObject(type, ref.getOid(), null, task, subResult);
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get account construction resource ref", ex);
            subResult.recordFatalError(pageBase.createStringResource("AssignmentEditorDto.message.getReference.fatalError").getString(), ex);
        }

        return target;
    }

    private boolean isRole(AssignmentType assignment) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef == null) {
            return false;
        }
        if (targetRef.asReferenceValue().getObject() != null) {
            // object assignment
            return targetRef.asReferenceValue().getObject().canRepresent(RoleType.class);
        } else {
            // object assignment through reference
            if (assignment.getTargetRef().getType() != null) {
                return RoleType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType());
            }
            return false;
        }
    }

    private ObjectViewDto loadTenantOrgReference(AssignmentType assignment, ObjectReferenceType ref) {
        ObjectViewDto dto = null;

        if (isRole(assignment)) {
            if (ref != null) {
                Task task = pageBase.createSimpleTask("Load tenant for assignment");
                OperationResult result = task.getResult();
                PrismObject<OrgType> tenant = WebModelServiceUtils.loadObject(OrgType.class, ref.getOid(),
                        pageBase, task, result);
                if (tenant != null) {

                    dto = new ObjectViewDto(ref.getOid(),
                            WebComponentUtil.getEffectiveName(tenant, OrgType.F_DISPLAY_NAME));
                    dto.setType(OrgType.class);
                } else if (ref.getTargetName() == null) {
                    dto = new ObjectViewDto(ObjectViewDto.BAD_OID);
                    dto.setType(OrgType.class);
                }

                return dto;
            }
        }

        dto = new ObjectViewDto();
        dto.setType(OrgType.class);
        return dto;
    }

    private String getNameForTargetObject(AssignmentType assignment) {
        if (assignment == null) {
            return null;
        }

        if (AssignmentEditorDtoType.POLICY_RULE.equals(type)) {
            PrismContainer<PolicyRuleType> policyRuleContainer = getPolicyRuleContainer(assignment);
            PrismProperty<?> policyRuleNameProperty = policyRuleContainer != null && policyRuleContainer.getValue() != null ?
                    (PrismProperty) policyRuleContainer.getValue().find(PolicyRuleType.F_NAME) : null;
            String policyRuleName = policyRuleNameProperty != null ?
                    policyRuleNameProperty.getValue().getValue().toString() : "";
            return pageBase.createStringResource("AssignmentEditorDto.policyRuleTitle").getString() +
                    (StringUtils.isEmpty(policyRuleName) ? "" : " - " + policyRuleName);

        }
        StringBuilder sb = new StringBuilder();

        if (assignment.getConstruction() != null) {
            // account assignment through account construction
            ConstructionType construction = assignment.getConstruction();
            if (construction.getResourceRef() != null) {
                sb.append(WebComponentUtil.getName(construction.getResourceRef()));
            }
            return sb.toString();
        }

        if (assignment.getTargetRef() != null) {
            PrismObject<FocusType> target = assignment.getTargetRef().asReferenceValue().getObject();
            if (target == null) {
                Task task = pageBase.createSimpleTask("Load assignment name");
                target = WebModelServiceUtils.loadObject(FocusType.class,
                        assignment.getTargetRef().getOid(), pageBase, task, task.getResult());
            }
            if (target != null) {
                sb.append(WebComponentUtil.getEffectiveName(target, OrgType.F_DISPLAY_NAME));
            } else {
                sb.append(WebComponentUtil.getName(assignment.getTargetRef()));
            }
            appendTenantAndOrgName(sb);
        }

        if (assignment.getTargetRef() != null && assignment.getTargetRef().getRelation() != null) {
            String relationName = pageBase
                    .getString(RelationUtil.getRelationHeaderLabelKey(assignment.getTargetRef().getRelation()));
            sb.append(" - ").append(relationName);
        }

        return sb.toString();
    }

    private void appendTenantAndOrgName(StringBuilder sb) {
        if (tenantRef != null) {
            if (ObjectViewDto.BAD_OID.equals(tenantRef.getOid())) {
                sb.append(" - ").append("(tenant not found)");
            } else if (tenantRef.getOid() != null) {
                sb.append(" - ").append(tenantRef.getName());
            }
        }
        if (orgRef != null) {
            if (ObjectViewDto.BAD_OID.equals(orgRef.getOid())) {
                sb.append(" - ").append("(org not found)");
            } else if (orgRef.getOid() != null) {
                sb.append(" - ").append(orgRef.getName());
            }
        }

    }

    private String getAlternativeName(AssignmentType assignment) {
        if (assignment == null) {
            return null;
        }

        if (assignment.getFocusMappings() != null) {
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

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public UserDtoStatus getStatus() {
        return status;
    }

    public ActivationType getActivation() {
        ActivationType type = newAssignment.getActivation();
        if (type == null) {
            type = new ActivationType();
            // type.setAdministrativeStatus(ActivationStatusType.ENABLED);
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

    public ExtensionType getExtension() {
        return newAssignment.getExtension();
    }

    public AssignmentEditorDtoType getType() {
        return type;
    }

    public void setStatus(UserDtoStatus status) {
        this.status = status;
    }

    public boolean isModified(PrismContext prismContext) throws SchemaException {
        return !getOldValue().equivalent(getNewValue(prismContext));
    }

    public PrismContainerValue<AssignmentType> getOldValue() {
        return oldAssignment.asPrismContainerValue();
    }

    public PrismContainerValue<AssignmentType> getNewValue(PrismContext prismContext) throws SchemaException {
        prismContext.adopt(newAssignment);
        // this removes activation element if it's empty
        ActivationType activation = newAssignment.getActivation();
        if (activation == null || activation.asPrismContainerValue().isEmpty()) {
            newAssignment.setActivation(null);
        }
        //hack for delegation panel. this code should be rewritten
        if (newAssignment.getMetadata() != null && newAssignment.getMetadata().asPrismContainerValue().isEmpty()) {
            newAssignment.setMetadata(null);
        }

        if (tenantRef != null && AssignmentEditorDtoType.ROLE.equals(this.type)) {
            if (tenantRef.getOid() == null) {
                newAssignment.setTenantRef(null);
            } else {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setOid(this.tenantRef.getOid());
                ref.setType(OrgType.COMPLEX_TYPE);
                newAssignment.setTenantRef(ref);
            }
        }

        if (orgRef != null && AssignmentEditorDtoType.ROLE.equals(this.type)) {
            if (orgRef.getOid() == null) {
                newAssignment.setOrgRef(null);
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

            construction.getAttribute().add(attribute.getConstruction(prismContext));
        }

        return newAssignment.asPrismContainerValue();
    }

    public String getDescription() {
        return newAssignment.getDescription();
    }

    public QName getFocusType() {
        return newAssignment.getFocusType();
    }

    public String getRelation() {
        return getRelationQName() != null ? getRelationQName().getLocalPart() : null;
    }

    public QName getRelationQName() {
        ObjectReferenceType ref = newAssignment.getTargetRef();
        if (ref == null || ref.getRelation() == null) {
            return null;        // TODO default vs. null ?
        }
        return ref.getRelation();
    }

    public void setDescription(String description) {
        newAssignment.setDescription(description);
    }

    public void setFocusType(QName focusType) {
        newAssignment.setFocusType(focusType);
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

    public boolean isSimpleView() {
        return simpleView;
    }

    public void setSimpleView(boolean simpleView) {
        this.simpleView = simpleView;
    }

    public boolean isAlreadyAssigned() {
        return isAlreadyAssigned;
    }

    public void setAlreadyAssigned(boolean alreadyAssigned) {
        isAlreadyAssigned = alreadyAssigned;
    }

    public AssignmentConstraintsType getDefaultAssignmentConstraints() {
        return defaultAssignmentConstraints;
    }

    public PrismContainer<PolicyRuleType> getPolicyRuleContainer(AssignmentType assignment) {
        if (assignment == null) {
            assignment = this.newAssignment;
        }
        if (assignment == null) {
            return null;
        }
        PrismContainer policyRuleContainer = assignment.asPrismContainerValue().findContainer(AssignmentType.F_POLICY_RULE);
        return policyRuleContainer != null ? (PrismContainer<PolicyRuleType>) policyRuleContainer : null;
    }

    public void setDefaultAssignmentConstraints(AssignmentConstraintsType defaultAssignmentConstraints) {
        this.defaultAssignmentConstraints = defaultAssignmentConstraints;
    }

    public List<QName> getAssignedRelationsList() {
        return assignedRelationsList;
    }

    public void setAssignedRelationsList(List<QName> assignedRelationsList) {
        this.assignedRelationsList = assignedRelationsList;
    }

    public List<AssignmentInfoDto> getPrivilegeLimitationList() {
        return privilegeLimitationList;
    }

    public void setPrivilegeLimitationList(List<AssignmentInfoDto> privilegeLimitationList) {
        if (newAssignment.getLimitTargetContent() == null) {
            newAssignment.setLimitTargetContent(new AssignmentSelectorType());
        }
        List<ObjectReferenceType> referencesList = newAssignment.getLimitTargetContent().getTargetRef();
        if (referencesList == null) {
            referencesList = new ArrayList<>();
        }
        referencesList.clear();
        for (AssignmentInfoDto previewDto : privilegeLimitationList) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(previewDto.getTargetOid());
            ref.setTargetName(new PolyStringType(previewDto.getTargetName()));
            ref.setType(previewDto.getTargetType());
            ref.setRelation(previewDto.getRelation());
            referencesList.add(ref);
        }
        this.privilegeLimitationList = privilegeLimitationList;
    }

    public boolean isLimitTargetAllowTransitive() {
        AssignmentSelectorType limitTargetContent = newAssignment.getLimitTargetContent();
        if (limitTargetContent == null) {
            return false;
        }
        Boolean allowTransitive = limitTargetContent.isAllowTransitive();
        if (allowTransitive == null) {
            return false;
        }
        return allowTransitive;
    }

    public void setLimitTargetAllowTransitive(Boolean newValue) {
        AssignmentSelectorType limitTargetContent = newAssignment.getLimitTargetContent();
        if (limitTargetContent == null) {
            if (newValue == null) {
                return;
            }
            limitTargetContent = new AssignmentSelectorType();
            newAssignment.setLimitTargetContent(limitTargetContent);
        }
        limitTargetContent.setAllowTransitive(newValue);
    }

    public UserType getDelegationOwner() {
        return delegationOwner;
    }

    public void setDelegationOwner(UserType delegationOwner) {
        this.delegationOwner = delegationOwner;
    }

    public List<QName> getNotAssignedRelationsList() {
        List<QName> availableRelations = RelationUtil.getCategoryRelationChoices(AreaCategoryType.ADMINISTRATION, pageBase);
        List<QName> assignedRelationsList = getAssignedRelationsList();
        if (assignedRelationsList == null || assignedRelationsList.size() == 0) {
            return availableRelations;
        }
        for (QName assignedRelation : assignedRelationsList) {
            Iterator<QName> availableRelationsIterator = availableRelations.iterator();
            while (availableRelationsIterator.hasNext()) {
                QName rel = availableRelationsIterator.next();
                if (QNameUtil.match(assignedRelation, rel)) {
                    availableRelationsIterator.remove();
                    break;
                }
            }
        }
        return availableRelations;
    }

    public boolean isAssignable() {
        if (!isAlreadyAssigned) {
            return true;
        }
        if (defaultAssignmentConstraints == null) {
            return true;
        }
        if (defaultAssignmentConstraints.isAllowSameTarget() && defaultAssignmentConstraints.isAllowSameRelation()) {
            return true;
        }
        List<QName> availableRelations = RelationUtil.getCategoryRelationChoices(AreaCategoryType.ADMINISTRATION, pageBase);
        int relationsListSize = availableRelations == null ? 0 : availableRelations.size();
        if (defaultAssignmentConstraints.isAllowSameTarget() && !defaultAssignmentConstraints.isAllowSameRelation()
                && getAssignedRelationsList().size() < relationsListSize) {
            return true;
        }
        if (!defaultAssignmentConstraints.isAllowSameTarget()) {
            return false;
        }
        return false;
    }

    public boolean isMultyAssignable() {
        if (defaultAssignmentConstraints == null) {
            return true;
        }
        return defaultAssignmentConstraints.isAllowSameTarget() && defaultAssignmentConstraints.isAllowSameRelation();
    }

    public boolean isSingleAssignable() {
        if (defaultAssignmentConstraints == null) {
            return false;
        }
        return !defaultAssignmentConstraints.isAllowSameTarget() && !defaultAssignmentConstraints.isAllowSameRelation();
    }

    public OtherPrivilegesLimitationType getPrivilegesLimitation() {
        return newAssignment.getLimitOtherPrivileges();
    }

    public void setPrivilegesLimitation(OtherPrivilegesLimitationType limitations) {
        newAssignment.setLimitOtherPrivileges(limitations);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssignmentEditorDto)) {
            return false;
        }

        AssignmentEditorDto that = (AssignmentEditorDto) o;
        return type == that.type
                && status == that.status
                && minimized == that.minimized
                && showEmpty == that.showEmpty
                && editable == that.editable
                && simpleView == that.simpleView
                && Objects.equals(altName, that.altName)
                && Objects.equals(attributes, that.attributes)
                && Objects.equals(name, that.name)
                && Objects.equals(newAssignment, that.newAssignment)
                && Objects.equals(oldAssignment, that.oldAssignment)
                && Objects.equals(isOrgUnitManager, that.isOrgUnitManager)
                && Objects.equals(orgRef, that.orgRef)
                && Objects.equals(tenantRef, that.tenantRef);
    }

    @Override
    public AssignmentEditorDto clone() {
        AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, newAssignment, pageBase);
        dto.setSimpleView(isSimpleView());
        return dto;
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
        result = 31 * result + (simpleView ? 1 : 0);
        result = 31 * result + (isOrgUnitManager ? 1 : 0);
        result = 31 * result + (newAssignment != null ? newAssignment.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AssignmentEditorDto(name=" + name + ", status=" + status + ", showEmpty=" + showEmpty
                + ", minimized=" + minimized + ", isOrgUnitManager=" + isOrgUnitManager + ")";
    }

    public String getNameForTargetObject() {
        return getNameForTargetObject(this.newAssignment);
    }
}
