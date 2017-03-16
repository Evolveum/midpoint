/*
 * Copyright (c) 2010-2016 Evolveum
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentsPreviewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;

/**
 * TODO: unify with AssignmentItemDto
 * 
 * @author lazyman
 */
public class AssignmentEditorDto extends SelectableBean implements Comparable<AssignmentEditorDto> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentEditorDto.class);

	private static final String DOT_CLASS = AssignmentEditorDto.class.getName() + ".";
	private static final String OPERATION_LOAD_ORG_TENANT = DOT_CLASS + "loadTenantOrg";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
	private static final String OPERATION_LOAD_REFERENCE_OBJECT = DOT_CLASS + "loadReferenceObject";
	private static final String OPERATION_LOAD_ATTRIBUTES = DOT_CLASS + "loadAttributes";

	public static final String F_TYPE = "type";
	public static final String F_NAME = "name";
	public static final String F_DESCRIPTION = "description";
	public static final String F_ACTIVATION = "activation";
	public static final String F_RELATION = "relation";
	public static final String F_FOCUS_TYPE = "focusType";
	public static final String F_TENANT_REF = "tenantRef";
	public static final String F_ORG_REF = "orgRef";
	public static final String F_ALT_NAME = "altName";
	public static final String F_IS_ORG_UNIT_MANAGER = "isOrgUnitManager";

	private String name;
	private String altName;
	private AssignmentEditorDtoType type;
	private UserDtoStatus status;
	private AssignmentType oldAssignment;
	private List<AssignmentsPreviewDto> privilegeLimitationList;
	private ObjectViewDto<OrgType> tenantRef;
	private ObjectViewDto<OrgType> orgRef;

	private boolean showEmpty = false;
	private boolean minimized = true;
	private boolean editable = true;
	private boolean simpleView = false;

	private boolean isAlreadyAssigned = false;

	private Boolean isOrgUnitManager = Boolean.FALSE;
	private AssignmentType newAssignment;
	private List<ACAttributeDto> attributes;
	private PageBase pageBase;
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
		List<Item> itemsList = newValue.getItems();
		if (itemsList != null && itemsList.size() > 0) {
			Item item = itemsList.get(0);
			if (item != null && item.getDefinition() != null) {
				this.editable = item.getDefinition().canAdd() || item.getDefinition().canModify();
			}
		}

		this.tenantRef = loadTenantOrgReference(assignment, assignment.getTenantRef());
		this.orgRef = loadTenantOrgReference(assignment, assignment.getOrgRef());

		this.name = getNameForTargetObject(assignment);
		this.altName = getAlternativeName(assignment);

		this.attributes = prepareAssignmentAttributes(assignment, pageBase);
		this.isOrgUnitManager = determineUserOrgRelation(assignment);
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
		if (SchemaConstants.ORG_DEPUTY.equals(relation)){
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
		if (relation != null){
			targetRef.setRelation(relation);
		}

		AssignmentType assignment = new AssignmentType();
		assignment.setTargetRef(targetRef);

		return new AssignmentEditorDto(status, assignment, pageBase);
	}

	private AssignmentEditorDtoType getType(AssignmentType assignment) {
		if (assignment.getTarget() != null) {
			// object assignment
			return AssignmentEditorDtoType.getType(assignment.getTarget().getClass());
		} else if (assignment.getTargetRef() != null) {
			return AssignmentEditorDtoType.getType(assignment.getTargetRef().getType());
		} // account assignment through account construction
		return AssignmentEditorDtoType.CONSTRUCTION;

	}

	private List<AssignmentsPreviewDto> getAssignmentPrivilegesList(AssignmentType assignment){
		List<AssignmentsPreviewDto> list = new ArrayList<>();
		AssignmentSelectorType assignmentSelectorType = assignment.getLimitTargetContent();
		if (assignmentSelectorType != null && assignmentSelectorType.getTargetRef() != null){
			for (ObjectReferenceType objectRef : assignmentSelectorType.getTargetRef()){
				AssignmentsPreviewDto dto = new AssignmentsPreviewDto();
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
	private Boolean determineUserOrgRelation(AssignmentType assignment) {

		if (assignment == null || assignment.getTargetRef() == null
				|| assignment.getTargetRef().getRelation() == null) {
			return Boolean.FALSE;
		}

		if (SchemaConstants.ORG_MANAGER.equals(assignment.getTargetRef().getRelation())) {
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
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

		PrismObject<ResourceType> resource = construction.getResource() != null
				? construction.getResource().asPrismObject() : null;
		if (resource == null) {
			resource = getReference(construction.getResourceRef(), result, pageBase);
		}

		try {
			PrismContext prismContext = pageBase.getPrismContext();
			RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource,
					LayerType.PRESENTATION, prismContext);
			RefinedObjectClassDefinition objectClassDefinition = refinedSchema
					.getRefinedDefinition(ShadowKindType.ACCOUNT, construction.getIntent());

			if (objectClassDefinition == null) {
				return attributes;
			}

			PrismContainerDefinition definition = objectClassDefinition
					.toResourceAttributeContainerDefinition();

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Refined definition for {}\n{}", construction, definition.debugDump());
			}

			Collection<ItemDefinition> definitions = definition.getDefinitions();
			for (ResourceAttributeDefinitionType attribute : assignment.getConstruction().getAttribute()) {

				for (ItemDefinition attrDef : definitions) {
					if (attrDef instanceof PrismPropertyDefinition) {
						PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) attrDef;

						if (propertyDef.isOperational() || propertyDef.isIgnored()) {
							continue;
						}

						if (ItemPathUtil.getOnlySegmentQName(attribute.getRef())
								.equals(propertyDef.getName())) {
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
			if (ref.getType() != null) {
				type = pageBase.getPrismContext().getSchemaRegistry()
						.determineCompileTimeClass(ref.getType());
			}
			target = pageBase.getModelService().getObject(type, ref.getOid(), null, task, subResult);
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get account construction resource ref", ex);
			subResult.recordFatalError("Couldn't get account construction resource ref.", ex);
		}

		return target;
	}

	private boolean isRole(AssignmentType assignment) {
		if (assignment.getTarget() != null) {
			// object assignment
			return RoleType.class.equals(assignment.getTarget().getClass());
		} else if (assignment.getTargetRef() != null) {
			// object assignment through reference
			if (assignment.getTargetRef().getType() != null) {
				return RoleType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType());
			}
			return false;
		} else if (assignment.getConstruction() != null) {
			// account assignment through account construction
			return false;
		}

		return false;

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

		StringBuilder sb = new StringBuilder();

		if (assignment.getConstruction() != null) {
			// account assignment through account construction
			ConstructionType construction = assignment.getConstruction();
			if (construction.getResource() != null) {
				sb.append(WebComponentUtil.getName(construction.getResource()));
			} else if (construction.getResourceRef() != null) {
				sb.append(WebComponentUtil.getName(construction.getResourceRef()));
			}
			return sb.toString();
		}

		if (assignment.getTarget() != null) {
			sb.append(WebComponentUtil.getEffectiveName(assignment.getTarget(), OrgType.F_DISPLAY_NAME));
			appendTenantAndOrgName(sb);
		} else if (assignment.getTargetRef() != null) {
			Task task = pageBase.createSimpleTask("Load assignment name");
			PrismObject<FocusType> target = WebModelServiceUtils.loadObject(FocusType.class,
					assignment.getTargetRef().getOid(), pageBase, task, task.getResult());
			if (target != null) {
				sb.append(WebComponentUtil.getEffectiveName(target, OrgType.F_DISPLAY_NAME));
			} else {
				sb.append(WebComponentUtil.getName(assignment.getTargetRef()));
			}
			appendTenantAndOrgName(sb);
		}

		if (assignment.getTargetRef() != null && assignment.getTargetRef().getRelation() != null) {
			sb.append(" - "  + RelationTypes.getRelationType(assignment.getTargetRef().getRelation()).getHeaderLabel());
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
		// this removes activation element if it's empty
		ActivationType activation = newAssignment.getActivation();
		if (activation == null || activation.asPrismContainerValue().isEmpty()) {
			newAssignment.setActivation(null);
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
				;
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
		ObjectReferenceType ref = newAssignment.getTargetRef();
		if (ref == null || ref.getRelation() == null) {
			return null;
		}

		return ref.getRelation().getLocalPart();
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

	public List<AssignmentsPreviewDto> getPrivilegeLimitationList() {
		return privilegeLimitationList;
	}

	public void setPrivilegeLimitationList(List<AssignmentsPreviewDto> privilegeLimitationList) {
		if (newAssignment.getLimitTargetContent() == null){
			newAssignment.setLimitTargetContent(new AssignmentSelectorType());
		}
		List<ObjectReferenceType> referencesList = newAssignment.getLimitTargetContent().getTargetRef();
		if (referencesList == null){
			referencesList = new ArrayList<>();
		}
		referencesList.clear();
		for (AssignmentsPreviewDto previewDto : privilegeLimitationList){
			ObjectReferenceType ref = new ObjectReferenceType();
			ref.setOid(previewDto.getTargetOid());
			ref.setTargetName(new PolyStringType(previewDto.getTargetName()));
			ref.setType(previewDto.getTargetType());
			referencesList.add(ref);
		}
		this.privilegeLimitationList = privilegeLimitationList;
	}

	public UserType getDelegationOwner() {
		return delegationOwner;
	}

	public void setDelegationOwner(UserType delegationOwner) {
		this.delegationOwner = delegationOwner;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AssignmentEditorDto))
			return false;

		AssignmentEditorDto that = (AssignmentEditorDto) o;

		if (isOrgUnitManager != that.isOrgUnitManager)
			return false;
		if (minimized != that.minimized)
			return false;
		if (showEmpty != that.showEmpty)
			return false;
		if (editable != that.editable)
			return false;
		if (simpleView != that.simpleView)
			return false;
		if (altName != null ? !altName.equals(that.altName) : that.altName != null)
			return false;
		if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null)
			return false;
		if (name != null ? !name.equals(that.name) : that.name != null)
			return false;
		if (newAssignment != null ? !newAssignment.equals(that.newAssignment) : that.newAssignment != null)
			return false;
		if (oldAssignment != null ? !oldAssignment.equals(that.oldAssignment) : that.oldAssignment != null)
			return false;
		if (status != that.status)
			return false;
		if (tenantRef != null ? !tenantRef.equals(that.tenantRef) : that.tenantRef != null)
			return false;
		if (orgRef != null ? !orgRef.equals(that.orgRef) : that.orgRef != null)
			return false;
		if (type != that.type)
			return false;

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
