/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
public class CertDefinitionDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_NUMBER_OF_STAGES = "numberOfStages";
    public static final String F_XML = "xml";
    public static final String F_OWNER = "owner";
    public static final String F_SCOPE_DEFINITION = "scopeDefinition";
    public static final String F_STAGE_DEFINITION = "stageDefinition";
    public static final String F_LAST_STARTED = "lastStarted";
    public static final String F_LAST_CLOSED = "lastClosed";

    private AccessCertificationDefinitionType oldDefinition;            // to be able to compute the delta when saving
    private AccessCertificationDefinitionType definition;               // definition that is (at least partially) dynamically updated when editing the form
    private final DefinitionScopeDto definitionScopeDto;
    private final List<StageDefinitionDto> stageDefinition;
    private String xml;
    private ObjectViewDto owner;

    public CertDefinitionDto(AccessCertificationDefinitionType definition, PageBase page, Task task, OperationResult result) {
        this.oldDefinition = definition.clone();
        this.definition = definition;
        owner = loadOwnerReference(definition.getOwnerRef());

        try {
            xml = page.getPrismContext().serializeObjectToString(definition.asPrismObject(), PrismContext.LANG_XML);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't serialize campaign definition to XML", e);
        }

        definitionScopeDto = createDefinitionScopeDto(definition.getScopeDefinition(), page.getPrismContext());
        stageDefinition = new ArrayList<>();
        for (AccessCertificationStageDefinitionType stageDef  : definition.getStageDefinition()){
            stageDefinition.add(createStageDefinitionDto(stageDef));
        }
    }

    private ObjectViewDto loadOwnerReference(ObjectReferenceType ref) {
        ObjectViewDto dto;

        if (ref != null) {
            if (ref.getTargetName() != null) {
                dto = new ObjectViewDto(ref.getOid(), WebMiscUtil.getOrigStringFromPoly(ref.getTargetName()));
                dto.setType(UserType.class);
                return dto;
            } else {
                dto = new ObjectViewDto(ObjectViewDto.BAD_OID);
                dto.setType(UserType.class);
                return dto;
            }
        } else {
            dto = new ObjectViewDto();
            dto.setType(UserType.class);
            return dto;
        }
    }

    public String getXml() {
        return xml;
    }

    public String getName() {
        return WebMiscUtil.getName(definition);
    }

    public String getDescription() {
        return definition.getDescription();
    }

    public int getNumberOfStages() {
        return definition.getStageDefinition().size();
    }

    public AccessCertificationDefinitionType getDefinition() {
        return definition;
    }

    public AccessCertificationDefinitionType getUpdatedDefinition(PrismContext prismContext) {
        updateOwner();
        updateScopeDefinition(prismContext);
        updateStageDefinition();
        return definition;
    }

    private void updateOwner() {
        String oid = owner.getKnownOid();
        if (oid != null) {
            definition.setOwnerRef(ObjectTypeUtil.createObjectRef(owner.getKnownOid(), ObjectTypes.USER));
        } else {
            definition.setOwnerRef(null);
        }
    }

    public AccessCertificationDefinitionType getOldDefinition() {
        return oldDefinition;
    }

    public void setDefinition(AccessCertificationDefinitionType definition) {
        this.definition = definition;
    }

    public void setName(String name){
        PolyStringType namePolyString  = new PolyStringType(name);
        definition.setName(namePolyString);
    }

    public void setDescription(String description){
        definition.setDescription(description);
    }

    public ObjectViewDto getOwner() {
        return owner;
    }

    public void setOwner(PrismReferenceValue owner) {
        ObjectReferenceType ownerRef = new ObjectReferenceType();
        ownerRef.setupReferenceValue(owner);
        definition.setOwnerRef(ownerRef);
    }

    private DefinitionScopeDto createDefinitionScopeDto(AccessCertificationScopeType scopeTypeObj, PrismContext prismContext) {
        DefinitionScopeDto dto = new DefinitionScopeDto();
        if (scopeTypeObj != null) {
            dto.setName(scopeTypeObj.getName());
            dto.setDescription(scopeTypeObj.getDescription());
            if (scopeTypeObj instanceof AccessCertificationObjectBasedScopeType) {
                AccessCertificationObjectBasedScopeType objScopeType = (AccessCertificationObjectBasedScopeType) scopeTypeObj;
                if (objScopeType.getObjectType() != null) {
                    dto.setObjectType(DefinitionScopeObjectType.valueOf(objScopeType.getObjectType().getLocalPart()));
                }
                dto.loadSearchFilter(objScopeType.getSearchFilter(), prismContext);
                if (objScopeType instanceof AccessCertificationAssignmentReviewScopeType) {
                    AccessCertificationAssignmentReviewScopeType assignmentScope =
                            (AccessCertificationAssignmentReviewScopeType) objScopeType;
                    dto.setIncludeAssignments(!Boolean.FALSE.equals(assignmentScope.isIncludeAssignments()));
                    dto.setIncludeInducements(!Boolean.FALSE.equals(assignmentScope.isIncludeInducements()));
                    dto.setIncludeResources(!Boolean.FALSE.equals(assignmentScope.isIncludeResources()));
                    dto.setIncludeRoles(!Boolean.FALSE.equals(assignmentScope.isIncludeRoles()));
                    dto.setIncludeOrgs(!Boolean.FALSE.equals(assignmentScope.isIncludeOrgs()));
                    dto.setEnabledItemsOnly(!Boolean.FALSE.equals(assignmentScope.isEnabledItemsOnly()));
                }
            }
        }
        return dto;
    }
    private StageDefinitionDto createStageDefinitionDto(AccessCertificationStageDefinitionType stageDefObj) {
        StageDefinitionDto dto = new StageDefinitionDto();
        if (stageDefObj != null) {
            dto.setNumber(stageDefObj.getNumber());
            dto.setName(stageDefObj.getName());
            dto.setDescription(stageDefObj.getDescription());
            dto.setDays(stageDefObj.getDays());
            dto.setNotifyBeforeDeadline(convertListIntegerToString(stageDefObj.getNotifyBeforeDeadline()));
            dto.setNotifyOnlyWhenNoDecision(Boolean.TRUE.equals(stageDefObj.isNotifyOnlyWhenNoDecision()));
            dto.setReviewerDto(createAccessCertificationReviewerDto(stageDefObj.getReviewerSpecification()));
        } else {
            dto.setReviewerDto(new AccessCertificationReviewerDto());
        }
        return dto;
    }

    private AccessCertificationReviewerDto createAccessCertificationReviewerDto(AccessCertificationReviewerSpecificationType reviewer) {
        AccessCertificationReviewerDto dto = new AccessCertificationReviewerDto();
        if (reviewer != null) {
            dto.setName(reviewer.getName());
            dto.setDescription(reviewer.getDescription());
            dto.setUseTargetOwner(Boolean.TRUE.equals(reviewer.isUseTargetOwner()));
            dto.setUseTargetApprover(Boolean.TRUE.equals(reviewer.isUseTargetApprover()));
            dto.setUseObjectOwner(Boolean.TRUE.equals(reviewer.isUseObjectOwner()));
            dto.setUseObjectApprover(Boolean.TRUE.equals(reviewer.isUseObjectApprover()));
            dto.setUseObjectManager(createManagerSearchDto(reviewer.getUseObjectManager()));
            dto.setDefaultReviewerRef(cloneListObjects(reviewer.getDefaultReviewerRef()));
            dto.setAdditionalReviewerRef(cloneListObjects(reviewer.getAdditionalReviewerRef()));
            dto.setApprovalStrategy(reviewer.getApprovalStrategy());
            dto.setFirstDefaultReviewerRef(loadOwnerReference(reviewer.getDefaultReviewerRef() == null ? null :
                    (reviewer.getDefaultReviewerRef().size() == 0 ? null : reviewer.getDefaultReviewerRef().get(0))));
            dto.setFirstAdditionalReviewerRef(loadOwnerReference(reviewer.getAdditionalReviewerRef() == null ? null :
                    (reviewer.getAdditionalReviewerRef().size() == 0 ? null : reviewer.getAdditionalReviewerRef().get(0))));
        }
        return dto;
    }

    private List<ObjectReferenceType> cloneListObjects(List<ObjectReferenceType> listToClone){
        List<ObjectReferenceType> list = new ArrayList<>();
        if (listToClone != null){
            for (ObjectReferenceType objectReferenceType : listToClone){
                list.add(objectReferenceType.clone());
            }
        }
        return list;
    }

    public DefinitionScopeDto getScopeDefinition() {
        return definitionScopeDto;
    }

    public void updateScopeDefinition(PrismContext prismContext) {
        AccessCertificationAssignmentReviewScopeType scopeTypeObj = null;
        if (definitionScopeDto != null) {
            scopeTypeObj = new AccessCertificationAssignmentReviewScopeType();
            scopeTypeObj.setName(definitionScopeDto.getName());
            scopeTypeObj.setDescription(definitionScopeDto.getDescription());
            scopeTypeObj.setObjectType(definitionScopeDto.getObjectType() != null ? new QName(definitionScopeDto.getObjectType().name()) : null);
            SearchFilterType parsedSearchFilter = definitionScopeDto.getParsedSearchFilter(prismContext);
            if (parsedSearchFilter != null) {
                // check if everything is OK
                try {
                    QueryConvertor.parseFilterPreliminarily(parsedSearchFilter.getFilterClauseXNode(), prismContext);
                } catch (SchemaException e) {
                    throw new SystemException("Couldn't parse search filter: " + e.getMessage(), e);
                }
            }
            scopeTypeObj.setSearchFilter(parsedSearchFilter);
            scopeTypeObj.setIncludeAssignments(definitionScopeDto.isIncludeAssignments());
            scopeTypeObj.setIncludeInducements(definitionScopeDto.isIncludeInducements());
            scopeTypeObj.setIncludeResources(definitionScopeDto.isIncludeResources());
            scopeTypeObj.setIncludeOrgs(definitionScopeDto.isIncludeOrgs());
            scopeTypeObj.setEnabledItemsOnly(definitionScopeDto.isEnabledItemsOnly());
            // needed because of prism implementation limitation (because the scopeDefinition is declared as AccessCertificationScopeType)
            scopeTypeObj.asPrismContainerValue().setConcreteType(AccessCertificationAssignmentReviewScopeType.COMPLEX_TYPE);
        }
        definition.setScopeDefinition(scopeTypeObj);
    }

    public void updateStageDefinition() {
        List<AccessCertificationStageDefinitionType> stageDefinitionTypeList = new ArrayList<>();
        if (stageDefinition != null && stageDefinition.size() > 0) {
            for (StageDefinitionDto stageDefinitionDto : stageDefinition){
                stageDefinitionTypeList.add(createStageDefinitionType(stageDefinitionDto));
            }
        }
        definition.getStageDefinition().clear();
        definition.getStageDefinition().addAll(stageDefinitionTypeList);
    }

    private AccessCertificationStageDefinitionType createStageDefinitionType(StageDefinitionDto stageDefDto) {
        AccessCertificationStageDefinitionType stageDefType = new AccessCertificationStageDefinitionType();
        if (stageDefDto != null) {
            stageDefType.setNumber(stageDefDto.getNumber());
            stageDefType.setName(stageDefDto.getName());
            stageDefType.setDescription(stageDefDto.getDescription());
            stageDefType.setDays(stageDefDto.getDays());
            stageDefType.getNotifyBeforeDeadline().clear();
            stageDefType.getNotifyBeforeDeadline().addAll(convertStringToListInteger(stageDefDto.getNotifyBeforeDeadline()));
            stageDefType.setNotifyOnlyWhenNoDecision(Boolean.TRUE.equals(stageDefDto.isNotifyOnlyWhenNoDecision()));
            stageDefType.setReviewerSpecification(createAccessCertificationReviewerType(stageDefDto.getReviewerDto()));
        }
        return stageDefType;
    }

    private AccessCertificationReviewerSpecificationType createAccessCertificationReviewerType(AccessCertificationReviewerDto reviewerDto) {
        AccessCertificationReviewerSpecificationType reviewerObject = new AccessCertificationReviewerSpecificationType();
        if (reviewerDto != null) {
            reviewerObject.setName(reviewerDto.getName());
            reviewerObject.setDescription(reviewerDto.getDescription());
            reviewerObject.setUseTargetOwner(Boolean.TRUE.equals(reviewerDto.isUseTargetOwner()));
            reviewerObject.setUseTargetApprover(Boolean.TRUE.equals(reviewerDto.isUseTargetApprover()));
            reviewerObject.setUseObjectOwner(Boolean.TRUE.equals(reviewerDto.isUseObjectOwner()));
            reviewerObject.setUseObjectApprover(Boolean.TRUE.equals(reviewerDto.isUseObjectApprover()));
            reviewerObject.setUseObjectManager(createManagerSearchType(reviewerDto.getUseObjectManager()));
            updateDefaultReviewer(reviewerDto);
            reviewerObject.getDefaultReviewerRef().clear();
            reviewerObject.getDefaultReviewerRef().addAll(cloneListObjects(reviewerDto.getDefaultReviewerRef()));
            updateAdditionalReviewer(reviewerDto);
            reviewerObject.getAdditionalReviewerRef().clear();
            reviewerObject.getAdditionalReviewerRef().addAll(cloneListObjects(reviewerDto.getAdditionalReviewerRef()));
            reviewerObject.setApprovalStrategy(reviewerDto.getApprovalStrategy());
        }
        return reviewerObject;
    }

    private ManagerSearchType createManagerSearchType(ManagerSearchDto managerSearchDto){
        ManagerSearchType managerSearchType = new ManagerSearchType();
        if (managerSearchDto != null){
            managerSearchType.setOrgType(managerSearchDto.getOrgType());
            managerSearchType.setAllowSelf(managerSearchDto.isAllowSelf());
        }
        return  managerSearchType;
    }

    private ManagerSearchDto createManagerSearchDto(ManagerSearchType managerSearchType){
        ManagerSearchDto managerSearchDto = new ManagerSearchDto();
        if (managerSearchType != null){
            managerSearchDto.setOrgType(managerSearchType.getOrgType());
            managerSearchDto.setAllowSelf(managerSearchType.isAllowSelf());
        }
        return managerSearchDto;
    }

    private String convertListIntegerToString(List<Integer> list){
        String result = "";
        for (Integer listItem : list){
            result += Integer.toString(listItem);
            if(list.indexOf(listItem) < list.size() - 1){
                result += ", ";
            }
        }
        return result;
    }

    private List<Integer> convertStringToListInteger(String object){
        List<Integer> list = new ArrayList<>();
        if (object != null) {
            String[] values = object.split(",");
            for (String value : values) {
                if (!value.trim().equals("")) {
                    list.add(Integer.parseInt(value.trim()));
                }
            }
        }
        return list;
    }

    private void updateDefaultReviewer(AccessCertificationReviewerDto reviewerDto){
        if (reviewerDto.getFirstDefaultReviewerRef() != null) {
            String oid = reviewerDto.getFirstDefaultReviewerRef().getKnownOid();
            if (reviewerDto.getDefaultReviewerRef() == null){
                reviewerDto.setDefaultReviewerRef(new ArrayList<ObjectReferenceType>());
            }
            if (oid != null) {
                if (reviewerDto.getDefaultReviewerRef().size() == 0){
                    reviewerDto.getDefaultReviewerRef().
                            add(ObjectTypeUtil.createObjectRef(reviewerDto.getFirstDefaultReviewerRef().getKnownOid(), ObjectTypes.USER));
                } else {
                    reviewerDto.getDefaultReviewerRef().
                            set(0, ObjectTypeUtil.createObjectRef(reviewerDto.getFirstDefaultReviewerRef().getKnownOid(), ObjectTypes.USER));
                }
            } else {
                if (reviewerDto.getDefaultReviewerRef().size() > 0) {
                    reviewerDto.getDefaultReviewerRef().remove(0);
                }
            }
        }
    }

    private void updateAdditionalReviewer(AccessCertificationReviewerDto reviewerDto){
        if (reviewerDto.getFirstAdditionalReviewerRef() != null) {
            String oid = reviewerDto.getFirstAdditionalReviewerRef().getKnownOid();
            if (reviewerDto.getAdditionalReviewerRef() == null){
                reviewerDto.setAdditionalReviewerRef(new ArrayList<ObjectReferenceType>());
            }
            if (oid != null) {
                if (reviewerDto.getAdditionalReviewerRef().size() == 0){
                    reviewerDto.getAdditionalReviewerRef().
                            add(ObjectTypeUtil.createObjectRef(reviewerDto.getFirstAdditionalReviewerRef().getKnownOid(), ObjectTypes.USER));
                } else {
                    reviewerDto.getAdditionalReviewerRef().
                            set(0, ObjectTypeUtil.createObjectRef(reviewerDto.getFirstAdditionalReviewerRef().getKnownOid(), ObjectTypes.USER));
                }
            } else {
                if (reviewerDto.getAdditionalReviewerRef().size() > 0) {
                    reviewerDto.getDefaultReviewerRef().remove(0);
                }
            }
        }
    }

    public String getLastStarted() {
        return formatDate(definition.getLastCampaignStartedTimestamp());
    }

    private String formatDate(XMLGregorianCalendar dateGc) {
        if (dateGc == null) {
            return "-";
        } else {
            return WebMiscUtil.formatDate(XmlTypeConverter.toDate(dateGc));
        }
    }

    public String getLastClosed() {
        return formatDate(definition.getLastCampaignClosedTimestamp());
    }
}
